package com.tullyapp.tully.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.tullyapp.tully.CustomView.CatSwipeRevealLayout;
import com.tullyapp.tully.Models.EngineerAccessModel;
import com.tullyapp.tully.R;

import java.util.ArrayList;

/**
 * Created by apple on 23/01/18.
 */

public class EngineerAccessAdapter extends RecyclerView.Adapter<EngineerAccessAdapter.EngineerAccessViewHolder> {

    private final LayoutInflater infalter;
    ArrayList<EngineerAccessModel> engineerAccessModelArrayList = new ArrayList<>();
    private Context context;
    private OnTap onTap;
    private ViewBinderHelper binderHelper;

    public EngineerAccessAdapter(Context context) {
        this.context = context;
        infalter = LayoutInflater.from(context);
        this.binderHelper = new ViewBinderHelper();
        this.binderHelper.setOpenOnlyOne(true);
    }

    public interface OnTap{
        void onDeleteTap(EngineerAccessModel engineerAccessModel, int position);
        void onSettingsTap(EngineerAccessModel engineerAccessModel, int position);
    }

    public void setOnTapListener(OnTap onTap){
        this.onTap = onTap;
    }

    @Override
    public EngineerAccessViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new EngineerAccessViewHolder(infalter.inflate(R.layout.engineer_access_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(EngineerAccessViewHolder engineerAccessViewHolder, int i) {
        engineerAccessViewHolder.init(engineerAccessViewHolder.getAdapterPosition());
    }

    public void add(EngineerAccessModel accessModel){
        this.engineerAccessModelArrayList.add(accessModel);
    }

    public void remove(int position){
        this.engineerAccessModelArrayList.remove(position);
        this.notifyItemRemoved(position);
    }

    public void clearData(){
        this.engineerAccessModelArrayList.clear();
        this.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return engineerAccessModelArrayList.size();
    }

    class EngineerAccessViewHolder extends RecyclerView.ViewHolder{
        private CatSwipeRevealLayout swipeLayout;
        private TextView tv_email;
        private ImageView delete_btn, settings_btn;

        EngineerAccessViewHolder(View itemView) {
            super(itemView);
            swipeLayout = itemView.findViewById(R.id.swipeLayout);
            tv_email = itemView.findViewById(R.id.tv_email);
            delete_btn = itemView.findViewById(R.id.delete_btn);
            settings_btn = itemView.findViewById(R.id.settings_btn);
        }

        void init(final int position){
            final EngineerAccessModel engineerAccessModel = engineerAccessModelArrayList.get(position);
            binderHelper.bind(swipeLayout,engineerAccessModel.getId());

            if (swipeLayout.isOpened()){
                swipeLayout.close(true);
            }

            tv_email.setText((engineerAccessModel.getName().isEmpty() ? engineerAccessModel.getEmail() : engineerAccessModel.getName()));

            delete_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onTap!=null){
                    onTap.onDeleteTap(engineerAccessModel, position);
                }
                }
            });

            settings_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (onTap!=null){
                    onTap.onSettingsTap(engineerAccessModel, position);
                }
                }
            });
        }
    }
}
