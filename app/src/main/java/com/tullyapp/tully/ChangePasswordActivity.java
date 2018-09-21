package com.tullyapp.tully;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

public class ChangePasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_update;
    private EditText et_confirm_pwd, et_new_pwd, et_old_pwd;

    private FirebaseAuth mAuth;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Edit Profile");
        mAuth = FirebaseAuth.getInstance();

        initUI();
    }

    private void initUI(){

        progressBar = findViewById(R.id.progressBar);

        btn_update = findViewById(R.id.btn_update);

        et_confirm_pwd = findViewById(R.id.et_confirm_pwd);
        et_new_pwd = findViewById(R.id.et_new_pwd);
        et_old_pwd = findViewById(R.id.et_old_pwd);

        btn_update.setOnClickListener(this);
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
            case R.id.btn_update:
                updatePassword();
                break;
        }
    }

    private void updatePassword(){
        FirebaseUser currentUser = mAuth.getCurrentUser();
        boolean hasPasswordLogin = false;
        String email = "";
        if (currentUser!=null){
            for (UserInfo profile : currentUser.getProviderData()) {
                String providerId = profile.getProviderId();
                if (providerId.equals("firebase")){
                    hasPasswordLogin = true;
                    email = profile.getEmail();
                }
            }

            if (hasPasswordLogin && email!=null && !email.isEmpty()){
                final String oldPwd = et_old_pwd.getText().toString().trim();
                final String newPWd = et_new_pwd.getText().toString().trim();
                final String confirmPwd = et_confirm_pwd.getText().toString().trim();

                if (oldPwd.isEmpty()){
                    Toast.makeText(this, "Old Passsword is required", Toast.LENGTH_SHORT).show();
                }
                else if (newPWd.isEmpty()){
                    Toast.makeText(this, "New Password cannot be empty", Toast.LENGTH_SHORT).show();
                }
                else if (confirmPwd.isEmpty()){
                    Toast.makeText(this, "Confirm password cannot be empty", Toast.LENGTH_SHORT).show();
                }
                else if (newPWd.length()<8){
                    Toast.makeText(this, "Minimum password length is 8 Charaters", Toast.LENGTH_SHORT).show();
                }
                else if (!newPWd.equals(confirmPwd)){
                    Toast.makeText(this, "Confirm Password Missmatch with new Password", Toast.LENGTH_SHORT).show();
                }
                else{
                    progressBar.setVisibility(View.VISIBLE);
                    AuthCredential authCredentials = EmailAuthProvider.getCredential(email,oldPwd);
                    mAuth.getCurrentUser().reauthenticate(authCredentials).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            progressBar.setVisibility(View.GONE);
                            mAuth.getCurrentUser().updatePassword(newPWd);
                            Toast.makeText(ChangePasswordActivity.this, "Password Updated", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ChangePasswordActivity.this, "Old Password Missmatch", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }else{
                Toast.makeText(this, "You have not yet setup email password login", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
