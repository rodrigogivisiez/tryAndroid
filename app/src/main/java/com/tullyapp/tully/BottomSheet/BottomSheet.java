package com.tullyapp.tully.BottomSheet;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tullyapp.tully.R;

public class BottomSheet extends BottomSheetDialogFragment implements View.OnClickListener {

    private static final String ARG_LAYOUT = "ARG_LAYOUT";
    private Listener mListener;
    private int layout;

    // TODO: Customize parameters
    public static BottomSheet newInstance(int layout) {
        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT, layout);
        BottomSheet fragment = new BottomSheet();
        fragment.setArguments(args);
        return fragment;
    }

    public void setmListener(Listener listener){
        this.mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            layout = getArguments().getInt(ARG_LAYOUT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(this.layout, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        switch (layout){
            case R.layout.no_project_bottom_sheet:

                    view.findViewById(R.id.bsb_create).setOnClickListener(this);
                    view.findViewById(R.id.bsb_share_np).setOnClickListener(this);

                break;

            case R.layout.project_bottom_sheet:

                    view.findViewById(R.id.bsb_rename).setOnClickListener(this);
                    view.findViewById(R.id.bsb_share_p).setOnClickListener(this);

                break;

            case R.layout.add_chat_file_bottom_sheet:

                view.findViewById(R.id.bsb_add_doc).setOnClickListener(this);
                view.findViewById(R.id.bsb_add_images).setOnClickListener(this);

                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Fragment parent = getParentFragment();
        if (parent != null) {
            mListener = (Listener) parent;
        } else {
            mListener = (Listener) context;
        }
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @Override
    public void onClick(View view) {
        if (mListener!=null){
            mListener.onItemClicked(view.getId());
        }
    }

    public interface Listener {
        void onItemClicked(int id);
    }

}
