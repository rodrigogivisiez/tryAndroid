package com.tullyapp.tully.Analyzer;


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
import com.tullyapp.tully.Models.CreditCard;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Services.ProcessPaymentService;

import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.ACTION_GET_CUSTOMER_ID;
import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.PARAM_CUSTOMER_ID;
import static com.tullyapp.tully.Services.ProcessPaymentService.PAYMENT_RESPONSE_STATUS;
import static com.tullyapp.tully.Utils.Constants.ACTION_ANALYZER_PAYMENT;

/**
 * A simple {@link Fragment} subclass.
 */
public class AnalyzerPaymentFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = AnalyzerPaymentFragment.class.getSimpleName();
    private Button btn_pay;
    private Card card;
    private ConstraintLayout card_view;
    private TextView tv_loading, back_to_market;
    private RelativeLayout loading_view, rl_order_complete;
    private CardMultilineWidget cardMultilineWidget;
    private ResponseReceiver responseReceiver;
    private OnAnalyzerPaymentFragmentAction onAnalyzerPaymentFragmentAction;
    public static boolean isProcessing = false;
    public static boolean wasTransactionSuccess = false;

    public AnalyzerPaymentFragment() {
        // Required empty public constructor
    }

    interface OnAnalyzerPaymentFragmentAction{
        void onAnalyzerPaymentFragmentClose();
        void onSuccessFullPayment();
    }

    public void setAnalyzerPaymentFragmentAction(OnAnalyzerPaymentFragmentAction onAnalyzerPaymentFragmentAction){
        this.onAnalyzerPaymentFragmentAction = onAnalyzerPaymentFragmentAction;
    }

    public static AnalyzerPaymentFragment newInstance(){
        return new AnalyzerPaymentFragment();
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wasTransactionSuccess = false;
        isProcessing = false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        responseReceiver = new ResponseReceiver();
        registerReceivers(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analyzer_payment_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cardMultilineWidget = view.findViewById(R.id.card_multiline_widget);
        btn_pay = view.findViewById(R.id.btn_pay);
        loading_view = view.findViewById(R.id.loading_view);
        tv_loading = view.findViewById(R.id.tv_loading);

        rl_order_complete = view.findViewById(R.id.rl_order_complete);
        card_view = view.findViewById(R.id.card_view);

        back_to_market = view.findViewById(R.id.back_to_market);

        btn_pay.setText(R.string.pay_1_99);

        btn_pay.setOnClickListener(this);
        back_to_market.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_pay:
                hideKeyboard((Activity) getContext());
                makePayment();
                break;

            case R.id.back_to_market:
                if (onAnalyzerPaymentFragmentAction!=null){
                    onAnalyzerPaymentFragmentAction.onSuccessFullPayment();
                }
                break;
        }
    }

    private void makePayment(){
        card = cardMultilineWidget.getCard();
        if (card!=null && card.validateCard()){
            loading_view.setVisibility(View.VISIBLE);
            tv_loading.setText(R.string.processing_transaction);
            isProcessing = true;
            FirebaseDatabaseOperations.getCustomerIdentification(getContext());
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
            // progressBar.setVisibility(View.GONE);
            switch (intent.getAction()) {
                case ACTION_ANALYZER_PAYMENT:
                    int status = intent.getIntExtra(PAYMENT_RESPONSE_STATUS,0);
                    loading_view.setVisibility(View.GONE);
                    isProcessing = false;
                    if (status==1){
                        wasTransactionSuccess = true;
                        card_view.setVisibility(View.GONE);
                        rl_order_complete.setVisibility(View.VISIBLE);
                    }
                    else{
                        wasTransactionSuccess = false;
                    }
                    break;

                case ACTION_GET_CUSTOMER_ID:
                    CreditCard creditCard = new CreditCard(
                        card.getNumber(),
                        card.getExpMonth(),
                        card.getExpYear(),
                        card.getCVC()
                    );
                    cardMultilineWidget.clear();
                    String customer_id = intent.getStringExtra(PARAM_CUSTOMER_ID);
                    ProcessPaymentService.processAnalyzePayment(getContext(),creditCard, customer_id, ACTION_ANALYZER_PAYMENT);
                    break;
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unregisterBroadcast();
    }

    private void registerReceivers(Context context){
        unregisterBroadcast();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ANALYZER_PAYMENT);
        filter.addAction(ACTION_GET_CUSTOMER_ID);
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
}
