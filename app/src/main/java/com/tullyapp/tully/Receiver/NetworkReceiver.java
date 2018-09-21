package com.tullyapp.tully.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.tullyapp.tully.Services.DeletePendings;
import com.tullyapp.tully.Services.UploadPendings;

public class NetworkReceiver extends BroadcastReceiver {

    private static final String TAG = NetworkReceiver.class.getSimpleName();
    private Context context;

    public NetworkReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        try{
            ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm!=null){
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork!=null){
                    boolean isConnected = activeNetwork.isConnectedOrConnecting();
                    if (isConnected){
                        if (!UploadPendings.isRunning) UploadPendings.startAction(context);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }



}
