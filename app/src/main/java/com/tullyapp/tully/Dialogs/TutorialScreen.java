package com.tullyapp.tully.Dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tullyapp.tully.Adapters.IndicatorAdapter;
import com.tullyapp.tully.Fragments.TutorialFragment;
import com.tullyapp.tully.R;

import java.util.ArrayList;
import java.util.Arrays;

import static com.tullyapp.tully.Utils.Constants.TUTS_HOME;
import static com.tullyapp.tully.Utils.Constants.TUTS_LYRICS;
import static com.tullyapp.tully.Utils.Constants.TUTS_MARKET_PLACE;
import static com.tullyapp.tully.Utils.Constants.TUTS_PLAY;
import static com.tullyapp.tully.Utils.Constants.TUTS_RECORDING;

public class TutorialScreen extends android.support.v4.app.DialogFragment implements ViewPager.OnPageChangeListener, View.OnClickListener {

    private static final String TAG = TutorialScreen.class.getSimpleName();
    private static final String PARAM_TUTS = "PARAM_TUTS";
    private ViewPager viewPager;
    private RecyclerView recycle_view_indicator;
    private Context context;
    private IndicatorAdapter indicatorAdapter;
    private ScreenAdapter screenAdapter;
    private String selectedTuts;
    private TextView tv_label, tv_continue, tv_cancel;
    private ArrayList<String> titleArray;
    private int currentIndex = 0;
    private ArrayList<Integer> resources;
    private OnTutorialClosed onTutorialClosed;

    public TutorialScreen() {
        // empty constructor
    }

    public interface OnTutorialClosed{
        void onTutsClosed(String tut);
    }

    public void setOnTutorialClosed(OnTutorialClosed onTutorialClosed){
        this.onTutorialClosed = onTutorialClosed;
    }

    public static TutorialScreen newInstance(String tuts){
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_TUTS,tuts);
        TutorialScreen fragment = new TutorialScreen();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public int getTheme() {
        return R.style.FullDialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tutorial_layout, container, false);
        viewPager = v.findViewById(R.id.viewPager);
        recycle_view_indicator = v.findViewById(R.id.recycle_view_indicator);
        tv_label = v.findViewById(R.id.tv_label);
        tv_continue = v.findViewById(R.id.tv_continue);
        tv_cancel = v.findViewById(R.id.tv_cancel);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recycle_view_indicator.setLayoutManager(new LinearLayoutManager(this.context, LinearLayoutManager.HORIZONTAL, false));
        indicatorAdapter = new IndicatorAdapter(this.context);
        recycle_view_indicator.setAdapter(indicatorAdapter);
        tv_continue.setOnClickListener(this);
        tv_cancel.setOnClickListener(this);
        try{
            selectedTuts = getArguments().getString(PARAM_TUTS);
            if (selectedTuts!=null){
                initTuts(selectedTuts);
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    private void initTuts(String tuts){

        resources = new ArrayList<>();

        switch (tuts){
            case TUTS_HOME:
                resources.add(R.drawable.tuts_home_one);
                resources.add(R.drawable.tuts_home_two);
                titleArray = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.tuts_home)));
            break;

            case TUTS_PLAY:
                resources.add(R.drawable.tuts_play_one);
                resources.add(R.drawable.tuts_play_two);
                resources.add(R.drawable.tuts_play_three);
                resources.add(R.drawable.tuts_play_four);
                resources.add(R.drawable.tuts_play_five);
                titleArray = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.tuts_play)));
                break;

            case TUTS_LYRICS:
                resources.add(R.drawable.tuts_lyrics_one);
                resources.add(R.drawable.tuts_lyrics_two);
                titleArray = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.tuts_lyrics)));
                break;

            case TUTS_MARKET_PLACE:
                resources.add(R.drawable.tuts_marketplace_one);
                resources.add(R.drawable.tuts_marketplace_two);
                titleArray = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.tuts_marketplace)));
                break;

            case TUTS_RECORDING:
                resources.add(R.drawable.tuts_recording_one);
                titleArray = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.tuts_recording)));
                break;
        }

        screenAdapter = new ScreenAdapter(getChildFragmentManager(),resources);
        viewPager.setAdapter(screenAdapter);
        viewPager.addOnPageChangeListener(this);

        indicatorAdapter.add(true);
        for (int i=1; i<resources.size(); i++){
            indicatorAdapter.add(false);
        }

        indicatorAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        try{
            tv_label.setText(titleArray.get(position));
            indicatorAdapter.setActive(position);
            currentIndex = position;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_continue:
                if (currentIndex == resources.size()-1){
                    this.dismiss();
                }
                else{
                    viewPager.setCurrentItem(currentIndex+1,true);
                }
                break;

            case R.id.tv_cancel:
                this.dismiss();
                break;
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (onTutorialClosed!=null){
            onTutorialClosed.onTutsClosed(selectedTuts);
        }
        super.onDismiss(dialog);
    }

    public class ScreenAdapter extends FragmentPagerAdapter {

        ArrayList<Integer> resources;

        ScreenAdapter(FragmentManager fm, ArrayList<Integer> resources) {
            super(fm);
            this.resources = resources;
        }

        @Override
        public Fragment getItem(int position) {
            return TutorialFragment.newInstance(resources.get(position));
        }

        @Override
        public int getCount() {
            return resources.size();
        }
    }
}
