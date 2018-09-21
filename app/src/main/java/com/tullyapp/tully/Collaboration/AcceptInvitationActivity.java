package com.tullyapp.tully.Collaboration;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.R;


import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT;
import static com.tullyapp.tully.Utils.Constants.IS_COLLABORATION_PROJECT;

/**
 * Created by Santosh Patil on 5/9/18.
 */
public class AcceptInvitationActivity extends AppCompatActivity implements View.OnClickListener,
        ValueEventListener {

    private DatabaseReference databaseReference;
    private String projectID;

    private String collaborationID;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_invitation);
        getSupportActionBar().hide();

        initUI();
        getCollaborationId();
    }

    private void initUI() {
        Button btnAcceptInvitation = findViewById(R.id.btn_accept);
        ImageView ivCancel = findViewById(R.id.iv_cancel);
//        TextView tvInvitedBy = findViewById(R.id.tv_invited);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        Intent intent = getIntent();
        Bundle bundle = new Bundle();
        //bundle = intent.getBundleExtra("project_id");
        Project project = (Project) getIntent().getSerializableExtra(INTENT_PARAM_PROJECT);
        Boolean isCollaborationProject = Boolean.valueOf(intent.getStringExtra(IS_COLLABORATION_PROJECT));
        if(isCollaborationProject) {
            projectID = project.getId();
        } else {
            projectID = (String) bundle.get("project_id");
            //projectID = getIntent().getStringExtra("project_id");
            System.out.println("---> " + projectID);
        }

        btnAcceptInvitation.setOnClickListener(this);
        ivCancel.setOnClickListener(this);

    }


    private void getCollaborationId() {
        databaseReference = FirebaseDatabase.getInstance().getReference().child(firebaseAuth.getCurrentUser().getUid());
        databaseReference.child("projects/" + projectID).addListenerForSingleValueEvent(new ValueEventListener() {
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_accept:
                DatabaseReference leadersRef = FirebaseDatabase.getInstance().getReference("collaborations/"+projectID+"/invitations");
                Query query = leadersRef.orderByChild("receiver_id").equalTo(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                query.addListenerForSingleValueEvent(this);
                finish();
                break;

            case R.id.iv_cancel:
                finish();
                break;
                default:

        }
    }


    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        for (DataSnapshot child: dataSnapshot.getChildren()) {
            child.getRef().child("invite_accept").setValue(true);
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }
}