package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.tullyapp.tully.R;

import java.util.ArrayList;
import java.util.List;

public class IndicatorAdapter extends RecyclerView.Adapter<IndicatorAdapter.IndicatorHolder> {

    private static final String TAG = IndicatorAdapter.class.getSimpleName();
    private final LayoutInflater inflator;
    //private Context context;
    private List<Boolean> list;

    public IndicatorAdapter(Context context) {
        //this.context = context;
        inflator = LayoutInflater.from(context);
        this.list = new ArrayList<>();
    }

    public void add(boolean boo){
        this.list.add(boo);
    }

    public void setActive(int position){
        for (int i=0; i<list.size(); i++){
            if (i==position){
                this.list.set(i,true);
            }
            else{
                this.list.set(i,false);
            }
        }
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IndicatorHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new IndicatorHolder(inflator.inflate(R.layout.indicator,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull IndicatorHolder holder, int position) {
        holder.setActive(list.get(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class IndicatorHolder extends RecyclerView.ViewHolder{

        ImageView img_indicator;

        IndicatorHolder(View itemView) {
            super(itemView);
            img_indicator = itemView.findViewById(R.id.img_indicator);
        }

        void setActive(boolean boo){
            if (boo){
                img_indicator.setImageResource(R.drawable.active_indicator_icon);
            }
            else{
                img_indicator.setImageResource(R.drawable.indicator_icon);
            }
        }
    }
}
