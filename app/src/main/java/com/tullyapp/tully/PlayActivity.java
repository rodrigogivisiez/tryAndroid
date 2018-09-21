package com.tullyapp.tully;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.tullyapp.tully.Adapters.PlayCPListAdapter;
import com.tullyapp.tully.Analyzer.AnalyzeSubscriptionDialogFragment;
import com.tullyapp.tully.BottomSheet.BottomSheet;
import com.tullyapp.tully.Collaboration.CollaborationActivity;
import com.tullyapp.tully.Collaboration.InviteActivity;
import com.tullyapp.tully.Collaboration.SubscribeActivity;
import com.tullyapp.tully.Dialogs.TutorialScreen;
import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.FirebaseDataModels.Lyrics;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.Services.AudioAnalyzeService;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.Configuration;
import com.tullyapp.tully.Utils.OnSwipeListener;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import cz.msebera.android.httpclient.Header;

import static com.tullyapp.tully.LoopActivity.PARAM_END_TIME;
import static com.tullyapp.tully.LoopActivity.PARAM_START_TIME;
import static com.tullyapp.tully.PlayNoteActivity.MULTI_TRACK_REQUEST;
import static com.tullyapp.tully.Services.AudioAnalyzeService.ACTION_ANALYZE_PROGRESS;
import static com.tullyapp.tully.Services.AudioAnalyzeService.INTENT_PARAM_BPM;
import static com.tullyapp.tully.Services.AudioAnalyzeService.INTENT_PARAM_KEY;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_LOOPON;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_LOOPON_START_TIME;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_LOOPON_STOP_TIME;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT_ID;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT_MAIN_FILE;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT_NAME;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_RECORDING;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_COPYTOTULLY;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_RECORDINGS;
import static com.tullyapp.tully.Utils.Constants.TUTORIAL_SCREENS;
import static com.tullyapp.tully.Utils.Constants.TUTS_PLAY;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Utils.formatAudioTime;
import static com.tullyapp.tully.Utils.Utils.getDirectory;
import static com.tullyapp.tully.Utils.Utils.getMime;
import static com.tullyapp.tully.Utils.Utils.isInternetAvailable;
import static com.tullyapp.tully.Utils.Utils.showToast;
import static com.tullyapp.tully.Utils.ViewUtils.shareAllowDownloadPopup;

public class PlayActivity extends AppCompatActivity implements View.OnClickListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener, PlayCPListAdapter.AdapterInterface, BottomSheet.Listener, AnalyzeSubscriptionDialogFragment.SubscriptionEvents, CompoundButton.OnCheckedChangeListener {

    private static final int REQUEST_CODE = 123 ;
    private static final int NEW_REQUEST_CODE = 321;
    private static final String TAG = PlayActivity.class.getSimpleName();
    private AppCompatSeekBar musicProgressbar;
    private Dialog dialog;
    private EditText et_popup_input;
    private TextView tv_pop_title, tv_pop_desc, tv_rec_counter;
    private static final int REQUEST_LOOP_ACTIVITY = 235;

    private TextView infolbl, lyrics_sample, tv_projectname,label_detecting, tv_percent, tv_bpm, tv_key, tv_key_value, tv_bpm_value;
    private ImageView plus_sign, analyze_music;

    private AppCompatButton btn_popup_create, btn_popup_cancel, btn_cp_url;
    private ConstraintLayout cl_swipeup;

    private RelativeLayout createProjectSection, cp_list;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private AudioFile audioFile;
    private Project project;
    private File localdir_copytotully, localdir_recordings;
    private ImageView btn_play, btn_recordl, notification_deco, play_left, play_right, img_option, img_cast, loop_icon;
    private ImageView analyze_icon, three_dash_left, three_dash_right;

    private TextView tv_endtime, tv_startTime ,tv_filename, loop_text;
    private int duration, numberOfRecordings, numberOfAudios;

    private MediaPlayer mPlayer;
    private boolean isReleased = false;
    private boolean isPaused = false;
    private File mainAudio;
    private int no_of_recs = 0;
    private boolean isProject = false;
    private boolean gotoRec = false;
    private boolean isSubscribed = false;
    private Intent intent;

    private short recordingIndex = 0;
    private short audioIndex = 0;
    private ProgressDialog progressDialog;
    private ProgressBar progressBar;

    private AppCompatButton btn_allow, btn_not_allow;
    private Button btn_detect;

    private ArrayList<Recording> recordingArrayList = new ArrayList<>();
    private FirebaseStorage storage;
    private double received;
    private double total;
    private MixpanelAPI mixpanel;
    private GestureDetectorCompat detector;
    private PlayCPListAdapter playCPListAdapter;
    private ArrayList<AudioFile> audioFileArrayList = new ArrayList<>();
    private Handler handler = new Handler();
    private BottomSheet no_project_bottom_sheet, project_bottom_sheet;
    private AudioManager audioManager;
    private LinearLayout unloop_box;
    private boolean loopon = false;
    private int startTime;
    private int endTime;
    private Dialog shareDialog;
    private LayoutInflater vi;
    private ResponseReceiver responseReceiver;
    private int bpm;
    private String key;
    private RelativeLayout insertPoint;
    private View lyricsView, analyzeView;
    private AnalyzeSubscriptionDialogFragment analyzeSubscriptionDialogFragment;
    private boolean analyzerSubscribed = false;
    private ProgressBar analyze_progressbar;
    private long freeTrials = 0;
    private SwitchCompat switch_expire_after_once, switch_expire_after_one_hour, switch_expire_never;
    private boolean allowDownload;
    private boolean allowSwitch = true;

    private enum AUDUI_OUTPUT{
        SPEAKER, EARPHONES, BLUETOOTH
    }
    private AUDUI_OUTPUT AOUTPUT = AUDUI_OUTPUT.SPEAKER;

    private enum CURRENT_VIEW{
        LYRICS_VIEW, ANALYZE_VIEW
    }
    private CURRENT_VIEW current_view = CURRENT_VIEW.LYRICS_VIEW;

    private enum PopupAction {
        CREATE_RECORDING,
        RENAME_PROJECT
    }

    private PopupAction popupAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        getSupportActionBar().setTitle("Play");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        invalidateOptionsMenu();
        responseReceiver = new ResponseReceiver();
        registerReceivers();

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser()!=null){
            audioFile = (AudioFile) getIntent().getSerializableExtra("COPYTOTULLY");
            project = (Project) getIntent().getSerializableExtra(INTENT_PARAM_PROJECT);
            initUI();
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        else{
            onBackPressed();
        }
    }

    private void initUI(){
        vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        dialog = new Dialog(PlayActivity.this, R.style.MyDialogTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.input_popup);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        progressBar = findViewById(R.id.progressbar);

        loop_text = findViewById(R.id.loop_text);
        unloop_box = findViewById(R.id.unloop_box);

        loop_icon = findViewById(R.id.loop_icon);

        et_popup_input = dialog.findViewById(R.id.et_popup_input);

        tv_pop_title = dialog.findViewById(R.id.tv_pop_title);
        tv_pop_title.setText(R.string.name_your_project);

        tv_pop_desc = dialog.findViewById(R.id.tv_pop_desc);
        tv_pop_desc.setText(R.string.you_can_access_this_project);

        btn_popup_create = dialog.findViewById(R.id.btn_popup_create);
        btn_popup_cancel = dialog.findViewById(R.id.btn_popup_cancel);

        createProjectSection = findViewById(R.id.createProjectSection);

        musicProgressbar = findViewById(R.id.appCompatSeekBar);
        musicProgressbar.setOnSeekBarChangeListener(this);

        cl_swipeup = findViewById(R.id.cl_swipeup);
        cp_list = findViewById(R.id.cp_list);

        analyze_music = findViewById(R.id.analyze_music);

        btn_play = findViewById(R.id.btn_play);
        tv_startTime = findViewById(R.id.tv_startTime);
        tv_endtime = findViewById(R.id.tv_endtime);
        //player_write = findViewById(R.id.player_write);
        btn_recordl = findViewById(R.id.btn_record);
        tv_rec_counter = findViewById(R.id.tv_rec_counter);
        notification_deco = findViewById(R.id.notification_deco);
        play_left = findViewById(R.id.play_left);
        play_right = findViewById(R.id.play_right);

        //lyrics_sample = findViewById(R.id.lyrics_sample);
        tv_projectname = findViewById(R.id.tv_projectname);
        tv_filename = findViewById(R.id.tv_filename);
        img_option = findViewById(R.id.img_option);
        img_cast = findViewById(R.id.img_cast);
        insertPoint = createProjectSection;
        lyricsView = vi.inflate(R.layout.start_lyrics_view,null);
        lyrics_sample = lyricsView.findViewById(R.id.lyrics_sample);
        infolbl = lyricsView.findViewById(R.id.infolbl);
        plus_sign = lyricsView.findViewById(R.id.plus_sign);
        tv_key_value = lyricsView.findViewById(R.id.tv_key_value);
        tv_bpm_value = lyricsView.findViewById(R.id.tv_bpm_value);

        analyzeView = vi.inflate(R.layout.audio_analyzing_view,null);
        three_dash_left = analyzeView.findViewById(R.id.three_dash_left);
        three_dash_right = analyzeView.findViewById(R.id.three_dash_right);
        tv_bpm = analyzeView.findViewById(R.id.tv_bpm);
        tv_key = analyzeView.findViewById(R.id.tv_key);
        analyze_icon = analyzeView.findViewById(R.id.analyze_icon);
        label_detecting = analyzeView.findViewById(R.id.label_detecting);
        tv_percent = analyzeView.findViewById(R.id.tv_percent);
        btn_detect = analyzeView.findViewById(R.id.btn_detect);
        analyze_progressbar = analyzeView.findViewById(R.id.progressBar);

        btn_play.setOnClickListener(this);
        btn_popup_create.setOnClickListener(this);
        btn_popup_cancel.setOnClickListener(this);
        createProjectSection.setOnClickListener(this);
        //player_write.setOnClickListener(this);
        btn_recordl.setOnClickListener(this);
        notification_deco.setOnClickListener(this);
        play_left.setOnClickListener(this);
        play_right.setOnClickListener(this);
        img_option.setOnClickListener(this);
        img_cast.setOnClickListener(this);
        unloop_box.setOnClickListener(this);
        analyze_music.setOnClickListener(this);
        btn_detect.setOnClickListener(this);

        shareDialog = shareAllowDownloadPopup(PlayActivity.this);

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

        btn_close.setOnClickListener(this);

        btn_allow.setOnClickListener(this);
        btn_not_allow.setOnClickListener(this);
        btn_cp_url.setOnClickListener(this);

        RecyclerView recycle_view = findViewById(R.id.recycle_view);

        localdir_copytotully = getDirectory(getApplicationContext(),LOCAL_DIR_NAME_COPYTOTULLY);
        localdir_recordings = getDirectory(getApplicationContext(),LOCAL_DIR_NAME_RECORDINGS);

        storage = FirebaseStorage.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());

        if (!Configuration.play_tuts){
            showTutorailDialog(TUTS_PLAY);
        }
        else{
            initiate();
        }

        detector = new GestureDetectorCompat(this, onSwipeListener);
        cl_swipeup.setOnTouchListener(this);

        playCPListAdapter = new PlayCPListAdapter(this, audioFileArrayList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycle_view.setLayoutManager(linearLayoutManager);
        recycle_view.setHasFixedSize(true);
        recycle_view.setAdapter(playCPListAdapter);
        playCPListAdapter.setAdapterListener(this);

        fetchCopyToTullies();

        no_project_bottom_sheet = BottomSheet.newInstance(R.layout.no_project_bottom_sheet);
        project_bottom_sheet = BottomSheet.newInstance(R.layout.project_bottom_sheet);

        no_project_bottom_sheet.setmListener(this);
        project_bottom_sheet.setmListener(this);
    }

    private void setLyricsView(){
        if (project==null){
            current_view = CURRENT_VIEW.LYRICS_VIEW;
            insertPoint.addView(lyricsView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tv_key_value.setText(audioFile.getKey());
            tv_bpm_value.setText(String.valueOf(audioFile.getBpm()));
            analyze_music.setImageResource(R.drawable.ic_analyze_music);
        }
        else{
            current_view = CURRENT_VIEW.LYRICS_VIEW;
            insertPoint.addView(lyricsView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            if (recordingArrayList.size()>0){
                Recording recording = recordingArrayList.get(recordingIndex);
                tv_key_value.setText(recording.getKey());
                tv_bpm_value.setText(String.valueOf(recording.getBpm()));
                analyze_music.setImageResource(R.drawable.ic_analyze_music);
                if (lyrics_sample.getText().toString().isEmpty()){
                    lyrics_sample.setVisibility(View.GONE);
                    infolbl.setVisibility(View.VISIBLE);
                    plus_sign.setVisibility(View.VISIBLE);
                }
                else{
                    infolbl.setVisibility(View.GONE);
                    plus_sign.setVisibility(View.GONE);
                }
            }
        }
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

    private void initiate(){
        if (project!=null){
            isProject = true;
            makeIsProject();
            tv_projectname.setText(project.getProject_name());
            tv_filename.setText(project.getMainFileTitle());
            play();
        }
        else{
            selectCPFile();
        }
        setLyricsView();
    }

    private void showTutorailDialog(String tut) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        TutorialScreen tutorialScreen = TutorialScreen.newInstance(tut);
        tutorialScreen.show(ft,TutorialScreen.class.getSimpleName());
        tutorialScreen.setOnTutorialClosed(new TutorialScreen.OnTutorialClosed() {
            @Override
            public void onTutsClosed(String tut) {
            mDatabase.child(TUTORIAL_SCREENS).child(tut).setValue(true);
            Configuration.play_tuts = true;
            initiate();
            }
        });
    }

    private void selectCPFile(){
        if (project!=null){
            clearProject();
        }
        else{
            if (audioFile!=null){
                try{
                    mainAudio = new File(localdir_copytotully, audioFile.getFilename());
                    tv_projectname.setText("");
                    tv_filename.setText(audioFile.getTitle());
                    play();
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(this, "Failed creating file, permission issue, contact support", Toast.LENGTH_SHORT).show();
                }
            }
            else{
                Toast.makeText(this, "Audio Seems Broken, please delete this and re-upload", Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        }
    }

    private void clearProject(){
        recordingIndex = 0;
        isProject = false;
        recordingArrayList.clear();
        project = null;
        no_of_recs = 0;
        tv_rec_counter.setText("0");
        lyrics_sample.setText("");
        lyrics_sample.setVisibility(View.GONE);
        infolbl.setVisibility(View.VISIBLE);
        plus_sign.setVisibility(View.VISIBLE);
        selectCPFile();
    }

    OnSwipeListener onSwipeListener = new OnSwipeListener() {
        @Override
        public boolean onSwipe(Direction direction) {
            // Possible implementation
            if (direction == Direction.up ) {
                createProjectSection.setVisibility(View.GONE);
                cp_list.setVisibility(View.VISIBLE);
                return true;
            } else if (direction == Direction.down) {
                createProjectSection.setVisibility(View.VISIBLE);
                cp_list.setVisibility(View.GONE);
                return true;
            }
            return super.onSwipe(direction);
        }
    };

    private void fetchCopyToTullies(){
        DatabaseReference mdb = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid()).child("copytotully");
        mdb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    AudioFile freeBeat = null;
                    for (DataSnapshot nodes : dataSnapshot.getChildren()){
                        AudioFile audioFile = nodes.getValue(AudioFile.class);
                        audioFile.setId(nodes.getKey());
                        if (audioFile.getId().equals(getString(R.string.free_beat_id))){
                            freeBeat = audioFile;
                            continue;
                        }
                        audioFileArrayList.add(audioFile);
                    }

                    if (freeBeat!=null) audioFileArrayList.add(freeBeat);
                    Collections.reverse(audioFileArrayList);

                    playCPListAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void makeIsProject(){
        recordingIndex = 0;
        isProject = true;
        mainAudio = new File(localdir_recordings, project.getProject_main_recording());
        if (project.getRecordings()!=null) {
            no_of_recs = project.getRecordings().size();
            no_of_recs = Math.abs(no_of_recs-1);
            recordingArrayList.clear();
            TreeMap<String,Recording> recordingsTreeMap = new TreeMap<>();
            recordingsTreeMap.putAll(project.getRecordings());

            recordingArrayList.add(0,null);
            for(Map.Entry<String,Recording> entry : recordingsTreeMap.entrySet()) {
                Recording recording = entry.getValue();
                recording.setId(entry.getKey());
                recording.setProjectId(project.getId());
                if (project.getProject_main_recording().equals(recording.getTid())){
                    recordingArrayList.set(0,recording);
                }
                else{
                    recordingArrayList.add(recording);
                }
            }
        }
        tv_rec_counter.setText(no_of_recs+"");
        updateLyrics();
    }

    private void updateLyrics(){
        if (project.getLyrics()!=null){
            Iterator it = project.getLyrics().entrySet().iterator();
            if (it.hasNext()){
                Map.Entry pair = (Map.Entry) it.next();
                Lyrics lyrics = (Lyrics) pair.getValue();
                if (lyrics!=null){
                    lyrics.setId(pair.getKey().toString());
                    lyrics_sample.setText(lyrics.getDesc());
                }
            }
        }
        if (lyrics_sample.getText().toString().isEmpty()){
            infolbl.setVisibility(View.VISIBLE);
            plus_sign.setVisibility(View.VISIBLE);
            lyrics_sample.setVisibility(View.GONE);
        }
        else {
            infolbl.setVisibility(View.GONE);
            plus_sign.setVisibility(View.GONE);
            lyrics_sample.setVisibility(View.VISIBLE);
        }
    }

    private void play(){
        clearPlayer();
        String path;
        if (mainAudio!=null && mainAudio.exists()){
            path = mainAudio.getAbsolutePath();
            Log.e(TAG,path);
            if (!path.isEmpty()){
                try {
                    isPaused = false;
                    isReleased = false;
                    btn_play.setImageResource(R.drawable.player_pause_icon);
                    mPlayer = new MediaPlayer();
                    mPlayer.setDataSource(path);
                    mPlayer.prepareAsync();
                    // mPlayer.setLooping(isRepeat);
                    mPlayer.setOnPreparedListener(this);
                    mPlayer.setOnCompletionListener(this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                Toast.makeText(this, "Seems Audio is broken", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            if (isProject){
                if (recordingArrayList.size()>0){
                    try{
                        if (recordingArrayList.get(recordingIndex).getDownloadURL()!=null){
                            Recording recording = recordingArrayList.get(recordingIndex);
                            StorageReference storageRef = storage.getReferenceFromUrl(recording.getDownloadURL());
                            File localFile = new File(localdir_recordings, recording.getTid());
                            downloadFile(storageRef,localFile);
                        }
                        else{
                            Toast.makeText(this, "Seems Audio is broken", Toast.LENGTH_SHORT).show();
                        }
                    }catch (Exception e){
                        Toast.makeText(this, "Seems Audio is broken, contact support", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(this, "Seems Audio is broken, contact support", Toast.LENGTH_SHORT).show();
                }
            }
            else{
                try{
                    if (audioFile.getDownloadURL()!=null && !audioFile.getDownloadURL().isEmpty()){
                        StorageReference storageRef = storage.getReferenceFromUrl(audioFile.getDownloadURL());
                        File localFile = new File(localdir_copytotully, audioFile.getFilename());
                        downloadFile(storageRef,localFile);
                    }
                    else{
                        Toast.makeText(this, "File Broken or missing, contact support", Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(this, "Link broken, contact support", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void downloadFile(StorageReference storageRef, final File localFile){
        try {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMax(100);
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setTitle("Downloading");
            progressDialog.setProgressPercentFormat(null);
            progressDialog.setMessage("getting audio file in your local device. Don't worry its just once !");
            progressDialog.setCancelable(false);
            progressDialog.show();
            final File tempFile = File.createTempFile(localFile.getName(),null,this.getCacheDir());
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
                                mainAudio = localFile;
                                play();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }finally {
                                tempFile.delete();
                            }
                        }else{
                            tempFile.delete();
                            Toast.makeText(PlayActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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

    private void showCreatePopup(){
        tv_pop_title.setText(R.string.name_your_project);
        tv_pop_desc.setText(R.string.you_can_access_this_project);
        et_popup_input.setText("");
        et_popup_input.requestFocus();
        popupAction = PopupAction.CREATE_RECORDING;
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
    }

    private void showRenamePopup(){
        tv_pop_title.setText(R.string.rename_your_project);
        tv_pop_desc.setText(R.string.you_can_access_this_project_from_the_home_tab);
        et_popup_input.setText(project.getProject_name());
        et_popup_input.requestFocus();
        popupAction = PopupAction.RENAME_PROJECT;
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.createProjectSection:
                if (current_view == CURRENT_VIEW.ANALYZE_VIEW){
                    analyze_music.setImageResource(R.drawable.ic_analyze_music);
                    if (project!=null){
                        Recording recording = recordingArrayList.get(recordingIndex);
                        insertPoint.removeAllViewsInLayout();
                        tv_bpm_value.setText(recording.getBpm()+"");
                        tv_key_value.setText(recording.getKey());
                        insertPoint.addView(lyricsView,0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    }
                    else{
                        insertPoint.removeAllViewsInLayout();
                        tv_bpm_value.setText(audioFile.getBpm()+"");
                        tv_key_value.setText(audioFile.getKey());
                        insertPoint.addView(lyricsView,0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    }
                    current_view = CURRENT_VIEW.LYRICS_VIEW;
                }
                else{
                    gotoRec = false;
                    selectSelection(false);
                }
                break;

            case R.id.btn_popup_cancel:
                dialog.dismiss();
                break;

            case R.id.btn_popup_create:
                String val = et_popup_input.getText().toString().trim();
                if (!val.isEmpty()){
                    if (popupAction == PopupAction.CREATE_RECORDING){
                        createProject(val);
                    }
                    else{
                        rename(val);
                    }
                }
                break;

            case R.id.btn_play:
                togglePlay();
                break;

            case R.id.note_view:
                gotoRec = false;
                selectSelection(false);
                break;

            case R.id.img_option:
                try{
                    if (isProject){
                        if (project_bottom_sheet.isAdded()){
                            project_bottom_sheet.dismiss();
                        }
                        project_bottom_sheet.show(getSupportFragmentManager(), project_bottom_sheet.getTag());
                    }
                    else{
                        if (no_project_bottom_sheet.isAdded()){
                            no_project_bottom_sheet.dismiss();
                        }
                        no_project_bottom_sheet.show(getSupportFragmentManager(), no_project_bottom_sheet.getTag());
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;

            case R.id.img_cast:
                audioCast();
                break;


            case R.id.btn_record:
                gotoRec = true;
                selectSelection(true);
                break;

            case R.id.notification_deco:
                if (no_of_recs>0){
                    try{
                        if (mPlayer!=null && mPlayer.isPlaying()){
                            togglePlay();
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        intent = new Intent(PlayActivity.this,MultiTrackMainActivity.class);
                        intent.putExtra(INTENT_PARAM_PROJECT_ID,project.getId());
                        intent.putExtra(INTENT_PARAM_PROJECT_MAIN_FILE,project.getProject_main_recording());
                        intent.putExtra(INTENT_PARAM_PROJECT_NAME,project.getProject_name());
                        startActivityForResult(intent,MULTI_TRACK_REQUEST);
                    }
                }
                break;

            case R.id.play_left:
                navigatePlay(R.id.play_left);
                break;

            case R.id.play_right:
                navigatePlay(R.id.play_right);
                break;

            case R.id.unloop_box:
                if (!loopon){
                    clearPlayer();
                    btn_play.setImageResource(R.drawable.player_play_icon);
                    Intent intent = new Intent(PlayActivity.this, LoopActivity.class);
                    intent.putExtra("MAIN",mainAudio.getAbsolutePath());
                    startActivityForResult(intent,REQUEST_LOOP_ACTIVITY);
                    break;
                }
                else{
                    loopon = false;
                    loop_text.setTextColor(getResources().getColor(R.color.colorPrimary));
                    loop_text.setText(getString(R.string.loop));
                    loop_icon.setImageResource(R.drawable.loop);
                    play();
                }
                break;

            case R.id.btn_allow:
                if (!allowSwitch){
                    allowSwitch = true;
                    btn_allow.setBackgroundResource(R.drawable.app_greenbtn);
                    btn_allow.setTextColor(getResources().getColor(android.R.color.white));
                    btn_not_allow.setBackgroundResource(R.drawable.app_whitebtn);
                    btn_not_allow.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                }
                break;

            case R.id.btn_not_allow:
                if (allowSwitch){
                    allowSwitch = false;
                    btn_allow.setBackgroundResource(R.drawable.app_whitebtn);
                    btn_allow.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
                    btn_not_allow.setBackgroundResource(R.drawable.app_greenbtn);
                    btn_not_allow.setTextColor(getResources().getColor(android.R.color.white));
                }
                break;

            case R.id.btn_cp_url:
                shareDialog.dismiss();
                shareAudio(allowSwitch);
                break;

            case R.id.btn_close:
                shareDialog.dismiss();
                break;

            case R.id.analyze_music:
                toggleAnalyzerView();
                break;

            case R.id.btn_detect:
                if (project==null && audioFile!=null){
                    analyzeAudio();
                }
                else if (project!=null && recordingArrayList.size()>0){
                    analyzeAudio();
                }
                else{
                    Toast.makeText(this, "No Audio File Select", Toast.LENGTH_SHORT).show();
                }
                //checkSubscriptionAndAnalyze(true);
                break;
        }
    }

    private boolean isHeadsetOn() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn();
        } else {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    return true;
                }
            }
        }
        return false;
    }

    private void audioCast(){
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        //Log.e(TAG,audioManager.getMode()+" : "+audioManager.isWiredHeadsetOn()+" : "+audioManager.isSpeakerphoneOn()+" : "+audioManager.isBluetoothScoOn()+" : "+audioManager.getRingerMode());
        alt_bld.setTitle("Audio Output");
        alt_bld.setItems(R.array.audio_output, new DialogInterface
                .OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                try{
                    switch (item){
                        case 0:
                            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                            audioManager.setSpeakerphoneOn(true);
                            break;

                        case 1:
                            audioManager.setMode(AudioManager.MODE_NORMAL);
                            audioManager.setSpeakerphoneOn(false);
                            break;

                        case 2:
                            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                            audioManager.startBluetoothSco();
                            audioManager.setBluetoothScoOn(true);
                            break;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                dialog.dismiss();// dismiss the alertbox after chose option
            }
        }).setCancelable(true);
        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    private void navigatePlay(int playDirection){
        if (isProject){
            int oldInext = recordingIndex;
            numberOfRecordings = recordingArrayList.size();
            switch (playDirection){
                case R.id.play_left:
                    if (recordingIndex>0){
                        recordingIndex--;
                    }else{
                        Toast.makeText(this, "Its First Already", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case R.id.play_right:
                    if (recordingIndex<numberOfRecordings-1){
                        recordingIndex++;
                    }
                    else{
                        Toast.makeText(this, "End Reached", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
            if (oldInext!=recordingIndex){
                Recording recording = recordingArrayList.get(recordingIndex);
                mainAudio = new File(localdir_recordings, recording.getTid());
                if (current_view == CURRENT_VIEW.LYRICS_VIEW){
                    tv_bpm_value.setText(recording.getBpm()+"");
                    tv_key_value.setText(recording.getKey());
                }
                else{
                    insertPoint.removeAllViewsInLayout();
                    tv_bpm_value.setText(recording.getBpm()+"");
                    tv_key_value.setText(recording.getKey());
                    insertPoint.addView(lyricsView,0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
                analyze_music.setImageResource(R.drawable.ic_analyze_music);
                loopon = false;
                loop_text.setTextColor(getResources().getColor(R.color.colorPrimary));
                loop_text.setText(getString(R.string.loop));
                loop_icon.setImageResource(R.drawable.loop);

                if (mPlayer!=null){
                    clearPlayer();
                    isReleased = true;
                }

                play();
            }
        }
        else{
            int oldAudioIndex = audioIndex;
            numberOfAudios = audioFileArrayList.size();
            switch (playDirection){
                case R.id.play_left:
                    if (audioIndex>0){
                        audioIndex--;
                    }else{
                        Toast.makeText(this, "Its First Already", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case R.id.play_right:
                    if (audioIndex<numberOfAudios-1){
                        audioIndex++;
                    }
                    else{
                        Toast.makeText(this, "End Reached", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }

            if (oldAudioIndex!=audioIndex){

                audioFile = audioFileArrayList.get(audioIndex);
                mainAudio = new File(localdir_copytotully, audioFileArrayList.get(audioIndex).getFilename());

                if (current_view == CURRENT_VIEW.LYRICS_VIEW){
                    tv_bpm_value.setText(audioFile.getBpm()+"");
                    tv_key_value.setText(audioFile.getKey());
                }
                else{
                    insertPoint.removeAllViewsInLayout();
                    tv_bpm_value.setText(audioFile.getBpm()+"");
                    tv_key_value.setText(audioFile.getKey());
                    insertPoint.addView(lyricsView,0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
                analyze_music.setImageResource(R.drawable.ic_analyze_music);
                loopon = false;
                loop_text.setTextColor(getResources().getColor(R.color.colorPrimary));
                loop_text.setText(getString(R.string.loop));
                loop_icon.setImageResource(R.drawable.loop);

                if (mPlayer!=null){
                    clearPlayer();
                    isReleased = true;
                }

                tv_projectname.setText("");
                tv_filename.setText(audioFile.getTitle());
                play();
            }
        }
    }

    private void resetAnalyzeView(){
        label_detecting.setVisibility(View.INVISIBLE);
        tv_bpm.setVisibility(View.INVISIBLE);
        tv_key.setVisibility(View.INVISIBLE);
        three_dash_left.setVisibility(View.VISIBLE);
        three_dash_right.setVisibility(View.VISIBLE);
        analyze_icon.setVisibility(View.INVISIBLE);
        analyze_progressbar.setVisibility(View.INVISIBLE);
        btn_detect.setVisibility(View.VISIBLE);
        current_view = CURRENT_VIEW.ANALYZE_VIEW;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE:
                if(resultCode == RESULT_OK) {
                    project = (Project) data.getSerializableExtra(INTENT_PARAM_PROJECT);
                    tv_projectname.setText(project.getProject_name());
                    loopon = data.getBooleanExtra(INTENT_PARAM_LOOPON,false);
                    //isRepeat = data.getBooleanExtra(INTENT_PARAM_ISLOOPING,false);
                    if (loopon){
                        startTime = data.getIntExtra(INTENT_PARAM_LOOPON_START_TIME,0);
                        endTime = data.getIntExtra(INTENT_PARAM_LOOPON_STOP_TIME,0);
                        tv_startTime.setText(formatAudioTime(startTime));
                        tv_endtime.setText(formatAudioTime(endTime));
                        loop_text.setText(getString(R.string.unloop));
                        loop_text.setTextColor(getResources().getColor(R.color.colorAccent));
                        loop_icon.setImageResource(R.drawable.ic_unloop);
                    }
                    else{
                        loop_text.setText(getString(R.string.loop));
                        loop_text.setTextColor(getResources().getColor(R.color.colorPrimary));
                        loop_icon.setImageResource(R.drawable.loop);
                    }
                    makeIsProject();
                }
                break;

            case NEW_REQUEST_CODE:
                if (resultCode == RESULT_OK){
                    project = (Project) data.getSerializableExtra(INTENT_PARAM_PROJECT);
                    loopon = data.getBooleanExtra(INTENT_PARAM_LOOPON,false);
                    //isRepeat = data.getBooleanExtra(INTENT_PARAM_ISLOOPING,false);
                    tv_projectname.setText(project.getProject_name());
                    if (loopon){
                        startTime = data.getIntExtra(INTENT_PARAM_LOOPON_START_TIME,0);
                        endTime = data.getIntExtra(INTENT_PARAM_LOOPON_STOP_TIME,0);
                        loop_text.setText(getString(R.string.unloop));
                        tv_startTime.setText(formatAudioTime(startTime));
                        tv_endtime.setText(formatAudioTime(endTime));
                        loop_text.setTextColor(getResources().getColor(R.color.colorAccent));
                        loop_icon.setImageResource(R.drawable.ic_unloop);
                    }
                    else{
                        loop_text.setText(getString(R.string.loop));
                        loop_text.setTextColor(getResources().getColor(R.color.colorPrimary));
                        loop_icon.setImageResource(R.drawable.loop);
                    }

                    makeIsProject();
                }
                break;

            case REQUEST_LOOP_ACTIVITY:
                if(resultCode == RESULT_OK) {
                    startTime = data.getIntExtra(PARAM_START_TIME,0);
                    endTime = data.getIntExtra(PARAM_END_TIME,0);
                    tv_startTime.setText(formatAudioTime(startTime));
                    tv_endtime.setText(formatAudioTime(endTime));
                    loopon = true;
                    loop_text.setText(getString(R.string.unloop));
                    loop_text.setTextColor(getResources().getColor(R.color.colorAccent));
                    loop_icon.setImageResource(R.drawable.ic_unloop);
                    play();
                }
                break;
        }
    }

    private void selectSelection(boolean startRecording){
        if (isProject){
            resetMplayer();

            if (project!=null && project.getId()!=null){
                intent = new Intent(PlayActivity.this,PlayNoteActivity.class);
                intent.putExtra(INTENT_PARAM_PROJECT,project);
                intent.putExtra(INTENT_PARAM_RECORDING,startRecording);
                //intent.putExtra(INTENT_PARAM_ISLOOPING,isRepeat);

                intent.putExtra(INTENT_PARAM_LOOPON,loopon);
                intent.putExtra(INTENT_PARAM_LOOPON_START_TIME,startTime);
                intent.putExtra(INTENT_PARAM_LOOPON_STOP_TIME,endTime);

                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, btn_play, ViewCompat.getTransitionName(btn_play));

                ActivityCompat.startActivityForResult(this,intent, REQUEST_CODE, options.toBundle());
            }
            else{
                Toast.makeText(this, "Project seems broken, please contact support", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            showCreatePopup();
        }
    }

    private void resetMplayer(){
        if (mPlayer!=null){
            btn_play.setImageResource(R.drawable.player_play_icon);
            musicProgressbar.setProgress(0);
            tv_startTime.setText("00:00");
            clearPlayer();
            mPlayer = null;
        }
    }

    private void togglePlay(){
        if (isReleased || mPlayer==null){
            play();
        }else{
            try{
                if (mPlayer.isPlaying()){
                    mPlayer.pause();
                    isPaused = true;
                    handler.removeCallbacks(MediaObserver);
                    btn_play.setImageResource(R.drawable.player_play_icon);
                }else if (isPaused){
                    mPlayer.start();
                    isPaused = false;
                    btn_play.setImageResource(R.drawable.player_pause_icon);
                    handler.post(MediaObserver);
                }
                else{
                    play();
                }
            }catch (Exception e){
                play();
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        try{
            //7568484Log.e(TAG,progress+"");
            tv_startTime.setText(formatAudioTime(progress));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        handler.removeCallbacks(MediaObserver);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mPlayer!=null){
            try{
                int currentPosition = 0;
                if (mPlayer.isPlaying()){
                    mPlayer.pause();
                    if (loopon){
                        currentPosition  = seekBar.getProgress() + startTime;
                    }
                    else{
                        currentPosition = seekBar.getProgress();
                    }
                    mPlayer.seekTo(currentPosition);
                    mPlayer.start();
                    handler.post(MediaObserver);
                }
                else{
                    if (loopon){
                        currentPosition  = seekBar.getProgress() + startTime;
                    }
                    else{
                        currentPosition = seekBar.getProgress();
                    }
                    mPlayer.seekTo(currentPosition);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    @Override
    public void onFileTap(AudioFile audioFile, int position) {
        loopon = false;
        loop_text.setTextColor(getResources().getColor(R.color.colorPrimary));
        loop_text.setText(getString(R.string.loop));
        loop_icon.setImageResource(R.drawable.loop);
        clearPlayer();
        this.audioFile = audioFile;
        selectCPFile();
    }

    private void clearPlayer(){
        handler.removeCallbacks(MediaObserver);
        if (mPlayer!=null){
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

    Runnable MediaObserver = new Runnable() {
        static final long PROGRESS_UPDATE = 500;
        private int currenttime;

        @Override
        public void run() {
            try {

                if (loopon && currenttime>=endTime){
                    mPlayer.seekTo(startTime);
                }

                currenttime = mPlayer.getCurrentPosition();
                if (loopon){
                    musicProgressbar.setProgress(currenttime-startTime);
                }
                else{
                    musicProgressbar.setProgress(currenttime);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    tv_startTime.setText(formatAudioTime(currenttime));
                    }
                });
                handler.postDelayed(this,PROGRESS_UPDATE);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mixpanel = MixpanelAPI.getInstance(this, YOUR_PROJECT_TOKEN);
    }

    private void createProject(String projectName){
        mixpanel.track("Creating project");
        if (audioFile.getId().equals(getString(R.string.free_beat_id))){
            mixpanel.track("Free Beat Project Created");
        }
        dialog.dismiss();
        resetMplayer();
        File localFile = new File(localdir_copytotully, audioFile.getFilename());

        if (localFile.exists()){

            File recFile = new File(localdir_recordings, localFile.getName());

            try {
                FileChannel inChannel = new FileInputStream(localFile).getChannel();
                FileChannel outChannel = new FileOutputStream(recFile).getChannel();
                try {
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                    Log.e("NEW FILE",recFile.getPath()+" : "+recFile.length()+"");
                } finally {
                    if (inChannel != null)
                        inChannel.close();
                    outChannel.close();
                }
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }

            String mime = getMime(localFile.getAbsolutePath());
            String projectid = mDatabase.child("projects").push().getKey();
            mDatabase.child("projects").child(projectid).child("project_name").setValue(projectName);
            mDatabase.child("projects").child(projectid).child("project_main_recording").setValue(audioFile.getFilename());
            mDatabase.child("projects").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()){
                        long count = dataSnapshot.getChildrenCount();
                        String eventName = "";

                        if (count==5){
                            eventName = "5 Projects Created";
                        }
                        else if (count == 20){
                            eventName = "20 Projects Created";
                        }
                        else if (count == 50){
                            eventName = "50 Projects Created";
                        }
                        else if (count == 75){
                            eventName = "75 Projects Created";
                        }
                        else if (count >= 100){
                            eventName = "100+ Projects Created";
                        }

                        if (!eventName.isEmpty()) mixpanel.track(eventName);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            String recordingId = mDatabase.child("projects").child(projectid).child("recordings").push().getKey();

            String lyricsKey = mDatabase.child("projects").child(projectid).child("lyrics").push().getKey();

            Recording recording = new Recording();
            recording.setId(recordingId);
            recording.setTid(audioFile.getFilename());
            recording.setName(audioFile.getTitle());
            recording.setMime(mime);
            recording.setProjectId(projectid);
            recording.setSize(localFile.length());

            Project project = new Project();
            project.setId(projectid);
            project.setProject_main_recording(audioFile.getFilename());
            project.setProject_name(projectName);

            HashMap<String,Recording> recordingsHashMap = new HashMap<>();
            recordingsHashMap.put(recordingId,recording);
            project.setRecordings(recordingsHashMap);

            HashMap<String,Lyrics> lyricsHashMap = new HashMap<>();
            Lyrics lyrics = new Lyrics();
            lyrics.setId(lyricsKey);
            lyrics.setDesc("");
            lyrics.setProjectID(projectid);
            lyricsHashMap.put(lyricsKey,lyrics);
            project.setLyrics(lyricsHashMap);

            FirebaseDatabaseOperations.startActionUploadProjectRecording(PlayActivity.this,recording,localFile);
            Intent intent = new Intent(PlayActivity.this,PlayNoteActivity.class);
            intent.putExtra(INTENT_PARAM_PROJECT,project);
            intent.putExtra(INTENT_PARAM_LOOPON,loopon);
            intent.putExtra(INTENT_PARAM_LOOPON_START_TIME,startTime);
            intent.putExtra(INTENT_PARAM_LOOPON_STOP_TIME,endTime);
            if (gotoRec) {
                intent.putExtra(INTENT_PARAM_RECORDING,true);
                intent.putExtra("PROJECTID",project.getId());
                intent.putExtra("MAIN",project.getProject_main_recording());
            }
            startActivityForResult(intent,NEW_REQUEST_CODE);
        }
        else{
            Toast.makeText(this, "local file does not exist", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (loopon){
            mPlayer.seekTo(startTime);
            tv_startTime.setText(formatAudioTime(startTime));
            musicProgressbar.setMax(endTime-startTime);
            duration = endTime;
        }
        else{
            duration = mPlayer.getDuration();
            Log.e(TAG,duration+"");
            tv_startTime.setText(formatAudioTime(0));
            musicProgressbar.setMax(duration);
        }
        mPlayer.start();
        tv_endtime.setText(formatAudioTime(duration));
        handler.post(MediaObserver);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        handler.removeCallbacks(MediaObserver);
        clearPlayer();
        musicProgressbar.setProgress(0);
        tv_startTime.setText(getString(R.string._00_00));
        isReleased = true;
        btn_play.setImageResource(R.drawable.player_play_icon);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_invite, menu);
        if(project != null) {
            menu.findItem(R.id.action_invite_collaborator).setVisible(true);
        } else {
            menu.findItem(R.id.action_invite_collaborator).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_invite_collaborator:
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                    isPaused = true;
                    handler.removeCallbacks(MediaObserver);
                    btn_play.setImageResource(R.drawable.player_play_icon);
                }
                checkCollaborationSubscription();
                /*Intent intent = new Intent(PlayActivity.this, CollaborationActivity.class);
                intent.putExtra(INTENT_PARAM_PROJECT, project);
                startActivity(intent);*/
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }


    private void checkCollaborationSubscription() {
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid() + "/settings/collaboration_subscription");

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    //isSubscribed = dataSnapshot.getValue(Boolean.class);
                    isSubscribed = (boolean) dataSnapshot.child("is_subscribe").getValue();
                }
                if(!isSubscribed) {
                    intent = new Intent(PlayActivity.this, SubscribeActivity.class);
                    startActivity(intent);
                } else {
                    if(project != null) {
                        intent = new Intent(PlayActivity.this, InviteActivity.class);
                        intent.putExtra(INTENT_PARAM_PROJECT, project);
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                System.out.println("Error : " + databaseError.getMessage());
            }
        });
    }

    @Override
    public void onBackPressed() {
        try{
            handler.removeCallbacks(MediaObserver);
        }catch (Exception e){
            e.printStackTrace();
        }
        clearPlayer();
        try{
            dialog.dismiss();
        }catch (Exception e){
            e.printStackTrace();
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        unregisterBroadcast();
        super.onDestroy();
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

    @Override
    public void onItemClicked(int id) {
        switch (id){
            case R.id.bsb_create:
                gotoRec = false;
                selectSelection(false);
                no_project_bottom_sheet.dismiss();
                break;

            case R.id.bsb_share_np:
                if (mainAudio.exists()){
                    if (!audioFile.getId().equals(getString(R.string.free_beat_id))){
                        shareDialog.show();
                    }
                    else{
                        shareAudioFile(audioFile,true);
                    }
                }
                no_project_bottom_sheet.dismiss();
                break;

            case R.id.bsb_rename:
                showRenamePopup();
                project_bottom_sheet.dismiss();
                break;

            case R.id.bsb_share_p:
                if (mainAudio.exists()){
                    //shareAudio();
                    shareDialog.show();
                }
                project_bottom_sheet.dismiss();
                break;
        }
    }

    private void rename(final String value){
        mDatabase.child("projects").child(project.getId()).child("project_name").setValue(value);
        project.setProject_name(value);
        tv_projectname.setText(value);
        dialog.dismiss();
        Toast.makeText(this, "Project Renamed", Toast.LENGTH_SHORT).show();
    }

    private void shareAudio(boolean b){
        if (isProject){
            shareProject(project,b);
        }
        else{
            shareAudioFile(audioFile,b);
        }
    }

    private void shareProject(Project project, boolean b){
        if (isInternetAvailable(this)){
            progressBar.setVisibility(View.VISIBLE);
            boolean available = false;
            for(Object o : project.getRecordings().entrySet()){
                Map.Entry pair = (Map.Entry) o;
                Recording rec = (Recording) pair.getValue();
                if (project.getProject_main_recording().equals(rec.getTid())){
                    if (rec.getDownloadURL()!=null){
                        available = true;
                        break;
                    }
                }
            }

            JSONArray configArr = new JSONArray();
            JSONObject config = new JSONObject();
            try {
                config.put("allow_download",b);
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
                params.put("userid", mAuth.getCurrentUser().getUid());
                params.put("projectid", project.getId());
                params.put("config",configArr.toString());
                client.post(APIs.SHARE_PROJECT, params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        progressBar.setVisibility(View.GONE);
                        String responseString = new String(responseBody);
                        Log.e("RESPONSE",responseString);
                        try {
                            JSONObject response = new JSONObject(responseString);
                            shareLink(response.getJSONObject("data").getString("link"),"Share Project");
                            Toast.makeText(PlayActivity.this, response.getString("msg"), Toast.LENGTH_SHORT).show();
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Toast.makeText(PlayActivity.this, "Network Fail", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        error.printStackTrace();
                    }

                });
            }
            else{
                Toast.makeText(PlayActivity.this, "File Still uploading or is broken", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        }
        else{
            Toast.makeText(PlayActivity.this, "Network Connection Issue", Toast.LENGTH_SHORT).show();
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
            params.put("ids", audioFile.getId());
            params.put("config",configArr.toString());
            client.post(APIs.SHARE_AUDIO, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    progressBar.setVisibility(View.GONE);
                    String responseString = new String(responseBody);
                    Log.e("RESPONSE",responseString);
                    try {
                        JSONObject response = new JSONObject(responseString);
                        shareLink(response.getJSONObject("data").getString("link"),"Share Audio");
                        Toast.makeText(PlayActivity.this, response.getString("msg"), Toast.LENGTH_SHORT).show();
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
        else{
            Toast.makeText(PlayActivity.this, "Audio still uploading or broken", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_ANALYZE_PROGRESS);
        LocalBroadcastManager.getInstance(PlayActivity.this).registerReceiver(responseReceiver, filter);
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(PlayActivity.this).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case ACTION_ANALYZE_PROGRESS:
                    bpm = Math.round(intent.getFloatExtra(INTENT_PARAM_BPM,0f));
                    key = intent.getStringExtra(INTENT_PARAM_KEY);
                    label_detecting.setVisibility(View.GONE);
                    tv_bpm.setText(bpm+"");
                    tv_key.setText(key);
                    tv_bpm_value.setText(bpm+"");
                    tv_key_value.setText(key);
                    tv_bpm.setVisibility(View.VISIBLE);
                    tv_key.setVisibility(View.VISIBLE);
                    three_dash_left.setVisibility(View.GONE);
                    three_dash_right.setVisibility(View.GONE);
                    analyze_icon.setImageResource(R.drawable.bpm_done);
                    analyze_progressbar.setVisibility(View.INVISIBLE);
                    if (project!=null){
                        if (recordingArrayList.size()>0){
                            Recording recording = recordingArrayList.get(recordingIndex);
                            Log.e(TAG,recording.getProjectId());
                            Log.e(TAG,recording.getId());
                            recording.setBpm(bpm);
                            recording.setKey(key);
                            FirebaseDatabaseOperations.updateBmpandKeyRecording(PlayActivity.this,recording);
                        }
                        else{
                            Toast.makeText(context, "No Recording Exist", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else{
                        audioFile.setBpm(bpm);
                        audioFile.setKey(key);
                        FirebaseDatabaseOperations.updateBmpandKeyAudioFile(PlayActivity.this,audioFile);
                    }
                    break;
            }
        }
    }

    @Override
    public void onSucessfullSubscription() {
        analyzerSubscribed = true;
    }

    private void toggleAnalyzerView(){
        if (current_view == CURRENT_VIEW.ANALYZE_VIEW){
            insertPoint.removeAllViewsInLayout();
            insertPoint.addView(lyricsView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            current_view = CURRENT_VIEW.LYRICS_VIEW;
            analyze_music.setImageResource(R.drawable.ic_analyze_music);
        }
        else{
            actionAnalyzeorView(false);
            analyze_music.setImageResource(R.drawable.ic_analyze_music_green);
        }
    }

    private void analyzeAudio(){
        actionAnalyzeorView(true);
    }

    private void checkSubscriptionAndAnalyze(final boolean detect){
        if (!analyzerSubscribed){
            mDatabase.child("settings").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()){
                        try{
                            if (dataSnapshot.hasChild("audioAnalyzer") && dataSnapshot.child("audioAnalyzer").hasChild("freeTrials")){
                                freeTrials = (long) dataSnapshot.child("audioAnalyzer").child("freeTrials").getValue();
                            }
                            else{
                                freeTrials = (detect ? 1 : 0);
                            }
                            if (dataSnapshot.hasChild("audioAnalyzer") && dataSnapshot.child("audioAnalyzer").hasChild("isActive")){
                                boolean subscribed = (boolean) dataSnapshot.child("audioAnalyzer").child("isActive").getValue();
                                if (subscribed){
                                    actionAnalyzeorView(detect);
                                }
                                else{
                                    if (freeTrials < 5){
                                        if (detect){
                                            dataSnapshot.child("audioAnalyzer").child("freeTrials").getRef().setValue(freeTrials+1);
                                        }
                                        actionAnalyzeorView(detect);
                                    }
                                    else{
                                        analyzerSubscription();
                                    }
                                }
                            }
                            else{
                                if (freeTrials < 5){
                                    if (detect){
                                        dataSnapshot.child("audioAnalyzer").child("freeTrials").getRef().setValue(freeTrials+1);
                                    }
                                    actionAnalyzeorView(detect);
                                }
                                else{
                                    analyzerSubscription();
                                }
                            }
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    else{
                        Log.e(TAG,"SETTING NOT EXIST");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else{
            actionAnalyzeorView(detect);
        }
    }

    private void actionAnalyzeorView(boolean detect){
        analyze_music.setImageResource(R.drawable.ic_analyze_music_green);
        if (detect){
            label_detecting.setVisibility(View.VISIBLE);
            analyze_icon.setVisibility(View.VISIBLE);
            analyze_icon.setImageResource(R.drawable.ic_analyze_icon);
            analyze_progressbar.setVisibility(View.VISIBLE);
            //tv_percent.setVisibility(View.VISIBLE);
            btn_detect.setVisibility(View.GONE);
            AudioAnalyzeService.startAnalyzingAudio(PlayActivity.this,mainAudio.getAbsolutePath());
            current_view = CURRENT_VIEW.ANALYZE_VIEW;
        }
        else{
            insertPoint.removeAllViewsInLayout();
            insertPoint.addView(analyzeView,0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            resetAnalyzeView();
        }
    }

    private void analyzerSubscription(){
        analyzeSubscriptionDialogFragment = AnalyzeSubscriptionDialogFragment.newInstance();
        analyzeSubscriptionDialogFragment.setSubscriptionEvents(PlayActivity.this);
        analyzeSubscriptionDialogFragment.show(getSupportFragmentManager(),AnalyzeSubscriptionDialogFragment.class.getSimpleName());
    }
}