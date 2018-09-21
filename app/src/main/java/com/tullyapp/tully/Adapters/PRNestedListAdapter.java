package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
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

import java.util.List;

import static com.tullyapp.tully.Utils.Constants.MUSIC_OBJECT;
import static com.tullyapp.tully.Utils.Constants.ON_CHECKED;
import static com.tullyapp.tully.Utils.Constants.ON_LONG_PRESSED;
import static com.tullyapp.tully.Utils.Constants.ON_PAUSE;
import static com.tullyapp.tully.Utils.Constants.ON_PLAY;
import static com.tullyapp.tully.Utils.Constants.ON_RESUME;
import static com.tullyapp.tully.Utils.Constants.POSITION_PARAM;
import static com.tullyapp.tully.Utils.Constants.WIDGET_DELETE;
import static com.tullyapp.tully.Utils.Constants.WIDGET_RENAME;
import static com.tullyapp.tully.Utils.Constants.WIDGET_SHARE;

/**
 * Created by apple on 07/01/18.
 */

public class PRNestedListAdapter extends RecyclerView.Adapter<PRNestedListAdapter.PRNHolder>{

    private static final String TAG = PRNestedListAdapter.class.getSimpleName();
    private List<Recording> recordingList;
    private Context context;
    private LocalBroadcastManager lbm;
    private int outerPosition;
    private Percent percent;
    private boolean selection = false;
    private ViewBinderHelper binderHelper;

    PRNestedListAdapter(Context context, List<Recording> recordingList, int outerPosition) {
        this.context = context;
        this.recordingList = recordingList;
        this.lbm = LocalBroadcastManager.getInstance(context);
        this.outerPosition = outerPosition;
        this.percent = new Percent(0);
        this.binderHelper = new ViewBinderHelper();
        this.binderHelper.setOpenOnlyOne(true);
    }

    @Override
    public PRNHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new PRNHolder(LayoutInflater.from(context).inflate(R.layout.pr_item, viewGroup,false));
    }

    @Override
    public void onBindViewHolder(PRNHolder holder, int position, List<Object> payloads) {
        if(!payloads.isEmpty()) {
            try{
                Bundle b = (Bundle) payloads.get(0);
                boolean check = b.getBoolean(ON_CHECKED);
                //holder.checkbox.setChecked(check);
                super.onBindViewHolder(holder, position, payloads);
            }
            catch (Exception e){
                percent.setPercent(recordingList.get(position).getProgressPercent());
                holder.imageProgressBar.setCurrentValue(percent);
            }
        }
        else{
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    public void showSelection(boolean boo){
        this.selection = boo;
        notifyDataSetChanged();
    }

    public void setCheckedAll(boolean boo){
        for(Recording r : recordingList){
            r.setChecked(boo);
        }
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(PRNHolder prnHolder, int i) {
        prnHolder.init(prnHolder.getAdapterPosition());
    }

    @Override
    public int getItemCount() {
        return recordingList.size();
    }

    class PRNHolder extends RecyclerView.ViewHolder{
        RelativeLayout list_item;
        ImageView btn_recording_play;
        TextView recording_heading;
        ImageProgressBar imageProgressBar;
        AppCompatCheckBox checkbox;
        SwipeRevealLayout swipeLayout;
        LinearLayout widget_rename, widget_share, widget_delete;

        PRNHolder(View itemView) {
            super(itemView);
            btn_recording_play = itemView.findViewById(R.id.btn_recording_play);
            recording_heading = itemView.findViewById(R.id.recording_heading);
            imageProgressBar = itemView.findViewById(R.id.img_progress);
            checkbox = itemView.findViewById(R.id.checkbox);
            list_item = itemView.findViewById(R.id.list_item);

            swipeLayout = itemView.findViewById(R.id.swipeLayout);
            widget_rename = itemView.findViewById(R.id.widget_rename);
            widget_share = itemView.findViewById(R.id.widget_share);
            widget_delete = itemView.findViewById(R.id.widget_delete);
        }

        void init(final int position){
            final Recording recording = recordingList.get(position);
            recording.setOuterPos(outerPosition);

            binderHelper.bind(swipeLayout,recording.getId());
            if (swipeLayout.isOpened()){
                swipeLayout.close(true);
            }

            widget_rename.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendWidgetAction(recording,WIDGET_RENAME,position);
                    swipeLayout.close(true);
                }
            });

            widget_share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendWidgetAction(recording,WIDGET_SHARE,position);
                    swipeLayout.close(true);
                }
            });

            widget_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendWidgetAction(recording,WIDGET_DELETE,position);
                    swipeLayout.close(true);
                }
            });

            recording_heading.setText(recording.getName());
            btn_recording_play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (recording.isPlaySelected()){
                    if (recording.isPlaying()){
                        recording.setPlaying(false);
                        recording.setPaused(true);
                        btn_recording_play.setImageResource(R.drawable.recordlist_play);
                        pauseEvent(recording,position);
                    }
                    else if (recording.isPaused()){
                        recording.setPaused(false);
                        recording.setPlaying(true);
                        btn_recording_play.setImageResource(R.drawable.record_pause);
                        resumeEvent(recording,position);
                    }
                    else{
                        playEvent(recording,position);
                    }
                }else{
                    playEvent(recording,position);
                }
                }
            });

            //Log.e(TAG," -------"+recording.getName());
            if (recording.isPlaySelected()){
                //Log.e(TAG,"Selected");
                if (recording.isPlaying()){
                    //Log.e(TAG,"isPlaying");
                    btn_recording_play.setImageResource(R.drawable.record_pause);
                }else{
                    //Log.e(TAG,"notPlaying");
                    btn_recording_play.setImageResource(R.drawable.recordlist_play);
                }
            }else{
                //Log.e(TAG,"notPlaySelected");
                btn_recording_play.setImageResource(R.drawable.play_gray_icon);
            }

            checkbox.setChecked(recording.isChecked());

            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (recording.isChecked() != isChecked){
                        checkedEvent(recording,isChecked,position);
                    }
                }
            });

            list_item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (checkbox.getVisibility()==View.VISIBLE){
                    checkedEvent(recording,!checkbox.isChecked(),position);
                }
                }
            });

            list_item.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onLongPress(recording,position);
                    return true;
                }
            });

            if (selection){
                checkbox.setVisibility(View.VISIBLE);
                //checkbox.setChecked(checked);
            }else{
                checkbox.setVisibility(View.GONE);
            }
            //Log.e(TAG,"---------");
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

    private void onLongPress(Recording recording, int position){
        Intent intent = new Intent(ON_LONG_PRESSED);
        intent.putExtra(MUSIC_OBJECT, recording);
        intent.putExtra(POSITION_PARAM,position);
        lbm.sendBroadcast(intent);
    }

    private void checkedEvent(Recording recording, boolean boo, int position){
        Intent intent = new Intent(ON_CHECKED);
        intent.putExtra(MUSIC_OBJECT, recording);
        intent.putExtra(POSITION_PARAM,position);
        intent.putExtra(ON_CHECKED,boo);
        lbm.sendBroadcast(intent);
    }

    private void sendWidgetAction(Recording recording, String action, int position){
        Intent intent = new Intent(action);
        intent.putExtra(MUSIC_OBJECT, recording);
        intent.putExtra(POSITION_PARAM,position);
        lbm.sendBroadcast(intent);
    }
}
