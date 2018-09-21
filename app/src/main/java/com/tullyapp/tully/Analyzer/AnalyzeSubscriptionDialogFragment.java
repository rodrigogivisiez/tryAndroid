package com.tullyapp.tully.Analyzer;


import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tullyapp.tully.R;


public class AnalyzeSubscriptionDialogFragment extends DialogFragment implements AnalyzerSubscribeFragment.OnAction, AnalyzerPaymentFragment.OnAnalyzerPaymentFragmentAction {

    private AnalyzerSubscribeFragment analyzerSubscribeFragment;
    private AnalyzerPaymentFragment analyzerPaymentFragment;
    private SubscriptionEvents subscriptionEvents;

    public interface SubscriptionEvents{
        void onSucessfullSubscription();
    }

    public AnalyzeSubscriptionDialogFragment() {
        // Required empty public constructor
    }

    public void setSubscriptionEvents(SubscriptionEvents subscriptionEvents){
        this.subscriptionEvents = subscriptionEvents;
    }

    public static AnalyzeSubscriptionDialogFragment newInstance() {
        return new AnalyzeSubscriptionDialogFragment();
    }

    @Override
    public int getTheme() {
        return R.style.FullScreenDialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        analyzerSubscribeFragment = AnalyzerSubscribeFragment.newInstance();
        analyzerSubscribeFragment.setOnAction(this);
        AnalyzerPaymentFragment.wasTransactionSuccess = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analyze_subscription_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addFragment(analyzerSubscribeFragment,false);
    }

    private void addFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_up, R.animator.fade_out, R.animator.fade_out, R.animator.fade_out);
        if (addToBackStack) {
            fragmentTransaction.add(R.id.fragment_container, fragment);
            fragmentTransaction.addToBackStack(fragment.getClass().getName());
        } else {
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.addToBackStack(fragment.getClass().getName());
        }
        fragmentTransaction.commit();
        //fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onCloseBtn() {
        this.dismiss();
    }

    @Override
    public void onSubscribe() {
        analyzerPaymentFragment = AnalyzerPaymentFragment.newInstance();
        analyzerPaymentFragment.setAnalyzerPaymentFragmentAction(this);
        addFragment(analyzerPaymentFragment,false);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(android.content.DialogInterface dialog, int keyCode,android.view.KeyEvent event) {
                if (AnalyzerPaymentFragment.isProcessing){
                    return (keyCode == android.view.KeyEvent.KEYCODE_BACK);
                }
                else{
                    if (AnalyzerPaymentFragment.wasTransactionSuccess && subscriptionEvents!=null){
                        subscriptionEvents.onSucessfullSubscription();
                        AnalyzeSubscriptionDialogFragment.this.dismiss();
                        return false;
                    }
                    else{
                        return false;
                    }
                }
            }
        });
    }

    @Override
    public void onAnalyzerPaymentFragmentClose() {
        this.dismiss();
    }

    @Override
    public void onSuccessFullPayment() {
        if (subscriptionEvents!=null){
            subscriptionEvents.onSucessfullSubscription();
            this.dismiss();
        }
    }
}
