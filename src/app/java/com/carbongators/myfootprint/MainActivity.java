package com.carbongators.myfootprint;

import android.graphics.Color;
import android.os.Bundle;

import com.ekn.gruzer.gaugelibrary.ArcGauge;
import com.ekn.gruzer.gaugelibrary.Range;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
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
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.Math;

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

    // Global variables to calculate carbon footprint
    public double gas = 0;
    public double electricity = 0;
    public double oil = 0;
    public double propane= 0;
    public double milesDriven= 0;
    public double mileage= 0;
    public boolean maintenance;
    public boolean[] recyclable = new boolean[5];

    // Default value is -1
    public int footPrint = -1;

    LineChart lineChart;

    ArcGauge arcGauge;
    com.ekn.gruzer.gaugelibrary.Range range1, range2, range3;

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

        // Change the text at the top of screen to say "Hi, " + user first name
        //DOES NOT WORK YET
        String greeting = "Hi, " + "Andres";
        ((TextView) findViewById(R.id.textView3)).setText(greeting);

    }

    @MainThread
    public String getUserFirstName() {

        JSONObject userInfo = mUserInfoJson.get();

        if (userInfo != null) {
            if (userInfo.has("given_name")) {
                try {
                    return userInfo.getString("given_name");
                } catch (JSONException e) {
                    return null;
                }
            }
        }

        return null;

    }


    @MainThread
    public int getUserToken() {

        JSONObject userInfo = mUserInfoJson.get();

        if (userInfo != null) {
            if (userInfo.has("sub")) {
                try {
                    return Integer.parseInt(userInfo.getString("sub"));
                } catch (JSONException e) {
                    return -1;
                }
            }
        }

        return -1;

    }


    @Override
    protected void onStart() {
        super.onStart();

        arcGauge = findViewById(R.id.arcGuage);
        arcGauge.setMinValue(0);
        arcGauge.setMaxValue(1000);

        range1 = new Range();
        range1.setFrom(0);
        range1.setTo(333);
        range1.setColor(Color.RED);
        arcGauge.addRange(range1);

        range2 = new Range();
        range2.setFrom(333);
        range2.setTo(666);
        range2.setColor(Color.YELLOW);
        arcGauge.addRange(range2);

        range3 = new Range();
        range3.setFrom(666);
        range3.setTo(1000);
        range3.setColor(Color.GREEN);
        arcGauge.addRange(range3);

        ((LineChart)findViewById(R.id.lineChart)).setVisibility(View.GONE);

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

    public void button5_onClick(View v){
        ConstraintLayout homeScreen = (ConstraintLayout)findViewById(R.id.homeScreen);
        ConstraintLayout questions = (ConstraintLayout)findViewById(R.id.questionsScreen);
        homeScreen.setVisibility(View.GONE);
        questions.setVisibility(View.VISIBLE);
    }
    public void button6_onClick(View v){
        ConstraintLayout homeScreen = (ConstraintLayout)findViewById(R.id.homeScreen);
        ConstraintLayout questions = (ConstraintLayout)findViewById(R.id.questionsScreen);
        gas = Double.parseDouble(((EditText) findViewById(R.id.editTextNumberDecimal7)).getText().toString());
        electricity = Double.parseDouble(((EditText) findViewById(R.id.editTextNumberDecimal8)).getText().toString());
        oil = Double.parseDouble(((EditText) findViewById(R.id.editTextNumberDecimal9)).getText().toString());
        propane = Double.parseDouble(((EditText) findViewById(R.id.editTextNumberDecimal10)).getText().toString());
        milesDriven = Double.parseDouble(((EditText) findViewById(R.id.editTextNumberDecimal11)).getText().toString());
        mileage = Double.parseDouble(((EditText) findViewById(R.id.editTextNumberDecimal12)).getText().toString());
        maintenance = ((CheckBox) findViewById(R.id.checkBox4)).isChecked();

        recyclable[0] = ((CheckBox) findViewById(R.id.checkBox2)).isChecked();
        recyclable[1] = ((CheckBox) findViewById(R.id.checkBox3)).isChecked();
        recyclable[2] = ((CheckBox) findViewById(R.id.checkBox5)).isChecked();
        recyclable[3] = ((CheckBox) findViewById(R.id.checkBox6)).isChecked();
        recyclable[4] = ((CheckBox) findViewById(R.id.checkBox7)).isChecked();

        footPrint = calcTotalFootprint(11111, gas, electricity, oil, propane, milesDriven, mileage, maintenance, recyclable);
        int score = 1000 - (int)(636 * Math.atan(1.0 * footPrint / 10000));

        arcGauge.setValue(score);

        Description desc = new Description();
        desc.setText("");
        lineChart = (LineChart) findViewById(R.id.lineChart);
        LineDataSet lineDataSet = new LineDataSet(lineChartData(),"Your score over the last 7 days");
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet);
        LineData data = new LineData(dataSets);
        lineDataSet.setCircleColor(Color.GREEN);
        lineDataSet.setColor(Color.GREEN);
        lineDataSet.setCircleRadius(4);
        lineChart.setBackgroundColor(Color.TRANSPARENT);
        lineChart.setDescription(desc);
        lineChart.setData(data);
        lineChart.invalidate();
        ((LineChart)findViewById(R.id.lineChart)).setVisibility(View.VISIBLE);

        ((TextView) findViewById(R.id.textView5)).setVisibility(View.GONE);
        homeScreen.setVisibility(View.VISIBLE);
        questions.setVisibility(View.GONE);
    }
    public static double houseHoldFootprint(int zip, double nGasUse, double elecUse, double oilUse, double propUse)
    {
        double totalHouseHoldFPrint = 0; //in lbs
        totalHouseHoldFPrint += nGasUse*119.58;
        totalHouseHoldFPrint += elecUse*14.4215172;
        totalHouseHoldFPrint += oilUse*22.61;
        totalHouseHoldFPrint += propUse*12.43;
        return totalHouseHoldFPrint;
    }
    public static double transportFootprint(int zip, double milesPerWeek, double milesPerGallon) //handles the list outside
    {
        double totalOutput = 0;
        totalOutput += 4*milesPerWeek*milesPerGallon;
        return totalOutput;
    }
    public static double wasteFootprint(int zip, boolean[] recycleList)
    {
        double totalWasteFootprint = 0;
        if(recycleList[0])
        {
            totalWasteFootprint -= 89.38/12;
        }
        if(recycleList[1])
        {
            totalWasteFootprint -= 35.56/12;
        }
        if(recycleList[2])
        {
            totalWasteFootprint -= 25.39/12;
        }
        if(recycleList[3])
        {
            totalWasteFootprint -= 113.14/12;
        }
        if(recycleList[4])
        {
            totalWasteFootprint -= 27.46/12;
        }
        return totalWasteFootprint;
    }
    public static int calcTotalFootprint(int zip, double nGasUse, double elecUse, double oilUse, double propUse, double milesPerWeek, double milesPerGallon, boolean maintenance, boolean[] recycleList)
    {
        double totalFootprint = 0;
        totalFootprint += houseHoldFootprint(zip, nGasUse, elecUse, oilUse, propUse);
        totalFootprint += transportFootprint(zip, milesPerWeek, milesPerGallon);
        totalFootprint += wasteFootprint(zip, recycleList);
        int totalOutput = (int)totalFootprint;
        return totalOutput;
    }

    private ArrayList<Entry> lineChartData() {
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        for(int i = 1; i <= 7; i++)
        {
            dataVals.add(new Entry(i, i * i));
        }
        return dataVals;
    }
}
