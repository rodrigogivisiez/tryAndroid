package com.tullyapp.tully.Engineer;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.tullyapp.tully.Analyzer.AnalyzeSubscriptionDialogFragment;
import com.tullyapp.tully.MasterPlayActivity;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.PreferenceKeys;
import com.tullyapp.tully.Utils.PreferenceUtil;

import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_GET_ENGINEER_ADMIN_ACCESS_SUBSCRIPTION;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.IS_ACTIVE;
import static com.tullyapp.tully.Services.ProcessPaymentService.PARAM_PLAN_TYPE;

/**
 * A simple {@link Fragment} subclass.
 */
public class EngineerInviteFragment extends Fragment implements EngineerInviteForm.EngineerInviteEvents, EngineerPaymentFragment.OnPaymentAction {

    private static final String ARG_PARAM_MASTER_READY = "ARG_PARAM_MASTER_READY";
    private static final String TAG = EngineerInviteFragment.class.getSimpleName();

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private boolean mastersReady;
    private EngineerInviteForm engineerInviteForm;
    private FragmentEvent fragmentEvent;
    private ProgressBar progressBar;
    private EngineerPaymentFragment engineerPaymentFragment;
    private ResponseReceiver responseReceiver;
    private boolean isActive = false;
    private String planType;

    public EngineerInviteFragment() {
        // Required empty public constructor
    }

    public static EngineerInviteFragment newInstance(boolean mastersReady) {
        EngineerInviteFragment fragment = new EngineerInviteFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_PARAM_MASTER_READY, mastersReady);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        responseReceiver = new ResponseReceiver();
        registerReceivers(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mastersReady = getArguments().getBoolean(ARG_PARAM_MASTER_READY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_engineer_invite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar = view.findViewById(R.id.progressBar);
        FirebaseDatabaseOperations.getEngineerAdminSubscription(getContext());

        boolean introFinished = PreferenceUtil.getPref(getContext()).getBoolean(PreferenceKeys.ENGINEER_INTRO_FINISHED, false);
        if (!introFinished){
            EngineerIntroDialogFragment engineerIntroDialogFragment = EngineerIntroDialogFragment.newInstance();
            engineerIntroDialogFragment.show(getChildFragmentManager(),EngineerIntroDialogFragment.class.getSimpleName());
            PreferenceUtil.getPref(getContext()).edit().putBoolean(PreferenceKeys.ENGINEER_INTRO_FINISHED,true).apply();
        }
        // engineerInviteForm.setFragmentEvent(this);
        // tv_email = view.findViewById(R.id.tv_email);
        // btn_okay = view.findViewById(R.id.btn_okay);
        // btn_okay.setOnClickListener(this);
    }

    private void addFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_up, R.animator.fade_out, R.animator.fade_out, R.animator.fade_out);
        if (addToBackStack) {
            fragmentTransaction.add(R.id.frame_container, fragment);
            fragmentTransaction.addToBackStack(fragment.getClass().getName());
        } else {
            fragmentTransaction.replace(R.id.frame_container, fragment);
            fragmentTransaction.addToBackStack(fragment.getClass().getName());
        }
        fragmentTransaction.commit();
        //fragmentTransaction.commitAllowingStateLoss();
    }


    @Override
    public void subscribeAndSendInvitation(String planType, String email, boolean isAdmin) {
        engineerPaymentFragment = EngineerPaymentFragment.newInstance(planType,email,isAdmin);
        engineerPaymentFragment.setPaymentAction(this);
        addFragment(engineerPaymentFragment,true);
    }

    @Override
    public void onActionClose() {

    }

    @Override
    public void onSuccessFullPayment(String planType, String email, boolean isAdmin) {
        engineerInviteForm = EngineerInviteForm.newInstance(planType,email,isAdmin);
        addFragment(engineerInviteForm,false);
        engineerInviteForm.setEngineerInviteEvents(this);
    }

    public interface FragmentEvent{
        void onBack();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unregisterBroadcast();
    }

    public void setFragmentEvent(FragmentEvent fragmentEvent){
        this.fragmentEvent = fragmentEvent;
    }

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                progressBar.setVisibility(View.GONE);
                switch (intent.getAction()) {
                    case ACTION_GET_ENGINEER_ADMIN_ACCESS_SUBSCRIPTION:
                        isActive = intent.getBooleanExtra(IS_ACTIVE,false);
                        if (isActive){
                            planType = intent.getStringExtra(PARAM_PLAN_TYPE);
                            engineerInviteForm = EngineerInviteForm.newInstance(planType,null, false);
                        }
                        else{
                            engineerInviteForm = EngineerInviteForm.newInstance(null, null, false);
                        }
                        addFragment(engineerInviteForm,false);
                        engineerInviteForm.setEngineerInviteEvents(EngineerInviteFragment.this);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void registerReceivers(Context context){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_GET_ENGINEER_ADMIN_ACCESS_SUBSCRIPTION);
        LocalBroadcastManager.getInstance(context).registerReceiver(responseReceiver, filter);
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}
