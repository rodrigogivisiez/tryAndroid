package com.tullyapp.tully;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import static com.tullyapp.tully.Utils.Utils.isValidEmail;
import static com.tullyapp.tully.Utils.Utils.showAlert;
import static com.tullyapp.tully.Utils.Utils.showMessage;

public class ForgotPasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private Toolbar toolbar;

    private EditText et_email;
    private Button btn_send;
    private FirebaseAuth mAuth;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        initUI();
    }

    private void initUI(){
        btn_send = findViewById(R.id.btn_send);
        et_email = findViewById(R.id.et_email);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();

        btn_send.setOnClickListener(this);
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
            case R.id.btn_send:
                sendPasswordResetEmail();
                break;
        }
    }

    private void sendPasswordResetEmail(){
        String email = et_email.getText().toString();

        if (email.isEmpty())
            showAlert(ForgotPasswordActivity.this,"Validation Error","Email is required");
        else if (!isValidEmail(email))
            showAlert(ForgotPasswordActivity.this,"Validation Error","Email is invalid");
        else{

            progressBar.setVisibility(View.VISIBLE);
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(ForgotPasswordActivity.this, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()){
                        resetForm();
                        showMessage(ForgotPasswordActivity.this,"Reset Email Sent","Please check your mail");
                    }else{
                        showAlert(ForgotPasswordActivity.this,"Oops",task.getException().getMessage());
                    }
                }
            }).addOnFailureListener(ForgotPasswordActivity.this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressBar.setVisibility(View.GONE);
                    showAlert(ForgotPasswordActivity.this,"Oops",e.getMessage());
                }
            });
        }
    }

    private void resetForm(){
        et_email.setText("");
    }
}
