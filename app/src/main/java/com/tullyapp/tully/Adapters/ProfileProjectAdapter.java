package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.R;

import java.util.ArrayList;

/**
 * Created by macbookpro on 10/09/17.
 */

public class ProfileProjectAdapter extends RecyclerView.Adapter<ProfileProjectAdapter.ProjectViewHolder> {

    private Context context;
    private ArrayList<Project> projects;

    public ProfileProjectAdapter(Context context, ArrayList<Project> projects) {
        this.context = context;
        this.projects = projects;
    }

    public Project getItemByPosition(int position){
        if (projects!=null && projects.get(position)!=null){
            return projects.get(position);
        }
        return null;
    }

    @Override
    public ProjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ProjectViewHolder(LayoutInflater.from(context).inflate(R.layout.profile_project_item,parent,false));
    }

    @Override
    public void onBindViewHolder(ProjectViewHolder holder, int position) {
        final Project project = projects.get(position);
        holder.tv_projectname.setText(project.getProject_name());
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder{

        private TextView tv_projectname;

        ProjectViewHolder(View itemView) {
            super(itemView);
            tv_projectname = itemView.findViewById(R.id.tv_projectname);
        }
    }
}
