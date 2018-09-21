package com.tullyapp.tully;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tullyapp.tully.Adapters.ProfileLyricsAdapter;
import com.tullyapp.tully.Adapters.ProfileRecordingsAdapter;
import com.tullyapp.tully.FirebaseDataModels.Lyrics;
import com.tullyapp.tully.FirebaseDataModels.LyricsModule.LyricsAppModel;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.RecyclerItemClickListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_PROJECT;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.DB_PARAM;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_LYRICS;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_RECORDINGS;
import static com.tullyapp.tully.Utils.Constants.PROJECT_PARAM;
import static com.tullyapp.tully.Utils.Utils.getDirectory;
import static com.tullyapp.tully.ViewPagerFragments.HomeAllFragment.REQUEST_PROJECT;

public class ProjectActivity extends AppCompatActivity implements ProfileRecordingsAdapter.playRecordingListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, View.OnClickListener {

    private static final int LYREICS_REQUEST_CODE = 12;
    private static final int REQUEST_CODE = 321;
    private static final String TAG = ProjectActivity.class.getSimpleName();
    private TextView tv_projectname;

    private TextView lyrics_heading;
    private TextView recording_heading;

    private ProfileLyricsAdapter lyricsAdapter;
    private ProfileRecordingsAdapter recordingsAdapter;

    private ArrayList<Lyrics> lyricsArrayList = new ArrayList<>();
    private ArrayList<Recording> recordingArrayList = new ArrayList<>();

    private Intent intent;

    private ResponseReceiver responseReceiver;

    private ProgressBar progressBar;

    private String projectId;

    private MediaPlayer mediaPlayer = null;
    private Recording recObject;
    private Project project;
    private File localdir_recordings;
    private int currentPos;
    private Handler handler = new Handler();
    private int audioDuration;
    private ImageView projectMain;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        intent = getIntent();
        project = (Project) intent.getSerializableExtra(PROJECT_PARAM);
        getSupportActionBar().setTitle(project.getProject_name());
        responseReceiver = new ResponseReceiver();

        progressBar = findViewById(R.id.progressBar);

        projectMain = findViewById(R.id.profile_picture);
        tv_projectname = findViewById(R.id.tv_projectname);
        RecyclerView recycle_view_lyrics = findViewById(R.id.recycle_view_lyrics);
        RecyclerView recycle_view_recording = findViewById(R.id.recycle_view_recording);

        lyrics_heading = findViewById(R.id.lyrics_heading);
        recording_heading = findViewById(R.id.recording_heading);

        recycle_view_lyrics.setLayoutManager(new LinearLayoutManager(this, LinearLayout.HORIZONTAL, false));
        recycle_view_recording.setLayoutManager(new LinearLayoutManager(this, LinearLayout.HORIZONTAL, false));

        lyricsAdapter = new ProfileLyricsAdapter(this,lyricsArrayList);
        recordingsAdapter = new ProfileRecordingsAdapter(this, recordingArrayList);

        recycle_view_lyrics.setAdapter(lyricsAdapter);
        recycle_view_recording.setAdapter(recordingsAdapter);

        recycle_view_lyrics.addOnItemTouchListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onItemClick(View view, int position) {
                clearPlayer();
                Lyrics lyric = lyricsArrayList.get(position);
                Intent intent = new Intent(ProjectActivity.this,LyricsEditActivity.class);
                LyricsAppModel lyricsAppModel = new LyricsAppModel();
                lyricsAppModel.setLyrics(lyric);
                lyricsAppModel.setOfProject(true);
                lyricsAppModel.setProjectId(projectId);
                intent.putExtra(INTENT_PARAM_LYRICS,lyricsAppModel);

                TextView ll = view.findViewById(R.id.lyrics);
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(ProjectActivity.this, ll, ViewCompat.getTransitionName(ll));
                startActivityForResult(intent, LYREICS_REQUEST_CODE, options.toBundle());
            }
        }));

        projectId = project.getId();

        recordingsAdapter.setOnPlayClickListener(this);

        projectMain.setOnClickListener(this);

        loadUI();

        localdir_recordings = getDirectory(getApplicationContext(),LOCAL_DIR_NAME_RECORDINGS);

        registerReceivers();
        fetchProject();
    }

    private void fetchProject(){
        if (projectId!=null) FirebaseDatabaseOperations.startActionPullProject(ProjectActivity.this, projectId);
    }

    @Override
    public void onPlayRecord(Recording recording, int position) {
        play(recording, position);
    }

    @Override
    public void onRecordPause(Recording recording, int position) {
        try{
            if (mediaPlayer!=null && mediaPlayer.isPlaying()){
                handler.removeCallbacks(updateProgressbar);
                mediaPlayer.pause();
            }
        }catch (Exception e){
            handler.removeCallbacks(updateProgressbar);
            try{
                mediaPlayer.pause();
            }
            catch (Exception ex){

            }
        }
    }

    @Override
    public void onResume(Recording recording, int position) {
        if (mediaPlayer!=null){
            try {
                mediaPlayer.start();
                handler.post(updateProgressbar);
            }catch (Exception e){

            }
        }
    }

    private void play(Recording recording, int position){

        currentPos = position;

        if (mediaPlayer!=null){
            clearPlayer();
        }

        recObject = recording;
        File localFile = new File(localdir_recordings, recording.getTid());
        String path = "";

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
                recordingsAdapter.markAudioPlaying(recording);
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(path);
                mediaPlayer.prepareAsync();
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setOnCompletionListener(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        audioDuration = mp.getDuration();
        handler.post(updateProgressbar);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        handler.removeCallbacks(updateProgressbar);
        clearPlayer();
        recordingsAdapter.markAudioComplete(recObject);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.profile_picture:
                if (project!=null){
                    intent = new Intent(ProjectActivity.this,PlayActivity.class);
                    project.setId(projectId);
                    intent.putExtra(INTENT_PARAM_PROJECT,project);
                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, projectMain, ViewCompat.getTransitionName(projectMain));
                    startActivityForResult(intent, REQUEST_PROJECT, options.toBundle());
                }
                break;
        }
    }

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case ACTION_PULL_PROJECT:
                    progressBar.setVisibility(View.GONE);
                    Project newproject = (Project) intent.getSerializableExtra(DB_PARAM);
                    if (newproject!=null){
                        project = newproject;
                        project.setId(projectId);
                        loadUI();
                    }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*switch (requestCode){
            case LYREICS_REQUEST_CODE:
                if(resultCode == Activity.RESULT_OK){
                    Toast.makeText(this, "Lyrics Modified", Toast.LENGTH_SHORT).show();
                    fetchProject();
                }
                if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "Lyrics Not Modified", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_CODE:
                fetchProject();
                break;
        }*/

        fetchProject();
    }

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_PULL_PROJECT);
        LocalBroadcastManager.getInstance(ProjectActivity.this).registerReceiver(responseReceiver, filter);
    }

    private void loadUI(){
        tv_projectname.setText(project.getProject_name());

        HashMap<String, Lyrics> lyricsHashMap = project.getLyrics();
        HashMap<String, Recording> recordingsHashMap = project.getRecordings();

        lyricsArrayList.clear();
        recordingArrayList.clear();

        if (lyricsHashMap !=null){
            Iterator it = lyricsHashMap.entrySet().iterator();
            Lyrics lyrics;
            while (it.hasNext()){
                Map.Entry pair = (Map.Entry)it.next();
                lyrics = (Lyrics) pair.getValue();
                lyrics.setId(pair.getKey().toString());
                lyrics.setTitle(project.getProject_name());
                lyrics.setProjectID(project.getId());
                lyricsArrayList.add(lyrics);
            }

            lyrics_heading.setVisibility(View.VISIBLE);
            lyrics_heading.setText("Lyrics "+lyricsArrayList.size());
        }
        else{
            lyrics_heading.setVisibility(View.GONE);
        }

        if (recordingsHashMap !=null){
            Iterator iterator = recordingsHashMap.entrySet().iterator();
            Recording recording;
            while (iterator.hasNext()){
                Map.Entry pair = (Map.Entry)iterator.next();
                recording = (Recording) pair.getValue();

                if (recording.getTid()!=null && recording.getTid().equals(project.getProject_main_recording()))
                    continue;

                recording.setId(pair.getKey().toString());
                recording.setProjectName(project.getProject_name());
                recording.setProjectId(project.getId());
                recordingArrayList.add(recording);
            }

            recording_heading.setVisibility(View.VISIBLE);
            recording_heading.setText("Recording "+ recordingArrayList.size());

        }else{
            recording_heading.setVisibility(View.GONE);
        }

        recordingsAdapter.notifyDataSetChanged();
        lyricsAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearPlayer();
        unregisterBroadcast();
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(ProjectActivity.this).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){

        }
    }

    private void clearPlayer(){
        handler.removeCallbacks(updateProgressbar);
        try{
            mediaPlayer.stop();
        }catch (Exception e){

        }
        try {
            mediaPlayer.release();
        }catch (Exception e){

        }
    }

    Runnable updateProgressbar = new Runnable() {
        static final long PROGRESS_UPDATE = 100;
        int currentTime;
        int percent;
        double dd;
        @Override
        public void run() {
            try{
                if (mediaPlayer.isPlaying()){
                    currentTime = mediaPlayer.getCurrentPosition();
                    dd = audioDuration / 100;
                    percent = (int) (currentTime / dd);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recordingsAdapter.updateProgress(currentPos,percent);
                        }
                    });

                    handler.postDelayed(this, PROGRESS_UPDATE);
                }
            }catch (Exception e){

            }
        }
    };



}
