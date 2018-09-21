package com.tullyapp.tully;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.tullyapp.tully.CustomView.ImageProgressBar;
import com.tullyapp.tully.CustomView.RecorderVisualizerView;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.Services.DeleteProjects;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.Percent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_SAVE_RECORDINGS;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.RESPONSE_PARAM;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_RECORDINGS;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Utils.getDirectory;
import static com.tullyapp.tully.Utils.Utils.getFileName;
import static com.tullyapp.tully.Utils.ViewUtils.shareAllowDownloadPopup;

public class RecordingActivity extends AppCompatActivity implements View.OnClickListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, CompoundButton.OnCheckedChangeListener {

    private static final long PROGRESS_UPDATE = 100;
    private ImageView btn_record, record_play;
    private AppCompatButton btn_popup_create, btn_popup_cancel;
    private ResponseReceiver responseReceiver;

    private TextView no1,no2,no3,no4;

    private static final String LOG_TAG = "RecordingActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    boolean isPlaying = false;
    boolean isRecording = false;

    File localdir = null;
    File localFile = null;
    String localFileName = "";
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {android.Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.MODIFY_AUDIO_SETTINGS, Manifest.permission.BLUETOOTH};
    private Chronometer mChronometer;
    private TextView tv_pop_title, tv_pop_desc, recording_lbl;

    private View bottomSheet;
    private BottomSheetBehavior mBottomSheetBehavior;

    private Dialog dialog;
    private EditText et_popup_input;
    private AlertDialog.Builder builder;
    private LinearLayout ll_share, ll_delete, stat_ll;
    private Recording recording = null;
    private RecorderVisualizerView visualizerView;
    public static final int REPEAT_INTERVAL = 40;
    private Handler handler = new Handler();
    private Handler progressHandler = new Handler();
    private ImageProgressBar img_progress;
    private int audioDuration;
    private boolean anyRecordingDone = false;
    private byte animIndex = 4;
    private boolean animating = false;
    private MixpanelAPI mixpanel;
    private FirebaseAuth mAuth;
    private Dialog shareDialog;
    private AppCompatButton btn_allow, btn_not_allow, btn_cp_url;
    private SwitchCompat switch_expire_after_once, switch_expire_after_one_hour, switch_expire_never;
    private static final String TAG = RecordingActivity.class.getSimpleName();
    private boolean allowSwitch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("New Record");
        initUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mixpanel = MixpanelAPI.getInstance(this, YOUR_PROJECT_TOKEN);
    }

    private void initUI(){
        record_play = findViewById(R.id.record_play);
        btn_record = findViewById(R.id.btn_record);
        mChronometer= findViewById(R.id.chronometer);
        visualizerView  = findViewById(R.id.visualizer);
        img_progress = findViewById(R.id.img_progress);

        mAuth = FirebaseAuth.getInstance();

        no1 = findViewById(R.id.no1);
        no2 = findViewById(R.id.no2);
        no3 = findViewById(R.id.no3);
        no4 = findViewById(R.id.no4);

        recording_lbl = findViewById(R.id.recording_lbl);

        btn_record.setOnClickListener(this);
        record_play.setOnClickListener(this);

        dialog = new Dialog(RecordingActivity.this, R.style.MyDialogTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.input_popup);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        et_popup_input = dialog.findViewById(R.id.et_popup_input);

        tv_pop_title = dialog.findViewById(R.id.tv_pop_title);
        tv_pop_title.setText("Name Your Recording");

        tv_pop_desc = dialog.findViewById(R.id.tv_pop_desc);
        tv_pop_desc.setText("You can access this record from the recordings menu");

        btn_popup_create = dialog.findViewById(R.id.btn_popup_create);
        btn_popup_cancel = dialog.findViewById(R.id.btn_popup_cancel);

        btn_popup_create.setOnClickListener(this);
        btn_popup_cancel.setOnClickListener(this);

        bottomSheet = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setPeekHeight(0);
        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setSkipCollapsed(true);

        responseReceiver = new ResponseReceiver();
        registerReceivers();

        ll_share = findViewById(R.id.ll_share);
        ll_delete = findViewById(R.id.ll_delete);
        stat_ll = findViewById(R.id.stat_ll);

        ll_share.setOnClickListener(this);
        ll_delete.setOnClickListener(this);

        localdir = getDirectory(RecordingActivity.this,LOCAL_DIR_NAME_RECORDINGS);

        shareDialog = shareAllowDownloadPopup(this);

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

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                try{
                    permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
        }
        if (!permissionToRecordAccepted ) Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
    }


/*
--------------
Media Recorder
--------------
*/

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            isRecording = false;
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        }
        else{
            try {
                img_progress.setVisibility(View.GONE);
                visualizerView.setVisibility(View.VISIBLE);
                visualizerView.clear();
                visualizerView.invalidate();
                localFileName = getFileName()+".wav";
                localFile = new File(localdir, localFileName);
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(AudioFormat.ENCODING_PCM_16BIT);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mRecorder.setAudioChannels(2);
                mRecorder.setAudioEncodingBitRate(128000);
                mRecorder.setAudioSamplingRate(44100);
                mRecorder.setOutputFile(localFile.getPath());
                mRecorder.prepare();
                mRecorder.start();
                btn_record.setImageResource(R.drawable.record_icon);
                startRecordingTimer();
                handler.post(updateVisualizer);
                anyRecordingDone = true;
                stat_ll.setVisibility(View.VISIBLE);
                recording_lbl.setText(getString(R.string.recording));
            } catch (IOException e) {
                Log.e(LOG_TAG, "prepare() failed : "+e.getMessage());
                isRecording = false;
                clearRecorder();
            }
            catch (Exception e){
                Toast.makeText(this, "Recording Failed", Toast.LENGTH_SHORT).show();
                isRecording = false;
                clearRecorder();
            }
        }
    }

    private void startAnimate(TextView tv){
        animating = true;
        ObjectAnimator anim = ObjectAnimator.ofFloat(tv,"scaleX",1.5f);
        anim.setDuration(500);
        anim.start();

        ObjectAnimator anim2 = ObjectAnimator.ofFloat(tv,"scaleY",1.5f);
        anim2.setDuration(500);
        anim2.start();

        ObjectAnimator colorAnim = ObjectAnimator.ofInt(tv, "textColor", getResources().getColor(R.color.inactiveAnimationTextColor), getResources().getColor(R.color.colorAccent));
        colorAnim.setDuration(500);
        colorAnim.setEvaluator(new ArgbEvaluator());
        colorAnim.start();

        ObjectAnimator back = ObjectAnimator.ofFloat(tv,"scaleX",1f);
        back.setDuration(500);
        back.setStartDelay(500);
        back.start();

        ObjectAnimator back2 = ObjectAnimator.ofFloat(tv,"scaleY",1f);
        back2.setDuration(500);
        back2.setStartDelay(500);
        back2.start();

        ObjectAnimator backColor = ObjectAnimator.ofInt(tv, "textColor", getResources().getColor(R.color.colorAccent), getResources().getColor(R.color.inactiveAnimationTextColor));
        backColor.setDuration(500);
        backColor.setStartDelay(500);
        backColor.setEvaluator(new ArgbEvaluator());
        backColor.start();

        back2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animIndex--;
                if (animIndex>0){
                    switch (animIndex){
                        case 3:
                            startAnimate(no3);
                            break;

                        case 2:
                            startAnimate(no2);
                            break;

                        case 1:
                            startAnimate(no1);
                            break;
                    }
                }
                else{
                    animIndex = 4;
                    animating = false;
                    onRecord(isRecording);
                }
            }
        });
    }


    private void stopRecording() {
        clearRecorder();
        showPopup();
    }

    private void clearRecorder(){
        try {
            handler.removeCallbacks(updateVisualizer);
            visualizerView.clear();
            visualizerView.invalidate();
            btn_record.setImageResource(R.drawable.record_inactive);
            mChronometer.setBase(SystemClock.elapsedRealtime());
            handler.removeCallbacks(updateProgressbar);
            mChronometer.stop();
            mRecorder.stop();
            mRecorder.release();
            isRecording = false;

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void clearPlayer() {
        try {
            img_progress.setCurrentValue(new Percent(1));
            img_progress.invalidate();
            record_play.setImageResource(R.drawable.record_play);
            mChronometer.stop();
            mChronometer.setBase(SystemClock.elapsedRealtime());
            if (mPlayer!=null){
                progressHandler.removeCallbacks(updateProgressbar);
                mPlayer.stop();
                mPlayer.release();
            }
            isPlaying = false;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

/*
--------------
Media Player
--------------
*/

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        if (localFile!=null && localFile.exists()){
            visualizerView.clear();
            visualizerView.invalidate();
            visualizerView.setVisibility(View.GONE);
            img_progress.setVisibility(View.VISIBLE);
            img_progress.setCurrentValue(new Percent(0));
            img_progress.invalidate();
            String path = localFile.getAbsolutePath();
            try {
                mPlayer = new MediaPlayer();
                mPlayer.setDataSource(path);
                mPlayer.prepareAsync();
                mPlayer.setOnPreparedListener(this);
                mPlayer.setOnCompletionListener(this);
                recording_lbl.setText("");
                record_play.setImageResource(R.drawable.recording_play_pause);
            } catch (IOException e) {
                e.printStackTrace();
                stopPlaying();
            } catch (Exception e){
                e.printStackTrace();
            }
        }else{
            stopPlaying();
        }
    }

    private void stopPlaying() {
       clearPlayer();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mPlayer!=null){
            try{
                mPlayer.start();
                audioDuration = mPlayer.getDuration();
                progressHandler.post(updateProgressbar);
                mChronometer.setBase(SystemClock.elapsedRealtime());
                mChronometer.start();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mChronometer.stop();
        mChronometer.setBase(SystemClock.elapsedRealtime());
        progressHandler.removeCallbacks(updateProgressbar);
        mPlayer.stop();
        mPlayer.release();
        isPlaying = false;
        record_play.setImageResource(R.drawable.record_play);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.recording_screen_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            if (mBottomSheetBehavior.getState()==BottomSheetBehavior.STATE_EXPANDED){
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }else{
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            return true;
        }
        else if (id == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        if (anyRecordingDone){
            setResult(Activity.RESULT_OK,returnIntent);
        }else{
            setResult(Activity.RESULT_CANCELED,returnIntent);
        }
        super.onBackPressed();
    }

    private void showPopup(){
        et_popup_input.setText("");
        et_popup_input.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.show();
    }

    private void deleteRecording(){
        if (localFile!=null){
            if (localFile.exists()){
                localFile.delete();
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

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case ACTION_SAVE_RECORDINGS:
                    Log.e("SAVED","RECORDING-RECEIVED");
                    recording = (Recording) intent.getSerializableExtra(RESPONSE_PARAM);
                    break;
            }
        }
    }

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_SAVE_RECORDINGS);
        LocalBroadcastManager.getInstance(RecordingActivity.this).registerReceiver(responseReceiver, filter);
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(RecordingActivity.this).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterBroadcast();
        if (isRecording){
            clearRecorder();
            deleteRecording();
        }

        if (isPlaying) clearPlayer();

        super.onDestroy();
    }

    private void showDeleteAlert(){
        builder = new AlertDialog.Builder(RecordingActivity.this);
        builder.setTitle("Delete Recording ?")
        .setMessage("The recorded file will be deleted")
        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                deleteRecording();
            }
        }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                showPopup();
            }
        })
        .show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_record:
                if (isPlaying){
                    Toast.makeText(this, "please stop the playing first", Toast.LENGTH_SHORT).show();
                }else{
                    if (!animating){
                        if (!isRecording){
                            isRecording = true;
                            startAnimate(no4);
                        }
                        else{
                            isRecording = false;
                            onRecord(false);
                        }
                    }
                }
                break;

            case R.id.record_play:
                if (isRecording){
                    Toast.makeText(this, "please stop recording first", Toast.LENGTH_SHORT).show();
                }else{
                    isPlaying = !isPlaying;
                    onPlay(isPlaying);
                }
                break;

            case R.id.btn_popup_create:
                String val = et_popup_input.getText().toString();
                if (!val.isEmpty()){
                    mixpanel.track("Recording");
                    Recording recording = new Recording();
                    recording.setTid(localFileName);
                    recording.setOfProject(false);
                    recording.setName(val);
                    recording.setSize(localFile.length());
                    FirebaseDatabaseOperations.startActionSaveRecording(RecordingActivity.this, recording,localFile);
                    dialog.dismiss();
                }else{
                    Toast.makeText(this, "please enter record name", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.btn_popup_cancel:
                dialog.dismiss();
                showDeleteAlert();
                break;

            case R.id.ll_delete:
                if (recording!=null){
                    if (isPlaying){
                        localFile = null;
                        mPlayer.stop();
                        mPlayer.release();
                    }
                    if (isRecording){
                        mRecorder.stop();
                        mRecorder.release();
                    }
                    ArrayList<Recording> selectedRecordingList = new ArrayList<>();
                    selectedRecordingList.add(recording);
                    DeleteProjects.startActionDeleteRecordings(getApplicationContext(),selectedRecordingList);
                    recording = null;
                }else{
                    Toast.makeText(this, "no record found or please wait until file is uploaded", Toast.LENGTH_SHORT).show();
                }
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                break;

            case R.id.ll_share:
                if (recording!=null){
                    shareDialog.show();
                }
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
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
                shareAudioFile(recording,allowSwitch);
                break;
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

    private void shareAudioFile(Recording recording, boolean b){

        JSONArray no_project_rec_ids = new JSONArray();
        no_project_rec_ids.put(recording.getId());

        if (no_project_rec_ids.length()>0){

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
            params.put("config",configArr.toString());
            client.post(APIs.SHARE_RECORDING, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    String responseString = new String(responseBody);
                    Log.e("RESPONSE",responseString);
                    try {
                        JSONObject response = new JSONObject(responseString);
                        shareLink(response.getJSONObject("data").getString("link"),"Share Recording");
                        Toast.makeText(RecordingActivity.this, response.getString("msg"), Toast.LENGTH_SHORT).show();
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


    private void startRecordingTimer(){
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.start();
    }

    // updates the visualizer every 50 milliseconds
    Runnable updateVisualizer = new Runnable() {
        @Override
        public void run() {
            if (isRecording){ // if we are already recording
                // get the current amplitude
                int x = mRecorder.getMaxAmplitude();
                visualizerView.addAmplitude(x); // update the VisualizeView
                visualizerView.invalidate(); // refresh the VisualizerView

                // update in 40 milliseconds
                handler.postDelayed(this, REPEAT_INTERVAL);
            }
        }
    };

    Runnable updateProgressbar = new Runnable() {
        int currentTime;
        Percent percent = new Percent(0);
        double dd;
        @Override
        public void run() {
            if (isPlaying){
                try {
                    currentTime = mPlayer.getCurrentPosition();
                    dd = audioDuration / 100;
                    percent.setPercent((int) (currentTime / dd));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            img_progress.setCurrentValue(percent);
                        }
                    });
                    progressHandler.postDelayed(this, PROGRESS_UPDATE);
                }catch (Exception e){

                }
            }
        }
    };

}
