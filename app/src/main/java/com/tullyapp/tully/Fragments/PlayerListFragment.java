package com.tullyapp.tully.Fragments;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.tullyapp.tully.Adapters.PlayerListAdapter;
import com.tullyapp.tully.Dialogs.TutorialScreen;
import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.Interface.AuthToken;
import com.tullyapp.tully.PlayActivity;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Services.DeleteProjects;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.Configuration;
import com.tullyapp.tully.Utils.Constants;
import com.tullyapp.tully.Utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyStore;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

import static com.tullyapp.tully.Services.DeleteProjects.ACTION_AUDIO_DELETED;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_COPYTOTULLY;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.DB_PARAM;
import static com.tullyapp.tully.Utils.Constants.TUTORIAL_SCREENS;
import static com.tullyapp.tully.Utils.Constants.TUTS_PLAY;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Utils.showAlert;
import static com.tullyapp.tully.Utils.ViewUtils.shareAllowDownloadPopup;
import static com.tullyapp.tully.ViewPagerFragments.HomeAllFragment.REQUEST_PROJECT;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlayerListFragment extends BaseFragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, PlayerListAdapter.AdapterInterface, PlayerListAdapter.OnWidgetAction {

    private static final String TAG = PlayerListFragment.class.getSimpleName();
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private LinearLayout ll_no_data;

    protected SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private AppCompatCheckBox share_widget_checkbox_button;
    private LinearLayout share_widget_layout;
    private PlayerListAdapter playerListAdapter;
    private ArrayList<AudioFile> selectedPlayerListList = new ArrayList<>();
    private ResponseReceiver responseReceiver;
    private ArrayList<AudioFile> searchCopyToTullies = new ArrayList<>();

    private Context context;
    private FragmentActivity mActivity;
    private RecyclerView recycle_view;

    private Dialog shareDialog;
    private AppCompatButton btn_allow, btn_not_allow;

    private MixpanelAPI mixpanel;

    private Dialog renameDialog;
    private EditText et_popup_input;
    private TextView tv_pop_title, tv_pop_desc;
    private AppCompatButton btn_popup_cancel, btn_popup_rename, btn_cp_url;

    private AudioFile renameAudio;
    private int renamePos;
    private ImageView btn_close;
    private boolean isFromSearch;
    private AlertDialog.Builder optionMenu;
    private static final CharSequence selectionMenuItems[] = new CharSequence[] {"Rename", "Share", "Delete"};
    private SwitchCompat switch_expire_after_once, switch_expire_after_one_hour, switch_expire_never;
    private AuthToken authToken;
    private boolean allowDownload;
    private boolean allowSwitch = true;


    public PlayerListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        mActivity = (FragmentActivity) context;

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


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
    }


    @Override
    public void onStart() {
        super.onStart();
        mixpanel = MixpanelAPI.getInstance(getContext(), YOUR_PROJECT_TOKEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_player_list, container, false);

        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
        progressBar = view.findViewById(R.id.progressBar);
        recycle_view = view.findViewById(R.id.recycle_view);
        LinearLayout widget_share = view.findViewById(R.id.widget_share);
        LinearLayout widget_delete = view.findViewById(R.id.widget_delete);
        LinearLayout widget_check_all = view.findViewById(R.id.widget_check_all);
        share_widget_checkbox_button = view.findViewById(R.id.share_widget_checkbox_button);
        share_widget_layout = view.findViewById(R.id.share_widget);

        ll_no_data = view.findViewById(R.id.ll_no_data);

        widget_share.setOnClickListener(this);
        widget_delete.setOnClickListener(this);
        widget_check_all.setOnClickListener(this);
        share_widget_checkbox_button.setOnClickListener(this);
        share_widget_checkbox_button.setOnCheckedChangeListener(this);

        playerListAdapter = new PlayerListAdapter(context);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_from_bottom);
        recycle_view.setLayoutAnimation(animation);
        recycle_view.setLayoutManager(linearLayoutManager);
        recycle_view.setAdapter(playerListAdapter);

        responseReceiver = new ResponseReceiver();
        registerReceivers();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchCopyToTullies();
            }
        });

        fetchCopyToTullies();

        return view;
    }

    private void runLayoutAnimation() {
        final LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_from_bottom);
        recycle_view.setLayoutAnimation(controller);
        recycle_view.getAdapter().notifyDataSetChanged();
        recycle_view.scheduleLayoutAnimation();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        playerListAdapter.setAdapterListener(this);
        playerListAdapter.setOnWidgetAction(this);

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
                    et_popup_input.setText(renameAudio.getTitle());
                    et_popup_input.requestFocus();
                    renameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    renameDialog.show();
                    break;

                case 1:
                    if (selectedPlayerListList==null){
                        selectedPlayerListList = new ArrayList<>();
                    }
                    selectedPlayerListList.clear();
                    selectedPlayerListList.add(renameAudio);
                    if (!renameAudio.getId().equals(getString(R.string.free_beat_id))){
                        shareDialog.show();
                    }
                    else{
                        shareAudioFile(selectedPlayerListList,true);
                    }
                    break;

                case 2:
                    if (selectedPlayerListList==null){
                        selectedPlayerListList = new ArrayList<>();
                    }
                    selectedPlayerListList.clear();
                    selectedPlayerListList.add(renameAudio);
                    deleteConfirmation(selectedPlayerListList);
                    break;
            }
            }
        });

        if (!Configuration.play_tuts) showTutorailDialog(TUTS_PLAY);
    }

    private void showTutorailDialog(String tut) {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        TutorialScreen tutorialScreen = TutorialScreen.newInstance(tut);
        tutorialScreen.show(ft,TutorialScreen.class.getSimpleName());
        tutorialScreen.setOnTutorialClosed(new TutorialScreen.OnTutorialClosed() {
            @Override
            public void onTutsClosed(String tut) {
                mDatabase.child(TUTORIAL_SCREENS).child(tut).setValue(true);
                Configuration.play_tuts = true;
            }
        });
    }

    private void fetchCopyToTullies(){
        progressBar.setVisibility(View.VISIBLE);
        FirebaseDatabaseOperations.startActionCopyToTully(context);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.widget_share:
                selectedPlayerListList = playerListAdapter.getSelectedItems();
                if (selectedPlayerListList.size()==0){
                    Toast.makeText(context, "No Files selected", Toast.LENGTH_SHORT).show();
                }
                else{
                    shareDialog.show();
                }
                toogleWidget(false);
                break;

            case R.id.widget_check_all:
                share_widget_checkbox_button.setChecked(!share_widget_checkbox_button.isChecked());
                break;

            case R.id.widget_delete:
                selectedPlayerListList = playerListAdapter.getSelectedItems();
                if (selectedPlayerListList.size()==0){
                    Toast.makeText(context, "No recording selected", Toast.LENGTH_SHORT).show();
                }else{
                    deleteConfirmation(selectedPlayerListList);
                }
                toogleWidget(false);
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
                shareAudioFile(selectedPlayerListList,allowSwitch);
                break;

            case R.id.btn_popup_create:
                String val = et_popup_input.getText().toString().trim();
                if (!val.isEmpty()){
                    rename(renameAudio,val,renamePos);
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

    private void rename(final AudioFile audioFile, final String value, final int position){
        try{
            progressBar.setVisibility(View.VISIBLE);
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
            mDatabase.child("copytotully").child(audioFile.getId()).child("title").setValue(value).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                audioFile.setTitle(value);
                playerListAdapter.updateAtPos(position);
                }
            }).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void shareAudioFile(ArrayList<AudioFile> audioFiles, boolean b){
        progressBar.setVisibility(View.VISIBLE);
        selectedPlayerListList = audioFiles;
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
        StringBuilder stringBuilder = new StringBuilder();

        for (AudioFile af : selectedPlayerListList){
            if (af.getId()!=null && !af.getId().isEmpty()){
                stringBuilder.append(af.getId());
                stringBuilder.append(",");
            }
        }

        if (stringBuilder.length()>0){

            JSONArray configArr = new JSONArray();
            JSONObject config = new JSONObject();
            try {
                config.put("allow_download",allowDownload);
                config.put("expiry",getExpiryConfig());
                configArr.put(config);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String ids = stringBuilder.substring(0, stringBuilder.length() - 1);
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
            params.put("ids", ids);
            params.put("config",configArr.toString());
            client.post(APIs.SHARE_AUDIO, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    String responseString = new String(responseBody);
                    Log.e("RESPONSE",responseString);
                    try {
                        progressBar.setVisibility(View.GONE);
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
                    progressBar.setVisibility(View.GONE);
                    error.printStackTrace();
                }
            });
        }
        else{
            progressBar.setVisibility(View.GONE);
        }
    }

    private void shareLink(String link, String title){
        mixpanel.track(title);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, link);
        startActivity(Intent.createChooser(share, title));
    }

    private void deleteConfirmation(final ArrayList<AudioFile> selectedCopyToTullies) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.sure_delete_message_title)
            .setMessage(R.string.sure_delete_message_files)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    deleteSelection(selectedCopyToTullies);
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

    private void deleteSelection(ArrayList<AudioFile> selectedCopyToTullies){
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(context, "Deleting selected files", Toast.LENGTH_SHORT).show();
        DeleteProjects.startActionDeleteCopyToTully(context,selectedCopyToTullies, ACTION_AUDIO_DELETED);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.share_widget_checkbox_button:
                playerListAdapter.setCheckedAll(share_widget_checkbox_button.isChecked());
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

    private void toogleWidget(boolean show){
        if (show){
            share_widget_layout.setVisibility(View.VISIBLE);
            share_widget_layout.setAlpha(0.0f);
            share_widget_layout.animate()
                    .alpha(1.0f)
                    .setListener(null);
            playerListAdapter.showSelection(true);
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
            playerListAdapter.showSelection(false);
        }
    }

    @Override
    public void onSearchKey(String searchKey) {
        progressBar.setVisibility(View.VISIBLE);
        searchCopyToTullies.clear();
        mDatabase.child("copytotully").orderByChild("title").startAt(searchKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot node : dataSnapshot.getChildren()){
                        AudioFile audioFile = node.getValue(AudioFile.class);
                        audioFile.setId(node.getKey());
                        searchCopyToTullies.add(audioFile);
                    }
                }
                progressBar.setVisibility(View.GONE);
                isFromSearch = true;
                updateData(searchCopyToTullies);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onSearchCancelled() {
        fetchCopyToTullies();
    }

    @Override
    public void fabButtonClicked() {}

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
    public void onFileTap(AudioFile audioFile, int position) {
        Intent intent = new Intent(context, PlayActivity.class);
        intent.putExtra("COPYTOTULLY", audioFile);

        RecyclerView.ViewHolder holder = recycle_view.findViewHolderForAdapterPosition(position);
        ImageView ll = holder.itemView.findViewById(R.id.player_icon);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(mActivity, ll, ViewCompat.getTransitionName(ll));

        mActivity.startActivityFromFragment(this,intent, REQUEST_PROJECT, options.toBundle());
    }

    @Override
    public void onLongPress(AudioFile audioFile, int position) {
        renamePos = position;
        renameAudio = audioFile;
        optionMenu.show();
    }

    @Override
    public void onShare(AudioFile audioFile, int position) {
        if (selectedPlayerListList==null){
            selectedPlayerListList = new ArrayList<>();
        }
        selectedPlayerListList.clear();
        selectedPlayerListList.add(audioFile);

        if (!audioFile.getId().equals(getString(R.string.free_beat_id))){
            shareDialog.show();
        }
        else{
            shareAudioFile(selectedPlayerListList,true);
        }
    }

    @Override
    public void onRename(AudioFile audioFile, int position) {
        renameAudio = audioFile;
        renamePos = position;
        et_popup_input.setText(audioFile.getTitle());
        renameDialog.show();
    }

    @Override
    public void onDelete(AudioFile audioFile, int position) {
        if (selectedPlayerListList==null){
            selectedPlayerListList = new ArrayList<>();
        }
        selectedPlayerListList.clear();
        selectedPlayerListList.add(audioFile);
        deleteConfirmation(selectedPlayerListList);
    }

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            progressBar.setVisibility(View.GONE);
            switch (intent.getAction()){
                case ACTION_PULL_COPYTOTULLY:
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    try{
                        ArrayList<AudioFile> receivedAudioFiles = (ArrayList<AudioFile>) intent.getSerializableExtra(DB_PARAM);
                        if (receivedAudioFiles !=null){
                            isFromSearch = false;
                            updateData(receivedAudioFiles);
                        }
                        else{
                            playerListAdapter.clear();
                            ll_no_data.setVisibility(View.VISIBLE);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    break;

                case ACTION_AUDIO_DELETED:
                    fetchCopyToTullies();
                    break;
            }
        }
    }

    private void updateData(ArrayList<AudioFile> receivedAudioFiles){
        playerListAdapter.clear();
        AudioFile freeBeat = null;
        for (AudioFile a : receivedAudioFiles){
            if (a.getId().equals(getString(R.string.free_beat_id))){
                freeBeat = a;
                continue;
            }
            playerListAdapter.add(a);
        }

        if (isFromSearch && receivedAudioFiles.size()==0){
            showAlert(getContext(),getString(R.string.please_search_again),getString(R.string.no_search_result_found));
        }

        if (freeBeat!=null) playerListAdapter.add(freeBeat);

        playerListAdapter.reverseList();

        runLayoutAnimation();
    }

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_PULL_COPYTOTULLY);
        filter.addAction(ACTION_AUDIO_DELETED);
        LocalBroadcastManager.getInstance(context).registerReceiver(responseReceiver, filter);
    }


    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(context).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unregisterBroadcast();
    }
}
