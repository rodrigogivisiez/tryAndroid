package com.tullyapp.tully.Fragments;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.dekoservidoni.omfm.OneMoreFabMenu;
import com.tullyapp.tully.HomeActivity;
import com.tullyapp.tully.LyricsEditActivity;
import com.tullyapp.tully.R;
import com.tullyapp.tully.RecordingActivity;
import com.tullyapp.tully.Utils.PreferenceKeys;
import com.tullyapp.tully.Utils.PreferenceUtil;

import static com.tullyapp.tully.Fragments.LyricsListFragment.NEW_LYREICS_REQUEST_CODE;
import static com.tullyapp.tully.Fragments.RecordingListFragment.NEW_RECORDING_REQUEST_CODE;

public class WriteRecordFragment extends BaseFragment implements OneMoreFabMenu.OptionsClick {

    private static final String TAG = WriteRecordFragment.class.getSimpleName();
    private BaseFragment baseFragment;
    private HomeActivity homeActivity;
    private OneMoreFabMenu fab;
    private FragmentManager fragmentManager;
    private RelativeLayout ll_no_data;
    private String lastSelectedFragmentClass;

    public WriteRecordFragment() {
        // Required empty public constructor
    }

    public static WriteRecordFragment newInstance() {
        return new WriteRecordFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        fragmentManager = getChildFragmentManager();
        homeActivity = (HomeActivity) getActivity();
        if (homeActivity!=null){
            homeActivity.setActionItemActive(R.id.action_write_list,false);
            homeActivity.setActionItemActive(R.id.action_record_list,false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_write_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fab = view.findViewById(R.id.fab);
        fab.setOptionsClick(this);
        ll_no_data = view.findViewById(R.id.ll_no_data);
        lastSelectedFragmentClass = PreferenceUtil.getPref(getContext()).getString(PreferenceKeys.CLASS_NAME,"");
        if (!lastSelectedFragmentClass.isEmpty()){
            if (lastSelectedFragmentClass.equals(LyricsListFragment.class.getSimpleName())){
                actionEvent(R.id.action_write_list);
            }
            else{
                actionEvent(R.id.action_record_list);
            }
        }
    }

    @Override
    public void onSearchKey(String searchKey) {
        try{
            baseFragment.onSearchKey(searchKey);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onSearchCancelled() {
        try{
            baseFragment.onSearchCancelled();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void fabButtonClicked() {

    }

    @Override
    public void actionEvent(int event) {
        try{
            switch (event){
                case R.id.action_write_list:
                    baseFragment = (BaseFragment) LyricsListFragment.instantiate(getContext(),LyricsListFragment.class.getName());
                    addFragment(baseFragment,false);
                    homeActivity.setActionItemActive(R.id.action_write_list,true);
                    homeActivity.setActionItemActive(R.id.action_record_list,false);
                    PreferenceUtil.getPref(getContext()).edit().putString(PreferenceKeys.CLASS_NAME,LyricsListFragment.class.getSimpleName()).apply();
                    break;

                case R.id.action_record_list:
                    baseFragment = (BaseFragment) RecordingListFragment.instantiate(getContext(),RecordingListFragment.class.getName());
                    addFragment(baseFragment,false);
                    homeActivity.setActionItemActive(R.id.action_record_list,true);
                    homeActivity.setActionItemActive(R.id.action_write_list,false);
                    PreferenceUtil.getPref(getContext()).edit().putString(PreferenceKeys.CLASS_NAME,RecordingListFragment.class.getSimpleName()).apply();
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void addFragment(Fragment fragment, boolean addToBackStack) {
        ll_no_data.setVisibility(View.GONE);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_up, R.animator.fade_out, R.animator.fade_out, R.animator.fade_out);
        if (addToBackStack) {
            fragmentTransaction.add(R.id.container, fragment);
            fragmentTransaction.addToBackStack(fragment.getClass().getName());
        } else {
            fragmentTransaction.replace(R.id.container, fragment);
        }
        fragmentTransaction.commit();
        //fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case NEW_RECORDING_REQUEST_CODE:
                if (fragmentManager.getFragments().size()>0){
                    try{
                        RecordingListFragment instance = (RecordingListFragment) baseFragment;
                        instance.fetchRecordings();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                break;

            case NEW_LYREICS_REQUEST_CODE:
                if (fragmentManager.getFragments().size()>0){
                    try{
                        LyricsListFragment instance = (LyricsListFragment) baseFragment;
                        instance.fetchLyrics();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public void onOptionClick(Integer integer) {
        switch (integer){
            case R.id.option_btn_record:
                Intent intent = new Intent(getContext(),RecordingActivity.class);
                startActivityForResult(intent,NEW_RECORDING_REQUEST_CODE);
                break;

            case R.id.option_btn_write:
                Intent intent1 = new Intent(getContext(), LyricsEditActivity.class);
                startActivityForResult(intent1,NEW_LYREICS_REQUEST_CODE);
                break;
        }
    }
}
