package com.tullyapp.tully;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.squareup.picasso.Picasso;
import com.tullyapp.tully.Dialogs.ImportDialogFragment;
import com.tullyapp.tully.Dialogs.TutorialScreen;
import com.tullyapp.tully.Dictionary.DicDataManager;
import com.tullyapp.tully.Engineer.EngineerAccessActivity;
import com.tullyapp.tully.Fragments.BaseFragment;
import com.tullyapp.tully.Fragments.HomeFragment;
import com.tullyapp.tully.Fragments.MarketMainFragment;
import com.tullyapp.tully.Fragments.PlayerListFragment;
import com.tullyapp.tully.Fragments.WriteRecordFragment;
import com.tullyapp.tully.Receiver.NetworkReceiver;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.CircleTransform;
import com.tullyapp.tully.Utils.Configuration;

import io.intercom.android.sdk.Intercom;
import io.intercom.android.sdk.UserAttributes;
import io.intercom.android.sdk.identity.Registration;
import io.intercom.android.sdk.push.IntercomPushClient;

import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_FULL_PROFILE;
import static com.tullyapp.tully.Utils.Constants.HOMEFRAGMENT;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_INVITE_ENGINEER;
import static com.tullyapp.tully.Utils.Constants.LYRICSLISTFRAGMENT;
import static com.tullyapp.tully.Utils.Constants.MARKETWATCH;
import static com.tullyapp.tully.Utils.Constants.MARKET_MAIN_FRAGMENT;
import static com.tullyapp.tully.Utils.Constants.PLAYERLISTFRAGMENT;
import static com.tullyapp.tully.Utils.Constants.TUTORIAL_SCREENS;
import static com.tullyapp.tully.Utils.Constants.TUTS_HOME;
import static com.tullyapp.tully.Utils.Constants.WRITERECORDFRAGMENT;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Utils.logOutSequence;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    public static final String ACTION_KILL_HOME = "KILLHOME";
    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final int SETTINGS_ACTIVITY = 99;

    private FirebaseAuth mAuth;
    private NetworkReceiver mNetworkReceiver;

    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothAdapter mBluetoothAdapter;

    private final IntercomPushClient intercomPushClient = new IntercomPushClient();
    private MixpanelAPI mixpanel;

    private BaseFragment baseFragmentInstance;
    private MenuItem actionSettings, action_record_list, actionSearch, actionButton, action_write_list, action_options, action_invite_engineer, action_artist;
    private NavigationView navigationView;
    private DatabaseReference mDatabase;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser()!=null){
            initializeHome();
        }
    }

    private void initializeHome(){
        initUI();
        intercomLogin();
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        //setmBluetoothHeadset();

        //Fabric.with(this, new Crashlytics());
        mixpanel = MixpanelAPI.getInstance(HomeActivity.this, YOUR_PROJECT_TOKEN);
        mixpanel.track("Home Screen");

        try{
            new InitDB().execute();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        addFragment(new HomeFragment(), false);

        try{
            FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        }catch ( Exception e){
            e.printStackTrace();
        }

        mNetworkReceiver = new NetworkReceiver();
        registerBroadcast();

        if (!Configuration.home_tuts) showTutorailDialog(TUTS_HOME);
    }

    void showTutorailDialog(String tut) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        TutorialScreen tutorialScreen = TutorialScreen.newInstance(tut);
        tutorialScreen.show(ft,TutorialScreen.class.getSimpleName());
        tutorialScreen.setOnTutorialClosed(new TutorialScreen.OnTutorialClosed() {
            @Override
            public void onTutsClosed(String tut) {
                mDatabase.child(TUTORIAL_SCREENS).child(tut).setValue(true);
                Configuration.home_tuts = true;
            }
        });
    }

    private void intercomLogin(){
        Registration registration = Registration.create().withUserId(mAuth.getCurrentUser().getUid());
        Intercom.client().registerIdentifiedUser(registration);

        UserAttributes userAttributes = new UserAttributes.Builder()
            .withName(mAuth.getCurrentUser().getDisplayName())
            .withEmail(mAuth.getCurrentUser().getEmail())
            .build();

        Intercom.client().updateUser(userAttributes);

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String token = instanceIdResult.getToken();
                intercomPushClient.sendTokenToIntercom(getApplication(), token);
                FirebaseDatabaseOperations.setPushNotificationToken(getApplicationContext(),token);
            }
        });
    }

    BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            try{
                Log.e(TAG,"onServiceConnected");
                if (profile == BluetoothProfile.HEADSET) {
                    mBluetoothHeadset = (BluetoothHeadset) proxy;
                    if(mBluetoothHeadset!=null && mBluetoothHeadset.getConnectedDevices().size()>0) {
                        Log.e(TAG,"Bluetooth device is connected");
                        AudioManager am = (AudioManager) HomeActivity.this.getSystemService(Context.AUDIO_SERVICE);
                        am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        am.startBluetoothSco();
                        am.setBluetoothScoOn(true);
                        mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET,mBluetoothHeadset);
                        mProfileListener = null;
                        mBluetoothHeadset = null;
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            try{
                Log.e(TAG,"onServiceDisconnected");
                if (profile == BluetoothProfile.HEADSET) {
                    mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET,mBluetoothHeadset);
                    mBluetoothHeadset = null;
                    mProfileListener = null;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
       /* try{
            *//*
                mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET,mBluetoothHeadset);
                mProfileListener = null;
                mBluetoothHeadset = null;
                mBluetoothAdapter = null;
            *//*
        }catch (Exception e){
            e.printStackTrace();
        }*/
    }

    private void setmBluetoothHeadset(){
        try{
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mBluetoothAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.HEADSET);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private class InitDB extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try{
                DicDataManager dicDataManager = DicDataManager.getInstance();
                dicDataManager.createDatabase(HomeActivity.this);
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }


    private void initUI(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setElevation(0);
        getSupportActionBar().setTitle("Home");

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);

        View headerView = navigationView.inflateHeaderView(R.layout.nav_header_home);
        ImageView drawer_profile_image = headerView.findViewById(R.id.drawer_profile_image);
        TextView drawer_user_name = headerView.findViewById(R.id.drawer_user_name);

        drawer_user_name.setText(mAuth.getCurrentUser().getDisplayName());

        try{
            Picasso.with(HomeActivity.this).load(mAuth.getCurrentUser().getPhotoUrl()).placeholder(R.drawable.default_profile_picture).transform(new CircleTransform()).into(drawer_profile_image);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        boolean flag = true;
        if (baseFragmentInstance!=null){
            Log.e(TAG,baseFragmentInstance.getChildFragmentManager().getFragments().toString());
            if (baseFragmentInstance.getChildFragmentManager().getFragments().size()>1){
                baseFragmentInstance.getChildFragmentManager().popBackStack();
                flag = false;
            }

            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else if (!baseFragmentInstance.getClass().getSimpleName().equals(HOMEFRAGMENT)){
                if (flag){
                    navigationView.getMenu().getItem(0).setChecked(true);
                    addFragment(new HomeFragment(), false);
                }
            }
            else{
                super.onBackPressed();
            }
        }
        else{
            super.onBackPressed();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadcast();
        //Log.e("onDestroy","CALLED");
    }

    private void unregisterBroadcast(){
        if (mNetworkReceiver!=null){
            try {
                unregisterReceiver(mNetworkReceiver);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void registerBroadcast(){
        unregisterBroadcast();
        IntentFilter mFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, mFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);

        actionButton = menu.findItem(R.id.action_options);
        actionSearch = menu.findItem(R.id.action_search);
        actionSettings = menu.findItem(R.id.action_settings);
        action_record_list = menu.findItem(R.id.action_record_list);
        action_write_list = menu.findItem(R.id.action_write_list);
        action_options = menu.findItem(R.id.action_options);
        action_invite_engineer = menu.findItem(R.id.action_invite_engineer);
        action_artist = menu.findItem(R.id.action_artist);

        updateActionButtons();

        MenuItem searchViewItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchViewItem);

        MenuItemCompat.setOnActionExpandListener(searchViewItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (baseFragmentInstance!=null){
                    baseFragmentInstance.onSearchCancelled();
                }
                return true;  // Return true to collapse action view
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;  // Return true to expand action view
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.trim().isEmpty()){
                    searchView.clearFocus();
                    if (baseFragmentInstance!=null){
                        baseFragmentInstance.onSearchKey(query.trim());
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int id = item.getItemId();
        Intent intent;
        switch (id){
            case R.id.action_search:
                return true;

            case R.id.action_settings:
                intent = new Intent(this,SettingsActivity.class);
                startActivityForResult(intent,SETTINGS_ACTIVITY);
                break;

            case R.id.action_artist:
                ImportDialogFragment.newInstance().show(getSupportFragmentManager(),ImportDialogFragment.class.getSimpleName());
                break;

            case R.id.action_invite_engineer:
                intent = new Intent(this,EngineerAccessActivity.class);
                intent.putExtra(INTENT_PARAM_INVITE_ENGINEER,true);
                startActivityForResult(intent,0);
                break;

            default:
                baseFragmentInstance.actionEvent(id);
                return true;
        }
        return true;
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id){

            case R.id.nav_home:
                addFragment(HomeFragment.instantiate(this,HomeFragment.class.getName()), false);
                break;

            case R.id.nav_player:
                addFragment(PlayerListFragment.instantiate(this,PlayerListFragment.class.getName()), false);
                break;

            case R.id.nav_write_record:
                addFragment(WriteRecordFragment.instantiate(this,WriteRecordFragment.class.getName()), false);
                break;

            case R.id.nav_profile:
                // addFragment(MarketWatchToggleFragment.instantiate(this,MarketWatchToggleFragment.class.getName()), false);
                addFragment(MarketMainFragment.instantiate(this,MarketMainFragment.class.getName()), false);
                break;

            case R.id.nav_help:
                mixpanel.track(getString(R.string.help_support));
                Intercom.client().displayMessenger();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void addFragment(Fragment fragment, boolean addToBackStack) {
        baseFragmentInstance = (BaseFragment) fragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_up, R.animator.fade_out, R.animator.fade_out, R.animator.fade_out);
        if (addToBackStack) {
            fragmentTransaction.add(R.id.main_container, fragment);
            fragmentTransaction.addToBackStack(null);
        } else {
            fragmentTransaction.replace(R.id.main_container, fragment);
        }
        //fragmentTransaction.commit();
        fragmentTransaction.commit();
        updateActionButtons();
    }

    private void updateActionButtons(){
        switch (baseFragmentInstance.getClass().getSimpleName()){
            case HOMEFRAGMENT:
                getSupportActionBar().show();
                actionVisibility(false,actionButton,actionSettings, action_record_list, action_write_list);
                actionVisibility(true,actionSearch,action_artist,action_invite_engineer);
                setToolbarTitle(getString(R.string.home));
                break;

            case LYRICSLISTFRAGMENT:
                actionVisibility(true,actionButton,actionSearch);
                actionVisibility(false,actionSettings, action_record_list, action_write_list,action_artist,action_invite_engineer);
                setToolbarTitle(getString(R.string.write));
                break;

            case PLAYERLISTFRAGMENT:
                actionVisibility(true,actionButton,actionSearch);
                actionVisibility(false,actionSettings, action_record_list, action_write_list,action_artist,action_invite_engineer);
                setToolbarTitle(getString(R.string.player_list));
                break;

            case WRITERECORDFRAGMENT:
                actionVisibility(true,actionButton,actionSearch, action_record_list, action_write_list);
                actionVisibility(false,actionSettings,action_options,action_artist,action_invite_engineer);
                setToolbarTitle(getString(R.string.write_record));
                break;

            case MARKETWATCH:
                actionVisibility(true, actionSettings);
                actionVisibility(false,actionButton,actionSearch, action_record_list, action_write_list,action_artist,action_invite_engineer);
                setToolbarTitle(getString(R.string.market_place));
                break;

            case MARKET_MAIN_FRAGMENT:
                actionVisibility(true, actionSettings);
                actionVisibility(false,actionButton,actionSearch, action_record_list, action_write_list,action_artist,action_invite_engineer);
                setToolbarTitle(getString(R.string.market_place));
                break;
        }
    }

    private void actionVisibility(boolean visible, MenuItem... menuItems){
        for(MenuItem menuItem : menuItems){
            if (menuItem!=null)
                menuItem.setVisible(visible);
        }
    }


    public void setActionItemActive(int id, boolean boo){
        switch (id){
            case R.id.action_record_list:
                if (boo)
                    action_record_list.setIcon(R.drawable.ic_record_icon_active);
                else
                    action_record_list.setIcon(R.drawable.ic_record_icon);
                break;

            case R.id.action_write_list:
                if (boo)
                    action_write_list.setIcon(R.drawable.ic_write_icon_active);
                else
                    action_write_list.setIcon(R.drawable.ic_write_icon);
                break;
        }
    }

    private void setToolbarTitle(String title){
        try{
            getSupportActionBar().setTitle(title);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
