package com.tullyapp.tully;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

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
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.tullyapp.tully.Utils.FingerprintHandler;
import com.tullyapp.tully.Utils.PreferenceKeys;
import com.tullyapp.tully.Utils.PreferenceUtil;

import org.json.JSONException;
import org.json.JSONObject;

import static com.tullyapp.tully.Utils.Constants.REQUEST_ONE_TOUCH;
import static com.tullyapp.tully.Utils.Constants.USE_FINGERPRINT_CODE;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Utils.isValidEmail;
import static com.tullyapp.tully.Utils.Utils.showAlert;

public class SignupActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText et_email;
    private EditText et_password;

    private Button btn_signup;
    private Button btn_login;
    private Button btn_fb_login;
    private CallbackManager callbackManager;
    private FirebaseAuth mAuth;
    private LoginButton fb_login_button;
    private ProgressBar progressBar;
    private static final String TAG = LoginActivity.class.getName();
    private DatabaseReference mDatabase;
    private boolean isManualLogin = false;
    private FingerprintHandler fingerprintHandler;
    private Toolbar toolbar;
    private MixpanelAPI mixpanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        initUI();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void initUI(){

        mAuth = FirebaseAuth.getInstance();

        et_email = findViewById(R.id.et_email);
        et_password = findViewById(R.id.et_password);

        btn_login = findViewById(R.id.btn_login);
        btn_fb_login = findViewById(R.id.btn_fb_login);
        btn_fb_login.setOnClickListener(this);

        btn_signup = findViewById(R.id.btn_signup);
        btn_signup.setOnClickListener(this);

        progressBar = findViewById(R.id.progressBar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerprintHandler = new FingerprintHandler(this);
            checkForOneTouch();
        }

        /* --- Facebook Login --- */
        fb_login_button = findViewById(R.id.fb_login_button);
        fb_login_button.setReadPermissions("email");

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
                progressBar.setVisibility(View.GONE);
                Log.e(TAG,error.getMessage());
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken accessToken){
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    PreferenceUtil.getPref(SignupActivity.this).edit().remove(PreferenceKeys.ONE_TOUCH_AUTH).apply();
                    isManualLogin = false;
                    checkinFirebaes();
                } else {
                    progressBar.setVisibility(View.GONE);
                    LoginManager.getInstance().logOut();
                    showAlert(SignupActivity.this,"Oops",task.getException().getMessage());
                }
            }
        }).addOnFailureListener(SignupActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.GONE);
                showAlert(SignupActivity.this,"Oops",e.getMessage());
            }
        });
    }

    public void gotofpwd(View v){
        startActivity(new Intent(SignupActivity.this,ForgotPasswordActivity.class));
    }

    public void gotologin(View v){
        startActivity(new Intent(SignupActivity.this,LoginActivity.class));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_signup:
                onSignupClick();
                break;

            case R.id.btn_fb_login:
                progressBar.setVisibility(View.VISIBLE);
                fb_login_button.performClick();
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mixpanel = MixpanelAPI.getInstance(this, YOUR_PROJECT_TOKEN);

        // Add code to print out the key hash
        /*try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.e("MY KEY HASH:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }*/
    }

    private void onSignupClick(){
        final String email = et_email.getText().toString();
        final String password = et_password.getText().toString();

        if (email.isEmpty())
            popupMsg("Validation Error","Email cannot be empty");
        else if (password.isEmpty())
            popupMsg("Validation Error","Password cannot be empty");
        else if (password.length()<8)
            popupMsg("Validation Error","Your password must be atleast 8 characters");
        else if (!isValidEmail(email))
            popupMsg("Validation Error","Email address seems invalid");
        else{
            progressBar.setVisibility(View.VISIBLE);

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            mixpanel.track("Creating project");

                            JSONObject loginObj = new JSONObject();
                            try {
                                loginObj.put("email",email);
                                loginObj.put("password",password);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            PreferenceUtil.getPref(SignupActivity.this).edit().putString(PreferenceKeys.MANUAL_USER_LOGIN_CREDENTIALS,loginObj.toString()).apply();
                            isManualLogin = true;
                            checkinFirebaes();
                        }
                    }
                }).addOnFailureListener(SignupActivity.this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        showAlert(SignupActivity.this,"Oops",e.getMessage());
                    }
                });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==REQUEST_ONE_TOUCH){
            boolean is_profile_ready = PreferenceUtil.getPref(SignupActivity.this).getBoolean(PreferenceKeys.IS_ARTIST_PROFILE_READY,false);
            if (is_profile_ready){
                gotoHome();
            }
            else{
                gotoArtistCreation();
            }
        }
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void checkinFirebaes(){
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid()).child("profile");
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    PreferenceUtil.getPref(SignupActivity.this).edit().putBoolean(PreferenceKeys.IS_ARTIST_PROFILE_READY,true).apply();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isManualLogin && checkForOneTouch()) {
                        gotoOneTouch();
                    } else{
                        gotoHome();
                    }
                }
                else{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isManualLogin && checkForOneTouch()) {
                        gotoOneTouch();
                    }
                    else{
                        gotoArtistCreation();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Log.e("Err",databaseError.getMessage());
            }
        });
    }

    private boolean checkForOneTouch(){
        if (ActivityCompat.checkSelfPermission(SignupActivity.this, android.Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SignupActivity.this, new String[]{android.Manifest.permission.USE_FINGERPRINT}, USE_FINGERPRINT_CODE);
        }else{
            boolean isFingerPrintHardWareAvailable = false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                isFingerPrintHardWareAvailable = FingerprintHandler.fingerPrintisHardwareDetected(SignupActivity.this);
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
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    checkForOneTouch();
                }else{
                    gotoHome();
                }
                break;
        }
    }

    private void popupMsg(String title, String msg){
        showAlert(SignupActivity.this,title,msg);
    }

    private void gotoHome(){
        Intent intent = new Intent(SignupActivity.this,HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(intent);
        SignupActivity.this.finish();
    }

    private void gotoArtistCreation(){
        Intent intent = new Intent(SignupActivity.this,ArtistCreationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        SignupActivity.this.finish();
    }

    private void gotoOneTouch(){
        Intent intent = new Intent(SignupActivity.this,OneTouchLoginActivity.class);
        startActivityForResult(intent,REQUEST_ONE_TOUCH);
    }
}
