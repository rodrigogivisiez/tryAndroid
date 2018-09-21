package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tullyapp.tully.FirebaseDataModels.AudioFile;
import com.tullyapp.tully.R;

import java.util.ArrayList;

import static com.tullyapp.tully.Utils.Constants.BYTETOMB;

/**
 * Created by macbookpro on 20/09/17.
 */

public class PlayCPListAdapter extends RecyclerView.Adapter<PlayCPListAdapter.PlayerListViewHolder> {

    private Context context;
    private ArrayList<AudioFile> copyToTullies;
    private AdapterInterface adapterListener;

    public PlayCPListAdapter(Context context, ArrayList<AudioFile> copyToTullies) {
        this.context = context;
        this.copyToTullies = copyToTullies;
    }

    public interface AdapterInterface{
        void onFileTap(AudioFile audioFile, int position);
    }

    public void setAdapterListener(AdapterInterface adapterListener){
        this.adapterListener = adapterListener;
    }

    @Override
    public PlayerListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PlayerListViewHolder(LayoutInflater.from(context).inflate(R.layout.play_copytully_list,parent,false));
    }

    @Override
    public void onBindViewHolder(final PlayerListViewHolder holder, int i) {
        final int position = holder.getAdapterPosition();
        final AudioFile audioFile = copyToTullies.get(position);
        holder.tv_file_title.setText(audioFile.getTitle());
        holder.tv_index.setText(String.valueOf(position+1));
        holder.list_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (adapterListener!=null){
                audioFile.setChecked(true);
                adapterListener.onFileTap(audioFile,position);
            }
            }
        });

        double size = audioFile.getSize();
        size = size / BYTETOMB;
        holder.tv_file_size.setText(String.format("%.2f", size)+" MB");
    }

    @Override
    public int getItemCount() {
        return copyToTullies.size();
    }


    public void showSelection(boolean boo){
        notifyDataSetChanged();
    }


    class PlayerListViewHolder extends RecyclerView.ViewHolder{

        TextView tv_file_title;
        TextView tv_index;
        TextView tv_file_size;
        RelativeLayout list_item;

        PlayerListViewHolder(View itemView) {
            super(itemView);
            tv_file_title = itemView.findViewById(R.id.tv_file_title);
            list_item = itemView.findViewById(R.id.list_item);
            tv_file_size = itemView.findViewById(R.id.tv_file_size);
            tv_index = itemView.findViewById(R.id.tv_index);
        }
    }
}
