package com.tullyapp.tully;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.tullyapp.tully.Analyzer.AnalyzeSubscriptionDialogFragment;
import com.tullyapp.tully.FirebaseDataModels.Masters;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.Services.AudioAnalyzeService;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tullyapp.tully.Fragments.MasterDetailsFragment.PARAM_MASTER_FILE;
import static com.tullyapp.tully.Services.AudioAnalyzeService.ACTION_ANALYZE_PROGRESS;
import static com.tullyapp.tully.Services.AudioAnalyzeService.INTENT_PARAM_BPM;
import static com.tullyapp.tully.Services.AudioAnalyzeService.INTENT_PARAM_KEY;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_ISLOOPING;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_MASTER;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_MASTERS;
import static com.tullyapp.tully.Utils.Utils.formatAudioTime;
import static com.tullyapp.tully.Utils.Utils.getDirectory;

public class MasterPlayActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, AnalyzeSubscriptionDialogFragment.SubscriptionEvents {

    private static final int REQUEST_CODE = 123;
    private static final String TAG = MasterPlayActivity.class.getSimpleName();
    public static final String INTENT_PARAM_START_TIME = "INTENT_PARAM_START_TIME";
    private FirebaseAuth mAuth;
    public static final String PARAM_MASTER = "PARAM_MASTER";

    private ArrayList<Masters> mastersArrayList;
    private int currentIndex;

    private RelativeLayout createProjectSection, cp_list;
    private MediaPlayer mPlayer;
    private boolean isReleased = false;
    private boolean isPaused = false;
    private boolean isRepeat = false;
    private ImageView btn_play, repeateToggle, play_left, play_right, img_option, img_cast, analyze_music, plus_sign;
    private ImageView analyze_icon, three_dash_left, three_dash_right;
    private TextView infolbl, lyrics_sample, tv_projectname,label_detecting, tv_percent, tv_bpm, tv_key, tv_key_value, tv_bpm_value;
    private AppCompatSeekBar musicProgressbar;

    private TextView tv_endtime, tv_startTime ,tv_filename;
    private int duration, numberOfRecordings;
    private Button btn_detect;
    private Handler handler = new Handler();

    private AudioManager audioManager;
    private FirebaseStorage storage;
    private File mainAudio;
    private ProgressDialog progressDialog;
    private File localdir_masters;
    private ResponseReceiver responseReceiver;

    private View lyricsView, analyzeView;
    private boolean analyzerSubscribed = false;
    private ProgressBar analyze_progressbar;
    private long freeTrials = 0;
    private LayoutInflater vi;
    private DatabaseReference mDatabase;
    private AnalyzeSubscriptionDialogFragment analyzeSubscriptionDialogFragment;
    private RelativeLayout insertPoint;
    private int bpm;
    private String key;
    private AtomicInteger currenttime = new AtomicInteger(0);

    private enum  AUDUI_OUTPUT{
        SPEAKER, EARPHONES, BLUETOOTH
    }

    private enum CURRENT_VIEW{
        LYRICS_VIEW, ANALYZE_VIEW
    }
    private CURRENT_VIEW current_view = CURRENT_VIEW.ANALYZE_VIEW;

    private AUDUI_OUTPUT AOUTPUT = AUDUI_OUTPUT.SPEAKER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_play);

        getSupportActionBar().setTitle("Play");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ArrayList<Masters> masters = (ArrayList<Masters>) getIntent().getSerializableExtra(PARAM_MASTER);
        mastersArrayList = new ArrayList<>();

        responseReceiver = new ResponseReceiver();

        registerReceivers();

        Masters selectedM = (Masters) getIntent().getSerializableExtra(PARAM_MASTER_FILE);

        int i = 0;
        for (Masters m : masters){
            if (m.getType().equals("file")){
                mastersArrayList.add(m);
                if (selectedM.getId().equals(m.getId())){
                    currentIndex = i;
                }
                i++;
            }
        }

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser()!=null){
            initUI();
            mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
    }

    private void initUI(){
        vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        musicProgressbar = findViewById(R.id.appCompatSeekBar);
        musicProgressbar.setOnSeekBarChangeListener(this);

        btn_play = findViewById(R.id.btn_play);
        tv_startTime = findViewById(R.id.tv_startTime);
        tv_endtime = findViewById(R.id.tv_endtime);
        repeateToggle = findViewById(R.id.repeateToggle);
        play_left = findViewById(R.id.play_left);
        play_right = findViewById(R.id.play_right);

        tv_filename = findViewById(R.id.tv_filename);
        img_option = findViewById(R.id.img_option);
        img_cast = findViewById(R.id.img_cast);
        analyze_music = findViewById(R.id.analyze_music);
        createProjectSection = findViewById(R.id.createProjectSection);
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

        createProjectSection.setOnClickListener(this);
        btn_play.setOnClickListener(this);
        //player_write.setOnClickListener(this);
        repeateToggle.setOnClickListener(this);
        play_left.setOnClickListener(this);
        play_right.setOnClickListener(this);
        //img_option.setOnClickListener(this);
        img_cast.setOnClickListener(this);
        analyze_music.setOnClickListener(this);
        btn_detect.setOnClickListener(this);

        storage = FirebaseStorage.getInstance();
        localdir_masters = getDirectory(getApplicationContext(),LOCAL_DIR_NAME_MASTERS);
        Masters masterAudio = mastersArrayList.get(currentIndex);
        mainAudio = new File(localdir_masters, masterAudio.getFilename());

        if (mastersArrayList.get(currentIndex).getKey()!=null){
            current_view = CURRENT_VIEW.LYRICS_VIEW;
            insertPoint.addView(lyricsView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tv_key_value.setText(masterAudio.getKey());
            tv_bpm_value.setText(masterAudio.getBpm()+"");
        }
        else{
            current_view = CURRENT_VIEW.ANALYZE_VIEW;
            insertPoint.addView(analyzeView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        updateLyricsUI();

        play();
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
                    tv_bpm.setVisibility(View.VISIBLE);
                    tv_key.setVisibility(View.VISIBLE);
                    three_dash_left.setVisibility(View.GONE);
                    three_dash_right.setVisibility(View.GONE);
                    analyze_icon.setImageResource(R.drawable.bpm_done);
                    analyze_progressbar.setVisibility(View.INVISIBLE);
                    mastersArrayList.get(currentIndex).setBpm(bpm);
                    mastersArrayList.get(currentIndex).setKey(key);
                    FirebaseDatabaseOperations.updateBmpandKeyMasterFile(MasterPlayActivity.this,mastersArrayList.get(currentIndex));
                    break;
            }
        }
    }

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_ANALYZE_PROGRESS);
        LocalBroadcastManager.getInstance(MasterPlayActivity.this).registerReceiver(responseReceiver, filter);
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(MasterPlayActivity.this).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void play(){
        clearPlayer();
        String path;
        if (mainAudio.exists()){
            path = mainAudio.getAbsolutePath();
            if (!path.isEmpty()){
                try {
                    isPaused = false;
                    isReleased = false;
                    btn_play.setImageResource(R.drawable.player_pause_icon);
                    mPlayer = new MediaPlayer();
                    mPlayer.setDataSource(path);
                    mPlayer.prepareAsync();
                    mPlayer.setLooping(isRepeat);
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
            StorageReference storageRef = storage.getReferenceFromUrl(mastersArrayList.get(currentIndex).getDownloadURL());
            File localFile = new File(localdir_masters, mastersArrayList.get(currentIndex).getFilename());
            downloadFile(storageRef,localFile);
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
                    long received = taskSnapshot.getBytesTransferred();
                    long total = taskSnapshot.getTotalByteCount();
                    progressDialog.setProgress((int) ((received / total) * 100));
                }
            }).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
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
                        Toast.makeText(MasterPlayActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (IOException e) {
            progressDialog.dismiss();
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.createProjectSection:
                if (current_view == CURRENT_VIEW.ANALYZE_VIEW){
                    analyze_music.setImageResource(R.drawable.ic_analyze_music);
                    insertPoint.removeAllViewsInLayout();
                    tv_bpm_value.setText(bpm+"");
                    tv_key_value.setText(key);
                    insertPoint.addView(lyricsView,0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    current_view = CURRENT_VIEW.LYRICS_VIEW;
                }
                else{
                    resetMplayer();
                    Intent intent = new Intent(MasterPlayActivity.this, MasterPlayNoteActivity.class);
                    intent.putExtra(INTENT_PARAM_MASTER,mastersArrayList.get(currentIndex));
                    intent.putExtra(INTENT_PARAM_ISLOOPING,isRepeat);
                    intent.putExtra(INTENT_PARAM_START_TIME,currenttime.get());
                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, btn_play, ViewCompat.getTransitionName(btn_play));
                    ActivityCompat.startActivityForResult(this,intent, REQUEST_CODE, options.toBundle());
                }
                break;

            case R.id.btn_play:
                togglePlay();
                break;

            case R.id.img_cast:
                audioCast();
                break;

            case R.id.repeateToggle:
                isRepeat = !isRepeat;
                if (isRepeat) repeateToggle.setImageResource(R.drawable.repeate_active_icon);
                else repeateToggle.setImageResource(R.drawable.repeat);

                if (mPlayer!=null){
                    try{
                        mPlayer.setLooping(isRepeat);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                break;

            case R.id.play_left:
                navigatePlay(R.id.play_left);
                break;

            case R.id.play_right:
                navigatePlay(R.id.play_right);
                break;

            case R.id.analyze_music:
                toogleAnalyzeView();
                break;

            case R.id.btn_detect:
                analyzeAudio();
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==123){
            switch (resultCode){
                case RESULT_OK:
                    Masters master = (Masters) data.getSerializableExtra(INTENT_PARAM_MASTER);
                    mastersArrayList.get(currentIndex).setLyrics(master.getLyrics());
                    break;

                case RESULT_CANCELED:
                    break;
            }
            updateLyricsUI();
        }
    }

    private void updateLyricsUI(){
        String val = mastersArrayList.get(currentIndex).getLyrics();
        tv_filename.setText(mastersArrayList.get(currentIndex).getName());
        if (val!=null && !val.isEmpty()){
            lyrics_sample.setText(val);
            lyrics_sample.setVisibility(View.VISIBLE);
            plus_sign.setVisibility(View.GONE);
            infolbl.setVisibility(View.GONE);
        }
        else{
            lyrics_sample.setVisibility(View.GONE);
            lyrics_sample.setText("");
            plus_sign.setVisibility(View.GONE);
            infolbl.setVisibility(View.GONE);
        }
    }

    private void audioCast(){
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        //Log.e(TAG,audioManager.getMode()+" : "+audioManager.isWiredHeadsetOn()+" : "+audioManager.isSpeakerphoneOn()+" : "+audioManager.isBluetoothScoOn()+" : "+audioManager.getRingerMode());
        alt_bld.setTitle("Audio Output");
        alt_bld.setItems(R.array.audio_output, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
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
                dialog.dismiss();// dismiss the alertbox after chose option
            }
        }).setCancelable(true);
        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    private void navigatePlay(int playDirection){
        int oldInext = currentIndex;
        if (mastersArrayList.size()>1){
            switch (playDirection){
                case R.id.play_left:
                    if (currentIndex>0){
                        currentIndex--;
                    }else{
                        Toast.makeText(this, "Its First Already", Toast.LENGTH_SHORT).show();
                    }
                    break;


                case R.id.play_right:
                    if (currentIndex<mastersArrayList.size()-1){
                        currentIndex++;
                    }
                    else{
                        Toast.makeText(this, "End Reached", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }

        if (oldInext!=currentIndex){
            updateLyricsUI();
            mainAudio = new File(localdir_masters, mastersArrayList.get(currentIndex).getFilename());

            if (mPlayer!=null){
                clearPlayer();
                isReleased = true;
            }

            play();
        }
    }

    private void resetMplayer(){
        if (mPlayer!=null){
            btn_play.setImageResource(R.drawable.player_play_icon);
            musicProgressbar.setProgress(0);
            tv_startTime.setText(R.string._00_00);
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
                    Log.e("ENTER","PAUSED");
                    mPlayer.pause();
                    isPaused = true;
                    handler.removeCallbacks(MediaObserver);
                    btn_play.setImageResource(R.drawable.player_play_icon);
                }else if (isPaused){
                    Log.e("RESUMING","Yoo");
                    mPlayer.start();
                    isPaused = false;
                    btn_play.setImageResource(R.drawable.player_pause_icon);
                    handler.post(MediaObserver);
                }
                else{
                    Log.e("ELSE","PLAY");
                    play();
                }
            }catch (Exception e){
                play();
            }
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        handler.removeCallbacks(MediaObserver);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mPlayer!=null){
            try{
                if (mPlayer.isPlaying()){
                    int currentPosition = seekBar.getProgress();
                    mPlayer.seekTo(currentPosition);
                    mPlayer.pause();
                    mPlayer.start();
                    handler.post(MediaObserver);
                }
                else{
                    int currentPosition = seekBar.getProgress();
                    mPlayer.seekTo(currentPosition);
                }
            }catch (Exception e){

            }
        }
    }

    private void toogleAnalyzeView(){
        if (current_view == CURRENT_VIEW.ANALYZE_VIEW){
            insertPoint.removeAllViewsInLayout();
            insertPoint.addView(lyricsView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            current_view = CURRENT_VIEW.LYRICS_VIEW;
        }
        else{
            actionAnalyzeorView(false);
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

    private void actionAnalyzeorView(boolean detect){
        if (detect){
            label_detecting.setVisibility(View.VISIBLE);
            analyze_icon.setVisibility(View.VISIBLE);
            analyze_icon.setImageResource(R.drawable.ic_analyze_icon);
            analyze_progressbar.setVisibility(View.VISIBLE);
            //tv_percent.setVisibility(View.VISIBLE);
            btn_detect.setVisibility(View.GONE);
            AudioAnalyzeService.startAnalyzingAudio(MasterPlayActivity.this,mainAudio.getAbsolutePath());
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
        analyzeSubscriptionDialogFragment.setSubscriptionEvents(MasterPlayActivity.this);
        analyzeSubscriptionDialogFragment.show(getSupportFragmentManager(),AnalyzeSubscriptionDialogFragment.class.getSimpleName());
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
        static final long PROGRESS_UPDATE = 300;

        @Override
        public void run() {
            try {
                currenttime.set(mPlayer.getCurrentPosition());
                musicProgressbar.setProgress(currenttime.get());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    tv_startTime.setText(formatAudioTime(currenttime.get()));
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
    public void onSucessfullSubscription() {
        analyzerSubscribed = true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mPlayer.start();
        duration = mPlayer.getDuration();
        musicProgressbar.setMax(duration);
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
    public void onBackPressed() {
        handler.removeCallbacks(MediaObserver);
        clearPlayer();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadcast();
    }
}
