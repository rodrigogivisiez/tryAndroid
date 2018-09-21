package com.tullyapp.tully.Analyzer;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.tullyapp.tully.R;

public class AnalyzerSubscribeFragment extends Fragment implements View.OnClickListener {

    private ImageView btn_close, three_dash_right, analyze_icon, btn_subscribe;
    private TextView tv_bpm, tv_key, label_bpm, label_key, label_detecting, tv_percent;
    private Button btn_continue;
    private FrameLayout fragment_container;
    private OnAction onAction;

    public AnalyzerSubscribeFragment() {
        // Required empty public constructor
    }

    interface OnAction{
        void onCloseBtn();
        void onSubscribe();
    }

    public void setOnAction(OnAction onAction){
        this.onAction = onAction;
    }

    public static AnalyzerSubscribeFragment newInstance() {
        return new AnalyzerSubscribeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analyzer_subscribe, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btn_close = view.findViewById(R.id.btn_close);
        three_dash_right = view.findViewById(R.id.three_dash_right);
        analyze_icon = view.findViewById(R.id.analyze_icon);
        btn_subscribe = view.findViewById(R.id.btn_subscribe);

        tv_bpm = view.findViewById(R.id.tv_bpm);
        tv_key = view.findViewById(R.id.tv_key);
        label_bpm = view.findViewById(R.id.label_bpm);
        label_key = view.findViewById(R.id.label_key);
        label_detecting = view.findViewById(R.id.label_detecting);
        tv_percent = view.findViewById(R.id.tv_percent);

        btn_continue = view.findViewById(R.id.btn_continue);

        btn_continue.setOnClickListener(this);
        btn_subscribe.setOnClickListener(this);
        btn_close.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_continue:
                this.onAction.onSubscribe();
                break;

            case R.id.btn_subscribe:
                this.onAction.onSubscribe();
                break;

            case R.id.btn_close:
                this.onAction.onCloseBtn();
                break;
        }
    }
}
