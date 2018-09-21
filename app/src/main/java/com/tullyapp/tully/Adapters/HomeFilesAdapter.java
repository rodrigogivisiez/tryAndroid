package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.R;

import java.net.URLDecoder;
import java.util.ArrayList;

import static com.tullyapp.tully.Utils.Constants.BYTETOMB;

/**
 * Created by macbookpro on 10/09/17.
 */

public class HomeFilesAdapter extends RecyclerView.Adapter<HomeFilesAdapter.FilesHolder>{

    private static final String TAG = HomeFilesAdapter.class.getSimpleName();
    private ArrayList<AudioFile> copyToTullies;
    private Context context;
    private boolean selection=false;
    private boolean checkall = false;
    private boolean isFromHome = false;
    private AdapterInterface adapterInterface;

    public HomeFilesAdapter(Context context, ArrayList<AudioFile> copyToTullies, boolean isFromHome) {
        this.copyToTullies = copyToTullies;
        this.context = context;
        this.isFromHome = isFromHome;
    }

    public interface AdapterInterface{
        void onLongPressedFile(AudioFile audioFile, int position);
        void onCopyToTully(AudioFile audioFile, int position);
        void onVideoPlay(AudioFile audioFile, int position);
    }

    public void setAdapterInterface(AdapterInterface adapterInterface){
        this.adapterInterface = adapterInterface;
    }

    @Override
    public FilesHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FilesHolder(LayoutInflater.from(context).inflate(R.layout.audiofile_grid_item,parent,false));
    }
    @Override
    public void onBindViewHolder(final FilesHolder holder, int i) {

        final int position = holder.getAdapterPosition();
        final AudioFile audioFile = copyToTullies.get(position);

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
                if (!audioFile.isVideo()){
                    adapterInterface.onLongPressedFile(audioFile, position);
                }
            }
            return true;
                }
        });

        if (audioFile.getId()!=null){
            if (audioFile.getId().equals("-L1111aaaaaaaaaaaaaa") || audioFile.getId().equals("video")){
                holder.grid_icon.setImageResource(R.drawable.master_file_audio_icon);
            }
            else if (audioFile.isBeat()){
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

    public void checkAll(boolean boo){
        this.checkall = boo;
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return copyToTullies.size();
    }

    class FilesHolder extends RecyclerView.ViewHolder{
        private LinearLayout griditemcopy;
        private TextView grid_title;
        private TextView grid_subtitle;
        private ImageView grid_icon;

        FilesHolder(View itemView) {
            super(itemView);
            grid_icon = itemView.findViewById(R.id.grid_icon);
            griditemcopy = itemView.findViewById(R.id.griditemcopy);
            grid_title = itemView.findViewById(R.id.grid_title);
            grid_subtitle = itemView.findViewById(R.id.grid_subtitle);
        }
    }

}
