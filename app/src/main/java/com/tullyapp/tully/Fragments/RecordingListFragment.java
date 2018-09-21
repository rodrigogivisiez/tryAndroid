package com.tullyapp.tully.Fragments;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
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
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatCheckBox;
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
import android.view.animation.LayoutAnimationController;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.tullyapp.tully.Adapters.RecordingListAdapter;
import com.tullyapp.tully.Dialogs.TutorialScreen;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.Interface.AuthToken;
import com.tullyapp.tully.Models.OrganizedRec;
import com.tullyapp.tully.R;
import com.tullyapp.tully.RecordingActivity;
import com.tullyapp.tully.Services.DeleteProjects;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.Configuration;
import com.tullyapp.tully.Utils.Constants;
import com.tullyapp.tully.Utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import cz.msebera.android.httpclient.Header;

import static android.app.Activity.RESULT_OK;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_RECORDINGS;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_SEARCH_RECORDING;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.DB_PARAM;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_RECORDINGS;
import static com.tullyapp.tully.Utils.Constants.MUSIC_OBJECT;
import static com.tullyapp.tully.Utils.Constants.ON_CHECKED;
import static com.tullyapp.tully.Utils.Constants.ON_LONG_PRESSED;
import static com.tullyapp.tully.Utils.Constants.ON_PAUSE;
import static com.tullyapp.tully.Utils.Constants.ON_PLAY;
import static com.tullyapp.tully.Utils.Constants.ON_RESUME;
import static com.tullyapp.tully.Utils.Constants.POSITION_PARAM;
import static com.tullyapp.tully.Utils.Constants.TUTORIAL_SCREENS;
import static com.tullyapp.tully.Utils.Constants.TUTS_RECORDING;
import static com.tullyapp.tully.Utils.Constants.WIDGET_DELETE;
import static com.tullyapp.tully.Utils.Constants.WIDGET_RENAME;
import static com.tullyapp.tully.Utils.Constants.WIDGET_SHARE;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Utils.getDirectory;
import static com.tullyapp.tully.Utils.Utils.showAlert;
import static com.tullyapp.tully.Utils.ViewUtils.shareAllowDownloadPopup;

/**
 * A simple {@link Fragment} subclass.
 */
public class RecordingListFragment extends BaseFragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, RecordingListAdapter.OnWidgetAction {

    public static final int NEW_RECORDING_REQUEST_CODE = 123;
    private static final String TAG = RecordingListFragment.class.getSimpleName();
    private LinearLayout no_recordings;
    private ResponseReceiver responseReceiver;
    protected SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private RecyclerView recycle_view;
    private LinearLayout widget_share;
    private LinearLayout widget_delete;
    private LinearLayout widget_check_all;
    private AppCompatCheckBox share_widget_checkbox_button;
    private LinearLayout share_widget_layout;

    private ArrayList<OrganizedRec> recordingAppModels = new ArrayList<>();

    private RecordingListAdapter recordingListAdapter;

    private MediaPlayer mPlayer = null;
    private File localdir;
    private File localFile;
    private int currentPos;
    private Recording recordingObject;
    private int audioDuration;
    private Context context;
    private FragmentActivity mActivity;
    private FirebaseAuth mAuth;
    private MixpanelAPI mixpanel;
    private Handler handler = new Handler();

    private Dialog shareDialog;
    private AppCompatButton btn_allow, btn_not_allow;

    private static final CharSequence selectionMenuItems[] = new CharSequence[] {"Rename", "Share", "Delete"};
    private AlertDialog.Builder optionMenu;

    private ArrayList<Recording> selectedRecordingList;
    private Dialog renameDialog;
    private EditText et_popup_input;
    private TextView tv_pop_title, tv_pop_desc;
    private AppCompatButton btn_popup_cancel, btn_popup_rename, btn_cp_url;
    private Recording renameRecordingObj;
    private int renamePos;
    private DatabaseReference mDatabase;
    private ImageView btn_close;
    private boolean isFromSearch;
    private OrganizedRec organizedRec;
    private int opos;
    private boolean allowDownload;
    private AuthToken authToken;
    private SwitchCompat switch_expire_after_once, switch_expire_after_one_hour, switch_expire_never;
    private boolean allowSwitch = true;

    private enum OBJ_TYPE {ORGANIZED,RECORDING}
    private OBJ_TYPE objType;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        mActivity = (FragmentActivity) context;
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        authToken = new AuthToken() {
            @Override
            public void onToken(String token, String callback) {
                switch (callback){
                    case "shareAudioLink":
                        shareAudioLink(token);
                        break;
                }
            }
        };
    }

    public RecordingListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
        mixpanel = MixpanelAPI.getInstance(getContext(), YOUR_PROJECT_TOKEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_recording_list, container, false);
        no_recordings = view.findViewById(R.id.no_recordings);
        progressBar = view.findViewById(R.id.progressBar);
        recycle_view = view.findViewById(R.id.recycle_view);
        widget_share = view.findViewById(R.id.widget_share);
        widget_delete = view.findViewById(R.id.widget_delete);
        widget_check_all = view.findViewById(R.id.widget_check_all);
        share_widget_checkbox_button = view.findViewById(R.id.share_widget_checkbox_button);
        share_widget_layout = view.findViewById(R.id.share_widget);

        widget_share.setOnClickListener(this);
        widget_delete.setOnClickListener(this);
        widget_check_all.setOnClickListener(this);
        share_widget_checkbox_button.setOnClickListener(this);
        share_widget_checkbox_button.setOnCheckedChangeListener(this);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        localdir = getDirectory(context,LOCAL_DIR_NAME_RECORDINGS);
        recordingListAdapter = new RecordingListAdapter(context,recordingAppModels);
        recordingListAdapter.setOnWidgetAction(this);
        recycle_view.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_from_bottom);
        recycle_view.setLayoutAnimation(animation);
        recycle_view.setAdapter(recordingListAdapter);
        recycle_view.setHasFixedSize(true);
        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchRecordings();
            }
        });

        shareDialog = shareAllowDownloadPopup(getContext());

        btn_close = shareDialog.findViewById(R.id.btn_close);
        renameDialog = new Dialog(getContext(), R.style.MyDialogTheme);
        renameDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        renameDialog.setContentView(R.layout.input_popup);
        renameDialog.setCancelable(false);
        renameDialog.setCanceledOnTouchOutside(false);
        btn_close.setOnClickListener(this);

        et_popup_input = renameDialog.findViewById(R.id.et_popup_input);
        tv_pop_title = renameDialog.findViewById(R.id.tv_pop_title);
        tv_pop_desc = renameDialog.findViewById(R.id.tv_pop_desc);
        et_popup_input = renameDialog.findViewById(R.id.et_popup_input);

        tv_pop_title.setText(getString(R.string.rename_file));

        btn_popup_rename = renameDialog.findViewById(R.id.btn_popup_create);
        btn_popup_cancel = renameDialog.findViewById(R.id.btn_popup_cancel);

        btn_popup_rename.setText(R.string.rename_cap);

        btn_popup_rename.setOnClickListener(this);
        btn_popup_cancel.setOnClickListener(this);

        btn_allow = shareDialog.findViewById(R.id.btn_allow);
        btn_not_allow = shareDialog.findViewById(R.id.btn_not_allow);
        switch_expire_after_once = shareDialog.findViewById(R.id.switch_expire_after_once);
        switch_expire_after_one_hour = shareDialog.findViewById(R.id.switch_expire_after_one_hour);
        switch_expire_never = shareDialog.findViewById(R.id.switch_expire_never);
        btn_cp_url = shareDialog.findViewById(R.id.btn_cp_url);
        switch_expire_after_once.setOnCheckedChangeListener(this);
        switch_expire_after_one_hour.setOnCheckedChangeListener(this);
        switch_expire_never.setOnCheckedChangeListener(this);

        btn_cp_url.setOnClickListener(this);
        btn_allow.setOnClickListener(this);
        btn_not_allow.setOnClickListener(this);

        optionMenu = new AlertDialog.Builder(getContext());
        //optionMenu.setTitle("Select");
        optionMenu.setItems(selectionMenuItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        if (objType == OBJ_TYPE.RECORDING) et_popup_input.setText(renameRecordingObj.getName());
                        else if (objType == OBJ_TYPE.ORGANIZED) et_popup_input.setText(organizedRec.getRecordingList().get(0).getProjectName());
                        et_popup_input.requestFocus();
                        renameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                        renameDialog.show();
                        break;

                    case 1:
                        if (selectedRecordingList==null){
                            selectedRecordingList = new ArrayList<>();
                        }
                        selectedRecordingList.clear();
                        if (objType == OBJ_TYPE.RECORDING) {

                            selectedRecordingList.add(renameRecordingObj);
                        }
                        else if (objType == OBJ_TYPE.ORGANIZED){
                            selectedRecordingList = organizedRec.getRecordingList();
                        }
                        shareDialog.show();
                        break;

                    case 2:
                        if (selectedRecordingList==null){
                            selectedRecordingList = new ArrayList<>();
                        }
                        selectedRecordingList.clear();
                        if (objType == OBJ_TYPE.RECORDING) {
                            selectedRecordingList.add(renameRecordingObj);
                        }
                        else if (objType == OBJ_TYPE.ORGANIZED){
                            selectedRecordingList = organizedRec.getRecordingList();
                        }
                        deleteConfirmation(selectedRecordingList);
                        break;
                }
            }
        });

        responseReceiver = new ResponseReceiver();
        registerReceivers();
        fetchRecordings();

        if (!Configuration.recording_tuts) showTutorailDialog(TUTS_RECORDING);
    }

    private void showTutorailDialog(String tut) {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        TutorialScreen tutorialScreen = TutorialScreen.newInstance(tut);
        tutorialScreen.show(ft,TutorialScreen.class.getSimpleName());
        tutorialScreen.setOnTutorialClosed(new TutorialScreen.OnTutorialClosed() {
            @Override
            public void onTutsClosed(String tut) {
            mDatabase.child(TUTORIAL_SCREENS).child(tut).setValue(true);
            Configuration.recording_tuts = true;
            }
        });
    }

    private void runLayoutAnimation() {
        final LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_from_bottom);
        recycle_view.setLayoutAnimation(controller);
        recycle_view.getAdapter().notifyDataSetChanged();
        recycle_view.scheduleLayoutAnimation();
    }

    @Override
    public void onSearchKey(String searchKey) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseDatabaseOperations.startSearchRecording(context,searchKey);
    }

    @Override
    public void onSearchCancelled() {
        fetchRecordings();
    }

    @Override
    public void fabButtonClicked() {
        Intent intent = new Intent(context,RecordingActivity.class);
        FloatingActionButton ll = mActivity.findViewById(R.id.fab);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(mActivity, ll, ViewCompat.getTransitionName(ll));
        mActivity.startActivityFromFragment(this,intent, NEW_RECORDING_REQUEST_CODE, options.toBundle());
    }

    @Override
    public void actionEvent(int event) {
        switch (event){
            case R.id.action_options:
                if (share_widget_layout.getVisibility()==View.VISIBLE){
                    toogleWidget(false);
                }
                else{
                    toogleWidget(true);
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case NEW_RECORDING_REQUEST_CODE:
                if (resultCode == RESULT_OK){
                    fetchRecordings();
                }
                break;
        }
    }

    public void fetchRecordings(){
        progressBar.setVisibility(View.VISIBLE);
        FirebaseDatabaseOperations.startAction(context,ACTION_PULL_RECORDINGS);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.widget_share:
                    if (objType == OBJ_TYPE.RECORDING) shareRecordings();
                    else if (objType == OBJ_TYPE.ORGANIZED) shareOrganizedRecording();
                break;

            case R.id.widget_check_all:
                share_widget_checkbox_button.setChecked(!share_widget_checkbox_button.isChecked());
                break;

            case R.id.widget_delete:
                if (objType == OBJ_TYPE.RECORDING) {
                    selectedRecordingList = recordingListAdapter.getSelectedRecordings();
                    if (selectedRecordingList.size()==0){
                        Toast.makeText(context, "No recording selected", Toast.LENGTH_SHORT).show();
                    }else{
                        deleteConfirmation(selectedRecordingList);
                    }
                    toogleWidget(false);
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
                shareDialog.dismiss();
                shareAudioFile(selectedRecordingList,allowSwitch);
                break;

            case R.id.btn_popup_create:
                String val = et_popup_input.getText().toString().trim();
                if (!val.isEmpty()){
                    if (objType == OBJ_TYPE.RECORDING) {
                        rename(renameRecordingObj,val,renamePos);
                    }
                    else if (objType == OBJ_TYPE.ORGANIZED) {
                        rename(organizedRec,val,opos);
                    }
                    renameDialog.dismiss();
                }
                break;

            case R.id.btn_popup_cancel:
                renameDialog.dismiss();
                break;

            case R.id.btn_close:
                shareDialog.dismiss();
                break;
        }
    }

    private void shareOrganizedRecording(){
        selectedRecordingList = organizedRec.getRecordingList();
        if (selectedRecordingList!=null && selectedRecordingList.size()>0){

        }
    }

    private void shareRecordings(){
        selectedRecordingList = recordingListAdapter.getSelectedRecordings();
        if (selectedRecordingList.size()==0){
            Toast.makeText(context, "No recording selected", Toast.LENGTH_SHORT).show();
        }
        else if (selectedRecordingList.size()>1){
            boolean isMix = false;
            String pid = "";
            for (Recording r : selectedRecordingList){
                if (r.isOfProject()){
                    if (pid.isEmpty()){
                        pid = r.getProjectId();
                    }
                    else{
                        if (!pid.equals(r.getProjectId())){
                            isMix = true;
                            break;
                        }
                    }
                }
                else{
                    isMix = true;
                    break;
                }
            }

            if (isMix){
                Toast.makeText(context, "Multiple Share is not supported currently", Toast.LENGTH_SHORT).show();
            }
            else{
                shareDialog.show();
            }
        }
        else{
            shareDialog.show();
        }
        toogleWidget(false);
    }

    private void shareAudioFile(ArrayList<Recording> recordings, boolean b){
        selectedRecordingList = recordings;
        allowDownload = b;
        Utils.getAuthToken(getContext(),mAuth,authToken,"shareAudioLink");
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

        JSONArray no_project_rec_ids = new JSONArray();
        JSONArray project_rec_ids = new JSONArray();

        for (Recording rec : selectedRecordingList){
            if (rec.isOfProject()){
                try {
                    JSONObject obj = new JSONObject();
                    obj.put(rec.getProjectId(),rec.getId());
                    project_rec_ids.put(obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else{
                no_project_rec_ids.put(rec.getId());
            }
        }

        if (no_project_rec_ids.length()>0 || project_rec_ids.length()>0){

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
            params.put("no_project_rec_ids", no_project_rec_ids.toString());
            params.put("project_recs", project_rec_ids.toString());
            params.put("config",configArr.toString());

            client.post(APIs.SHARE_RECORDING, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    String responseString = new String(responseBody);
                    Log.e("RESPONSE",responseString);
                    try {
                        JSONObject response = new JSONObject(responseString);
                        shareLink(response.getJSONObject("data").getString("link"),"Share Recording");
                        Toast.makeText(getContext(), response.getString("msg"), Toast.LENGTH_SHORT).show();
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

    private void deleteConfirmation(final ArrayList<Recording> selectedRecordingList) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.sure_delete_message_title_recording)
            .setMessage(R.string.sure_delete_message_recording)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    deleteSelection(selectedRecordingList);
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

    private void deleteSelection(ArrayList<Recording> selectedRecordingList){
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(context, "Deleting selected recording", Toast.LENGTH_SHORT).show();
        DeleteProjects.startActionDeleteRecordings(context,selectedRecordingList);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.share_widget_checkbox_button:
                recordingListAdapter.setCheckedAll(share_widget_checkbox_button.isChecked());
                break;

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

    @Override
    public void onShare(Recording recording, int position) {
        objType = OBJ_TYPE.RECORDING;
        if (selectedRecordingList == null){
            selectedRecordingList = new ArrayList<>();
        }
        selectedRecordingList.clear();
        selectedRecordingList.add(recording);
        shareDialog.show();
    }

    @Override
    public void onRename(Recording recording, int position) {
        objType = OBJ_TYPE.RECORDING;
        renameRecordingObj = recording;
        renamePos = position;
        et_popup_input.setText(recording.getName());
        renameDialog.show();
    }

    @Override
    public void onDelete(Recording recording, int position) {
        objType = OBJ_TYPE.RECORDING;
        if (selectedRecordingList == null){
            selectedRecordingList = new ArrayList<>();
        }
        selectedRecordingList.clear();
        selectedRecordingList.add(recording);
        deleteConfirmation(selectedRecordingList);
    }

    @Override
    public void onLongPress(Recording recording, int position) {
        objType = OBJ_TYPE.RECORDING;
        renameRecordingObj = recording;
        renamePos = position;
        optionMenu.show();
    }

    @Override
    public void onLongPress(OrganizedRec organizedRec, int position) {
        objType = OBJ_TYPE.ORGANIZED;
        this.organizedRec = organizedRec;
        opos = position;
        optionMenu.show();
    }

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                progressBar.setVisibility(View.GONE);
                String ACTION = intent.getAction();
                ArrayList<Recording> arrList;
                Recording r;
                int pos;

                if (ACTION!=null){
                    switch (ACTION){
                        case ACTION_PULL_RECORDINGS:
                            isFromSearch = false;
                            arrList = (ArrayList<Recording>) intent.getSerializableExtra(DB_PARAM);
                            onRecordingDataReceived(arrList);
                            break;

                        case ACTION_SEARCH_RECORDING:
                            isFromSearch = true;
                            arrList = (ArrayList<Recording>) intent.getSerializableExtra(DB_PARAM);
                            onRecordingDataReceived(arrList);
                            break;

                        case ON_PLAY:
                            recordingObject = (Recording) intent.getSerializableExtra(MUSIC_OBJECT);
                            currentPos = intent.getIntExtra(POSITION_PARAM,-1);
                            play();
                            break;

                        case ON_PAUSE:
                            recordingObject = (Recording) intent.getSerializableExtra(MUSIC_OBJECT);
                            currentPos = intent.getIntExtra(POSITION_PARAM,-1);
                            onRecordPause();
                            break;

                        case ON_RESUME:
                            onResumeRecord();
                            break;

                        case ON_CHECKED:
                            r = (Recording) intent.getSerializableExtra(MUSIC_OBJECT);
                            pos = intent.getIntExtra(POSITION_PARAM,-1);
                            boolean check = intent.getBooleanExtra(ON_CHECKED,true);
                            recordingListAdapter.markChecked(r,check,pos);
                            break;

                        case WIDGET_DELETE:
                            objType = OBJ_TYPE.RECORDING;
                            if (selectedRecordingList == null){
                                selectedRecordingList = new ArrayList<>();
                            }
                            selectedRecordingList.clear();
                            r = (Recording) intent.getSerializableExtra(MUSIC_OBJECT);
                            //pos = intent.getIntExtra(POSITION_PARAM,-1);
                            selectedRecordingList.add(r);
                            deleteConfirmation(selectedRecordingList);
                            break;

                        case WIDGET_RENAME:
                            objType = OBJ_TYPE.RECORDING;
                            renameRecordingObj = (Recording) intent.getSerializableExtra(MUSIC_OBJECT);
                            renamePos = intent.getIntExtra(POSITION_PARAM,-1);
                            et_popup_input.setText(renameRecordingObj.getName());
                            renameDialog.show();
                            break;

                        case WIDGET_SHARE:
                            objType = OBJ_TYPE.RECORDING;
                            if (selectedRecordingList == null){
                                selectedRecordingList = new ArrayList<>();
                            }
                            selectedRecordingList.clear();
                            r = (Recording) intent.getSerializableExtra(MUSIC_OBJECT);
                            selectedRecordingList.add(r);
                            shareDialog.show();
                            break;

                        case ON_LONG_PRESSED:
                            objType = OBJ_TYPE.RECORDING;
                            renameRecordingObj = (Recording) intent.getSerializableExtra(MUSIC_OBJECT);
                            renamePos = intent.getIntExtra(POSITION_PARAM,-1);
                            optionMenu.show();
                            break;
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void rename(final Recording recording, final String value, final int position){
        try{
            progressBar.setVisibility(View.VISIBLE);
            if (recording.isOfProject()){
                mDatabase.child("projects").child(recording.getProjectId()).child("recordings").child(recording.getId()).child("name").setValue(value).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        recording.setName(value);
                        recordingListAdapter.updateRecordingAtPos(recording,position);
                    }
                }).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
            else{
                mDatabase.child("no_project").child("recordings").child(recording.getId()).child("name").setValue(value).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        recording.setName(value);
                        recordingListAdapter.updateRecordingAtPos(recording,position);
                    }
                }).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void rename(final OrganizedRec organizedRec, final String value, final int position){
        try{
            progressBar.setVisibility(View.VISIBLE);
            mDatabase.child("projects").child(organizedRec.getProjectId()).child("project_name").setValue(value).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    for(Recording r : organizedRec.getRecordingList()){
                        r.setProjectName(value);
                    }
                    recordingListAdapter.updateRecordingAtPos(position);
                }
            }).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    progressBar.setVisibility(View.GONE);
                }
            });
        }catch (Exception e){
           e.printStackTrace();
        }
    }

    private void onRecordingDataReceived(ArrayList<Recording> arrList){
        recordingAppModels.clear();

        if (arrList.size()>0){

            List<OrganizedRec> tempOrganized = new ArrayList<>();

            OrganizedRec organizedRec = null;

            String lastProjectId = "";

            for (Recording recording : arrList){
                try{
                    if (recording.isOfProject()){
                        if (!lastProjectId.equals(recording.getProjectId())){
                            lastProjectId = recording.getProjectId();
                            organizedRec = new OrganizedRec();
                            organizedRec.setOfProject(true);
                            organizedRec.getRecordingList().add(recording);
                            organizedRec.setProjectId(recording.getProjectId());
                            tempOrganized.add(organizedRec);
                        }
                        else{
                            organizedRec.setOfProject(true);
                            organizedRec.getRecordingList().add(recording);
                        }
                    }
                    else{
                        organizedRec = new OrganizedRec();
                        organizedRec.setOfProject(false);
                        organizedRec.setRecording(recording);
                        tempOrganized.add(organizedRec);
                    }
                }catch (NullPointerException e){
                    e.printStackTrace();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            TreeMap<String,OrganizedRec> mergeAndSort = new TreeMap<>();

            for (OrganizedRec or : tempOrganized){
                if (or.isOfProject()){
                    mergeAndSort.put(or.getProjectId(),or);
                }else{
                    mergeAndSort.put(or.getRecording().getId(),or);
                }
            }

            List<OrganizedRec> organizedRecs = new ArrayList<>();

            for (Object o : mergeAndSort.entrySet()){
                Map.Entry pair = (Map.Entry) o;
                OrganizedRec or = (OrganizedRec) pair.getValue();
                organizedRecs.add(or);
            }

            Collections.reverse(organizedRecs);

            if (isFromSearch && organizedRecs.size()==0){
                showAlert(getContext(),getString(R.string.please_search_again),getString(R.string.no_search_result_found));
            }

            no_recordings.setVisibility(View.GONE);
            recordingAppModels.addAll(organizedRecs);
            runLayoutAnimation();
        }
        else{
            recordingAppModels.clear();
            runLayoutAnimation();
            no_recordings.setVisibility(View.VISIBLE);

            if (isFromSearch){
                showAlert(getContext(),getString(R.string.please_search_again),getString(R.string.no_search_result_found));
            }
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_PULL_RECORDINGS);
        filter.addAction(ACTION_SEARCH_RECORDING);
        filter.addAction(ON_PLAY);
        filter.addAction(ON_PAUSE);
        filter.addAction(ON_RESUME);
        filter.addAction(ON_CHECKED);

        filter.addAction(WIDGET_DELETE);
        filter.addAction(WIDGET_SHARE);
        filter.addAction(WIDGET_RENAME);
        filter.addAction(ON_LONG_PRESSED);

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(responseReceiver, filter);
    }


    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unregisterBroadcast();
        handler.removeCallbacks(MediaObserver);
        if (mPlayer!=null){
            clearPlayer();
        }
    }

    private void toogleWidget(boolean show){
        if (show){
            share_widget_layout.setVisibility(View.VISIBLE);
            share_widget_layout.setAlpha(0.0f);
            share_widget_layout.animate()
                    .alpha(1.0f)
                    .setListener(null);
            recordingListAdapter.showSelection(true);
        }else{
            share_widget_layout.animate()
            .alpha(0.0f)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                share_widget_layout.setVisibility(View.GONE);
                }
            });
            recordingListAdapter.showSelection(false);
        }
    }

    private void onRecordPause(){
        handler.removeCallbacks(MediaObserver);
        try{
            if (mPlayer!=null && mPlayer.isPlaying()){
                mPlayer.pause();
            }
        }catch (Exception e){
            try{
                mPlayer.pause();
            }catch (Exception ex){
                e.printStackTrace();
            }
        }
    }

    private void onResumeRecord() {
        try{
            if (mPlayer!=null){
                mPlayer.start();
                //if (!recordingObject.isOfProject() && currentPos!=-1){
                    handler.post(MediaObserver);
                //}
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        try{
            progressBar.setVisibility(View.GONE);
            audioDuration = mp.getDuration();
            mp.start();
            handler.post(MediaObserver);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        clearPlayer();
        try{
            recordingListAdapter.markAudioComplete(recordingObject,currentPos);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void play(){
        handler.removeCallbacks(MediaObserver);
        if (mPlayer!=null){
            clearPlayer();
        }

        localFile = new File(localdir, recordingObject.getTid());
        String path = "";

        if (localFile.exists()){
            path = localFile.getAbsolutePath();
        }
        else{
            if (recordingObject.getDownloadURL()!=null){
                path = recordingObject.getDownloadURL();
            }
            else{
                Toast.makeText(context, "url not provided", Toast.LENGTH_SHORT).show();
            }
        }

        if (!path.isEmpty()){
            try {
                progressBar.setVisibility(View.VISIBLE);
                recordingListAdapter.markAudioPlaying(recordingObject,currentPos);
                mPlayer = new MediaPlayer();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setDataSource(path);
                mPlayer.prepareAsync();
                mPlayer.setOnPreparedListener(this);
                mPlayer.setOnCompletionListener(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Runnable MediaObserver = new Runnable() {
        static final long PROGRESS_UPDATE = 200;
        private int currenttime;
        private double dd;
        int percent;

        @Override
        public void run() {
            try {
                currenttime = mPlayer.getCurrentPosition();
                dd = audioDuration / 100;
                percent = (int) (currenttime / dd);
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recordingListAdapter.updateProgress(recordingObject,currentPos,percent);
                    }
                });
                handler.postDelayed(this,PROGRESS_UPDATE);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    private void clearPlayer(){
        handler.removeCallbacks(MediaObserver);
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
