package com.tullyapp.tully.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.FirebaseDataModels.BeatAudio;
import com.tullyapp.tully.FirebaseDataModels.LyricsModule.LyricsAppModel;
import com.tullyapp.tully.FirebaseDataModels.Masters;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_HOME;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_LYRICS;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_RECORDINGS;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_MASTER;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_COPYTOTULLY;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_MASTERS;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_RECORDINGS;
import static com.tullyapp.tully.Utils.Utils.getDirectory;


public class DeleteProjects extends IntentService {

    private static final String PARAM_REACTION = "com.tullyapp.tully.reaction";
    public static final String ACTION_DELETE_SINGLEPROJECT_RECORDINGS = "com.tullyapp.tully.SINGLEPROJECT_RECORDINGS";
    private static final String TAG = DeleteProjects.class.getSimpleName();
    private FirebaseAuth mAuth;

    public static final String ACTION_DELETE_PROJECTS = "com.tullyapp.tully.Services.action.DELETE_PROJECTS";
    public static final String ACTION_DELETE_COPYTOTULLY = "com.tullyapp.tully.Services.action.COPYTOTULLY";
    public static final String ACTION_DELETE_BEAT = "com.tullyapp.tully.Services.action.BEAT";
    public static final String ACTION_DELETE_LYRICS = "com.tullyapp.tully.Services.action.DELETE_LYRICS";
    public static final String ACTION_DELETE_RECORDINGS = "com.tullyapp.tully.Services.action.DELETE_RECORDINGS";
    public static final String ACTION_DELETE_MASTERS = "com.tullyapp.tully.Services.action.DELETE_MASTERS";
    public static final String ACTION_AUDIO_DELETED = "com.tullyapp.tully.Services.action.ACTION_AUDIO_DELETED";

    private static final String PARAM_LIST = "com.tullyapp.tully.Services.extra.LIST";
    private File localdir_recordings;
    private File localdir_copytotully;

    public DeleteProjects() {
        super("DeleteProjects");
    }

    public static void startActionDeleteProjects(Context context, ArrayList<Project> projects) {
        Intent intent = new Intent(context, DeleteProjects.class);
        intent.setAction(ACTION_DELETE_PROJECTS);
        intent.putExtra(PARAM_LIST, projects);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startActionDeleteCopyToTully(Context context, ArrayList<AudioFile> copyToTullies, String reaction){
        Intent intent = new Intent(context, DeleteProjects.class);
        intent.setAction(ACTION_DELETE_COPYTOTULLY);
        intent.putExtra(PARAM_LIST, copyToTullies);
        intent.putExtra(PARAM_REACTION, reaction);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startActionDeleteBeat(Context context, ArrayList<BeatAudio> beatAudios, String reaction){
        Intent intent = new Intent(context, DeleteProjects.class);
        intent.setAction(ACTION_DELETE_BEAT);
        intent.putExtra(PARAM_LIST, beatAudios);
        intent.putExtra(PARAM_REACTION, reaction);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startActionDeleteLyrics(Context context, ArrayList<LyricsAppModel> lyricsAppModels){
        Intent intent = new Intent(context, DeleteProjects.class);
        intent.setAction(ACTION_DELETE_LYRICS);
        intent.putExtra(PARAM_LIST, lyricsAppModels);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void startActionDeleteRecordings(Context context, ArrayList<Recording> recordingAppModels){
        Intent intent = new Intent(context, DeleteProjects.class);
        intent.setAction(ACTION_DELETE_RECORDINGS);
        intent.putExtra(PARAM_LIST, recordingAppModels);
        context.startService(intent);
    }

    public static void startActionDeleteSingleProjectRecording(Context context, ArrayList<Recording> recordings){
        Intent intent = new Intent(context, DeleteProjects.class);
        intent.setAction(ACTION_DELETE_SINGLEPROJECT_RECORDINGS);
        intent.putExtra(PARAM_LIST, recordings);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void deleteMasters(Context context, Masters masters, String action){
        Intent intent = new Intent(context,DeleteProjects.class);
        intent.setAction(ACTION_DELETE_MASTERS);
        intent.putExtra(INTENT_PARAM_MASTER,masters);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "deleting project",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }
        mAuth = FirebaseAuth.getInstance();
        if (intent != null && mAuth.getCurrentUser()!=null) {

            switch (intent.getAction()){
                case ACTION_DELETE_PROJECTS:
                    handleActionDeleteProjects((ArrayList<Project>) intent.getSerializableExtra(PARAM_LIST));
                    break;

                case ACTION_DELETE_COPYTOTULLY:
                    handleActionDeleteCopyToTully((ArrayList<AudioFile>) intent.getSerializableExtra(PARAM_LIST), intent.getStringExtra(PARAM_REACTION));
                    break;

                case ACTION_DELETE_BEAT:
                    handleActionDeleteBeat((ArrayList<BeatAudio>) intent.getSerializableExtra(PARAM_LIST), intent.getStringExtra(PARAM_REACTION));
                    break;

                case ACTION_DELETE_LYRICS:
                    handleActionDeleteLyrics((ArrayList<LyricsAppModel>) intent.getSerializableExtra(PARAM_LIST));
                    break;

                case ACTION_DELETE_RECORDINGS:
                    handleActionDeleteRecordings((ArrayList<Recording>) intent.getSerializableExtra(PARAM_LIST));
                    break;

                case ACTION_DELETE_SINGLEPROJECT_RECORDINGS:
                    handleSingleProjectRecording((ArrayList<Recording>) intent.getSerializableExtra(PARAM_LIST));
                    break;

                case ACTION_DELETE_MASTERS:
                    deleteMasters(ACTION_DELETE_MASTERS, (Masters) intent.getSerializableExtra(INTENT_PARAM_MASTER));
                    break;
            }
        }
    }


    private void updateParentCount(Masters node){
        final String parentID = node.getParent_id();
        final DatabaseReference userNode = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        userNode.child("masters").orderByChild("parent_id").equalTo(parentID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    long c = dataSnapshot.getChildrenCount();
                     userNode.child("masters/"+parentID).child("count").setValue(c);
                }
                else{
                    userNode.child("masters/"+parentID).child("count").setValue(0);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void deleteMasters(final String action, final Masters node){
        final DatabaseReference userNode = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        if (node.getType().equals("folder")){
            userNode.child("masters").orderByChild("parent_id").equalTo(node.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()){
                        Map<String, Object> childUpdates = new HashMap<>();
                        for (DataSnapshot masterNode : dataSnapshot.getChildren()){
                            Masters master = masterNode.getValue(Masters.class);
                            master.setId(masterNode.getKey());
                            childUpdates.put(masterNode.getKey(), null);
                            if (master.getType().equals("file")){
                                deleteMasterFile(master);
                            }
                        }
                        dataSnapshot.getRef().child(node.getId()).removeValue();
                    }
                    else{
                        dataSnapshot.getRef().child(node.getId()).removeValue();
                    }
                    sendFinishBroadCast(ACTION_PULL_HOME,"Master Files Deleted");
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else{
            deleteMasterFile(node);
            if (node.getParent_id()!=null && !node.getParent_id().equals("0")) updateParentCount(node);
            sendFinishBroadCast(ACTION_PULL_HOME,"Master Files Deleted");
        }
    }

    private void deleteMasterFile(Masters node){
        DatabaseReference mdb = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid() + "/masters");
        mdb.child(node.getId()).removeValue();
        File localdir_masters = getDirectory(getApplicationContext(), LOCAL_DIR_NAME_MASTERS);
        File localFile = new File(localdir_masters, node.getFilename());
        if (localFile.exists()) localFile.delete();
    }


    private void handleSingleProjectRecording(ArrayList<Recording> recordings){
        localdir_recordings = getDirectory(getApplicationContext(),LOCAL_DIR_NAME_RECORDINGS);
        recursiveDeleteSingleProjectRecordings(recordings,0);
    }

    private void recursiveDeleteSingleProjectRecordings(final ArrayList<Recording> recordingAppModels, int index){
        try{
            if (index>=recordingAppModels.size()){
                sendFinishBroadCastDirect(ACTION_DELETE_SINGLEPROJECT_RECORDINGS,"Recording deleted");
            }
            else{
                Recording recordingAppModel = recordingAppModels.get(index);
                File localFile = new File(localdir_recordings, recordingAppModel.getTid());
                if (localFile.exists()) localFile.delete();

                final String uid = mAuth.getCurrentUser().getUid();
                DatabaseReference userNode = FirebaseDatabase.getInstance().getReference().child(uid);

                userNode.child("projects").child(recordingAppModel.getProjectId()).child("recordings").child(recordingAppModel.getId()).removeValue();
                recursiveDeleteSingleProjectRecordings(recordingAppModels,index + 1);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void handleActionDeleteRecordings(ArrayList<Recording> recordingAppModels){
        localdir_recordings = getDirectory(getApplicationContext(),LOCAL_DIR_NAME_RECORDINGS);
        recursiveDeleteRecordings(recordingAppModels,0);
    }

    private void recursiveDeleteRecordings(final ArrayList<Recording> recordingAppModels, int index){
        if (index>=recordingAppModels.size()){
            sendFinishBroadCast(ACTION_PULL_RECORDINGS,"Recording deleted");
        }
        else{
            Recording recordingAppModel = recordingAppModels.get(index);
            File localFile = new File(localdir_recordings, recordingAppModel.getTid());

            if (localFile.exists()) localFile.delete();
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
            if (recordingAppModel.isOfProject()){
                mDatabase.child("projects").child(recordingAppModel.getProjectId()).child("recordings").child(recordingAppModel.getId()).removeValue();
                recursiveDeleteRecordings(recordingAppModels,index + 1);
            }
            else{
                mDatabase.child("no_project").child("recordings").child(recordingAppModel.getId()).removeValue();
                recursiveDeleteRecordings(recordingAppModels,index + 1);
            }
        }
    }

    private void handleActionDeleteLyrics(ArrayList<LyricsAppModel> lyricsAppModels){
        recursiveDeleteLyrics(lyricsAppModels,0);
    }

    private void recursiveDeleteLyrics(final ArrayList<LyricsAppModel> lyricsAppModels, int index){
        if (index>=lyricsAppModels.size()){
            sendFinishBroadCast(ACTION_PULL_LYRICS,"Lyrics deleted");
        }
        else{
            LyricsAppModel lyricsAppModel = lyricsAppModels.get(index);
            final String uid = mAuth.getCurrentUser().getUid();
            final DatabaseReference userNode = FirebaseDatabase.getInstance().getReference().child(uid);
            if (lyricsAppModel.isOfProject()){
                userNode.child("projects").child(lyricsAppModel.getProjectId()).child("lyrics").child(lyricsAppModel.getLyrics().getId()).removeValue();
                recursiveDeleteLyrics(lyricsAppModels,index + 1);
            }
            else{
                userNode.child("no_project").child("lyrics").child(lyricsAppModel.getLyrics().getId()).removeValue();
                recursiveDeleteLyrics(lyricsAppModels,index + 1);
            }
        }
    }

    private void handleActionDeleteBeat(ArrayList<BeatAudio> beatAudios, String reaction){
        localdir_copytotully = getDirectory(getApplicationContext(),LOCAL_DIR_NAME_COPYTOTULLY);
        recursiveDeleteBeats(beatAudios, reaction, 0);
    }

    private void handleActionDeleteCopyToTully(ArrayList<AudioFile> copyToTullies, String reaction){
        localdir_copytotully = getDirectory(getApplicationContext(),LOCAL_DIR_NAME_COPYTOTULLY);
        recursiveDeleteCopyToTully(copyToTullies, reaction, 0);
    }

    private void recursiveDeleteBeats(final ArrayList<BeatAudio> beatAudios, final String reaction, int index){
        if (index>=beatAudios.size()){
            if (reaction.equals(ACTION_PULL_HOME)){
                sendFinishBroadCast(reaction,"");
            }
            else{
                sendFinishBroadCastDirect(reaction,"Audio Files deleted");
            }
        }
        else{
            AudioFile audioFile = beatAudios.get(index);
            final String uid = mAuth.getCurrentUser().getUid();
            final DatabaseReference userNode = FirebaseDatabase.getInstance().getReference().child(uid);
            String path = uid+"/beats/"+ audioFile.getFilename();
            try{
                File localFile = new File(localdir_copytotully, audioFile.getFilename());
                if (localFile.exists()){
                    localFile.delete();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            userNode.child("beats").child(audioFile.getId()).removeValue();
            recursiveDeleteBeats(beatAudios,reaction,index + 1);
        }
    }

    private void recursiveDeleteCopyToTully(final ArrayList<AudioFile> copyToTullies, final String reaction, int index){
        if (index>=copyToTullies.size()){
            if (reaction.equals(ACTION_PULL_HOME)){
                sendFinishBroadCast(reaction,"");
            }
            else{
                sendFinishBroadCastDirect(reaction,"Audio Files deleted");
            }
        }
        else{
            final String uid = mAuth.getCurrentUser().getUid();
            final DatabaseReference userNode = FirebaseDatabase.getInstance().getReference().child(uid);
            AudioFile audioFile = copyToTullies.get(index);
            try{
                File localFile = new File(localdir_copytotully, audioFile.getFilename());
                if (localFile.exists()) localFile.delete();
            }catch (Exception e){
                e.printStackTrace();
            }

            userNode.child("copytotully").child(audioFile.getId()).removeValue();
            recursiveDeleteCopyToTully(copyToTullies,reaction,index + 1);
        }
    }

    private void handleActionDeleteProjects(ArrayList<Project> projects) {
        localdir_recordings = getDirectory(getApplicationContext(),LOCAL_DIR_NAME_RECORDINGS);
        recursiveDeleteProjects(projects,0);
    }

    private void recursiveDeleteProjects(final ArrayList<Project> projects, int index){
        if (index>= projects.size()) {
            sendFinishBroadCast(ACTION_PULL_HOME,"Project were deleted");
        }
        else{
            HashMap<String, Recording> recordingses;
            Project project = projects.get(index);
            recordingses = project.getRecordings();
            final String uid = mAuth.getCurrentUser().getUid();
            final DatabaseReference userNode = FirebaseDatabase.getInstance().getReference().child(uid);
            if (recordingses!=null){
                Iterator<Map.Entry<String, Recording>> it = recordingses.entrySet().iterator();
                while (it.hasNext()) {

                    Map.Entry<String, Recording> pair = it.next();
                    Recording recording = pair.getValue();

                    try{
                        File localFile = new File(localdir_recordings, recording.getTid());
                        if (localFile.exists()) localFile.delete();
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    it.remove(); // avoids a ConcurrentModificationException
                }
            }

            if (project.getId()!=null){
                userNode.child("projects").child(project.getId()).removeValue();
            }

            recursiveDeleteProjects(projects,index + 1);
        }
    }

    private void sendFinishBroadCast(String ACTION, String message){
        //Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        FirebaseDatabaseOperations.startAction(getApplicationContext(),ACTION);
    }

    private void sendFinishBroadCastDirect(String ACTION, String msg){
        Intent localIntent = new Intent(ACTION);
        LocalBroadcastManager.getInstance(DeleteProjects.this).sendBroadcast(localIntent);
    }

}
