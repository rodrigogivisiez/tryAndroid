package com.tullyapp.tully;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tullyapp.tully.Utils.ExtendedNumberPicker;

import static com.tullyapp.tully.Utils.Utils.logOutSequence;
import static com.tullyapp.tully.Utils.Utils.showAlert;

public class EditProfileActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView fbbtn, btn_save_artist_name, btn_save_email;

    private EditText et_name, et_email, et_popup_email, et_popup_pwd;

    private TextView tv_name, tv_email, tv_fbname, tv_genre;

    private Button btn_popup_login, btn_popup_cancel, btn_save;

    private LoginButton fb_login_button;
    private CallbackManager callbackManager;

    private ExtendedNumberPicker numberPicker;

    private FirebaseAuth mAuth;

    private ProgressBar progressBar;
    private Dialog dialog, genereDialog;
    private String[] genere;
    private RelativeLayout rl_genre;
    private String selectedGenere;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Edit Profile");
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser()!=null){
            initUI();
        }
        else{
            logOutSequence(this,mAuth);
        }
    }

    private void initUI(){
        progressBar = findViewById(R.id.progressBar);
        tv_genre = findViewById(R.id.tv_genre);

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
            }
        });

        rl_genre = findViewById(R.id.rl_genre);
        rl_genre.setOnClickListener(this);

        genereDialog = new Dialog(EditProfileActivity.this, R.style.MyDialogTheme);
        genereDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        genereDialog.setContentView(R.layout.genere_selection);
        genereDialog.setCancelable(true);
        genereDialog.setCanceledOnTouchOutside(true);

        numberPicker = genereDialog.findViewById(R.id.numberPicker);
        btn_save = genereDialog.findViewById(R.id.btn_save);

        btn_save.setOnClickListener(this);

        genere = getResources().getStringArray(R.array.genere);

        numberPicker.setDisplayedValues( null );
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(genere.length-1);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setDisplayedValues( genere );


        dialog = new Dialog(EditProfileActivity.this, R.style.MyDialogTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.reauth_popup);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        btn_popup_login = dialog.findViewById(R.id.btn_popup_login);
        btn_popup_cancel = dialog.findViewById(R.id.btn_popup_cancel);
        et_popup_email = dialog.findViewById(R.id.et_popup_email);
        et_popup_pwd = dialog.findViewById(R.id.et_popup_pwd);

        fbbtn = findViewById(R.id.fbbtn);
        et_name = findViewById(R.id.et_name);
        et_email = findViewById(R.id.et_email);

        btn_save_artist_name = findViewById(R.id.btn_save_artist_name);
        btn_save_email = findViewById(R.id.btn_save_email);

        tv_name = findViewById(R.id.tv_name);
        tv_email = findViewById(R.id.tv_email);
        tv_fbname = findViewById(R.id.tv_fbname);

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            updateFacebook();
            tv_email.setText(user.getEmail());
            et_email.setText(user.getEmail());
            tv_name.setText(user.getDisplayName());
            et_name.setText(user.getDisplayName());
        }

        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());

        fbbtn.setOnClickListener(this);
        tv_name.setOnClickListener(this);
        tv_email.setOnClickListener(this);
        et_email.setOnClickListener(this);
        tv_fbname.setOnClickListener(this);

        btn_save_artist_name.setOnClickListener(this);
        btn_save_email.setOnClickListener(this);

        btn_popup_login.setOnClickListener(this);
        btn_popup_cancel.setOnClickListener(this);

        getGenere();
    }

    private void updateFacebook(){
        for (UserInfo profile : mAuth.getCurrentUser().getProviderData()) {
            String providerId = profile.getProviderId();
            if (providerId.equals("facebook.com")){
                tv_fbname.setText(profile.getDisplayName());
            }
        }
    }

    private void handleFacebookAccessToken(AccessToken accessToken){
        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        mAuth.getCurrentUser().linkWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        updateFacebook();
                    }
                }
            }).addOnFailureListener(EditProfileActivity.this, new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
                LoginManager.getInstance().logOut();
                progressBar.setVisibility(View.GONE);
                showAlert(EditProfileActivity.this,"Oops",e.getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        progressBar.setVisibility(View.VISIBLE);
        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fbbtn:
                fb_login_button.performClick();
                break;

            case R.id.tv_name:
                btn_save_artist_name.setVisibility(View.VISIBLE);
                tv_name.setVisibility(View.INVISIBLE);
                et_name.setVisibility(View.VISIBLE);
                break;

            case R.id.tv_email:
                btn_save_email.setVisibility(View.VISIBLE);
                tv_email.setVisibility(View.INVISIBLE);
                et_email.setVisibility(View.VISIBLE);
                break;

            case R.id.btn_save_artist_name:
                saveName();
                break;

            case R.id.btn_save_email:
                saveEmail();
                break;

            case R.id.btn_popup_login:
                reAuth();
                break;

            case R.id.btn_popup_cancel:
                dialog.dismiss();
                break;

            case R.id.btn_save:
                selectedGenere = genere[numberPicker.getValue()];
                genereDialog.dismiss();
                setGenere();
                break;

            case R.id.rl_genre:
                genereDialog.show();
                break;
        }
    }

    private void reAuth(){
        String email = et_popup_email.getText().toString();
        String password = et_popup_pwd.getText().toString();

        try{
            if (!email.isEmpty() && !password.isEmpty()){
                if (mAuth.getCurrentUser()!=null){
                    AuthCredential credential = EmailAuthProvider.getCredential(email, password);
                    mAuth.getCurrentUser().reauthenticate(credential).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            updateEmail();
                            dialog.dismiss();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(EditProfileActivity.this, "Failed Authenticating", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            else{
                Toast.makeText(this, "Not Registered with Email and Password", Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void getGenere(){
        mDatabase.child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    try{
                        String g = (String) dataSnapshot.child("genre").getValue();
                        tv_genre.setText(g);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setGenere(){
        tv_genre.setText(selectedGenere);
        mDatabase.child("profile").child("genre").setValue(selectedGenere);
    }



    private void saveName(){
        progressBar.setVisibility(View.VISIBLE);
        String name = et_name.getText().toString().trim();
        if (!name.isEmpty()){

            if (mAuth.getCurrentUser()!=null){
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(name).build();

                mAuth.getCurrentUser().updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(EditProfileActivity.this, "Name was updated", Toast.LENGTH_SHORT).show();
                            tv_name.setText(et_name.getText().toString());
                        }
                        else{
                            Toast.makeText(EditProfileActivity.this, "Failed to update", Toast.LENGTH_SHORT).show();
                            et_name.setText(tv_name.getText().toString());
                        }
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        }else{
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            et_name.setText(tv_name.getText().toString());
            progressBar.setVisibility(View.GONE);
        }

        btn_save_artist_name.setVisibility(View.INVISIBLE);
        et_name.setVisibility(View.INVISIBLE);
        tv_name.setVisibility(View.VISIBLE);
    }

    private void saveEmail(){
        progressBar.setVisibility(View.VISIBLE);
        String email = et_email.getText().toString().trim();
        if (!email.isEmpty()){
            if (mAuth.getCurrentUser()!=null){
                et_popup_email.setText("");
                et_popup_pwd.setText("");
                dialog.show();
            }
        }else{
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            et_email.setText(tv_email.getText().toString());
            progressBar.setVisibility(View.GONE);
        }

        btn_save_email.setVisibility(View.INVISIBLE);
        et_email.setVisibility(View.INVISIBLE);
        tv_email.setVisibility(View.VISIBLE);
    }

    private void updateEmail(){
        String email = et_email.getText().toString().trim();
        mAuth.getCurrentUser().updateEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(EditProfileActivity.this, "Email was updated", Toast.LENGTH_SHORT).show();
                tv_email.setText(et_email.getText().toString());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("Err",e.getMessage());
                Toast.makeText(EditProfileActivity.this, "Failed to update", Toast.LENGTH_SHORT).show();
                et_email.setText(tv_email.getText().toString());
            }
        });
    }
}
