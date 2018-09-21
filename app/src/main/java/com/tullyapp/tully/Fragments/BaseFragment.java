package com.tullyapp.tully.Fragments;

import android.support.v4.app.Fragment;

public abstract class BaseFragment extends Fragment {
    public abstract void onSearchKey(String searchKey);
    public abstract void onSearchCancelled();
    public abstract void fabButtonClicked();
    public abstract void actionEvent(int event);
}
