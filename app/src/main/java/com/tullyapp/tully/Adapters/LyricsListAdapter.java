package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.tullyapp.tully.FirebaseDataModels.LyricsModule.LyricsAppModel;
import com.tullyapp.tully.R;

import java.util.ArrayList;

/**
 * Created by macbookpro on 13/09/17.
 */

public class LyricsListAdapter extends RecyclerView.Adapter<LyricsListAdapter.LyricsListViewHolder>{

    private static final String TAG = LyricsListAdapter.class.getSimpleName();
    private ArrayList<LyricsAppModel> lyricsAppModels;
    private Context context;
    private boolean selection = false;
    private AdapterInterface adapterListener;

    private ViewBinderHelper binderHelper;
    private OnWidgetAction onWidgetAction;

    public interface OnWidgetAction{
        void onShare(LyricsAppModel lyricsAppModel, int position);
        void onDelete(LyricsAppModel lyricsAppModel, int position);
    }
    public void setOnWidgetAction(OnWidgetAction onWidgetAction){
        this.onWidgetAction = onWidgetAction;
    }

    public LyricsListAdapter(Context context,ArrayList<LyricsAppModel> lyricsAppModels) {
        this.lyricsAppModels = lyricsAppModels;
        this.context = context;
        this.binderHelper = new ViewBinderHelper();
        this.binderHelper.setOpenOnlyOne(true);
    }

    public interface AdapterInterface{
        void onLyricsTap(LyricsAppModel lyricsAppModel, int position);
        void onLongPress(LyricsAppModel lyricsAppModel, int position);
    }

    public void setAdapterListener(AdapterInterface adapterListener){
        this.adapterListener = adapterListener;
    }

    @Override
    public LyricsListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LyricsListViewHolder(LayoutInflater.from(context).inflate(R.layout.lyrics_list_item,parent,false));
    }

    @Override
    public void onBindViewHolder(final LyricsListViewHolder holder, final int position) {
        final LyricsAppModel lyricsAppModel = lyricsAppModels.get(holder.getAdapterPosition());
        boolean isOfProject = lyricsAppModel.isOfProject();
        if (isOfProject){
            holder.tv_projectname.setText(lyricsAppModel.getProjectName());
            holder.tv_projectname.setTextColor(context.getResources().getColor(R.color.colorPrimary));
        }else{
            holder.tv_projectname.setText(context.getString(R.string.no_project_assigned));
            holder.tv_projectname.setTextColor(context.getResources().getColor(R.color.colorAccent));
        }

        holder.widget_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onShare(lyricsAppModel,holder.getAdapterPosition());
                    holder.swipeLayout.close(true);
                }
            }
        });

        holder.widget_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onDelete(lyricsAppModel,holder.getAdapterPosition());
                    holder.swipeLayout.close(true);
                }
            }
        });

        binderHelper.bind(holder.swipeLayout,lyricsAppModel.getLyrics().getId());
        if (holder.swipeLayout.isOpened()){
            holder.swipeLayout.close(true);
        }

        holder.tv_lyrics_excerpt.setText(lyricsAppModel.getLyrics().getDesc());
        if (selection){
            holder.selectItem.setVisibility(View.VISIBLE);
        }else{
            holder.selectItem.setVisibility(View.GONE);
        }

        holder.selectItem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            lyricsAppModel.setChecked(isChecked);
            }
        });

        holder.list_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (holder.selectItem.getVisibility()==View.VISIBLE){
                boolean boo = !holder.selectItem.isChecked();
                lyricsAppModel.setChecked(boo);
                holder.selectItem.setChecked(boo);
            }
            else{
                if (adapterListener!=null){
                    adapterListener.onLyricsTap(lyricsAppModel,holder.getAdapterPosition());
                }
            }
            }
        });

        holder.list_item.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (adapterListener!=null){
                    adapterListener.onLongPress(lyricsAppModel,holder.getAdapterPosition());
                }
                return true;
            }
        });

        if (lyricsAppModel.isChecked()){
            holder.selectItem.setChecked(true);
        }
        else{
            holder.selectItem.setChecked(false);
        }
    }

    @Override
    public int getItemCount() {
        return lyricsAppModels.size();
    }

    public void setLyricsAppModels(ArrayList<LyricsAppModel> lyricsAppModels) {
        this.lyricsAppModels = lyricsAppModels;
    }

    public ArrayList<LyricsAppModel> getSelectedLyrics(){
        ArrayList<LyricsAppModel> lyricsAppModelArrayList = new ArrayList<>();
        for (LyricsAppModel lyricsAppModel : lyricsAppModels){
            if (lyricsAppModel.isChecked()){
                lyricsAppModelArrayList.add(lyricsAppModel);
            }
        }
        return lyricsAppModelArrayList;
    }

    public void showSelection(boolean boo){
        this.selection = boo;
        for (LyricsAppModel la : lyricsAppModels){
            la.setChecked(false);
        }
        notifyDataSetChanged();
    }

    public void setCheckedAll(boolean boo){
        for (LyricsAppModel la : lyricsAppModels){
            la.setChecked(boo);
        }
        notifyDataSetChanged();
    }

    public void clearList(){
        lyricsAppModels.clear();
        notifyDataSetChanged();
    }

    class LyricsListViewHolder extends RecyclerView.ViewHolder{
        SwipeRevealLayout swipeLayout;
        TextView tv_projectname;
        TextView tv_lyrics_excerpt;
        AppCompatCheckBox selectItem;
        RelativeLayout list_item;
        LinearLayout widget_share, widget_delete;

        LyricsListViewHolder(View itemView) {
            super(itemView);
            swipeLayout = itemView.findViewById(R.id.swipeLayout);
            tv_projectname = itemView.findViewById(R.id.tv_projectname);
            tv_lyrics_excerpt = itemView.findViewById(R.id.tv_lyrics_excerpt);
            selectItem = itemView.findViewById(R.id.selectItem);
            list_item = itemView.findViewById(R.id.list_item);

            widget_share = itemView.findViewById(R.id.widget_share);
            widget_delete = itemView.findViewById(R.id.widget_delete);
        }
    }
}
