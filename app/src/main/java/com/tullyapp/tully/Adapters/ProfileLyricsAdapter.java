package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tullyapp.tully.FirebaseDataModels.Lyrics;
import com.tullyapp.tully.R;

import java.util.ArrayList;

/**
 * Created by macbookpro on 10/09/17.
 */

public class ProfileLyricsAdapter extends RecyclerView.Adapter<ProfileLyricsAdapter.LyricsViewHolder> {

    private Context context;
    private ArrayList<Lyrics> lyricses;
    private Lyrics lyrics;

    public ProfileLyricsAdapter(Context context, ArrayList<Lyrics> lyricses) {
        this.context = context;
        this.lyricses = lyricses;
    }

    @Override
    public LyricsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LyricsViewHolder(LayoutInflater.from(context).inflate(R.layout.profile_lyrics_item,parent,false));
    }

    @Override
    public void onBindViewHolder(LyricsViewHolder holder, int position) {
        lyrics = lyricses.get(position);
        holder.lyrics_title.setText(lyrics.getTitle());
        holder.lyrics.setText(lyrics.getDesc());
    }

    @Override
    public int getItemCount() {
        return lyricses.size();
    }

    class LyricsViewHolder extends RecyclerView.ViewHolder{

        private TextView lyrics_title;
        private TextView lyrics;

        LyricsViewHolder(View itemView) {
            super(itemView);

            lyrics_title = itemView.findViewById(R.id.lyrics_title);
            lyrics = itemView.findViewById(R.id.lyrics);

        }
    }
}
