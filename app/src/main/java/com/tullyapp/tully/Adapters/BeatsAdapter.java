package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tullyapp.tully.Models.Beats;
import com.tullyapp.tully.R;

import java.util.ArrayList;

import static com.tullyapp.tully.Utils.Constants.BYTETOMB;

/**
 * Created by kathan on 12/02/18.
 */

public class BeatsAdapter  extends RecyclerView.Adapter<BeatsAdapter.BeatsAdapterViewHolder>  {

    private final LayoutInflater inflator;
    private ArrayList<Beats> beatsArrayList;
    private Context context;
    private MarketAudioListener marketAudioListener;

    public BeatsAdapter(Context context) {
        this.context = context;
        inflator = LayoutInflater.from(context);
        beatsArrayList = new ArrayList<>();
    }

    public interface MarketAudioListener{
        void onPlay(Beats beats, int position);

        void onPaused(Beats beats, int position);

        void onResumed(Beats beats, int position);

        void onPurchase(Beats beats, int position);

        void onFreePurchase(Beats beats, int position);
    }

    @Override
    public BeatsAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new BeatsAdapterViewHolder(inflator.inflate(R.layout.market_list_item,viewGroup,false));
    }

    @Override
    public void onBindViewHolder(BeatsAdapterViewHolder beatsAdapterViewHolder, int i) {
        beatsAdapterViewHolder.init(beatsAdapterViewHolder.getAdapterPosition());
    }

    public void add(Beats beats){
        beatsArrayList.add(beats);
        //this.notifyItemChanged();
    }

    public void setMarketAudioListener(MarketAudioListener marketAudioListener){
        this.marketAudioListener = marketAudioListener;
    }

    public void clearList(){
        this.beatsArrayList.clear();
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return beatsArrayList.size();
    }

    class BeatsAdapterViewHolder extends RecyclerView.ViewHolder{

        TextView tv_index, tv_file_title, tv_subtitle, tv_file_size;
        ImageView btn_play;
        Button price_btn;

        BeatsAdapterViewHolder(View itemView) {
            super(itemView);
            tv_index = itemView.findViewById(R.id.tv_index);
            tv_file_title = itemView.findViewById(R.id.tv_file_title);
            tv_subtitle = itemView.findViewById(R.id.tv_subtitle);
            tv_file_size = itemView.findViewById(R.id.tv_file_size);

            btn_play = itemView.findViewById(R.id.btn_play);
            price_btn = itemView.findViewById(R.id.price_btn);
        }

        void init(int pos){
            final int position = pos;
            final Beats beats = beatsArrayList.get(position);
            tv_file_title.setText(beats.getName());
            tv_subtitle.setText(beats.getProducer_name());
            if (beats.isFree())
                price_btn.setText(R.string.free);
            else
                price_btn.setText(beats.getPrice()+" $");

            double size = beats.getTrackSize();
            size = size / BYTETOMB;
            tv_file_size.setText(String.format("%.2f", size)+" MB");
            tv_index.setText(position+1+"");

            btn_play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                 if (marketAudioListener!=null){
                     if (beats.isPlaySelected()){
                         if (beats.isPlaying()){
                             beats.setPlaying(false);
                             beats.setPaused(true);
                             marketAudioListener.onPaused(beats,position);
                             btn_play.setImageResource(R.drawable.pr_play);
                         }
                         else{
                             beats.setPlaying(false);
                             beats.setPaused(true);
                             beats.setPaused(false);
                             beats.setPlaying(true);
                             marketAudioListener.onResumed(beats,position);
                             btn_play.setImageResource(R.drawable.market_pause_btn);
                         }
                     }
                     else{
                         marketAudioListener.onPlay(beats,position);
                         btn_play.setImageResource(R.drawable.market_pause_btn);
                     }
                 }
                }
            });

            if (beats.isPlaySelected()){
                if (beats.isPlaying()){
                    btn_play.setImageResource(R.drawable.market_pause_btn);
                }
                else{
                    btn_play.setImageResource(R.drawable.pr_play);
                }
            }else{
                btn_play.setImageResource(R.drawable.pr_play);
            }

            price_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (marketAudioListener!=null){

                        if (beats.isFree()){
                            marketAudioListener.onFreePurchase(beats,position);
                        }
                        else{
                            marketAudioListener.onPurchase(beats,position);
                        }

                    }
                }
            });
        }
    }

    public void markAudioPlaying(Beats beats, int position){
        for (Beats b : beatsArrayList){
            if (b.isPlaySelected()){
                b.setPlaySelected(false);
                b.setPlaying(false);
                b.setPaused(false);
            }
        }

        beatsArrayList.get(position).setPlaySelected(true);
        beatsArrayList.get(position).setPlaying(true);
        beatsArrayList.get(position).setPaused(false);

        this.notifyDataSetChanged();
    }

    public void markAudioComplete(Beats beats, int position){
        for (Beats b : beatsArrayList){
            b.setPlaySelected(false);
            b.setPlaying(false);
            b.setPaused(false);
        }

        this.notifyDataSetChanged();
    }

    public void markAudioPlaying(int postions, boolean isPlaying){
        this.beatsArrayList.get(postions).setPlaying(isPlaying);
        this.beatsArrayList.get(postions).setPaused(!isPlaying);
        this.notifyItemChanged(postions);
    }

    public Beats hasNext(int position){
        try{
            return this.beatsArrayList.get(position+1);
        }catch (IndexOutOfBoundsException e){
            return null;
        }

    }
}
