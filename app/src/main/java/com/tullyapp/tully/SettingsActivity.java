package com.tullyapp.tully;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.tullyapp.tully.Analyzer.AnalyzeSubscriptionDialogFragment;
import com.tullyapp.tully.Engineer.EngineerAccessActivity;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.Configuration;
import com.tullyapp.tully.Utils.Constants;
import com.tullyapp.tully.Utils.PreferenceKeys;
import com.tullyapp.tully.Utils.PreferenceUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;
import io.intercom.android.sdk.Intercom;

import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_INVITE_ENGINEER;
import static com.tullyapp.tully.Utils.Utils.logOutSequence;
import static com.tullyapp.tully.Utils.ViewUtils.fullScreenTextDialog;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, AnalyzeSubscriptionDialogFragment.SubscriptionEvents {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    private Button btn_logout;
    private Button btn_popup_cancel;
    private FirebaseAuth mAuth;
    private Dialog dialog;
    private RelativeLayout rate_us, privacy_policy, editprofile, change_pwd, set_onetouch, terms_conditions, rl_help, engineer_access, rl_about_us;
    private Intent intent;
    private SwitchCompat push_switch, audioAnalyzerSwitch;
    private DatabaseReference mDbRef;
    private Button btn_rate;
    private Dialog about_dialog;
    private ImageView btn_close;
    private String subscription_id = null;
    private AnalyzeSubscriptionDialogFragment analyzeSubscriptionDialogFragment;
    private ProgressBar progressBar;
    private boolean subscribed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Settings");
        mAuth = FirebaseAuth.getInstance();
        initUI();
    }

    private void initUI(){
        btn_logout = findViewById(R.id.btn_logout);
        rate_us = findViewById(R.id.rate_us);
        privacy_policy = findViewById(R.id.privacy_policy);
        editprofile = findViewById(R.id.editprofile);
        change_pwd = findViewById(R.id.change_pwd);
        set_onetouch = findViewById(R.id.set_onetouch);
        terms_conditions = findViewById(R.id.terms_conditions);
        rl_help = findViewById(R.id.rl_help);
        engineer_access = findViewById(R.id.engineer_access);
        push_switch = findViewById(R.id.push_switch);
        audioAnalyzerSwitch = findViewById(R.id.audioAnalyzerSwitch);
        rl_about_us = findViewById(R.id.rl_about_us);
        //engineer_access.setVisibility(View.GONE);
        progressBar = findViewById(R.id.progressBar);
        about_dialog = fullScreenTextDialog(this);
        btn_close = about_dialog.findViewById(R.id.btn_close);

        dialog = new Dialog(SettingsActivity.this, R.style.MyDialogTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.rate_us_popup);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        btn_rate = dialog.findViewById(R.id.btn_rate);
        btn_popup_cancel = dialog.findViewById(R.id.btn_popup_cancel);

        btn_logout.setOnClickListener(this);
        btn_popup_cancel.setOnClickListener(this);
        rate_us.setOnClickListener(this);
        privacy_policy.setOnClickListener(this);
        editprofile.setOnClickListener(this);
        change_pwd.setOnClickListener(this);
        set_onetouch.setOnClickListener(this);
        terms_conditions.setOnClickListener(this);
        rl_help.setOnClickListener(this);
        engineer_access.setOnClickListener(this);
        btn_rate.setOnClickListener(this);
        rl_about_us.setOnClickListener(this);
        btn_close.setOnClickListener(this);

        mDbRef = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        push_switch.setChecked(Configuration.pushNotification);
        checkAudioAnalyzerSubscription();

        audioAnalyzerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked){
                if (!subscribed){
                    buttonView.setChecked(false);
                    analyzeSubscriptionDialogFragment = AnalyzeSubscriptionDialogFragment.newInstance();
                    analyzeSubscriptionDialogFragment.setSubscriptionEvents(SettingsActivity.this);
                    analyzeSubscriptionDialogFragment.show(getSupportFragmentManager(),AnalyzeSubscriptionDialogFragment.class.getSimpleName());
                }
            }
            else{
                if (subscription_id!=null && subscribed){
                    buttonView.setChecked(true);
                    unsubscribeConfirmation();
                }
            }
            }
        });

        push_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mDbRef.child("settings").child("pushNotification").setValue(isChecked);
            Configuration.pushNotification = isChecked;
            }
        });
    }


    private void unsubscribeConfirmation(){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        builder.setTitle("Unsubscribe ?")
        .setMessage("You will not be able to use Audio Analyzer")
        .setPositiveButton(R.string.unsubscribe, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                progressBar.setVisibility(View.VISIBLE);
                authIdToken(subscription_id);
            }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
        .show();
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

    private void authIdToken(final String subscription_id){
        boolean valid = true;
        final String idToken = PreferenceUtil.getPref(this).getString(PreferenceKeys.ID_TOKEN,"");
        long timestamp = PreferenceUtil.getPref(this).getLong(PreferenceKeys.ID_TOKEN_EXPIRATION,-1);
        if (idToken.isEmpty()){
            valid = false;
        }
        else{
            long diff = timestamp - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
            if (diff<100){
                valid = false;
            }
        }


        if (valid){
            cancelSubscription(idToken,subscription_id);
        }
        else{
            if (mAuth.getCurrentUser()!=null){
                mAuth.getCurrentUser().getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                    if (task.isSuccessful()){
                        PreferenceUtil.getPref(SettingsActivity.this).edit().putString(PreferenceKeys.ID_TOKEN,task.getResult().getToken()).apply();
                        PreferenceUtil.getPref(SettingsActivity.this).edit().putLong(PreferenceKeys.ID_TOKEN_EXPIRATION,task.getResult().getExpirationTimestamp()).apply();
                        cancelSubscription(task.getResult().getToken(),subscription_id);
                    }
                    }
                });
            }
        }
    }

    private void cancelSubscription(String authToken, final String subscription_id){
        AsyncHttpClient client = new AsyncHttpClient();
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            client.setSSLSocketFactory(sf);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        RequestParams params = new RequestParams();
        params.put("subscription_id",subscription_id);
        client.addHeader(Constants.Authorization,authToken);
        client.post(APIs.CANCEL_AUDIO_ANALYZER_SUBSCRIPTION, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                progressBar.setVisibility(View.GONE);
                String responseString = new String(bytes);
                Log.e(TAG,responseString);
                try {
                    JSONObject response = new JSONObject(responseString);
                    if (response.getInt("status")==1){
                        subscribed = false;
                        SettingsActivity.this.subscription_id = null;
                        audioAnalyzerSwitch.setChecked(false);
                    }
                    else{
                        Toast.makeText(getApplicationContext(), response.getString("msg"), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_logout:
                logOutSequence(this,mAuth);
                break;

            case R.id.rate_us:
                dialog.show();
                break;

            case R.id.rl_about_us:
                about_dialog.show();
                break;

            case R.id.btn_close:
                about_dialog.dismiss();
                break;

            case R.id.btn_rate:
                Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=com.tullyapp.tully"));
                startActivity(viewIntent);
                break;

            case R.id.btn_popup_cancel:
                dialog.dismiss();
                break;

            case R.id.privacy_policy:
                intent = new Intent(this,ContentView.class);
                intent.putExtra("NAME","Privacy Policy");
                intent.putExtra("URL","https://drive.google.com/uc?id=0B52VwE7cG-_wQ2NoV1NtbEN5ekU");
                startActivity(intent);
                break;

            case R.id.editprofile:
                intent = new Intent(this,EditProfileActivity.class);
                startActivity(intent);
                break;

            case R.id.change_pwd:
                intent = new Intent(this,ChangePasswordActivity.class);
                startActivity(intent);
                break;

            case R.id.set_onetouch:
                intent = new Intent(this,SwitchTouchSignInActivity.class);
                startActivity(intent);
                break;

            case R.id.terms_conditions:
                intent = new Intent(this,ContentView.class);
                intent.putExtra("NAME","Terms of Service");
                intent.putExtra("URL","https://drive.google.com/file/d/0B52VwE7cG-_wblYyTEFUWC1oTVk/view?usp=sharing");
                startActivity(intent);
                break;

            case R.id.rl_help:
                Intercom.client().displayMessenger();
                break;

            case R.id.engineer_access:
                intent = new Intent(this,EngineerAccessActivity.class);
                intent.putExtra(INTENT_PARAM_INVITE_ENGINEER,false);
                startActivity(intent);
                break;
        }
    }

    private void checkAudioAnalyzerSubscription(){
        mDbRef.child("settings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try{
                    if (dataSnapshot.exists()){
                        if (dataSnapshot.hasChild("audioAnalyzer") && dataSnapshot.child("audioAnalyzer").hasChild("isActive")){
                            subscribed = (boolean) dataSnapshot.child("audioAnalyzer").child("isActive").getValue();
                            subscription_id = (String) dataSnapshot.child("audioAnalyzer").child("subscriptionId").getValue();
                            if (subscribed){
                                audioAnalyzerSwitch.setChecked(true);
                            }
                            else{
                                audioAnalyzerSwitch.setChecked(false);
                            }
                        }
                        else{
                            audioAnalyzerSwitch.setChecked(false);
                        }
                    }
                    else{
                        audioAnalyzerSwitch.setChecked(false);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                audioAnalyzerSwitch.setChecked(false);
            }
        });
    }

    @Override
    public void onSucessfullSubscription() {
        subscribed = true;
        checkAudioAnalyzerSubscription();
    }
}