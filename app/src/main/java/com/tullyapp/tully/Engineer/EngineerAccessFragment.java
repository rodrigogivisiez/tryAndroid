package com.tullyapp.tully.Engineer;


import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tullyapp.tully.Adapters.EngineerAccessAdapter;
import com.tullyapp.tully.Models.EngineerAccessModel;
import com.tullyapp.tully.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class EngineerAccessFragment extends Fragment implements EngineerAccessAdapter.OnTap {

    private EngineerAccessAdapter engineerAccessAdapter;
    private RecyclerView recycle_view;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private DatabaseReference engineerNode;
    EngineerActions engineerActions;

    public EngineerAccessFragment() {
        // Required empty public constructor
    }

    interface EngineerActions{
        void onListEngineer();
        void onEngineerIndividual(EngineerAccessModel engineerAccessModel);
    }

    public void setEngineerActions(EngineerActions engineerActions){
        this.engineerActions = engineerActions;
    }

    public static EngineerAccessFragment newInstance() {
        return new EngineerAccessFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_engineer_access, container, false);
        engineerAccessAdapter = new EngineerAccessAdapter(getContext());
        recycle_view = view.findViewById(R.id.recycle_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycle_view.setLayoutManager(linearLayoutManager);
        recycle_view.setAdapter(engineerAccessAdapter);
        engineerAccessAdapter.setOnTapListener(this);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mAuth.getCurrentUser()!=null){
            mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
            engineerNode = FirebaseDatabase.getInstance().getReference().child("engineer/users");
            engineerNode.keepSynced(true);
            loadData();
        }
    }

    private void loadData(){
        engineerAccessAdapter.clearData();
        mDatabase.child("engineer/access").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    dataSnapshot.getValue(EngineerAccessModel.class);
                    for (DataSnapshot child : dataSnapshot.getChildren()){
                        EngineerAccessModel engineerAccessModel = child.getValue(EngineerAccessModel.class);
                        if (engineerAccessModel !=null){
                            engineerAccessModel.setId(child.getKey());
                            engineerAccessAdapter.add(engineerAccessModel);
                        }
                    }
                    engineerAccessAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onDeleteTap(EngineerAccessModel engineerAccessModel, int position) {
        deleteConfirmation(engineerAccessModel, position);
    }

    @Override
    public void onSettingsTap(EngineerAccessModel engineerAccessModel, int position) {
        if (engineerActions!=null){
            engineerActions.onEngineerIndividual(engineerAccessModel);
        }
    }

    private void deleteConfirmation(final EngineerAccessModel engineerAccessModel, final int position){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.sure_delete_message_title)
            .setMessage(R.string.sure_delete_message)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                deleteEngineerAccess(engineerAccessModel,position);
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

    private void deleteEngineerAccess(final EngineerAccessModel engineerAccessModel, int position){
        mDatabase.child("engineer/access").child(engineerAccessModel.getId()).removeValue();
        final String userId = mAuth.getCurrentUser().getUid();
        engineerNode.child(engineerAccessModel.getId()+"/received_invitation").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String id;
                    for (DataSnapshot node : dataSnapshot.getChildren()){
                        id = node.getValue().toString();
                        if (id.equals(userId)){
                            engineerNode.child(engineerAccessModel.getId()+"/received_invitation/"+node.getKey()).removeValue();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        engineerAccessAdapter.remove(position);
    }
}
