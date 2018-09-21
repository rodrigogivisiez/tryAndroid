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
import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.R;

import java.util.ArrayList;
import java.util.Collections;

import static com.tullyapp.tully.Utils.Constants.BYTETOMB;

/**
 * Created by macbookpro on 20/09/17.
 */

public class PlayerListAdapter extends RecyclerView.Adapter<PlayerListAdapter.PlayerListViewHolder> {

    private static final String TAG = PlayerListAdapter.class.getSimpleName();
    private Context context;
    private ArrayList<AudioFile> copyToTullies;
    private double size;
    private boolean selection = false;
    private AdapterInterface adapterListener;

    private ViewBinderHelper binderHelper;
    private OnWidgetAction onWidgetAction;
    public interface OnWidgetAction{
        void onShare(AudioFile audioFile, int position);
        void onRename(AudioFile audioFile, int position);
        void onDelete(AudioFile audioFile, int position);
    }
    public void setOnWidgetAction(OnWidgetAction onWidgetAction){
        this.onWidgetAction = onWidgetAction;
    }

    public PlayerListAdapter(Context context) {
        this.context = context;
        this.copyToTullies = new ArrayList<>();
        this.binderHelper  = new ViewBinderHelper();
        this.binderHelper.setOpenOnlyOne(true);
    }

    public void clear(){
        this.copyToTullies.clear();
        notifyDataSetChanged();
    }

    public void add(AudioFile audioFiles){
        this.copyToTullies.add(audioFiles);
    }

    public void reverseList(){
        Collections.reverse(this.copyToTullies);
    }

    public interface AdapterInterface{
        void onFileTap(AudioFile audioFile, int position);
        void onLongPress(AudioFile audioFile,int position);
    }

    public void setAdapterListener(AdapterInterface adapterListener){
        this.adapterListener = adapterListener;
    }

    @NonNull
    @Override
    public PlayerListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PlayerListViewHolder(LayoutInflater.from(context).inflate(R.layout.player_list_item,parent,false));
    }

    @Override
    public void onBindViewHolder(PlayerListViewHolder holder, int i) {
        holder.init(holder.getAdapterPosition());
    }

    @Override
    public int getItemCount() {
        return copyToTullies.size();
    }

    public ArrayList<AudioFile> getSelectedItems(){
        ArrayList<AudioFile> audioFileArrayList = new ArrayList<>();
        for (AudioFile audioFile : copyToTullies){
            if (audioFile.isChecked()){
                audioFileArrayList.add(audioFile);
            }
        }

        return audioFileArrayList;
    }

    public void showSelection(boolean boo){
        this.selection = boo;
        for(AudioFile audioFile : copyToTullies){
            audioFile.setChecked(false);
        }
        this.notifyDataSetChanged();
    }

    public void updateAtPos(int position){
        try{
            this.notifyItemChanged(position);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void setCheckedAll(boolean boo){
        for (AudioFile audioFile : copyToTullies){
            audioFile.setChecked(true);
        }
        this.notifyDataSetChanged();
    }

    class PlayerListViewHolder extends RecyclerView.ViewHolder{

        private RelativeLayout list_item;
        private AppCompatCheckBox selectItem;
        private TextView tv_file_title;
        private TextView tv_file_size;
        private ImageView player_icon;
        private SwipeRevealLayout swipeLayout;
        private LinearLayout widget_rename, widget_share, widget_delete;

        PlayerListViewHolder(View itemView) {
            super(itemView);
            player_icon = itemView.findViewById(R.id.player_icon);
            list_item = itemView.findViewById(R.id.list_item);
            selectItem = itemView.findViewById(R.id.selectItem);
            tv_file_title = itemView.findViewById(R.id.tv_file_title);
            tv_file_size = itemView.findViewById(R.id.tv_file_size);

            swipeLayout = itemView.findViewById(R.id.swipeLayout);

            widget_rename = itemView.findViewById(R.id.widget_rename);
            widget_share = itemView.findViewById(R.id.widget_share);
            widget_delete = itemView.findViewById(R.id.widget_delete);
        }

        void init(final int position){
            final AudioFile audioFile = copyToTullies.get(position);
            tv_file_title.setText(audioFile.getTitle());
            size = audioFile.getSize();
            size = (size / BYTETOMB);
            tv_file_size.setText(String.format("%.2f", size)+" MB");

            selectItem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    audioFile.setChecked(isChecked);
                }
            });

            binderHelper.bind(swipeLayout,audioFile.getId());

            if (swipeLayout.isOpened()){
                swipeLayout.close(true);
            }

            widget_rename.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onRename(audioFile,position);
                    swipeLayout.close(true);
                }
                }
            });

            widget_share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onShare(audioFile,position);
                    swipeLayout.close(true);
                }
                }
            });

            widget_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onDelete(audioFile,position);
                    swipeLayout.close(true);
                }
                }
            });

            list_item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectItem.getVisibility()==View.VISIBLE){
                        selectItem.setChecked(!selectItem.isChecked());
                    }
                    else{
                        if (adapterListener!=null){
                            adapterListener.onFileTap(audioFile,position);
                        }
                    }
                }
            });

            list_item.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (adapterListener!=null){
                        adapterListener.onLongPress(audioFile,position);
                    }
                    return true;
                }
            });

            if (selection){
                selectItem.setChecked(audioFile.isChecked());
                selectItem.setVisibility(View.VISIBLE);
            }else{
                selectItem.setVisibility(View.GONE);
            }

            if (audioFile.getId()!=null){
                if (audioFile.getId().equals("-L1111aaaaaaaaaaaaaa") || audioFile.getId().equals("video")){
                    player_icon.setImageResource(R.drawable.player_list_item_dark);
                }
                else{
                    player_icon.setImageResource(R.drawable.player_file_icon);
                }
            }
        }
    }
}
