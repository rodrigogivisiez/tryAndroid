package com.tullyapp.tully.Multitrack;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.tullyapp.tully.Adapters.MultiTrackProjectAdapter;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Services.DeleteProjects;
import com.tullyapp.tully.Utils.APIs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.KeyStore;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

import static com.tullyapp.tully.Services.DeleteProjects.ACTION_DELETE_SINGLEPROJECT_RECORDINGS;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT_ID;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT_MAIN_FILE;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT_NAME;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_COPYTOTULLY;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_RECORDINGS;
import static com.tullyapp.tully.Utils.Utils.formatAudioTime;
import static com.tullyapp.tully.Utils.Utils.getDirectory;
import static com.tullyapp.tully.Utils.ViewUtils.shareAllowDownloadPopup;

public class MultiTrackProject extends Fragment implements View.OnClickListener, MultiTrackProjectAdapter.playRecordingListener, MultiTrackProjectAdapter.OnWidgetAction, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = MultiTrackProject.class.getSimpleName();

    private String project_id, mainFile, projectName;

    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private RecyclerView recycle_view;
    private EditText et_popup_input;
    private TextView tv_pop_title, tv_pop_desc;
    private AppCompatButton btn_popup_cancel, btn_popup_rename, btn_cp_url;
    private AppCompatButton btn_allow, btn_not_allow;

    private SwipeRefreshLayout swipeRefreshLayout;
    private MultiTrackProjectAdapter multiTrackProjectAdapter;
    private Dialog shareDialog;
    private Dialog renameDialog;

    private MediaPlayer mPlayer = null;
    private File localFile, localdir_recordings, localdir_copytotully;
    private int currentPos;
    private int audioDuration;
    private ImageView btn_multi_play;
    private Handler handler, superPoweredHandler;


    private Recording renameRecordingObj;
    private int renamePos;
    private AlertDialog.Builder optionMenu;
    private static final CharSequence selectionMenuItems[] = new CharSequence[] {"Rename", "Share", "Delete"};
    private ResponseReceiver responseReceiver;
    private ArrayList<Recording> selectedRecordingList;
    private FirebaseStorage storage;
    private ProgressDialog progressDialog;
    private long received, total;
    private boolean mixPlaying = false;
    private int samplerate, buffersize;
    private ArrayList selectedMerges;
    private int[] mixPoss = null;
    private boolean wasMixInitialized = false;
    private int[] volume;
    private int delta;
    private SwitchCompat switch_expire_after_once, switch_expire_after_one_hour, switch_expire_never;
    private boolean allowSwitch = true;
    private ImageView btn_close;

    public MultiTrackProject() {
        // Required empty public constructor
    }

    public static MultiTrackProject newInstance(String projectid, String projectName, String main_file) {
        MultiTrackProject fragment = new MultiTrackProject();
        Bundle args = new Bundle();
        args.putString(INTENT_PARAM_PROJECT_ID, projectid);
        args.putString(INTENT_PARAM_PROJECT_NAME,projectName);
        args.putString(INTENT_PARAM_PROJECT_MAIN_FILE, main_file);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            project_id = getArguments().getString(INTENT_PARAM_PROJECT_ID);
            mainFile = getArguments().getString(INTENT_PARAM_PROJECT_MAIN_FILE);
            projectName = getArguments().getString(INTENT_PARAM_PROJECT_NAME);
            mAuth = FirebaseAuth.getInstance();
            storage = FirebaseStorage.getInstance();
            handler = new Handler();
            superPoweredHandler = new Handler();
            selectedMerges = new ArrayList();

            String samplerateString = null, buffersizeString = null;
            if (Build.VERSION.SDK_INT >= 17) {
                AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
                    buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
                }
            }
            if (samplerateString == null) samplerateString = "48000";
            if (buffersizeString == null) buffersizeString = "480";
            samplerate = Integer.parseInt(samplerateString);
            buffersize = Integer.parseInt(buffersizeString);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_multi_track_project, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar = view.findViewById(R.id.progressBar);
        recycle_view = view.findViewById(R.id.recycle_view);
        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
        btn_multi_play = view.findViewById(R.id.btn_multi_play);

        multiTrackProjectAdapter = new MultiTrackProjectAdapter(getContext());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycle_view.setLayoutManager(linearLayoutManager);
        recycle_view.setAdapter(multiTrackProjectAdapter);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchRecordings(project_id);
            }
        });

        shareDialog = shareAllowDownloadPopup(getContext());

        renameDialog = new Dialog(getContext(), R.style.MyDialogTheme);
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

        multiTrackProjectAdapter.setOnPlayClickListener(this);
        multiTrackProjectAdapter.setOnWidgetAction(this);

        btn_allow = shareDialog.findViewById(R.id.btn_allow);
        btn_not_allow = shareDialog.findViewById(R.id.btn_not_allow);
        switch_expire_after_once = shareDialog.findViewById(R.id.switch_expire_after_once);
        switch_expire_after_one_hour = shareDialog.findViewById(R.id.switch_expire_after_one_hour);
        switch_expire_never = shareDialog.findViewById(R.id.switch_expire_never);
        btn_cp_url = shareDialog.findViewById(R.id.btn_cp_url);
        btn_close = shareDialog.findViewById(R.id.btn_close);
        switch_expire_after_once.setOnCheckedChangeListener(this);
        switch_expire_after_one_hour.setOnCheckedChangeListener(this);
        switch_expire_never.setOnCheckedChangeListener(this);

        btn_allow.setOnClickListener(this);
        btn_not_allow.setOnClickListener(this);
        btn_multi_play.setOnClickListener(this);
        btn_cp_url.setOnClickListener(this);
        btn_close.setOnClickListener(this);

        responseReceiver = new ResponseReceiver();
        registerReceivers();

        localdir_recordings = getDirectory(getContext(),LOCAL_DIR_NAME_RECORDINGS);
        localdir_copytotully = getDirectory(getContext(),LOCAL_DIR_NAME_COPYTOTULLY);

        optionMenu = new AlertDialog.Builder(getContext());
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

        fetchRecordings(project_id);
    }

    private void fetchRecordings(final String projectId){
        clearPlayer();
        stopMixPlayer();
        if (wasMixInitialized){
            selectedMerges.clear();
            selectedRecordingList.clear();
            mixPoss = null;
        }
        multiTrackProjectAdapter.clear();
        progressBar.setVisibility(View.VISIBLE);
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("projects").child(projectId).child("recordings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try{
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    if (dataSnapshot.exists()){
                        for (DataSnapshot node : dataSnapshot.getChildren()) {
                            Recording recording = node.getValue(Recording.class);
                            recording.setId(node.getKey());
                            recording.setProjectId(projectId);
                            recording.setOfProject(true);
                            recording.setProjectName(projectName);
                            multiTrackProjectAdapter.add(recording);
                            localFile = new File(localdir_recordings, recording.getTid());
                            if (localFile.exists()) {
                                recording.setLocalAvailable(true);
                                recording.setLocalPath(localFile.getAbsolutePath());
                            }
                            else{
                                recording.setLocalAvailable(false);
                            }
                        }

                        if (multiTrackProjectAdapter.getItemCount()>0) multiTrackProjectAdapter.reverse();
                        multiTrackProjectAdapter.notifyDataSetChanged();
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // recycle_view.getRecycledViewPool().clear();
                // recordingListAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()){
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

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            progressBar.setVisibility(View.GONE);
            switch (intent.getAction()){
                case ACTION_DELETE_SINGLEPROJECT_RECORDINGS:
                    fetchRecordings(project_id);
                    break;
            }
        }
    }

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_DELETE_SINGLEPROJECT_RECORDINGS);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(responseReceiver, filter);
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void deleteConfirmation(final ArrayList<Recording> selectedRecordingList) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(getContext());
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
        Toast.makeText(getContext(), "Deleting selected recording", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.VISIBLE);
        DeleteProjects.startActionDeleteSingleProjectRecording(getContext(),selectedRecordingList);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.widget_share:
                selectedRecordingList = multiTrackProjectAdapter.getSelectedRecordings();
                if (selectedRecordingList.size()==0){
                    Toast.makeText(getContext(), "No recording selected", Toast.LENGTH_SHORT).show();
                }
                else{
                    shareDialog.show();
                }
                //toogleWidget(false);
                break;

            case R.id.widget_check_all:
                //share_widget_checkbox_button.setChecked(!share_widget_checkbox_button.isChecked());
                break;

            case R.id.widget_delete:
                selectedRecordingList = multiTrackProjectAdapter.getSelectedRecordings();
                if (selectedRecordingList.size()==0){
                    Toast.makeText(getContext(), "No recording selected", Toast.LENGTH_SHORT).show();
                }else{
                    deleteConfirmation(selectedRecordingList);
                }
                //toogleWidget(false);
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

            case R.id.btn_close:
                shareDialog.dismiss();
                break;

            case R.id.btn_multi_play:
                if (mixPlaying){
                    btn_multi_play.setImageResource(R.drawable.ic_multi_play);
                    onPlayPause(false);
                    mixPlaying = false;
                    superPoweredHandler.removeCallbacks(SuperPowered);
                    multiTrackProjectAdapter.markMixAudioPause(mixPoss[0]);
                    multiTrackProjectAdapter.markMixAudioPause(mixPoss[1]);
                }
                else{
                    selectedRecordingList = multiTrackProjectAdapter.getSelectedRecordings();
                    if (selectedRecordingList.size()==2){
                        downloadBeforeMergePlay(0);
                    }
                    else{
                        Toast.makeText(getContext(), "Two Audio mixing supported for now !", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
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
                    multiTrackProjectAdapter.updateAtPos(position);
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

    private void play(Recording recording, int position){
        currentPos = position;
        handler.removeCallbacks(MediaObserver);

        if (mPlayer!=null){
            clearPlayer();
        }

        localFile = new File(localdir_recordings, recording.getTid());

        if (localFile.exists()){
            String path = localFile.getAbsolutePath();
            try {
                progressBar.setVisibility(View.VISIBLE);
                multiTrackProjectAdapter.markAudioPlaying(currentPos);
                mPlayer = new MediaPlayer();
                mPlayer.setDataSource(path);
                mPlayer.prepareAsync();
                mPlayer.setOnPreparedListener(this);
                mPlayer.setOnCompletionListener(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            if (recording.getDownloadURL()!=null){
                downloadFile(recording, position);
            }
            else{
                Toast.makeText(getContext(), "url not provided", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void mergePlay(){
        boolean allLocal = true;
        boolean changed = false;
        if (selectedMerges.size()==0){
            changed = true;
            for(Recording recording : selectedRecordingList){
                selectedMerges.add(recording.getId());
                if (!recording.isLocalAvailable()){
                    allLocal = false;
                }
            }
        }
        else{
            for(Recording recording : selectedRecordingList){
                if (!selectedMerges.contains(recording.getId())){
                    changed = true;
                }
                if (!recording.isLocalAvailable()){
                    Toast.makeText(getContext(), recording.getName()+" was not able to download, cannot merge and play", Toast.LENGTH_LONG).show();
                    allLocal = false;
                    break;
                }
            }
        }

        if (allLocal){
            mixPlaying = true;
            mixPoss = multiTrackProjectAdapter.getPositions(selectedRecordingList.get(0).getId(),selectedRecordingList.get(1).getId());
            btn_multi_play.setImageResource(R.drawable.btn_multi_pause);
            superPoweredHandler.post(SuperPowered);
            if (changed){
                clearPlayer();
                selectedMerges.clear();
                for(Recording recording : selectedRecordingList){
                    selectedMerges.add(recording.getId());
                }
                multiTrackProjectAdapter.markAudioPlaying(mixPoss[0],mixPoss[1]);
                playMix(selectedRecordingList.get(0).getLocalPath(),selectedRecordingList.get(1).getLocalPath());
            }
            else{
                clearPlayer();
                multiTrackProjectAdapter.markAudioPlaying(mixPoss[0],mixPoss[1]);
                onPlayPause(true);
            }
        }
    }

    private void playMix(String path1, String path2){
        MixAudio(
            samplerate,     // sampling rate
            buffersize,     // buffer size
            path1,          // path 1
            path2           // path 2
        );
        onPlayPause(true);
        wasMixInitialized = true;
    }

    private void downloadBeforeMergePlay(final int index){
        if (selectedRecordingList.size() == index){
            mergePlay();
        }
        else{
            final Recording recording = selectedRecordingList.get(index);
            if (!recording.isLocalAvailable()){
                try {
                    StorageReference storageRef = storage.getReferenceFromUrl(recording.getDownloadURL());
                    final File localFile = new File(localdir_recordings, recording.getTid());
                    progressDialog = new ProgressDialog(getContext());
                    progressDialog.setMax(100);
                    progressDialog.setIndeterminate(false);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setTitle("Downloading");
                    progressDialog.setProgressPercentFormat(null);
                    progressDialog.setMessage("getting "+recording.getName()+". Don't worry its just once !");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    final File tempFile = File.createTempFile(localFile.getName(),null,getContext().getCacheDir());
                    storageRef.getFile(tempFile).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        try{
                            received = taskSnapshot.getBytesTransferred();
                            total = taskSnapshot.getTotalByteCount();
                            progressDialog.setProgress((int) ((received / total) * 100));
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        }
                    }).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                        try{
                            progressDialog.dismiss();
                            if (task.isSuccessful()){
                                try {
                                    FileChannel src = new FileInputStream(tempFile).getChannel();
                                    FileChannel dest = new FileOutputStream(localFile).getChannel();
                                    dest.transferFrom(src, 0, src.size());
                                    recording.setLocalPath(localFile.getAbsolutePath());
                                    recording.setLocalAvailable(true);
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }finally {
                                    tempFile.delete();
                                    downloadBeforeMergePlay(index+1);
                                }
                            }else{
                                tempFile.delete();
                                Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                                downloadBeforeMergePlay(index+1);
                            }
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            try{
                                progressDialog.dismiss();
                                downloadBeforeMergePlay(index+1);
                            }catch (Exception e2){
                                e2.printStackTrace();
                            }
                        }
                        }
                    });
                } catch (IOException e) {
                    try{
                        progressDialog.dismiss();
                        downloadBeforeMergePlay(index+1);
                    }catch (Exception e2){
                        e2.printStackTrace();
                    }
                    e.printStackTrace();
                }
            }
            else{
                downloadBeforeMergePlay(index+1);
            }
        }
    }

    private void downloadFile(final Recording recording, final int position){
        try {
            StorageReference storageRef = storage.getReferenceFromUrl(recording.getDownloadURL());
            final File localFile = new File(localdir_recordings, recording.getTid());
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMax(100);
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setTitle("Downloading");
            progressDialog.setProgressPercentFormat(null);
            progressDialog.setMessage("getting audio file in your local device. Don't worry its just once !");
            progressDialog.setCancelable(false);
            progressDialog.show();
            final File tempFile = File.createTempFile(localFile.getName(),null,getContext().getCacheDir());
            storageRef.getFile(tempFile).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                try{
                    received = taskSnapshot.getBytesTransferred();
                    total = taskSnapshot.getTotalByteCount();
                    progressDialog.setProgress((int) ((received / total) * 100));
                }catch (Exception e){
                    e.printStackTrace();
                }
                }
            }).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                try{
                    progressDialog.dismiss();
                    if (task.isSuccessful()){
                        try {
                            FileChannel src = new FileInputStream(tempFile).getChannel();
                            FileChannel dest = new FileOutputStream(localFile).getChannel();
                            dest.transferFrom(src, 0, src.size());
                            recording.setLocalAvailable(true);
                            recording.setLocalPath(localFile.getAbsolutePath());
                            play(recording,position);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }finally {
                            tempFile.delete();
                        }
                    }
                    else{
                        tempFile.delete();
                        Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                }
            });
        } catch (IOException e) {
            try{
                progressDialog.dismiss();
            }catch (Exception e2){
                e2.printStackTrace();
            }
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
                    //Log.e("RESPONSE",responseString);
                    try {
                        JSONObject response = new JSONObject(responseString);
                        shareLink(response.getJSONObject("data").getString("link"),"Share Recording");
                        Toast.makeText(getContext(), response.getString("msg"), Toast.LENGTH_SHORT).show();
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
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, link);
        startActivity(Intent.createChooser(share, title));
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

    @Override
    public void onVolumeProgressChange(int progress, Recording recording, int position) {
        int mPos = selectedMerges.indexOf(recording.getId());
        if (mPos != -1){
            volume = multiTrackProjectAdapter.getVolume(mixPoss[0],mixPoss[1]);
            delta = volume[1] - volume[0];
            onVolume(volume[0] * 0.01f, volume[1] * 0.01f, delta);
        }
    }

    @Override
    public void onVolumeProgressTouchStart(Recording recording, int position) {

    }

    @Override
    public void onVolumeProgressTouchEnd(Recording recording, int position) {

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        try{
            progressBar.setVisibility(View.GONE);
            audioDuration = mp.getDuration();
            mp.start();
            handler.post(MediaObserver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        handler.removeCallbacks(MediaObserver);
        try{
            mp.stop();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        try{
            mp.release();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        try{
            multiTrackProjectAdapter.markAudioComplete(currentPos);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public void onPlayRecord(Recording recording, int position) {
        //Log.e(TAG,recording.getDownloadURL());
        //Log.e(TAG,recording.getLocalPath());
        //AudioAnalyzeService.startAnalyzingAudio(getContext(),recording.getLocalPath());
        int mPos = selectedMerges.indexOf(recording.getId());
        if (mPos != -1){
            multiTrackProjectAdapter.markMixAudioPlaying(position);
            onPlayPausePlayer(true,mPos);
        }
        else{
            stopMixPlayer();
            if (wasMixInitialized){
                selectedMerges.clear();
                selectedRecordingList.clear();
                mixPoss = null;
            }
            play(recording, position);
        }
    }

    @Override
    public void onRecordPause(Recording recording, int position) {
        int mPos = selectedMerges.indexOf(recording.getId());
        if (mPos != -1){
            multiTrackProjectAdapter.markMixAudioPause(position);
            onPlayPausePlayer(false,mPos);
            Recording r1 = multiTrackProjectAdapter.getObjectAtPosition(mixPoss[0]);
            Recording r2 = multiTrackProjectAdapter.getObjectAtPosition(mixPoss[1]);
            if (r1.isPaused() && r2.isPaused()){
                stopMixPlayer();
            }
        }
        else{
            try{
                if (mPlayer!=null && mPlayer.isPlaying()){
                    handler.removeCallbacks(MediaObserver);
                    mPlayer.pause();
                }
            }
            catch (Exception e){
                handler.removeCallbacks(MediaObserver);
                mPlayer.pause();
            }
        }
    }

    @Override
    public void onResume(Recording recording, int position) {
        int mPos = selectedMerges.indexOf(recording.getId());
        if (mPos != -1){
            multiTrackProjectAdapter.markMixAudioPlaying(position);
            onPlayPausePlayer(true,mPos);
        }
        else{
            if (mPlayer!=null){
                stopMixPlayer();
                mPlayer.start();
                handler.post(MediaObserver);
            }
        }
    }

    private void stopMixPlayer(){
        if (wasMixInitialized){
            mixPlaying = false;
            onPlayPause(false);
            superPoweredHandler.removeCallbacks(SuperPowered);
            btn_multi_play.setImageResource(R.drawable.ic_multi_play);
        }
    }

    Runnable SuperPowered = new Runnable() {
        static final long PROGRESS_UPDATE = 600;
        String progress1, progress2;
        long durationA, durationB;
        int isPlayingA,isPlayingB, isAEOF, isBEOF;
        double dd;
        int percentA, percentB;
        boolean flagA = false, flagB = false;
        boolean didPlayedOnceA = false, didPlayedOnceB = false;
        @Override
        public void run() {
            double[] p1 = getPlayerPosition(0);

            isPlayingA = (int) p1[2];
            isPlayingB = (int) p1[6];

            if (isPlayingA == 1){
                progress1 = formatAudioTime((long) p1[0]);
                dd = p1[1] / 100;
                percentA = (int) (p1[0] / dd);
                durationA = (long) p1[1];
                flagA = false;
                isAEOF = 0;
            }

            if (isPlayingB == 1){
                progress2 = formatAudioTime((long) p1[4]);
                dd = p1[5] / 100;
                percentB = (int) (p1[4] / dd);
                durationB = (long) p1[5];
                flagB =false;
                isAEOF = 0;
            }

            isAEOF = (int) p1[3];
            isBEOF = (int) p1[7];
            try{
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isPlayingA == 1){
                            multiTrackProjectAdapter.updateProgress(mixPoss[0],progress1,percentA, durationA);
                            didPlayedOnceA = true;
                        }
                        else{
                            if (!flagA && didPlayedOnceA && isAEOF == 1){
                                flagA = true;
                                multiTrackProjectAdapter.resetProgress(mixPoss[0],durationA);
                            }
                        }

                        if (isPlayingB == 1) {
                            multiTrackProjectAdapter.updateProgress(mixPoss[1],progress2,percentB, durationB);
                            didPlayedOnceB = true;
                        }
                        else{
                            if (!flagB && didPlayedOnceB && isBEOF == 1){
                                flagB = true;
                                multiTrackProjectAdapter.resetProgress(mixPoss[1],durationA);
                            }
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }
            superPoweredHandler.postDelayed(this,PROGRESS_UPDATE);
            if (isAEOF == 1 && isBEOF == 1){
                try{
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stopMixPlayer();
                        }
                    });
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    };

    Runnable MediaObserver = new Runnable() {
        static final long PROGRESS_UPDATE = 600;
        private int currenttime, percent;
        private double dd;

        @Override
        public void run() {
            currenttime = mPlayer.getCurrentPosition();
            dd = audioDuration / 100;
            percent = (int) (currenttime / dd);
            Log.e(TAG,percent+"");
            try{
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        multiTrackProjectAdapter.updateProgress(currentPos,percent);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }
            handler.postDelayed(this,PROGRESS_UPDATE);
        }
    };

    private void clearPlayer(){
        handler.removeCallbacks(MediaObserver);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mixPlaying){
            onPlayPause(false);
            superPoweredHandler.removeCallbacks(SuperPowered);
        }
        if (wasMixInitialized){
            onClearPlayers();
        }
    }

    private native void MixAudio(int samplerate, int buffersize, String path1, String path2);
    private native void onPlayPause(boolean play);
    private native void onPlayPausePlayer(boolean play, int playerIndex);
    private native void onVolume(float volumeA, float volumeB, int delta);
    private native double[] getPlayerPosition(int playerID);
    private native void onClearPlayers();

    static {
        // Initialize the players and effects, and start the audio engine.
        System.loadLibrary("MixAudio");
    }
}
