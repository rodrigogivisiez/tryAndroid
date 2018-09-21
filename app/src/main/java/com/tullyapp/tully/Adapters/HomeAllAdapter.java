package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.R;

import java.util.ArrayList;
import java.util.List;

import static com.tullyapp.tully.Utils.Constants.BYTETOMB;

/**
 * Created by macbookpro on 04/09/17.
 */

public class HomeAllAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    Context context;
    private List<Project> list;
    private List<AudioFile> copyToTullies;
    private boolean selection=false;
    private boolean checkall = false;
    private static final int TYPE_PROJECT = 0;
    private static final int TYPE_COPYTOTYLLY = 1;
    private AdapterInterface adapterInterface;

    public HomeAllAdapter(Context context, List<Project> list, List<AudioFile> copyToTullies) {
        this.context = context;
        this.list = list;
        this.copyToTullies = copyToTullies;
    }

    public interface AdapterInterface{
        void OnlongPressedProject(Project project, int position);
        void onLongPressedFile(AudioFile audioFile, int position);
        void onProjectTap(Project project, int position);
        void onCopyToTully(AudioFile audioFile, int position);
    }

    public void setAdapterInterface(AdapterInterface adapterInterface){
        this.adapterInterface = adapterInterface;
    }

    public void setProjectList(ArrayList<Project> projects){
        this.list = projects;
    }

    public void setCopyToTullies(ArrayList<AudioFile> copyToTullies){
        this.copyToTullies = copyToTullies;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType==TYPE_PROJECT)
            return new HomeAll(LayoutInflater.from(context).inflate(R.layout.project_grid_item, parent, false));
        else if (viewType==TYPE_COPYTOTYLLY)
            return new CopyToTullyHolder(LayoutInflater.from(context).inflate(R.layout.audiofile_grid_item, parent, false));

        return null;

    }

    @Override
    public int getItemViewType(int position) {

        if(position < list.size()){
            return TYPE_PROJECT;
        }

        if(position - list.size() < copyToTullies.size()){
            return TYPE_COPYTOTYLLY;
        }

        return -1;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int i) {

        final int position = holder.getAdapterPosition();

        if (holder instanceof HomeAll) {
            final Project project = list.get(position);
            HomeAll viewHolder = (HomeAll) holder;

            viewHolder.grid_title.setText(project.getProject_name());
            viewHolder.grid_subtitle.setText(project.getItemcount()+" items");

            viewHolder.grid_check.setChecked(this.checkall);

            if (selection){
                viewHolder.grid_check.setVisibility(View.VISIBLE);
            }else {
                viewHolder.grid_check.setVisibility(View.GONE);
            }
            viewHolder.griditem.setOnClickListener(new View.OnClickListener() {
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

            viewHolder.grid_check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    project.setChecked(isChecked);
                }
            });

            viewHolder.griditem.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                if (adapterInterface!=null){
                    adapterInterface.OnlongPressedProject(project, position);
                }
                return false;
                }
            });

        } else if (holder instanceof CopyToTullyHolder) {

            final AudioFile audioFile = copyToTullies.get(position - list.size());

            if (audioFile !=null){
                CopyToTullyHolder viewHolder = (CopyToTullyHolder) holder;
                viewHolder.grid_title.setText(audioFile.getTitle());
                double size = audioFile.getSize();
                size = size / BYTETOMB;
                viewHolder.grid_subtitle.setText(String.format("%.2f", size)+" MB");

                viewHolder.grid_check_copy.setChecked(this.checkall);

                if (selection){
                    viewHolder.grid_check_copy.setVisibility(View.VISIBLE);
                }else {
                    viewHolder.grid_check_copy.setVisibility(View.GONE);
                }

                viewHolder.griditemcopy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    AppCompatCheckBox chk = v.findViewById(R.id.grid_check_copy);
                    if (chk.getVisibility()==View.GONE){
                        if (adapterInterface!=null){
                            adapterInterface.onCopyToTully(audioFile,position);
                        }
                    }else{
                        chk.setChecked(!chk.isChecked());
                    }
                    }
                });

                viewHolder.grid_check_copy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    audioFile.setChecked(isChecked);
                    }
                });

                viewHolder.griditemcopy.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                    if (adapterInterface!=null){
                        adapterInterface.onLongPressedFile(audioFile, position - list.size());
                    }
                    return false;
                    }
                });
            }

        }
    }

    public void showSelection(boolean boo){
        this.selection = boo;
        this.notifyDataSetChanged();
    }


    public void checkAll(boolean boo){
        this.checkall = boo;
        this.notifyDataSetChanged();
    }

    public void updateProjectAtPos(Project project, int position){
        list.set(position, project);
        this.notifyDataSetChanged();
    }

    public void updateFileAtPos(AudioFile audioFile, int position){
        copyToTullies.set(position, audioFile);
        this.notifyDataSetChanged();
    }

    public ArrayList<Project> getSelectedProjectsList(){
        ArrayList<Project> projects = new ArrayList<>();
        for (Project project : list){
            if (project.isChecked()){
                projects.add(project);
            }
        }

        return projects;
    }

    public ArrayList<AudioFile> getSelectedCopyToTullies(){
        ArrayList<AudioFile> audioFileList = new ArrayList<>();
        for (AudioFile audioFile : copyToTullies){
            if (audioFile.isChecked()){
                audioFileList.add(audioFile);
            }
        }

        return audioFileList;
    }


    @Override
    public int getItemCount() {
        return list.size() + copyToTullies.size();
    }

    public void clearList(){
        this.list.clear();
        this.copyToTullies.clear();
        this.notifyDataSetChanged();
    }


    public class HomeAll extends RecyclerView.ViewHolder{

        private LinearLayout griditem;
        private TextView grid_title;
        private TextView grid_subtitle;
        private AppCompatCheckBox grid_check;

        HomeAll(View itemView) {
            super(itemView);
            griditem = itemView.findViewById(R.id.griditem);
            grid_title = itemView.findViewById(R.id.grid_title);
            grid_subtitle = itemView.findViewById(R.id.grid_subtitle);
            grid_check = itemView.findViewById(R.id.grid_check);
        }
    }

    public class CopyToTullyHolder extends RecyclerView.ViewHolder{
        private LinearLayout griditemcopy;
        private TextView grid_title;
        private TextView grid_subtitle;
        private AppCompatCheckBox grid_check_copy;

        CopyToTullyHolder(View itemView) {
            super(itemView);
            griditemcopy = itemView.findViewById(R.id.griditemcopy);
            grid_title = itemView.findViewById(R.id.grid_title);
            grid_subtitle = itemView.findViewById(R.id.grid_subtitle);
            grid_check_copy = itemView.findViewById(R.id.grid_check_copy);
        }
    }
}
