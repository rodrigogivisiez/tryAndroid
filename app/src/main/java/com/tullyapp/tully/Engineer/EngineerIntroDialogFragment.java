package com.tullyapp.tully.Engineer;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.tullyapp.tully.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class EngineerIntroDialogFragment extends DialogFragment implements View.OnClickListener {

    public EngineerIntroDialogFragment() {
        // Required empty public constructor
    }

    public static EngineerIntroDialogFragment newInstance(){
        return new EngineerIntroDialogFragment();
    }

    @Override
    public int getTheme() { return R.style.FullScreenDialog; }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_engineer_intro_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button btn_got_it = view.findViewById(R.id.btn_got_it);
        btn_got_it.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_got_it:
                this.dismiss();
                break;
        }
    }
}
