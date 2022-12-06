package com.carbongators.myfootprint;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
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
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.carbongators.myfootprint.databinding.ActivityMainBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
    public double gasMileageGlobal = 0.0;
    public double kwhPriceGlobal = 0.0;
    public double carbonFootprintGlobal = 0.0;
    public boolean[] externalFuelsBoolGlobal = {false, false, false};
    public boolean hasDoneInitialFetch = false;

    public List<String> friendsListTokens;
    public List<String> friendsListNames;
    public List<String> friendsListImages;
    public List<Boolean> friendsListIsFriendNotInvite;
    public List<Double> friendsListDrivingBreakdown;
    public List<Double> friendsListElecBreakdown;
    public List<Double> friendsListOtherBreakdown;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

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
    ArrayList<NewsArticleModel> newsArticleModels = new ArrayList<>();
    int[] newsImages = {R.drawable.bill, R.drawable.marine};
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
        //String greeting = "Hi, " + getUserFirstName();
        //(TextView) findViewById(R.id.textView3)).setText(greeting);
        //Notification to be sent when button6 is clicked
        String greeting = "Hi, " + "Andres";
        ((TextView) findViewById(R.id.textView3)).setText(greeting);
        Button button6 = (Button) findViewById(R.id.button6);
        createNotificationChannel();
        button6.setOnClickListener(v -> {
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

            String footPrintString = ""+footPrint;
            ((TextView) findViewById(R.id.textView5)).setText(footPrintString);
            ((TextView) findViewById(R.id.textView5)).setTextColor(Color.GREEN);
            homeScreen.setVisibility(View.VISIBLE);
            questions.setVisibility(View.GONE);

            Toast.makeText(this, "Data Entered!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this,ReminderBroadcast.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

            long timeAtButtonClick = System.currentTimeMillis();
            long oneDayInMillis = 1000*86400;
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeAtButtonClick + oneDayInMillis, pendingIntent);

        });
        //News Article Setup
        RecyclerView recyclerView = findViewById(R.id.newsRecycler);
        setUpNewsModels();
        News_RecyclerViewAdapter adapter = new News_RecyclerViewAdapter(this, newsArticleModels);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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
    public String getUserToken() {

        JSONObject userInfo = mUserInfoJson.get();

        if (userInfo != null) {
            if (userInfo.has("sub")) {
                try {
                    return userInfo.getString("sub");
                } catch (JSONException e) {
                    return null;
                }
            }
        }

        return null;

    }

    public String getUserFullName() {

        JSONObject userInfo = mUserInfoJson.get();

        if (userInfo != null) {
            if (userInfo.has("name")) {
                try {
                    return userInfo.getString("name");
                } catch (JSONException e) {
                    return null;
                }
            }
        }

        return null;

    }

    public String getUserEmail() {

        JSONObject userInfo = mUserInfoJson.get();

        if (userInfo != null) {
            if (userInfo.has("email")) {
                try {
                    return userInfo.getString("email");
                } catch (JSONException e) {
                    return null;
                }
            }
        }

        return null;

    }

    public String getUserImageURL() {

        JSONObject userInfo = mUserInfoJson.get();

        if (userInfo != null) {
            if (userInfo.has("picture")) {
                try {
                    return userInfo.getString("picture");
                } catch (JSONException e) {
                    return null;
                }
            }
        }

        return null;

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

        try {
            TextView greetingText = (TextView) findViewById(R.id.textView3);

            String temp = "Hi, " + getUserFirstName();

            greetingText.setText(temp);

        } catch (Exception E) {

        } finally {
            if(!hasDoneInitialFetch) {
                hasDoneInitialFetch = true;

                try {
                    firebase_fetchAll();
                }catch (Exception e) {

                }

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

        //firebase_updateEmailReg();
    }

    public void testButtonReference_onClick(View v) {
        firebase_updateUserReferenceStats(false, true, true, 0.112, 24, new boolean[]{false, false, true, false, false});
        //firebase_sendFriendRequest("jackschedel@gmail.com");

    }

    public void drivingSample_onClick(View v) {
        firebase_fetchGasMileage();
        // THESE ARE TWO SEPERATE FUNCTIONS FOR A REASON. PLEASE CALL FETCH ASAP ON FORM CREATE TO GIVE IT TIME


        firebase_weeklyDrivingUpdate(42.3, gasMileageGlobal);


         // still need update carbon footprint function
    }

    public void elecBillSample_onClick(View v) {
        firebase_fetchKwhPrice();
        // THESE ARE TWO SEPERATE FUNCTIONS FOR A REASON. PLEASE CALL FETCH ASAP ON FORM CREATE TO GIVE IT TIME


        firebase_monthlyElectricityUpdate(12.32, kwhPriceGlobal);


        // still need update carbon footprint function
    }

    public void externalFuelsSample_onClick(View v) {
        // you will want to call fetchExternalFuelBools somewhere on app init so that way you can use externalFuelsBool for your data entry page.
        // externalFuelsBool[i]:
        // 0: naturalGas
        // 1: fuelOil
        // 2: propane
        // true if it should be asked, false if they do not use it.

        firebase_fetchExternalFuelBools();
        // THESE ARE TWO SEPERATE FUNCTIONS FOR A REASON. PLEASE CALL FETCH ASAP ON FORM CREATE TO GIVE IT TIME


        // feel free to put whatever in any unused external fuels.
        // I already have the externalFuelsBool checking in place in the calculator too :)
        firebase_monthlyExternalFuelUpdate(3.0, 2.5, 1.2);


        // still need update carbon footprint function
    }

    public void firebase_fetchAll() {
        firebase_fetchExternalFuelBools();
        firebase_fetchKwhPrice();
        firebase_fetchGasMileage();
        firebase_updateGoogleAccountInfo();
        firebase_updateEmailReg();
        firebase_fetchCarbonFootprintFromBreakdown();

    }

    public void firebase_fetchCarbonFootprintFromBreakdown() {
        db.collection(getUserToken()).document("statistics").collection("nonDated").document("lastBreakdown")
            .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            carbonFootprintGlobal = (double) document.getData().getOrDefault("recycleComp", false);
                            carbonFootprintGlobal += (double) document.getData().getOrDefault("drivingComp", false);
                            carbonFootprintGlobal += (double) document.getData().getOrDefault("elecComp", false);
                            carbonFootprintGlobal += (double) document.getData().getOrDefault("externalComp", false);

                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
    }


    /*
    public String tempFriendsToken = "";

    public void firebase_sendFriendRequest(String email) {

        db.collection("global").document("emails")
            .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            tempFriendsToken = (String) document.getData().getOrDefault(email, "null");

                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });

        Map<String, Object> toInsert = new HashMap<>();
        toInsert.put(getUserEmail(), false);

        // Set the user's drivingComp stat found in:
        // (userTokenNumber listed as sub in Account page json)/statistics/nonDated/lastBreakdown
        db.collection(tempFriendsToken).document("friends")
            .update(toInsert)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "DocumentSnapshot successfully written!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error writing document", e);
                }
            });
    }

     */

    public void firebase_monthlyExternalFuelUpdate(double naturalGasBill, double fuelOilBill, double propaneBill) {


        Map<String, Object> toInsert = new HashMap<>();
        toInsert.put("externalComp", externalFuelsFootprint(0, naturalGasBill, fuelOilBill, propaneBill));

        // Set the user's drivingComp stat found in:
        // (userTokenNumber listed as sub in Account page json)/statistics/nonDated/lastBreakdown
        db.collection(getUserToken()).document("statistics").collection("nonDated").document("lastBreakdown")
            .update(toInsert)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "DocumentSnapshot successfully written!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error writing document", e);
                }
            });
    }

    public void firebase_fetchExternalFuelBools() {
        db.collection(getUserToken()).document("statistics").collection("nonDated").document("referenceStats")
            .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {

                            externalFuelsBoolGlobal[0] = (boolean) document.getData().getOrDefault("naturalGas", false);
                            externalFuelsBoolGlobal[1] = (boolean) document.getData().getOrDefault("fuelOil", false);
                            externalFuelsBoolGlobal[2] = (boolean) document.getData().getOrDefault("propane", false);

                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
    }


    public void firebase_monthlyElectricityUpdate(double priceOfElectricityBill, double kwhPrice) {
        Map<String, Object> toInsert = new HashMap<>();

        if(kwhPrice == 0)
            toInsert.put("elecComp", 0.0);
        else
            toInsert.put("elecComp", houseHoldFootprint(0, 0, (priceOfElectricityBill / kwhPrice), 0, 0));

        // Set the user's drivingComp stat found in:
        // (userTokenNumber listed as sub in Account page json)/statistics/nonDated/lastBreakdown
        db.collection(getUserToken()).document("statistics").collection("nonDated").document("lastBreakdown")
            .update(toInsert)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "DocumentSnapshot successfully written!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error writing document", e);
                }
            });

    }

    public void firebase_pushFootprintData() {
        Map<String, Object> toInsert = new HashMap<>();

        if(carbonFootprintGlobal == 0)
            return;

        String currDate = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(new Date());

        toInsert.put(currDate, carbonFootprintGlobal);

        // Set the user's drivingComp stat found in:
        // (userTokenNumber listed as sub in Account page json)/statistics/nonDated/lastBreakdown
        db.collection(getUserToken()).document("statistics")
            .update(toInsert)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "DocumentSnapshot successfully written!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error writing document", e);
                }
            });
    }

    public void firebase_fetchFriendsData() {

    }

    public void firebase_updateEmailReg() {
        Map<String, Object> toInsert = new HashMap<>();
        toInsert.put(getUserEmail(), getUserToken());

        db.collection("global").document("emails")
            .set(toInsert)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "DocumentSnapshot successfully written!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error writing document", e);
                }
            });
    }

    public void firebase_updateGoogleAccountInfo() {
        Map<String, Object> toInsert = new HashMap<>();
        toInsert.put("email", getUserEmail());
        toInsert.put("image", getUserImageURL());
        toInsert.put("name", getUserFullName());


        // Set the user's reference stats found in:
        // (userTokenNumber listed as sub in Account page json)/statistics/nonDated/referenceStats
        db.collection(getUserToken()).document("account")
            .set(toInsert)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "DocumentSnapshot successfully written!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error writing document", e);
                }
            });
    }


    public void firebase_fetchGasMileage() {
        db.collection(getUserToken()).document("statistics").collection("nonDated").document("referenceStats")
            .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {

                            gasMileageGlobal = (double) document.getData().getOrDefault("gasMileage", 0.0);
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
    }



    public void firebase_fetchKwhPrice() {
        db.collection(getUserToken()).document("statistics").collection("nonDated").document("referenceStats")
            .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {

                            kwhPriceGlobal = (double) document.getData().getOrDefault("kwhPrice", 0.0);
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
    }

    public void firebase_weeklyDrivingUpdate(double milesDrivenThisWeek, double gasMileage) {

        Map<String, Object> toInsert = new HashMap<>();
        toInsert.put("drivingComp", transportFootprint(0, milesDrivenThisWeek, gasMileage));

        // Set the user's drivingComp stat found in:
        // (userTokenNumber listed as sub in Account page json)/statistics/nonDated/lastBreakdown
        db.collection(getUserToken()).document("statistics").collection("nonDated").document("lastBreakdown")
            .update(toInsert)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "DocumentSnapshot successfully written!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error writing document", e);
                }
            });

    }

    public void firebase_updateUserReferenceStats(boolean fuelOil, boolean propane, boolean naturalGas, double kwhPrice, double gasMileage, boolean[] recycleList) {
        Map<String, Object> toInsert = new HashMap<>();
        toInsert.put("fuelOil", fuelOil);
        toInsert.put("propane", propane);
        toInsert.put("naturalGas", naturalGas);
        toInsert.put("kwhPrice", kwhPrice);
        toInsert.put("gasMileage", gasMileage);



        // Set the user's reference stats found in:
        // (userTokenNumber listed as sub in Account page json)/statistics/nonDated/referenceStats
        db.collection(getUserToken()).document("statistics").collection("nonDated").document("referenceStats")
            .set(toInsert)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "DocumentSnapshot successfully written!");
            }
        })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error writing document", e);
                }
            });


        Map<String, Object> toInsert2 = new HashMap<>();
        toInsert2.put("recycleComp", wasteFootprint(0, recycleList));


        db.collection(getUserToken()).document("statistics").collection("nonDated").document("lastBreakdown")
            .update(toInsert2)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "DocumentSnapshot successfully written!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error writing document", e);
                }
            });
    }


    /*public void button6_onClick(View v){
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

        String footPrintString = ""+footPrint;
        ((TextView) findViewById(R.id.textView5)).setText(footPrintString);
        ((TextView) findViewById(R.id.textView5)).setTextColor(Color.GREEN);
        homeScreen.setVisibility(View.VISIBLE);
        questions.setVisibility(View.GONE);
    }*/
    public static double houseHoldFootprint(int zip, double nGasUse, double elecUse, double oilUse, double propUse)
    {
        double totalHouseHoldFPrint = 0; //in tons
        totalHouseHoldFPrint += nGasUse*119.58;
        totalHouseHoldFPrint += elecUse*14.4215172;
        totalHouseHoldFPrint += oilUse*22.61;
        totalHouseHoldFPrint += propUse*12.43;
        return totalHouseHoldFPrint / 2000;
    }
    public static double transportFootprint(int zip, double milesPerWeek, double milesPerGallon) //handles the list outside
    {
        if(milesPerGallon == 0) {
            return 0;
        }

        return (4*milesPerWeek) / milesPerGallon;
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

    public double externalFuelsFootprint(int zip, double nGasUse, double oilUse, double propUse) {
        // TODO: fix to be based on bill price, using average american price.
        if(!externalFuelsBoolGlobal[0])
            nGasUse = 0;

        if(!externalFuelsBoolGlobal[1])
            oilUse = 0;

        if(!externalFuelsBoolGlobal[2])
            oilUse = 0;

        double totalHouseHoldFPrint = 0; //in tons
        totalHouseHoldFPrint += nGasUse*119.58;
        totalHouseHoldFPrint += oilUse*22.61;
        totalHouseHoldFPrint += propUse*12.43;
        return totalHouseHoldFPrint / 2000;
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

    //Notification Channel Method
    private void createNotificationChannel() {
        CharSequence name = "DailyInputReminderChannel";
        String description = "Channel for Daily Input Reminder";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("notifyInputFootprint", name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    //News Article Setup
    private void setUpNewsModels() {
        String[] newsSources = getResources().getStringArray(R.array.news_source);
        String[] articleTitles = getResources().getStringArray(R.array.article_titles);

        for(int i = 0; i < newsSources.length; i++)
        {
            newsArticleModels.add(new NewsArticleModel(articleTitles[i], newsSources[i], newsImages[i]));
        }
    }
}
