package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
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
import com.tullyapp.tully.Models.OrganizedRec;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Utils.Percent;

import java.util.ArrayList;
import java.util.List;

import static com.tullyapp.tully.Utils.Constants.ALL_SELECTED;
import static com.tullyapp.tully.Utils.Constants.MUSIC_OBJECT;
import static com.tullyapp.tully.Utils.Constants.ON_CHECKED;
import static com.tullyapp.tully.Utils.Constants.ON_PAUSE;
import static com.tullyapp.tully.Utils.Constants.ON_PLAY;
import static com.tullyapp.tully.Utils.Constants.ON_RESUME;
import static com.tullyapp.tully.Utils.Constants.POSITION_PARAM;

/**
 * Created by macbookpro on 14/09/17.
 */

public class RecordingListAdapter  extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private static final String TAG = RecordingListAdapter.class.getSimpleName();
    private ArrayList<OrganizedRec> recordingAppModels;
    private Context context;
    private boolean selection = false;
    private boolean checked = false;
    private Percent percent;
    private static final int NOPROJECT_REC = 1;
    private static final int PROJECT_REC = 0;
    private LocalBroadcastManager lbm;
    private int lastExpandedPosition = -1;
    private boolean preventEvent;
    private ViewBinderHelper binderHelper;
    private OnWidgetAction onWidgetAction;

    public interface OnWidgetAction{
        void onShare(Recording recording, int position);
        void onRename(Recording recording, int position);
        void onDelete(Recording recording, int position);
        void onLongPress(Recording recording, int position);
        void onLongPress(OrganizedRec organizedRec, int position);
    }

    public void setOnWidgetAction(OnWidgetAction onWidgetAction){
        this.onWidgetAction = onWidgetAction;
    }

    public RecordingListAdapter(Context context, ArrayList<OrganizedRec> recordingAppModels) {
        this.recordingAppModels = recordingAppModels;
        this.context = context;
        this.percent = new Percent(0);
        this.lbm = LocalBroadcastManager.getInstance(context);
        this.binderHelper = new ViewBinderHelper();
        this.binderHelper.setOpenOnlyOne(true);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == PROJECT_REC){
            return new ProjectRecordingListViewHolder(LayoutInflater.from(context).inflate(R.layout.recording_list_item_with_project,parent,false));
        }else{
            return new RecordingListViewHolder(LayoutInflater.from(context).inflate(R.layout.recording_list_item,parent,false));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (recordingAppModels.get(position).isOfProject())
            return PROJECT_REC;

        return NOPROJECT_REC;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position, List<Object> payloads) {
        if(payloads.size()>0) {
            final OrganizedRec recordingAppModel = recordingAppModels.get(position);
            if (!recordingAppModel.isOfProject()){
                percent.setPercent(recordingAppModel.getRecording().getProgressPercent());
                RecordingListViewHolder holder = (RecordingListViewHolder) viewHolder;
                holder.imageProgressBar.setCurrentValue(percent);
            }
            else{
                ProjectRecordingListViewHolder holder = (ProjectRecordingListViewHolder) viewHolder;
                Bundle b;
                int pos;
                try{
                    b = (Bundle) payloads.get(0);
                    if (b!=null){
                        boolean all_selected = b.getBoolean(ALL_SELECTED);
                        pos = b.getInt(POSITION_PARAM);
                        holder.nestedListAdapter.notifyItemChanged(pos,b);

                        preventEvent = true;

                        if (all_selected){
                            holder.checkBox.setChecked(true);
                        }
                        else{
                            holder.checkBox.setChecked(false);
                        }
                    }
                    else{
                        throw new Exception("No bundle");
                    }
                }
                catch (Exception e){
                    preventEvent = false;
                    e.printStackTrace();
                    pos = (int) payloads.get(0);
                    holder.nestedListAdapter.notifyItemChanged(pos,true);
                }

            }
        }
        else{
            super.onBindViewHolder(viewHolder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int i) {
        int position = holder.getAdapterPosition();
        if (holder instanceof ProjectRecordingListViewHolder){
            ProjectRecordingListViewHolder viewHolder = (ProjectRecordingListViewHolder) holder;
            viewHolder.init(position);
        }
        else{
            RecordingListViewHolder viewHolder = (RecordingListViewHolder) holder;
            viewHolder.init(position);
        }
    }

    public void showSelection(boolean boo){
        this.selection = boo;

        for (OrganizedRec recordingAppModel : recordingAppModels){
            if(!recordingAppModel.isOfProject()){
                recordingAppModel.getRecording().setChecked(false);
            }
            if (recordingAppModel.isOfProject()){
                if (!boo){
                    recordingAppModel.setExpanded(false);
                    lastExpandedPosition=-1;
                }
                for (Recording recording : recordingAppModel.getRecordingList()){
                    if (recording.isChecked()){
                        recording.setChecked(false);
                    }
                }
            }
        }

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
        ArrayList<Recording> selectedRecordingList = new ArrayList<>();
        for (OrganizedRec recordingAppModel : recordingAppModels){
            if(!recordingAppModel.isOfProject() && recordingAppModel.getRecording().isChecked()){
                selectedRecordingList.add(recordingAppModel.getRecording());
            }
            if (recordingAppModel.isOfProject()){
                for (Recording recording : recordingAppModel.getRecordingList()){
                    if (recording.isChecked()){
                        selectedRecordingList.add(recording);
                    }
                }
            }
        }
        return selectedRecordingList;
    }

    public class RecordingListViewHolder extends RecyclerView.ViewHolder{
        ImageView btn_recording_play;
        RelativeLayout list_item;
        TextView recording_heading;
        TextView recording_sub_title;
        AppCompatCheckBox checkbox;
        ImageProgressBar imageProgressBar;
        SwipeRevealLayout swipeLayout;
        LinearLayout widget_rename, widget_share, widget_delete;

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

        void init(final int position){
            OrganizedRec organizedRec = recordingAppModels.get(position);
            final Recording recordingAppModel = organizedRec.getRecording();

            binderHelper.bind(swipeLayout,recordingAppModel.getId());
            if (swipeLayout.isOpened()){
                swipeLayout.close(true);
            }
            widget_rename.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onRename(recordingAppModel,position);
                    swipeLayout.close(true);
                }
                }
            });

            widget_share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onShare(recordingAppModel,position);
                    swipeLayout.close(true);
                }
                }
            });

            widget_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onDelete(recordingAppModel,position);
                    swipeLayout.close(true);
                }
                }
            });

            recording_heading.setText(recordingAppModel.getName());
            recording_sub_title.setText(recordingAppModel.getProjectName());

            if (!recordingAppModel.isOfProject()){
                recording_sub_title.setTextColor(context.getResources().getColor(R.color.colorAccent));
            }
            else{
                recording_sub_title.setTextColor(context.getResources().getColor(R.color.colorLightText));
            }

            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    recordingAppModel.setChecked(isChecked);
                }
            });

            list_item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (checkbox.getVisibility()==View.VISIBLE){
                    checkbox.setChecked(!checkbox.isChecked());
                }
                }
            });

            list_item.setOnLongClickListener(new View.OnLongClickListener() {
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
                    btn_recording_play.setImageResource(R.drawable.record_pause);
                }else{
                    btn_recording_play.setImageResource(R.drawable.recordlist_play);
                }
            }else{
                btn_recording_play.setImageResource(R.drawable.play_gray_icon);
            }

            btn_recording_play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (recordingAppModel.isPlaySelected()){
                    if (recordingAppModel.isPlaying()){
                        recordingAppModel.setPlaying(false);
                        recordingAppModel.setPaused(true);
                        btn_recording_play.setImageResource(R.drawable.recordlist_play);
                        pauseEvent(recordingAppModel,position);
                    }
                    else if (recordingAppModel.isPaused()){
                        recordingAppModel.setPaused(false);
                        recordingAppModel.setPlaying(true);
                        btn_recording_play.setImageResource(R.drawable.record_pause);
                        resumeEvent(recordingAppModel,position);
                    }
                    else{
                        playEvent(recordingAppModel,position);
                    }
                }else{
                    playEvent(recordingAppModel,position);
                }
                }
            });

            if (selection){
                checkbox.setVisibility(View.VISIBLE);
                checkbox.setChecked(checked);
            }else{
                checkbox.setVisibility(View.GONE);
            }

            percent.setPercent(1);
            imageProgressBar.setCurrentValue(percent);
            percent.setPercent(0);
            imageProgressBar.setCurrentValue(percent);
        }
    }

    private void playEvent(Recording recording, int position){
        Intent intent = new Intent(ON_PLAY);
        intent.putExtra(MUSIC_OBJECT, recording);
        intent.putExtra(POSITION_PARAM,position);
        lbm.sendBroadcast(intent);
    }

    private void resumeEvent(Recording recording, int position){
        Intent intent = new Intent(ON_RESUME);
        intent.putExtra(MUSIC_OBJECT, recording);
        intent.putExtra(POSITION_PARAM,position);
        lbm.sendBroadcast(intent);
    }

    private void pauseEvent(Recording recording, int position){
        Intent intent = new Intent(ON_PAUSE);
        intent.putExtra(MUSIC_OBJECT, recording);
        intent.putExtra(POSITION_PARAM,position);
        lbm.sendBroadcast(intent);
    }

    public class ProjectRecordingListViewHolder extends RecyclerView.ViewHolder{

        private RelativeLayout project_recording_item;
        private TextView tv_projectname;
        private RecyclerView recycle_view_project_recordings;
        private PRNestedListAdapter nestedListAdapter;
        private AppCompatCheckBox checkBox;

        ProjectRecordingListViewHolder(View itemView) {
            super(itemView);
            project_recording_item = itemView.findViewById(R.id.project_recording_item);
            tv_projectname = itemView.findViewById(R.id.tv_projectname);
            recycle_view_project_recordings = itemView.findViewById(R.id.recycle_view_project_recordings);
            checkBox = itemView.findViewById(R.id.checkbox);
        }

        void init(final int position){
            final OrganizedRec organizedRec = recordingAppModels.get(position);
            tv_projectname.setText(organizedRec.getRecordingList().get(0).getProjectName());
            nestedListAdapter = new PRNestedListAdapter(context,organizedRec.getRecordingList(),position);

            if (selection){
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(checked);
            }else{
                checkBox.setVisibility(View.INVISIBLE);
            }

            recycle_view_project_recordings.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));

            nestedListAdapter.setCheckedAll(checked);
            nestedListAdapter.showSelection(selection);
            recycle_view_project_recordings.setAdapter(nestedListAdapter);

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked){
                        lastExpandedPosition = position;
                        recycle_view_project_recordings.setVisibility(View.VISIBLE);
                        nestedListAdapter.setCheckedAll(true);
                    }
                    else{
                        if (!preventEvent){
                            lastExpandedPosition = -1;
                            recycle_view_project_recordings.setVisibility(View.GONE);
                            nestedListAdapter.setCheckedAll(false);
                        }
                    }
                }
            });

            if (organizedRec.isExpanded() || lastExpandedPosition==position){
                lastExpandedPosition = position;
                recycle_view_project_recordings.setVisibility(View.VISIBLE);
            }
            else{
                recycle_view_project_recordings.setVisibility(View.GONE);
            }

            project_recording_item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (recycle_view_project_recordings.getVisibility()==View.VISIBLE){
                        if (checkBox.isChecked()){
                            checkBox.setChecked(false);
                            nestedListAdapter.setCheckedAll(false);
                            recycle_view_project_recordings.setVisibility(View.GONE);
                            lastExpandedPosition = -1;
                        }
                        else{
                            checkBox.setChecked(true);
                            nestedListAdapter.setCheckedAll(true);
                            recycle_view_project_recordings.setVisibility(View.VISIBLE);
                        }
                    }
                    else{
                        lastExpandedPosition = position;
                        recycle_view_project_recordings.setVisibility(View.VISIBLE);
                        checkBox.setChecked(true);
                        nestedListAdapter.setCheckedAll(true);
                    }
                }
            });

            project_recording_item.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (onWidgetAction!=null){
                        onWidgetAction.onLongPress(organizedRec,position);
                    }
                    return true;
                }
            });
        }
    }

    public void markAudioComplete(Recording recordingObj, int position){
        if (recordingObj.isOfProject()){
            int outerPos = recordingObj.getOuterPos();
            OrganizedRec rec = recordingAppModels.get(outerPos);
            rec.getRecordingList().get(position).setPlaying(false);
            rec.getRecordingList().get(position).setPaused(false);
            rec.getRecordingList().get(position).setPlaySelected(true);
            rec.getRecordingList().get(position).setProgressPercent(0);
            this.notifyDataSetChanged();
        }
        else{
            OrganizedRec rec = recordingAppModels.get(position);
            rec.getRecording().setPlaying(false);
            rec.getRecording().setPaused(false);
            rec.getRecording().setPlaySelected(true);
            rec.getRecording().setProgressPercent(0);
            this.notifyItemChanged(position);
        }
    }

    public void markAudioPlaying(Recording recordingObj, int position){
        for (OrganizedRec or : recordingAppModels){
            for (Recording recording : or.getRecordingList()){
                if (recording.isPlaySelected()){
                    recording.setPlaySelected(false);
                    recording.setPlaying(false);
                    recording.setPaused(false);
                    recording.setProgressPercent(0);
                }
            }

            if (!or.isOfProject()){
                or.getRecording().setPlaySelected(false);
                or.getRecording().setPlaying(false);
                or.getRecording().setPaused(false);
                or.getRecording().setProgressPercent(0);
            }
            or.setExpanded(false);
        }

        if (recordingObj.isOfProject()){
            OrganizedRec rec = recordingAppModels.get(recordingObj.getOuterPos());
            rec.setExpanded(true);
            rec.getRecordingList().get(position).setPlaySelected(true);
            rec.getRecordingList().get(position).setPlaying(true);
            rec.getRecordingList().get(position).setPaused(false);
        }
        else{
            OrganizedRec rec = recordingAppModels.get(position);

            rec.getRecording().setPlaySelected(true);
            rec.getRecording().setPlaying(true);
            rec.getRecording().setPaused(false);
        }

        this.notifyDataSetChanged();
    }

    public void updateProgress(Recording recordingObject, int position, int percent){
        try {
            if (!recordingObject.isOfProject()){
                OrganizedRec rec = recordingAppModels.get(position);
                rec.getRecording().setProgressPercent(percent);
                this.notifyItemChanged(position,true);
            }
            else{
                OrganizedRec rec = recordingAppModels.get(recordingObject.getOuterPos());
                rec.getRecordingList().get(position).setProgressPercent(percent);
                this.notifyItemChanged(recordingObject.getOuterPos(),position);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void updateRecordingAtPos(Recording recording, int position){
        try {
            if (!recording.isOfProject()){
                this.notifyItemChanged(position);
            }
            else{
                this.notifyItemChanged(recording.getOuterPos());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void updateRecordingAtPos(int position){
        this.notifyItemChanged(position);
    }

    public void markChecked(Recording recording, boolean check, int position){
        int pos = recording.getOuterPos();
        OrganizedRec o = recordingAppModels.get(pos);
        o.setExpanded(true);
        o.getRecordingList().get(position).setChecked(check);

        boolean allSelected = true;
        for (Recording r : o.getRecordingList()){
            if (!r.isChecked()){
                allSelected = false;
            }
        }

        Bundle b = new Bundle();
        b.putSerializable(MUSIC_OBJECT,recording);
        b.putBoolean(ON_CHECKED,check);
        b.putInt(POSITION_PARAM,position);
        b.putBoolean(ALL_SELECTED,allSelected);

        this.notifyItemChanged(pos,b);
    }
}
