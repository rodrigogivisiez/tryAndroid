package com.tullyapp.tully;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarFinalValueListener;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.tullyapp.tully.Utils.CatRangeBar;

import java.io.IOException;

import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Utils.formatAudioTime;

public class LoopActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, View.OnClickListener, OnRangeSeekbarFinalValueListener, OnRangeSeekbarChangeListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = LoopActivity.class.getSimpleName();
    public static final String PARAM_START_TIME = "PARAM_START_TIME";
    public static final String PARAM_END_TIME = "PARAM_END_TIME";

    private CatRangeBar range_selector;
    private Button btn_cancel, btn_apply;
    private ImageView play_pause;
    private TextView tv_starttime, tv_endtime, current_time;

    private AppCompatSeekBar pline;

    private static final double THREASHOLD_RATIO = 4.160599;
    private static int THREASHLOD;

    private String main_audio;
    private Handler handler = new Handler();
    private boolean isPaused;
    private MediaPlayer mPlayer;
    private int startPos;
    private int endPos;
    private static int duration;
    private MixpanelAPI mixpanel;
    private int ps;
    private Display display;
    private Point size = new Point();
    private int crh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loop);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Loop Recording");
        main_audio = getIntent().getStringExtra("MAIN");
        initUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mixpanel = MixpanelAPI.getInstance(this, YOUR_PROJECT_TOKEN);
    }

    private void initUI(){
        range_selector = findViewById(R.id.range_selector);
        btn_cancel = findViewById(R.id.btn_cancel);
        btn_apply = findViewById(R.id.btn_apply);
        play_pause = findViewById(R.id.play_pause);

        pline = findViewById(R.id.pline);
        pline.setPadding(0,0,0,0);

        pline.setProgress(0);

        tv_starttime = findViewById(R.id.tv_starttime);
        tv_endtime = findViewById(R.id.tv_endtime);
        current_time = findViewById(R.id.current_time);

        btn_cancel = findViewById(R.id.btn_cancel);
        btn_apply = findViewById(R.id.btn_apply);

        btn_cancel.setOnClickListener(this);
        btn_apply.setOnClickListener(this);

        pline.setOnSeekBarChangeListener(this);

        play_pause.setOnClickListener(this);
        range_selector.setMinValue(0);
        range_selector.setOnRangeSeekbarFinalValueListener(this);
        range_selector.setOnRangeSeekbarChangeListener(this);

        display = getWindowManager().getDefaultDisplay();
        display.getSize(size);

        play();

        current_time.measure(0, 0); //must call measure!
        crh = current_time.getMeasuredWidth();
    }

    private void play(){
        try {
            isPaused = false;
            play_pause.setImageResource(R.drawable.player_pause_icon);
            mPlayer = new MediaPlayer();
            mPlayer.setDataSource(main_audio);
            mPlayer.prepareAsync();
            mPlayer.setLooping(true);
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void togglePlay(){
        try{
            if (mPlayer.isPlaying()){
                mPlayer.pause();
                isPaused = true;
                handler.removeCallbacks(MediaObserver);
                play_pause.setImageResource(R.drawable.player_play_icon);
            }else if (isPaused){
                mPlayer.start();
                isPaused = false;
                play_pause.setImageResource(R.drawable.player_pause_icon);
                handler.post(MediaObserver);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
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
        private int currenttime;
        @Override
        public void run() {
            try {
                currenttime = mPlayer.getCurrentPosition();

                /*if (duration>0){
                    float loc = (currenttime * 100) / duration;
                    float ploc = (size.x-crh) * loc / 100;
                    current_time.setX(ploc);
                    //current_time.animate().translationX(ploc);
                }*/

                current_time.setText(formatAudioTime(currenttime));
                if (currenttime>=endPos){
                    mPlayer.seekTo(startPos);
                }
                pline.setProgress(currenttime);
                handler.postDelayed(this, PROGRESS_UPDATE);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    private static int calMiddleLine(int time){
        float p1 = (time * 100) / duration;
        int f = (int) Math.ceil(p1 * THREASHLOD / 100);
        return time+(THREASHLOD-f);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        endPos = mPlayer.getDuration();
        duration  = mPlayer.getDuration();
        THREASHLOD = (int) ((duration * THREASHOLD_RATIO) / 100);
        pline.setMax(duration);
        range_selector.setMaxValue(endPos);
        tv_endtime.setText(formatAudioTime(endPos));
        mPlayer.start();
        handler.post(MediaObserver);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.play_pause:
                togglePlay();
                break;

            case R.id.btn_cancel:
                onBackPressed();
                break;

            case R.id.btn_apply:
                //Loop function
                mixpanel.track(getString(R.string.loop_function));
                Intent intent = new Intent();
                intent.putExtra(PARAM_START_TIME,startPos);
                intent.putExtra(PARAM_END_TIME,endPos);
                setResult(Activity.RESULT_OK, intent);
                onBackPressed();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void finalValue(Number minValue, Number maxValue) {
        try{
            if (startPos!=minValue.intValue()){
                mPlayer.seekTo(minValue.intValue());
            }
            startPos = minValue.intValue();
            endPos = maxValue.intValue();
            tv_starttime.setText(formatAudioTime(startPos));
            tv_endtime.setText(formatAudioTime(endPos));
        }catch (Exception e){
            e.printStackTrace();
        }
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
        clearPlayer();
        super.onBackPressed();
    }

    @Override
    public void valueChanged(Number minValue, Number maxValue) {
        int s = minValue.intValue();
        int e = maxValue.intValue();
        tv_starttime.setText(formatAudioTime(s));
        tv_endtime.setText(formatAudioTime(e));
        if (ps!=s){
            pline.setProgress(s);
        }
        ps = s;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        if (duration>0){
            float loc = (progress * 100) / duration;
            float ploc = (size.x-crh) * loc / 100;
            current_time.setX(ploc);
            current_time.setText(formatAudioTime(progress));
        }

        if (progress<startPos) {
            pline.setProgress(startPos);
        }
        else if (progress>endPos){
            pline.setProgress(endPos);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        handler.removeCallbacks(MediaObserver);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        try{
            mPlayer.seekTo(seekBar.getProgress());
            handler.removeCallbacks(MediaObserver);
            handler.post(MediaObserver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
