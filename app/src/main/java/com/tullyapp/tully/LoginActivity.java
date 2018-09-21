package com.tullyapp.tully;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tullyapp.tully.Utils.Configuration;
import com.tullyapp.tully.Utils.FingerprintHandler;
import com.tullyapp.tully.Utils.PreferenceKeys;
import com.tullyapp.tully.Utils.PreferenceUtil;

import org.json.JSONException;
import org.json.JSONObject;

import static com.tullyapp.tully.Utils.Constants.REQUEST_ONE_TOUCH;
import static com.tullyapp.tully.Utils.Constants.TUTS_HOME;
import static com.tullyapp.tully.Utils.Constants.TUTS_LYRICS;
import static com.tullyapp.tully.Utils.Constants.TUTS_MARKET_PLACE;
import static com.tullyapp.tully.Utils.Constants.TUTS_PLAY;
import static com.tullyapp.tully.Utils.Constants.TUTS_RECORDING;
import static com.tullyapp.tully.Utils.Constants.USE_FINGERPRINT_CODE;
import static com.tullyapp.tully.Utils.Utils.isValidEmail;
import static com.tullyapp.tully.Utils.Utils.showAlert;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener, FingerprintHandler.OnResponseInterface {

    private static final int INITIATE_TOUCH = 987;
    private static final String TAG = LoginActivity.class.getSimpleName();
    private LoginButton fb_login_button;
    private CallbackManager callbackManager;
    private FirebaseAuth mAuth;
    private Button btn_fb_login;
    private Button btn_login;
    private ProgressBar progressBar;

    private TextView tv_terms, tv_privacy_policy;

    private EditText et_email;
    private EditText et_password;
    private DatabaseReference mDatabase;
    private boolean isManualLogin = false;
    private String oneTouchAuth;
    private FingerprintHandler fingerprintHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initUI();
    }

    private void initUI(){

        mAuth = FirebaseAuth.getInstance();

        progressBar = findViewById(R.id.progressBar);
        btn_login = findViewById(R.id.btn_login);

        et_email = findViewById(R.id.et_email);
        et_password = findViewById(R.id.et_password);

        tv_terms = findViewById(R.id.tv_terms);
        tv_privacy_policy = findViewById(R.id.tv_privacy_policy);

        tv_terms.setOnClickListener(this);
        tv_privacy_policy.setOnClickListener(this);

        btn_login.setOnClickListener(this);

        btn_fb_login = findViewById(R.id.btn_fb_login);
        btn_fb_login.setOnClickListener(this);

        fb_login_button = findViewById(R.id.fb_login_button);
        fb_login_button.setReadPermissions("email");

        try{
            PreferenceUtil.getPref(this).edit().remove(PreferenceKeys.ID_TOKEN).apply();
            PreferenceUtil.getPref(this).edit().remove(PreferenceKeys.ID_TOKEN_EXPIRATION).apply();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        callbackManager = CallbackManager.Factory.create();
        fb_login_button.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(TAG,error.getMessage());
                progressBar.setVisibility(View.GONE);
            }
        });

        oneTouchAuth = PreferenceUtil.getPref(this).getString(PreferenceKeys.ONE_TOUCH_AUTH,"");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerprintHandler = new FingerprintHandler(this);
            if (!oneTouchAuth.isEmpty()){
                fingerprintHandler.setOnResponseListener(this);
                fingerprintHandler.startFingerPrintAuth();
            }
        }
    }


    private void handleFacebookAccessToken(AccessToken accessToken){
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    PreferenceUtil.getPref(LoginActivity.this).edit().remove(PreferenceKeys.ONE_TOUCH_AUTH).apply();
                    checkinFirebaes();
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    LoginManager.getInstance().logOut();
                    showAlert(LoginActivity.this,"Oops",task.getException().getMessage());
                }
            }
        }).addOnFailureListener(LoginActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.GONE);
                showAlert(LoginActivity.this,"Oops",e.getMessage());
            }
        });
    }

    public void gotosignup(View v){
        Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()){
            case R.id.btn_fb_login:
                progressBar.setVisibility(View.VISIBLE);
                fb_login_button.performClick();
                break;

            case R.id.btn_login:
                final String email = et_email.getText().toString();
                final String password = et_password.getText().toString();

                if (email.isEmpty())
                    popupMsg("Validation Error","Email cannot be empty");
                else if (password.isEmpty())
                    popupMsg("Validation Error","Password cannot be empty");
                else if (!isValidEmail(email))
                    popupMsg("Validation Error","Email address seems invalid");
                else{
                    signInWithEmailAndPassword(email,password);
                }
                break;

            case R.id.tv_terms:
                intent = new Intent(this,ContentView.class);
                intent.putExtra("NAME","Terms of Service");
                intent.putExtra("URL","https://drive.google.com/file/d/0B52VwE7cG-_wblYyTEFUWC1oTVk/view?usp=sharing");
                startActivity(intent);
                break;

            case R.id.tv_privacy_policy:
                intent = new Intent(this,ContentView.class);
                intent.putExtra("NAME","Privacy Policy");
                intent.putExtra("URL","https://drive.google.com/file/d/0B52VwE7cG-_wQ2NoV1NtbEN5ekU/view?usp=sharing");
                startActivity(intent);
                break;
        }
    }

    public void gotofpwd(View v){
        startActivity(new Intent(LoginActivity.this,ForgotPasswordActivity.class));
    }

    private void signInWithEmailAndPassword(final String email, final String password) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email,password)
        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    isManualLogin = true;
                    JSONObject loginObj = new JSONObject();
                    try {
                        loginObj.put("email",email);
                        loginObj.put("password",password);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String manualAuth = PreferenceUtil.getPref(LoginActivity.this).getString(PreferenceKeys.MANUAL_USER_LOGIN_CREDENTIALS, "");
                    PreferenceUtil.getPref(LoginActivity.this).edit().putString(PreferenceKeys.MANUAL_USER_LOGIN_CREDENTIALS,loginObj.toString()).apply();
                    if (manualAuth.isEmpty()){
                        oneTouchAuth="";
                    }
                    else if (!oneTouchAuth.isEmpty()){
                        try {
                            JSONObject mauth = new JSONObject(manualAuth);
                            JSONObject oauth = new JSONObject(oneTouchAuth);
                            if (!mauth.getString("email").equals(oauth.getString("email")) || !mauth.getString("password").equals(oauth.getString("password"))){
                                oneTouchAuth="";
                            }
                        } catch (JSONException e) {
                            oneTouchAuth="";
                            e.printStackTrace();
                        }
                    }
                    checkinFirebaes();
                }
            }
        }).addOnFailureListener(LoginActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.GONE);
                showAlert(LoginActivity.this,"Oops",e.getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==REQUEST_ONE_TOUCH){
            boolean is_profile_ready = PreferenceUtil.getPref(LoginActivity.this).getBoolean(PreferenceKeys.IS_ARTIST_PROFILE_READY,false);
            Log.e("ISREADY",is_profile_ready+"");
            if (is_profile_ready){
                gotoHome();
            }
            else{
                gotoArtistCreation();
            }
        }
        else{
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void checkinFirebaes(){
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        getConfiguration();
    }

    private void checkProfile(){
        mDatabase.child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    PreferenceUtil.getPref(LoginActivity.this).edit().putBoolean(PreferenceKeys.IS_ARTIST_PROFILE_READY,true).apply();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isManualLogin && oneTouchAuth.isEmpty() && checkForOneTouch()) {
                        gotoOneTouch();
                    }
                    else{
                        gotoHome();
                    }
                }
                else if (isManualLogin && oneTouchAuth.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkForOneTouch()) {
                    gotoOneTouch();
                }
                else{
                    gotoArtistCreation();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.VISIBLE);
                Log.e("Err",databaseError.getMessage());
            }
        });
    }

    private void getConfiguration(){
        mDatabase.child("settings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){

                    if (dataSnapshot.child("marketPlace").exists()){
                        Configuration.market_watch = (boolean) dataSnapshot.child("marketPlace").getValue();
                    }
                    if (dataSnapshot.child("pushNotification").exists()){
                        Configuration.pushNotification = (boolean) dataSnapshot.child("pushNotification").getValue();
                    }

                    final String TUTS_SCREEN = "tutorial_screens";

                    if (dataSnapshot.child(TUTS_SCREEN).exists()){
                        String[] tutorial_screens = {TUTS_HOME, TUTS_LYRICS, TUTS_PLAY, TUTS_RECORDING, TUTS_MARKET_PLACE};
                        for (String screen : tutorial_screens){
                            if (dataSnapshot.child(TUTS_SCREEN).child(screen).exists()){
                                boolean boo = (boolean) dataSnapshot.child(TUTS_SCREEN).child(screen).getValue();
                                switch (screen){
                                    case TUTS_HOME:
                                        Configuration.home_tuts = boo;
                                        break;

                                    case TUTS_LYRICS:
                                        Configuration.lyrics_tuts = boo;
                                        break;

                                    case TUTS_PLAY:
                                        Configuration.play_tuts = boo;
                                        break;

                                    case TUTS_MARKET_PLACE:
                                        Configuration.marketplace_tuts = boo;
                                        break;

                                    case TUTS_RECORDING:
                                        Configuration.recording_tuts = boo;
                                        break;
                                }
                            }
                        }
                    }

                    checkProfile();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                checkProfile();
            }
        });
    }

    private boolean checkForOneTouch(){
        if (ActivityCompat.checkSelfPermission(LoginActivity.this, android.Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{android.Manifest.permission.USE_FINGERPRINT}, USE_FINGERPRINT_CODE);
        }else{
            boolean isFingerPrintHardWareAvailable = false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                isFingerPrintHardWareAvailable = FingerprintHandler.fingerPrintisHardwareDetected(LoginActivity.this);
            }
            return isFingerPrintHardWareAvailable;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case USE_FINGERPRINT_CODE:
                checkForOneTouch();
                break;

            case INITIATE_TOUCH:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        fingerprintHandler.startFingerPrintAuth();
                    }
                }
                break;
        }
    }

    private void gotoHome(){
        Intent intent = new Intent(LoginActivity.this,HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(intent);
        LoginActivity.this.finish();
    }

    private void gotoArtistCreation(){
        Intent intent = new Intent(LoginActivity.this,ArtistCreationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        LoginActivity.this.finish();
    }

    private void gotoOneTouch(){
        Intent intent = new Intent(LoginActivity.this,OneTouchLoginActivity.class);
        startActivityForResult(intent,REQUEST_ONE_TOUCH);
    }

    private void popupMsg(String title,String msg){
        showAlert(LoginActivity.this,title,msg);
    }

    @Override
    public void onResponse(String message, boolean status) {
        if (status){
            try {
                JSONObject auth = new JSONObject(oneTouchAuth);
                signInWithEmailAndPassword(auth.getString("email"), auth.getString("password"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else{
            Toast.makeText(this, message+" : You may Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void initiated(boolean boo) {
        et_password.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_onetouch,0);
    }
}
