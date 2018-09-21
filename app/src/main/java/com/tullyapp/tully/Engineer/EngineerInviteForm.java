package com.tullyapp.tully.Engineer;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.tullyapp.tully.Interface.AuthToken;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.Constants;
import com.tullyapp.tully.Utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

import static com.tullyapp.tully.Engineer.EngineerPaymentFragment.PARAM_EMAIL;
import static com.tullyapp.tully.Engineer.EngineerPaymentFragment.PARAM_IS_ADMIN;
import static com.tullyapp.tully.Services.ProcessPaymentService.PARAM_PLAN_TYPE;
import static com.tullyapp.tully.Utils.Constants.PENDING_INVITATION;
import static com.tullyapp.tully.Utils.Utils.isInternetAvailable;
import static com.tullyapp.tully.Utils.Utils.isValidEmail;

/**
 * A simple {@link Fragment} subclass.
 */
public class EngineerInviteForm extends Fragment implements View.OnClickListener{

    private static final String TAG = EngineerInviteForm.class.getSimpleName();
    private EditText et_email;
    private Button btn_okay, btn_unlimited, btn_upgrade_send, btn_send;
    private FirebaseAuth mAuth;

    private TextView tv_email;
    private TextView tv_desc;
    private ImageView iv_active_basic, iv_active_unlimited;
    private AppCompatButton btn_send_invite;

    private ProgressBar progressBar;
    private DatabaseReference mDatabase;

    private SwitchCompat switch_admin_access;
    private RelativeLayout rl_free, rl_basic, rl_unlimited;

    private EngineerInviteEvents engineerInviteEvents;
    private String selectedPlan;
    private String invitedMail;
    private InviteSendDialogFragment inviteSendDialogFragment;
    private boolean isAdmin;
    private AuthToken authToken;
    private String finalEmail;

    interface EngineerInviteEvents{
        void subscribeAndSendInvitation(String planType, String email, boolean isAdmin);
    }

    public void setEngineerInviteEvents(EngineerInviteEvents engineerInviteEvents){
        this.engineerInviteEvents = engineerInviteEvents;
    }

    public EngineerInviteForm() {
        // Required empty public constructor
    }

    public static EngineerInviteForm newInstance(String planType, String email, boolean isAdmin){
        EngineerInviteForm engineerInviteForm = new EngineerInviteForm();
        Bundle args = new Bundle();
        args.putString(PARAM_PLAN_TYPE, planType);
        args.putString(PARAM_EMAIL, email);
        args.putBoolean(PARAM_IS_ADMIN,isAdmin);
        engineerInviteForm.setArguments(args);
        return engineerInviteForm;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments()!=null){
            mAuth = FirebaseAuth.getInstance();
            selectedPlan = getArguments().getString(PARAM_PLAN_TYPE);
            invitedMail = getArguments().getString(PARAM_EMAIL);
            isAdmin = getArguments().getBoolean(PARAM_IS_ADMIN);

            authToken = new AuthToken() {
                @Override
                public void onToken(String token, String callback) {
                    switch (callback){
                        case "shootInvitation":
                            shootInvitation(token);
                            break;
                    }
                }
            };
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.engineer_invite_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        btn_send_invite = view.findViewById(R.id.btn_send_invite);
        mDatabase.child(PENDING_INVITATION).keepSynced(true);

        tv_desc = view.findViewById(R.id.tv_desc);
        et_email = view.findViewById(R.id.et_email);
        progressBar = view.findViewById(R.id.progressBar);
        switch_admin_access = view.findViewById(R.id.switch_admin_access);
        rl_free = view.findViewById(R.id.rl_free);
        rl_basic = view.findViewById(R.id.rl_basic);
        rl_unlimited = view.findViewById(R.id.rl_unlimited);
        iv_active_basic = view.findViewById(R.id.iv_active_basic);
        iv_active_unlimited = view.findViewById(R.id.iv_active_unlimited);

        btn_unlimited = view.findViewById(R.id.btn_unlimited);
        btn_upgrade_send = view.findViewById(R.id.btn_upgrade_send);
        btn_send = view.findViewById(R.id.btn_send);

        btn_unlimited.setOnClickListener(this);
        btn_upgrade_send.setOnClickListener(this);
        btn_send.setOnClickListener(this);
        btn_send_invite.setOnClickListener(this);

        if (selectedPlan!=null){
            switch (selectedPlan){
                case "basic":
                    btn_send_invite.setVisibility(View.VISIBLE);
                    rl_free.setVisibility(View.GONE);
                    btn_upgrade_send.setVisibility(View.GONE);
                    iv_active_basic.setVisibility(View.VISIBLE);
                    tv_desc.setText(R.string.share_basic_desc);
                    break;
                case "unlimited":
                    btn_send_invite.setVisibility(View.VISIBLE);
                    rl_free.setVisibility(View.GONE);
                    rl_basic.setVisibility(View.GONE);
                    btn_unlimited.setVisibility(View.GONE);
                    iv_active_unlimited.setVisibility(View.VISIBLE);
                    tv_desc.setText(R.string.share_unlimited_desc);
                    break;
            }
        }

        if (invitedMail!=null && !invitedMail.isEmpty()){
            Log.e(TAG,"AUTO SENT EMAIL INVITATION");
            sendInvite(invitedMail,isAdmin);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_send_invite:
                sendInvitation(switch_admin_access.isChecked());
                break;

            case R.id.btn_send:
                sendInvitation(switch_admin_access.isChecked());
                break;

            case R.id.btn_upgrade_send:
                if (isValidEmail(et_email.getText().toString().trim())){
                    if (engineerInviteEvents!=null){
                        engineerInviteEvents.subscribeAndSendInvitation("basic",et_email.getText().toString().trim(),switch_admin_access.isChecked());
                    }
                }
                else{
                    Toast.makeText(getContext(), "Invalid email", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.btn_unlimited:
                if (isValidEmail(et_email.getText().toString().trim())) {
                    if (engineerInviteEvents != null) {
                        engineerInviteEvents.subscribeAndSendInvitation("unlimited",et_email.getText().toString().trim(), switch_admin_access.isChecked());
                    }
                }
                else{
                    Toast.makeText(getContext(), "Invalid email", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void sendInvitation(boolean isAdmin){
        if (isInternetAvailable(getContext())){
            String val = et_email.getText().toString().trim();
            if (isValidEmail(val)){
                et_email.setText("");
                if (!val.isEmpty()){
                    sendInvite(val.toLowerCase(),isAdmin);
                }
            }
            else{
                Toast.makeText(getContext(), "Email is required", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Toast.makeText(getContext(), "Please make sure, Internet is on !", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendInvite(final String email, final boolean isAdmin){
        if (mAuth.getCurrentUser()!=null){

            boolean boo = true;
            for (UserInfo profile : mAuth.getCurrentUser().getProviderData()) {
                if (profile.getEmail()!=null && profile.getEmail().equals(email)){
                    boo = false;
                }
            }

            if (boo){
                progressBar.setVisibility(View.VISIBLE);
                mDatabase.child(PENDING_INVITATION).orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            Map<String,Object> updateData = new HashMap<>();
                            DataSnapshot node = dataSnapshot.child(dataSnapshot.getChildren().iterator().next().getKey()+"/invited");
                            DataSnapshot userNode = node.child(mAuth.getCurrentUser().getUid());
                            if (userNode.exists()){
                                long sent = (long) userNode.child("sentCount").getValue();
                                if (sent>4L){
                                    changeUI("5 Times invitation limt exceeds", "please contact support", email);
                                }else{
                                    updateData.put("isAdmin",isAdmin);
                                    updateData.put("sentCount",sent+1L);
                                    userNode.getRef().updateChildren(updateData);
                                    generateTokenAndShootInvite(email,isAdmin);
                                }
                            }
                            else{
                                updateData.put("isAdmin",isAdmin);
                                updateData.put("sentCount",1L);
                                userNode.getRef().updateChildren(updateData);
                                generateTokenAndShootInvite(email,isAdmin);
                                generateTokenAndShootInvite(email,isAdmin);
                            }
                        }
                        else{
                            DatabaseReference engineerAccess = mDatabase.child(mAuth.getCurrentUser().getUid()).child("engineer/access");
                            engineerAccess.keepSynced(true);
                            engineerAccess.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()){
                                        changeUI("Engineer already have access", "please contact support", email);
                                    }
                                    else{
                                        String nodeKey = mDatabase.child(PENDING_INVITATION).push().getKey();
                                        mDatabase.child(PENDING_INVITATION).child(nodeKey).child("email").setValue(email);
                                        Map<String,Object> updateData = new HashMap<>();
                                        updateData.put("isAdmin",isAdmin);
                                        updateData.put("sentCount",1L);
                                        mDatabase.child(PENDING_INVITATION).child(nodeKey).child("invited/"+mAuth.getCurrentUser().getUid()).updateChildren(updateData);
                                        generateTokenAndShootInvite(email,isAdmin);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    changeUI("Invitation sending failed", "please try after some time", email);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(getContext(), "Try again later", Toast.LENGTH_SHORT).show();
                        changeUI("Invitation sending failed", "please try after some time", email);
                    }
                });

            }
            else{
                Toast.makeText(getContext(), "You don't need invitation, you can login directly into engineer panel", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    private void shootInvitation(String token){
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
        client.addHeader(Constants.Authorization,token);
        RequestParams params = new RequestParams();
        params.put("invite_email", finalEmail);
        client.post(APIs.INVITE_ENGINEER, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                progressBar.setVisibility(View.GONE);
                String responseString = new String(bytes);
                try {
                    JSONObject response = new JSONObject(responseString);
                    if (response.getInt("status")==1){
                        changeUI(getString(R.string.invite_successfully_sent), getString(R.string.your_invitation_has_been_successfully_sent_to), finalEmail);
                    }
                    else{
                        Toast.makeText(getContext(), "Invitation sending failed, please try after some time", Toast.LENGTH_SHORT).show();
                        changeUI("Invitation sending failed", "please try after some time", finalEmail);
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Something Went wrong :(", Toast.LENGTH_SHORT).show();
                    changeUI("Invitation sending failed", "please try after some time", finalEmail);
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Something Went wrong :(", Toast.LENGTH_SHORT).show();
                changeUI("Invitation sending failed", "please try after some time", finalEmail);
                throwable.printStackTrace();
            }
        });
    }

    private void generateTokenAndShootInvite(final String email, boolean isAdmin){
        finalEmail = email;
        Utils.getAuthToken(getContext(),mAuth,authToken,"shootInvitation");
    }

    private void changeUI(String title, String subtitle, String email){
        progressBar.setVisibility(View.GONE);
        inviteSendDialogFragment = InviteSendDialogFragment.newInstance(title,subtitle,email);
        inviteSendDialogFragment.show(getChildFragmentManager(),InviteSendDialogFragment.class.getSimpleName());
    }
}
