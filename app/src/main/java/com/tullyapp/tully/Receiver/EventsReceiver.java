package com.tullyapp.tully.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.Interface.AudioFileEvents;
import com.tullyapp.tully.Interface.ProjectRecordingEvents;
import com.tullyapp.tully.Utils.ActionEventConstant;


public class EventsReceiver extends BroadcastReceiver {

    private ProjectRecordingEvents projectRecordingEvents;
    private AudioFileEvents audioFileEvents;

    public EventsReceiver(Context context,String... events) {
        IntentFilter filter= new IntentFilter();
        for (String action : events){
            filter.addAction(action);
        }

        this.unregister(context);
        LocalBroadcastManager.getInstance(context).registerReceiver(this, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action!=null){
            switch (action){
                case ActionEventConstant.PROJECT_RECORDING_UPLOADED:
                    if (projectRecordingEvents!=null){
                        Recording recording = (Recording) intent.getSerializableExtra(Recording.class.getName());
                        projectRecordingEvents.projectRecordingUploaded(recording);
                    }
                    break;

                case ActionEventConstant.AUDIO_FILE_UPLOADED:
                    if (audioFileEvents!=null){
                        AudioFile audioFile = (AudioFile) intent.getSerializableExtra(AudioFile.class.getName());
                        audioFileEvents.audioFileUploaded(audioFile);
                    }
                    break;

                default:
                    break;
            }
        }
    }


    public void setProjectRecordingEvents(ProjectRecordingEvents projectRecordingEvents){
        this.projectRecordingEvents = projectRecordingEvents;
    }

    public void setAudioFileEvents(AudioFileEvents audioFileEvents){
        this.audioFileEvents = audioFileEvents;
    }


    public void unregister(Context context){
        try{
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
