package com.tullyapp.tully.Fragments;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;
import com.tullyapp.tully.Adapters.ArtistOptionAdapter;
import com.tullyapp.tully.Adapters.ProfileLyricsAdapter;
import com.tullyapp.tully.Adapters.ProfileProjectAdapter;
import com.tullyapp.tully.Adapters.ProfileRecordingsAdapter;
import com.tullyapp.tully.FirebaseDataModels.FullProfile;
import com.tullyapp.tully.FirebaseDataModels.Lyrics;
import com.tullyapp.tully.FirebaseDataModels.LyricsModule.LyricsAppModel;
import com.tullyapp.tully.FirebaseDataModels.Profile;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.LyricsEditActivity;
import com.tullyapp.tully.Models.ArtistOption;
import com.tullyapp.tully.ProjectActivity;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.CircleTransform;
import com.tullyapp.tully.Utils.RecyclerItemClickListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_FULL_PROFILE;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.DB_PARAM;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_LYRICS;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_RECORDINGS;
import static com.tullyapp.tully.Utils.Constants.PROJECT_PARAM;
import static com.tullyapp.tully.Utils.Utils.getDirectory;
import static com.tullyapp.tully.ViewPagerFragments.HomeAllFragment.REQUEST_PROJECT;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends BaseFragment implements ProfileRecordingsAdapter.playRecordingListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, View.OnClickListener {

    private static final int LYREICS_REQUEST_CODE = 2;

    private ArtistOptionAdapter artistOptionAdapter;
    private ProfileProjectAdapter profileProjectAdapter;
    private ProfileLyricsAdapter profileLyricsAdapter;
    private ProfileRecordingsAdapter profileRecordingsAdapter;

    private ArrayList<ArtistOption> artistOptions = new ArrayList<>();
    private ArrayList<Project> projects = new ArrayList<>();
    private ArrayList<Lyrics> lyricses = new ArrayList<>();
    private ArrayList<Recording> recordings = new ArrayList<>();

    private TextView project_heading;
    private TextView lyrics_heading;
    private TextView recording_heading;

    private ResponseReceiver responseReceiver;

    private FirebaseAuth mAuth;
    private FullProfile fullProfile;

    private ProgressBar progressBar;
    private String userChoosenTask;

    private static final int REQUEST_CAMERA = 1888;
    private final int SELECT_FILE = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100;

    private MediaPlayer mediaPlayer = null;
    private Recording recObject;
    private File localdir;
    private Handler handler = new Handler();
    private int currentPos;
    private int audioDuration;
    private Context context;
    private ImageView profilePicture;
    private FragmentActivity mActivity;
    private TextView tv_projectname;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        mActivity = (FragmentActivity) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unRegisterReceiver();
        clearPlayer();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        profilePicture = view.findViewById(R.id.profile_picture);
        profilePicture.setOnClickListener(this);

        tv_projectname = view.findViewById(R.id.tv_projectname);

        project_heading = view.findViewById(R.id.project_heading);
        lyrics_heading = view.findViewById(R.id.lyrics_heading);
        recording_heading = view.findViewById(R.id.recording_heading);

        RecyclerView recycle_view_artist_option = view.findViewById(R.id.recycle_view_artist_option);
        RecyclerView recyclerView_projects = view.findViewById(R.id.recyclerView_projects);
        RecyclerView recycle_view_lyrics = view.findViewById(R.id.recycle_view_lyrics);
        RecyclerView recycle_view_recording = view.findViewById(R.id.recycle_view_recording);

        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        recycle_view_artist_option.setLayoutManager(layoutManager);

        layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        recyclerView_projects.setLayoutManager(layoutManager);

        layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        recycle_view_lyrics.setLayoutManager(layoutManager);

        layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        recycle_view_recording.setLayoutManager(layoutManager);

        artistOptionAdapter = new ArtistOptionAdapter(context,artistOptions);
        recycle_view_artist_option.setAdapter(artistOptionAdapter);

        profileProjectAdapter = new ProfileProjectAdapter(context, projects);
        profileLyricsAdapter = new ProfileLyricsAdapter(context,lyricses);
        profileRecordingsAdapter = new ProfileRecordingsAdapter(context, recordings);

        recyclerView_projects.setAdapter(profileProjectAdapter);
        recycle_view_lyrics.setAdapter(profileLyricsAdapter);
        recycle_view_recording.setAdapter(profileRecordingsAdapter);

        progressBar = view.findViewById(R.id.progressBar);

        recycle_view_lyrics.addOnItemTouchListener(new RecyclerItemClickListener(context, new RecyclerItemClickListener.OnItemClickListener(){
            @Override
            public void onItemClick(View view, int position) {
            clearPlayer();
            Lyrics lyric = lyricses.get(position);
            Intent intent = new Intent(context,LyricsEditActivity.class);
            LyricsAppModel lyricsAppModel = new LyricsAppModel();
            lyricsAppModel.setOfProject(lyric.getProjectID() != null);
            lyricsAppModel.setProjectId(lyric.getProjectID());
            lyricsAppModel.setLyrics(lyric);
            intent.putExtra(INTENT_PARAM_LYRICS,lyricsAppModel);

            TextView ll = view.findViewById(R.id.lyrics);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(mActivity, ll, ViewCompat.getTransitionName(ll));
            startActivityForResult(intent, LYREICS_REQUEST_CODE, options.toBundle());
            }
        }));

        recyclerView_projects.addOnItemTouchListener(new RecyclerItemClickListener(context, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(context, ProjectActivity.class);
                Project project = profileProjectAdapter.getItemByPosition(position);
                if (project!=null){
                    intent.putExtra(PROJECT_PARAM,project);
                    TextView ll = view.findViewById(R.id.tv_projectname);
                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(mActivity, ll, ViewCompat.getTransitionName(ll));
                    startActivityForResult(intent, REQUEST_PROJECT, options.toBundle());
                }
            }
        }));

        localdir = getDirectory(context,LOCAL_DIR_NAME_RECORDINGS);
        profileRecordingsAdapter.setOnPlayClickListener(this);

        responseReceiver = new ResponseReceiver();

        registerReceiver();
        fetchFullDB();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tv_projectname.setText(mAuth.getCurrentUser().getDisplayName());
        try{
            Picasso.with(getContext()).load(mAuth.getCurrentUser().getPhotoUrl()).placeholder(R.drawable.default_profile_picture).transform(new CircleTransform()).into(profilePicture);
        }catch (Exception e){

        }
    }

    private void fetchFullDB(){
        progressBar.setVisibility(View.VISIBLE);
        FirebaseDatabaseOperations.startActionPullFullProfile(context);
    }

    private void registerReceiver(){
        unRegisterReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PULL_FULL_PROFILE);
        LocalBroadcastManager.getInstance(context).registerReceiver(responseReceiver,filter);
    }

    private void unRegisterReceiver(){
        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(responseReceiver);
        }catch (Exception e){

        }
    }

    @Override
    public void onPlayRecord(Recording recording, int position) {
        play(recording, position);
    }

    @Override
    public void onRecordPause(Recording recording, int position) {
        try{
            if (mediaPlayer!=null){
                handler.removeCallbacks(updateProgressbar);
                mediaPlayer.pause();
            }
        }catch (Exception e){

        }
    }

    @Override
    public void onResume(Recording recording, int position) {
        if (mediaPlayer!=null){
            try {
                mediaPlayer.start();
                handler.post(updateProgressbar);
            }catch (Exception e){

            }
        }
    }


    private void play(Recording recording, int position){

        currentPos = position;

        handler.removeCallbacks(updateProgressbar);

        if (mediaPlayer!=null){
            clearPlayer();
        }

        recObject = recording;
        File localFile = new File(localdir, recording.getTid());
        String path = "";

        if (localFile.exists()){
            path = localFile.getAbsolutePath();
        }
        else{
            if (recording.getDownloadURL()!=null){
                path = recording.getDownloadURL();
            }
            else{
                Toast.makeText(context, "url not provided", Toast.LENGTH_SHORT).show();
            }
        }

        if (!path.isEmpty()){
            try {
                profileRecordingsAdapter.markAudioPlaying(recording);
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(path);
                mediaPlayer.prepareAsync();
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setOnCompletionListener(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        try{
            mp.start();
            audioDuration = mp.getDuration();
            handler.post(updateProgressbar);
        }catch (Exception e){

        }

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        handler.removeCallbacks(updateProgressbar);
        clearPlayer();
        try {
            profileRecordingsAdapter.markAudioComplete(recObject);
        }catch (Exception e){

        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.profile_picture:
                selectImage();
                break;
        }
    }

    @Override
    public void onSearchKey(String searchKey) {

    }

    @Override
    public void onSearchCancelled() {

    }

    @Override
    public void fabButtonClicked() {

    }

    @Override
    public void actionEvent(int event) {

    }

    class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action!=null){
                switch (action){
                    case ACTION_PULL_FULL_PROFILE:
                        progressBar.setVisibility(View.GONE);
                        fullProfile = (FullProfile) intent.getSerializableExtra(DB_PARAM);
                        if (fullProfile !=null){
                            loadUI();
                        }
                        break;
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case LYREICS_REQUEST_CODE:
                if(resultCode == Activity.RESULT_OK){
                    Toast.makeText(context, "Lyrics Modified", Toast.LENGTH_SHORT).show();
                    fetchFullDB();
                }
                if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(context, "Lyrics Not Modified", Toast.LENGTH_SHORT).show();
                }
                break;

            case SELECT_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    onSelectFromGalleryResult(data);
                }
                break;

            case REQUEST_CAMERA:
                if (resultCode == Activity.RESULT_OK) {
                    onCaptureImageResult(data);
                }
                break;
        }
    }

    private void loadUI(){
        handler.removeCallbacks(updateProgressbar);
        artistOptions.clear();
        projects.clear();
        lyricses.clear();
        recordings.clear();

        Profile profile = fullProfile.getProfile();

        if (profile!=null && profile.getArtist_option()!=null && !profile.getArtist_option().isEmpty()){
            String[] artistOptionsArray = profile.getArtist_option().split(",");

            if (artistOptionsArray.length>0){
                for (String anArtistOptionsArray : artistOptionsArray) {
                    artistOptions.add(new ArtistOption(R.drawable.profile_ico_profile, anArtistOptionsArray));
                }
            }
        }

        if (profile!=null && profile.getGenre()!=null && !profile.getGenre().isEmpty()){
            artistOptions.add(new ArtistOption(R.drawable.profile_ico_audio,profile.getGenre()));
        }

        HashMap<String,Project> projectsHashMap = fullProfile.getProjects();
        HashMap<String,Lyrics> lyricsHashMap;
        HashMap<String,Recording> recordingsHashMap;

        Iterator it = null;
        Iterator lit;
        Iterator rit;
        if (projectsHashMap!=null){
            it = projectsHashMap.entrySet().iterator();
        }

        Project project;
        Lyrics lyric;
        Recording recording;

        if (it!=null){
            while (it.hasNext()){
                Map.Entry pair = (Map.Entry) it.next();
                project = (Project) pair.getValue();
                project.setId(pair.getKey().toString());
                projects.add(project);

                lyricsHashMap = project.getLyrics();
                if (lyricsHashMap!=null){
                    lit = lyricsHashMap.entrySet().iterator();
                    while (lit.hasNext()){
                        Map.Entry lyricsPair = (Map.Entry) lit.next();
                        lyric = (Lyrics) lyricsPair.getValue();
                        lyric.setId(lyricsPair.getKey().toString());
                        lyric.setTitle(project.getProject_name());
                        lyric.setProjectID(project.getId());
                        lyricses.add(lyric);
                        lit.remove();
                    }
                }

                recordingsHashMap = project.getRecordings();
                if (recordingsHashMap!=null){
                    rit = recordingsHashMap.entrySet().iterator();
                    while (rit.hasNext()){
                        Map.Entry recordingPair = (Map.Entry) rit.next();
                        recording = (Recording) recordingPair.getValue();
                        if (project.getProject_main_recording() !=null && project.getProject_main_recording().equals(recording.getTid()))
                            continue;
                        recording.setId(recordingPair.getKey().toString());
                        recording.setProjectName(project.getProject_name());
                        recording.setProjectId(project.getId());
                        recordings.add(recording);
                        rit.remove();
                    }
                }
                it.remove();
            }
        }

        if (fullProfile.getNo_project()!=null){
            lyricsHashMap = fullProfile.getNo_project().getLyrics();

            if (lyricsHashMap!=null){
                lit = lyricsHashMap.entrySet().iterator();
                while (lit.hasNext()){
                    Map.Entry lyricsPair = (Map.Entry) lit.next();
                    lyric = (Lyrics) lyricsPair.getValue();
                    lyric.setId(lyricsPair.getKey().toString());
                    lyric.setTitle("No Project Assigned");
                    lyricses.add(lyric);

                    lit.remove();
                }
            }

            recordingsHashMap = fullProfile.getNo_project().getRecordings();

            if (recordingsHashMap!=null){
                rit = recordingsHashMap.entrySet().iterator();
                while (rit.hasNext()){
                    Map.Entry recordingPair = (Map.Entry) rit.next();
                    recording = (Recording) recordingPair.getValue();
                    recording.setId(recordingPair.getKey().toString());
                    recording.setProjectName("No Project Assigned");
                    recordings.add(recording);
                    rit.remove();
                }
            }
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();

        SpannableString str1= new SpannableString(""+ projects.size());
        SpannableString str2= new SpannableString(""+ lyricses.size());
        SpannableString str3= new SpannableString(""+ recordings.size());
        StyleSpan bold = new StyleSpan(Typeface.BOLD);
        str1.setSpan(bold, 0, str1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        str2.setSpan(bold, 0, str2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        str3.setSpan(bold, 0, str3.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        builder.append("Project ");
        builder.append(str1);
        project_heading.setText(builder, TextView.BufferType.SPANNABLE);

        builder.clear();
        builder.append("Lyrics ");
        builder.append(str2);
        lyrics_heading.setText(builder, TextView.BufferType.SPANNABLE);

        builder.clear();
        builder.append("Recording ");
        builder.append(str3);
        recording_heading.setText(builder, TextView.BufferType.SPANNABLE);

        artistOptionAdapter.notifyDataSetChanged();
        profileProjectAdapter.notifyDataSetChanged();
        profileLyricsAdapter.notifyDataSetChanged();
        profileRecordingsAdapter.notifyDataSetChanged();
    }

    private void selectImage() {
        final CharSequence[] items = { "Take Photo", "Choose from Library", "Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result= Utility.checkPermission(context);

                if (items[item].equals("Take Photo")) {
                    userChoosenTask="Take Photo";
                    if(result)
                        cameraIntent();

                } else if (items[item].equals("Choose from Library")) {
                    userChoosenTask="Choose from Library";
                    if(result)
                        galleryIntent();

                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void cameraIntent(){
        if (ContextCompat.checkSelfPermission(context,android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
        else{
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, REQUEST_CAMERA);
            }
            else {
                Toast.makeText(context, "Camera Permission failed", Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                galleryIntent();
            }
        }
    }

    private void onSelectFromGalleryResult(Intent data) {
        Bitmap bm;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(context.getContentResolver(), data.getData());
                bm=getResizedBitmap(bm,500,500);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 80, bytes);
                FirebaseDatabaseOperations.uploadProfilePic(context,bytes.toByteArray());
                CircleTransform circleTransform = new CircleTransform();
                bm = circleTransform.transform(bm);
                profilePicture.setImageBitmap(bm);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        int width = bm.getWidth();
        int height = bm.getHeight();

        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) newHeight / (float) newWidth;

        int finalWidth = newWidth;
        int finalHeight = newHeight;

        if (ratioMax > 1) {
            finalWidth = (int) ((float)newHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float)newWidth / ratioBitmap);
        }

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, finalWidth, finalHeight, true);

        Log.e("SIZE",resizedBitmap.getWidth()+","+resizedBitmap.getHeight());

        return resizedBitmap;
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail = getResizedBitmap(thumbnail,500,500);
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, bytes);
        File destination = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".jpg");
        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FirebaseDatabaseOperations.uploadProfilePic(context,bytes.toByteArray());
        CircleTransform circleTransform = new CircleTransform();
        thumbnail = circleTransform.transform(thumbnail);
        profilePicture.setImageBitmap(thumbnail);
    }

    private void galleryIntent(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select File"),SELECT_FILE);
    }

    public static class Utility {
        static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        static boolean checkPermission(final Context context) {
            int currentAPIVersion = Build.VERSION.SDK_INT;
            Log.i("API", String.valueOf(currentAPIVersion));
            if(currentAPIVersion>=android.os.Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                        alertBuilder.setCancelable(true);
                        alertBuilder.setTitle("Permission necessary");
                        alertBuilder.setMessage("External storage permission is necessary");
                        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                            }
                        });
                        AlertDialog alert = alertBuilder.create();
                        alert.show();
                    } else {
                        ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                    return false;
                } else {
                    return true;
                }
            } else {
                return true;
            }
        }
    }


    Runnable updateProgressbar = new Runnable() {
        static final long PROGRESS_UPDATE = 100;
        int currentTime;
        int percent;
        double dd;
        @Override
        public void run() {
            try{
                if (mediaPlayer.isPlaying()){
                    currentTime = mediaPlayer.getCurrentPosition();
                    dd = audioDuration / 100;
                    percent = (int) (currentTime / dd);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            profileRecordingsAdapter.updateProgress(currentPos,percent);
                        }
                    });
                    handler.postDelayed(this, PROGRESS_UPDATE);
                }
            }catch (Exception e){

            }
        }
    };

    private void clearPlayer(){
        handler.removeCallbacks(updateProgressbar);
        try{
            mediaPlayer.stop();
        }catch (Exception e){

        }
        try {
            mediaPlayer.release();
        }catch (Exception e){

        }
    }
}
