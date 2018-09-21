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
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.R;

import java.util.ArrayList;
import java.util.Map;

public class HomeProjectsSwipeViewAdapter extends RecyclerView.Adapter<HomeProjectsSwipeViewAdapter.HomeProjects> {
    private ArrayList<Project> list;
    private Context context;
    private boolean selection=false;
    private boolean checkall = false;
    private AdapterInterface adapterInterface;
    private OnWidgetAction onWidgetAction;
    private ViewBinderHelper binderHelper;

    public HomeProjectsSwipeViewAdapter(Context context, ArrayList<Project> list) {
        this.list = list;
        this.context = context;
        this.binderHelper = new ViewBinderHelper();
        this.binderHelper.setOpenOnlyOne(true);
    }

    public interface AdapterInterface{
        void onProjectTap(Project project, int i);
        void onLongPress(Project project, int i);
    }

    public interface OnWidgetAction{
        void onShare(Project project, int position);
        void onRename(Project project, int position);
        void onDelete(Project project, int position);
    }

    public void setOnWidgetAction(OnWidgetAction onWidgetAction){
        this.onWidgetAction = onWidgetAction;
    }

    public void setAdapterInterface(AdapterInterface adapterInterface){
        this.adapterInterface = adapterInterface;
    }

    @Override
    public HomeProjects onCreateViewHolder(ViewGroup parent, int viewType) {
        return new HomeProjects(LayoutInflater.from(context).inflate(R.layout.project_folder_horizontal, parent, false));
    }

    @Override
    public void onBindViewHolder(final HomeProjects holder, final int i) {
        final int position = holder.getAdapterPosition();
        final Project project = list.get(position);

        holder.grid_title.setText(project.getProject_name());
        holder.grid_subtitle.setText(project.getItemcount()+" items");

        holder.griditem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (adapterInterface!=null){
                adapterInterface.onProjectTap(project, position);
            }
            }
        });

        holder.griditem.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (adapterInterface!=null){
                    adapterInterface.onLongPress(project,position);
                }
                return true;
            }
        });

        binderHelper.bind(holder.swipeLayout,project.getId());

        if (holder.swipeLayout.isOpened()){
            holder.swipeLayout.close(true);
        }

        holder.widget_rename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onRename(project,position);
                    holder.swipeLayout.close(true);
                }
            }
        });

        holder.widget_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onShare(project,position);
                    holder.swipeLayout.close(true);
                }
            }
        });

        holder.widget_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWidgetAction!=null){
                    onWidgetAction.onDelete(project,position);
                    holder.swipeLayout.close(true);
                }
            }
        });

    }

    public void showSelection(boolean boo){
        this.selection = boo;
        this.notifyDataSetChanged();
    }

    public void updateProjectAtPos(Project project, int position){
        this.list.set(position,project);
        this.notifyItemChanged(position);
    }


    public void updateProjectRecording(Recording recording){
        try{
            int key = 0;
            for (Project p : list){
                if (p.getId().equals(recording.getProjectId())){
                    for (Object o : p.getRecordings().entrySet()){
                        Map.Entry pair = (Map.Entry) o;
                        if (pair.getKey().toString().equals(recording.getId())){
                            p.getRecordings().put(pair.getKey().toString(),recording);
                            this.notifyItemChanged(key);
                            break;
                        }
                    }
                }
                key++;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public int getItemCount() {
        return list.size();
    }

    class HomeProjects extends RecyclerView.ViewHolder{

        private RelativeLayout griditem;
        private TextView grid_title;
        private TextView grid_subtitle;
        LinearLayout widget_rename, widget_share, widget_delete;
        SwipeRevealLayout swipeLayout;

        HomeProjects(View itemView) {
            super(itemView);
            griditem = itemView.findViewById(R.id.griditem);
            grid_title = itemView.findViewById(R.id.grid_title);
            grid_subtitle = itemView.findViewById(R.id.grid_subtitle);

            swipeLayout = itemView.findViewById(R.id.swipeLayout);
            widget_rename = itemView.findViewById(R.id.widget_rename);
            widget_share = itemView.findViewById(R.id.widget_share);
            widget_delete = itemView.findViewById(R.id.widget_delete);
        }
    }
}
