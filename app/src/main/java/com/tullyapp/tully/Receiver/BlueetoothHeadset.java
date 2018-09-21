package com.tullyapp.tully.Receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

public class BlueetoothHeadset extends BroadcastReceiver {

    private static final String TAG = BlueetoothHeadset.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        String action = intent.getAction();
        try{
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            switch (state){
                case -1:
                    am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    am.startBluetoothSco();
                    am.setBluetoothScoOn(true);
                    break;

                case 10:
                    am.setMode(AudioManager.MODE_NORMAL);
                    am.stopBluetoothSco();
                    am.setBluetoothScoOn(false);
                    break;
            }

            Log.e(TAG,action+" : "+state);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
