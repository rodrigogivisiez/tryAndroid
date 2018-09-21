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

import com.tullyapp.tully.FirebaseDataModels.BeatAudio;
import com.tullyapp.tully.R;

import java.util.ArrayList;

import static com.tullyapp.tully.Utils.Constants.BYTETOMB;

/**
 * Created by kathan on 19/03/18.
 */

public class BeatAudioAdapter extends RecyclerView.Adapter<BeatAudioAdapter.FilesHolder>{

    private static final String TAG = BeatAudioAdapter.class.getSimpleName();
    private ArrayList<BeatAudio> beatAudioArrayList;
        private Context context;
        private boolean selection=false;
        private boolean checkall = false;
        private boolean isFromHome = false;
        private AdapterInterface adapterInterface;

        public BeatAudioAdapter(Context context, boolean isFromHome) {
            this.beatAudioArrayList = new ArrayList<>();
            this.context = context;
            this.isFromHome = isFromHome;
        }

        public interface AdapterInterface{
            void onLongPressedFile(BeatAudio beatAudio, int position);
            void onBeat(BeatAudio beatAudio, int position);
        }

        public void setAdapterInterface(AdapterInterface adapterInterface){
            this.adapterInterface = adapterInterface;
        }

        public void add(BeatAudio beatAudio){
            this.beatAudioArrayList.add(beatAudio);
        }

        public void clear(){
            this.beatAudioArrayList.clear();
        }

        @Override
        public FilesHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FilesHolder(LayoutInflater.from(context).inflate(R.layout.beat_file_home,parent,false));
        }

        @Override
        public void onBindViewHolder(final FilesHolder holder, int i) {

            final int position = holder.getAdapterPosition();
            final BeatAudio beatAudio = beatAudioArrayList.get(position);

            holder.grid_title.setText(beatAudio.getTitle());

            double size = beatAudio.getSize();
            if (size>0){
                size = size / BYTETOMB;
                holder.grid_subtitle.setText(String.format("%.2f", size)+" MB");
            }
            else{
                holder.grid_subtitle.setText("");
            }

            holder.grid_check_copy.setChecked(this.checkall);

            if (selection){
                holder.grid_check_copy.setVisibility(View.VISIBLE);
            }else {
                holder.grid_check_copy.setVisibility(View.GONE);
            }
            holder.griditemcopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.grid_check_copy.getVisibility()==View.VISIBLE){
                        holder.grid_check_copy.setChecked(!holder.grid_check_copy.isChecked());
                    }else{
                        if (adapterInterface!=null){
                            adapterInterface.onBeat(beatAudio,position);
                        }
                    }
                }
            });

            holder.griditemcopy.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (adapterInterface!=null){
                        if (!beatAudio.isVideo()){
                            adapterInterface.onLongPressedFile(beatAudio, position);
                        }
                    }
                    return false;
                }
            });

            holder.grid_check_copy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                beatAudio.setChecked(isChecked);
                }
            });
        }

        public void updateFileAtPos(BeatAudio beatAudio, int position){
            this.beatAudioArrayList.set(position, beatAudio);
            this.notifyItemChanged(position);
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
            return beatAudioArrayList.size();
        }

        class FilesHolder extends RecyclerView.ViewHolder{
            private LinearLayout griditemcopy;
            private TextView grid_title;
            private TextView grid_subtitle;
            private AppCompatCheckBox grid_check_copy;

            FilesHolder(View itemView) {
                super(itemView);
                griditemcopy = itemView.findViewById(R.id.griditemcopy);
                grid_title = itemView.findViewById(R.id.grid_title);
                grid_subtitle = itemView.findViewById(R.id.grid_subtitle);
                grid_check_copy = itemView.findViewById(R.id.grid_check_copy);
            }
        }
}
