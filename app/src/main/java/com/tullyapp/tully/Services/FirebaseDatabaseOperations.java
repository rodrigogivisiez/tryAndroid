package com.tullyapp.tully.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.FirebaseDataModels.BeatAudio;
import com.tullyapp.tully.FirebaseDataModels.FullProfile;
import com.tullyapp.tully.FirebaseDataModels.HomeDb;
import com.tullyapp.tully.FirebaseDataModels.Lyrics;
import com.tullyapp.tully.FirebaseDataModels.LyricsModule.LyricsAppModel;
import com.tullyapp.tully.FirebaseDataModels.Masters;
import com.tullyapp.tully.FirebaseDataModels.Profile;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.FirebaseDataModels.Settings;
import com.tullyapp.tully.Models.EngineerAccessModel;
import com.tullyapp.tully.Models.RemainingUpload;
import com.tullyapp.tully.R;
import com.tullyapp.tully.SLDB.DataManager;
import com.tullyapp.tully.Utils.ActionEventConstant;
import com.tullyapp.tully.Utils.FirebaseDatabaseKeys;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static com.tullyapp.tully.Engineer.EngineerSettingsFragment.PARAM_ENGINEER_ACCESS_MODEL;
import static com.tullyapp.tully.Fragments.MasterDetailsFragment.ACTION_FETCH_MASTERS_FRAGMENT;
import static com.tullyapp.tully.Fragments.MasterDetailsFragment.PARAM_MASTER_FILE;
import static com.tullyapp.tully.Utils.Constants.UPLOAD_TYPE_PROJECT_RECORDING;
import static com.tullyapp.tully.Utils.Constants.UPLOAD_TYPE_RECORDING;
import static com.tullyapp.tully.Utils.Utils.isInternetAvailable;

public class FirebaseDatabaseOperations extends IntentService {

    public static final String ACTION_PULL_HOME = "com.tullyapp.tully.Services.action.PULL_HOME";
    public static final String ACTION_FETCH_MASTERS = "com.tullyapp.tully.Services.action.FETCH_MASTERS";
    public static final String ACTION_FETCH_MASTER_INNER = "com.tullyapp.tully.Services.action.FETCH_MASTER_INNER";
    public static final String ACTION_PULL_LYRICS = "com.tullyapp.tully.Services.action.PULL_LYRICS";
    public static final String ACTION_PULL_RECORDINGS = "com.tullyapp.tully.Services.action.PULL_RECORDINGS";
    public static final String ACTION_PULL_COPYTOTULLY = "com.tullyapp.tully.Services.action.PULL_COPYTOTULLY";
    public static final String ACTION_PULL_PROJECT = "com.tullyapp.tully.Services.action.PULL_PROJECTS";
    public static final String ACTION_SAVE_LYRICS = "com.tullyapp.tully.Services.action.SAVE_LYRICS";
    public static final String ACTION_SAVE_RECORDINGS = "com.tullyapp.tully.Services.action.SAVE_RECORDINGS";
    public static final String ACTION_PULL_FULL_PROFILE = "com.tullyapp.tully.Services.action.FULL_PROFILE";
    public static final String ACTION_SEARCH_HOME = "com.tullyapp.tully.Services.action.SEARCH_HOME";
    public static final String ACTION_GET_ENGINEER_ADMIN_ACCESS_SUBSCRIPTION = "ACTION_GET_ENGINEER_ADMIN_ACCESS_SUBSCRIPTION";
    public static final String ACTION_SET_CONNECTION_STATUS = "ACTION_SET_CONNECTION_STATUS";
    public static final String ACTION_UPLOAD_PROJECT_RECORDING = "com.tullyapp.tully.UPLOAD_PROJECT_RECORDING";

    public static final String DB_PARAM = "DB_PARAM";
    public static final String REQUEST_PARAM = "REQUEST_PARAM";
    public static final String DB_PULL_STATUS = "STATUS";
    public static final String RESPONSE_PARAM = "RESPONSE_PARAM";
    private static final String FILE_PARAM = "FILE_PARAM";
    private static final String PARAM_RECORDING = "PARAM_RECORDING";
    public static final String ACTION_SEARCH_RECORDING = "SEARCH_RECORDING" ;
    private static final String KEY = "KEY";
    public static final String ACTION_SEARCH_LYRICS = "ACTION_SEARCH_LYRICS";
    private static final String UPLOAD_PICTURE = "UPLOAD_PICTURE";
    private static final String PARENT_ID = "PARENT_ID";
    private static final String TAG = FirebaseDatabaseOperations.class.getSimpleName();
    private static final String UPDATE_BPM_AND_KEY_AUDIO = "UPDATE_BPM_AND_KEY_AUDIO";
    private static final String UPDATE_BPM_AND_KEY_RECORDING = "UPDATE_BPM_AND_KEY_RECORDING";
    public static final String IS_ACTIVE = "IS_ACTIVE";
    private static final String PARAM_PLAN_TYPE = "PARAM_PLAN_TYPE";
    private static final String UPDATE_BPM_AND_KEY_MASTERS = "UPDATE_BPM_AND_KEY_MASTERS";
    public static final String ACTION_GET_CUSTOMER_ID = "ACTION_GET_CUSTOMER_ID";
    public static final String PARAM_CUSTOMER_ID = "PARAM_CUSTOMER_ID";
    public static final String ACTION_GET_ENGINEER_DATA = "ACTION_GET_ENGINEER_DATA";
    private static final String ACTION_SET_ENGINEER_ADMIN_ACCESS = "ACTION_SET_ENGINEER_ADMIN_ACCESS";
    public static final String PARAM_PROFILE = "PARAM_PROFILE";
    public static final String PARAM_SETTINGS = "PARAM_SETTINGS";
    private static final String ACTION_SET_NOTIFICATION_TOKEN = "ACTION_SET_NOTIFICATION_TOKEN";
    private static final String PARAM_NOTIFICATION_TOKEN = "PARAM_NOTIFICATION_TOKEN";
    private FirebaseAuth mAuth;
    ArrayList<Recording> recordingAppModelArrayList = new ArrayList<>();
    ArrayList<AudioFile> audioFileArrayList = new ArrayList<>();
    private FirebaseStorage storage;
    private StorageReference storageRef;
    public FirebaseDatabaseOperations() {
        super("FirebaseDatabaseOperations");
    }

    public static void startAction(Context context, String action) {
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(action);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void fetchMasters(Context context, String action, String parent_id){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(action);
        intent.putExtra(PARENT_ID,parent_id);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startActionsaveLyric(Context context, LyricsAppModel lyricsAppModel){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_SAVE_LYRICS);
        intent.putExtra(REQUEST_PARAM,lyricsAppModel);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startActionSaveRecording(Context context, Recording recording, File file){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_SAVE_RECORDINGS);
        intent.putExtra(REQUEST_PARAM,recording);
        intent.putExtra(FILE_PARAM,file);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startActionPullProject(Context context,String projectId){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_PULL_PROJECT);
        intent.putExtra(REQUEST_PARAM,projectId);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startActionPullFullProfile(Context context){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_PULL_FULL_PROFILE);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startActionCopyToTully(Context context){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_PULL_COPYTOTULLY);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startActionUploadProjectRecording(Context context, Recording recording, File file){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_UPLOAD_PROJECT_RECORDING);
        intent.putExtra(PARAM_RECORDING,recording);
        intent.putExtra(FILE_PARAM,file);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startSearchHome(Context context, String key){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_SEARCH_HOME);
        intent.putExtra(KEY,key);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startSearchRecording(Context context, String key){
        Intent intent = new Intent(context,FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_SEARCH_RECORDING);
        intent.putExtra(KEY,key);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startSearchNoProjectLyrics(Context context, String key){
        Intent intent = new Intent(context,FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_SEARCH_LYRICS);
        intent.putExtra(KEY,key);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void uploadProfilePic(Context context, byte[] bytes){
        try {
            Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
            intent.setAction(UPLOAD_PICTURE);
            intent.putExtra(KEY,bytes);
            try{
                ContextCompat.startForegroundService(context,intent);
            }catch (Exception e){
                e.printStackTrace();
            }
        }catch (Exception e){
            Toast.makeText(context, "File too Large", Toast.LENGTH_SHORT).show();
        }
    }

    public static void updateBmpandKeyAudioFile(Context context, AudioFile audioFile){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(UPDATE_BPM_AND_KEY_AUDIO);
        intent.putExtra(REQUEST_PARAM,audioFile);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void updateBmpandKeyMasterFile(Context context, Masters masters){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(UPDATE_BPM_AND_KEY_MASTERS);
        intent.putExtra(REQUEST_PARAM,masters);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void updateBmpandKeyRecording(Context context, Recording recording){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(UPDATE_BPM_AND_KEY_RECORDING);
        intent.putExtra(REQUEST_PARAM,recording);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void getEngineerAdminSubscription(Context context){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_GET_ENGINEER_ADMIN_ACCESS_SUBSCRIPTION);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void getCustomerIdentification(Context context){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_GET_CUSTOMER_ID);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void getEngineerData(Context context, EngineerAccessModel engineerAccessModel){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_GET_ENGINEER_DATA);
        intent.putExtra(PARAM_ENGINEER_ACCESS_MODEL,engineerAccessModel);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void setAdminAccessForEngineer(Context context, EngineerAccessModel engineerAccessModel, boolean boo){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_SET_ENGINEER_ADMIN_ACCESS);
        intent.putExtra(PARAM_ENGINEER_ACCESS_MODEL,engineerAccessModel);
        intent.putExtra(REQUEST_PARAM,boo);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void setPushNotificationToken(Context context, String token){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_SET_NOTIFICATION_TOKEN);
        intent.putExtra(PARAM_NOTIFICATION_TOKEN,token);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void setConnectionState(Context context){
        Intent intent = new Intent(context, FirebaseDatabaseOperations.class);
        intent.setAction(ACTION_SET_CONNECTION_STATUS);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (Build.VERSION.SDK_INT >= 26) {
                String CHANNEL_ID = getString(R.string.app_name);
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "database operations", NotificationManager.IMPORTANCE_DEFAULT);
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("").setContentText("").build();
                startForeground(1, notification);
            }
            mAuth = FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser()!=null){
                String Action = intent.getAction();

                if (Action!=null){
                    switch (Action){
                        case ACTION_PULL_HOME:
                                pullHome(Action);
                            break;

                        case ACTION_SET_CONNECTION_STATUS:
                            setConnectionState();
                            break;

                        case ACTION_SET_NOTIFICATION_TOKEN:
                            setPushNotificationToken(intent.getStringExtra(PARAM_NOTIFICATION_TOKEN));
                            break;

                        case ACTION_SET_ENGINEER_ADMIN_ACCESS:
                                setEngineerAdminAccess((EngineerAccessModel) intent.getSerializableExtra(PARAM_ENGINEER_ACCESS_MODEL), intent.getBooleanExtra(REQUEST_PARAM,false));
                            break;

                        case ACTION_GET_ENGINEER_DATA:
                                getEngineerData((EngineerAccessModel) intent.getSerializableExtra(PARAM_ENGINEER_ACCESS_MODEL));
                            break;

                        case ACTION_GET_CUSTOMER_ID:
                                getCustomerId();
                            break;

                        case UPDATE_BPM_AND_KEY_AUDIO:
                                updateBpmAndKeyAudio((AudioFile) intent.getSerializableExtra(REQUEST_PARAM));
                            break;

                        case UPDATE_BPM_AND_KEY_MASTERS:
                                updateBpmAndKeyMasters((Masters) intent.getSerializableExtra(REQUEST_PARAM));
                            break;

                        case UPDATE_BPM_AND_KEY_RECORDING:
                                updateBmpandKeyRecording((Recording) intent.getSerializableExtra(REQUEST_PARAM));
                            break;

                        case ACTION_FETCH_MASTERS:
                            pullMasters(Action, intent.getStringExtra(PARENT_ID));
                            break;

                        case ACTION_FETCH_MASTER_INNER:
                            pullMasters(Action, intent.getStringExtra(PARENT_ID));
                            break;

                        case ACTION_FETCH_MASTERS_FRAGMENT:
                            pullMasters(Action, intent.getStringExtra(PARENT_ID));
                            break;

                        case ACTION_PULL_LYRICS:
                            pullLyrics();
                            break;

                        case ACTION_SAVE_LYRICS:
                            saveLyrics((LyricsAppModel) intent.getSerializableExtra(REQUEST_PARAM));
                            break;

                        case ACTION_PULL_RECORDINGS:
                            pullProjectsRecording();
                            break;

                        case ACTION_SAVE_RECORDINGS:
                            saveRecordings((Recording) intent.getSerializableExtra(REQUEST_PARAM), (File) intent.getSerializableExtra(FILE_PARAM));
                            break;

                        case ACTION_PULL_PROJECT:
                            pullProject(intent.getStringExtra(REQUEST_PARAM));
                            break;

                        case ACTION_PULL_FULL_PROFILE:
                            pullFullProfile();
                            break;

                        case ACTION_PULL_COPYTOTULLY:
                            pullCopyToTull();
                            break;

                        case ACTION_UPLOAD_PROJECT_RECORDING:
                            uploadProjectRecording((Recording) intent.getSerializableExtra(PARAM_RECORDING), (File) intent.getSerializableExtra(FILE_PARAM));
                            break;

                        case ACTION_SEARCH_HOME:
                            searchHomeFiles(intent.getStringExtra(KEY));
                            break;

                        case ACTION_SEARCH_RECORDING:
                            searchNoProjectRecording(intent.getStringExtra(KEY));
                            break;

                        case ACTION_SEARCH_LYRICS:
                            searchNoProjectLyrics(intent.getStringExtra(KEY));
                            break;

                        case ACTION_GET_ENGINEER_ADMIN_ACCESS_SUBSCRIPTION:
                            getActionSetEngineerAdminAccessSubscriptionSubscription();
                            break;

                        case UPLOAD_PICTURE:
                            try {
                                uploadProfilePicture((byte[]) intent.getSerializableExtra(KEY));
                            }catch (Exception e){
                                Toast.makeText(this, "File Too Large", Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                }
            }
        }
    }

    private void setConnectionState(){
        if (mAuth.getCurrentUser()!=null){
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference userDatabase = database.getReference().child(mAuth.getCurrentUser().getUid());
            // since I can connect from multiple devices, we store each connection instance separately
            // any time that connectionsRef's value is null (i.e. has no children) I am offline
            final DatabaseReference userProfileReference = userDatabase.child("profile");
            // stores the timestamp of my last disconnect (the last time I was seen online)
            final DatabaseReference lastOnlineRef = userProfileReference.child("lastOnline");

            final DatabaseReference connectedRef = database.getReference(".info/connected");
            connectedRef.addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean connected = snapshot.getValue(Boolean.class);
                    if (connected) {
                        DatabaseReference con = userProfileReference.child("actives").push();

                        // when this device disconnects, remove it
                        con.onDisconnect().removeValue();

                        // when I disconnect, update the last time I was seen online
                        lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP);

                        // add this device to my connections list
                        // this value could contain info about the device or a timestamp too
                        con.setValue(Boolean.TRUE);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("Listener was cancelled at .info/connected");
                }
            });
        }
    }

    private void setPushNotificationToken(String token){
        if (token!=null){
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
            Map<String,Object> values = new HashMap<>();
            values.put(FirebaseDatabaseKeys.notificationToken,token);
            mDatabase.child(FirebaseDatabaseKeys.settings).updateChildren(values);
        }
    }

    private void setEngineerAdminAccess(EngineerAccessModel engineerAccessModel, boolean boo){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        Map<String,Object> values = new HashMap<>();
        values.put("adminAccess",boo);
        mDatabase.child("engineer").child("access").child(engineerAccessModel.getId()).updateChildren(values);
    }

    private void getEngineerData(EngineerAccessModel engineerAccessModel){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("masters").orderByChild("parentEngineer").equalTo("0:"+engineerAccessModel.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Intent intent = new Intent(ACTION_GET_ENGINEER_DATA);
                TreeMap<String, Masters> masterNodes = new TreeMap<>();
                try{
                    if (dataSnapshot.exists()){
                        for(DataSnapshot node : dataSnapshot.getChildren()){
                            Masters masters = node.getValue(Masters.class);
                            if (masters!=null){
                                masters.setId(node.getKey());
                                masterNodes.put(node.getKey(),masters);
                            }
                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                finally {
                    intent.putExtra(PARAM_MASTER_FILE,masterNodes);
                    sendBroadCast(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                TreeMap<String, Masters> masterNodes = new TreeMap<>();
                Intent intent = new Intent(ACTION_GET_ENGINEER_DATA);
                intent.putExtra(PARAM_MASTER_FILE,masterNodes);
                sendBroadCast(intent);
            }
        });
    }

    private void getCustomerId(){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("settings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Intent intent = new Intent(ACTION_GET_CUSTOMER_ID);
                if (dataSnapshot.exists()){
                    if (dataSnapshot.hasChild("customer_id")){
                        String customer_id = (String) dataSnapshot.child("customer_id").getValue();
                        intent.putExtra(PARAM_CUSTOMER_ID,customer_id);
                        sendBroadCast(intent);
                    }
                    else {
                        intent.putExtra(PARAM_CUSTOMER_ID,"");
                        sendBroadCast(intent);
                    }
                }
                else {
                    intent.putExtra(PARAM_CUSTOMER_ID,"");
                    sendBroadCast(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Intent intent = new Intent(ACTION_GET_CUSTOMER_ID);
                intent.putExtra(PARAM_CUSTOMER_ID,"");
                sendBroadCast(intent);
            }
        });
    }

    private void getActionSetEngineerAdminAccessSubscriptionSubscription(){
        DatabaseReference mDb = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDb.child("settings").keepSynced(true);
        mDb.child("settings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Intent intent = new Intent(ACTION_GET_ENGINEER_ADMIN_ACCESS_SUBSCRIPTION);
                intent.putExtra(IS_ACTIVE,false);
                if (dataSnapshot.exists()){
                    if (dataSnapshot.hasChild("engineerAdminAccess") && dataSnapshot.child("engineerAdminAccess").hasChild("isActive")){
                        boolean isActive = (boolean) dataSnapshot.child("engineerAdminAccess").child("isActive").getValue();
                        String planType = (String) dataSnapshot.child("engineerAdminAccess").child("planType").getValue();
                        intent.putExtra(IS_ACTIVE,isActive);
                        if (isActive){
                            intent.putExtra(PARAM_PLAN_TYPE,planType);
                        }
                    }
                }
                sendBroadCast(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Intent intent = new Intent(ACTION_GET_ENGINEER_ADMIN_ACCESS_SUBSCRIPTION);
                intent.putExtra(IS_ACTIVE,false);
                sendBroadCast(intent);
            }
        });
    }

    private void updateBmpandKeyRecording(Recording recording) {
        DatabaseReference mDb = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        DatabaseReference ref = mDb.child("projects").child(recording.getProjectId()).child("recordings").child(recording.getId());
        ref.child("bpm").setValue(recording.getBpm());
        ref.child("key").setValue(recording.getKey());
    }

    private void updateBpmAndKeyAudio(AudioFile audioFile){
        DatabaseReference mDb = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        DatabaseReference ref = mDb.child("copytotully").child(audioFile.getId());
        ref.child("bpm").setValue(audioFile.getBpm());
        ref.child("key").setValue(audioFile.getKey());
    }

    private void updateBpmAndKeyMasters(Masters masters){
        DatabaseReference mDb = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        DatabaseReference ref = mDb.child("masters").child(masters.getId());
        ref.child("bpm").setValue(masters.getBpm());
        ref.child("key").setValue(masters.getKey());
    }

    private void uploadProjectRecording(final Recording recording, final File file){
        final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("projects").child(recording.getProjectId()).child("recordings").child(recording.getId()).setValue(recording);

        if (isInternetAvailable(getApplication())){
            storage = FirebaseStorage.getInstance();
            storageRef = storage.getReference().child(mAuth.getCurrentUser().getUid()+"/projects/"+recording.getProjectId()+"/recording/"+recording.getTid());
            StorageMetadata metadata = new StorageMetadata.Builder().setContentType(recording.getMime()).build();
            addSessionUpload(UPLOAD_TYPE_PROJECT_RECORDING,recording,file.getAbsolutePath(),recording.getId());
            UploadTask uploadTask = storageRef.putFile(Uri.fromFile(file), metadata);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    taskSnapshot.getStorage().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                        recording.setDownloadURL(task.getResult().toString());
                        removeSessionUpload(recording.getId());
                        mDatabase.child("projects").child(recording.getProjectId()).child("recordings").child(recording.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()){
                                    mDatabase.child("projects").child(recording.getProjectId()).child("recordings").child(recording.getId()).child("downloadURL").setValue(recording.getDownloadURL());

                                    Intent intent = new Intent(ActionEventConstant.PROJECT_RECORDING_UPLOADED);
                                    intent.putExtra(Recording.class.getName(),recording);
                                    sendBroadCast(intent);

                                }
                                else{
                                    storageRef.delete();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                storageRef.delete();
                            }
                        });
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Intent intent = new Intent(ActionEventConstant.PROJECT_RECORDING_UPLOAD_FAILED);
                    intent.putExtra(Recording.class.getName(),recording);
                    sendBroadCast(intent);
                }
            });
        }else{
            addPendingUploads(UPLOAD_TYPE_PROJECT_RECORDING, recording, file.getAbsolutePath());
            /*sendFinishBroadCast(ACTION_UPLOAD_PROJECT_RECORDING,"File saved offline");*/
        }
    }
    
    private void pullCopyToTull(){
        audioFileArrayList = new ArrayList<>();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid()).child("copytotully");
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot nodes : dataSnapshot.getChildren()){
                        AudioFile audioFile = nodes.getValue(AudioFile.class);
                        audioFile.setId(nodes.getKey());
                        audioFileArrayList.add(audioFile);
                    }
                }
                Intent localIntent = new Intent(ACTION_PULL_COPYTOTULLY);
                localIntent.putExtra(DB_PARAM, audioFileArrayList);
                sendBroadCast(localIntent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Intent localIntent = new Intent(ACTION_PULL_COPYTOTULLY);
                sendBroadCast(localIntent);
            }
        });
    }

    private void pullFullProfile(){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Intent localIntent = new Intent(ACTION_PULL_FULL_PROFILE);
                if (dataSnapshot.exists()){
                    FullProfile fullProfile = dataSnapshot.getValue(FullProfile.class);
                    localIntent.putExtra(DB_PARAM, fullProfile);
                    sendBroadCast(localIntent);
                }
                else{
                    sendBroadCast(localIntent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Intent localIntent = new Intent(ACTION_PULL_FULL_PROFILE);
                sendBroadCast(localIntent);
            }
        });
    }

    private void pullProject(String projectID){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("projects").child(projectID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Intent localIntent = new Intent(ACTION_PULL_PROJECT);
                if (dataSnapshot.exists()){
                    Project project = dataSnapshot.getValue(Project.class);
                    localIntent.putExtra(DB_PARAM,project);
                }
                sendBroadCast(localIntent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Intent localIntent = new Intent(ACTION_PULL_PROJECT);
                sendBroadCast(localIntent);
            }
        });
    }

    private void saveRecordings(final Recording recording, final File file){
        final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        final String key = mDatabase.child("no_project").child("recordings").push().getKey();
        mDatabase.child("no_project").child("recordings").child(key).setValue(recording);

        if (isInternetAvailable(getApplication())){
            storage = FirebaseStorage.getInstance();
            storageRef = storage.getReference().child(mAuth.getCurrentUser().getUid()+"/no_project/recording/"+recording.getTid());
            StorageMetadata metadata = new StorageMetadata.Builder().setContentType("audio/x-wav").build();
            addSessionUpload(UPLOAD_TYPE_RECORDING,recording,file.getAbsolutePath(),key);
            UploadTask uploadTask = storageRef.putFile(Uri.fromFile(file), metadata);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                Intent localIntent = new Intent(ACTION_SAVE_RECORDINGS);
                sendBroadCast(localIntent);
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                    recording.setDownloadURL(uri.toString());
                    removeSessionUpload(key);
                    mDatabase.child("no_project").child("recordings").child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Intent localIntent = new Intent(ACTION_SAVE_RECORDINGS);
                            if (dataSnapshot.exists()){
                                mDatabase.child("no_project").child("recordings").child(key).child("downloadURL").setValue(recording.getDownloadURL());
                                recording.setId(key);
                                localIntent.putExtra(RESPONSE_PARAM,recording);
                                sendBroadCast(localIntent);
                            }else{
                                storageRef.delete();
                                sendBroadCast(localIntent);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Intent localIntent = new Intent(ACTION_SAVE_RECORDINGS);
                            sendBroadCast(localIntent);
                        }
                    });
                    }
                });
                }
            });
        }
        else{
            recording.setId(key);
            addPendingUploads(UPLOAD_TYPE_RECORDING, recording, file.getAbsolutePath());
            Intent localIntent = new Intent(ACTION_SAVE_RECORDINGS);
            sendBroadCast(localIntent);
        }
    }

    private void searchNoProjectRecording(final String key){
        final ArrayList<Recording> recordingArrayList = new ArrayList<>();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("no_project").child("recordings").orderByChild("name").startAt(key).endAt(key+getString(R.string.last_chr)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot nodes : dataSnapshot.getChildren()){
                        Recording recordingAppModel = nodes.getValue(Recording.class);
                        recordingAppModel.setOfProject(false);
                        recordingAppModel.setId(nodes.getKey());
                        recordingAppModel.setProjectName(getResources().getString(R.string.no_project_assigned));
                        recordingArrayList.add(recordingAppModel);
                    }
                }
                searchProjectRecording(key, recordingArrayList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                searchProjectRecording(key, recordingArrayList);
            }
        });
    }

    private void searchProjectRecording(final String key, final ArrayList<Recording> recordingArrayList){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("projects").orderByChild("project_name").startAt(key).endAt(key+getString(R.string.last_chr)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Intent localIntent = new Intent(ACTION_SEARCH_RECORDING);
                if (dataSnapshot.exists()){
                    for (DataSnapshot projects : dataSnapshot.getChildren()){
                        String project_main_recording = projects.child("project_main_recording").getValue().toString();

                        for (DataSnapshot nodes : projects.child("recordings").getChildren()){
                            Recording recordingAppModel = nodes.getValue(Recording.class);
                            if (recordingAppModel.getTid().equals(project_main_recording))
                                continue;
                            recordingAppModel.setOfProject(true);
                            recordingAppModel.setId(nodes.getKey());
                            recordingAppModel.setProjectId(projects.getKey());
                            recordingAppModel.setProjectName(projects.child("project_name").getValue().toString());
                            recordingArrayList.add(recordingAppModel);
                        }
                    }
                    localIntent.putExtra(DB_PARAM, recordingArrayList);
                    sendBroadCast(localIntent);
                }else{
                    localIntent.putExtra(DB_PARAM, recordingArrayList);
                    sendBroadCast(localIntent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Intent localIntent = new Intent(ACTION_SEARCH_RECORDING);
                localIntent.putExtra(DB_PARAM, recordingArrayList);
                sendBroadCast(localIntent);
            }
        });
    }

    private void pullProjectsRecording(){
        if (recordingAppModelArrayList!=null) recordingAppModelArrayList.clear();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("projects").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    try{
                        for (DataSnapshot projects : dataSnapshot.getChildren()){
                            String project_main_recording="";
                            if (projects.child("project_main_recording").getValue()!=null){
                                project_main_recording = projects.child("project_main_recording").getValue().toString();
                            }

                            for (DataSnapshot nodes : projects.child("recordings").getChildren()){
                                Recording recordingAppModel = nodes.getValue(Recording.class);
                                if (recordingAppModel.getTid().equals(project_main_recording))
                                    continue;
                                recordingAppModel.setOfProject(true);
                                recordingAppModel.setId(nodes.getKey());
                                recordingAppModel.setProjectId(projects.getKey());
                                recordingAppModel.setProjectName(projects.child("project_name").getValue().toString());
                                recordingAppModelArrayList.add(recordingAppModel);
                            }
                            // Collections.reverse(recordingAppModelArrayList);
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                pullRecordings();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                pullRecordings();
            }
        });
    }

    private void pullRecordings(){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("no_project").child("recordings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Intent localIntent = new Intent(ACTION_PULL_RECORDINGS);
                if (dataSnapshot.exists()){
                    ArrayList<Recording> pr = new ArrayList<>();
                    for (DataSnapshot nodes : dataSnapshot.getChildren()){
                        Recording recordingAppModel = nodes.getValue(Recording.class);
                        recordingAppModel.setOfProject(false);
                        recordingAppModel.setId(nodes.getKey());
                        recordingAppModel.setProjectName(getResources().getString(R.string.no_project_assigned));
                        pr.add(recordingAppModel);
                    }
                    // Collections.reverse(pr);
                    recordingAppModelArrayList.addAll(pr);
                }

                localIntent.putExtra(DB_PARAM,recordingAppModelArrayList);
                sendBroadCast(localIntent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Intent localIntent = new Intent(ACTION_PULL_RECORDINGS);
                localIntent.putExtra(DB_PARAM,recordingAppModelArrayList);
                sendBroadCast(localIntent);
            }
        });
    }

    private void saveLyrics(final LyricsAppModel lyricsAppModel){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        if (lyricsAppModel.isOfProject()){
            mDatabase.child("projects").child(lyricsAppModel.getProjectId()).child("lyrics").child(lyricsAppModel.getLyrics().getId()).child("desc").setValue(lyricsAppModel.getLyrics().getDesc()).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(FirebaseDatabaseOperations.this, "Lyrics saved", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                Toast.makeText(FirebaseDatabaseOperations.this, "Failed to save lyrics", Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            if (lyricsAppModel.getLyrics().getId()==null) {
                lyricsAppModel.getLyrics().setId(mDatabase.push().getKey());
                Intent localIntent = new Intent(ACTION_SAVE_LYRICS);
                localIntent.putExtra(RESPONSE_PARAM,lyricsAppModel.getLyrics().getId());
                sendBroadCast(localIntent);
            }
            mDatabase.child("no_project").child("lyrics").child(lyricsAppModel.getLyrics().getId()).child("desc").setValue(lyricsAppModel.getLyrics().getDesc()).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(FirebaseDatabaseOperations.this, "Lyrics saved", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                Toast.makeText(FirebaseDatabaseOperations.this, "Failed to save lyrics", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void pullLyrics(){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        final ArrayList<LyricsAppModel> lyricsAppModelArrayList = new ArrayList<>();
        //.endAt("-L7pZ55v7bEAx2MmdYZb").limitToLast(11)
        mDatabase.child("no_project").child("lyrics").orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    LyricsAppModel lyricsAppModel;
                    Lyrics lyrics;
                    for (DataSnapshot lyricsSnap : dataSnapshot.getChildren()){
                        lyrics = lyricsSnap.getValue(Lyrics.class);
                        if (lyrics!=null){
                            lyrics.setId(lyricsSnap.getKey());
                            lyricsAppModel = new LyricsAppModel();
                            lyricsAppModel.setLyrics(lyrics);
                            lyricsAppModel.setOfProject(false);
                            lyricsAppModelArrayList.add(lyricsAppModel);
                        }
                    }
                }
                pullProjectLyrics(lyricsAppModelArrayList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                pullProjectLyrics(lyricsAppModelArrayList);
            }
        });
    }

    private void pullProjectLyrics(final ArrayList<LyricsAppModel> lyricsAppModelArrayList){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("projects").orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot projectNode : dataSnapshot.getChildren()){
                        LyricsAppModel lyricsAppModel;
                        Lyrics lyrics;
                        ArrayList<LyricsAppModel> pl = new ArrayList<>();
                        if (projectNode.child("lyrics").exists()){
                            for (DataSnapshot lyricsNode : projectNode.child("lyrics").getChildren()){
                                lyrics = lyricsNode.getValue(Lyrics.class);
                                lyrics.setId(lyricsNode.getKey());

                                lyricsAppModel = new LyricsAppModel();
                                lyricsAppModel.setLyrics(lyrics);
                                lyricsAppModel.setOfProject(true);
                                lyricsAppModel.setProjectName(projectNode.child("project_name").getValue().toString());
                                lyricsAppModel.setProjectId(projectNode.getKey());
                                pl.add(lyricsAppModel);
                            }
                            lyricsAppModelArrayList.addAll(pl);
                        }
                    }
                }
                Intent intent = new Intent(ACTION_PULL_LYRICS);
                intent.putExtra(DB_PARAM,lyricsAppModelArrayList);
                sendBroadCast(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Intent intent = new Intent(ACTION_PULL_LYRICS);
                intent.putExtra(DB_PARAM,lyricsAppModelArrayList);
                sendBroadCast(intent);
            }
        });
    }

    private void searchNoProjectLyrics(final String key){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        final ArrayList<LyricsAppModel> lyricsAppModelArrayList = new ArrayList<>();
        mDatabase.child("no_project").child("lyrics").orderByChild("desc").startAt(key).endAt(key+getString(R.string.last_chr)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    LyricsAppModel lyricsAppModel;
                    Lyrics lyrics;
                    for (DataSnapshot lyricsSnap : dataSnapshot.getChildren()){
                        lyrics = lyricsSnap.getValue(Lyrics.class);
                        lyrics.setId(lyricsSnap.getKey());
                        lyricsAppModel = new LyricsAppModel();
                        lyricsAppModel.setLyrics(lyrics);
                        lyricsAppModel.setOfProject(false);
                        lyricsAppModelArrayList.add(lyricsAppModel);
                    }
                }
                searchProjectLyrics(key,lyricsAppModelArrayList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                searchProjectLyrics(key,lyricsAppModelArrayList);
            }
        });
    }

    private void searchProjectLyrics(final String key, final ArrayList<LyricsAppModel> lyricsAppModelArrayList){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("projects").orderByChild("project_name").startAt(key).endAt(key+getString(R.string.last_chr)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Intent localIntent = new Intent(ACTION_SEARCH_LYRICS);
                if (dataSnapshot.exists()){
                    for (DataSnapshot projectNode : dataSnapshot.getChildren()){
                        LyricsAppModel lyricsAppModel;
                        Lyrics lyrics;

                        if (projectNode.child("lyrics").exists()){
                            for (DataSnapshot lyricsNode : projectNode.child("lyrics").getChildren()){
                                lyrics = lyricsNode.getValue(Lyrics.class);
                                lyrics.setId(lyricsNode.getKey());

                                lyricsAppModel = new LyricsAppModel();
                                lyricsAppModel.setLyrics(lyrics);
                                lyricsAppModel.setOfProject(true);
                                lyricsAppModel.setProjectName(projectNode.child("project_name").getValue().toString());
                                lyricsAppModel.setProjectId(projectNode.getKey());
                                lyricsAppModelArrayList.add(lyricsAppModel);
                            }
                        }
                    }

                }

                localIntent.putExtra(DB_PARAM,lyricsAppModelArrayList);
                sendBroadCast(localIntent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Intent localIntent = new Intent(ACTION_SEARCH_LYRICS);
                localIntent.putExtra(DB_PARAM,lyricsAppModelArrayList);
                sendBroadCast(localIntent);
            }

        });
    }

    private void pullHome(final String action) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.keepSynced(true);
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                Intent localIntent = new Intent(action);
                if (dataSnapshot.exists()){

                    final HomeDb homeDatabase = dataSnapshot.getValue(HomeDb.class);

                    DatabaseReference master = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid() + "/masters");
                    master.keepSynced(true);
                    master.orderByChild("parent_id").equalTo("0").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot masterSnapShot) {
                            TreeMap<String, Masters> masterNodes = new TreeMap<>();

                            if (masterSnapShot.exists()){
                                for(DataSnapshot node : masterSnapShot.getChildren()){
                                    Masters masters = node.getValue(Masters.class);
                                    masters.setId(node.getKey());
                                    masterNodes.put(node.getKey(),masters);
                                }
                            }

                            homeDatabase.setMastersTreeMap(masterNodes);

                            Profile profile = null;
                            Settings settings = null;
                            try{
                                 profile = dataSnapshot.child("profile").getValue(Profile.class);
                                 settings = dataSnapshot.child("settings").getValue(Settings.class);
                                 if (settings.getEngineerAdminAccess()!=null){
                                     Boolean isActive = (Boolean) dataSnapshot.child("settings/engineerAdminAccess/isActive").getValue();
                                     if (isActive!=null) settings.getEngineerAdminAccess().setActive(isActive);
                                 }
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }

                            Intent localIntent = new Intent(action);
                            localIntent.putExtra(DB_PARAM, homeDatabase);
                            localIntent.putExtra(PARAM_PROFILE,profile);
                            localIntent.putExtra(PARAM_SETTINGS,settings);

                            if (homeDatabase.hasAnyFilesOrProjects()){
                                localIntent.putExtra(DB_PULL_STATUS,true);
                                sendBroadCast(localIntent);
                            }
                            else{
                                localIntent.putExtra(DB_PULL_STATUS,false);
                                sendBroadCast(localIntent);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Intent localIntent = new Intent(action);
                            if (homeDatabase.hasAnyFilesOrProjects()){
                                localIntent.putExtra(DB_PULL_STATUS,true);
                                sendBroadCast(localIntent);
                            }
                            else{
                                localIntent.putExtra(DB_PULL_STATUS,false);
                                sendBroadCast(localIntent);
                            }
                        }
                    });
                }
                else{
                    localIntent.putExtra(DB_PARAM, new HomeDb());
                    localIntent.putExtra(DB_PULL_STATUS,false);
                    sendBroadCast(localIntent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Intent localIntent = new Intent(action);
                localIntent.putExtra(DB_PULL_STATUS,false);
                localIntent.putExtra(DB_PARAM, new HomeDb());
                sendBroadCast(localIntent);
            }
        });
    }

    private void pullMasters(final String action, String parent_id) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid() + "/masters");
        mDatabase.keepSynced(true);
        mDatabase.orderByChild("parent_id").equalTo(parent_id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Intent localIntent = new Intent(action);
                if (dataSnapshot.exists()){

                    TreeMap<String, Masters> masterNodes = new TreeMap<>();

                    for(DataSnapshot node : dataSnapshot.getChildren()){
                        Masters masters = node.getValue(Masters.class);
                        masters.setId(node.getKey());
                        masterNodes.put(node.getKey(),masters);
                    }

                    localIntent.putExtra(DB_PARAM, masterNodes);

                }
                else{
                    localIntent.putExtra(DB_PARAM, new TreeMap<String, Masters>());
                }
                sendBroadCast(localIntent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Intent localIntent = new Intent(action);
                localIntent.putExtra(DB_PARAM, new TreeMap<String, Masters>());
                sendBroadCast(localIntent);
            }
        });
    }

    private void searchHomeFiles(final String key){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        final HashMap<String,AudioFile> copyToTullyHashMap = new HashMap<>();
        mDatabase.child("copytotully").orderByChild("title").startAt(key).endAt(key+getString(R.string.last_chr)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot node : dataSnapshot.getChildren()){
                        AudioFile audioFile = node.getValue(AudioFile.class);
                        audioFile.setId(node.getKey());
                        copyToTullyHashMap.put(node.getKey(), audioFile);
                    }
                }

                searchHomeBeats(key,copyToTullyHashMap);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                searchHomeBeats(key,copyToTullyHashMap);
            }
        });
    }

    private void searchHomeBeats(final String key, final HashMap<String, AudioFile> copyToTullyHashMap){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        final HashMap<String,BeatAudio> beatAudioHashMap = new HashMap<>();
        mDatabase.child("beats").orderByChild("title").startAt(key).endAt(key+getString(R.string.last_chr)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot node : dataSnapshot.getChildren()){
                        BeatAudio beatAudio = node.getValue(BeatAudio.class);
                        beatAudio.setId(node.getKey());
                        beatAudioHashMap.put(node.getKey(), beatAudio);
                    }
                }
                searchHomeProjects(key,copyToTullyHashMap, beatAudioHashMap);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                searchHomeProjects(key,copyToTullyHashMap,beatAudioHashMap);
            }
        });
    }

    private void searchHomeProjects(final String key, final HashMap<String, AudioFile> copyToTullyHashMap, final HashMap<String, BeatAudio> beatAudioHashMap){
        final HashMap<String,Project> projectsHashMap = new HashMap<>();
        final HomeDb homeDatabase = new HomeDb();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        mDatabase.child("projects").orderByChild("project_name").startAt(key).endAt(key+getString(R.string.last_chr)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Intent localIntent = new Intent(ACTION_SEARCH_HOME);
                if (dataSnapshot.exists()){
                    for (DataSnapshot node : dataSnapshot.getChildren()){
                        Project project = node.getValue(Project.class);
                        project.setId(node.getKey());
                        projectsHashMap.put(node.getKey(), project);
                    }
                }

                homeDatabase.setCopytotully(copyToTullyHashMap);
                homeDatabase.setProjects(projectsHashMap);
                homeDatabase.setBeats(beatAudioHashMap);

                localIntent.putExtra(DB_PULL_STATUS,true);
                localIntent.putExtra(DB_PARAM, homeDatabase);
                sendBroadCast(localIntent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Intent localIntent = new Intent(ACTION_SEARCH_HOME);
                homeDatabase.setCopytotully(copyToTullyHashMap);
                homeDatabase.setProjects(projectsHashMap);
                homeDatabase.setBeats(beatAudioHashMap);
                localIntent.putExtra(DB_PULL_STATUS,true);
                localIntent.putExtra(DB_PARAM, homeDatabase);
                sendBroadCast(localIntent);
            }

        });
    }

    private void sendBroadCast(Intent intent){
        LocalBroadcastManager.getInstance(FirebaseDatabaseOperations.this).sendBroadcast(intent);
    }

    private void uploadProfilePicture(byte[] data){
        final String uid = mAuth.getCurrentUser().getUid();
        String filename = uid+".jpg";

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference().child("profile_pictures/"+filename);

        UploadTask uploadTask = storageRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(FirebaseDatabaseOperations.this, "Failed uploading image", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(uid).child("profile").child("myimg");
                        mDatabase.setValue(uri.toString());
                        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder().setPhotoUri(uri).build();
                        mAuth.getCurrentUser().updateProfile(profileUpdate);
                    }
                });
            }
        });
    }


    private void addPendingUploads(String uploadType, Object object, String filePath){
        Gson gson = new Gson();
        String recordingString = gson.toJson(object);
        RemainingUpload remainingUpload = new RemainingUpload(0, uploadType, filePath, recordingString);
        DataManager.AddPendingUpload(getApplicationContext(),remainingUpload);
    }

    private void addSessionUpload(String uploadType, Object object, String filePath, String key){
        Gson gson = new Gson();
        String recordingString = gson.toJson(object);
        RemainingUpload remainingUpload = new RemainingUpload(0, uploadType, filePath, recordingString);
        remainingUpload.setKey(key);
        DataManager.AddSessionUpload(getApplicationContext(),remainingUpload);
    }

    private void removeSessionUpload(String key){
        DataManager.deleteSessionUpload(getApplicationContext(),key);
    }
}
