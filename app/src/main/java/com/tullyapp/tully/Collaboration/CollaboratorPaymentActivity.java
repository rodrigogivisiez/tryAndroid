package com.tullyapp.tully.Collaboration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.stripe.android.model.Card;
import com.stripe.android.view.CardMultilineWidget;
import com.tullyapp.tully.Models.CreditCard;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Services.ProcessPaymentService;

import static com.tullyapp.tully.Engineer.EngineerPaymentFragment.hideKeyboard;
import static com.tullyapp.tully.Services.ProcessPaymentService.PAYMENT_RESPONSE_STATUS;
import static com.tullyapp.tully.Utils.Constants.ACTION_COLLABORATION_PAYMENT;
import static com.tullyapp.tully.Utils.Utils.isInternetAvailable;

/**
 * Created by Santosh on 14/9/18.
 */
public class CollaboratorPaymentActivity extends AppCompatActivity implements View.OnClickListener {

    private ConstraintLayout cardView;
    private RelativeLayout loadingView, rl_order_complete;
    private CardMultilineWidget cardMultilineWidget;
    private ResponseReceiver responseReceiver;
    private static boolean isProcessing = false;
    private static boolean wasTransactionSuccess = false;
    private TextView tvLoading;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_engineer_payment);
        ActionBar actionBar=getSupportActionBar();
        if(null!=actionBar) {
            setTitle(getString(R.string.collaboration_payment));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        initUI();
    }

    private void initUI() {
        Button btnMakePayment = findViewById(R.id.btn_pay);
        tvLoading = findViewById(R.id.tv_loading);
        TextView tvGotoInviteScreen = findViewById(R.id.back_to_market);
        TextView tvSubTitle = findViewById(R.id.tv_subtitle);
        TextView tvSeeOnCharts = findViewById(R.id.tv_2);
        loadingView = findViewById(R.id.loading_view);
        rl_order_complete = findViewById(R.id.rl_order_complete);
        cardView = findViewById(R.id.card_view);
        cardMultilineWidget = findViewById(R.id.card_multiline_widget);

        tvGotoInviteScreen.setText(getString(R.string.goto_invite));
        btnMakePayment.setOnClickListener(this);
        tvGotoInviteScreen.setOnClickListener(this);
        tvSubTitle.setVisibility(View.GONE);
        tvSeeOnCharts.setVisibility(View.GONE);

        responseReceiver = new ResponseReceiver();
        registerReceivers(this);
    }

    private void makePayment() {
        Card card = cardMultilineWidget.getCard();
        if (card !=null && card.validateCard()){
            loadingView.setVisibility(View.VISIBLE);
            tvLoading.setText(R.string.processing_transaction);
            isProcessing=true;
            CreditCard creditCard = new CreditCard(
                    card.getNumber(),
                    card.getExpMonth(),
                    card.getExpYear(),
                    card.getCVC()
            );
            cardMultilineWidget.clear();
            ProcessPaymentService.processCollaborationPayment(this, creditCard, ACTION_COLLABORATION_PAYMENT);
            hideKeyboard(this);
        }
        else{
            Toast.makeText(this, getString(R.string.invalid_card), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                unregisterBroadcast();
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pay:
                if(isInternetAvailable(this)) {
                    hideKeyboard(this);
                    makePayment();
                } else {
                    Toast.makeText(this, getString(R.string.error_network_connection), Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.back_to_market:
                Intent intent = new Intent(CollaboratorPaymentActivity.this, InviteActivity.class);
                startActivity(intent);
                finish();
                break;
            default:
        }
    }

    @Override
    public void onBackPressed() {
        if(!isProcessing) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, getString(R.string.transaction_in_progress), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBroadcast();
    }

    private void registerReceivers(Context context) {
        unregisterBroadcast();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_COLLABORATION_PAYMENT);
        LocalBroadcastManager.getInstance(context).registerReceiver(responseReceiver, filter);
    }

    private void unregisterBroadcast() {
        try {
            LocalBroadcastManager.getInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class ResponseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // progressBar.setVisibility(View.GONE);
            switch (intent.getAction()) {
                case ACTION_COLLABORATION_PAYMENT:
                    int status = intent.getIntExtra(PAYMENT_RESPONSE_STATUS,0);
                    loadingView.setVisibility(View.GONE);
                    isProcessing = false;
                    if (status==1){
                        wasTransactionSuccess = true;
                        cardView.setVisibility(View.GONE);
                        rl_order_complete.setVisibility(View.VISIBLE);
                    }
                    else{
                        wasTransactionSuccess = false;
                    }
                    break;
                default:
            }
        }
    }
}