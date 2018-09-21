package com.tullyapp.tully;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
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
import com.tullyapp.tully.Adapters.LyricsPopupListAdapter;
import com.tullyapp.tully.Dictionary.SearchRhym;
import com.tullyapp.tully.FirebaseDataModels.Masters;
import com.tullyapp.tully.Models.LyricsWordSynonym;
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

import cz.msebera.android.httpclient.Header;

import static android.view.Gravity.TOP;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.tullyapp.tully.MasterPlayActivity.INTENT_PARAM_START_TIME;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_ISLOOPING;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_MASTER;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_MASTERS;
import static com.tullyapp.tully.Utils.Utils.formatAudioTime;
import static com.tullyapp.tully.Utils.Utils.getDirectory;
import static com.tullyapp.tully.Utils.Utils.hideSoftKeyboard;
import static com.tullyapp.tully.Utils.Utils.isInternetAvailable;

public class MasterPlayNoteActivity extends AppCompatActivity implements View.OnClickListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener, ExtendedEditText.SelectionChangeListener, NotifyingScrollView.ScrollChangeListener, LyricsPopupListAdapter.LyricsPopupClickEventsListener, SearchRhym.OfflineRhym{

    private ExtendedEditText et_lyrics;
    private ImageView btn_play, repeateToggle, img_cast;

    private TextView tv_startTime, tv_endtime, tv_title;
    private MediaPlayer mPlayer;
    private boolean isPaused = false;
    private boolean isRepeat = false;
    private AppCompatSeekBar musicProgressbar;

    private final PopupWindow popupWindow = new PopupWindow();

    private static final int DEFAULT_WIDTH = -1;
    private static final int DEFAULT_HEIGHT = -1;

    private final Point currLoc = new Point();
    private final Point startLoc = new Point();

    private final Rect cbounds = new Rect();
    private Dialog infoPopupDialog;
    private TextView tv_word_info;

    private LyricsPopupListAdapter lyricsPopupListAdapter;
    private ArrayList<LyricsWordSynonym> lyricsWordSynonymArrayList = new ArrayList<>();
    private ProgressBar popup_progressbar;

    private Handler handler = new Handler();
    private SearchRhym searchRhym;
    private File localdir_masters;
    private Masters master;
    private File mainAudio;
    private String enteredText = "";
    private String lastSavedLyrics = "";
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private int startTime;

    private enum  AUDUI_OUTPUT{
        SPEAKER, EARPHONES, BLUETOOTH
    }
    private AUDUI_OUTPUT AOUTPUT = AUDUI_OUTPUT.SPEAKER;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_play_note);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Play - Rhyme");

        isRepeat = getIntent().getBooleanExtra(INTENT_PARAM_ISLOOPING,false);
        master = (Masters) getIntent().getSerializableExtra(INTENT_PARAM_MASTER);
        startTime = getIntent().getIntExtra(INTENT_PARAM_START_TIME,0);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid()).child("masters").child(master.getId());

        if (master!=null){

            localdir_masters = getDirectory(getApplicationContext(),LOCAL_DIR_NAME_MASTERS);

            initUI();

            searchRhym = new SearchRhym(this);
            searchRhym.setOfflineRhymListener(this);

            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
    }

    private void initUI(){
        et_lyrics = findViewById(R.id.et_lyrics);
        tv_startTime = findViewById(R.id.tv_startTime);
        tv_endtime= findViewById(R.id.tv_endtime);
        repeateToggle = findViewById(R.id.repeateToggle);

        if (isRepeat)
            repeateToggle.setImageResource(R.drawable.repeate_active_icon);
        else
            repeateToggle.setImageResource(R.drawable.repeat);

        tv_title = findViewById(R.id.tv_title);
        tv_title.setText(master.getName());

        img_cast = findViewById(R.id.img_cast);

        infoPopupDialog = new Dialog(this, R.style.MyDialogTheme);
        infoPopupDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        infoPopupDialog.setContentView(R.layout.word_info_popup);
        infoPopupDialog.setCancelable(true);
        infoPopupDialog.setCanceledOnTouchOutside(true);
        tv_word_info = infoPopupDialog.findViewById(R.id.tv_word_info);
        ImageView btn_close = infoPopupDialog.findViewById(R.id.btn_close);

        final LayoutInflater inflater = LayoutInflater.from(this);
        View tooltip_layout = inflater.inflate(R.layout.tooltip_layout, null);

        tooltip_layout.bringToFront();

        popupWindow.setContentView(tooltip_layout);
        popupWindow.setWidth(WRAP_CONTENT);
        popupWindow.setHeight(WRAP_CONTENT);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);
        popupWindow.setFocusable(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            popupWindow.setOverlapAnchor(true);
        }

        RecyclerView recycle_view = tooltip_layout.findViewById(R.id.recycle_view);
        popup_progressbar = tooltip_layout.findViewById(R.id.popup_progressbar);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recycle_view.setLayoutManager(linearLayoutManager);

        lyricsPopupListAdapter = new LyricsPopupListAdapter(this,lyricsWordSynonymArrayList,this);
        recycle_view.setAdapter(lyricsPopupListAdapter);

        repeateToggle.setOnClickListener(this);
        btn_close.setOnClickListener(this);

        musicProgressbar = findViewById(R.id.appCompatSeekBar);
        musicProgressbar.setOnSeekBarChangeListener(this);

        btn_play = findViewById(R.id.btn_play);
        btn_play.setOnClickListener(this);

        if (master.getLyrics()!=null && !master.getLyrics().isEmpty()){
            et_lyrics.setText(master.getLyrics());
        }

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

        /*et_lyrics.setEditBackPressedListener(new ExtendedEditText.EditBackPressedListener() {
            @Override
            public void onEditBackPressed() {
            if (!isFullView){
                etparent.requestFocus();
            }
            }
        });*/

        et_lyrics.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

            }
        });

        img_cast.setOnClickListener(this);

        et_lyrics.setSelectionChangeListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            popupWindow.setOverlapAnchor(true);
            popupWindow.setElevation(10);
        }

        et_lyrics.requestFocusFromTouch();
        et_lyrics.requestFocus();

        mainAudio = new File(localdir_masters, master.getFilename());

        tv_startTime.setText(formatAudioTime(startTime));

        togglePlay();
    }

    private void saveLyrics(String txt){
        lastSavedLyrics = txt;
        master.setLyrics(txt);
        Toast.makeText(this, "Saving lyrics", Toast.LENGTH_SHORT).show();
        mDatabase.child("lyrics").setValue(txt);
    }

    private void audioCast(){
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        //Log.e(TAG,audioManager.getMode()+" : "+audioManager.isWiredHeadsetOn()+" : "+audioManager.isSpeakerphoneOn()+" : "+audioManager.isBluetoothScoOn()+" : "+audioManager.getRingerMode());
        alt_bld.setTitle("Audio Output");
        alt_bld.setItems(R.array.audio_output, new DialogInterface
                .OnClickListener() {
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
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.btn_play:
                togglePlay();
                break;

            case R.id.repeateToggle:
                isRepeat = !isRepeat;
                if (isRepeat)
                    repeateToggle.setImageResource(R.drawable.repeate_active_icon);
                else
                    repeateToggle.setImageResource(R.drawable.repeat);

                if (mPlayer!=null){
                    try{
                        mPlayer.setLooping(isRepeat);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                break;

            case R.id.btn_close:
                infoPopupDialog.dismiss();
                break;

            case R.id.img_cast:
                audioCast();
                break;
        }
    }


    private void play(){
        if (mainAudio.exists()){
            String path = mainAudio.getAbsolutePath();
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
                    mPlayer.pause();
                    int currentPosition = seekBar.getProgress();
                    mPlayer.seekTo(currentPosition);
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

    Runnable MediaObserver = new Runnable() {
        static final long PROGRESS_UPDATE = 300;
        private int currenttime;

        @Override
        public void run() {
            try {
                currenttime = mPlayer.getCurrentPosition();
                musicProgressbar.setProgress(currenttime);
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
    public void onTextUnselected() {
        popupWindow.dismiss();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        try{
            mPlayer.seekTo(startTime);
            startTime = 0;
            mPlayer.start();
            int duration = mPlayer.getDuration();
            musicProgressbar.setMax(duration);
            tv_endtime.setText(formatAudioTime(duration));
            handler.post(MediaObserver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        clearPlayer();
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

    @Override
    public void onBackPressed() {
        clearPlayer();
        Intent intent = new Intent();
        if (!lastSavedLyrics.equals(et_lyrics.getText().toString().trim())) {
            saveLyrics(et_lyrics.getText().toString().trim());
            intent.putExtra(INTENT_PARAM_MASTER,master);
            setResult(Activity.RESULT_OK, intent);
        }else{
            setResult(Activity.RESULT_CANCELED, intent);
        }

        super.onBackPressed();
    }

    private void clearPlayer(){
        handler.removeCallbacks(MediaObserver);
        if (mPlayer!=null){
            try{
                mPlayer.stop();
                mPlayer.release();
            }catch (Exception ignored){

            }
        }
        musicProgressbar.setProgress(0);
        tv_startTime.setText(R.string._00_00);
        btn_play.setImageResource(R.drawable.player_play_icon);
    }

    @Override
    public void onScrollChanged() {
        if (popupWindow.isShowing()) {
            final Point ploc = calculatePopupLocation();
            popupWindow.update(ploc.x, ploc.y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }
    }

    @Override
    public void onTextSelected() {
        final View popupContent = popupWindow.getContentView();
        final int startSelection=et_lyrics.getSelectionStart();
        final int endSelection=et_lyrics.getSelectionEnd();

        try {
            String txt = et_lyrics.getText().toString().substring(startSelection, endSelection);
            fetchSynonym(txt);
            /*et_lyrics.post(new Runnable() {
                @Override
                public void run() {
                et_lyrics.setSelection(et_lyrics.length());
                }
            });*/
            hideSoftKeyboard(MasterPlayNoteActivity.this);
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
                                final Rect cframe = new Rect();
                                final int[] cloc = new int[2];
                                popupContent.getLocationOnScreen(cloc);
                                popupContent.getLocalVisibleRect(cbounds);
                                popupContent.getWindowVisibleDisplayFrame(cframe);

                                final int scrollY = ((View) et_lyrics.getParent()).getScrollY();
                                final int[] tloc = new int[2];
                                et_lyrics.getLocationInWindow(tloc);

                                final int startX = cloc[0] + cbounds.centerX();
                                final int startY = cloc[1] + cbounds.centerY() - (tloc[1] - cframe.top) - scrollY;
                                startLoc.set(startX, startY);

                                final Point ploc = calculatePopupLocation();
                                popupWindow.update(ploc.x, ploc.y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
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



    /** Used to calculate where we should position the {@link PopupWindow} */
    private Point calculatePopupLocation() {
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
    }
}
