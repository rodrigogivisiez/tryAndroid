package com.tullyapp.tully.Engineer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tullyapp.tully.Adapters.MasterListAdapter;
import com.tullyapp.tully.FirebaseDataModels.Masters;
import com.tullyapp.tully.MasterNavActivity;
import com.tullyapp.tully.MasterPlayActivity;
import com.tullyapp.tully.Models.EngineerAccessModel;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import static com.tullyapp.tully.Fragments.MasterDetailsFragment.PARAM_MASTER_FILE;
import static com.tullyapp.tully.MasterNavActivity.PARENT_ID;
import static com.tullyapp.tully.MasterPlayActivity.PARAM_MASTER;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_GET_ENGINEER_DATA;


public class EngineerSettingsFragment extends Fragment implements MasterListAdapter.ItemTap, CompoundButton.OnCheckedChangeListener {

    public static final String PARAM_ENGINEER_ACCESS_MODEL = "PARAM_ENGINEER_ACCESS_MODEL";
    private static final String TAG = EngineerSettingsFragment.class.getSimpleName();
    private MasterListAdapter masterListAdapter;
    private RecyclerView recycle_view;
    private EngineerAccessModel engineerAccessModel;
    private TextView tv_engineer;
    private SwitchCompat switch_admin_access;
    private ProgressBar progressBar;

    private ResponseReceiver responseReceiver;

    public EngineerSettingsFragment() {
        // Required empty public constructor
    }

    public static EngineerSettingsFragment newInstance(EngineerAccessModel engineerAccessModel) {
        EngineerSettingsFragment engineerSettingsFragment = new EngineerSettingsFragment();
        Bundle args = new Bundle();
        args.putSerializable(PARAM_ENGINEER_ACCESS_MODEL,engineerAccessModel);
        engineerSettingsFragment.setArguments(args);
        return engineerSettingsFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments()!=null){
            engineerAccessModel = (EngineerAccessModel) getArguments().getSerializable(PARAM_ENGINEER_ACCESS_MODEL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_engineer_settings, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        responseReceiver = new ResponseReceiver();
        registerReceivers(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recycle_view = view.findViewById(R.id.recycle_view);
        masterListAdapter = new MasterListAdapter(getContext(),true,false);
        progressBar = view.findViewById(R.id.progressBar);
        tv_engineer = view.findViewById(R.id.tv_engineer);
        switch_admin_access = view.findViewById(R.id.switch_admin_access);
        switch_admin_access.setChecked(engineerAccessModel.getAdminAccess());
        tv_engineer.setText(engineerAccessModel.getName());
        masterListAdapter.setItemTapListener(this);
        recycle_view.setLayoutManager(new GridLayoutManager(getContext(),3));
        recycle_view.setAdapter(masterListAdapter);
        recycle_view.setHasFixedSize(true);
        recycle_view.setNestedScrollingEnabled(false);
        switch_admin_access.setOnCheckedChangeListener(this);
        recycle_view.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fade_scale));
        FirebaseDatabaseOperations.getEngineerData(getContext(),engineerAccessModel);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onFileTap(ArrayList<Masters> masters, int position) {
        Intent intent = new Intent(getContext(), MasterPlayActivity.class);
        intent.putExtra(PARAM_MASTER,masters);
        intent.putExtra(PARAM_MASTER_FILE,masters.get(position));
        startActivityForResult(intent,0);
    }

    @Override
    public void onFolderTap(Masters masterNode, int position) {
        Intent intent = new Intent(getContext(), MasterNavActivity.class);
        intent.putExtra(PARENT_ID,masterNode.getId());
        startActivityForResult(intent,0);
    }

    @Override
    public void onLongPress(Masters masterNode, int position) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        FirebaseDatabaseOperations.setAdminAccessForEngineer(getContext(),engineerAccessModel,isChecked);
    }

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case ACTION_GET_ENGINEER_DATA:
                    progressBar.setVisibility(View.GONE);
                    masterListAdapter.clearData();
                    TreeMap<String, Masters> masterNodes = (TreeMap<String, Masters>) intent.getSerializableExtra(PARAM_MASTER_FILE);
                    if (masterNodes!=null && masterNodes.size()>0){
                        Iterator iterator = masterNodes.entrySet().iterator();
                        while (iterator.hasNext()){
                            Map.Entry pair = (Map.Entry) iterator.next();
                            Masters master = (Masters) pair.getValue();
                            masterListAdapter.add(master);
                        }
                    }
                    masterListAdapter.notifyDataSetChanged();
                    break;
            }
        }
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void registerReceivers(Context context){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_GET_ENGINEER_DATA);
        LocalBroadcastManager.getInstance(context).registerReceiver(responseReceiver, filter);
    }
}
