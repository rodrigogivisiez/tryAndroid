package com.tullyapp.tully.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.tullyapp.tully.R;

public class AudioAnalyzeService extends IntentService {

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.tullyapp.tully.Services.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.tullyapp.tully.Services.extra.PARAM2";
    private static final String ACTION_ANALYZE_AUDIO = "ACTION_ANALYZE_AUDIO";
    private static final String PARAM_AUDIO_PATH = "PARAM_AUDIO_PATH";
    private static final String TAG = AudioAnalyzeService.class.getSimpleName();
    private static final long PROGRESS_UPDATE = 10;
    public static final String PROGRESS_VALUE = "PROGRESS_VALUE";
    public static final String ACTION_ANALYZE_PROGRESS = "ACTION_ANALYZE_PROGRESS";
    public static final String INTENT_PARAM_BPM = "INTENT_PARAM_BPM";
    public static final String INTENT_PARAM_KEY = "INTENT_PARAM_KEY";
    private Handler handler;
    private LocalBroadcastManager lbm;
    private Intent sendIntent;

    public AudioAnalyzeService() {
        super("AudioAnalyzeService");
    }

    public static void startAnalyzingAudio(Context context, String path){
        Intent intent = new Intent(context, AudioAnalyzeService.class);
        intent.setAction(ACTION_ANALYZE_AUDIO);
        intent.putExtra(PARAM_AUDIO_PATH, path);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "checking pending deletions", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();
            startForeground(1, notification);
        }

        if (intent != null) {
            final String action = intent.getAction();
            switch (action){
                case ACTION_ANALYZE_AUDIO:
                    String path = intent.getStringExtra(PARAM_AUDIO_PATH);
                    startAnalyzeAudio(path);
                    break;
            }
        }
    }

    private void startAnalyzeAudio(String path){
        sendIntent = new Intent(ACTION_ANALYZE_PROGRESS);
        handler = new Handler();
        this.lbm = LocalBroadcastManager.getInstance(getApplicationContext());
        new AnalyzeAudioAsync(path).execute();
    }

    class AnalyzeAudioAsync extends AsyncTask<String, Void, Boolean> {
        String path;

        AnalyzeAudioAsync(String path) {
            this.path = path;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.e(TAG,"STARTING ANALYZING");
            init();
            //handler.post(getProgress);
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            return analyzeAudio(path);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean){
                sendIntent.putExtra(INTENT_PARAM_BPM,getBpm());
                sendIntent.putExtra(INTENT_PARAM_KEY,getKey());
                lbm.sendBroadcast(sendIntent);
            }
            else{
                Log.e(TAG,"ANALYZE FINISHED");
                sendIntent.putExtra(INTENT_PARAM_BPM,0);
                sendIntent.putExtra(INTENT_PARAM_KEY,"-");
                lbm.sendBroadcast(sendIntent);
            }
        }
    }

    Runnable getProgress = new Runnable(){
        private int p = 0;
        @Override
        public void run() {
            p = getPercent();
            //sendIntent.putExtra(PROGRESS_VALUE,p);
            handler.postDelayed(this,PROGRESS_UPDATE);
        }
    };

    private native void init();
    private native boolean analyzeAudio(String path);
    private native float getBpm();
    private native String getKey();
    private native int getPercent();

    static {
        System.loadLibrary("Analyze");
    }
}
