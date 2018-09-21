package com.tullyapp.tully;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;
import com.tullyapp.tully.Utils.FingerprintHandler;
import com.tullyapp.tully.Utils.PreferenceKeys;
import com.tullyapp.tully.Utils.PreferenceUtil;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import static com.tullyapp.tully.Utils.Constants.USE_FINGERPRINT_CODE;

public class OneTouchLoginActivity extends AppCompatActivity implements FingerprintHandler.OnResponseInterface, View.OnClickListener {

    private KeyStore keyStore;
    private static final String KEY_NAME = "TullyApp";
    private Cipher cipher;
    private KeyguardManager keyguardManager;
    private FingerprintManager fingerprintManager;
    private Button btn_onetouch;
    private boolean initiated = false;
    private Intent returnIntent;
    private FirebaseAuth mAuth;
    private boolean manualAvailable = false;
    private String auth;
    private boolean authenticated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one_touch_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        // Initializing both Android Keyguard Manager and Fingerprint Manager
        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
        btn_onetouch = findViewById(R.id.btn_onetouch);
        btn_onetouch.setOnClickListener(this);

        returnIntent = new Intent();

        auth = PreferenceUtil.getPref(OneTouchLoginActivity.this).getString(PreferenceKeys.MANUAL_USER_LOGIN_CREDENTIALS,null);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser()!=null){
            for (UserInfo providerId : mAuth.getCurrentUser().getProviderData()){
                if (providerId.getProviderId().equals("password")){
                    manualAvailable = true;
                }
            }

            if (manualAvailable && auth!=null){
                startFingerPrintAuth();
            }
            else{
                Toast.makeText(this, "Email and Password login is not configured", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(OneTouchLoginActivity.this,LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            startActivity(intent);
            this.finish();
        }
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

    private void startFingerPrintAuth(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.USE_FINGERPRINT}, USE_FINGERPRINT_CODE);
            }
            else{
                if (fingerprintManager.isHardwareDetected()) {
                    if (!fingerprintManager.hasEnrolledFingerprints()) {
                        Toast.makeText(this, "Register at least one fingerprint in Settings", Toast.LENGTH_SHORT).show();
                        onBackPressed();
                    }else{
                        // Checks whether lock screen security is enabled or not
                        if (!keyguardManager.isKeyguardSecure()) {
                            Toast.makeText(this, "Lock screen security not enabled in Settings", Toast.LENGTH_SHORT).show();
                        }else{
                            generateKey();
                            if (cipherInit()) {
                                initiated = true;
                                FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(cipher);
                                FingerprintHandler helper = new FingerprintHandler(this);
                                helper.setOnResponseListener(this);
                                helper.startAuth(fingerprintManager, cryptoObject);
                            }
                        }
                    }
                }
            }
        }
    }



    @TargetApi(Build.VERSION_CODES.M)
    protected void generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (Exception e) {
            e.printStackTrace();
        }


        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get KeyGenerator instance", e);
        }


        try {
            keyStore.load(null);
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException |
                InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    public boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }


        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    @Override
    public void onResponse(String message, boolean status) {
        Log.e("STATUS",status+"");
        try{
            if (status){
                authenticated = true;
                PreferenceUtil.getPref(OneTouchLoginActivity.this).edit().putString(PreferenceKeys.ONE_TOUCH_AUTH,auth).apply();
                //PreferenceUtil.getPref(OneTouchLoginActivity.this).edit().remove(PreferenceKeys.MANUAL_USER_LOGIN_CREDENTIALS).apply();
                Toast.makeText(this, "One Touch Login is setup", Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
            else{
                authenticated = false;
                Toast.makeText(this, message+" : You may Try again", Toast.LENGTH_SHORT).show();
                initiated = false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void initiated(boolean boo) {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (authenticated){
            setResult(Activity.RESULT_OK,returnIntent);
        }
        else{
            setResult(Activity.RESULT_CANCELED,returnIntent);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_onetouch:
                if (!initiated && manualAvailable && auth!=null)
                    startFingerPrintAuth();
                break;
        }
    }
}
