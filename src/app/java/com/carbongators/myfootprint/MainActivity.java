package com.carbongators.myfootprint;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.AuthorizationServiceDiscovery;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.EndSessionRequest;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;
import okio.Okio;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.carbongators.myfootprint.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private static final String TAG = "MainActivity";

    private static final String KEY_USER_INFO = "userInfo";

    private static final int END_SESSION_REQUEST_CODE = 911;

    private AuthorizationService mAuthService;
    private AuthStateManager mStateManager;
    public final AtomicReference<JSONObject> mUserInfoJson = new AtomicReference<>();
    private ExecutorService mExecutor;
    private Configuration mConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStateManager = AuthStateManager.getInstance(this);
        mExecutor = Executors.newSingleThreadExecutor();
        mConfiguration = Configuration.getInstance(this);

        mAuthService = new AuthorizationService(
            this,
            new AppAuthConfiguration.Builder()
                .setConnectionBuilder(mConfiguration.getConnectionBuilder())
                .build());

        if (savedInstanceState != null) {
            try {
                mUserInfoJson.set(new JSONObject(savedInstanceState.getString(KEY_USER_INFO)));
            } catch (JSONException ex) {
                finish();
                Log.e(TAG, "Failed to parse saved user info JSON, discarding", ex);
            }
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_social, R.id.navigation_account)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }


    @Override
    protected void onStart() {
        super.onStart();

        if (mExecutor.isShutdown()) {
            mExecutor = Executors.newSingleThreadExecutor();
        }

        if (mStateManager.getCurrent().isAuthorized()) {
            displayAuthorized();
            return;
        }

        // the stored AuthState is incomplete, so check if we are currently receiving the result of
        // the authorization flow from the browser.
        AuthorizationResponse response = AuthorizationResponse.fromIntent(getIntent());
        AuthorizationException ex = AuthorizationException.fromIntent(getIntent());

        if (response != null || ex != null) {
            mStateManager.updateAfterAuthorization(response, ex);
        }

        if (response != null && response.authorizationCode != null) {
            // authorization code exchange is required
            mStateManager.updateAfterAuthorization(response, ex);
            exchangeAuthorizationCode(response);
        } else if (ex != null) {
            finish();
        } else {
            finish();
        }
    }
    @MainThread
    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
        performTokenRequest(
            authorizationResponse.createTokenExchangeRequest(),
            this::handleCodeExchangeResponse);
    }

    @WorkerThread
    private void handleCodeExchangeResponse(
        @Nullable TokenResponse tokenResponse,
        @Nullable AuthorizationException authException) {

        mStateManager.updateAfterTokenResponse(tokenResponse, authException);
        if (!mStateManager.getCurrent().isAuthorized()) {
            final String message = "Authorization Code exchange failed"
                + ((authException != null) ? authException.error : "");

            // WrongThread inference is incorrect for lambdas
            //noinspection WrongThread
            finish();
        } else {
            runOnUiThread(this::displayAuthorized);
        }
    }

    public String getAccessTokenExp(){

        return accessTokenExp;
    }

    public String accessTokenExp;

    @MainThread
    private void displayAuthorized() {


        AuthState state = mStateManager.getCurrent();


        if (state.getAccessToken() == null) {
            accessTokenExp = String.valueOf(R.string.no_access_token_returned);
        } else {
            Long expiresAt = state.getAccessTokenExpirationTime();
            if (expiresAt == null) {
                accessTokenExp = String.valueOf(R.string.no_access_token_expiry);
            } else if (expiresAt < System.currentTimeMillis()) {
                accessTokenExp = String.valueOf(R.string.access_token_expired);
            } else {
                String template = getResources().getString(R.string.access_token_expires_at);
                accessTokenExp = String.format(template,
                    DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss ZZ").print(expiresAt));
            }
        }

        fetchUserInfo();


    }

    @MainThread
    private void endSession() {
        AuthState currentState = mStateManager.getCurrent();
        AuthorizationServiceConfiguration config =
            currentState.getAuthorizationServiceConfiguration();
        if (config.endSessionEndpoint != null) {
            Intent endSessionIntent = mAuthService.getEndSessionRequestIntent(
                new EndSessionRequest.Builder(config)
                    .setIdTokenHint(currentState.getIdToken())
                    .setPostLogoutRedirectUri(mConfiguration.getEndSessionRedirectUri())
                    .build());
            startActivityForResult(endSessionIntent, END_SESSION_REQUEST_CODE);
        } else {
            signOut();
        }
    }

    @MainThread
    public void signOut() {
        // discard the authorization and token state, but retain the configuration and
        // dynamic client registration (if applicable), to save from retrieving them again.
        AuthState currentState = mStateManager.getCurrent();
        AuthState clearedState =
            new AuthState(currentState.getAuthorizationServiceConfiguration());
        if (currentState.getLastRegistrationResponse() != null) {
            clearedState.update(currentState.getLastRegistrationResponse());
        }
        mStateManager.replace(clearedState);

        Intent mainIntent = new Intent(this, LoginActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainIntent);
        finish();
    }


    @MainThread
    public void fetchUserInfo() {
        mStateManager.getCurrent().performActionWithFreshTokens(mAuthService, this::fetchUserInfo);
    }

    @MainThread
    private void fetchUserInfo(String accessToken, String idToken, AuthorizationException ex) {
        if (ex != null) {
            Log.e(TAG, "Token refresh failed when fetching user info");
            mUserInfoJson.set(null);
            runOnUiThread(this::displayAuthorized);
            return;
        }

        AuthorizationServiceDiscovery discovery =
            mStateManager.getCurrent()
                .getAuthorizationServiceConfiguration()
                .discoveryDoc;

        Uri userInfoEndpoint =
            mConfiguration.getUserInfoEndpointUri() != null
                ? Uri.parse(mConfiguration.getUserInfoEndpointUri().toString())
                : Uri.parse(discovery.getUserinfoEndpoint().toString());

        mExecutor.submit(() -> {
            try {
                HttpURLConnection conn = mConfiguration.getConnectionBuilder().openConnection(
                    userInfoEndpoint);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setInstanceFollowRedirects(false);
                String response = Okio.buffer(Okio.source(conn.getInputStream()))
                    .readString(Charset.forName("UTF-8"));
                mUserInfoJson.set(new JSONObject(response));
            } catch (IOException ioEx) {
                Log.e(TAG, "Network error when querying userinfo endpoint", ioEx);
            } catch (JSONException jsonEx) {
                Log.e(TAG, "Failed to parse userinfo response");
            }

            runOnUiThread(this::displayAuthorized);
        });
    }


    @MainThread
    public void refreshAccessToken() {
        performTokenRequest(
            mStateManager.getCurrent().createTokenRefreshRequest(),
            this::handleAccessTokenResponse);
    }

    @MainThread
    private void performTokenRequest(
        TokenRequest request,
        AuthorizationService.TokenResponseCallback callback) {
        ClientAuthentication clientAuthentication;
        try {
            clientAuthentication = mStateManager.getCurrent().getClientAuthentication();
        } catch (ClientAuthentication.UnsupportedAuthenticationMethod ex) {
            Log.d(TAG, "Token request cannot be made, client authentication for the token "
                + "endpoint could not be constructed (%s)", ex);
            finish();
            return;
        }

        mAuthService.performTokenRequest(
            request,
            clientAuthentication,
            callback);
    }

    @WorkerThread
    private void handleAccessTokenResponse(
        @Nullable TokenResponse tokenResponse,
        @Nullable AuthorizationException authException) {
        mStateManager.updateAfterTokenResponse(tokenResponse, authException);
        runOnUiThread(this::displayAuthorized);
    }





}
