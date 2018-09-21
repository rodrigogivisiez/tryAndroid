package com.tullyapp.tully.ViewPagerFragments;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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
import com.tullyapp.tully.Adapters.HomeFilesSwipeViewAdapter;
import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.FirebaseDataModels.HomeDb;
import com.tullyapp.tully.Interface.AudioFileEvents;
import com.tullyapp.tully.Interface.AuthToken;
import com.tullyapp.tully.PlayActivity;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Receiver.EventsReceiver;
import com.tullyapp.tully.Services.DeleteProjects;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.Constants;
import com.tullyapp.tully.Utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

import static com.tullyapp.tully.Fragments.HomeFragment.PARAM_HOME_DB;
import static com.tullyapp.tully.Fragments.HomeFragment.PARAM_IS_FROM_SEARCH;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_HOME;
import static com.tullyapp.tully.Utils.ActionEventConstant.AUDIO_FILE_UPLOADED;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Utils.showAlert;
import static com.tullyapp.tully.Utils.ViewUtils.shareAllowDownloadPopup;
import static com.tullyapp.tully.ViewPagerFragments.HomeAllFragment.REQUEST_COPYTULLY;

public class FragmentHomeFiles extends Fragment implements View.OnClickListener, HomeFilesSwipeViewAdapter.AdapterInterface, HomeFilesSwipeViewAdapter.OnWidgetAction, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = FragmentHomeFiles.class.getSimpleName();
    private ProgressBar progressBar;
    private HomeFilesSwipeViewAdapter homeFilesAdapter;

    private ArrayList<AudioFile> copyToTullies = new ArrayList<>();
    protected SwipeRefreshLayout swipeRefreshLayout;

    private Context context;
    private HomeDb homeDb;
    private ArrayList<AudioFile> selectedCopyToTullies;
    AlertDialog.Builder optionMenu;
    private static final CharSequence selectionMenuItems[] = new CharSequence[] {"Rename", "Share", "Delete"};

    private AudioFile ct_obj;
    private int rename_index;

    private Dialog renameDialog;
    private EditText et_popup_input;
    private TextView tv_pop_title;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerView;
    private FragmentActivity mActivity;
    private Dialog shareDialog;
    private AppCompatButton btn_allow, btn_not_allow, btn_cp_url;
    private EventsReceiver eventsReceiver;
    private VideoView videoView;
    private MixpanelAPI mixpanel;
    private Dialog dialog;
    private ImageView btn_close;
    private boolean isFromSearch = false;

    private ResponseReceiver responseReceiver;

    private static final String fragmentHomeFiles = "com.tullyapp.tully.ViewPagerFragments.FragmentHomeFiles";
    private AuthToken authToken;
    private AudioFile selectedAudioFile;
    private boolean allowDownload;
    private SwitchCompat switch_expire_after_once, switch_expire_after_one_hour, switch_expire_never;
    private boolean allowSwitch = true;

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
            Log.e(TAG,intent.getAction());
            switch (intent.getAction()){
                case fragmentHomeFiles:
                    homeDb = (HomeDb) intent.getSerializableExtra(PARAM_HOME_DB);
                    isFromSearch = intent.getBooleanExtra(PARAM_IS_FROM_SEARCH,false);
                    updateAdapter();
                    break;
            }
        }
    }

    public FragmentHomeFiles() {
        // Required empty public constructor
    }

    public static FragmentHomeFiles newInstance() {
        Bundle args = new Bundle();
        FragmentHomeFiles fragment = new FragmentHomeFiles();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mixpanel = MixpanelAPI.getInstance(getContext(), YOUR_PROJECT_TOKEN);
        responseReceiver = new ResponseReceiver();
        registerReceivers();
        mAuth = FirebaseAuth.getInstance();
        authToken = new AuthToken() {
            @Override
            public void onToken(String token, String callback) {
                switch (callback){
                    case "shareCopyToTullyFile":
                        shareCopyToTullyFile(token);
                        break;
                }
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();
        mActivity = (FragmentActivity) context;
        eventsReceiver = new EventsReceiver(getContext(), AUDIO_FILE_UPLOADED);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fragment_home_files, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        recyclerView = view.findViewById(R.id.recycle_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        renameDialog = new Dialog(context, R.style.MyDialogTheme);
        renameDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        renameDialog.setContentView(R.layout.input_popup);
        renameDialog.setCancelable(false);
        renameDialog.setCanceledOnTouchOutside(false);

        et_popup_input = renameDialog.findViewById(R.id.et_popup_input);
        tv_pop_title = renameDialog.findViewById(R.id.tv_pop_title);

        AppCompatButton btn_popup_rename = renameDialog.findViewById(R.id.btn_popup_create);
        AppCompatButton btn_popup_cancel = renameDialog.findViewById(R.id.btn_popup_cancel);

        btn_popup_rename.setText("RENAME");

        btn_popup_rename.setOnClickListener(this);
        btn_popup_cancel.setOnClickListener(this);

        homeFilesAdapter = new HomeFilesSwipeViewAdapter(this.context,copyToTullies,false);
        recyclerView.setAdapter(homeFilesAdapter);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fade_scale));

        homeFilesAdapter.setAdapterInterface(this);
        homeFilesAdapter.setOnWidgetAction(this);


        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
            Toast.makeText(context, "Loading ! Just few seconds", Toast.LENGTH_SHORT).show();
                FirebaseDatabaseOperations.startAction(context, ACTION_PULL_HOME);
            }
        });

        return view;
    }

    private void runLayoutAnimation(){
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dialog = new Dialog(getContext(), R.style.MyDialogTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.video_popup);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        videoView = dialog.findViewById(R.id.videoView);
        videoView.setZOrderOnTop(true);

        shareDialog = shareAllowDownloadPopup(getContext());

        btn_allow = shareDialog.findViewById(R.id.btn_allow);
        btn_not_allow = shareDialog.findViewById(R.id.btn_not_allow);
        btn_close = shareDialog.findViewById(R.id.btn_close);
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
        btn_close.setOnClickListener(this);

        optionMenu = new AlertDialog.Builder(context);
        optionMenu.setItems(selectionMenuItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case 0:
                    tv_pop_title.setText(R.string.rename_file);
                    et_popup_input.setText(ct_obj.getTitle());
                    et_popup_input.requestFocus();
                    renameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    renameDialog.show();
                    break;

                case 1:
                    if (!ct_obj.getId().equals(getString(R.string.free_beat_id))){
                        shareDialog.show();
                    }
                    else{
                        shareCopyToTully(ct_obj,true);
                    }
                    break;

                case 2:
                    selectedCopyToTullies = new ArrayList<>();
                    selectedCopyToTullies.add(ct_obj);
                    deleteConfirmation(selectedCopyToTullies);
                    break;
            }
            }
        });

        eventsReceiver.setAudioFileEvents(new AudioFileEvents() {
            @Override
            public void audioFileUploaded(AudioFile audioFile) {
                homeFilesAdapter.updateFile(audioFile);
            }

            @Override
            public void audioFileUploadFailed(AudioFile audioFile) {

            }
        });

        updateAdapter();
    }

    public void updateAdapter() {
        swipeRefreshLayout.setRefreshing(false);
        if (homeDb !=null && homeFilesAdapter!=null){
            copyToTullies.clear();

            String id;
            AudioFile freeBeat = null;

            boolean no_file = true;

            if (homeDb.getSortedCopytotully()!=null){
                Iterator iterator = homeDb.getSortedCopytotully().entrySet().iterator();
                AudioFile audioFile;

                while (iterator.hasNext()){
                    Map.Entry pair = (Map.Entry)iterator.next();
                    id = pair.getKey().toString();
                    audioFile = (AudioFile) pair.getValue();
                    audioFile.setId(id);
                    if (id.equals(getString(R.string.free_beat_id))){
                        freeBeat = audioFile;
                        continue;
                    }
                    copyToTullies.add(audioFile);
                }

                if (copyToTullies.size()>0) no_file = false;

                if (freeBeat!=null) copyToTullies.add(freeBeat);

                AudioFile video = new AudioFile();
                video.setTitle("Getting Started");
                video.setFilename("Video");
                video.setSize(0);
                video.setId("video");
                video.setVideo(true);
                copyToTullies.add(video);
                Collections.reverse(copyToTullies);

                if (isFromSearch && no_file){
                    showAlert(getContext(),getString(R.string.please_search_again),getString(R.string.no_search_result_found));
                }
            }
            else{
                if (isFromSearch){
                    showAlert(getContext(),getString(R.string.please_search_again),getString(R.string.no_search_result_found));
                }
            }

            runLayoutAnimation();

            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_popup_cancel:
                renameDialog.dismiss();
                break;

            case R.id.btn_popup_create:
                String val = et_popup_input.getText().toString().trim();
                if (!val.isEmpty()){
                    progressBar.setVisibility(View.VISIBLE);
                    rename(ct_obj,val,rename_index);
                    renameDialog.dismiss();
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
                shareCopyToTully(ct_obj,true);
                break;

            case R.id.btn_close:
                shareDialog.dismiss();
                break;
        }
    }

    private void rename(final AudioFile audioFile, final String value, final int position){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("copytotully").child(audioFile.getId()).child("title").setValue(value).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                audioFile.setTitle(value);
                homeFilesAdapter.updateFileAtPos(audioFile, position);
                progressBar.setVisibility(View.GONE);
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void shareCopyToTully(AudioFile audioFile, boolean b) {
        if (audioFile.getDownloadURL()!=null && !audioFile.getDownloadURL().isEmpty()){
            progressBar.setVisibility(View.VISIBLE);
            selectedAudioFile = audioFile;
            allowDownload = b;
            Utils.getAuthToken(getContext(),mAuth,authToken,"shareCopyToTullyFile");
        }
        else{
            progressBar.setVisibility(View.GONE);
            Toast.makeText(context, "Audio still uploading or broken", Toast.LENGTH_SHORT).show();
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

    private void shareCopyToTullyFile(String token){
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
        client.addHeader(Constants.Authorization,token);
        RequestParams params = new RequestParams();
        params.put("userid", mAuth.getCurrentUser().getUid());
        params.put("ids", selectedAudioFile.getId());
        params.put("config",configArr.toString());
        client.post(APIs.SHARE_AUDIO, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                progressBar.setVisibility(View.GONE);
                mixpanel.track("Sharing for Files");
                String responseString = new String(responseBody);
                Log.e("RESPONSE",responseString);
                try {
                    JSONObject response = new JSONObject(responseString);
                    shareLink(response.getJSONObject("data").getString("link"),"Share Audio");
                    Toast.makeText(getContext(), response.getString("msg"), Toast.LENGTH_SHORT).show();
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                try{
                    progressBar.setVisibility(View.GONE);
                    error.printStackTrace();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void deleteConfirmation(final ArrayList<AudioFile> copyToTullies){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.sure_to_delete_selected_files)
        .setMessage(R.string.sure_delete_message_files)
        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                deleteSelection(copyToTullies);
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

    private void deleteSelection(ArrayList<AudioFile> copyToTullies){
        progressBar.setVisibility(View.VISIBLE);
        if (copyToTullies.size()>0){
            Toast.makeText(context, "Deleting selected files, might take few seconds", Toast.LENGTH_SHORT).show();
            DeleteProjects.startActionDeleteCopyToTully(getContext(),copyToTullies, ACTION_PULL_HOME);
        }
    }

    private void shareLink(String link, String title){
        mixpanel.track(title);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, link);
        startActivity(Intent.createChooser(share, title));
    }

    @Override
    public void onCopyToTully(AudioFile audioFile, int position) {
        Intent intent = new Intent(context, PlayActivity.class);
        intent.putExtra("COPYTOTULLY", audioFile);

        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
        TextView ll = holder.itemView.findViewById(R.id.grid_title);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(mActivity, ll, ViewCompat.getTransitionName(ll));

        mActivity.startActivityFromFragment(this,intent,REQUEST_COPYTULLY, options.toBundle());
    }

    @Override
    public void onVideoPlay(AudioFile audioFile, int position) {
        mixpanel.track(getString(R.string.getting_started));
        String path = "android.resource://" + getContext().getPackageName() + "/" + R.raw.android;
        videoView.setVideoURI(Uri.parse(path));
        dialog.show();
        videoView.requestFocus();
        videoView.start();
    }

    @Override
    public void onLongPress(AudioFile audioFile, int position) {
        ct_obj = audioFile;
        rename_index = position;
        optionMenu.show();
    }

    @Override
    public void onShare(AudioFile audioFile, int position) {
        ct_obj = audioFile;
        rename_index = position;
        if (!ct_obj.getId().equals(getString(R.string.free_beat_id))){
            shareDialog.show();
        }
        else{
            shareCopyToTully(audioFile,true);
        }
    }

    @Override
    public void onRename(AudioFile audioFile, int position) {
        ct_obj = audioFile;
        rename_index = position;
        tv_pop_title.setText(R.string.rename_file);
        et_popup_input.setText(ct_obj.getTitle());
        et_popup_input.requestFocus();
        renameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        renameDialog.show();
    }

    @Override
    public void onDelete(AudioFile audioFile, int position) {
        ct_obj = audioFile;
        rename_index = position;
        selectedCopyToTullies = new ArrayList<>();
        selectedCopyToTullies.add(ct_obj);
        deleteConfirmation(selectedCopyToTullies);
    }

    @Override
    public void onDetach() {
        unregisterBroadcast();
        super.onDetach();
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(FragmentHomeFiles.class.getName());
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(responseReceiver, filter);
    }
}
