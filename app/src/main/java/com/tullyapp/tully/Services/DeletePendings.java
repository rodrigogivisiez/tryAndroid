package com.tullyapp.tully.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tullyapp.tully.Models.StorageDelete;
import com.tullyapp.tully.R;
import com.tullyapp.tully.SLDB.DataManager;

import java.util.ArrayList;

public class DeletePendings extends IntentService {

    private static final String TAG = DeletePendings.class.getSimpleName();
    public static boolean isRunning = false;
    private ArrayList<StorageDelete> pendingStorageArrayList;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;

    public DeletePendings() {
        super("DeletePendings");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startAction(Context context) {
        isRunning = true;
        Intent intent = new Intent(context, DeletePendings.class);
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
                    "checking pending deletions",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser()!=null && intent!=null){
            new DeletePendingStorage().execute();
        }
        else{
            isRunning = false;
        }
    }

    private class DeletePendingStorage extends AsyncTask<Void, Void, Boolean>{
        @Override
        protected Boolean doInBackground(Void... voids) {
            pendingStorageArrayList = DataManager.loadStorageDeletes(getApplicationContext());
            storageRef = FirebaseStorage.getInstance().getReference();
            return recursiveDeletePendingStorage();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            isRunning = false;
        }
    }


    private boolean recursiveDeletePendingStorage(){
        if (pendingStorageArrayList.size()>0){

            StorageDelete pendingDeleteStorage = pendingStorageArrayList.get(0);

            Log.e(TAG,pendingDeleteStorage.getStorage_path());

            storageRef.child(pendingDeleteStorage.getStorage_path()).delete();

            pendingStorageArrayList.remove(0);
            DataManager.deletePendingStorageDelete(getApplicationContext(),pendingDeleteStorage.get_id());

            recursiveDeletePendingStorage();
        }

        return true;
    }


}
