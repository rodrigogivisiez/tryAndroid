package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.FirebaseDataModels.Recording;
import com.tullyapp.tully.R;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by macbookpro on 05/09/17.
 */

public class HomeProjectsAdapter extends RecyclerView.Adapter<HomeProjectsAdapter.HomeProjects> {

    private ArrayList<Project> list;
    private Context context;
    private boolean selection=false;
    private boolean checkall = false;
    private AdapterInterface adapterInterface;
    private boolean isFromHome = false;

    public HomeProjectsAdapter(Context context, ArrayList<Project> list, boolean isFromHome) {
        this.list = list;
        this.context = context;
        this.isFromHome = isFromHome;
    }

    public interface AdapterInterface{
        void OnlongPressedProject(Project project, int position);
        void onProjectTap(Project project, int i);
    }

    public void setAdapterInterface(AdapterInterface adapterInterface){
        this.adapterInterface = adapterInterface;
    }

    @Override
    public HomeProjects onCreateViewHolder(ViewGroup parent, int viewType) {
        return new HomeProjects(LayoutInflater.from(context).inflate(R.layout.project_folder_home, parent, false));
    }

    @Override
    public void onBindViewHolder(HomeProjects holder, final int i) {
        final int position = holder.getAdapterPosition();
        final Project project = list.get(position);

        holder.grid_title.setText(project.getProject_name());
        holder.grid_subtitle.setText(project.getItemcount()+" items");

        holder.griditem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            AppCompatCheckBox chk = v.findViewById(R.id.grid_check);
            if (chk.getVisibility()==View.GONE){
                if (adapterInterface!=null){
                    adapterInterface.onProjectTap(project, position);
                }
            }else{
                chk.setChecked(!chk.isChecked());
            }
            }
        });

        holder.griditem.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
            if (adapterInterface!=null){
                adapterInterface.OnlongPressedProject(project,position);
            }
            return false;
            }
        });

    }

    public void showSelection(boolean boo){
        this.selection = boo;
        this.notifyDataSetChanged();
    }

    public void updateProjectAtPos(Project project, int position){
        try{
            this.list.set(position,project);
            this.notifyItemChanged(position);
        }catch (Exception e){
            e.printStackTrace();
        }
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

        private LinearLayout griditem;
        private TextView grid_title;
        private TextView grid_subtitle;

        HomeProjects(View itemView) {
            super(itemView);
            griditem = itemView.findViewById(R.id.griditem);
            grid_title = itemView.findViewById(R.id.grid_title);
            grid_subtitle = itemView.findViewById(R.id.grid_subtitle);
        }
    }
}
