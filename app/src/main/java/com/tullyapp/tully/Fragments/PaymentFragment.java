package com.tullyapp.tully.Fragments;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.stripe.android.model.Card;
import com.stripe.android.view.CardMultilineWidget;
import com.tullyapp.tully.Models.Beats;
import com.tullyapp.tully.Models.CreditCard;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Services.ProcessPaymentService;

import static com.tullyapp.tully.Services.ProcessPaymentService.PAYMENT_RESPONSE_STATUS;
import static com.tullyapp.tully.Utils.Constants.ACTION_PAYMENT_MARKETPLACE;
import static com.tullyapp.tully.Utils.Constants.BYTETOMB;

/**
 * A simple {@link Fragment} subclass.
 */
public class PaymentFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = PaymentFragment.class.getSimpleName();
    private static final String ARG_PARAM_BEAT = "ARG_PARAM_BEAT";
    private Button btn_pay;
    private Card card;

    private TextView tv_title, tv_subtitle, tv_file_size, tv_loading, back_to_market;
    private RelativeLayout loading_view, rl_order_complete;

    private ConstraintLayout card_view;

    private BackInterface backInterface;

    private Beats beat;

    CardMultilineWidget cardMultilineWidget;

    private ResponseReceiver responseReceiver;

    public PaymentFragment() {
        // Required empty public constructor
    }

    public static PaymentFragment newInstance(Beats beat) {
        PaymentFragment fragment = new PaymentFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM_BEAT, beat);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onAttachToParentFragment(getParentFragment());
        Bundle bundle = getArguments();
        if (bundle!=null && bundle.size()>0){
            beat = (Beats) getArguments().getSerializable(ARG_PARAM_BEAT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment, container, false);
        cardMultilineWidget = view.findViewById(R.id.card_multiline_widget);
        tv_title = view.findViewById(R.id.tv_title);
        tv_subtitle = view.findViewById(R.id.tv_subtitle);
        tv_file_size = view.findViewById(R.id.tv_file_size);
        btn_pay = view.findViewById(R.id.btn_pay);
        loading_view = view.findViewById(R.id.loading_view);
        tv_loading = view.findViewById(R.id.tv_loading);

        rl_order_complete = view.findViewById(R.id.rl_order_complete);
        card_view = view.findViewById(R.id.card_view);

        back_to_market = view.findViewById(R.id.back_to_market);

        tv_title.setText(beat.getName());
        tv_subtitle.setText(beat.getProducer_name());
        double size = beat.getTrackSize() / BYTETOMB;
        tv_file_size.setText(beat.getPrice()+" $ | "+String.format("%.2f", size)+" MB");
        btn_pay.setText("Pay "+beat.getPrice()+" $");

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btn_pay.setOnClickListener(this);
        back_to_market.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_pay:
                    makePayment();
                break;

            case R.id.back_to_market:
                if (backInterface!=null){
                    backInterface.onBack();
                }
                break;
        }
    }

    public void onAttachToParentFragment(Fragment fragment) {
        try {
            backInterface = (BackInterface) fragment;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(fragment.toString() + " must implement OnPlayerSelectionSetListener");
        }
    }

    interface BackInterface{
        void onBack();
    }

    private void makePayment(){
        card = cardMultilineWidget.getCard();
        if (card!=null && card.validateCard()){
            loading_view.setVisibility(View.VISIBLE);

            CreditCard creditCard = new CreditCard(
                card.getNumber(),
                card.getExpMonth(),
                card.getExpYear(),
                card.getCVC()
            );
            tv_loading.setText(R.string.processing_transaction);
            ProcessPaymentService.processPayment(getContext(),creditCard,beat,ACTION_PAYMENT_MARKETPLACE);
            cardMultilineWidget.clear();
            hideKeyboard(getActivity());
        }
        else{
            Toast.makeText(getContext(), "Invalid Card", Toast.LENGTH_SHORT).show();
        }
    }

    public static void hideKeyboard(Activity activity) {
        try{
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            // check if no view has focus:
            View currentFocusedView = activity.getCurrentFocus();
            if (currentFocusedView != null) {
                inputManager.hideSoftInputFromWindow(currentFocusedView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //progressBar.setVisibility(View.GONE);
            switch (intent.getAction()){
                case ACTION_PAYMENT_MARKETPLACE:
                    int status = intent.getIntExtra(PAYMENT_RESPONSE_STATUS,0);
                    loading_view.setVisibility(View.GONE);
                    if (status==1){
                        card_view.setVisibility(View.GONE);
                        rl_order_complete.setVisibility(View.VISIBLE);
                    }
                    else{
                        if (backInterface!=null){
                            backInterface.onBack();
                        }
                    }
                    break;
            }
        }
    }

    private void registerReceivers(Context context){
        unregisterBroadcast();
        IntentFilter filter= new IntentFilter();
        filter.addAction(ACTION_PAYMENT_MARKETPLACE);
        LocalBroadcastManager.getInstance(context).registerReceiver(responseReceiver, filter);
    }

    private void unregisterBroadcast(){
        try{
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(responseReceiver);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        responseReceiver = new ResponseReceiver();
        registerReceivers(context);
    }

    @Override
    public void onDetach() {
        unregisterBroadcast();
        super.onDetach();
    }
}
