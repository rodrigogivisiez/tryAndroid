package com.tullyapp.tully.FCM;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.tullyapp.tully.Collaboration.AcceptInvitationActivity;
import com.tullyapp.tully.R;

import java.util.Date;
import java.util.Map;

import io.intercom.android.sdk.push.IntercomPushClient;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();
    public static final String TULLY_APP = "TULLY_APP";
    private final IntercomPushClient intercomPushClient = new IntercomPushClient();

    public MyFirebaseMessagingService() {
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.e(TAG,"TOKEN : "+token);
        registerAppUser(token);
    }

    public void registerAppUser(String refreshedToken){
        intercomPushClient.sendTokenToIntercom(getApplication(), refreshedToken);
    }

    public void startActivity(String className, Bundle extras, Context context) {
        Class cls = null;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException e) {
            //means you made a wrong input in firebase console
        }
        Intent intent = new Intent(context, cls);
        if (null != extras) {
            intent.putExtras(extras);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        NotificationCompat.Builder notificationBuilder;

        Map<String,String> message = remoteMessage.getData();

        String projectId = remoteMessage.getData().get("project_id");
        Bundle bundle = new Bundle();
        bundle.putString("project_id", projectId);
        System.out.println(">---> " + projectId);

        if (intercomPushClient.isIntercomPush(message)) {
            intercomPushClient.handlePush(getApplication(), message);
        }else{

            Log.e(TAG,"ENTER NOTIFICATION");

            //PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
            // SplashActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent contentIntent = PendingIntent.getActivity(this,
                    0, new Intent(this, AcceptInvitationActivity.class),
                    PendingIntent.FLAG_ONE_SHOT);
            String title;

            if (remoteMessage.getNotification()!=null){
                if (remoteMessage.getNotification().getTitle()!=null &&
                        !remoteMessage.getNotification().getTitle().isEmpty()){
                    title = remoteMessage.getNotification().getTitle();
                }
                else{
                    title = "Tully";
                }

                if (null != remoteMessage.getNotification().getClickAction()) {
                    startActivity(remoteMessage.getNotification().getClickAction(),null,
                            this);
                }

                notificationBuilder = new NotificationCompat.Builder(this,TULLY_APP)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle(title)
                    .setContentText(remoteMessage.getNotification().getBody())
                    .setPriority(remoteMessage.getPriority())
                    //.setStyle(new NotificationCompat.BigTextStyle().bigText(
                        // remoteMessage.getData().get("message")))
                    .setAutoCancel(true);

                NotificationManager notificationManager = (NotificationManager)
                        getSystemService(Context.NOTIFICATION_SERVICE);

                notificationBuilder.setContentIntent(contentIntent);

                long time = new Date().getTime();
                String tmpStr = String.valueOf(time);
                String last4Str = tmpStr.substring(tmpStr.length() - 5);
                int notificationId = Integer.valueOf(last4Str);

                Notification mNotification = notificationBuilder.build();
                if (notificationManager != null) {
                    notificationManager.notify(notificationId, mNotification);
                }
                else{
                    Log.e(TAG,"NM IS NULL");
                }
            }
        }
    }


}
