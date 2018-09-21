package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.tullyapp.tully.CustomView.ImageProgressBar;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Utils.Percent;

import java.util.ArrayList;
import java.util.List;


public class ProjectRecordingListAdapter  extends RecyclerView.Adapter<ProjectRecordingListAdapter.RecordingListViewHolder>{

    private ArrayList<Recording> recordingAppModels;
    private Context context;
    private ArrayList<Recording> selectedRecordingList;
    private boolean selection = false;
    private boolean checked = false;
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
    }
    public void setOnWidgetAction(OnWidgetAction onWidgetAction){
        this.onWidgetAction = onWidgetAction;
    }

    public ProjectRecordingListAdapter(Context context,ArrayList<Recording> recordingAppModels) {
        this.recordingAppModels = recordingAppModels;
        this.context = context;
        this.percent = new Percent(0);
        this.binderHelper = new ViewBinderHelper();
        this.binderHelper.setOpenOnlyOne(true);
    }

    @Override
    public RecordingListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecordingListViewHolder(LayoutInflater.from(context).inflate(R.layout.recording_list_item,parent,false));
    }

    @Override
    public void onBindViewHolder(RecordingListViewHolder holder, int position, List<Object> payloads) {
        if(!payloads.isEmpty()) {
            final Recording recordingAppModel = recordingAppModels.get(position);
            percent.setPercent(recordingAppModel.getProgressPercent());
            holder.imageProgressBar.setCurrentValue(percent);
        }
        else{
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecordingListViewHolder viewholder, final int position) {
        final RecordingListViewHolder holder = viewholder;
        final int pos = viewholder.getAdapterPosition();
        final Recording recordingAppModel = recordingAppModels.get(pos);
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

        holder.list_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (holder.checkbox.getVisibility()==View.VISIBLE){
                holder.checkbox.setChecked(!holder.checkbox.isChecked());
            }
            }
        });

        holder.list_item.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onLongPress(recordingAppModel,position);
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
        }else{
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
                    }else{
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

        if (selection){
            holder.checkbox.setVisibility(View.VISIBLE);
            holder.checkbox.setChecked(checked);
        }else{
            holder.checkbox.setVisibility(View.GONE);
        }

        percent.setPercent(1);
        holder.imageProgressBar.setCurrentValue(percent);
        percent.setPercent(0);
        holder.imageProgressBar.setCurrentValue(percent);
    }

    public void showSelection(boolean boo){
        this.selection = boo;
        notifyDataSetChanged();
    }

    public void setCheckedAll(boolean boo){
        this.checked = boo;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return recordingAppModels.size();
    }

    public ArrayList<Recording> getSelectedRecordings(){
        selectedRecordingList = new ArrayList<>();
        for (Recording recordingAppModel : recordingAppModels){
            if(recordingAppModel.isChecked()){
                selectedRecordingList.add(recordingAppModel);
            }
        }

        return selectedRecordingList;
    }

    public class RecordingListViewHolder extends RecyclerView.ViewHolder{
        public ImageView btn_recording_play;
        public RelativeLayout list_item;
        public TextView recording_heading;
        public TextView recording_sub_title;
        public AppCompatCheckBox checkbox;
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

        rec = recordingAppModels.get(position);
        rec.setPlaying(false);
        rec.setPaused(false);
        rec.setPlaySelected(true);
        rec.setProgressPercent(0);

        this.notifyItemChanged(position);
    }

    public void markAudioPlaying(int position){

        for (Recording r : recordingAppModels){
            if (r.isPlaySelected()){
                r.setPlaySelected(false);
                r.setPlaying(false);
                r.setProgressPercent(0);
            }
        }

        rec = recordingAppModels.get(position);
        rec.setPlaying(true);
        rec.setPaused(false);
        rec.setPlaySelected(true);

        this.notifyDataSetChanged();
    }

    public void updateProgress(int position, int percent){
        try {
            rec = recordingAppModels.get(position);
            rec.setProgressPercent(percent);
            this.notifyItemChanged(position,true);
        }catch (Exception e){
            e.printStackTrace();
        }
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
