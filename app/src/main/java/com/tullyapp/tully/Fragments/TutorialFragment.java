package com.tullyapp.tully.Fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.tullyapp.tully.R;

public class TutorialFragment extends Fragment {
    private static final String PARAM_RESOURCE = "PARAM_RESOURCE";
    private ImageView img_view;

    public TutorialFragment() {
    }

    public static TutorialFragment newInstance(int resource){
        Bundle args = new Bundle();
        args.putInt(PARAM_RESOURCE, resource);
        TutorialFragment tutorialFragment = new TutorialFragment();
        tutorialFragment.setArguments(args);
        return tutorialFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tutorial_image, container, false);
        int res = getArguments().getInt(PARAM_RESOURCE);
        img_view = v.findViewById(R.id.img_view);
        img_view.setImageResource(res);
        return v;
    }
}
