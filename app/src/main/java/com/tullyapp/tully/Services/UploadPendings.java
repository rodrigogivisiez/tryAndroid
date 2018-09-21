package com.tullyapp.tully.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.Models.RemainingUpload;
import com.tullyapp.tully.R;
import com.tullyapp.tully.SLDB.DataManager;
import com.tullyapp.tully.Utils.ActionEventConstant;

import java.io.File;
import java.util.ArrayList;

import static com.tullyapp.tully.Utils.Constants.FIREBASE_NODE_COPYTOTULLY;
import static com.tullyapp.tully.Utils.Constants.UPLOAD_LIST_TYPE_OFFLINED;
import static com.tullyapp.tully.Utils.Constants.UPLOAD_LIST_TYPE_SESSION;
import static com.tullyapp.tully.Utils.Constants.UPLOAD_TYPE_COPYTOTULLY;
import static com.tullyapp.tully.Utils.Constants.UPLOAD_TYPE_PROJECT_RECORDING;
import static com.tullyapp.tully.Utils.Constants.UPLOAD_TYPE_RECORDING;
import static com.tullyapp.tully.Utils.Utils.isInternetAvailable;

public class UploadPendings extends IntentService {

    private static final String TAG = UploadPendings.class.getSimpleName();
    private static ArrayList<RemainingUpload> pendingUploads;
    private static ArrayList<RemainingUpload> sessionUploads;

    public UploadPendings() {
        super("UploadPendings");
    }
    public static boolean isRunning = false;

    public static void startAction(Context context) {
        isRunning = true;
        Intent intent = new Intent(context, UploadPendings.class);
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
                    "checking pending uploads",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (intent != null && mAuth.getCurrentUser()!=null) {
            processPending();
        }
    }


    private void processPending(){
        new PendingUploadFetch().execute();
    }

    private class PendingUploadFetch extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {
            pendingUploads =  DataManager.loadPendingUploadsList(getApplicationContext());
            sessionUploads = DataManager.loadSessionUploads(getApplicationContext());
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference(mAuth.getCurrentUser().getUid());

            if (pendingUploads.size()>0 || (storageRef.getActiveUploadTasks().size()==0 && sessionUploads.size()>0)){
                Log.e(TAG,"UPLOAD-STARTING");
                upload();
            }
            else{

                Log.e(TAG,"NO RECORDS");
                isRunning = false;

                Log.e(TAG,"onPostExecute-FINISH");
            }

            return true;
        }
    }

    private void upload(){
        if (isInternetAvailable(getApplication())){
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference(mAuth.getCurrentUser().getUid());
            if (storageRef.getActiveUploadTasks().size()==0 && sessionUploads.size()>0){
                Log.e(TAG,"SESSION REMAINING : "+sessionUploads.size());
                RemainingUpload ru = sessionUploads.get(0);
                switch (ru.getUpload_type()){
                    case UPLOAD_TYPE_RECORDING:
                        uploadRecording(ru,UPLOAD_LIST_TYPE_SESSION);
                        break;

                    case UPLOAD_TYPE_PROJECT_RECORDING:
                        uploadProjectRecording(ru,UPLOAD_LIST_TYPE_SESSION);
                        break;

                    case UPLOAD_TYPE_COPYTOTULLY:
                        uploadCopyToTully(ru,UPLOAD_LIST_TYPE_SESSION);
                        break;
                }

            }
            else if (pendingUploads.size()>0){

                Log.e(TAG,"OFFLINED REMAINING : "+pendingUploads.size());
                RemainingUpload ru = pendingUploads.get(0);
                switch (ru.getUpload_type()){
                    case UPLOAD_TYPE_RECORDING:
                        uploadRecording(ru,UPLOAD_LIST_TYPE_OFFLINED);
                        break;

                    case UPLOAD_TYPE_PROJECT_RECORDING:
                        uploadProjectRecording(ru,UPLOAD_LIST_TYPE_OFFLINED);
                        break;

                    case UPLOAD_TYPE_COPYTOTULLY:
                        uploadCopyToTully(ru,UPLOAD_LIST_TYPE_OFFLINED);
                        break;
                }
            }
            else{
                Log.e(TAG,"FINISH-LIST");
                isRunning = false;
                Log.e(TAG,"onPostExecute-FINISH");
            }
        }
        else{
            Log.e(TAG,"FINISH-LIST");
            isRunning = false;
            Log.e(TAG,"onPostExecute-FINISH");
        }
    }

    private void uploadProjectRecording(final RemainingUpload ru, final String uploadListType){
        File file = new File(ru.getFile_path());
        if (file.exists()) {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseStorage storage = FirebaseStorage.getInstance();
            final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
            StorageReference storageRef = storage.getReference(mAuth.getCurrentUser().getUid());

            Gson gson = new Gson();
            final Recording recording = gson.fromJson(ru.getData(), Recording.class);
            final StorageReference sRef = storageRef.child("projects/" + recording.getProjectId() + "/recording/" + recording.getTid());
            StorageMetadata metadata = new StorageMetadata.Builder().setContentType(recording.getMime()).build();

            UploadTask uploadTask = sRef.putFile(Uri.fromFile(file), metadata);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.e("Error Upload file",exception.getMessage());
                    upload();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    deleteFromList(ru,uploadListType);

                    taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                    recording.setDownloadURL(uri.toString());
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
                                sRef.delete();
                            }

                            upload();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            sRef.delete();
                            upload();
                        }
                    });
                        }
                    });
                }
            });
        }
        else{
            deleteFromList(ru,uploadListType);
            upload();
        }
    }

    private void uploadCopyToTully(final RemainingUpload ru, final String uploadListType){
        File file = new File(ru.getFile_path());
        if (file.exists()){
            Gson gson = new Gson();
            final AudioFile audioFile = gson.fromJson(ru.getData(), AudioFile.class);
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseStorage storage = FirebaseStorage.getInstance();
            final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
            StorageReference storageRef = storage.getReference(mAuth.getCurrentUser().getUid());

            final StorageReference sRef = storageRef.child("copytoTully/" + audioFile.getFilename());

            UploadTask uploadTask = sRef.putFile(Uri.fromFile(file));
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.e("Error Upload file",exception.getMessage());
                    upload();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    deleteFromList(ru,uploadListType);
                    taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            audioFile.setDownloadURL(uri.toString());
                            mDatabase.child(FIREBASE_NODE_COPYTOTULLY).child(audioFile.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()){
                                        mDatabase.child(FIREBASE_NODE_COPYTOTULLY).child(audioFile.getId()).child("downloadURL").setValue(audioFile.getDownloadURL());

                                        Intent intent = new Intent(ActionEventConstant.AUDIO_FILE_UPLOADED);
                                        intent.putExtra(AudioFile.class.getName(),audioFile);
                                        sendBroadCast(intent);

                                    }else{
                                        sRef.delete();
                                    }

                                    upload();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    sRef.delete();
                                    upload();
                                }
                            });
                        }
                    });
                }
            });
        }
        else{
            deleteFromList(ru,uploadListType);
            upload();
        }
    }

    private void uploadRecording(final RemainingUpload ru, final String uploadListType){

        File file = new File(ru.getFile_path());

        if (file.exists()){
            Gson gson = new Gson();
            final Recording recording = gson.fromJson(ru.getData(), Recording.class);

            if (recording.getId()==null){
                deleteFromList(ru,uploadListType);
                upload();
            }else{
                if (recording.getTid()!=null){
                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
                    StorageReference storageRef = storage.getReference(mAuth.getCurrentUser().getUid());

                    final StorageReference sRef = storageRef.child("no_project/recording/" + recording.getTid());
                    StorageMetadata metadata = new StorageMetadata.Builder().setContentType("audio/x-wav").build();
                    deleteFromList(ru,uploadListType);
                    UploadTask uploadTask = sRef.putFile(Uri.fromFile(file), metadata);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            Log.e("Error Upload file",exception.getMessage());
                            upload();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                recording.setDownloadURL(uri.toString());
                                mDatabase.child("no_project").child("recordings").child(recording.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()){
                                            mDatabase.child("no_project").child("recordings").child(recording.getId()).child("downloadURL").setValue(recording.getDownloadURL());
                                        }else{
                                            sRef.delete();
                                        }
                                        upload();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        sRef.delete();
                                        upload();
                                    }
                                });
                            }
                        });
                        }
                    });
                }
            }
        }
        else {
            deleteFromList(ru,uploadListType);
            upload();
        }

    }

    private void deleteFromList(RemainingUpload ru, String uploadListType){
        if (uploadListType.equals(UPLOAD_LIST_TYPE_SESSION)){
            sessionUploads.remove(0);
            DataManager.deleteSessionUpload(getApplicationContext(),ru.get_id());
        }else{
            pendingUploads.remove(0);
            DataManager.deletePendingUpload(getApplicationContext(),ru.get_id());
        }
    }


    private void sendBroadCast(Intent intent){
        LocalBroadcastManager.getInstance(UploadPendings.this).sendBroadcast(intent);
    }

}
