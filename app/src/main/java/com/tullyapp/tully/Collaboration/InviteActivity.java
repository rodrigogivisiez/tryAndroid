package com.tullyapp.tully.Collaboration;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.ForgotPasswordActivity;
import com.tullyapp.tully.Handler.ValidTullyUserEmail;
import com.tullyapp.tully.Models.InviteCollaborator;
import com.tullyapp.tully.R;

import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT;
import static com.tullyapp.tully.Utils.Utils.hideKeyboard;
import static com.tullyapp.tully.Utils.Utils.hideSoftKeyboard;
import static com.tullyapp.tully.Utils.Utils.isInternetAvailable;
import static com.tullyapp.tully.Utils.Utils.isValidEmail;
import static com.tullyapp.tully.Utils.Utils.showAlert;
import static com.tullyapp.tully.Utils.Utils.showToast;

public class InviteActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText etEmail;
    private Button btnSendInvite;
    private FirebaseAuth mAuth;
    private Project project;
    private String senderName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_collaborator);
        getSupportActionBar().setTitle(getString(R.string.invite));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initUI();

        mAuth = FirebaseAuth.getInstance();
        project = (Project) getIntent().getSerializableExtra(INTENT_PARAM_PROJECT);
    }

    private void initUI() {
        btnSendInvite = (Button) findViewById(R.id.btn_send_invite);
        etEmail = (EditText) findViewById(R.id.et_email);
        btnSendInvite.setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send_invite:
                if(isInternetAvailable(this)) {
                    validateAndSendInvite();
                } else {
                    showToast(this, getString(R.string.error_network_connection));
                }
                break;
            default:
        }
    }

    private void validateAndSendInvite() {
        String email = etEmail.getText().toString();

        if (email.isEmpty())
            showAlert(InviteActivity.this,"Validation Error","Email is required");
        else if (!isValidEmail(email))
            showAlert(InviteActivity.this,"Validation Error","Email is invalid");
        else {
            etEmail.setText("");
            hideKeyboard(this);
            new ValidTullyUserEmail(this, email, project, mAuth).execute();



            /*senderName = mAuth.getCurrentUser().getDisplayName();
            FirebaseDatabase.getInstance()
                    .getReference("collaborations/"+project.getId()+"/invitations")
                    .push()
                    .setValue(new InviteCollaborator(mAuth.getCurrentUser().getUid(), senderName, email, project.getId(), false));
            Toast.makeText(this, getString(R.string.successfully_invited), Toast.LENGTH_SHORT).show();*/
        }
    }
}