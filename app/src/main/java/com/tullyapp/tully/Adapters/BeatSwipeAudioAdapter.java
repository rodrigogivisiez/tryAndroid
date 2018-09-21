package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.tullyapp.tully.FirebaseDataModels.BeatAudio;
import com.tullyapp.tully.R;

import java.util.ArrayList;

import static com.tullyapp.tully.Utils.Constants.BYTETOMB;

public class BeatSwipeAudioAdapter extends RecyclerView.Adapter<BeatSwipeAudioAdapter.FilesHolder>{

    private static final String TAG = BeatAudioAdapter.class.getSimpleName();
    private ArrayList<BeatAudio> beatAudioArrayList;
    private Context context;
    private boolean selection=false;
    private boolean checkall = false;
    private boolean isFromHome = false;
    private AdapterInterface adapterInterface;

    private OnWidgetAction onWidgetAction;
    private ViewBinderHelper binderHelper;

    public interface OnWidgetAction{
        void onShare(BeatAudio beatAudio, int position);
        void onRename(BeatAudio beatAudio, int position);
        void onDelete(BeatAudio beatAudio, int position);
    }

    public void setOnWidgetAction(OnWidgetAction onWidgetAction){
        this.onWidgetAction = onWidgetAction;
    }

    public BeatSwipeAudioAdapter(Context context, boolean isFromHome) {
        this.beatAudioArrayList = new ArrayList<>();
        this.context = context;
        this.isFromHome = isFromHome;
        this.binderHelper = new ViewBinderHelper();
        this.binderHelper.setOpenOnlyOne(true);
    }

    public interface AdapterInterface{
        void onLongPressedFile(BeatAudio beatAudio, int position);
        void onBeat(BeatAudio beatAudio, int position);
    }

    public void setAdapterInterface(AdapterInterface adapterInterface){
        this.adapterInterface = adapterInterface;
    }

    public void add(BeatAudio beatAudio){
        this.beatAudioArrayList.add(beatAudio);
    }

    public void clear(){
        this.beatAudioArrayList.clear();
    }

    @Override
    public FilesHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FilesHolder(LayoutInflater.from(context).inflate(R.layout.master_swipe_file,parent,false));
    }

    @Override
    public void onBindViewHolder(final FilesHolder holder, int i) {

        final int position = holder.getAdapterPosition();
        final BeatAudio beatAudio = beatAudioArrayList.get(position);

        binderHelper.bind(holder.swipeLayout,beatAudio.getId());
        if (holder.swipeLayout.isOpened()){
            holder.swipeLayout.close(true);
        }

        holder.grid_title.setText(beatAudio.getTitle());

        double size = beatAudio.getSize();
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
                    adapterInterface.onBeat(beatAudio,position);
                }
            }
        });

        holder.griditemcopy.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (adapterInterface!=null){
                    if (!beatAudio.isVideo()){
                        adapterInterface.onLongPressedFile(beatAudio, position);
                    }
                }
                return true;
            }
        });

        holder.widget_rename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (onWidgetAction!=null){
                onWidgetAction.onRename(beatAudio,position);
                holder.swipeLayout.close(true);
            }
            }
        });

        holder.widget_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (onWidgetAction!=null){
                onWidgetAction.onShare(beatAudio,position);
                holder.swipeLayout.close(true);
            }
            }
        });

        holder.widget_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (onWidgetAction!=null){
                onWidgetAction.onDelete(beatAudio,position);
                holder.swipeLayout.close(true);
            }
            }
        });
    }

    public void updateFileAtPos(BeatAudio beatAudio, int position){
        this.beatAudioArrayList.set(position, beatAudio);
        this.notifyItemChanged(position);
    }

    public void showSelection(boolean boo){
        this.selection = boo;
        this.notifyDataSetChanged();
    }


    public void checkAll(boolean boo){
        this.checkall = boo;
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return beatAudioArrayList.size();
    }

    class FilesHolder extends RecyclerView.ViewHolder{
        private RelativeLayout griditemcopy;
        private TextView grid_title;
        private TextView grid_subtitle;
        SwipeRevealLayout swipeLayout;
        LinearLayout widget_rename, widget_share, widget_delete;

        FilesHolder(View itemView) {
            super(itemView);
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
