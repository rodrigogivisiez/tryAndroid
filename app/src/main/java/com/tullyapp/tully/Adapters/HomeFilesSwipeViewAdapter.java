package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.R;

import java.net.URLDecoder;
import java.util.ArrayList;

import static com.tullyapp.tully.Utils.Constants.BYTETOMB;

public class HomeFilesSwipeViewAdapter extends RecyclerView.Adapter<HomeFilesSwipeViewAdapter.FilesHolder> {

    private ArrayList<AudioFile> copyToTullies;
    private Context context;
    private boolean selection=false;
    private boolean checkall = false;
    private boolean isFromHome = false;
    private AdapterInterface adapterInterface;
    private OnWidgetAction onWidgetAction;
    private ViewBinderHelper binderHelper;

    public interface OnWidgetAction{
        void onShare(AudioFile audioFile, int position);
        void onRename(AudioFile audioFile, int position);
        void onDelete(AudioFile audioFile, int position);
    }

    public void setOnWidgetAction(OnWidgetAction onWidgetAction){
        this.onWidgetAction = onWidgetAction;
    }

    public HomeFilesSwipeViewAdapter(Context context, ArrayList<AudioFile> copyToTullies, boolean isFromHome) {
        this.copyToTullies = copyToTullies;
        this.context = context;
        this.isFromHome = isFromHome;
        this.binderHelper = new ViewBinderHelper();
        this.binderHelper.setOpenOnlyOne(true);
    }

    public interface AdapterInterface{
        void onCopyToTully(AudioFile audioFile, int position);
        void onVideoPlay(AudioFile audioFile, int position);
        void onLongPress(AudioFile audioFile, int position);
    }

    public void setAdapterInterface(AdapterInterface adapterInterface){
        this.adapterInterface = adapterInterface;
    }

    @Override
    public FilesHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FilesHolder(LayoutInflater.from(context).inflate(R.layout.audio_list_item,parent,false));
    }

    @Override
    public void onBindViewHolder(final FilesHolder holder, int i) {

        final int position = holder.getAdapterPosition();
        final AudioFile audioFile = copyToTullies.get(position);

        binderHelper.bind(holder.swipeLayout,audioFile.getId());
        if (holder.swipeLayout.isOpened()){
            holder.swipeLayout.close(true);
        }

        try {
            holder.grid_title.setText(URLDecoder.decode(audioFile.getTitle(), "UTF-8"));
        }
        catch (Exception e){
            holder.grid_title.setText(audioFile.getTitle());
        }

        double size = audioFile.getSize();
        if (size>0){
            size = size / BYTETOMB;
            holder.grid_subtitle.setText(String.format("%.2f", size)+" MB");
        }
        else{
            holder.grid_subtitle.setText("");
        }

        holder.griditemcopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (adapterInterface!=null){
                if (!audioFile.isVideo()){
                    adapterInterface.onCopyToTully(audioFile,position);
                }
                else{
                    adapterInterface.onVideoPlay(audioFile,position);
                }
            }
            }
        });

        holder.griditemcopy.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (adapterInterface!=null){
                    adapterInterface.onLongPress(audioFile,position);
                }
                return true;
            }
        });

        holder.widget_rename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onRename(audioFile,position);
                    holder.swipeLayout.close(true);
                }
            }
        });

        holder.widget_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onShare(audioFile,position);
                    holder.swipeLayout.close(true);
                }
            }
        });

        holder.widget_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (onWidgetAction!=null){
                onWidgetAction.onDelete(audioFile,position);
                holder.swipeLayout.close(true);
            }
            }
        });

        if (audioFile.getId()!=null){
            if (audioFile.getId().equals("-L1111aaaaaaaaaaaaaa") || audioFile.getId().equals("video")){
                holder.grid_icon.setImageResource(R.drawable.master_file_audio_icon);
            }
            else{
                holder.grid_icon.setImageResource(R.drawable.audio_file_icon);
            }
        }
        else if (audioFile.isVideo) holder.grid_icon.setImageResource(R.drawable.master_file_audio_icon);
    }

    public void updateFileAtPos(AudioFile audioFile, int position){
        this.copyToTullies.set(position, audioFile);
        this.notifyItemChanged(position);
    }

    public void updateFile(AudioFile audioFile){
        try{
            int key = 0;
            for (AudioFile a : copyToTullies){
                if (a.getId().equals(audioFile.getId())){
                    copyToTullies.set(key,audioFile);
                    this.notifyItemChanged(key);
                }
                key++;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void showSelection(boolean boo){
        this.selection = boo;
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return copyToTullies.size();
    }

    class FilesHolder extends RecyclerView.ViewHolder{
        private RelativeLayout griditemcopy;
        private TextView grid_title;
        private TextView grid_subtitle;
        private ImageView grid_icon;
        private SwipeRevealLayout swipeLayout;
        private LinearLayout widget_rename, widget_share, widget_delete;

        FilesHolder(View itemView) {
            super(itemView);
            grid_icon = itemView.findViewById(R.id.player_icon);
            griditemcopy = itemView.findViewById(R.id.griditem);
            grid_title = itemView.findViewById(R.id.grid_title);
            grid_subtitle = itemView.findViewById(R.id.grid_subtitle);

            swipeLayout = itemView.findViewById(R.id.swipeLayout);
            widget_rename = itemView.findViewById(R.id.widget_rename);
            widget_share = itemView.findViewById(R.id.widget_share);
            widget_delete = itemView.findViewById(R.id.widget_delete);
        }
    }
}
