package com.tullyapp.tully.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.Models.RemainingUpload;
import com.tullyapp.tully.R;
import com.tullyapp.tully.SLDB.DataManager;
import com.tullyapp.tully.Utils.ActionEventConstant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_PULL_HOME;
import static com.tullyapp.tully.Utils.Constants.FIREBASE_NODE_COPYTOTULLY;
import static com.tullyapp.tully.Utils.Constants.LOCAL_DIR_NAME_COPYTOTULLY;
import static com.tullyapp.tully.Utils.Constants.UPLOAD_TYPE_COPYTOTULLY;
import static com.tullyapp.tully.Utils.Constants.YOUR_PROJECT_TOKEN;
import static com.tullyapp.tully.Utils.Utils.getContentName;
import static com.tullyapp.tully.Utils.Utils.getDirectory;
import static com.tullyapp.tully.Utils.Utils.isInternetAvailable;

/**
 * Created by macbookpro on 08/09/17.
 */

public class ReceivedCopyToTully extends IntentService {


    public static final String ACTION_RECEIVED_COPY_TO_TULLY = "com.tullyapp.tully.Services.action.RECEIVED_COPY_TO_TULLY";;
    private static final String DATAINTENT = "com.tullyapp.tully.dataintent";
    private static final String TAG = ReceivedCopyToTully.class.getSimpleName();

    private File localdir_copytotully;

    public ReceivedCopyToTully() {
        super("ReceivedCopyToTully");
    }

    public static void startReceivingCopyToTully(Context context, Intent dataIntent){
        Intent intent = new Intent(context, ReceivedCopyToTully.class);
        intent.setAction(ACTION_RECEIVED_COPY_TO_TULLY);
        intent.putExtra(DATAINTENT,dataIntent);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Copying audio file",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }

        Intent dataIntent = intent.getParcelableExtra(DATAINTENT);
        Uri u;
        if (Intent.ACTION_SEND.equals(dataIntent.getAction())){
            u = dataIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        else {
            u = dataIntent.getData();
        }
        
        String scheme = u.getScheme();

        localdir_copytotully = getDirectory(getApplicationContext(),LOCAL_DIR_NAME_COPYTOTULLY);

        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            try {
                String contentName = getContentName(getContentResolver(), u);
                if (contentName!=null){
                    receivedContent(contentName, getContentResolver().openInputStream(u));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            /*
                Log.e("u.getPath()",u.getPath());
                Log.e("u.getEncodedPath()",u.getEncodedPath());
            */
            receivedFromLocal(u.getEncodedPath(), u.getLastPathSegment());
        }
    }


    private void receivedContent(String filetitle, InputStream is){
        try {
            String extension = filetitle.substring(filetitle.lastIndexOf('.'));
            filetitle = filetitle.substring(0,filetitle.lastIndexOf('.'));

            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());

            final String key = mDatabase.child(FIREBASE_NODE_COPYTOTULLY).push().getKey();
            String filename = key+extension;

            File localFile = new File(localdir_copytotully, filename);

            byte[] buffer = new byte[1024];
            OutputStream mOutput = new FileOutputStream(localFile);

            int mLength;
            while ((mLength = is.read(buffer))>0) {
                mOutput.write(buffer, 0, mLength);
            }
            mOutput.flush();
            mOutput.close();
            is.close();

            final AudioFile audioFile = new AudioFile();
            audioFile.setFilename(filename);
            audioFile.setTitle(filetitle);

            uploadToFirebaseStorage(filename,key,localFile, audioFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (StringIndexOutOfBoundsException e){
            Toast.makeText(getApplicationContext(), "File Extension not found", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void receivedFromLocal(String path, String filetitle){

        File file = new File(path);

        if (file.exists()){
            try {

                String extension = path.substring(path.lastIndexOf('.'));

                filetitle = filetitle.substring(0,filetitle.lastIndexOf('.'));

                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());

                final String key = mDatabase.child(FIREBASE_NODE_COPYTOTULLY).push().getKey();
                String filename = key+extension;

                final AudioFile audioFile = new AudioFile();
                audioFile.setFilename(filename);
                audioFile.setTitle(filetitle);

                File localFile = new File(localdir_copytotully, filename);

                FileChannel inChannel = new FileInputStream(file).getChannel();
                FileChannel outChannel = new FileOutputStream(localFile).getChannel();
                try {
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                    Log.e("NEW FILE",localFile.getPath()+" : "+localFile.length()+"");
                } finally {
                    if (inChannel != null)
                        inChannel.close();
                    outChannel.close();
                }
                uploadToFirebaseStorage(filename,key,localFile, audioFile);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }
            catch (StringIndexOutOfBoundsException e){
                Toast.makeText(getApplicationContext(), "File Extension not found", Toast.LENGTH_SHORT).show();
            }
            catch (Exception e){
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Unable to import", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Log.e("Err","File does not exist");
        }
    }

    private void uploadToFirebaseStorage(final String filename, final String key, final File file, final AudioFile audioFile){

        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());

        audioFile.setSize(file.length());
        Task<Void> task = mDatabase.child(FIREBASE_NODE_COPYTOTULLY).child(key).setValue(audioFile);

        MixpanelAPI mixpanel = MixpanelAPI.getInstance(getApplicationContext(), YOUR_PROJECT_TOKEN);
        //mixpanel.track("Production into Tully");
        mixpanel.track(" Production Imported in Android");

        if (isInternetAvailable(getApplication())){
            task.addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                sendFinishBroadCast(ACTION_RECEIVED_COPY_TO_TULLY,"File is added and queued for upload");
                try {
                    audioFile.setId(key);
                    addSessionUpload(UPLOAD_TYPE_COPYTOTULLY, audioFile, file.getAbsolutePath(),key);
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    final StorageReference storageRef = storage.getReference().child(mAuth.getCurrentUser().getUid() + "/copytoTully/" + filename);
                    InputStream stream = new FileInputStream(file);
                    UploadTask uploadTask = storageRef.putStream(stream);
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                            removeSessionUpload(key);
                            taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    audioFile.setDownloadURL(uri.toString());
                                    DatabaseReference node = mDatabase.child(FIREBASE_NODE_COPYTOTULLY).child(key);
                                    node.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists()){
                                                mDatabase.child(FIREBASE_NODE_COPYTOTULLY).child(key).child("downloadURL").setValue(audioFile.getDownloadURL());
                                                Intent intent = new Intent(ActionEventConstant.AUDIO_FILE_UPLOADED);
                                                intent.putExtra(AudioFile.class.getName(),audioFile);
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
                    });
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    sendFinishBroadCast(ACTION_RECEIVED_COPY_TO_TULLY,"Failed saving file locally");
                }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    audioFile.setId(key);
                    addPendingUploads(UPLOAD_TYPE_COPYTOTULLY, audioFile, file.getAbsolutePath());
                    sendFinishBroadCast(ACTION_RECEIVED_COPY_TO_TULLY,"File failed to upload");
                }
            });
        }
        else{
            audioFile.setId(key);
            addPendingUploads(UPLOAD_TYPE_COPYTOTULLY, audioFile, file.getAbsolutePath());
            sendFinishBroadCast(ACTION_RECEIVED_COPY_TO_TULLY,"File Queued");
        }
    }

    private void sendBroadCast(Intent intent){
        LocalBroadcastManager.getInstance(ReceivedCopyToTully.this).sendBroadcast(intent);
    }

    private void sendFinishBroadCast(String ACTION, String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        FirebaseDatabaseOperations.startAction(getApplicationContext(), ACTION_PULL_HOME);
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
