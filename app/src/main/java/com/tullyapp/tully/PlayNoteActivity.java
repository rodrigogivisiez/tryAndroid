package com.tullyapp.tully;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.tullyapp.tully.Adapters.LyricsPopupListAdapter;
import com.tullyapp.tully.CustomView.RecorderVisualizerView;
import com.tullyapp.tully.Dictionary.SearchRhym;
import com.tullyapp.tully.FirebaseDataModels.Lyrics;
import com.tullyapp.tully.FirebaseDataModels.LyricsModule.LyricsAppModel;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.Models.LyricsWordSynonym;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.ExtendedEditText;
import com.tullyapp.tully.Utils.NotifyingScrollView;
import com.tullyapp.tully.Utils.ViewUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import cz.msebera.android.httpclient.Header;

import static android.view.Gravity.TOP;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.tullyapp.tully.LoopActivity.PARAM_END_TIME;
import static com.tullyapp.tully.LoopActivity.PARAM_START_TIME;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_ISLOOPING;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_LOOPON;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_LOOPON_START_TIME;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_LOOPON_STOP_TIME;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT_ID;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT_MAIN_FILE;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT_NAME;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_RECORDING;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_RECORDINGS;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Utils.formatAudioTime;
import static com.tullyapp.tully.Utils.Utils.getDirectory;
import static com.tullyapp.tully.Utils.Utils.getFileName;
import static com.tullyapp.tully.Utils.Utils.getMime;
import static com.tullyapp.tully.Utils.Utils.hideSoftKeyboard;
import static com.tullyapp.tully.Utils.Utils.isInternetAvailable;

public class PlayNoteActivity extends AppCompatActivity implements View.OnClickListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener, ExtendedEditText.SelectionChangeListener, NotifyingScrollView.ScrollChangeListener, LyricsPopupListAdapter.LyricsPopupClickEventsListener, SearchRhym.OfflineRhym {

    private static final long REPEAT_INTERVAL = 50;
    private static final int REQUEST_LOOP_ACTIVITY = 235;
    public static final int MULTI_TRACK_REQUEST = 32781;
    private RelativeLayout recording_layout, etparent;
    private LinearLayout player_layout, unloop_box, bpm_bar;
    private ExtendedEditText et_lyrics;
    private ImageView btn_record, loop_icon, btn_play;
    private TextView tv_startTime, tv_endtime, tv_rec_counter, loop_text, tv_word_info, tv_bpm_value, tv_key_value;
    private MediaObserver observer;
    private MediaPlayer mPlayer;
    private boolean isPaused = false;
    private boolean isRepeat = false;
    private AppCompatSeekBar musicProgressbar;
    private String enteredText, recordingFileName, projectId;
    private LyricsAppModel lyricsAppModel = new LyricsAppModel();
    private String lastSavedLyrics = "";
    private File localdir_recordings, recordingFile, localFile;
    private MediaRecorder mRecorder;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private String [] permissions = {android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE};
    private Chronometer mChronometer;
    private boolean isRecording = false;
    private boolean permissionToRecordAccepted = false;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private int recordingCount = 0;
    private boolean isFullView = false;
    private Project project;
    private Lyrics lyrics;
    private final PopupWindow popupWindow = new PopupWindow();
    private LyricsPopupListAdapter lyricsPopupListAdapter;
    private ArrayList<LyricsWordSynonym> lyricsWordSynonymArrayList = new ArrayList<>();
    private ProgressBar popup_progressbar;
    private static final int DEFAULT_WIDTH = -1;
    private static final int DEFAULT_HEIGHT = -1;

    private boolean loopon = false;

    private final Point currLoc = new Point();
    private final Point startLoc = new Point();

    private final Rect cbounds = new Rect();
    private Dialog infoPopupDialog;

    private Handler handler = new Handler();
    private RecorderVisualizerView visualizerView;
    private boolean startRecording;
    private MixpanelAPI mixpanel;
    private SearchRhym searchRhym;
    private String TAG = PlayNoteActivity.class.getSimpleName();
    private int startTime;
    private int endTime;
    private ImageView notification_deco;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_note);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Play – Create - Rhyme");
        mAuth = FirebaseAuth.getInstance();

        project = (Project) getIntent().getSerializableExtra(INTENT_PARAM_PROJECT);
        startRecording = getIntent().getBooleanExtra(INTENT_PARAM_RECORDING,false);
        isRepeat = getIntent().getBooleanExtra(INTENT_PARAM_ISLOOPING,false);

        loopon = getIntent().getBooleanExtra(INTENT_PARAM_LOOPON,false);
        if (loopon){
            startTime = getIntent().getIntExtra(INTENT_PARAM_LOOPON_START_TIME,0);
            endTime = getIntent().getIntExtra(INTENT_PARAM_LOOPON_STOP_TIME,0);
        }

        localdir_recordings = getDirectory(PlayNoteActivity.this,LOCAL_DIR_NAME_RECORDINGS);
        
        mixpanel = MixpanelAPI.getInstance(this, YOUR_PROJECT_TOKEN);

        try{
            projectId = project.getId();
            localFile = new File(localdir_recordings, project.getProject_main_recording());
            if (project.getLyrics()!=null){
                Iterator iterator = project.getLyrics().entrySet().iterator();
                if (iterator.hasNext()){
                    Map.Entry pair = (Map.Entry) iterator.next();
                    lyrics = (Lyrics) pair.getValue();
                    lyrics.setId(pair.getKey().toString());
                    lastSavedLyrics = lyrics.getDesc();
                    lyricsAppModel.setLyrics(lyrics);
                    lyricsAppModel.getLyrics().setId(lyrics.getId());
                    lyricsAppModel.setProjectId(projectId);
                    lyricsAppModel.setOfProject(true);
                }
                else{
                    initLyrics();
                }
            }
            else{
                initLyrics();
            }

            if (project.getRecordings()!=null) {
                recordingCount = project.getRecordings().size();
                recordingCount = Math.abs(recordingCount-1);
            }

            initUI();

            searchRhym = new SearchRhym(this);
            searchRhym.setOfflineRhymListener(this);
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "Broken Project, please contact support", Toast.LENGTH_SHORT).show();
        }
    }

    private void initLyrics(){
        lyrics = new Lyrics();
        project.setLyrics(new HashMap<String, Lyrics>());
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid()).child("projects").child(projectId);
        String lyricsId = mDatabase.child("lyrics").push().getKey();
        lyrics.setId(lyricsId);
        lyricsAppModel.setLyrics(lyrics);
        lyricsAppModel.setProjectId(projectId);
        lyricsAppModel.setOfProject(true);
        mixpanel.track("Writing lyrics in project");
    }

    private void initUI(){
        recording_layout = findViewById(R.id.recording_layout);
        player_layout = findViewById(R.id.player_layout);
        et_lyrics = findViewById(R.id.et_lyrics);
        etparent = findViewById(R.id.etparent);
        tv_startTime = findViewById(R.id.tv_startTime);
        tv_endtime= findViewById(R.id.tv_endtime);
        mChronometer = findViewById(R.id.chronometer);
        tv_rec_counter = findViewById(R.id.tv_rec_counter);
        notification_deco = findViewById(R.id.notification_deco);
        visualizerView  = findViewById(R.id.visualizer);

        loop_icon = findViewById(R.id.loop_icon);

        unloop_box = findViewById(R.id.unloop_box);
        loop_text = findViewById(R.id.loop_text);

        unloop_box.setOnClickListener(this);

        TextView tv_projectname = findViewById(R.id.tv_projectname);
        TextView tv_filename = findViewById(R.id.tv_filename);

        infoPopupDialog = new Dialog(PlayNoteActivity.this, R.style.MyDialogTheme);
        infoPopupDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        infoPopupDialog.setContentView(R.layout.word_info_popup);
        infoPopupDialog.setCancelable(true);
        infoPopupDialog.setCanceledOnTouchOutside(true);
        tv_word_info = infoPopupDialog.findViewById(R.id.tv_word_info);
        ImageView btn_close = infoPopupDialog.findViewById(R.id.btn_close);

        LayoutInflater inflater = LayoutInflater.from(this);
        View tooltip_layout = inflater.inflate(R.layout.tooltip_layout, null);

        tooltip_layout.bringToFront();

        popupWindow.setContentView(tooltip_layout);
        popupWindow.setWidth(WRAP_CONTENT);
        popupWindow.setHeight(WRAP_CONTENT);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);
        popupWindow.setFocusable(false);

        try{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                popupWindow.setOverlapAnchor(true);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }


        RecyclerView recycle_view = tooltip_layout.findViewById(R.id.recycle_view);
        popup_progressbar = tooltip_layout.findViewById(R.id.popup_progressbar);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recycle_view.setLayoutManager(linearLayoutManager);

        lyricsPopupListAdapter = new LyricsPopupListAdapter(this,lyricsWordSynonymArrayList,this);
        recycle_view.setAdapter(lyricsPopupListAdapter);

        tv_rec_counter.setText(recordingCount+"");

        notification_deco.setOnClickListener(this);
        btn_close.setOnClickListener(this);

        musicProgressbar = findViewById(R.id.appCompatSeekBar);
        musicProgressbar.setOnSeekBarChangeListener(this);

        btn_play = findViewById(R.id.btn_play);
        btn_record = findViewById(R.id.btn_record);

        btn_play.setOnClickListener(this);
        btn_record.setOnClickListener(this);

        if (lyricsAppModel.getLyrics() !=null && lyricsAppModel.getLyrics().getDesc()!=null)
            et_lyrics.setText(lyricsAppModel.getLyrics().getDesc());

            et_lyrics.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        enteredText = s.toString();
                        if (enteredText.length()>0 && enteredText.length()%50==0){
                            saveLyrics(enteredText.trim());
                        }
                    }

            });

        et_lyrics.setEditBackPressedListener(new ExtendedEditText.EditBackPressedListener() {
            @Override
            public void onEditBackPressed() {
            if (!isFullView){
                if (isRecording)
                    recording_layout.setVisibility(View.VISIBLE);
                else
                    recording_layout.setVisibility(View.GONE);
                etparent.requestFocus();
            }
            }
        });

        et_lyrics.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus){
                recording_layout.setVisibility(View.GONE);
            }
            }
        });

        et_lyrics.setSelectionChangeListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            popupWindow.setOverlapAnchor(true);
            popupWindow.setElevation(10);
        }

        tv_projectname.setText(project.getProject_name());
        tv_filename.setText(project.getMainFileTitle());

        if (!startRecording){
            et_lyrics.requestFocusFromTouch();
            et_lyrics.requestFocus();
        }

        if (loopon){
            loop_text.setText(getString(R.string.unloop));
            loop_text.setTextColor(getResources().getColor(R.color.colorAccent));
            loop_icon.setImageResource(R.drawable.ic_unloop);
        }
        else{
            loop_text.setText(getString(R.string.loop));
            loop_text.setTextColor(getResources().getColor(R.color.colorPrimary));
            loop_icon.setImageResource(R.drawable.loop);
        }

        Recording recording = getMainAudioObject();
        if (recording.getKey()!=null){
            bpm_bar = findViewById(R.id.bpm_bar);
            bpm_bar.setVisibility(View.VISIBLE);
            tv_bpm_value = findViewById(R.id.tv_bpm_value);
            tv_key_value = findViewById(R.id.tv_key_value);
            tv_bpm_value.setText(recording.getBpm()+"");
            tv_key_value.setText(recording.getKey());
        }

        observer = new MediaObserver();
        togglePlay();

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try{
            switch (requestCode){
                case REQUEST_RECORD_AUDIO_PERMISSION:
                    permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (permissionToRecordAccepted){
                        if (startRecording){
                            btn_record.performClick();
                        }
                    }
                    break;
            }
            if (!permissionToRecordAccepted ) Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void saveLyrics(String txt){
        lastSavedLyrics = txt;
        if (lastSavedLyrics.isEmpty()){
            if (project.getProject_main_recording()!=null && project.getProject_main_recording().equals(getString(R.string.free_beat_file_name))){
                mixpanel.track("Free Beat lyrics");
            }
        }
        lyricsAppModel.getLyrics().setDesc(txt);
        mixpanel.track("Update lyrics in project");
        Toast.makeText(this, "Saving lyrics", Toast.LENGTH_SHORT).show();
        FirebaseDatabaseOperations.startActionsaveLyric(getApplicationContext(),lyricsAppModel);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.btn_play:
                togglePlay();
                break;

            case R.id.btn_record:
                isRecording = !isRecording;
                View view = this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                recording_layout.setVisibility(View.VISIBLE);
                onRecord(isRecording);
                break;

            case R.id.notification_deco:
                if (recordingCount>0 && !isRecording){
                    try{
                        if (mPlayer!=null && mPlayer.isPlaying()){
                            togglePlay();
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        Intent intent = new Intent(PlayNoteActivity.this, MultiTrackMainActivity.class);
                        intent.putExtra(INTENT_PARAM_PROJECT_ID,project.getId());
                        intent.putExtra(INTENT_PARAM_PROJECT_MAIN_FILE,project.getProject_main_recording());
                        intent.putExtra(INTENT_PARAM_PROJECT_NAME,project.getProject_name());
                        startActivityForResult(intent,MULTI_TRACK_REQUEST);
                    }
                }
                break;

            case R.id.btn_close:
                infoPopupDialog.dismiss();
                break;

            case R.id.unloop_box:
                if (!loopon){
                    btn_play.setImageResource(R.drawable.player_play_icon);
                    clearPlayer();
                    Intent intent = new Intent(PlayNoteActivity.this, LoopActivity.class);
                    intent.putExtra("MAIN",localFile.getAbsolutePath());
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
        }
    }

    private void createAndUploadRecording(String val){
        try{
            Recording recording = new Recording();
            recording.setTid(recordingFileName);
            recording.setOfProject(true);
            recording.setProjectId(projectId);
            recording.setName(val);
            recording.setSize(recordingFile.length());
            recording.setMime(getMime(recordingFile.getAbsolutePath()));
            mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid()).child("projects");
            String recordingId = mDatabase.child(projectId).child("recording").push().getKey();
            recording.setId(recordingId);
            project.getRecordings().put(recordingId, recording);
            FirebaseDatabaseOperations.startActionUploadProjectRecording(PlayNoteActivity.this, recording,recordingFile);
            mixpanel.track("Recording in project");
            if (project.getProject_main_recording()!=null && project.getProject_main_recording().equals(getString(R.string.free_beat_file_name))){
                mixpanel.track("Free Beat Recording");
            }
            recordingCount++;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            et_lyrics.requestFocusFromTouch();
            et_lyrics.requestFocus();
            tv_rec_counter.setText(recordingCount+"");
        }
    }

    private Recording getMainAudioObject(){
        if (project.getRecordings()!=null){
            for (Object o : project.getRecordings().entrySet()) {
                Map.Entry pair = (Map.Entry) o;
                Recording recording = (Recording) pair.getValue();

                if (recording.getTid().equals(project.getProject_main_recording())) {
                    return recording;
                }
            }
        }
        return null;
    }

    private void play(){
        clearPlayer();

        String path = "";

        if (localFile.exists()){
            path = localFile.getAbsolutePath();
        }
        else{
            Recording recording = getMainAudioObject();
            if (recording!= null && recording.getDownloadURL() != null) {
                path = recording.getDownloadURL();
            }
        }

        if (!path.isEmpty()){
            try {
                isPaused = false;
                btn_play.setImageResource(R.drawable.player_pause_icon);
                mPlayer = new MediaPlayer();
                mPlayer.setDataSource(path);
                mPlayer.setLooping(isRepeat);
                mPlayer.prepareAsync();
                mPlayer.setOnPreparedListener(this);
                mPlayer.setOnCompletionListener(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            Toast.makeText(this, "Seems audio file is broken", Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePlay(){
        try{
            if (mPlayer == null){
                play();
            }else if (mPlayer.isPlaying()){
                mPlayer.pause();
                isPaused = true;
                observer.stop();
                btn_play.setImageResource(R.drawable.player_play_icon);
            }else if (isPaused){
                mPlayer.start();
                isPaused = false;
                btn_play.setImageResource(R.drawable.player_pause_icon);
                new Thread(observer).start();
            }
            else{
                play();
            }
        }catch (Exception e){
            play();
        }

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        try{
            int duration;
            if (loopon){
                mPlayer.seekTo(startTime);
                tv_startTime.setText(formatAudioTime(startTime));
                musicProgressbar.setMax(endTime-startTime);
                duration = endTime;
            }
            else{
                duration = mPlayer.getDuration();
                tv_startTime.setText(formatAudioTime(0));
                musicProgressbar.setMax(duration);
            }

            mPlayer.start();
            tv_endtime.setText(formatAudioTime(duration));

            new Thread(observer).start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        clearPlayer();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        try{
            tv_startTime.setText(formatAudioTime(progress));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        observer.stop();
        Log.e(TAG,"OBSERVER STOP");
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

                    Log.e(TAG,"POST : "+currentPosition);

                    mPlayer.seekTo(currentPosition);
                    mPlayer.start();

                    new Thread(observer).start();
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
    public void showWordInfoListener(String word, JSONArray description) {
        try {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            SpannableString str1= new SpannableString(word+" - ");
            str1.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), 0, str1.length(), 0);
            str1.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, str1.length(), 0);
            builder.append(str1);

            if (description!=null){
                int len = description.length();
                for (int i = 0; i<len; i++){
                    builder.append(description.get(i).toString());
                }
            }

            tv_word_info.setText(builder, TextView.BufferType.SPANNABLE);
            infoPopupDialog.show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void itemSelected(String word) {
        int start = Math.max(et_lyrics.getSelectionStart(), 0);
        int end = Math.max(et_lyrics.getSelectionEnd(), 0);
        et_lyrics.getText().replace(Math.min(start, end), Math.max(start, end), word, 0, word.length());
        popupWindow.dismiss();
        //saveLyrics(et_lyrics.getText().toString());
    }

    @Override
    public void onRhymReceive(ArrayList<String> rhymStrings) {
        popup_progressbar.setVisibility(View.GONE);
        if (rhymStrings.size()>0){

            LyricsWordSynonym lyricsWordSynonym = new LyricsWordSynonym("", false, null);
            lyricsWordSynonymArrayList.add(lyricsWordSynonym);

            for (String rhym : rhymStrings){
                lyricsWordSynonymArrayList.add(new LyricsWordSynonym(
                    rhym,
                    false,
                    null
                ));
            }

            lyricsWordSynonym = new LyricsWordSynonym("",false,null);
            lyricsWordSynonymArrayList.add(lyricsWordSynonym);
            lyricsPopupListAdapter.notifyDataSetChanged();

        } else{
            popupWindow.dismiss();
        }
    }

    private class MediaObserver implements Runnable {
        private AtomicBoolean stop = new AtomicBoolean(false);
        private int currenttime;
        void stop() {
            stop.set(true);
        }
        @Override
        public void run() {
            stop.set(false);
            while (!stop.get()) {
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
                }catch (Exception e){
                    e.printStackTrace();
                    this.stop();
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (observer!=null)
            observer.stop();

        if (mPlayer!=null){
            try {
                mPlayer.stop();
            }catch (Exception e){
                e.printStackTrace();
            }
            try{
                mPlayer.release();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        if (isRecording){
            clearRecorder();
            try{
                recordingFile.delete();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        Intent intent = new Intent();
        intent.putExtra(INTENT_PARAM_PROJECT,project);
        intent.putExtra(INTENT_PARAM_LOOPON,loopon);
        intent.putExtra(INTENT_PARAM_LOOPON_START_TIME,startTime);
        intent.putExtra(INTENT_PARAM_LOOPON_STOP_TIME,endTime);
        intent.putExtra(INTENT_PARAM_ISLOOPING,isRepeat);

        if (!lastSavedLyrics.equals(et_lyrics.getText().toString().trim())) {
            saveLyrics(et_lyrics.getText().toString().trim());
            lyricsAppModel.getLyrics().setDesc(et_lyrics.getText().toString().trim());
            project.getLyrics().put(lyricsAppModel.getLyrics().getId(),lyricsAppModel.getLyrics());
        }

        setResult(Activity.RESULT_OK, intent);
        super.onBackPressed();
    }

    /*
    -----------------
    Recoding Module
    -----------------
    */

    private void onRecord(boolean start) {
        if (start) {
            etparent.requestFocus();
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        if (permissionToRecordAccepted){
            try {
                recordingFileName = getFileName()+".wav";
                recordingFile = new File(localdir_recordings, recordingFileName);
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(AudioFormat.ENCODING_PCM_16BIT);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mRecorder.setAudioChannels(2);
                mRecorder.setAudioEncodingBitRate(128000);
                mRecorder.setAudioSamplingRate(44100);
                mRecorder.setOutputFile(recordingFile.getPath());
                mRecorder.prepare();
                mRecorder.start();
                handler.post(updateVisualizer);
                btn_record.setImageResource(R.drawable.record_icon);
                startRecordingTimer();
                getSupportActionBar().setTitle("Play – Record");
            } catch (Exception e) {
                e.printStackTrace();
                clearRecorder();
                recording_layout.setVisibility(View.GONE);
                getSupportActionBar().setTitle("Play – Create - Rhyme");
            }
        }
        else{
            startRecording = true;
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    private void startRecordingTimer(){
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.start();
    }

    private void stopRecording() {
        clearRecorder();
        recording_layout.setVisibility(View.GONE);
        getSupportActionBar().setTitle("Play – Create - Rhyme");
        // showPopup();
        String val = "Recording - "+String.valueOf(1+recordingCount);
        createAndUploadRecording(val);
        Toast.makeText(this, val+" Saved", Toast.LENGTH_SHORT).show();
    }

    /*private void showPopup(){
        et_popup_input.setText("");
        et_popup_input.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
    }*/

    private void clearRecorder(){
        try {
            handler.removeCallbacks(updateVisualizer);
            visualizerView.clear();
            visualizerView.invalidate();
            btn_record.setImageResource(R.drawable.player_record_icon);
            mChronometer.setBase(SystemClock.elapsedRealtime());
            mChronometer.stop();
            mRecorder.stop();
            mRecorder.release();
            isRecording = false;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void clearPlayer(){
        if (observer!=null) observer.stop();

        if (mPlayer!=null){
            try{
                mPlayer.stop();
                mPlayer.release();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        musicProgressbar.setProgress(0);
        tv_startTime.setText("00:00");
        btn_play.setImageResource(R.drawable.player_play_icon);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.play_note_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.full_lyrics:
                isFullView = !isFullView;
                if (isFullView){
                    menuItem.setIcon(R.drawable.lyrics_green_icon);
                    player_layout.setVisibility(View.GONE);
                    recording_layout.setVisibility(View.GONE);
                }else{
                    menuItem.setIcon(R.drawable.ic_full_lyrics_icon);
                    player_layout.setVisibility(View.VISIBLE);
                }
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onScrollChanged() {
        try{
            if (popupWindow.isShowing()) {
                final Point ploc = calculatePopupLocation();
                if (ploc!=null){
                    popupWindow.update(ploc.x, ploc.y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onTextSelected() {
        try {

            final View popupContent = popupWindow.getContentView();
            final int startSelection=et_lyrics.getSelectionStart();
            final int endSelection=et_lyrics.getSelectionEnd();

            String txt = et_lyrics.getText().toString().substring(startSelection, endSelection);
            fetchSynonym(txt);
            /*et_lyrics.post(new Runnable() {
                @Override
                public void run() {
                et_lyrics.setSelection(et_lyrics.length());
                }
            });*/
            hideSoftKeyboard(PlayNoteActivity.this);
            popupContent.requestFocus();
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            if (popupWindow.isShowing()) {
                final Point ploc = calculatePopupLocation();
                popupWindow.update(ploc.x, ploc.y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            } else {
                ViewUtils.onGlobalLayout(et_lyrics, new Runnable() {
                    @Override
                    public void run() {
                        popupWindow.showAtLocation(et_lyrics, TOP, 0, 0);
                            ViewUtils.onGlobalLayout(popupContent, new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Rect cframe = new Rect();
                                        int[] cloc = new int[2];
                                        popupContent.getLocationOnScreen(cloc);
                                        popupContent.getLocalVisibleRect(cbounds);
                                        popupContent.getWindowVisibleDisplayFrame(cframe);

                                        int scrollY = ((View) et_lyrics.getParent()).getScrollY();
                                        int[] tloc = new int[2];
                                        et_lyrics.getLocationInWindow(tloc);

                                        int startX = cloc[0] + cbounds.centerX();
                                        int startY = cloc[1] + cbounds.centerY() - (tloc[1] - cframe.top) - scrollY;
                                        startLoc.set(startX, startY);

                                        Point ploc = calculatePopupLocation();
                                        popupWindow.update(ploc.x, ploc.y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                });
            }
        }
        catch (Exception e){
            Log.e("Err",e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_LOOP_ACTIVITY:
                if(resultCode == RESULT_OK) {
                    startTime = data.getIntExtra(PARAM_START_TIME,0);
                    endTime = data.getIntExtra(PARAM_END_TIME,0);
                    loopon = true;
                    loop_text.setText(getString(R.string.unloop));
                    loop_text.setTextColor(getResources().getColor(R.color.colorAccent));
                    loop_icon.setImageResource(R.drawable.ic_unloop);
                    play();
                    break;
                }
        }
    }

    private void fetchSynonym(String key){
        key = key.trim();
        lyricsWordSynonymArrayList.clear();
        lyricsPopupListAdapter.notifyDataSetChanged();
        popup_progressbar.setVisibility(View.VISIBLE);

        if (isInternetAvailable(this)){
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
            params.put("rel_rhy", key);
            params.put("md", "d");
            client.get(APIs.SYNONYM_WORD, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    popup_progressbar.setVisibility(View.GONE);
                    String responseString = new String(responseBody);
                    try {
                        JSONArray response = new JSONArray(responseString);
                        int len = response.length();

                        LyricsWordSynonym lyricsWordSynonym;

                        if (len>0){
                            lyricsWordSynonym = new LyricsWordSynonym(getString(R.string.select_a_word),false,null);
                            lyricsWordSynonymArrayList.add(lyricsWordSynonym);

                            for(int i = 0; i<len; i++){
                                lyricsWordSynonym = new LyricsWordSynonym(response.getJSONObject(i).getString("word"),false,(response.getJSONObject(i).has("defs") ? response.getJSONObject(i).getJSONArray("defs") : null));
                                lyricsWordSynonymArrayList.add(lyricsWordSynonym);
                            }

                            lyricsWordSynonym = new LyricsWordSynonym("",false,null);
                            lyricsWordSynonymArrayList.add(lyricsWordSynonym);
                        }
                        else{
                            popupWindow.dismiss();
                        }

                        lyricsPopupListAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    popup_progressbar.setVisibility(View.GONE);
                    error.printStackTrace();
                }
            });
        }else{
            searchRhym.rhym(key);
        }
    }

    // updates the visualizer every 50 milliseconds
    Runnable updateVisualizer = new Runnable() {
        @Override
        public void run() {
            if (isRecording){ // if we are already recording
                // get the current amplitude
                if (mRecorder!=null && visualizerView!=null){
                    visualizerView.addAmplitude(mRecorder.getMaxAmplitude()); // update the VisualizeView
                }
                // update in 40 milliseconds
                handler.postDelayed(this, REPEAT_INTERVAL);
            }
        }
    };

    @Override
    public void onTextUnselected() {
        try{
            popupWindow.dismiss();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /** Used to calculate where we should position the {@link PopupWindow} */
    private Point calculatePopupLocation() {
        try{
            final ScrollView parent = (ScrollView) et_lyrics.getParent();

            // Calculate the selection start and end offset
            final int selStart = et_lyrics.getSelectionStart();
            final int selEnd = et_lyrics.getSelectionEnd();
            final int min = Math.max(0, Math.min(selStart, selEnd));
            final int max = Math.max(0, Math.max(selStart, selEnd));

            // Calculate the selection bounds
            final RectF selBounds = new RectF();
            final Path selection = new Path();
            et_lyrics.getLayout().getSelectionPath(min, max, selection);
            selection.computeBounds(selBounds, true /* this param is ignored */);

            // Retrieve the center x/y of the popup content
            final int cx = startLoc.x;
            final int cy = startLoc.y;

            // Calculate the top and bottom offset of the popup relative to the selection bounds
            final int popupHeight = cbounds.height();
            final int textPadding = et_lyrics.getPaddingLeft();
            final int topOffset = Math.round(selBounds.top - cy);
            final int btmOffset = Math.round(selBounds.bottom - (cy - popupHeight));

            // Calculate the x/y coordinates for the popup relative to the selection bounds
            final int scrollY = parent.getScrollY();
            final int x = Math.round(selBounds.centerX() + textPadding - cx);
            final int y = Math.round(selBounds.top - scrollY < cy ? btmOffset : topOffset);

            currLoc.set(x, y - scrollY - 150);
            return currLoc;
        }catch (Exception e){
            return null;
        }
    }
}
