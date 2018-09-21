package com.tullyapp.tully;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;

import io.intercom.android.sdk.Intercom;
import static com.tullyapp.tully.FCM.MyFirebaseMessagingService.TULLY_APP;

/**
 * Created by macbookpro on 25/10/17.
 */

public class App extends Application {
    private static final String TAG = App.class.getSimpleName() ;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG,"ENTER Application onCreate");
        Intercom.initialize(this, getString(R.string.intercom_app_key), getString(R.string.intercom_app_id));
        //FirebaseApp.initializeApp(this);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        createNotificationChannel();
        FirebaseDatabaseOperations.setConnectionState(getApplicationContext());
    }

    protected void attachBaseContext(Context base) {
        Log.e(TAG,"BEFORE attachBaseContext");
        super.attachBaseContext(base);
        MultiDex.install(this);
        Log.e(TAG,"AFTER attachBaseContext");
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        try{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = TULLY_APP;
                String description = "Notifications from Tully";
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel channel = new NotificationChannel(TULLY_APP, name, importance);
                channel.setDescription(description);
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager!=null) notificationManager.createNotificationChannel(channel);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    // Intent restartService = new Intent(getApplicationContext(), MyAppFirebaseMessagingService.class);
    // PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(),1,restartService,PendingIntent.FLAG_ONE_SHOT);
    // AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    // alarmManager.set(AlarmManager.ELAPSED_REALTIME,5000,pendingIntent);
}