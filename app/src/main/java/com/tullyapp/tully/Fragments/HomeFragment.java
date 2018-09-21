package com.tullyapp.tully.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.dekoservidoni.omfm.OneMoreFabMenu;
import com.tullyapp.tully.Adapters.HomeTabsAdapter;
import com.tullyapp.tully.Dialogs.ImportDialogFragment;
import com.tullyapp.tully.FirebaseDataModels.HomeDb;
import com.tullyapp.tully.FirebaseDataModels.Profile;
import com.tullyapp.tully.FirebaseDataModels.Settings;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.ViewPagerFragments.FragmentHomeFiles;
import com.tullyapp.tully.ViewPagerFragments.FragmentHomeProjects;
import com.tullyapp.tully.ViewPagerFragments.FragmentMasters;
import com.tullyapp.tully.ViewPagerFragments.HomeAllFragment;

import static com.tullyapp.tully.HomeActivity.ACTION_KILL_HOME;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_HOME;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_SEARCH_HOME;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.DB_PARAM;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.DB_PULL_STATUS;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.PARAM_PROFILE;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.PARAM_SETTINGS;
import static com.tullyapp.tully.Utils.Constants.TAB_ALL;
import static com.tullyapp.tully.Utils.Constants.TAB_FILES;
import static com.tullyapp.tully.Utils.Constants.TAB_MASTERS;
import static com.tullyapp.tully.Utils.Constants.TAB_PROJECTS;
import static com.tullyapp.tully.ViewPagerFragments.FragmentMasters.PARAM_MASTER_NODE;

public class HomeFragment extends BaseFragment {

    private static final String TAG = HomeFragment.class.getSimpleName();
    public static final String PARAM_HOME_DB = "PARAM_HOME_DB";
    public static final String PARAM_IS_FROM_SEARCH = "PARAM_IS_FROM_SEARCH";
    private HomeTabsAdapter homeTabsAdapter;
    private ViewPager mViewPager;
    private TabLayout tabLayout;

    private LinearLayout no_project_container;
    private ProgressBar progressBar;
    private HomeDb homeDb;
    private ResponseReceiver responseReceiver;
    private HomeAllFragment framgmentHomeAll;
    private FragmentHomeFiles fragmentHomeFiles;
    private FragmentHomeProjects fragmentHomeProjects;
    private FragmentMasters fragmentMasters;
    private boolean fragmentsLoaded = false;
    private FragmentActivity mActivity;
    private Context context;
    //private BeatsFragment fragmentBeats;
    private boolean isFromSearch = false;
    private Profile profile;
    private Settings settings;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (FragmentActivity) context;
        this.context = context;
        responseReceiver = new ResponseReceiver();
        registerReceivers();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unregisterBroadcast();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        homeTabsAdapter = new HomeTabsAdapter(getChildFragmentManager());
        mViewPager = view.findViewById(R.id.viewPager);
        mViewPager.setAdapter(homeTabsAdapter);
        mViewPager.setSaveFromParentEnabled(false);
        tabLayout = view.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        no_project_container = view.findViewById(R.id.no_project_container);
        progressBar = view.findViewById(R.id.progressBar);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragmentsLoaded = false;
        fetchDatabase();
    }

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_PULL_HOME);
        filter.addAction(ACTION_KILL_HOME);
        filter.addAction(ACTION_SEARCH_HOME);
        // filter.addAction(ACTION_FETCH_MASTERS);
        LocalBroadcastManager.getInstance(context).registerReceiver(responseReceiver, filter);
    }

    @Override
    public void onSearchKey(String searchKey) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseDatabaseOperations.startSearchHome(context, searchKey);
    }

    @Override
    public void onSearchCancelled() {
        fetchDatabase();
    }

    @Override
    public void fabButtonClicked() {

    }

    @Override
    public void actionEvent(int event) {

    }

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                switch (intent.getAction()){
                    case ACTION_PULL_HOME:
                        Log.e("DB_UPDATE","HOME");
                        progressBar.setVisibility(View.GONE);
                        boolean status = intent.getBooleanExtra(DB_PULL_STATUS,false);
                        homeDb = (HomeDb) intent.getSerializableExtra(DB_PARAM);
                        profile = (Profile) intent.getSerializableExtra(PARAM_PROFILE);
                        settings = (Settings) intent.getSerializableExtra(PARAM_SETTINGS);
                        isFromSearch = false;
                        if (!fragmentsLoaded){
                            if (status){
                                no_project_container.setVisibility(View.GONE);
                                loadFragments();
                            }else{
                                no_project_container.setVisibility(View.VISIBLE);
                            }
                        }else{
                            updateAdapterinFragment(mViewPager.getCurrentItem());
                        }
                        break;

                    case ACTION_SEARCH_HOME:
                        progressBar.setVisibility(View.GONE);
                        homeDb = (HomeDb) intent.getSerializableExtra(DB_PARAM);
                        isFromSearch = true;
                        updateAdapterinFragment(mViewPager.getCurrentItem());
                        break;

                    case ACTION_KILL_HOME:
                        mActivity.finish();
                        break;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void fetchDatabase(){
        progressBar.setVisibility(View.VISIBLE);
        FirebaseDatabaseOperations.startAction(context, ACTION_PULL_HOME);
    }

    private void loadFragments(){
        homeTabsAdapter.clearFragment();
        homeTabsAdapter.notifyDataSetChanged();

        framgmentHomeAll = HomeAllFragment.newInstance();
        fragmentHomeFiles = FragmentHomeFiles.newInstance();
        fragmentHomeProjects = FragmentHomeProjects.newInstance();
        //fragmentBeats = BeatsFragment.newInstance();
        fragmentMasters = FragmentMasters.newInstance();

        homeTabsAdapter.addFragment(framgmentHomeAll,TAB_ALL);
        homeTabsAdapter.addFragment(fragmentHomeFiles,TAB_FILES);
        homeTabsAdapter.addFragment(fragmentHomeProjects,TAB_PROJECTS);
        homeTabsAdapter.addFragment(fragmentMasters,TAB_MASTERS);
        // homeTabsAdapter.addFragment(fragmentBeats,TAB_BEATS);

        homeTabsAdapter.notifyDataSetChanged();
        tabLayout.setVisibility(View.VISIBLE);
        fragmentsLoaded = true;

        updateAdapterinFragment(0);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                updateAdapterinFragment(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void updateAdapterinFragment(int index){
        try{
            switch (index){
                case 0:
                    sendBroadCast(framgmentHomeAll.getClass().getName());
                    break;

                case 1:
                    sendBroadCast(fragmentHomeFiles.getClass().getName());
                    break;

                case 2:
                    sendBroadCast(fragmentHomeProjects.getClass().getName());
                    break;

                case 3:
                    sendMasterBroadCast();
                    break;

                case 4:
                    //sendBroadCast(fragmentBeats.getClass().getName());
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
            loadFragments();
        }
    }

    private void sendBroadCast(String fragmentClassName){
        Log.e(TAG,fragmentClassName);
        Intent intent = new Intent(fragmentClassName);
        intent.putExtra(PARAM_HOME_DB,homeDb);
        intent.putExtra(PARAM_PROFILE,profile);
        intent.putExtra(PARAM_SETTINGS,settings);
        intent.putExtra(PARAM_IS_FROM_SEARCH,isFromSearch);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void sendMasterBroadCast(){
        Intent intent = new Intent(fragmentMasters.getClass().getName());
        intent.putExtra(PARAM_MASTER_NODE,homeDb.getMastersTreeMap());
        intent.putExtra(PARAM_IS_FROM_SEARCH,isFromSearch);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(context).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}
