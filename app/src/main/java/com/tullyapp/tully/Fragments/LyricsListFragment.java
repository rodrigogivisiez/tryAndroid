package com.tullyapp.tully.Fragments;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.tullyapp.tully.Adapters.LyricsListAdapter;
import com.tullyapp.tully.Dialogs.TutorialScreen;
import com.tullyapp.tully.FirebaseDataModels.LyricsModule.LyricsAppModel;
import com.tullyapp.tully.LyricsEditActivity;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Services.DeleteProjects;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.Configuration;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import cz.msebera.android.httpclient.Header;

import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_LYRICS;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_SEARCH_LYRICS;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.DB_PARAM;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_LYRICS;
import static com.tullyapp.tully.Utils.Constants.TUTORIAL_SCREENS;
import static com.tullyapp.tully.Utils.Constants.TUTS_LYRICS;
import static com.tullyapp.tully.Utils.Utils.showAlert;
import static com.tullyapp.tully.ViewPagerFragments.HomeAllFragment.REQUEST_PROJECT;

/**
 * A simple {@link Fragment} subclass.
 */
public class LyricsListFragment extends BaseFragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, LyricsListAdapter.AdapterInterface, LyricsListAdapter.OnWidgetAction {

    public static final int NEW_LYREICS_REQUEST_CODE = 12;
    private static final String TAG = LyricsListFragment.class.getSimpleName() ;
    private FirebaseAuth mAuth;
    private ResponseReceiver responseReceiver;
    private ArrayList<LyricsAppModel> lyricsAppModels = new ArrayList<>();
    private ProgressBar progressBar;
    protected SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout no_lyrics;
    private LyricsListAdapter lyricsListAdapter;
    private RecyclerView recycle_view;
    private LinearLayout widget_share;
    private LinearLayout widget_delete;
    private LinearLayout widget_check_all;
    private AppCompatCheckBox share_widget_checkbox_button;
    private LinearLayout share_widget_layout;
    private ArrayList<LyricsAppModel> selectedLyricsList;
    private LyricsAppModel lyricsObject;
    private Context context;
    private FragmentActivity mActivity;
    private boolean isOnLoad = false;
    AlertDialog.Builder optionMenu;
    private static final CharSequence selectionMenuItems[] = new CharSequence[] {"Share", "Delete"};
    private DatabaseReference mDatabase;
    private boolean isFromSearch = false;

    public LyricsListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isOnLoad = true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        responseReceiver = new ResponseReceiver();
        registerReceivers();
        mActivity = (FragmentActivity) context;
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unregisterBroadcast();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_lyrics_list, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        no_lyrics = view.findViewById(R.id.no_lyrics);
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

        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchLyrics();
            }
        });

        lyricsListAdapter = new LyricsListAdapter(context,lyricsAppModels);

        lyricsListAdapter.setOnWidgetAction(this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_from_bottom);
        recycle_view.setLayoutAnimation(animation);
        recycle_view.setLayoutManager(linearLayoutManager);
        recycle_view.setAdapter(lyricsListAdapter);

        fetchLyrics();

        return view;
    }

    private void runLayoutAnimation() {
        if (isOnLoad){
            LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_from_bottom);
            recycle_view.setLayoutAnimation(controller);
            recycle_view.getAdapter().notifyDataSetChanged();
            recycle_view.scheduleLayoutAnimation();
        }
        else{
            recycle_view.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        lyricsListAdapter.setAdapterListener(this);

        optionMenu = new AlertDialog.Builder(context);
        optionMenu.setItems(selectionMenuItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        if (selectedLyricsList==null){
                            selectedLyricsList = new ArrayList<>();
                        }
                        selectedLyricsList.clear();
                        selectedLyricsList.add(lyricsObject);
                        shareLyrics(selectedLyricsList);
                        break;

                    case 1:
                        if (selectedLyricsList==null){
                            selectedLyricsList = new ArrayList<>();
                        }
                        selectedLyricsList.clear();
                        selectedLyricsList.add(lyricsObject);
                        deleteConfirmation(selectedLyricsList);
                        break;
                }
            }
        });

        if (!Configuration.lyrics_tuts) showTutorailDialog(TUTS_LYRICS);
    }

    private void showTutorailDialog(String tut) {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
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

    public void fetchLyrics(){
        progressBar.setVisibility(View.VISIBLE);
        FirebaseDatabaseOperations.startAction(context,ACTION_PULL_LYRICS);
    }

    private void registerReceivers(){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_PULL_LYRICS);
        filter.addAction(ACTION_SEARCH_LYRICS);
        LocalBroadcastManager.getInstance(context).registerReceiver(responseReceiver, filter);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.share_widget_checkbox_button:
                lyricsListAdapter.setCheckedAll(share_widget_checkbox_button.isChecked());
                break;
        }
    }

    @Override
    public void onLyricsTap(LyricsAppModel lyricsAppModel, int position) {
        Intent intent = new Intent(context, LyricsEditActivity.class);
        intent.putExtra(INTENT_PARAM_LYRICS,lyricsAppModel);
        try{
            RecyclerView.ViewHolder holder = recycle_view.findViewHolderForAdapterPosition(position);
            if (holder!=null && holder.itemView!=null){
                TextView ll = holder.itemView.findViewById(R.id.tv_lyrics_excerpt);
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(mActivity, ll, ViewCompat.getTransitionName(ll));
                mActivity.startActivityFromFragment(this,intent, REQUEST_PROJECT, options.toBundle());
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onLongPress(LyricsAppModel lyricsAppModel, int position) {
        lyricsObject = lyricsAppModel;
        optionMenu.show();
    }

    @Override
    public void onShare(LyricsAppModel lyricsAppModel, int position) {
        if (selectedLyricsList==null){
            selectedLyricsList = new ArrayList<>();
        }
        selectedLyricsList.clear();
        selectedLyricsList.add(lyricsAppModel);
        shareLyrics(selectedLyricsList);
    }

    @Override
    public void onDelete(LyricsAppModel lyricsAppModel, int position) {
        if (selectedLyricsList==null){
            selectedLyricsList = new ArrayList<>();
        }
        selectedLyricsList.clear();
        selectedLyricsList.add(lyricsAppModel);
        deleteConfirmation(selectedLyricsList);
    }

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String ACTION = intent.getAction();
            try{
                if (ACTION!=null){
                    switch (ACTION){
                        case ACTION_PULL_LYRICS:
                            Log.e("LYRICS_UPDATE","LYRICS");
                            isFromSearch = false;
                            swipeRefreshLayout.setRefreshing(false);
                            lyricsAppModels.clear();
                            lyricsAppModels = (ArrayList<LyricsAppModel>) intent.getSerializableExtra(DB_PARAM);
                            if (lyricsAppModels!=null){
                                if (lyricsAppModels.size()>0){
                                    showLyricsList();
                                }else{
                                    showLyricsList();
                                    noRecordingsUI();
                                }
                            }
                            else{
                                noRecordingsUI();
                            }
                            break;

                        case ACTION_SEARCH_LYRICS:
                            swipeRefreshLayout.setRefreshing(false);
                            isFromSearch = true;
                            lyricsAppModels.clear();
                            lyricsAppModels = (ArrayList<LyricsAppModel>) intent.getSerializableExtra(DB_PARAM);
                            if (lyricsAppModels!=null){
                                if (lyricsAppModels.size()>0){
                                    showLyricsList();
                                }else{
                                    showLyricsList();
                                    noRecordingsUI();
                                }
                            }
                            else{
                                noRecordingsUI();
                            }
                            break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        isOnLoad = false;
        fetchLyrics();
        /*if (requestCode == NEW_LYREICS_REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK){
                fetchLyrics();
            }
            if (resultCode == Activity.RESULT_CANCELED) {

            }
        }*/
    }

    private void showLyricsList(){
        progressBar.setVisibility(View.GONE);
        no_lyrics.setVisibility(View.GONE);

        TreeMap<String,LyricsAppModel> lyricsAppModelTreeMap = new TreeMap<>();

        for(LyricsAppModel lyricsAppModel : lyricsAppModels){
            if (lyricsAppModel.isOfProject()){
                lyricsAppModelTreeMap.put(lyricsAppModel.getProjectId(),lyricsAppModel);
            }
            else{
                lyricsAppModelTreeMap.put(lyricsAppModel.getLyrics().getId(),lyricsAppModel);
            }
        }

        ArrayList<LyricsAppModel> mergerdAndSorted = new ArrayList<>();

        for(Object o : lyricsAppModelTreeMap.entrySet()){
            Map.Entry pair = (Map.Entry) o;
            LyricsAppModel lm = (LyricsAppModel) pair.getValue();
            mergerdAndSorted.add(lm);
        }

        Collections.reverse(mergerdAndSorted);

        lyricsListAdapter.setLyricsAppModels(mergerdAndSorted);
        runLayoutAnimation();

        if (isFromSearch && mergerdAndSorted.size()==0){
            showAlert(getContext(),getString(R.string.please_search_again),getString(R.string.no_search_result_found));
        }
    }

    private void noRecordingsUI(){
        progressBar.setVisibility(View.GONE);
        no_lyrics.setVisibility(View.VISIBLE);
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
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.widget_share:
                selectedLyricsList = lyricsListAdapter.getSelectedLyrics();
                if (selectedLyricsList.size()==0){
                    Toast.makeText(context, "No lyrics selected", Toast.LENGTH_SHORT).show();
                }
                else if (selectedLyricsList.size()>1){
                    Toast.makeText(context, "Currently not supporting multiple share", Toast.LENGTH_SHORT).show();
                }
                else{
                    shareLyrics(selectedLyricsList);
                }
                break;

            case R.id.widget_check_all:
                share_widget_checkbox_button.setChecked(!share_widget_checkbox_button.isChecked());
                break;

            case R.id.widget_delete:
                selectedLyricsList = lyricsListAdapter.getSelectedLyrics();
                if (selectedLyricsList.size()==0){
                    Toast.makeText(context, "No lyrics selected", Toast.LENGTH_SHORT).show();
                }else{
                    deleteConfirmation(selectedLyricsList);
                }
                break;
        }
    }

    private void deleteConfirmation(final ArrayList<LyricsAppModel> selectedLyricsList) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.sure_delete_message_title_lyrics)
            .setMessage(R.string.sure_delete_message_lyrics)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    deleteSelection(selectedLyricsList);
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

    private void deleteSelection(ArrayList<LyricsAppModel> selectedLyricsList){
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(context, "Deleting selected lyrics", Toast.LENGTH_SHORT).show();
        DeleteProjects.startActionDeleteLyrics(context,selectedLyricsList);
        toogleWidget(false);
    }

    private void shareLyrics(ArrayList<LyricsAppModel> lyricsAppModelArrayList){

        toogleWidget(false);

        progressBar.setVisibility(View.VISIBLE);

        ArrayList<String> no_project_lyrics_ids = new ArrayList<>();
        ArrayList<String> project_ids = new ArrayList<>();

        for(LyricsAppModel lm : lyricsAppModelArrayList){
            if (lm.isOfProject()){
                project_ids.add(lm.getProjectId());
            }
            else{
                no_project_lyrics_ids.add(lm.getLyrics().getId());
            }
        }

        String no_project_lyrics = no_project_lyrics_ids.toString().replaceAll("[\\[.\\].\\s+]", "");
        String project_ids_lyrics = project_ids.toString().replaceAll("[\\[.\\].\\s+]", "");

        progressBar.setVisibility(View.GONE);

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
        params.put("no_project_lyrics_ids", no_project_lyrics);
        params.put("project_lyrics", project_ids_lyrics);

        client.post(APIs.SHARE_LYRICS, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                try{
                    progressBar.setVisibility(View.GONE);
                    String responseString = new String(bytes);
                    try {
                        JSONObject response = new JSONObject(responseString);
                        if (response.getInt("status")==1){
                            String url = response.getJSONObject("data").getString("link");
                            Intent share = new Intent(Intent.ACTION_SEND);
                            share.setType("text/plain");
                            share.putExtra(Intent.EXTRA_SUBJECT,"Shared Lyrics Link");
                            share.putExtra(Intent.EXTRA_TEXT, url);
                            startActivity(Intent.createChooser(share, "Share Lyrics"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Something went wrong :(", Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                try{
                    Toast.makeText(getContext(), "Network Fail", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void toogleWidget(boolean show){
        if (show){
            share_widget_layout.setVisibility(View.VISIBLE);
            share_widget_layout.setAlpha(0.0f);
            share_widget_layout.animate()
                .alpha(1.0f)
                .setListener(null);
            lyricsListAdapter.showSelection(true);
        }else{
            share_widget_checkbox_button.setChecked(false);
            share_widget_layout.animate()
                .alpha(0.0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    share_widget_layout.setVisibility(View.GONE);
                    }
                });
            lyricsListAdapter.showSelection(false);
        }
    }

    @Override
    public void onSearchKey(String searchKey) {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseDatabaseOperations.startSearchNoProjectLyrics(context,searchKey);
    }

    @Override
    public void onSearchCancelled() {
        fetchLyrics();
    }

    @Override
    public void fabButtonClicked() {
        Intent intent = new Intent(context,LyricsEditActivity.class);
        startActivityForResult(intent, NEW_LYREICS_REQUEST_CODE);
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
}
