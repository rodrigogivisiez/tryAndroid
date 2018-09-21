package com.tullyapp.tully;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.tullyapp.tully.Adapters.ProjectRecordingListAdapter;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.Services.DeleteProjects;
import com.tullyapp.tully.Utils.APIs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import cz.msebera.android.httpclient.Header;

import static com.tullyapp.tully.Services.DeleteProjects.ACTION_DELETE_SINGLEPROJECT_RECORDINGS;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_RECORDINGS;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Utils.getDirectory;
import static com.tullyapp.tully.Utils.ViewUtils.shareAllowDownloadPopup;

public class ProjectAudioListingActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, ProjectRecordingListAdapter.playRecordingListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, ProjectRecordingListAdapter.OnWidgetAction {

    private static final String TAG = ProjectAudioListingActivity.class.getSimpleName();
    protected SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private AppCompatCheckBox share_widget_checkbox_button;
    private LinearLayout share_widget_layout;

    private ArrayList<Recording> recordingAppModels = new ArrayList<>();

    private ProjectRecordingListAdapter recordingListAdapter;

    private MediaPlayer mPlayer = null;
    private File localdir;
    private File localFile;
    private String projectId;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String mainFile;
    private MixpanelAPI mixpanel;
    private int currentPos;
    private int audioDuration;
    private MediaObserver observer;
    private ResponseReceiver responseReceiver;

    private Dialog shareDialog;
    private AppCompatButton btn_allow, btn_not_allow, btn_cp_url;
    private ArrayList<Recording> selectedRecordingList;

    private Dialog renameDialog;
    private EditText et_popup_input;
    private TextView tv_pop_title, tv_pop_desc;
    private AppCompatButton btn_popup_cancel, btn_popup_rename;
    private Recording renameRecordingObj;
    private int renamePos;
    private AlertDialog.Builder optionMenu;
    private static final CharSequence selectionMenuItems[] = new CharSequence[] {"Rename", "Share", "Delete"};
    private RecyclerView recycle_view;
    private SwitchCompat switch_expire_after_once, switch_expire_after_one_hour, switch_expire_never;
    private boolean allowSwitch = true;
    private boolean allowDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_audio_listing);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mAuth = FirebaseAuth.getInstance();
        projectId = getIntent().getStringExtra("PROJECTID");
        mainFile = getIntent().getStringExtra("MAIN");
        if (projectId==null || mAuth.getCurrentUser()==null)
            onBackPressed();
        else
            initUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mixpanel = MixpanelAPI.getInstance(this, YOUR_PROJECT_TOKEN);
    }

    private void initUI(){
        progressBar = findViewById(R.id.progressBar);
        recycle_view = findViewById(R.id.recycle_view);
        LinearLayout widget_share = findViewById(R.id.widget_share);
        LinearLayout widget_delete = findViewById(R.id.widget_delete);
        LinearLayout widget_check_all = findViewById(R.id.widget_check_all);
        share_widget_checkbox_button = findViewById(R.id.share_widget_checkbox_button);
        share_widget_layout = findViewById(R.id.share_widget);

        widget_share.setOnClickListener(this);
        widget_delete.setOnClickListener(this);
        widget_check_all.setOnClickListener(this);
        share_widget_checkbox_button.setOnClickListener(this);
        share_widget_checkbox_button.setOnCheckedChangeListener(this);

        localdir = getDirectory(ProjectAudioListingActivity.this,LOCAL_DIR_NAME_RECORDINGS);
        recordingListAdapter = new ProjectRecordingListAdapter(this,recordingAppModels);
        recordingListAdapter.setOnPlayClickListener(this);
        recordingListAdapter.setOnWidgetAction(this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycle_view.setLayoutManager(linearLayoutManager);
        recycle_view.setAdapter(recordingListAdapter);

        swipeRefreshLayout = findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                progressBar.setVisibility(View.VISIBLE);
                fetchRecordings(projectId);
            }
        });

        fetchRecordings(projectId);

        shareDialog = shareAllowDownloadPopup(this);

        renameDialog = new Dialog(this, R.style.MyDialogTheme);
        renameDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        renameDialog.setContentView(R.layout.input_popup);
        renameDialog.setCancelable(false);
        renameDialog.setCanceledOnTouchOutside(false);

        et_popup_input = renameDialog.findViewById(R.id.et_popup_input);
        tv_pop_title = renameDialog.findViewById(R.id.tv_pop_title);
        tv_pop_desc = renameDialog.findViewById(R.id.tv_pop_desc);
        et_popup_input = renameDialog.findViewById(R.id.et_popup_input);

        tv_pop_title.setText(getString(R.string.rename_file));

        btn_popup_rename = renameDialog.findViewById(R.id.btn_popup_create);
        btn_popup_cancel = renameDialog.findViewById(R.id.btn_popup_cancel);

        btn_popup_rename.setText(R.string.rename_cap);

        btn_popup_rename.setOnClickListener(this);
        btn_popup_cancel.setOnClickListener(this);

        btn_allow = shareDialog.findViewById(R.id.btn_allow);
        btn_not_allow = shareDialog.findViewById(R.id.btn_not_allow);
        switch_expire_after_once = shareDialog.findViewById(R.id.switch_expire_after_once);
        switch_expire_after_one_hour = shareDialog.findViewById(R.id.switch_expire_after_one_hour);
        switch_expire_never = shareDialog.findViewById(R.id.switch_expire_never);
        btn_cp_url = shareDialog.findViewById(R.id.btn_cp_url);
        switch_expire_after_once.setOnCheckedChangeListener(this);
        switch_expire_after_one_hour.setOnCheckedChangeListener(this);
        switch_expire_never.setOnCheckedChangeListener(this);

        btn_allow.setOnClickListener(this);
        btn_not_allow.setOnClickListener(this);
        btn_cp_url.setOnClickListener(this);

        responseReceiver = new ResponseReceiver();
        registerReceivers();

        optionMenu = new AlertDialog.Builder(this);
        //optionMenu.setTitle("Select");
        optionMenu.setItems(selectionMenuItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case 0:
                    et_popup_input.setText(renameRecordingObj.getName());
                    et_popup_input.requestFocus();
                    renameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    renameDialog.show();
                    break;

                case 1:
                    if (selectedRecordingList==null){
                        selectedRecordingList = new ArrayList<>();
                    }
                    selectedRecordingList.clear();
                    selectedRecordingList.add(renameRecordingObj);
                    shareDialog.show();
                    break;

                case 2:
                    if (selectedRecordingList==null){
                        selectedRecordingList = new ArrayList<>();
                    }
                    selectedRecordingList.clear();
                    selectedRecordingList.add(renameRecordingObj);
                    deleteConfirmation(selectedRecordingList);
                    break;
            }
            }
        });

    }


    private void fetchRecordings(final String projectId){

        if (observer!=null)
            observer.stop();

        clearPlayer();
        recordingAppModels.clear();
        progressBar.setVisibility(View.VISIBLE);
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("projects").child(projectId).child("recordings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                if (dataSnapshot.exists()){

                    for (DataSnapshot node : dataSnapshot.getChildren()) {
                        Recording recording = node.getValue(Recording.class);
                        if (!recording.getTid().equals(mainFile)){
                            recording.setId(node.getKey());
                            recording.setProjectId(projectId);
                            recording.setOfProject(true);

                            recordingAppModels.add(recording);
                        }
                    }

                    if (recordingAppModels.size()>0) Collections.reverse(recordingAppModels);

                    recordingListAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                recycle_view.getRecycledViewPool().clear();
                recordingListAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.widget_share:
                selectedRecordingList = recordingListAdapter.getSelectedRecordings();
                if (selectedRecordingList.size()==0){
                    Toast.makeText(this, "No recording selected", Toast.LENGTH_SHORT).show();
                }
                else{
                    shareDialog.show();
                }
                toogleWidget(false);
                break;

            case R.id.widget_check_all:
                share_widget_checkbox_button.setChecked(!share_widget_checkbox_button.isChecked());
                break;

            case R.id.widget_delete:
                selectedRecordingList = recordingListAdapter.getSelectedRecordings();
                if (selectedRecordingList.size()==0){
                    Toast.makeText(this, "No recording selected", Toast.LENGTH_SHORT).show();
                }else{
                    deleteConfirmation(selectedRecordingList);
                }
                toogleWidget(false);
                break;

            case R.id.btn_allow:
                if (!allowSwitch){
                    allowSwitch = true;
                    btn_allow.setBackgroundResource(R.drawable.app_greenbtn);
                    btn_not_allow.setBackgroundResource(R.drawable.app_whitebtn);
                    btn_allow.setTextColor(getResources().getColor(android.R.color.white));
                    btn_not_allow.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                }
                break;

            case R.id.btn_not_allow:
                if (allowSwitch){
                    allowSwitch = false;
                    btn_allow.setBackgroundResource(R.drawable.app_whitebtn);
                    btn_not_allow.setBackgroundResource(R.drawable.app_greenbtn);
                    btn_allow.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    btn_not_allow.setTextColor(getResources().getColor(android.R.color.white));
                }
                break;

            case R.id.btn_cp_url:
                shareDialog.dismiss();
                shareAudioFile(selectedRecordingList,allowSwitch);
                break;

            case R.id.btn_popup_create:
                String val = et_popup_input.getText().toString().trim();
                if (!val.isEmpty()){
                    rename(renameRecordingObj,val,renamePos);
                    renameDialog.dismiss();
                }
                break;

            case R.id.btn_popup_cancel:
                renameDialog.dismiss();
                break;
        }
    }

    private void rename(final Recording recording, final String value, final int position){
        try{
            progressBar.setVisibility(View.VISIBLE);
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
            mDatabase.child("projects").child(recording.getProjectId()).child("recordings").child(recording.getId()).child("name").setValue(value).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                recording.setName(value);
                recordingListAdapter.updateAtPos(position);
                }
            }).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    progressBar.setVisibility(View.GONE);
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private int getExpiryConfig(){
        if (switch_expire_after_once.isChecked()){
            return 0;
        }
        else if (switch_expire_after_one_hour.isChecked()){
            return 60;
        }
        else{
            return -1;
        }
    }

    private void shareAudioFile(ArrayList<Recording> recordings, boolean b){

        JSONArray no_project_rec_ids = new JSONArray();
        JSONArray project_rec_ids = new JSONArray();

        for (Recording rec : recordings){
            try {
                JSONObject obj = new JSONObject();
                obj.put(rec.getProjectId(),rec.getId());
                project_rec_ids.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (project_rec_ids.length()>0){
            JSONArray configArr = new JSONArray();
            JSONObject config = new JSONObject();

            try {
                config.put("allow_download",b);
                config.put("expiry",getExpiryConfig());
                configArr.put(config);
            } catch (JSONException e) {
                e.printStackTrace();
            }

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

            RequestParams params = new RequestParams();
            params.put("userid", mAuth.getCurrentUser().getUid());
            params.put("no_project_rec_ids", no_project_rec_ids.toString());
            params.put("project_recs", project_rec_ids.toString());
            params.put("config",configArr.toString());

            client.post(APIs.SHARE_RECORDING, params, new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    String responseString = new String(responseBody);
                    Log.e("RESPONSE",responseString);
                    try {
                        JSONObject response = new JSONObject(responseString);
                        shareLink(response.getJSONObject("data").getString("link"),"Share Recording");
                        Toast.makeText(ProjectAudioListingActivity.this, response.getString("msg"), Toast.LENGTH_SHORT).show();
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    error.printStackTrace();
                }
            });
        }
    }

    private void shareLink(String link, String title){
        mixpanel.track(title);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, link);
        startActivity(Intent.createChooser(share, title));
    }

    private void deleteConfirmation(final ArrayList<Recording> selectedRecordingList) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(ProjectAudioListingActivity.this);
        builder.setTitle(R.string.sure_delete_message_title_recording)
        .setMessage(R.string.sure_delete_message_recording)
        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            deleteSelection(selectedRecordingList);
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

    private void deleteSelection(ArrayList<Recording> selectedRecordingList){
        Toast.makeText(this, "Deleting selected recording", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.VISIBLE);
        DeleteProjects.startActionDeleteSingleProjectRecording(getApplicationContext(),selectedRecordingList);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.share_widget_checkbox_button:
                recordingListAdapter.setCheckedAll(share_widget_checkbox_button.isChecked());
                break;

            case R.id.switch_expire_after_once:
                if (isChecked){
                    switch_expire_after_one_hour.setChecked(false);
                    switch_expire_never.setChecked(false);
                    Log.e(TAG,"EXPIRE AFTER ONCE");
                }
                break;

            case R.id.switch_expire_after_one_hour:
                if (isChecked){
                    switch_expire_after_once.setChecked(false);
                    switch_expire_never.setChecked(false);
                    Log.e(TAG,"EXPIRE AFTER HOUR");
                }
                break;

            case R.id.switch_expire_never:
                if (isChecked){
                    switch_expire_after_once.setChecked(false);
                    switch_expire_after_one_hour.setChecked(false);
                    Log.e(TAG,"EXPIRE NEVER");
                }
                break;
        }
    }

    private void toogleWidget(boolean show){
        if (show){
            share_widget_layout.setVisibility(View.VISIBLE);
            share_widget_layout.setAlpha(0.0f);
            share_widget_layout.animate()
                    .alpha(1.0f)
                    .setListener(null);
            recordingListAdapter.showSelection(true);
        }else{
            share_widget_layout.animate()
                    .alpha(0.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            share_widget_layout.setVisibility(View.GONE);
                        }
                    });
            recordingListAdapter.showSelection(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_settings:
                if (share_widget_layout.getVisibility()==View.VISIBLE){
                    toogleWidget(false);
                }
                else{
                    toogleWidget(true);
                }
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onPlayRecord(Recording recording, int position) {
        play(recording, position);
    }

    @Override
    public void onRecordPause(Recording recording, int position) {
        try{
            if (mPlayer!=null && mPlayer.isPlaying()){
                observer.stop();
                mPlayer.pause();
            }
        }catch (Exception e){
            observer.stop();
            mPlayer.pause();
        }
    }

    @Override
    public void onResume(Recording recording, int position) {
        if (mPlayer!=null){
            mPlayer.start();
            observer = new MediaObserver();
            new Thread(observer).start();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        progressBar.setVisibility(View.GONE);
        audioDuration = mp.getDuration();
        mp.start();
        observer = new MediaObserver();
        new Thread(observer).start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (observer!=null)
            observer.stop();

        try{
            mp.stop();
        }
        catch (Exception e){

        }
        try{
            mp.release();
        }
        catch (Exception e){

        }
        try{
            recordingListAdapter.markAudioComplete(currentPos);
        }
        catch (Exception e){

        }

    }

    private void play(Recording recording, int position){
        currentPos = position;

        if (observer!=null){
            observer.stop();
        }

        if (mPlayer!=null){
            clearPlayer();
        }

        localFile = new File(localdir, recording.getTid());
        String path="";

        if (localFile.exists()){
            path = localFile.getAbsolutePath();
        }
        else{
            if (recording.getDownloadURL()!=null){
                path = recording.getDownloadURL();
            }
            else{
                Toast.makeText(this, "url not provided", Toast.LENGTH_SHORT).show();
            }
        }

        if (!path.isEmpty()){
            try {
                progressBar.setVisibility(View.VISIBLE);
                recordingListAdapter.markAudioPlaying(currentPos);
                mPlayer = new MediaPlayer();
                mPlayer.setDataSource(path);
                mPlayer.prepareAsync();
                mPlayer.setOnPreparedListener(this);
                mPlayer.setOnCompletionListener(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.recording_listing, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        if (observer!=null){
            observer.stop();
        }
        if (mPlayer!=null){
            clearPlayer();
        }
        unregisterBroadcast();
        super.onDestroy();
    }

    @Override
    public void onShare(Recording recording, int position) {
        if (selectedRecordingList == null){
            selectedRecordingList = new ArrayList<>();
        }
        selectedRecordingList.clear();
        selectedRecordingList.add(recording);
        shareDialog.show();
    }

    @Override
    public void onRename(Recording recording, int position) {
        renameRecordingObj = recording;
        renamePos = position;
        et_popup_input.setText(recording.getName());
        renameDialog.show();
    }

    @Override
    public void onDelete(Recording recording, int position) {
        if (selectedRecordingList == null){
            selectedRecordingList = new ArrayList<>();
        }
        selectedRecordingList.clear();
        selectedRecordingList.add(recording);
        deleteConfirmation(selectedRecordingList);
    }

    @Override
    public void onLongPress(Recording recording, int position) {
        renameRecordingObj = recording;
        renamePos = position;
        optionMenu.show();
    }

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            progressBar.setVisibility(View.GONE);
            switch (intent.getAction()){
                case ACTION_DELETE_SINGLEPROJECT_RECORDINGS:
                    fetchRecordings(projectId);
                    break;
            }
        }
    }

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_DELETE_SINGLEPROJECT_RECORDINGS);
        LocalBroadcastManager.getInstance(ProjectAudioListingActivity.this).registerReceiver(responseReceiver, filter);
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(ProjectAudioListingActivity.this).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){

        }
    }

    private class MediaObserver implements Runnable {
        private AtomicBoolean stop = new AtomicBoolean(false);
        private int currenttime;
        private double dd = audioDuration / 100;
        public void stop() {
            stop.set(true);
        }

        @Override
        public void run() {
            while (!stop.get()) {
                currenttime = mPlayer.getCurrentPosition();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recordingListAdapter.updateProgress(currentPos,(int) (currenttime / dd));
                    }
                });

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void clearPlayer(){
        try{
            mPlayer.stop();
        }catch (Exception e){
            e.printStackTrace();
        }
        try {
            mPlayer.release();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
