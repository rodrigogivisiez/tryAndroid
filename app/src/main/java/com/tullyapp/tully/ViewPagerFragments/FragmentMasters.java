package com.tullyapp.tully.ViewPagerFragments;


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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.tullyapp.tully.Engineer.EngineerInviteFragment;
import com.tullyapp.tully.FirebaseDataModels.Masters;
import com.tullyapp.tully.Fragments.MasterDetailsFragment;
import com.tullyapp.tully.R;

import java.util.TreeMap;

import static com.tullyapp.tully.Fragments.HomeFragment.PARAM_IS_FROM_SEARCH;

public class FragmentMasters extends Fragment implements EngineerInviteFragment.FragmentEvent {

    private static final String ARG_PARAM_MASTER_READY = "ARG_PARAM_MASTER_READY";
    private static final String TAG = FragmentMasters.class.getSimpleName();
    public static final String PARAM_MASTER_NODE = "PARAM_MASTER_NODE";

    private TreeMap<String,Masters> masterNodes;
    private FragmentManager fManager;
    private ProgressBar progressBar;
    private EngineerInviteFragment engineerInviteFragment;
    private boolean isFromSearch = false;
    private ResponseReceiver responseReceiver;

    private static final String fragmentMasters = "com.tullyapp.tully.ViewPagerFragments.FragmentMasters";

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG,intent.getAction());
            switch (intent.getAction()){
                case fragmentMasters:
                    masterNodes = (TreeMap<String, Masters>) intent.getSerializableExtra(PARAM_MASTER_NODE);
                    isFromSearch = intent.getBooleanExtra(PARAM_IS_FROM_SEARCH,false);
                    updateDb();
                break;
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        responseReceiver = new ResponseReceiver();
        registerReceivers();
    }

    public FragmentMasters() {
        // Required empty public constructor
    }

    public static FragmentMasters newInstance() {
        return new FragmentMasters();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fManager = getChildFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fragment_masters, container, false);
        progressBar = view.findViewById(R.id.progressBar);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void updateDb(){
        progressBar.setVisibility(View.GONE);
        if (masterNodes!=null && masterNodes.size()>0){
            addFragment(MasterDetailsFragment.newInstance(masterNodes),false);
        }
        else{
            engineerInviteFragment = EngineerInviteFragment.newInstance(true);
            engineerInviteFragment.setFragmentEvent(this);
            addFragment(engineerInviteFragment,true);
        }
    }

    private void addFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = fManager.beginTransaction();
        //fragmentTransaction.setCustomAnimations(R.anim.slide_in_up, R.animator.fade_out, R.animator.fade_out, R.animator.fade_out);
        if (addToBackStack) {
            fragmentTransaction.replace(R.id.masters_container, fragment);
            fragmentTransaction.addToBackStack(null);
        } else {
            fragmentTransaction.replace(R.id.masters_container, fragment);
        }
        fragmentTransaction.commit();
        //fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onBack() {
        engineerInviteFragment = EngineerInviteFragment.newInstance(true);
        engineerInviteFragment.setFragmentEvent(this);
        addFragment(engineerInviteFragment,true);
    }

    @Override
    public void onDetach() {
        unregisterBroadcast();
        super.onDetach();
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(FragmentMasters.class.getName());
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(responseReceiver, filter);
    }
}
