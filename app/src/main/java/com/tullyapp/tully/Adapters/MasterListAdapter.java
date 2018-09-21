package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tullyapp.tully.FirebaseDataModels.Masters;
import com.tullyapp.tully.R;

import java.util.ArrayList;

import static com.tullyapp.tully.Utils.Constants.BYTETOMB;

/**
 * Created by kathan on 28/01/18.
 */

public class MasterListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private final LayoutInflater inflater;
    private ArrayList<Masters> mastersArrayList = new ArrayList<>();
    private static final int FOLDER_TYPE = 1;
    private static final int FILE_TYPE = 2;
    private boolean isFromFragment = true;
    private boolean isFromHome = false;
    private ItemTap itemTap;

    public interface ItemTap{
        void onFileTap(ArrayList<Masters> mastersArrayList, int position);
        void onFolderTap(Masters masterNode, int position);
        void onLongPress(Masters masterNode, int position);
    }

    public void setItemTapListener(ItemTap itemTap){
        this.itemTap = itemTap;
    }

    public MasterListAdapter(Context context, boolean isFromFragment, boolean isFromHome) {
        this.isFromFragment = isFromFragment;
        this.isFromHome = isFromHome;
        inflater = LayoutInflater.from(context);
    }

    public void remove(int position){
        this.mastersArrayList.remove(position);
        this.notifyItemRemoved(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {

        switch (i){
            case FOLDER_TYPE:
                //return new MasterFolderViewHolder(inflater.inflate(R.layout.project_grid_item, viewGroup, false));
                if (isFromHome)
                    return new MasterFolderViewHolder(inflater.inflate(R.layout.project_grid_item, viewGroup, false));
                else
                    return new MasterFolderViewHolder(inflater.inflate(R.layout.grid_item_center_folder_dark, viewGroup, false));

            case FILE_TYPE:
                //return new MasterFileViewHolder(inflater.inflate(R.layout.project_grid_item, viewGroup, false));
                if (isFromHome)
                    return new MasterFileViewHolder(inflater.inflate(R.layout.project_grid_item, viewGroup, false));
                else
                    return new MasterFileViewHolder(inflater.inflate(R.layout.grid_item_center_file_dark, viewGroup, false));
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
        LinearLayout griditem;
        TextView grid_title;
        TextView grid_subtitle;
        ImageView grid_icon;

        MasterFolderViewHolder(View itemView) {
            super(itemView);
            griditem = itemView.findViewById(R.id.griditem);
            grid_icon = itemView.findViewById(R.id.grid_icon);
            grid_title = itemView.findViewById(R.id.grid_title);
            grid_subtitle = itemView.findViewById(R.id.grid_subtitle);
        }

        void init(int position){
            final int pos = position;
            final Masters masters = mastersArrayList.get(position);
            grid_icon.setImageResource(R.drawable.master_folder_icon);
            grid_title.setText(masters.getName());
            grid_subtitle.setText(masters.getCount()+" Items");
            griditem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (isFromFragment){
                    if (itemTap!=null){
                        itemTap.onFolderTap(masters,pos);
                    }
                }
                else{
                    if (itemTap!=null){
                        itemTap.onFolderTap(masters,pos);
                    }
                }
                }
            });

            griditem.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                if (itemTap!=null){
                    itemTap.onLongPress(masters,pos);
                }
                return false;
                }
            });
        }
    }

    class MasterFileViewHolder extends RecyclerView.ViewHolder{
        LinearLayout griditemcopy;
        TextView grid_title;
        TextView grid_subtitle;
        ImageView grid_icon;

        MasterFileViewHolder(View itemView) {
            super(itemView);
            griditemcopy = itemView.findViewById(R.id.griditem);
            grid_title = itemView.findViewById(R.id.grid_title);
            grid_subtitle = itemView.findViewById(R.id.grid_subtitle);
            grid_icon = itemView.findViewById(R.id.grid_icon);
        }

        void init(int position){
            final int pos = position;
            final Masters masters = mastersArrayList.get(position);
            grid_title.setText(masters.getName());
            double size = masters.getSize();
            grid_icon.setImageResource(R.drawable.master_file_audio_icon);
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
                return false;
                }
            });
        }
    }
}
