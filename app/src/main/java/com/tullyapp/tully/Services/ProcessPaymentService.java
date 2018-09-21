package com.tullyapp.tully.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.stripe.android.SourceCallback;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.Token;
import com.tullyapp.tully.FirebaseDataModels.EngineerAdminAccess;
import com.tullyapp.tully.FirebaseDataModels.Settings;
import com.tullyapp.tully.Models.Beats;
import com.tullyapp.tully.Models.CreditCard;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.Constants;
import com.tullyapp.tully.Utils.PreferenceKeys;
import com.tullyapp.tully.Utils.PreferenceUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyStore;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.conn.ConnectTimeoutException;

import static com.tullyapp.tully.Services.FirebaseDatabaseOperations.PARAM_CUSTOMER_ID;
import static com.tullyapp.tully.Utils.APIs.STRIPE_KEY;
import static com.tullyapp.tully.Utils.Constants.ACTION_ANALYZER_PAYMENT;
import static com.tullyapp.tully.Utils.Constants.ACTION_COLLABORATION_PAYMENT;
import static com.tullyapp.tully.Utils.Constants.ACTION_ENGINEER_PAYMENT;
import static com.tullyapp.tully.Utils.Constants.ACTION_PAYMENT_MARKETPLACE;

public class ProcessPaymentService extends IntentService {

    private static final String PARAM_CARD = "PARAM_CARD";
    private static final String PARAM_BEAT = "PARAM_BEAT";
    private static final String TAG = ProcessPaymentService.class.getName();
    public static final String PARAM_PAYMENT_RESPONSE = "PARAM_PAYMENT_RESPONSE";
    public static final String PAYMENT_RESPONSE_STATUS = "PAYMENT_RESPONSE_STATUS";
    private static final String PARAM_ACTION = "PARAM_ACTION";
    public static final String PARAM_SUBSCRIPTION_ID = "PARAM_SUBSCRIPTION_ID";
    public static final String PARAM_PLAN_TYPE = "PARAM_PLAN_TYPE";
    private FirebaseAuth mAuth;
    private Beats beat;

    public ProcessPaymentService() {
        super("ProcessPaymentService");
    }

    public static void processPayment(Context context, CreditCard card, Beats beat, String action) {
        Intent intent = new Intent(context, ProcessPaymentService.class);
        intent.setAction(action);
        intent.putExtra(PARAM_CARD,card);
        intent.putExtra(PARAM_BEAT,beat);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void processAnalyzePayment(Context context, CreditCard card, String customer_id, String action){
        Intent intent = new Intent(context, ProcessPaymentService.class);
        intent.setAction(action);
        intent.putExtra(PARAM_CARD,card);
        intent.putExtra(PARAM_CUSTOMER_ID,customer_id);
        intent.putExtra(PARAM_ACTION,action);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void processEngineerPayment(Context context, CreditCard card, String planType, String action){
        Intent intent = new Intent(context, ProcessPaymentService.class);
        intent.setAction(action);
        intent.putExtra(PARAM_CARD,card);
        intent.putExtra(PARAM_ACTION,action);
        intent.putExtra(PARAM_PLAN_TYPE,planType);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void processCollaborationPayment(Context context, CreditCard card, String action) {
        Intent intent = new Intent(context, ProcessPaymentService.class);
        intent.setAction(action);
        intent.putExtra(PARAM_CARD,card);
        intent.putExtra(PARAM_ACTION,action);
        try{
            ContextCompat.startForegroundService(context,intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (Build.VERSION.SDK_INT >= 26) {
                String CHANNEL_ID = getString(R.string.app_name);
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Processing payment", NotificationManager.IMPORTANCE_DEFAULT);
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("").setContentText("").build();
                startForeground(1, notification);
            }
            String action = intent.getAction();
            CreditCard card;
            mAuth = FirebaseAuth.getInstance();
            String customer_id;
            switch (action) {
                case ACTION_PAYMENT_MARKETPLACE:
                    card = (CreditCard) intent.getSerializableExtra(PARAM_CARD);
                    beat = (Beats) intent.getSerializableExtra(PARAM_BEAT);
                    generateStripeTokenForBeat(card);
                    break;

                case ACTION_ANALYZER_PAYMENT:
                    card = (CreditCard) intent.getSerializableExtra(PARAM_CARD);
                    customer_id = intent.getStringExtra(PARAM_CUSTOMER_ID);
                    generateStripeTokenForAnalyzer(card,customer_id);
                    break;

                case ACTION_ENGINEER_PAYMENT:
                    card = (CreditCard) intent.getSerializableExtra(PARAM_CARD);
                    String planType = intent.getStringExtra(PARAM_PLAN_TYPE);
                    getCustomerInfoForEngineer(card,planType);
                    break;

                case ACTION_COLLABORATION_PAYMENT:
                    card = (CreditCard) intent.getSerializableExtra(PARAM_CARD);
                    String subscriptionPlanType = intent.getStringExtra(PARAM_PLAN_TYPE);
                    generateStripeTokenForCollaboration(card, "basic");
                    break;
            }
        }
    }

    private void getCustomerInfoForEngineer(final CreditCard card, final String planType){
        if (mAuth.getCurrentUser()!=null) {
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
            mDatabase.child("settings").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Settings settings = dataSnapshot.getValue(Settings.class);
                        if (settings!=null){
                            EngineerAdminAccess engineerAdminAccess = settings.getEngineerAdminAccess();
                            if (engineerAdminAccess!=null){
                                engineerAdminAccess.setCustomer_id(settings.getCustomer_id());
                            }
                            generateStripeTokenForEngineer(card,planType,engineerAdminAccess);
                        }
                    }
                    else{
                        generateStripeTokenForEngineer(card,planType,null);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Intent intent = new Intent(ACTION_ENGINEER_PAYMENT);
                    Toast.makeText(getApplicationContext(), "Network Error - Payment Failed", Toast.LENGTH_SHORT).show();
                    intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                    sendFinishBroadCast(intent);
                }
            });
        }
    }

    private void generateStripeTokenForEngineer(CreditCard card, final String planType, final EngineerAdminAccess engineerAdminAccess){
        final Stripe stripe = new Stripe(getApplicationContext(), STRIPE_KEY);
        final Card sCard = new Card(card.getCardNumber(), card.getExpireMonth(), card.getYear(), card.getCvc());
        SourceParams cardSourceParams = SourceParams.createCardParams(sCard);
        stripe.createSource(cardSourceParams, new SourceCallback() {
            @Override
            public void onError(Exception error) {
                error.printStackTrace();
                Intent intent = new Intent(ACTION_ENGINEER_PAYMENT);
                Toast.makeText(getApplicationContext(), "Network Error - Payment Failed", Toast.LENGTH_SHORT).show();
                intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                sendFinishBroadCast(intent);
            }

            @Override
            public void onSuccess(Source source) {
                // create your source as described
                SourceCardData cardData = (SourceCardData) source.getSourceTypeModel();
                String threeDStatus = cardData.getThreeDSecureStatus();
                if (SourceCardData.REQUIRED.equals(threeDStatus)) {
                    // this is the case where you would need to conduct a 3DS check
                }
                chargeViaServerForEngineer(source.getId(), planType, engineerAdminAccess);
            }
        });
    }


    private void chargeViaServerForEngineer(final String sourceId, final String planType, final EngineerAdminAccess engineerAdminAccess){
        boolean valid = true;
        String idToken = PreferenceUtil.getPref(this).getString(PreferenceKeys.ID_TOKEN,"");
        long timestamp = PreferenceUtil.getPref(this).getLong(PreferenceKeys.ID_TOKEN_EXPIRATION,-1);
        if (idToken.isEmpty()){
            valid = false;
        }
        else{
            long diff = timestamp - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
            if (diff<100){
                valid = false;
            }
        }
        if (valid){
            chargeEngineer(idToken,sourceId, planType, engineerAdminAccess);
        }
        else{
            if (mAuth.getCurrentUser()!=null){
                mAuth.getCurrentUser().getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()){
                            PreferenceUtil.getPref(ProcessPaymentService.this).edit().putString(PreferenceKeys.ID_TOKEN,task.getResult().getToken()).apply();
                            PreferenceUtil.getPref(ProcessPaymentService.this).edit().putLong(PreferenceKeys.ID_TOKEN_EXPIRATION,task.getResult().getExpirationTimestamp()).apply();
                            chargeEngineer(task.getResult().getToken(), sourceId, planType, engineerAdminAccess);
                        }
                        else{
                            Intent intent = new Intent(ACTION_ENGINEER_PAYMENT);
                            Toast.makeText(getApplicationContext(), "Network Error - Payment Failed", Toast.LENGTH_SHORT).show();
                            intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                            sendFinishBroadCast(intent);
                        }
                    }
                });
            }
        }
    }

    private void chargeEngineer(final String authToken, final String sourceId, final String planType, EngineerAdminAccess engineerAdminAccess){
        if (mAuth.getCurrentUser()!=null){
            AsyncHttpClient client = new AsyncHttpClient();
            try {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
                sf.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                client.setSSLSocketFactory(sf);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            RequestParams params = new RequestParams();
            params.put("source",sourceId);
            params.put("plan_type",planType);
            if (engineerAdminAccess!=null){
                params.put("customer_id",engineerAdminAccess.getCustomer_id());
                params.put("active",engineerAdminAccess.isActive());
                params.put("plan_id",engineerAdminAccess.getPlanId());
                params.put("subscription_id",engineerAdminAccess.getSubscriptionId());
            }
            client.addHeader(Constants.Authorization,authToken);
            client.post(APIs.SUBSCRIBE_ENGINEER_ADMIN_ACCESS, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int i, Header[] headers, byte[] bytes) {
                    String responseString = new String(bytes);
                    Log.e(TAG,responseString);
                    Intent intent = new Intent(ACTION_ENGINEER_PAYMENT);;
                    try {
                        JSONObject response = new JSONObject(responseString);
                        if (response.getInt("status")==1){
                            Toast.makeText(getApplicationContext(), response.getString("msg"), Toast.LENGTH_LONG).show();
                            intent.putExtra(PAYMENT_RESPONSE_STATUS,1);
                            sendFinishBroadCast(intent);
                        }
                        else{
                            Toast.makeText(getApplicationContext(), response.getString("msg"), Toast.LENGTH_LONG).show();
                            intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                            sendFinishBroadCast(intent);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                        sendFinishBroadCast(intent);
                    }
                }

                @Override
                public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                    Intent intent = new Intent(ACTION_ENGINEER_PAYMENT);
                    throwable.printStackTrace();
                    Log.e(TAG,throwable.getMessage());
                    Log.e(TAG,new String(bytes));
                    Toast.makeText(getApplicationContext(), "Network Error - Payment Failed", Toast.LENGTH_SHORT).show();
                    intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                    sendFinishBroadCast(intent);
                }
            });
        }
        else{
            Toast.makeText(getApplicationContext(), "User Auth Failed", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ACTION_ENGINEER_PAYMENT);
            intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
            sendFinishBroadCast(intent);
        }
    }


    private void generateStripeTokenForAnalyzer(CreditCard card, final String customer_id){
        final Stripe stripe = new Stripe(getApplicationContext(), STRIPE_KEY);
        final Card sCard = new Card(card.getCardNumber(), card.getExpireMonth(), card.getYear(), card.getCvc());
        SourceParams cardSourceParams = SourceParams.createCardParams(sCard);
        stripe.createSource(cardSourceParams, new SourceCallback() {
            @Override
            public void onError(Exception error) {
                error.printStackTrace();
                Intent intent = new Intent(ACTION_ANALYZER_PAYMENT);
                Toast.makeText(getApplicationContext(), "Network Error - Payment Failed", Toast.LENGTH_SHORT).show();
                intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                sendFinishBroadCast(intent);
            }

            @Override
            public void onSuccess(Source source) {
                // create your source as described
                SourceCardData cardData = (SourceCardData) source.getSourceTypeModel();
                String threeDStatus = cardData.getThreeDSecureStatus();
                if (SourceCardData.REQUIRED.equals(threeDStatus)) {
                    // this is the case where you would need to conduct a 3DS check
                }
                chargeViaServerForAnalyzer(source.getId(),customer_id);
            }
        });
    }

    private void chargeViaServerForAnalyzer(final String sourceId, final String customer_id){
        boolean valid = true;
        String idToken = PreferenceUtil.getPref(this).getString(PreferenceKeys.ID_TOKEN,"");
        long timestamp = PreferenceUtil.getPref(this).getLong(PreferenceKeys.ID_TOKEN_EXPIRATION,-1);
        if (idToken.isEmpty()){
            valid = false;
        }
        else{
            long diff = timestamp - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
            if (diff<100){
                valid = false;
            }
        }
        if (valid){
            chargeAnalyzer(idToken,sourceId, customer_id);
        }
        else{
            if (mAuth.getCurrentUser()!=null){
                mAuth.getCurrentUser().getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                    if (task.isSuccessful()){
                        PreferenceUtil.getPref(ProcessPaymentService.this).edit().putString(PreferenceKeys.ID_TOKEN,task.getResult().getToken()).apply();
                        PreferenceUtil.getPref(ProcessPaymentService.this).edit().putLong(PreferenceKeys.ID_TOKEN_EXPIRATION,task.getResult().getExpirationTimestamp()).apply();
                        chargeAnalyzer(task.getResult().getToken(), sourceId, customer_id);
                    }
                    else{
                        Intent intent = new Intent(ACTION_ANALYZER_PAYMENT);
                        Toast.makeText(getApplicationContext(), "Network Error - Payment Failed", Toast.LENGTH_SHORT).show();
                        intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                        sendFinishBroadCast(intent);
                    }
                    }
                });
            }
        }
    }

    private void chargeAnalyzer(final String authToken, final String sourceId, String customer_id){
        if (mAuth.getCurrentUser()!=null){
            AsyncHttpClient client = new AsyncHttpClient();
            try {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
                sf.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                client.setSSLSocketFactory(sf);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            RequestParams params = new RequestParams();
            params.put("source",sourceId);
            params.put("customer_id",customer_id);
            client.addHeader(Constants.Authorization,authToken);
            client.post(APIs.SUBSCRIBE_AUDIO_ANALYZER, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int i, Header[] headers, byte[] bytes) {
                    String responseString = new String(bytes);
                    Log.e(TAG,responseString);
                    Intent intent = new Intent(ACTION_ANALYZER_PAYMENT);;
                    try {
                        JSONObject response = new JSONObject(responseString);
                        if (response.getInt("status")==1){
                            Toast.makeText(getApplicationContext(), response.getString("msg"), Toast.LENGTH_LONG).show();
                            intent.putExtra(PAYMENT_RESPONSE_STATUS,1);
                            sendFinishBroadCast(intent);
                        }
                        else{
                            Toast.makeText(getApplicationContext(), response.getString("msg"), Toast.LENGTH_LONG).show();
                            intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                            sendFinishBroadCast(intent);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                        sendFinishBroadCast(intent);
                    }
                }

                @Override
                public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                    Intent intent = new Intent(ACTION_ANALYZER_PAYMENT);
                    throwable.printStackTrace();
                    Log.e(TAG,throwable.getMessage());
                    Toast.makeText(getApplicationContext(), "Network Error - Payment Failed", Toast.LENGTH_SHORT).show();
                    intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                    sendFinishBroadCast(intent);
                }
            });
        }
        else{
            Toast.makeText(getApplicationContext(), "User Auth Failed", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ACTION_ANALYZER_PAYMENT);
            intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
            sendFinishBroadCast(intent);
        }
    }

    private void generateStripeTokenForCollaboration(CreditCard card, final String planType) {
        Stripe stripe = new Stripe(getApplicationContext(), STRIPE_KEY);
        final Card sCard = new Card(card.getCardNumber(), card.getExpireMonth(), card.getYear(), card.getCvc());
        SourceParams cardSourceParams = SourceParams.createCardParams(sCard);
        stripe.createSource(cardSourceParams, new SourceCallback() {
            @Override
            public void onError(Exception error) {
                error.printStackTrace();
                Intent intent = new Intent(ACTION_COLLABORATION_PAYMENT);
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_error_payment_failed), Toast.LENGTH_SHORT).show();
                intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                sendFinishBroadCast(intent);
            }

            @Override
            public void onSuccess(Source source) {
                // create your source as described
                SourceCardData cardData = (SourceCardData) source.getSourceTypeModel();
                String threeDStatus = cardData.getThreeDSecureStatus();
                if (SourceCardData.REQUIRED.equals(threeDStatus)) {
                    // this is the case where you would need to conduct a 3DS check
                }
                chargeViaServerForCollaborator(source.getId(), planType);
            }
        });
    }


    private void chargeViaServerForCollaborator(final String sourceId, final String planType){
        boolean valid = true;
        String idToken = PreferenceUtil.getPref(this).getString(PreferenceKeys.ID_TOKEN,"");
        long timestamp = PreferenceUtil.getPref(this).getLong(PreferenceKeys.ID_TOKEN_EXPIRATION,-1);
        if (idToken.isEmpty()){
            valid = false;
        }
        else{
            long diff = timestamp - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
            if (diff<100){
                valid = false;
            }
        }
        if (valid){
            chargeCollaborator(idToken, sourceId, planType);
        }
        else{
            if (mAuth.getCurrentUser()!=null){
                mAuth.getCurrentUser().getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()){
                            PreferenceUtil.getPref(ProcessPaymentService.this).edit().putString(PreferenceKeys.ID_TOKEN,task.getResult().getToken()).apply();
                            PreferenceUtil.getPref(ProcessPaymentService.this).edit().putLong(PreferenceKeys.ID_TOKEN_EXPIRATION,task.getResult().getExpirationTimestamp()).apply();
                            chargeCollaborator(task.getResult().getToken(), sourceId, planType);
                        }
                        else{
                            Intent intent = new Intent(ACTION_COLLABORATION_PAYMENT);
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_error_payment_failed), Toast.LENGTH_SHORT).show();
                            intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                            sendFinishBroadCast(intent);
                        }
                    }
                });
            }
        }
    }

    private void chargeCollaborator(final String authToken, final String sourceId, final String planType){
        if (mAuth.getCurrentUser()!=null){
            AsyncHttpClient client = new AsyncHttpClient();
            client.setTimeout(20000);
            //client.setMaxRetriesAndTimeout(1, 5000);
            try {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
                sf.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                client.setSSLSocketFactory(sf);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            final RequestParams params = new RequestParams();
            params.put("source",sourceId);
            params.put("plan_type",planType);
            params.put("plan_id","plan_DNFE6cmeKhKhA3");
            params.put("is_subscribe",false);
            params.put("subscription_id","");
            params.put("customer_id","");
            params.put("active","");
            /*if (engineerAdminAccess!=null){
                params.put("customer_id",engineerAdminAccess.getCustomer_id());
                params.put("active",engineerAdminAccess.isActive());
                params.put("plan_id",engineerAdminAccess.getPlanId());
                params.put("subscription_id",engineerAdminAccess.getSubscriptionId());
            }*/
            client.addHeader(Constants.Authorization, authToken);
            client.post(APIs.SUBSCRIBE_COLLABORATOR, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int i, Header[] headers, byte[] bytes) {
                    String responseString = new String(bytes);
                    Log.e(TAG,responseString);
                    Intent intent = new Intent(ACTION_COLLABORATION_PAYMENT);
                    try {
                        JSONObject response = new JSONObject(responseString);
                        if (response.getInt("status")==1){
                            Toast.makeText(getApplicationContext(), response.getString("msg"), Toast.LENGTH_LONG).show();
                            intent.putExtra(PAYMENT_RESPONSE_STATUS,1);
                            sendFinishBroadCast(intent);
                        }
                        else{
                            Toast.makeText(getApplicationContext(), response.getString("msg"), Toast.LENGTH_LONG).show();
                            intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                            sendFinishBroadCast(intent);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                        sendFinishBroadCast(intent);
                    }
                }

                @Override
                public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                    System.out.println(i + "-------------> " + Arrays.toString(bytes));
                    Intent intent = new Intent(ACTION_COLLABORATION_PAYMENT);
                    throwable.printStackTrace();
                    Log.e(TAG,throwable.getMessage());
                    //Log.e(TAG,new String(bytes));
                    Toast.makeText(getApplicationContext(), "Network Error - Payment Failed", Toast.LENGTH_SHORT).show();
                    intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                    sendFinishBroadCast(intent);
                }
            });
        }
        else{
            Toast.makeText(getApplicationContext(), "User Auth Failed", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ACTION_COLLABORATION_PAYMENT);
            intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
            sendFinishBroadCast(intent);
        }
    }


    private void generateStripeTokenForBeat(CreditCard card){
        Stripe stripe = new Stripe(getApplicationContext(), STRIPE_KEY);
        stripe.createToken(
            new Card(card.getCardNumber(), card.getExpireMonth(), card.getYear(), card.getCvc()),
            new TokenCallback() {
                public void onSuccess(Token token) {
                    Log.e(TAG,token.getId());
                    chargeViaServerforBeat(token.getId());
                }
                public void onError(Exception error) {
                    Toast.makeText(getApplicationContext(),
                        error.getLocalizedMessage(),
                        Toast.LENGTH_LONG
                    ).show();
                }
            }
        );
    }

    private void chargeViaServerforBeat(final String payment_token){
        boolean valid = true;
        String idToken = PreferenceUtil.getPref(this).getString(PreferenceKeys.ID_TOKEN,"");
        long timestamp = PreferenceUtil.getPref(this).getLong(PreferenceKeys.ID_TOKEN_EXPIRATION,-1);
        if (idToken.isEmpty()){
            valid = false;
        }
        else{
            long diff = timestamp - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
            if (diff<100){
                valid = false;
            }
        }
        if (valid){
            chargeMarketPlace(idToken,payment_token);
        }
        else{
            if (mAuth.getCurrentUser()!=null){
                mAuth.getCurrentUser().getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                    if (task.isSuccessful()){
                        PreferenceUtil.getPref(ProcessPaymentService.this).edit().putString(PreferenceKeys.ID_TOKEN,task.getResult().getToken()).apply();
                        PreferenceUtil.getPref(ProcessPaymentService.this).edit().putLong(PreferenceKeys.ID_TOKEN_EXPIRATION,task.getResult().getExpirationTimestamp()).apply();
                        chargeMarketPlace(task.getResult().getToken(), payment_token);
                    }
                    }
                });
            }
        }
    }

    private void chargeMarketPlace(final String authToken,final String payment_token){
        if (mAuth.getCurrentUser()!=null){
            mAuth.getCurrentUser().getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                @Override
                public void onComplete(@NonNull Task<GetTokenResult> task) {
                    if (task.isSuccessful()){
                        AsyncHttpClient client = new AsyncHttpClient();
                        try {
                            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                            trustStore.load(null, null);
                            MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
                            sf.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                            client.setSSLSocketFactory(sf);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        RequestParams params = new RequestParams();
                        params.put("purchase_token",payment_token);
                        params.put("beat_id",beat.getId());
                        client.addHeader(Constants.Authorization,authToken);
                        client.post(APIs.CHARGE_URL, params, new AsyncHttpResponseHandler() {
                            @Override
                            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                                String responseString = new String(bytes);
                                Log.e(TAG,responseString);
                                Intent intent = new Intent(ACTION_PAYMENT_MARKETPLACE);
                                try {
                                    JSONObject response = new JSONObject(responseString);
                                    if (response.getInt("status")==1){
                                        Toast.makeText(getApplicationContext(), response.getString("msg"), Toast.LENGTH_LONG).show();
                                        intent.putExtra(PARAM_PAYMENT_RESPONSE,responseString);
                                        intent.putExtra(PAYMENT_RESPONSE_STATUS,1);
                                        sendFinishBroadCast(intent);
                                    }
                                    else{
                                        Toast.makeText(getApplicationContext(), response.getString("msg"), Toast.LENGTH_LONG).show();
                                        intent.putExtra(PARAM_PAYMENT_RESPONSE,responseString);
                                        intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                                        sendFinishBroadCast(intent);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                                    sendFinishBroadCast(intent);
                                }
                            }

                            @Override
                            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                                Intent intent = new Intent(ACTION_PAYMENT_MARKETPLACE);
                                Toast.makeText(getApplicationContext(), "Payment Failed", Toast.LENGTH_SHORT).show();
                                intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                                sendFinishBroadCast(intent);
                            }
                        });
                    }
                    else{
                        Intent intent = new Intent(ACTION_PAYMENT_MARKETPLACE);
                        intent.putExtra(PAYMENT_RESPONSE_STATUS,0);
                        sendFinishBroadCast(intent);
                        Toast.makeText(getApplicationContext(), "User Auth Failed", Toast.LENGTH_SHORT).show();
                    }


                }
            });
        }
        else{
            Intent intent = new Intent(ACTION_PAYMENT_MARKETPLACE);
            Toast.makeText(getApplicationContext(), "User Auth Failed", Toast.LENGTH_SHORT).show();
            sendFinishBroadCast(intent);
        }
    }

    private void sendFinishBroadCast(Intent localIntent){
        LocalBroadcastManager.getInstance(ProcessPaymentService.this).sendBroadcast(localIntent);
    }
}
