package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.tullyapp.tully.CustomView.ImageProgressBar;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Utils.Percent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.tullyapp.tully.Utils.Utils.formatAudioTime;


public class MultiTrackProjectAdapter extends RecyclerView.Adapter<MultiTrackProjectAdapter.RecordingListViewHolder>{
    private static final String TAG = MultiTrackProjectAdapter.class.getSimpleName();
    private ArrayList<Recording> recordings;
    private Context context;
    private ArrayList<Recording> selectedRecordingList;
    private Percent percent;
    private playRecordingListener playRecordingListener;
    private Recording rec;

    private ViewBinderHelper binderHelper;
    private OnWidgetAction onWidgetAction;
    public interface OnWidgetAction{
        void onShare(Recording recording, int position);
        void onRename(Recording recording, int position);
        void onDelete(Recording recording, int position);
        void onLongPress(Recording recording, int position);
        void onVolumeProgressChange(int progress, Recording recording, int position);
        void onVolumeProgressTouchStart(Recording recording, int position);
        void onVolumeProgressTouchEnd(Recording recording, int position);
    }
    public void setOnWidgetAction(OnWidgetAction onWidgetAction){
        this.onWidgetAction = onWidgetAction;
    }

    public MultiTrackProjectAdapter(Context context) {
        this.recordings = new ArrayList<>();
        this.context = context;
        this.percent = new Percent(0);
        this.binderHelper = new ViewBinderHelper();
        this.binderHelper.setOpenOnlyOne(true);
    }

    public void add(Recording rec){
        this.recordings.add(rec);
    }

    public void clear(){
        this.recordings.clear();
        this.notifyDataSetChanged();
    }

    public void reverse(){
        Collections.reverse(this.recordings);
    }

    @NonNull
    @Override
    public RecordingListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecordingListViewHolder(LayoutInflater.from(context).inflate(R.layout.multitrack_list_item,parent,false));
    }

    @Override
    public void onBindViewHolder(RecordingListViewHolder holder, int position, List<Object> payloads) {
        if(!payloads.isEmpty()) {
            Recording recording = recordings.get(position);
            int flag = (int) payloads.get(0);
            if (flag == 1){
                percent.setPercent(recording.getProgressPercent());
                holder.imageProgressBar.setCurrentValue(percent);
            }
            else if (flag == 2){
                percent.setPercent(recording.getProgressPercent());
                holder.imageProgressBar.setCurrentValue(percent);
                holder.tv_startTime.setText(recording.getProgress());
                holder.tv_endtime.setText(formatAudioTime(recording.getDuration()));
            }
            else if (flag == 3){
                if (recording.isPaused()){
                    holder.btn_recording_play.setImageResource(R.drawable.recordlist_play);
                }
                else{
                    holder.btn_recording_play.setImageResource(R.drawable.record_pause);
                }
            }
        }
        else{
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecordingListViewHolder viewholder, int position) {
        final RecordingListViewHolder holder = viewholder;
        final int pos = viewholder.getAdapterPosition();
        final Recording recordingAppModel = recordings.get(pos);
        holder.pos = viewholder.getAdapterPosition();
        holder.recording_heading.setText(recordingAppModel.getName());
        holder.recording_sub_title.setText(recordingAppModel.getProjectName());

        if (!recordingAppModel.isOfProject()){
            holder.recording_sub_title.setTextColor(context.getResources().getColor(R.color.colorAccent));
        }
        else{
            holder.recording_sub_title.setTextColor(context.getResources().getColor(R.color.colorLightText));
        }

        holder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            recordingAppModel.setChecked(isChecked);
            }
        });

        holder.checkbox.setChecked(recordingAppModel.isChecked());

        holder.list_item.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
            if (onWidgetAction!=null){
                onWidgetAction.onLongPress(recordingAppModel,pos);
            }
            return true;
            }
        });

        if (recordingAppModel.isPlaySelected()){
            if (recordingAppModel.isPlaying()){
                holder.btn_recording_play.setImageResource(R.drawable.record_pause);
            }else{
                holder.btn_recording_play.setImageResource(R.drawable.recordlist_play);
            }
        }
        else{
            holder.btn_recording_play.setImageResource(R.drawable.play_gray_icon);
        }

        holder.btn_recording_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playRecordingListener!=null){
                    if (recordingAppModel.isPlaySelected()){
                        if (recordingAppModel.isPlaying()){
                            recordingAppModel.setPlaying(false);
                            recordingAppModel.setPaused(true);
                            playRecordingListener.onRecordPause(recordingAppModel, pos);
                            holder.btn_recording_play.setImageResource(R.drawable.recordlist_play);
                        }
                        else if (recordingAppModel.isPaused()){
                            recordingAppModel.setPaused(false);
                            recordingAppModel.setPlaying(true);
                            playRecordingListener.onResume(recordingAppModel,pos);
                            holder.btn_recording_play.setImageResource(R.drawable.record_pause);
                        }
                        else{
                            playRecordingListener.onPlayRecord(recordingAppModel,pos);
                        }
                    }
                    else{
                        playRecordingListener.onPlayRecord(recordingAppModel,pos);
                    }
                }
            }
        });

        binderHelper.bind(holder.swipeLayout,recordingAppModel.getId());
        if (holder.swipeLayout.isOpened()){
            holder.swipeLayout.close(true);
        }

        holder.widget_rename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (onWidgetAction!=null){
                onWidgetAction.onRename(recordingAppModel,pos);
                holder.swipeLayout.close(true);
            }
            }
        });

        holder.widget_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (onWidgetAction!=null){
                onWidgetAction.onShare(recordingAppModel,pos);
                holder.swipeLayout.close(true);
            }
            }
        });

        holder.widget_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (onWidgetAction!=null){
                onWidgetAction.onDelete(recordingAppModel,pos);
                holder.swipeLayout.close(true);
            }
            }
        });

        if (recordingAppModel.isShowVolume()){
            holder.volume_bar.setVisibility(View.VISIBLE);
            holder.swipeLayout.setLockDrag(true);
        }
        else{
            holder.volume_bar.setVisibility(View.INVISIBLE);
            holder.swipeLayout.setLockDrag(false);
        }

        holder.volume_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (onWidgetAction!=null){
                    recordingAppModel.setVolume(progress);
                    onWidgetAction.onVolumeProgressChange(progress,recordingAppModel,pos);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (onWidgetAction!=null){
                    onWidgetAction.onVolumeProgressTouchStart(recordingAppModel,pos);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (onWidgetAction!=null){
                    onWidgetAction.onVolumeProgressTouchEnd(recordingAppModel,pos);
                }
            }
        });

        holder.tv_endtime.setText(formatAudioTime(recordingAppModel.getDuration()));

        percent.setPercent(1);
        holder.imageProgressBar.setCurrentValue(percent);
        percent.setPercent(0);
        holder.imageProgressBar.setCurrentValue(percent);
    }

    @Override
    public int getItemCount() {
        return recordings.size();
    }

    public ArrayList<Recording> getSelectedRecordings(){
        selectedRecordingList = new ArrayList<>();
        for (Recording recordingAppModel : recordings){
            if(recordingAppModel.isChecked()){
                selectedRecordingList.add(recordingAppModel);
            }
        }
        return selectedRecordingList;
    }

    public class RecordingListViewHolder extends RecyclerView.ViewHolder{
        public ImageView btn_recording_play;
        public RelativeLayout list_item;
        public TextView recording_heading, tv_startTime, tv_endtime;
        public TextView recording_sub_title;
        public AppCompatCheckBox checkbox;
        AppCompatSeekBar volume_bar;
        ImageProgressBar imageProgressBar;
        SwipeRevealLayout swipeLayout;
        LinearLayout widget_rename, widget_share, widget_delete;
        int pos;

        RecordingListViewHolder(View itemView) {
            super(itemView);
            btn_recording_play = itemView.findViewById(R.id.btn_recording_play);
            recording_heading = itemView.findViewById(R.id.recording_heading);
            recording_sub_title = itemView.findViewById(R.id.recording_sub_title);
            checkbox = itemView.findViewById(R.id.checkbox);
            list_item = itemView.findViewById(R.id.list_item);
            imageProgressBar = itemView.findViewById(R.id.img_progress);
            tv_startTime = itemView.findViewById(R.id.tv_startTime);
            tv_endtime = itemView.findViewById(R.id.tv_endtime);
            volume_bar = itemView.findViewById(R.id.volume_bar);

            swipeLayout = itemView.findViewById(R.id.swipeLayout);
            widget_rename = itemView.findViewById(R.id.widget_rename);
            widget_share = itemView.findViewById(R.id.widget_share);
            widget_delete = itemView.findViewById(R.id.widget_delete);
        }
    }

    public void setOnPlayClickListener(playRecordingListener playClickListener){
        this.playRecordingListener = playClickListener;
    }

    public void markAudioComplete(int position){
        rec = recordings.get(position);
        rec.setPlaying(false);
        rec.setPaused(false);
        rec.setPlaySelected(true);
        rec.setProgressPercent(0);
        this.notifyItemChanged(position);
    }

    public void markAudioPlaying(int position){

        for (Recording r : recordings){
            if (r.isPlaySelected()){
                r.setPlaySelected(false);
                r.setPlaying(false);
                r.setShowVolume(false);
                r.setChecked(false);
                r.setProgressPercent(0);
            }
        }

        rec = recordings.get(position);
        rec.setPlaying(true);
        rec.setPaused(false);
        rec.setPlaySelected(true);

        this.notifyDataSetChanged();
    }

    public int[] getVolume(int pos1, int pos2){
        return new int[] {recordings.get(pos1).getVolume(), recordings.get(pos2).getVolume()};
    }

    public void markMixAudioPlaying(int position){
        recordings.get(position).setPlaying(true);
        recordings.get(position).setPaused(false);
        recordings.get(position).setPlaySelected(true);
        recordings.get(position).setShowVolume(true);
        this.notifyItemChanged(position,3);
    }

    public void markMixAudioPause(int position){
        recordings.get(position).setPlaying(false);
        recordings.get(position).setPaused(true);
        recordings.get(position).setPlaySelected(true);
        recordings.get(position).setShowVolume(false);
        this.notifyItemChanged(position,3);
    }


    public Recording getObjectAtPosition(int position){
        return this.recordings.get(position);
    }

    public void markAudioPlaying(int pos1, int pos2){
        for (Recording r : recordings){
            if (r.isPlaySelected()){
                r.setPlaySelected(false);
                r.setPlaying(false);
                r.setProgressPercent(0);
            }
        }

        recordings.get(pos1).setPlaying(true);
        recordings.get(pos1).setPaused(false);
        recordings.get(pos1).setPlaySelected(true);
        recordings.get(pos1).setShowVolume(true);

        recordings.get(pos2).setPlaying(true);
        recordings.get(pos2).setPaused(false);
        recordings.get(pos2).setPlaySelected(true);
        recordings.get(pos2).setShowVolume(true);
        this.notifyDataSetChanged();
    }

    public void updateProgress(int position, int percent){
        try {
            rec = recordings.get(position);
            rec.setProgressPercent(percent);
            this.notifyItemChanged(position,1);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public int[] getPositions(String idA, String idB){
        int i = 0;
        int posA = -1, posB = -1;
        for (Recording recording : recordings){
            if (recording.getId().equals(idA)){
                posA = i;
            }
            if (recording.getId().equals(idB)){
                posB = i;
            }
            i++;
        }

        if (posA!=-1 && posB!=-1){
            return new int[]{posA,posB};
        }
        return null;
    }

    public void updateProgress(int position, String progress, int percent, long duration){
        // Log.e(TAG,position+" ::: "+progress+" : "+percent+" : "+duration);
        this.recordings.get(position).setDuration(duration);
        this.recordings.get(position).setProgress(progress);
        this.recordings.get(position).setProgressPercent(percent);
        this.notifyItemChanged(position,2);
    }

    public void resetProgress(int position, long duration){
        this.recordings.get(position).setProgress(context.getString(R.string._00_00));
        this.recordings.get(position).setProgressPercent(0);
        this.recordings.get(position).setPaused(true);
        this.recordings.get(position).setPlaying(false);
        this.recordings.get(position).setPlaySelected(true);
        this.recordings.get(position).setDuration(duration);
        this.notifyItemChanged(position);
    }

    public void updateAtPos(int position){
        this.notifyItemChanged(position);
    }

    public interface playRecordingListener{
        void onPlayRecord(Recording recording, int position);

        void onRecordPause(Recording recording, int position);

        void onResume(Recording recording, int position);
    }
}

