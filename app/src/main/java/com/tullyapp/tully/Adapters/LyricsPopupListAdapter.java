package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tullyapp.tully.Models.LyricsWordSynonym;
import com.tullyapp.tully.R;

import org.json.JSONArray;

import java.util.ArrayList;

/**
 * Created by macbookpro on 28/09/17.
 */

public class LyricsPopupListAdapter extends RecyclerView.Adapter<LyricsPopupListAdapter.LyricsPopupListeViewHolder> {

    private final LayoutInflater layoutInflator;
    private final Context context;
    private ArrayList<LyricsWordSynonym> lyricsWordSynonymArrayList;
    private LyricsPopupClickEventsListener lyricsPopupClickEventsListener;

    public LyricsPopupListAdapter(Context context, ArrayList<LyricsWordSynonym> lyricsWordSynonyms, LyricsPopupClickEventsListener lyricsPopupClickEventsListener) {
        this.context = context;
        layoutInflator = LayoutInflater.from(this.context);
        this.lyricsWordSynonymArrayList = lyricsWordSynonyms;
        this.lyricsPopupClickEventsListener = lyricsPopupClickEventsListener;
    }

    @Override
    public LyricsPopupListeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LyricsPopupListeViewHolder(layoutInflator.inflate(R.layout.lyrics_popup_list_item,parent,false));
    }

    @Override
    public void onBindViewHolder(LyricsPopupListeViewHolder holder, int position) {
        LyricsWordSynonym lyricsWordSynonym = lyricsWordSynonymArrayList.get(position);
        holder.title.setText(lyricsWordSynonym.getWord());
        holder.position = position;
    }


    @Override
    public int getItemCount() {
        return lyricsWordSynonymArrayList.size();
    }

    class LyricsPopupListeViewHolder extends RecyclerView.ViewHolder{

        final ImageView right_green_icon;
        public final TextView title;
        final ImageView info_btn;
        public final LinearLayout list_item;

        public int position;

        LyricsPopupListeViewHolder(View itemView) {
            super(itemView);
            list_item = itemView.findViewById(R.id.list_item);
            right_green_icon = itemView.findViewById(R.id.right_green_icon);
            title = itemView.findViewById(R.id.title);
            info_btn = itemView.findViewById(R.id.info_btn);
            info_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                LyricsWordSynonym lyricsWordSynonym = lyricsWordSynonymArrayList.get(position);
                    if (lyricsPopupClickEventsListener !=null){
                        lyricsPopupClickEventsListener.showWordInfoListener(lyricsWordSynonym.getWord(),lyricsWordSynonym.getDescription());
                    }
                }
            });

            list_item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                LyricsWordSynonym lyricsWordSynonym = lyricsWordSynonymArrayList.get(position);
                    if (lyricsPopupClickEventsListener !=null){
                        if (!lyricsWordSynonym.getWord().isEmpty())
                            lyricsPopupClickEventsListener.itemSelected(lyricsWordSynonym.getWord());
                    }
                }
            });
        }
    }

    public interface LyricsPopupClickEventsListener {
        void showWordInfoListener(String word, JSONArray description);
        void itemSelected(String word);
    }
}
