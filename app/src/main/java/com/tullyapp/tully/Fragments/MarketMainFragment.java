package com.tullyapp.tully.Fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tullyapp.tully.Models.Beats;
import com.tullyapp.tully.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class MarketMainFragment extends BaseFragment implements MarketSongListFragment.MarketSongListListener, PaymentFragment.BackInterface {


    public MarketMainFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_market_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addFragment(MarketSongListFragment.instantiate(getContext(),MarketSongListFragment.class.getName()),false);
    }

    private void addFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_up, R.animator.fade_out, R.animator.fade_out, R.animator.fade_out);
        if (addToBackStack) {
            fragmentTransaction.add(R.id.container, fragment);
            fragmentTransaction.addToBackStack(fragment.getClass().getName());
        } else {
            fragmentTransaction.replace(R.id.container, fragment);
            fragmentTransaction.addToBackStack(fragment.getClass().getName());
        }
        fragmentTransaction.commit();
        //fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onBeatsPurchaseClicked(Beats beats) {
        addFragment(PaymentFragment.newInstance(beats),true);
    }


    @Override
    public void onSearchKey(String searchKey) {

    }

    @Override
    public void onSearchCancelled() {

    }

    @Override
    public void fabButtonClicked() {

    }

    @Override
    public void actionEvent(int event) {

    }

    @Override
    public void onBack() {
        getChildFragmentManager().popBackStack();
        addFragment(MarketSongListFragment.instantiate(getContext(),MarketSongListFragment.class.getName()),false);
    }
}
