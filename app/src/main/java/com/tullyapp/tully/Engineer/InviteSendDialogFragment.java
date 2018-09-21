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
import android.widget.TextView;

import com.tullyapp.tully.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class InviteSendDialogFragment extends DialogFragment implements View.OnClickListener {


    private static final String PARAM_TITLE = "PARAM_TITLE";
    private static final String PARAM_SUBTITLE = "PARAM_SUBTITLE";
    private static final String PARAM_EMAIL = "PARAM_EMAIL";
    private String title, sub_title, email;
    private TextView tv_title, tv_subtitle, tv_email;
    private Button btn_okay;

    public InviteSendDialogFragment() {
        // Required empty public constructor
    }

    public static InviteSendDialogFragment newInstance(String title, String subtitle, String email){
        InviteSendDialogFragment inviteSendDialogFragment = new InviteSendDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_TITLE,title);
        bundle.putString(PARAM_SUBTITLE,subtitle);
        bundle.putString(PARAM_EMAIL,email);
        inviteSendDialogFragment.setArguments(bundle);
        return inviteSendDialogFragment;
    }

    @Override
    public int getTheme() {
        return R.style.FullScreenDialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments()!=null){
            title = getArguments().getString(PARAM_TITLE);
            sub_title = getArguments().getString(PARAM_SUBTITLE);
            email = getArguments().getString(PARAM_EMAIL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.engineer_invite_sent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tv_title = view.findViewById(R.id.tv_title);
        tv_subtitle = view.findViewById(R.id.tv_subtitle);
        tv_email = view.findViewById(R.id.tv_email);
        btn_okay = view.findViewById(R.id.btn_okay);

        tv_title.setText(title);
        tv_subtitle.setText(sub_title);
        tv_email.setText(email);
        btn_okay.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_okay:
                this.dismiss();
                break;
        }
    }
}
