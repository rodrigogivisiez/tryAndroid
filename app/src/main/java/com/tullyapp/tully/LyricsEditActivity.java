package com.tullyapp.tully;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.tullyapp.tully.Adapters.LyricsPopupListAdapter;
import com.tullyapp.tully.Dialogs.TutorialScreen;
import com.tullyapp.tully.Dictionary.SearchRhym;
import com.tullyapp.tully.FirebaseDataModels.Lyrics;
import com.tullyapp.tully.FirebaseDataModels.LyricsModule.LyricsAppModel;
import com.tullyapp.tully.Models.LyricsWordSynonym;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.Configuration;
import com.tullyapp.tully.Utils.NotifyingScrollView;
import com.tullyapp.tully.Utils.NotifyingSelectionEditText;
import com.tullyapp.tully.Utils.ViewUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.security.KeyStore;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

import static android.view.Gravity.TOP;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_SAVE_LYRICS;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.RESPONSE_PARAM;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_LYRICS;
import static com.tullyapp.tully.Utils.Constants.TUTORIAL_SCREENS;
import static com.tullyapp.tully.Utils.Constants.TUTS_LYRICS;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Utils.hideSoftKeyboard;
import static com.tullyapp.tully.Utils.Utils.isInternetAvailable;

public class LyricsEditActivity extends AppCompatActivity implements NotifyingScrollView.ScrollChangeListener, NotifyingSelectionEditText.SelectionChangeListener, LyricsPopupListAdapter.LyricsPopupClickEventsListener, View.OnClickListener, SearchRhym.OfflineRhym {

    private String enteredText = "";
    private LyricsAppModel lyricsAppModel;
    private boolean isNewLyrics = false;
    private boolean requestedForId = false;
    private Intent intent;
    private ResponseReceiver responseReceiver;
    private String initialLyrics = "";

    private static final int DEFAULT_WIDTH = -1;
    private static final int DEFAULT_HEIGHT = -1;

    private final Point currLoc = new Point();
    private final Point startLoc = new Point();

    private final Rect cbounds = new Rect();
    private final PopupWindow popupWindow = new PopupWindow();

    private ProgressBar popup_progressbar;

    private NotifyingSelectionEditText et_lyrics;
    private LyricsPopupListAdapter lyricsPopupListAdapter;
    private ArrayList<LyricsWordSynonym> lyricsWordSynonymArrayList = new ArrayList<>();
    private Dialog dialog;
    private TextView tv_word_info;
    private MixpanelAPI mixpanel;
    private SearchRhym searchRhym;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics_edit);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Write – Create");
        intent = getIntent();
        mixpanel = MixpanelAPI.getInstance(this, YOUR_PROJECT_TOKEN);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());

        initUI();
        registerReceivers();
        searchRhym = new SearchRhym(this);
        searchRhym.setOfflineRhymListener(this);
    }

    private void initUI(){

        dialog = new Dialog(LyricsEditActivity.this, R.style.MyDialogTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.word_info_popup);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        tv_word_info = dialog.findViewById(R.id.tv_word_info);
        ImageView btn_close = dialog.findViewById(R.id.btn_close);
        btn_close.setOnClickListener(this);

        et_lyrics = findViewById(R.id.et_lyrics);

        final LayoutInflater inflater = LayoutInflater.from(this);
        View tooltip_layout = inflater.inflate(R.layout.tooltip_layout, null);

        popupWindow.setContentView(tooltip_layout);
        popupWindow.setWidth(WRAP_CONTENT);
        popupWindow.setHeight(WRAP_CONTENT);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);
        popupWindow.setFocusable(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            popupWindow.setOverlapAnchor(true);
            popupWindow.setElevation(10);
        }

        RecyclerView recycle_view = tooltip_layout.findViewById(R.id.recycle_view);
        popup_progressbar = tooltip_layout.findViewById(R.id.popup_progressbar);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recycle_view.setLayoutManager(linearLayoutManager);

        lyricsPopupListAdapter = new LyricsPopupListAdapter(this,lyricsWordSynonymArrayList,this);
        recycle_view.setAdapter(lyricsPopupListAdapter);

        // Initialize the popup content, only add it to the Window once we've selected text
        lyricsAppModel = (LyricsAppModel) intent.getSerializableExtra(INTENT_PARAM_LYRICS);
        if (lyricsAppModel==null){
            lyricsAppModel = new LyricsAppModel();
            lyricsAppModel.setLyrics(new Lyrics());
            lyricsAppModel.setOfProject(false);
            isNewLyrics = true;
        }else{
            et_lyrics.setText(lyricsAppModel.getLyrics().getDesc());
            initialLyrics = lyricsAppModel.getLyrics().getDesc();
        }

        // Initialize to the NotifyingScrollView to observe scroll changes
        NotifyingScrollView notifyingScrollView = findViewById(R.id.notifying_scroll_view);
        notifyingScrollView.setScrollChangeListener(this);

        et_lyrics.setSelectionChangeListener(this);

        responseReceiver = new ResponseReceiver();

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

                    if (isNewLyrics){
                        if (!requestedForId){
                            requestedForId = true;
                            saveLyrics(enteredText);
                        }
                    }else{
                        saveLyrics(enteredText);
                    }

                }
            }
        });

        if (!Configuration.lyrics_tuts) showTutorailDialog(TUTS_LYRICS);
    }

    private void showTutorailDialog(String tut) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        TutorialScreen tutorialScreen = TutorialScreen.newInstance(tut);
        tutorialScreen.show(ft,TutorialScreen.class.getSimpleName());
        tutorialScreen.setOnTutorialClosed(new TutorialScreen.OnTutorialClosed() {
            @Override
            public void onTutsClosed(String tut) {
            mDatabase.child(TUTORIAL_SCREENS).child(tut).setValue(true);
            Configuration.lyrics_tuts = true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void saveLyrics(String txt){
        lyricsAppModel.getLyrics().setDesc(txt);
        FirebaseDatabaseOperations.startActionsaveLyric(getApplicationContext(),lyricsAppModel);
    }

    private void fetchSynonym(String key){
        key = key.trim();
        lyricsWordSynonymArrayList.clear();
        lyricsPopupListAdapter.notifyDataSetChanged();
        popup_progressbar.setVisibility(View.VISIBLE);
        mixpanel.track("Rhyme initiated");
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
                LyricsWordSynonym lyricsWordSynonym;
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    popup_progressbar.setVisibility(View.GONE);
                    String responseString = new String(responseBody);
                    try {
                        JSONArray response = new JSONArray(responseString);
                        int len = response.length();

                        if (len>0){

                            lyricsWordSynonym = new LyricsWordSynonym("",false,null);
                            lyricsWordSynonymArrayList.add(lyricsWordSynonym);

                            for(int i = 0; i<len; i++){

                                lyricsWordSynonym = new LyricsWordSynonym(
                                        response.getJSONObject(i).getString("word"),
                                        false,
                                        (response.getJSONObject(i).has("defs") ? response.getJSONObject(i).getJSONArray("defs") : null)
                                );

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

        } else{
            searchRhym.rhym(key);
        }
    }

    @Override
    public void onRhymReceive(ArrayList<String> rhymStrings) {
        try{
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
        catch (Exception e){
            e.printStackTrace();
            popupWindow.dismiss();
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

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_SAVE_LYRICS);
        LocalBroadcastManager.getInstance(LyricsEditActivity.this).registerReceiver(responseReceiver, filter);
    }

    @Override
    public void onScrollChanged() {
        if (popupWindow.isShowing()) {
            final Point ploc = calculatePopupLocation();
            if (ploc!=null){
                popupWindow.update(ploc.x, ploc.y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            }
        }
    }

    @Override
    public void onTextSelected() {

        final View popupContent = popupWindow.getContentView();
        final int startSelection=et_lyrics.getSelectionStart();
        final int endSelection=et_lyrics.getSelectionEnd();

        try{
            String txt = et_lyrics.getText().toString().substring(startSelection, endSelection);
            fetchSynonym(txt);
            /*et_lyrics.post(new Runnable() {
                @Override
                public void run() {
                    et_lyrics.setSelection(et_lyrics.length());
                }
            });*/
            hideSoftKeyboard(LyricsEditActivity.this);
            popupContent.requestFocus();
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            if (popupWindow.isShowing()) {
                final Point ploc = calculatePopupLocation();
                if (ploc!=null){
                    popupWindow.update(ploc.x, ploc.y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
                }
            } else {
                // Add the popup to the Window and position it relative to the selected text bounds
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
                        if (ploc!=null)
                            popupWindow.update(ploc.x, ploc.y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
                        }
                    });
                    }
                });
            }
        }catch (Exception e){
            Log.e("Error",e.getMessage());
        }
    }


    @Override
    public void onTextUnselected() {
        popupWindow.dismiss();
    }

    @Override
    public void showWordInfoListener(String word, JSONArray description) {
        try {
            mixpanel.track("Definition Selected");
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
            dialog.show();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void itemSelected(String word) {
        int start = Math.max(et_lyrics.getSelectionStart(), 0);
        int end = Math.max(et_lyrics.getSelectionEnd(), 0);
        et_lyrics.getText().replace(Math.min(start, end), Math.max(start, end), word, 0, word.length());
        popupWindow.dismiss();
        mixpanel.track("Rhyme Selected");
        //saveLyrics(et_lyrics.getText().toString());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_close:
                dialog.dismiss();
                break;
        }
    }

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case ACTION_SAVE_LYRICS:
                    Log.e("LYRICS_UPDATE","LYRICS");
                    String id = intent.getStringExtra(RESPONSE_PARAM);
                    if (id!=null){
                        lyricsAppModel.getLyrics().setId(id);
                        isNewLyrics = false;
                    }
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        boolean saveAtExist = false;
        String val = et_lyrics.getText().toString().trim();
        if (lyricsAppModel.getLyrics().getId()!=null && isNewLyrics){
            saveLyrics(val);
            saveAtExist = true;
            mixpanel.track("Writing lyrics");
            int lyricsLength = val.length();

            if (lyricsLength>=100 && lyricsLength<250)
                mixpanel.track("100 word count");
            else if (lyricsLength>=250 && lyricsLength<=500)
                mixpanel.track("250 word count");
            else if (lyricsLength>=500)
                mixpanel.track("500 word count");

        }
        else if (val.length()>0){
            if (!isNewLyrics){
                if (!initialLyrics.equals(et_lyrics.getText().toString())){
                    saveLyrics(val);
                    saveAtExist = true;
                    mixpanel.track("Update lyrics in project");
                }
            }else{
                saveLyrics(val);
                saveAtExist = true;
                mixpanel.track("Update lyrics");
            }
        }

        Intent returnIntent = new Intent();
        if (saveAtExist){
            setResult(Activity.RESULT_OK,returnIntent);
        }else{
            setResult(Activity.RESULT_CANCELED,returnIntent);
        }
        super.onBackPressed();
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(LyricsEditActivity.this).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){

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
        }
        catch (Exception e){
            return null;
        }

    }
}
