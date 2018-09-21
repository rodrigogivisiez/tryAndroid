package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tullyapp.tully.Models.YouTubeVideo;
import com.tullyapp.tully.R;

import java.util.ArrayList;

/**
 * Created by apple on 07/12/17.
 */

public class YouTubeListAdapter extends RecyclerView.Adapter<YouTubeListAdapter.YouTubeHolder>{

    private Context context;

    private ArrayList<YouTubeVideo> youTubeVideosArrayList;

    public YouTubeListAdapter(Context context) {
        this.context = context;
        this.youTubeVideosArrayList = new ArrayList<>();
    }

    @Override
    public YouTubeHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new YouTubeHolder(LayoutInflater.from(context).inflate(R.layout.youtube_video_item,viewGroup,false));
    }

    public void add(YouTubeVideo youTubeVideo){
        youTubeVideosArrayList.add(youTubeVideo);
        notifyDataSetChanged();
    }

    public void clearList(){
        this.youTubeVideosArrayList.clear();
        this.notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(YouTubeHolder holder, int position) {
        YouTubeVideo youTubeVideo = youTubeVideosArrayList.get(position);

        holder.title.setText(youTubeVideo.getTitle());
        holder.description.setText(youTubeVideo.getDescription());

        try{
            Picasso.with(context).load(youTubeVideo.getThumbnail()).placeholder(R.color.colorAccent).into(holder.imageView);
        }catch (Exception e){

        }
    }

    @Override
    public int getItemCount() {
        return youTubeVideosArrayList.size();
    }

    public class YouTubeHolder extends RecyclerView.ViewHolder{

        public ImageView imageView;
        public TextView title;
        public TextView description;

        public YouTubeHolder(View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.tv_title);
            description = itemView.findViewById(R.id.tv_desc);
        }
    }
}
