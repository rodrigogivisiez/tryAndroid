package com.tullyapp.tully.Collaboration;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Utils.Constants;

import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT;

/**
 * Created by Santosh on 19/9/18.
 */
public class CollaborationActivity extends AppCompatActivity implements View.OnClickListener {

    private Project project;
    private Button btnChat;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private String collaborationID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collaboration);
        getSupportActionBar().setTitle(getString(R.string.collaboration));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        project = (Project) getIntent().getSerializableExtra(INTENT_PARAM_PROJECT);
        btnChat = findViewById(R.id.btn_chat);
        btnChat.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        getCollaborationId();
        project = (Project) getIntent().getSerializableExtra(INTENT_PARAM_PROJECT);
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
            case R.id.btn_chat:
                Intent intent = new Intent(CollaborationActivity.this, GroupChatActivity.class);
                intent.putExtra(INTENT_PARAM_PROJECT, project);
                intent.putExtra(Constants.COLLBORATION_ID, collaborationID);
                startActivity(intent);
                break;
            default:
        }
    }

    private void getCollaborationId() {
        databaseReference = FirebaseDatabase.getInstance().getReference().child(firebaseAuth.getCurrentUser().getUid());
        databaseReference.child("projects/" + project.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    if (dataSnapshot.child("collaboration_id").exists()){
                        collaborationID =  (String) dataSnapshot.child("collaboration_id").getValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Error","Error : "+databaseError);
            }
        });
    }
}