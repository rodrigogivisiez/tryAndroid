package com.tullyapp.tully.ViewPagerFragments;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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
import com.tullyapp.tully.Adapters.BeatAudioAdapter;
import com.tullyapp.tully.Adapters.HomeFilesAdapter;
import com.tullyapp.tully.Adapters.HomeProjectsAdapter;
import com.tullyapp.tully.Adapters.MasterListAdapter;
import com.tullyapp.tully.Collaboration.AcceptInvitationActivity;
import com.tullyapp.tully.Collaboration.CollaborationActivity;
import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.FirebaseDataModels.BeatAudio;
import com.tullyapp.tully.FirebaseDataModels.HomeDb;
import com.tullyapp.tully.FirebaseDataModels.Masters;
import com.tullyapp.tully.FirebaseDataModels.Profile;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.FirebaseDataModels.Settings;
import com.tullyapp.tully.Interface.AudioFileEvents;
import com.tullyapp.tully.Interface.AuthToken;
import com.tullyapp.tully.Interface.ProjectRecordingEvents;
import com.tullyapp.tully.MasterNavActivity;
import com.tullyapp.tully.MasterPlayActivity;
import com.tullyapp.tully.PlayActivity;
import com.tullyapp.tully.ProjectActivity;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Receiver.EventsReceiver;
import com.tullyapp.tully.Services.DeleteProjects;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.Constants;
import com.tullyapp.tully.Utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import cz.msebera.android.httpclient.Header;

import static com.tullyapp.tully.Fragments.HomeFragment.PARAM_HOME_DB;
import static com.tullyapp.tully.Fragments.HomeFragment.PARAM_IS_FROM_SEARCH;
import static com.tullyapp.tully.Fragments.MasterDetailsFragment.PARAM_MASTER_FILE;
import static com.tullyapp.tully.MasterNavActivity.FOLDER_NAME;
import static com.tullyapp.tully.MasterNavActivity.PARENT_ID;
import static com.tullyapp.tully.MasterPlayActivity.PARAM_MASTER;
import static com.tullyapp.tully.Services.DeleteProjects.ACTION_DELETE_MASTERS;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_HOME;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.PARAM_PROFILE;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.PARAM_SETTINGS;
import static com.tullyapp.tully.Utils.ActionEventConstant.AUDIO_FILE_UPLOADED;
import static com.tullyapp.tully.Utils.ActionEventConstant.PROJECT_RECORDING_UPLOADED;
import static com.tullyapp.tully.Utils.Constants.BYTETOGB;
import static com.tullyapp.tully.Utils.Constants.BYTETOMB;
import static com.tullyapp.tully.Utils.Constants.BYTETOMBL;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT;
import static com.tullyapp.tully.Utils.Constants.IS_COLLABORATION_PROJECT;
import static com.tullyapp.tully.Utils.Constants.PROJECT_PARAM;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Constants._100MBINBYTE;
import static com.tullyapp.tully.Utils.Utils.isInternetAvailable;
import static com.tullyapp.tully.Utils.Utils.showAlert;
import static com.tullyapp.tully.Utils.Utils.showToast;
import static com.tullyapp.tully.Utils.ViewUtils.shareAllowDownloadPopup;

public class HomeAllFragment extends android.support.v4.app.Fragment implements View.OnClickListener, HomeFilesAdapter.AdapterInterface, HomeProjectsAdapter.AdapterInterface, BeatAudioAdapter.AdapterInterface, MasterListAdapter.ItemTap, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = HomeAllFragment.class.getSimpleName();

    private RecyclerView master_recycler, projects_recycler, files_recycler;

    private HomeFilesAdapter homeFilesAdapter;
    private HomeProjectsAdapter homeProjectsAdapter;
    private MasterListAdapter masterListAdapter;

    private ArrayList<AudioFile> audioFileArrayList = new ArrayList<>();
    private ArrayList<Project> projectArrayList = new ArrayList<>();

    public static final int REQUEST_PROJECT = 1;
    public static final int REQUEST_COPYTULLY = 2;

    private ProgressBar progressBar;
    private Dialog renameDialog;
    private EditText et_popup_input;
    private TextView tv_pop_title, label_masters_data_usage;

    private AppCompatButton btn_popup_cancel, btn_popup_rename;

    private HomeDb homeDb;
    private CurrentObj currentObj = null;
    private Object uobject;
    private FirebaseAuth mAuth;
    private AlertDialog.Builder optionMenu;

    private static final CharSequence selectionMenuItems[] = new CharSequence[] {"Rename", "Share", "Delete"};
    private ArrayList<AudioFile> selectedAudioFileList;
    private ArrayList<Project> selectedProjectList;

    private MixpanelAPI mixpanel;
    private FragmentActivity mActivity;
    private int selectedPosition;
    private Masters selectedMasterNode;

    private Dialog dialog;
    private VideoView videoView;

    private Dialog shareDialog;
    private AppCompatButton btn_allow, btn_not_allow, btn_cp_url;
    private boolean animate = false;
    private boolean isFromSearch = false;
    private RelativeLayout masters_header;
    private AuthToken authToken;
    private Project selectedProject;
    private boolean allowDownload;
    private AudioFile selectedAudio;
    private BeatAudio selectedBeat;
    private SwitchCompat switch_expire_after_once, switch_expire_after_one_hour, switch_expire_never;
    private Profile profile;
    private Settings settings;
    private boolean allowSwitch = true;
    private boolean isAcceptInvite = false, isAcceptInviteExist = false;

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

    private enum CurrentObj {PROJECT, FILE, MASTERS, BEAT}

    private int renamePosition;

    private enum RECYCLER_TYPE {
        MASTER, PROJECTS, FILES, BEAT
    }

    private EventsReceiver eventsReceiver;

    public HomeAllFragment() {
        // Required empty public constructor
    }

    private ResponseReceiver responseReceiver;

    private static final String homeAllFragment = "com.tullyapp.tully.ViewPagerFragments.HomeAllFragment";

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                switch (intent.getAction()){
                    case homeAllFragment:
                        homeDb = (HomeDb) intent.getSerializableExtra(PARAM_HOME_DB);
                        profile = (Profile) intent.getSerializableExtra(PARAM_PROFILE);
                        settings = (Settings) intent.getSerializableExtra(PARAM_SETTINGS);
                        isFromSearch = intent.getBooleanExtra(PARAM_IS_FROM_SEARCH,false);
                        updateAdapter();
                        break;
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mixpanel = MixpanelAPI.getInstance(getContext(), YOUR_PROJECT_TOKEN);
        responseReceiver = new ResponseReceiver();
        registerReceivers();
        authToken = new AuthToken() {
            @Override
            public void onToken(String token, String callback) {
                switch (callback){
                    case "shareProjectLink":
                        shareProjectLink(token);
                        break;

                    case "shareAudioLink":
                        shareAudioLink(token);
                        break;

                    case "shareMasterAudioFile":
                        shareMasterAudioFile(token);
                        break;

                    case "shareBeatFile":
                        shareBeatFile(token);
                        break;
                }
            }
        };
    }

    public static HomeAllFragment newInstance() {
        return new HomeAllFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = (FragmentActivity) getContext();
        animate = true;
        eventsReceiver = new EventsReceiver(getContext(), PROJECT_RECORDING_UPLOADED,AUDIO_FILE_UPLOADED);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_all, container,false);

        progressBar = view.findViewById(R.id.progressBar);
        master_recycler = view.findViewById(R.id.master_recycler);
        projects_recycler = view.findViewById(R.id.projects_recycler);
        files_recycler = view.findViewById(R.id.files_recycler);
        masters_header = view.findViewById(R.id.masters_header);
        label_masters_data_usage = view.findViewById(R.id.label_masters_data_usage);

        master_recycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        projects_recycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        files_recycler.setLayoutManager(new GridLayoutManager(getContext(),3));

        homeFilesAdapter = new HomeFilesAdapter(getContext(), audioFileArrayList,true);
        homeProjectsAdapter = new HomeProjectsAdapter(getContext(), projectArrayList,true);
        masterListAdapter = new MasterListAdapter(getContext(),true,true);

        homeFilesAdapter.setAdapterInterface(this);
        homeProjectsAdapter.setAdapterInterface(this);
        masterListAdapter.setItemTapListener(this);

        master_recycler.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fade_scale));
        master_recycler.setAdapter(masterListAdapter);

        projects_recycler.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fade_scale));
        projects_recycler.setAdapter(homeProjectsAdapter);

        files_recycler.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fade_scale));
        files_recycler.setAdapter(homeFilesAdapter);

        return view;
    }

    private void runLayoutAnimation(RECYCLER_TYPE recycler_type) {
        //final LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_from_bottom);
        switch (recycler_type){
            case FILES:
                files_recycler.getAdapter().notifyDataSetChanged();
                files_recycler.scheduleLayoutAnimation();
                break;

            case MASTER:
                master_recycler.getAdapter().notifyDataSetChanged();
                master_recycler.scheduleLayoutAnimation();
                break;

            case PROJECTS:
                projects_recycler.getAdapter().notifyDataSetChanged();
                projects_recycler.scheduleLayoutAnimation();
                break;
        }
    }

    private void runLayoutAnimation(){

        if (files_recycler!=null && projects_recycler!=null){
            files_recycler.getAdapter().notifyDataSetChanged();
            projects_recycler.getAdapter().notifyDataSetChanged();

             master_recycler.getAdapter().notifyDataSetChanged();

            if (animate){
                files_recycler.scheduleLayoutAnimation();
                projects_recycler.scheduleLayoutAnimation();
                master_recycler.scheduleLayoutAnimation();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dialog = new Dialog(getContext(), R.style.MyDialogTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.video_popup);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        videoView = dialog.findViewById(R.id.videoView);
        videoView.setZOrderOnTop(true);

        renameDialog = new Dialog(getContext(), R.style.MyDialogTheme);
        renameDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        renameDialog.setContentView(R.layout.input_popup);
        renameDialog.setCancelable(false);
        renameDialog.setCanceledOnTouchOutside(false);

        /*swiperefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });*/

        shareDialog = shareAllowDownloadPopup(getContext());

        btn_allow = shareDialog.findViewById(R.id.btn_allow);
        btn_not_allow = shareDialog.findViewById(R.id.btn_not_allow);
        ImageView btn_close = shareDialog.findViewById(R.id.btn_close);
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
        btn_close.setOnClickListener(this);

        et_popup_input = renameDialog.findViewById(R.id.et_popup_input);
        tv_pop_title = renameDialog.findViewById(R.id.tv_pop_title);

        btn_popup_rename = renameDialog.findViewById(R.id.btn_popup_create);
        btn_popup_cancel = renameDialog.findViewById(R.id.btn_popup_cancel);

        btn_popup_rename.setText(R.string.rename_cap);

        btn_popup_rename.setOnClickListener(this);
        btn_popup_cancel.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();

        optionMenu = new AlertDialog.Builder(getContext());
        optionMenu.setItems(selectionMenuItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        if (currentObj == CurrentObj.PROJECT){
                            tv_pop_title.setText(R.string.rename_your_project);
                            Project project = (Project) uobject;
                            et_popup_input.setText(project.getProject_name());
                        }
                        else if (currentObj == CurrentObj.FILE){
                            tv_pop_title.setText(R.string.rename_file);
                            AudioFile audioFile = (AudioFile) uobject;
                            et_popup_input.setText(audioFile.getTitle());
                        }
                        else if (currentObj == CurrentObj.BEAT){
                            tv_pop_title.setText(R.string.rename_beat);
                            BeatAudio beatAudio = (BeatAudio) uobject;
                            et_popup_input.setText(beatAudio.getTitle());
                        }
                        else{
                            tv_pop_title.setText(R.string.rename_your_project);
                            et_popup_input.setText(selectedMasterNode.getName());
                        }
                        et_popup_input.requestFocus();
                        renameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                        renameDialog.show();
                        break;

                    case 1:
                        if (currentObj == CurrentObj.PROJECT){
                            shareDialog.show();
                        }else if (currentObj == CurrentObj.FILE){
                            AudioFile a = (AudioFile) uobject;
                            if (!a.getId().equals(getString(R.string.free_beat_id))){
                                shareDialog.show();
                            }
                            else{
                                shareAudioFile(a,true);
                            }
                        }
                        else if (currentObj == CurrentObj.BEAT){
                            shareDialog.show();
                        }
                        else{
                            shareDialog.show();
                        }
                        break;

                    case 2:
                        selectedProjectList = new ArrayList<>();
                        selectedAudioFileList = new ArrayList<>();
                        if (currentObj == CurrentObj.PROJECT){
                            Project project = (Project) uobject;
                            selectedProjectList.add(project);
                            deleteConfirmation(selectedProjectList);
                        }else if (currentObj == CurrentObj.FILE){
                            AudioFile audioFile = (AudioFile) uobject;
                            selectedAudioFileList.add(audioFile);
                            deleteConfirmationFiles(selectedAudioFileList);
                        }
                        else if (currentObj == CurrentObj.BEAT){
                            BeatAudio beatAudio = (BeatAudio) uobject;
                            ArrayList<BeatAudio> beatList = new ArrayList();
                            beatList.add(beatAudio);
                            deleteConfirmationBeats(beatList);
                        }
                        else{
                            deleteConfirmation();
                        }
                        break;
                }
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                dialog.dismiss();
            }
        });

        eventsReceiver.setProjectRecordingEvents(new ProjectRecordingEvents() {
            @Override
            public void projectRecordingUploaded(Recording recording) {
                homeProjectsAdapter.updateProjectRecording(recording);
            }

            @Override
            public void projectRecordingUploadFailed(Recording recording) {

            }
        });

        eventsReceiver.setAudioFileEvents(new AudioFileEvents() {
            @Override
            public void audioFileUploaded(AudioFile audioFile) {
                homeFilesAdapter.updateFile(audioFile);
            }

            @Override
            public void audioFileUploadFailed(AudioFile audioFile) {

            }
        });
    }

    public void updateAdapter(){
        if (homeDb !=null ){
            try{
                boolean no_files = true;

                projectArrayList.clear();
                audioFileArrayList.clear();
                masterListAdapter.clearData();

                TreeMap<String,Project> projects = homeDb.getSortedProjects();
                if (projects!=null){
                    for (Object o : projects.entrySet()) {
                        Map.Entry pair = (Map.Entry) o;
                        Project projectObj =(Project) pair.getValue();
                        projectObj.setId(pair.getKey().toString());
                        projectArrayList.add(projectObj);
                    }

                    if (projectArrayList.size()>0) no_files = false;

                    Collections.reverse(projectArrayList);
                }

                if (profile!=null && settings!=null){
                    long storageUsed = 0;
                    String storageUsedInSize;
                    if (profile.getStorageUsed()!=null){
                        storageUsed = profile.getStorageUsed().getMasters();
                    }

                    if (storageUsed == 0){
                        storageUsedInSize = getString(R.string._0);
                    }
                    else{
                        if (storageUsed >=_100MBINBYTE){
                            storageUsedInSize = String.format("%d", (storageUsed / BYTETOGB))+" GB";
                        }
                        else{
                            storageUsedInSize = String.format("%d", (storageUsed / BYTETOMBL))+" MB";
                        }
                    }

                    if (settings.getEngineerAdminAccess()!=null && settings.getEngineerAdminAccess().isActive()){
                        if (settings.getEngineerAdminAccess().getPlanType().equals("free")){
                            storageUsedInSize+=" / 1 GB";
                        }
                        else if(settings.getEngineerAdminAccess().getPlanType().equals("basic")){
                            storageUsedInSize+=" / 1 TB";
                        }
                        else if(settings.getEngineerAdminAccess().getPlanType().equals("unlimited")){
                            storageUsedInSize+=" / UNLIMITED";
                        }
                    }
                    else{
                        storageUsedInSize+=" / 1 GB";
                    }
                    label_masters_data_usage.setText(storageUsedInSize);
                }


                String id;
                AudioFile freeBeat = null;
                TreeMap<String, AudioFile> audioFiles = homeDb.getSortedAudioFiles();
                if (audioFiles!=null){
                    for (Object o : audioFiles.entrySet()) {
                        Map.Entry pair = (Map.Entry) o;
                        id = pair.getKey().toString();
                        AudioFile audioFile = (AudioFile) pair.getValue();
                        audioFile.setId(id);

                        if (id.equals(getString(R.string.free_beat_id))){
                            freeBeat = audioFile;
                            continue;
                        }
                        audioFileArrayList.add(audioFile);
                    }

                    if (audioFileArrayList.size()>0) no_files = false;

                    if (freeBeat!=null) audioFileArrayList.add(freeBeat);

                    AudioFile video = new AudioFile();
                    video.setTitle("Getting Started");
                    video.setFilename("Video");
                    video.setSize(0);
                    video.setId("video");
                    video.setVideo(true);

                    audioFileArrayList.add(video);

                    Collections.reverse(audioFileArrayList);
                }

                TreeMap<String, Masters> mastersTree = homeDb.getMastersTreeMap();
                if (mastersTree!=null){
                    for (Object object : mastersTree.entrySet()){
                        Map.Entry pair = (Map.Entry) object;
                        Masters masters = (Masters) pair.getValue();
                        masters.setId(pair.getKey().toString());
                        masterListAdapter.add(masters);
                    }
                }

                if (masterListAdapter.getItemCount()>0){
                    masters_header.setVisibility(View.VISIBLE);
                    master_recycler.setVisibility(View.VISIBLE);
                }
                else{
                    masters_header.setVisibility(View.GONE);
                    master_recycler.setVisibility(View.GONE);
                }

                runLayoutAnimation();

                try{
                    progressBar.setVisibility(View.GONE);
                    //swiperefresh.setRefreshing(false);
                }catch (Exception e){
                    e.printStackTrace();
                }

                if (isFromSearch && no_files){
                    showAlert(getContext(),getString(R.string.please_search_again),getString(R.string.no_search_result_found));
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else{
            if (isFromSearch){
                try{
                    showAlert(getContext(),getString(R.string.please_search_again),getString(R.string.no_search_result_found));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_popup_cancel:
                renameDialog.dismiss();
                break;

            case R.id.btn_popup_create:
                String val = et_popup_input.getText().toString().trim();
                if (!val.isEmpty()){
                    if (currentObj == CurrentObj.PROJECT){
                        rename((Project) uobject, val, renamePosition);
                    }else if (currentObj == CurrentObj.FILE){
                        rename((AudioFile) uobject, val, renamePosition);
                    }
                    else if (currentObj == CurrentObj.BEAT){
                        rename((BeatAudio) uobject, val, renamePosition);
                    }
                    renameDialog.dismiss();
                    progressBar.setVisibility(View.VISIBLE);
                }
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
                if (currentObj == CurrentObj.PROJECT){
                    Project project = (Project) uobject;
                    shareProject(project,allowDownload);
                }else if (currentObj == CurrentObj.FILE){
                    AudioFile audioFile = (AudioFile) uobject;
                    shareAudioFile(audioFile,allowSwitch);
                }
                else if (currentObj == CurrentObj.BEAT){
                    BeatAudio beatAudio = (BeatAudio) uobject;
                    shareBeat(beatAudio,allowSwitch);
                }
                else{
                    shareMasterAudio(allowSwitch);
                }
                shareDialog.dismiss();
                break;

            case R.id.btn_close:
                shareDialog.dismiss();
                break;
        }
    }

    private void rename (final BeatAudio beatAudio, final String value, final int position){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("beats").child(beatAudio.getId()).child("title").setValue(value).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                beatAudio.setTitle(value);
                //beatAudioAdapter.updateFileAtPos(beatAudio, position);
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void rename(final Project project, final String value, final int position){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("projects").child(project.getId()).child("project_name").setValue(value).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            project.setProject_name(value);
            homeProjectsAdapter.updateProjectAtPos(project, position);
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void rename(final AudioFile audioFile, final String value, final int position){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("copytotully").child(audioFile.getId()).child("title").setValue(value).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            audioFile.setTitle(value);
            homeFilesAdapter.updateFileAtPos(audioFile, position);
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
            progressBar.setVisibility(View.GONE);
            }
        });
    }

    // masters rename
    private void rename(final String value){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("masters").child(selectedMasterNode.getId()).child("name").setValue(value).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            selectedMasterNode.setName(value);
            masterListAdapter.updateAtPos(selectedMasterNode,selectedPosition);
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
            progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mixpanel = MixpanelAPI.getInstance(getContext(), YOUR_PROJECT_TOKEN);
    }

    private void shareProject(final Project project, boolean b){
        if (isInternetAvailable(getContext())){
            selectedProject = project;
            allowDownload = b;
            progressBar.setVisibility(View.VISIBLE);
            Utils.getAuthToken(getContext(),mAuth,authToken,"shareProjectLink");
        }
        else{
            Toast.makeText(mActivity, "Network Connection Issue", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    private void shareProjectLink(String token){
        boolean available = false;
        for(Object o : selectedProject.getRecordings().entrySet()){
            Map.Entry pair = (Map.Entry) o;
            Recording rec = (Recording) pair.getValue();
            if (selectedProject.getProject_main_recording().equals(rec.getTid())){
                if (rec.getDownloadURL()!=null){
                    available = true;
                    break;
                }
            }
        }

        JSONArray configArr = new JSONArray();
        JSONObject config = new JSONObject();
        try {
            config.put("allow_download",allowDownload);
            config.put("expiry",getExpiryConfig());
            configArr.put(config);
        } catch (JSONException e) {
            e.printStackTrace();
            progressBar.setVisibility(View.GONE);
        }

        if (available){
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
            client.addHeader(Constants.Authorization,token);
            params.put("userid", mAuth.getCurrentUser().getUid());
            params.put("projectid", selectedProject.getId());
            params.put("config",configArr.toString());
            client.post(APIs.SHARE_PROJECT, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    progressBar.setVisibility(View.GONE);
                    mixpanel.track("Sharing for Projects");
                    String responseString = new String(responseBody);
                    Log.e("RESPONSE",responseString);
                    try {
                        JSONObject response = new JSONObject(responseString);
                        shareLink(response.getJSONObject("data").getString("link"),"Share Project");
                        Toast.makeText(getContext(), response.getString("msg"), Toast.LENGTH_SHORT).show();
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Toast.makeText(getContext(), "Network Fail", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    error.printStackTrace();
                }

            });
        }
        else{
            Toast.makeText(mActivity, "File Still uploading or is broken", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    private void shareLink(String link, String title){
        mixpanel.track(title);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, link);
        startActivity(Intent.createChooser(share, title));
    }

    private void shareAudioFile(AudioFile audioFile, boolean b){
        if (audioFile.getDownloadURL()!=null && !audioFile.getDownloadURL().isEmpty()){
            progressBar.setVisibility(View.VISIBLE);
            selectedAudio = audioFile;
            allowDownload = b;
            Utils.getAuthToken(getContext(),mAuth,authToken,"shareAudioLink");
        }
        else{
            Toast.makeText(getContext(), "Audio still uploading or broken", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
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

    private void shareAudioLink(String token){
        JSONArray configArr = new JSONArray();
        JSONObject config = new JSONObject();
        try {
            config.put("allow_download",allowDownload);
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
        client.addHeader(Constants.Authorization,token);
        params.put("userid", mAuth.getCurrentUser().getUid());
        params.put("ids", selectedAudio.getId());
        params.put("config",configArr.toString());
        client.post(APIs.SHARE_AUDIO, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                progressBar.setVisibility(View.GONE);

                mixpanel.track("Sharing for Files");

                String responseString = new String(responseBody);
                Log.e("RESPONSE",responseString);
                try {
                    JSONObject response = new JSONObject(responseString);
                    shareLink(response.getJSONObject("data").getString("link"),"Share Audio");
                    Toast.makeText(getContext(), response.getString("msg"), Toast.LENGTH_SHORT).show();
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                progressBar.setVisibility(View.GONE);
                error.printStackTrace();
            }
        });
    }

    private void shareBeat(BeatAudio beatAudio, boolean b) {
        progressBar.setVisibility(View.GONE);
        selectedBeat = beatAudio;
        allowDownload = b;
        Utils.getAuthToken(getContext(),mAuth,authToken,"shareBeatFile");
    }

    private void shareBeatFile(String token){
        JSONArray configArr = new JSONArray();
        JSONObject config = new JSONObject();
        try {
            config.put("allow_download",allowDownload);
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
        client.addHeader(Constants.Authorization,token);
        params.put("userid", mAuth.getCurrentUser().getUid());
        params.put("ids", selectedBeat.getId());
        params.put("config",configArr.toString());
        client.post(APIs.SHARE_BEAT, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                progressBar.setVisibility(View.GONE);
                mixpanel.track("Sharing for Files");
                String responseString = new String(responseBody);
                Log.e("RESPONSE",responseString);
                try {
                    JSONObject response = new JSONObject(responseString);
                    shareLink(response.getJSONObject("data").getString("link"),"Share Audio");
                    Toast.makeText(getContext(), response.getString("msg"), Toast.LENGTH_SHORT).show();
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                progressBar.setVisibility(View.GONE);
                error.printStackTrace();
            }
        });
    }

    private void shareMasterAudio(boolean b){
        allowDownload = b;
        Utils.getAuthToken(getContext(),mAuth,authToken,"shareMasterAudioFile");
    }

    private void shareMasterAudioFile(String token){
        JSONArray configArr = new JSONArray();
        JSONObject config = new JSONObject();
        try {
            config.put("allow_download",allowDownload);
            config.put("type",selectedMasterNode.getType());
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
        client.addHeader(Constants.Authorization,token);
        params.put("userid", mAuth.getCurrentUser().getUid());
        params.put("ids", selectedMasterNode.getId());
        params.put("config",configArr.toString());
        client.post(APIs.SHARE_MASTER, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                progressBar.setVisibility(View.GONE);
                mixpanel.track("Sharing for Files");
                String responseString = new String(responseBody);
                Log.e("RESPONSE",responseString);
                try {
                    JSONObject response = new JSONObject(responseString);
                    shareLink(response.getJSONObject("data").getString("link"),"Share Audio");
                    Toast.makeText(getContext(), response.getString("msg"), Toast.LENGTH_SHORT).show();
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                progressBar.setVisibility(View.GONE);
                error.printStackTrace();
            }
        });
    }

   private void deleteConfirmation(){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.sure_delete_message_title)
                .setMessage(R.string.sure_delete_message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteSelection();
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

    private void deleteSelection(){
        DeleteProjects.deleteMasters(getContext(), selectedMasterNode, ACTION_DELETE_MASTERS);
    }

    private void deleteConfirmationFiles(final ArrayList<AudioFile> copyToTullies){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.sure_to_delete_selected_files)
        .setMessage(R.string.sure_delete_message_files)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    deleteFiles(copyToTullies);
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

    private void deleteConfirmationBeats(final ArrayList<BeatAudio> beatAudios){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.sure_to_delete_selected_files)
            .setMessage(R.string.sure_delete_message_files)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    deleteBeatAudio(beatAudios);
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

    private void deleteFiles(ArrayList<AudioFile> copyToTullies){
        progressBar.setVisibility(View.VISIBLE);
        if (copyToTullies.size()>0){
            Toast.makeText(getContext(), "Deleting selected files, might take few seconds", Toast.LENGTH_SHORT).show();
            DeleteProjects.startActionDeleteCopyToTully(getContext(),copyToTullies, ACTION_PULL_HOME);
        }
    }

    private void deleteBeatAudio(ArrayList<BeatAudio> beatAudios){
        progressBar.setVisibility(View.VISIBLE);
        if (beatAudios.size()>0){
            Toast.makeText(getContext(), "Deleting selected files, might take few seconds", Toast.LENGTH_SHORT).show();
            DeleteProjects.startActionDeleteBeat(getContext(),beatAudios, ACTION_PULL_HOME);
        }
    }

    private void deleteConfirmation(final ArrayList<Project> projects){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.sure_delete_message_title)
            .setMessage(R.string.sure_delete_message)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    deleteSelection(projects);
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

    private void deleteSelection(ArrayList<Project> projects){
        progressBar.setVisibility(View.VISIBLE);
        if (projects.size()>0){
            Toast.makeText(getContext(), "Deleting selected projects, might take few seconds", Toast.LENGTH_SHORT).show();
            DeleteProjects.startActionDeleteProjects(getContext(), projects);
        }
    }


    @Override
    public void onLongPressedFile(AudioFile audioFile, int position) {
        //AudioAnalyzeService.startAnalyzingAudio(getContext(),"/data/user/0/com.tullyapp.tully/files/copytotully/-L8qd73ex0jC30gHQPPX.wav");
        if (audioFile.isBeat()){
            currentObj = CurrentObj.BEAT;
        }
        else{
            currentObj = CurrentObj.FILE;
        }
        renamePosition = position;
        uobject = audioFile;
        optionMenu.show();
    }

    @Override
    public void OnlongPressedProject(Project project, int position) {
        currentObj = CurrentObj.PROJECT;
        renamePosition = position;
        uobject = project;
        optionMenu.show();
    }

    @Override
    public void onCopyToTully(AudioFile audioFile, int position) {
        try{
            Intent intent = new Intent(getContext(), PlayActivity.class);
            intent.putExtra("COPYTOTULLY", audioFile);

            //RecyclerView.ViewHolder holder = files_recycler.findViewHolderForAdapterPosition(position);
            //TextView ll = holder.itemView.findViewById(R.id.grid_title);
            //ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(mActivity, ll, ViewCompat.getTransitionName(ll));

            if (audioFile.getId()!=null && audioFile.getId().equals("-L1111aaaaaaaaaaaaaa")){
                mixpanel.track(getString(R.string.free_beat));
            }

            mActivity.startActivityFromFragment(this,intent,REQUEST_PROJECT);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onVideoPlay(AudioFile audioFile, int position) {
        mixpanel.track(getString(R.string.getting_started));
        String path = "android.resource://" + getContext().getPackageName() + "/" + R.raw.android;
        videoView.setVideoURI(Uri.parse(path));
        dialog.show();
        videoView.requestFocus();
        videoView.start();
    }


    @Override
    public void onLongPressedFile(BeatAudio beatAudio, int position) {
        currentObj = CurrentObj.BEAT;
        renamePosition = position;
        uobject = beatAudio;
        optionMenu.show();
    }

    @Override
    public void onBeat(BeatAudio beatAudio, int position) {
        Intent intent = new Intent(getContext(), PlayActivity.class);
        intent.putExtra("COPYTOTULLY", beatAudio);

        RecyclerView.ViewHolder holder = files_recycler.findViewHolderForAdapterPosition(position);
        TextView ll = holder.itemView.findViewById(R.id.grid_title);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(mActivity, ll, ViewCompat.getTransitionName(ll));

        mActivity.startActivityFromFragment(this,intent,REQUEST_PROJECT, options.toBundle());
    }

    @Override
    public void onProjectTap(final Project project, final int position) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid() + "/projects/" + project.getId());

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    if(dataSnapshot.child("accept_invite").exists()) {
                        isAcceptInvite = (boolean) dataSnapshot.child("accept_invite").getValue();
                        isAcceptInviteExist = true;
                    } else {
                        isAcceptInviteExist = false;
                    }
            }

                if(isAcceptInviteExist) {
                    if(isAcceptInvite) {
                        gotoProjectActivityScreen(project, position);
                    } else {
                        Intent intent = new Intent(getContext(), AcceptInvitationActivity.class);
                        intent.putExtra(INTENT_PARAM_PROJECT, project);
                        intent.putExtra(IS_COLLABORATION_PROJECT, true);
                        getActivity().startActivity(intent);
                    }
                } else {
                    gotoProjectActivityScreen(project, position);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, databaseError.toString());
            }
        });


    }

    private void gotoProjectActivityScreen(Project project, int position) {
        Intent intent = new Intent(getContext(), ProjectActivity.class);
        intent.putExtra(PROJECT_PARAM, project);
        try {
            RecyclerView.ViewHolder holder = projects_recycler.findViewHolderForAdapterPosition(position);
            TextView ll = holder.itemView.findViewById(R.id.grid_title);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(mActivity, ll, ViewCompat.getTransitionName(ll));

            mActivity.startActivityFromFragment(this, intent, REQUEST_PROJECT, options.toBundle());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFileTap(ArrayList<Masters> mastersArrayList, int position) {
        Intent intent = new Intent(getContext(), MasterPlayActivity.class);
        intent.putExtra(PARAM_MASTER,mastersArrayList);
        intent.putExtra(PARAM_MASTER_FILE,mastersArrayList.get(position));
        startActivityForResult(intent,0);
    }

    @Override
    public void onFolderTap(Masters masterNode, int position) {
        Intent intent = new Intent(getContext(), MasterNavActivity.class);
        intent.putExtra(PARENT_ID,masterNode.getId());
        intent.putExtra(FOLDER_NAME,masterNode.getName());
        mActivity.startActivityFromFragment(this,intent,0);
    }

    @Override
    public void onLongPress(Masters masterNode, int position) {
        currentObj = CurrentObj.MASTERS;
        selectedPosition = position;
        selectedMasterNode = masterNode;
        optionMenu.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        animate = false;
        switch (requestCode){
            case REQUEST_PROJECT:
                refresh();
                break;

            case REQUEST_COPYTULLY:
                refresh();
                break;
        }
    }

    private void refresh(){
        progressBar.setVisibility(View.VISIBLE);
        FirebaseDatabaseOperations.startAction(getContext(), ACTION_PULL_HOME);
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
        filter.addAction(HomeAllFragment.class.getName());
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(responseReceiver, filter);
    }
}
