package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tullyapp.tully.CustomView.ImageProgressBar;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Utils.Percent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by macbookpro on 10/09/17.
 */

public class ProfileRecordingsAdapter extends RecyclerView.Adapter<ProfileRecordingsAdapter.RecordingsViewHolder> {

    private Context context;
    private ArrayList<Recording> recordings;
    private playRecordingListener playRecordingListener;
    private Percent percent;

    public ProfileRecordingsAdapter(Context context, ArrayList<Recording> recordings) {
        this.context = context;
        this.recordings = recordings;
        this.percent = new Percent(0);
    }

    @Override
    public RecordingsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecordingsViewHolder(LayoutInflater.from(context).inflate(R.layout.profile_recording_item,parent,false));
    }

    @Override
    public void onBindViewHolder(RecordingsViewHolder holder, int position, List<Object> payloads) {
        if(!payloads.isEmpty()) {
            final Recording recordingAppModel = recordings.get(position);
            percent.setPercent(recordingAppModel.getProgressPercent());
            holder.img_progress.setCurrentValue(percent);
        }
        else{
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(final RecordingsViewHolder holder, final int i) {
        final int position = holder.getAdapterPosition();
        final Recording recording = recordings.get(position);
        holder.tv_title.setText(recording.getProjectName());
        holder.tv_subtitle.setText(recording.getName());

        if (recording.isPlaySelected()){
            if (recording.isPlaying()){
                holder.btn_play.setImageResource(R.drawable.record_pause);
            }else{
                holder.btn_play.setImageResource(R.drawable.recordlist_play);
            }
        }else{
            holder.btn_play.setImageResource(R.drawable.play_gray_icon);
        }

        holder.btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playRecordingListener!=null && recording.isPlaySelected()){
                    if (recording.isPlaying()){
                        recording.setPlaying(false);
                        recording.setPaused(true);
                        playRecordingListener.onRecordPause(recording,position);
                        holder.btn_play.setImageResource(R.drawable.recordlist_play);
                    }
                    else if (recording.isPaused()){
                        recording.setPaused(false);
                        recording.setPlaying(true);
                        playRecordingListener.onResume(recording,position);
                        holder.btn_play.setImageResource(R.drawable.record_pause);
                    }
                    else{
                        playRecordingListener.onPlayRecord(recording,position);
                    }
                }else{
                    if (playRecordingListener != null) {
                        playRecordingListener.onPlayRecord(recording,position);
                    }
                }
            }
        });

        percent.setPercent(1);
        holder.img_progress.setCurrentValue(percent);
        percent.setPercent(0);
        holder.img_progress.setCurrentValue(percent);
    }

    public void setOnPlayClickListener(playRecordingListener playClickListener){
        this.playRecordingListener = playClickListener;
    }

    public void markAudioComplete(Recording recording){

        for (Recording rec : recordings){
            if (recording.getId().equals(rec.getId())){
                rec.setPlaying(false);
                rec.setPaused(false);
                rec.setPlaySelected(true);
                break;
            }
        }

        this.notifyDataSetChanged();
    }

    public void markAudioPlaying(Recording recording){
        for (Recording rec : recordings){
            if (recording.getId().equals(rec.getId())){
                rec.setPlaying(true);
                rec.setPaused(false);
                rec.setPlaySelected(true);
            }
            else{
                rec.setPlaying(false);
                rec.setPaused(false);
                rec.setPlaySelected(false);
            }
        }
        this.notifyDataSetChanged();
    }

    public void updateProgress(int position, int percent){
        Recording rec = recordings.get(position);
        rec.setProgressPercent(percent);
        this.notifyItemChanged(position,true);
    }

    @Override
    public int getItemCount() {
        return recordings.size();
    }

    class RecordingsViewHolder extends RecyclerView.ViewHolder{

        private TextView tv_title;
        private TextView tv_subtitle;
        private ImageView btn_play;
        private ImageProgressBar img_progress;

        RecordingsViewHolder(View itemView) {
            super(itemView);
            img_progress = itemView.findViewById(R.id.img_progress);
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_subtitle = itemView.findViewById(R.id.tv_subtitle);
            btn_play = itemView.findViewById(R.id.btn_play);
        }
    }

    public interface playRecordingListener{
        void onPlayRecord(Recording recording, int position);

        void onRecordPause(Recording recording, int position);

        void onResume(Recording recording, int position);
    }
}
