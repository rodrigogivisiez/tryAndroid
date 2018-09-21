package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tullyapp.tully.Models.ArtistOption;
import com.tullyapp.tully.R;

import java.util.ArrayList;

/**
 * Created by macbookpro on 10/09/17.
 */

public class ArtistOptionAdapter extends RecyclerView.Adapter<ArtistOptionAdapter.ArtistOptionViewHolder> {

    private ArrayList<ArtistOption> artistOptions;
    Context context;

    public ArtistOptionAdapter(Context context, ArrayList<ArtistOption> artistOptions) {
        this.context = context;
        this.artistOptions = artistOptions;
    }

    @Override
    public ArtistOptionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ArtistOptionViewHolder(LayoutInflater.from(context).inflate(R.layout.artist_option_item,parent,false));
    }

    @Override
    public void onBindViewHolder(ArtistOptionViewHolder holder, int position) {
        ArtistOption artistOption = artistOptions.get(position);
        holder.artist_option_text.setText(artistOption.getArtist_option_text());
    }

    @Override
    public int getItemCount() {
        return artistOptions.size();
    }

    class ArtistOptionViewHolder extends RecyclerView.ViewHolder{

        private TextView artist_option_text;

        ArtistOptionViewHolder(View itemView) {
            super(itemView);

            artist_option_text = itemView.findViewById(R.id.artist_option_text);
        }
    }
}
