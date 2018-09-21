package com.tullyapp.tully;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import com.tullyapp.tully.Adapters.MastersSwipeAdapter;
import com.tullyapp.tully.FirebaseDataModels.Masters;
import com.tullyapp.tully.Interface.AuthToken;
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
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import cz.msebera.android.httpclient.Header;

import static com.tullyapp.tully.Fragments.MasterDetailsFragment.PARAM_MASTER_FILE;
import static com.tullyapp.tully.MasterPlayActivity.PARAM_MASTER;
import static com.tullyapp.tully.Services.DeleteProjects.ACTION_DELETE_MASTERS;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_FETCH_MASTER_INNER;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.DB_PARAM;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.ViewUtils.shareAllowDownloadPopup;

public class MasterNavActivity extends AppCompatActivity implements View.OnClickListener, MastersSwipeAdapter.ItemTap, MastersSwipeAdapter.OnWidgetAction, CompoundButton.OnCheckedChangeListener {

    public static final String PARENT_ID = "PARENT_ID";
    public static final String FOLDER_NAME = "FOLDER_NAME";
    private static final String TAG = MasterNavActivity.class.getSimpleName();
    private RecyclerView recycle_view;
    private ProgressBar progressBar;
    private MastersSwipeAdapter mastersSwipeAdapter;
    private String parent_id;
    private TreeMap<String, Masters> masters;
    private ResponseReceiver responseReceiver;
    private Stack<String> nodeStack = new Stack<>();
    private int selectedPosition;
    private Masters selectedMasterNode;

    private EditText et_popup_input;
    private TextView tv_pop_title;
    private AppCompatButton btn_popup_cancel;
    private AppCompatButton btn_popup_rename;

    private SwipeRefreshLayout swiperefresh;

    private Dialog renameDialog;
    private static final CharSequence selectionMenuItems[] = new CharSequence[] {"Rename", "Share", "Delete"};
    private AlertDialog.Builder optionMenu;
    private FirebaseAuth mAuth;
    private String folder_name;
    private boolean allowDownload;
    private boolean allowSwitch = true;
    private AuthToken authToken;
    private Dialog shareDialog;
    private AppCompatButton btn_allow, btn_not_allow, btn_cp_url;
    private MixpanelAPI mixpanel;
    private SwitchCompat switch_expire_after_once, switch_expire_after_one_hour, switch_expire_never;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_nav);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Masters");
        parent_id = getIntent().getStringExtra(PARENT_ID);
        folder_name = getIntent().getStringExtra(FOLDER_NAME);
        if (parent_id!=null){
            recycle_view = findViewById(R.id.recycle_view);
            progressBar = findViewById(R.id.progressBar);
            mastersSwipeAdapter = new MastersSwipeAdapter(this,false, false);
            mastersSwipeAdapter.setItemTapListener(this);
            mastersSwipeAdapter.setOnWidgetAction(this);
            responseReceiver = new ResponseReceiver();
            registerReceivers();
            recycle_view.setHasFixedSize(true);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            recycle_view.setLayoutManager(linearLayoutManager);
            recycle_view.setAdapter(mastersSwipeAdapter);
            mAuth = FirebaseAuth.getInstance();
            mixpanel = MixpanelAPI.getInstance(this, YOUR_PROJECT_TOKEN);
            initUi();
            fetchMasters();

            authToken = new AuthToken() {
                @Override
                public void onToken(String token, String callback) {
                    switch (callback){
                        case "shareMasterAudioFile":
                            shareMasterAudioFile(token);
                            break;
                    }
                }
            };
        }
    }

    private void initUi(){
        renameDialog = new Dialog(this, R.style.MyDialogTheme);
        renameDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        renameDialog.setContentView(R.layout.input_popup);
        renameDialog.setCancelable(false);
        renameDialog.setCanceledOnTouchOutside(false);

        swiperefresh = findViewById(R.id.swiperefresh);

        swiperefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchMasters();
            }
        });

        et_popup_input = renameDialog.findViewById(R.id.et_popup_input);
        tv_pop_title = renameDialog.findViewById(R.id.tv_pop_title);

        btn_popup_rename = renameDialog.findViewById(R.id.btn_popup_create);
        btn_popup_cancel = renameDialog.findViewById(R.id.btn_popup_cancel);

        shareDialog = shareAllowDownloadPopup(MasterNavActivity.this);
        btn_allow = shareDialog.findViewById(R.id.btn_allow);
        btn_not_allow = shareDialog.findViewById(R.id.btn_not_allow);
        ImageView btn_close = shareDialog.findViewById(R.id.btn_close);
        switch_expire_after_once = shareDialog.findViewById(R.id.switch_expire_after_once);
        switch_expire_after_one_hour = shareDialog.findViewById(R.id.switch_expire_after_one_hour);
        switch_expire_never = shareDialog.findViewById(R.id.switch_expire_never);
        btn_cp_url = shareDialog.findViewById(R.id.btn_cp_url);
        switch_expire_after_once.setOnCheckedChangeListener(this);
        switch_expire_after_one_hour.setOnCheckedChangeListener(this);
        switch_expire_never.setOnCheckedChangeListener(this);

        btn_allow.setOnClickListener(this);
        btn_not_allow.setOnClickListener(this);
        btn_close.setOnClickListener(this);
        btn_cp_url.setOnClickListener(this);

        optionMenu = new AlertDialog.Builder(this);
        //optionMenu.setTitle("Select");
        optionMenu.setItems(selectionMenuItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case 0:
                    tv_pop_title.setText(R.string.rename_your_project);
                    et_popup_input.setText(selectedMasterNode.getName());
                    et_popup_input.requestFocus();
                    renameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    renameDialog.show();
                    break;

                case 1:
                    shareDialog.show();
                    break;

                case 2:
                    deleteConfirmation();
                    break;
            }
            }
        });

        btn_popup_rename.setText("RENAME");

        btn_popup_rename.setOnClickListener(this);
        btn_popup_cancel.setOnClickListener(this);
    }

    private void deleteConfirmation(){
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.sure_delete_message_title)
            .setMessage(R.string.sure_delete_message)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    deleteSelection();
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

    private void deleteSelection(){
        DeleteProjects.deleteMasters(this, selectedMasterNode, ACTION_DELETE_MASTERS);
        mastersSwipeAdapter.remove(selectedPosition);
    }

    private void rename(final String value){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("masters").child(selectedMasterNode.getId()).child("name").setValue(value).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            selectedMasterNode.setName(value);
            mastersSwipeAdapter.updateAtPos(selectedMasterNode,selectedPosition);
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
            progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void fetchMasters(){
        mastersSwipeAdapter.clearData();
        FirebaseDatabaseOperations.fetchMasters(this, ACTION_FETCH_MASTER_INNER, parent_id);
    }

    @Override
    public void onFileTap(ArrayList<Masters> mastersArrayList, int position) {
        Intent intent = new Intent(this, MasterPlayActivity.class);
        intent.putExtra(PARAM_MASTER,mastersArrayList);
        intent.putExtra(PARAM_MASTER_FILE,mastersArrayList.get(position));
        startActivityForResult(intent,0);
    }

    @Override
    public void onFolderTap(Masters masterNode,int position) {
        parent_id = masterNode.getId();
        nodeStack.push(masterNode.getParent_id());
        folder_name = masterNode.getName();
        fetchMasters();
    }

    @Override
    public void onLongPress(Masters masterNode, int position) {
        selectedPosition = position;
        selectedMasterNode = masterNode;
        optionMenu.show();
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
                    rename(val);
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
                shareMasterAudio(allowSwitch);
                break;

            case R.id.btn_close:
                shareDialog.dismiss();
                break;
        }
    }

    @Override
    public void onShare(Masters masters, int position) {
        shareDialog.show();
    }

    private void shareMasterAudio(boolean b){
        allowDownload = b;
        Utils.getAuthToken(this,mAuth,authToken,"shareMasterAudioFile");
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

    private void shareMasterAudioFile(String token){
        JSONArray configArr = new JSONArray();
        JSONObject config = new JSONObject();
        try {
            config.put("allow_download",allowDownload);
            config.put("type",selectedMasterNode.getType());
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
        params.put("ids", selectedMasterNode.getId());
        params.put("config",configArr.toString());
        client.post(APIs.SHARE_MASTER, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                progressBar.setVisibility(View.GONE);
                mixpanel.track("Sharing for Files");
                String responseString = new String(responseBody);
                Log.e("RESPONSE",responseString);
                try {
                    JSONObject response = new JSONObject(responseString);
                    shareLink(response.getJSONObject("data").getString("link"),"Share Audio");
                    Toast.makeText(MasterNavActivity.this, response.getString("msg"), Toast.LENGTH_SHORT).show();
                }
                catch (JSONException e) {
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

    private void shareLink(String link, String title){
        mixpanel.track(title);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, link);
        startActivity(Intent.createChooser(share, title));
    }


    @Override
    public void onRename(Masters masters, int position) {
        selectedPosition = position;
        selectedMasterNode = masters;
        tv_pop_title.setText(R.string.rename_your_project);
        et_popup_input.setText(selectedMasterNode.getName());
        et_popup_input.requestFocus();
        renameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        renameDialog.show();
    }

    @Override
    public void onDelete(Masters masters, int position) {
        selectedMasterNode = masters;
        selectedPosition = position;
        deleteConfirmation();
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
            Log.e(TAG,intent.getAction());
            switch (intent.getAction()){
                case ACTION_FETCH_MASTER_INNER:
                    progressBar.setVisibility(View.GONE);
                    masters = (TreeMap<String, Masters>) intent.getSerializableExtra(DB_PARAM);
                    loadData();
                    break;
            }
        }
    }

    private void loadData(){
        swiperefresh.setRefreshing(false);
        if (masters!=null && masters.size()>0){

            for(Object o : masters.entrySet()){
                Map.Entry pair = (Map.Entry) o;
                Masters masters = (Masters) pair.getValue();
                mastersSwipeAdapter.add(masters);
            }

            mastersSwipeAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadcast();
    }

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_FETCH_MASTER_INNER);
        LocalBroadcastManager.getInstance(this).registerReceiver(responseReceiver, filter);
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){
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
        if (nodeStack.size()>0){
            parent_id = nodeStack.pop();
            fetchMasters();
        }else{
            super.onBackPressed();
        }
    }
}
