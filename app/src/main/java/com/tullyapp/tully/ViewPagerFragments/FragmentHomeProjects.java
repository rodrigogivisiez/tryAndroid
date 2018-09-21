package com.tullyapp.tully.ViewPagerFragments;

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
import com.tullyapp.tully.Adapters.HomeProjectsSwipeViewAdapter;
import com.tullyapp.tully.FirebaseDataModels.HomeDb;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.Interface.AuthToken;
import com.tullyapp.tully.Interface.ProjectRecordingEvents;
import com.tullyapp.tully.ProjectActivity;
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
import java.util.Map;

import cz.msebera.android.httpclient.Header;

import static com.tullyapp.tully.Fragments.HomeFragment.PARAM_HOME_DB;
import static com.tullyapp.tully.Fragments.HomeFragment.PARAM_IS_FROM_SEARCH;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_HOME;
import static com.tullyapp.tully.Utils.ActionEventConstant.PROJECT_RECORDING_UPLOADED;
import static com.tullyapp.tully.Utils.Constants.PROJECT_PARAM;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Utils.showAlert;
import static com.tullyapp.tully.Utils.ViewUtils.shareAllowDownloadPopup;
import static com.tullyapp.tully.ViewPagerFragments.HomeAllFragment.REQUEST_COPYTULLY;

public class FragmentHomeProjects extends Fragment implements View.OnClickListener, HomeProjectsSwipeViewAdapter.AdapterInterface, HomeProjectsSwipeViewAdapter.OnWidgetAction, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = FragmentHomeProjects.class.getSimpleName();
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;

    private Dialog renameDialog;
    private EditText et_popup_input;
    private TextView tv_pop_title;
    private AppCompatButton btn_popup_cancel;
    private AppCompatButton btn_popup_rename, btn_cp_url;

    AlertDialog.Builder optionMenu;
    private static final CharSequence selectionMenuItems[] = new CharSequence[] {"Rename", "Share", "Delete"};

    private Project projectObj;
    private Dialog shareDialog;

    private RecyclerView recyclerView;
    private HomeProjectsSwipeViewAdapter homeProjectsAdapter;
    private ArrayList<Project> projectList = new ArrayList<>();

    private Context context;
    private HomeDb homeDb;
    private ArrayList<Project> selectedProjectList;

    protected SwipeRefreshLayout swipeRefreshLayout;
    private int resume_position;
    private FragmentActivity mActivity;
    private AppCompatButton btn_allow, btn_not_allow;

    private EventsReceiver eventsReceiver;
    private MixpanelAPI mixpanel;
    private ImageView btn_close;
    private boolean isFromSearch = false;

    private ResponseReceiver responseReceiver;

    private static final String fragmentHomeProjects = "com.tullyapp.tully.ViewPagerFragments.FragmentHomeProjects";
    private Project selectedProject;
    private boolean allowDownload;
    private AuthToken authToken;
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
            switch (intent.getAction()){
                case fragmentHomeProjects:
                    homeDb = (HomeDb) intent.getSerializableExtra(PARAM_HOME_DB);
                    isFromSearch = intent.getBooleanExtra(PARAM_IS_FROM_SEARCH,false);
                    updateAdapter();
                    break;
            }
        }
    }

    public FragmentHomeProjects() {
        // Required empty public constructor
    }

    public static FragmentHomeProjects newInstance() {
        FragmentHomeProjects fragment = new FragmentHomeProjects();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mixpanel = MixpanelAPI.getInstance(getContext(), YOUR_PROJECT_TOKEN);
        responseReceiver = new ResponseReceiver();
        registerReceivers();

        authToken = new AuthToken() {
            @Override
            public void onToken(String token, String callback) {
                switch (callback){
                    case "shareProjectLink":
                        shareProjectLink(token);
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
        selectedProjectList = new ArrayList<>();
        eventsReceiver = new EventsReceiver(getContext(), PROJECT_RECORDING_UPLOADED);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fragment_home_projects, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        recyclerView = view.findViewById(R.id.recycle_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);

        renameDialog = new Dialog(context, R.style.MyDialogTheme);
        renameDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        renameDialog.setContentView(R.layout.input_popup);
        renameDialog.setCancelable(false);
        renameDialog.setCanceledOnTouchOutside(false);

        et_popup_input = renameDialog.findViewById(R.id.et_popup_input);
        tv_pop_title = renameDialog.findViewById(R.id.tv_pop_title);

        btn_popup_rename = renameDialog.findViewById(R.id.btn_popup_create);
        btn_popup_cancel = renameDialog.findViewById(R.id.btn_popup_cancel);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                FirebaseDatabaseOperations.startAction(context, ACTION_PULL_HOME);
            }
        });

        homeProjectsAdapter = new HomeProjectsSwipeViewAdapter(getContext(), projectList);
        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fade_scale));
        recyclerView.setAdapter(homeProjectsAdapter);
        mAuth = FirebaseAuth.getInstance();
        homeProjectsAdapter.setAdapterInterface(this);
        homeProjectsAdapter.setOnWidgetAction(this);

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

        btn_popup_rename.setText("RENAME");

        btn_popup_rename.setOnClickListener(this);
        btn_popup_cancel.setOnClickListener(this);

        optionMenu = new AlertDialog.Builder(context);
        //optionMenu.setTitle("Select");
        optionMenu.setItems(selectionMenuItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case 0:
                    tv_pop_title.setText(R.string.rename_your_project);
                    et_popup_input.setText(projectObj.getProject_name());
                    et_popup_input.requestFocus();
                    renameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    renameDialog.show();
                    break;

                case 1:
                    shareDialog.show();
                    break;

                case 2:
                    selectedProjectList.clear();
                    selectedProjectList.add(projectObj);
                    deleteConfirmation(selectedProjectList);
                    break;
            }
            }
        });

        eventsReceiver.setProjectRecordingEvents(new ProjectRecordingEvents() {
            @Override
            public void projectRecordingUploaded(Recording recording) {
                homeProjectsAdapter.updateProjectRecording(recording);
            }

            @Override
            public void projectRecordingUploadFailed(Recording recording) {

            }
        });

        updateAdapter();
    }

    private void runLayoutAnimation(){
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }


    public void updateAdapter(){
        swipeRefreshLayout.setRefreshing(false);
        if (homeDb !=null && homeProjectsAdapter!=null){
            projectList.clear();

            if (homeDb.getSortedProjects()!=null){
                for (Object o : homeDb.getSortedProjects().entrySet()) {
                    Map.Entry pair = (Map.Entry) o;
                    Project pro =(Project) pair.getValue();
                    pro.setId(pair.getKey().toString());
                    projectList.add(pro);
                }

                if (isFromSearch && projectList.size()==0){
                    showAlert(getContext(),getString(R.string.please_search_again),getString(R.string.no_search_result_found));
                }

                Collections.reverse(projectList);
            }

            runLayoutAnimation();
        }
        else{
            if (isFromSearch){
                showAlert(getContext(),getString(R.string.please_search_again),getString(R.string.no_search_result_found));
            }
        }
        progressBar.setVisibility(View.GONE);
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
                    rename(projectObj,val,resume_position);
                    renameDialog.dismiss();
                    progressBar.setVisibility(View.VISIBLE);
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
                shareProject(projectObj,allowSwitch);
                break;

            case R.id.btn_close:
                shareDialog.dismiss();
                break;
        }
    }

    private void rename(final Project project, final String value, final int position){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("projects").child(project.getId()).child("project_name").setValue(value).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                project.setProject_name(value);
                homeProjectsAdapter.updateProjectAtPos(project, position);
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void shareProject(final Project project, boolean b){
        progressBar.setVisibility(View.VISIBLE);
        selectedProject = project;
        allowDownload = b;
        Utils.getAuthToken(getContext(),mAuth,authToken,"shareProjectLink");
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

    private void shareProjectLink(String token){
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
        params.put("projectid", selectedProject.getId());
        params.put("config",configArr.toString());

        client.post(APIs.SHARE_PROJECT, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                progressBar.setVisibility(View.GONE);
                mixpanel.track("Sharing for Projects");
                String responseString = new String(responseBody);
                try {
                    JSONObject response = new JSONObject(responseString);
                    sendSharedProjectLink(selectedProject.getProject_name(),response.getJSONObject("data").getString("link"));
                    Toast.makeText(context, response.getString("msg"), Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
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

    private void sendSharedProjectLink(String projectName, String link){
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, link);
        startActivity(Intent.createChooser(share, "Share Project"));
    }

    private void deleteConfirmation(final ArrayList<Project> projects){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.sure_delete_message_title)
        .setMessage(R.string.sure_delete_message)
        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                deleteSelection(projects);
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

    private void deleteSelection(ArrayList<Project> projects){
        progressBar.setVisibility(View.VISIBLE);
        if (projects.size()>0){
            Toast.makeText(context, "Deleting selected projects, might take few seconds", Toast.LENGTH_SHORT).show();
            DeleteProjects.startActionDeleteProjects(getContext(), projects);
        }
    }

    @Override
    public void onProjectTap(Project project, int position) {
        Intent intent = new Intent(context, ProjectActivity.class);
        intent.putExtra(PROJECT_PARAM,project);

        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
        TextView ll = holder.itemView.findViewById(R.id.grid_title);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(mActivity, ll, ViewCompat.getTransitionName(ll));

        mActivity.startActivityFromFragment(this,intent,REQUEST_COPYTULLY, options.toBundle());
    }

    @Override
    public void onLongPress(Project project, int i) {
        projectObj = project;
        resume_position = i;
        optionMenu.show();
    }

    @Override
    public void onShare(Project project, int position) {
        projectObj = project;
        shareDialog.show();
    }

    @Override
    public void onRename(Project project, int position) {
        projectObj = project;
        resume_position = position;

        tv_pop_title.setText(R.string.rename_your_project);
        et_popup_input.setText(projectObj.getProject_name());
        et_popup_input.requestFocus();
        renameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        renameDialog.show();
    }

    @Override
    public void onDelete(Project project, int position) {
        projectObj = project;
        resume_position = position;
        selectedProjectList.clear();
        selectedProjectList.add(projectObj);
        deleteConfirmation(selectedProjectList);
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
        filter.addAction(FragmentHomeProjects.class.getName());
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(responseReceiver, filter);
    }
}
