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
import com.tullyapp.tully.FirebaseDataModels.Masters;
import com.tullyapp.tully.R;

import java.util.ArrayList;

import static com.tullyapp.tully.Utils.Constants.BYTETOMB;

public class MastersSwipeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private static final String TAG = MastersSwipeAdapter.class.getSimpleName();
    private final LayoutInflater inflater;
    private ArrayList<Masters> mastersArrayList = new ArrayList<>();
    private static final int FOLDER_TYPE = 1;
    private static final int FILE_TYPE = 2;
    private boolean isFromFragment = true;
    private boolean isFromHome = false;
    private ItemTap itemTap;
    private OnWidgetAction onWidgetAction;
    private ViewBinderHelper binderHelper;

    public interface OnWidgetAction{
        void onShare(Masters masters, int position);
        void onRename(Masters masters, int position);
        void onDelete(Masters masters, int position);
    }

    public void setOnWidgetAction(OnWidgetAction onWidgetAction){
        this.onWidgetAction = onWidgetAction;
    }

    public interface ItemTap{
        void onFileTap(ArrayList<Masters> mastersArrayList, int position);
        void onFolderTap(Masters masterNode, int position);
        void onLongPress(Masters masterNode, int position);
    }

    public void setItemTapListener(ItemTap itemTap){
        this.itemTap = itemTap;
    }

    public MastersSwipeAdapter(Context context, boolean isFromFragment, boolean isFromHome) {
        this.isFromFragment = isFromFragment;
        this.isFromHome = isFromHome;
        inflater = LayoutInflater.from(context);
        this.binderHelper = new ViewBinderHelper();
        this.binderHelper.setOpenOnlyOne(true);
    }

    public void remove(int position){
        this.mastersArrayList.remove(position);
        this.notifyItemRemoved(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {

        switch (i){
            case FOLDER_TYPE:
                return new MasterFolderViewHolder(inflater.inflate(R.layout.master_swipe_folder, viewGroup, false));
                /*if (isFromHome)
                    return new MasterFolderViewHolder(inflater.inflate(R.layout.project_folder_home, viewGroup, false));
                else
                    return new MasterFolderViewHolder(inflater.inflate(R.layout.project_grid_item, viewGroup, false));*/

            case FILE_TYPE:
                return new MasterFileViewHolder(inflater.inflate(R.layout.master_swipe_file, viewGroup, false));
                /*if (isFromHome)
                    return new MasterFileViewHolder(inflater.inflate(R.layout.audio_file_home, viewGroup, false));
                else
                    return new MasterFileViewHolder(inflater.inflate(R.layout.audiofile_grid_item, viewGroup, false));*/
        }

        return null;
    }

    public void updateAtPos(Masters masterNode, int position){
        this.mastersArrayList.set(position,masterNode);
        this.notifyItemChanged(position);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if (viewHolder instanceof MasterFolderViewHolder){
            MasterFolderViewHolder masterFolderViewHolder = (MasterFolderViewHolder) viewHolder;
            masterFolderViewHolder.init(viewHolder.getAdapterPosition());
        }
        else{
            MasterFileViewHolder masterFileViewHolder = (MasterFileViewHolder) viewHolder;
            masterFileViewHolder.init(viewHolder.getAdapterPosition());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mastersArrayList!=null && mastersArrayList.size()>0){
            if (mastersArrayList.get(position).getType().equals("folder")){
                return FOLDER_TYPE;
            }
            else{
                return FILE_TYPE;
            }
        }
        else{
            return -1;
        }
    }

    public void add(Masters masters){
        this.mastersArrayList.add(masters);
    }

    public void clearData(){
        this.mastersArrayList.clear();
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mastersArrayList.size();
    }

    class MasterFolderViewHolder extends RecyclerView.ViewHolder{
        RelativeLayout griditem;
        TextView grid_title;
        TextView grid_subtitle;
        SwipeRevealLayout swipeLayout;
        LinearLayout widget_rename, widget_share, widget_delete;

        MasterFolderViewHolder(View itemView) {
            super(itemView);
            griditem = itemView.findViewById(R.id.griditem);
            grid_title = itemView.findViewById(R.id.grid_title);
            grid_subtitle = itemView.findViewById(R.id.grid_subtitle);
            swipeLayout = itemView.findViewById(R.id.swipeLayout);
            widget_rename = itemView.findViewById(R.id.widget_rename);
            widget_share = itemView.findViewById(R.id.widget_share);
            widget_delete = itemView.findViewById(R.id.widget_delete);
        }

        void init(int position){
            final int pos = position;
            final Masters masters = mastersArrayList.get(position);

            binderHelper.bind(swipeLayout,masters.getId());
            if (swipeLayout.isOpened()){
                swipeLayout.close(true);
            }

            grid_title.setText(masters.getName());
            grid_subtitle.setText(masters.getCount()+" Items");
            griditem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (itemTap!=null){
                        itemTap.onFolderTap(masters,pos);
                    }
                }
            });

            griditem.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                if (itemTap!=null){
                    itemTap.onLongPress(masters,pos);
                }
                return true;
                }
            });

            widget_rename.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onRename(masters,pos);
                    swipeLayout.close(true);
                }
                }
            });

            widget_share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onShare(masters,pos);
                    swipeLayout.close(true);
                }
                }
            });

            widget_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onDelete(masters,pos);
                    swipeLayout.close(true);
                }
                }
            });
        }
    }

    class MasterFileViewHolder extends RecyclerView.ViewHolder{
        RelativeLayout griditemcopy;
        TextView grid_title;
        TextView grid_subtitle;
        SwipeRevealLayout swipeLayout;
        LinearLayout widget_rename, widget_share, widget_delete;

        MasterFileViewHolder(View itemView) {
            super(itemView);
            griditemcopy = itemView.findViewById(R.id.griditem);
            grid_title = itemView.findViewById(R.id.grid_title);
            grid_subtitle = itemView.findViewById(R.id.grid_subtitle);
            swipeLayout = itemView.findViewById(R.id.swipeLayout);
            widget_rename = itemView.findViewById(R.id.widget_rename);
            widget_share = itemView.findViewById(R.id.widget_share);
            widget_delete = itemView.findViewById(R.id.widget_delete);
        }

        void init(int position){
            final int pos = position;
            final Masters masters = mastersArrayList.get(position);

            binderHelper.bind(swipeLayout,masters.getId());
            if (swipeLayout.isOpened()){
                swipeLayout.close(true);
            }

            grid_title.setText(masters.getName());
            double size = masters.getSize();
            size = size / BYTETOMB;
            grid_subtitle.setText(String.format("%.2f", size)+" MB");
            griditemcopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (itemTap!=null){
                    itemTap.onFileTap(mastersArrayList,pos);
                }
                }
            });

            griditemcopy.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                if (itemTap!=null){
                    itemTap.onLongPress(masters,pos);
                }
                return true;
                }
            });

            widget_rename.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onRename(masters,pos);
                    swipeLayout.close(true);
                }
                }
            });

            widget_share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onShare(masters,pos);
                    swipeLayout.close(true);
                }
                }
            });

            widget_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onDelete(masters,pos);
                    swipeLayout.close(true);
                }
                }
            });
        }
    }
}
