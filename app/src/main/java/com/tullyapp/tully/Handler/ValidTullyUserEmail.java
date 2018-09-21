package com.tullyapp.tully.Handler;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;
import com.loopj.android.http.SyncHttpClient;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.Models.InviteCollaborator;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Services.ProcessPaymentService;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.Constants;
import com.tullyapp.tully.Utils.PreferenceKeys;
import com.tullyapp.tully.Utils.PreferenceUtil;
import com.tullyapp.tully.Utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyStore;

import cz.msebera.android.httpclient.Header;

/**
 * Created by Santosh on 20/9/18.
 */
public class ValidTullyUserEmail extends AsyncTask<String, String, String> {

    private String email;
    private Context context;
    private ProgressBar progressBar;
    private Project project;
    private FirebaseAuth mAuth;
    private static final String TAG = ValidTullyUserEmail.class.getName();

    public ValidTullyUserEmail(Context context, String email, Project project, FirebaseAuth mAuth) {
        this.context = context;
        this.email = email;
        this.project = project;
        this.mAuth = mAuth;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
    }

    @Override
    protected String doInBackground(String... strings) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(20000);
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
        params.put("invite_email", email);
        String idToken = PreferenceUtil.getPref(context).getString(PreferenceKeys.ID_TOKEN,"");
        client.addHeader(Constants.Authorization, idToken);
        client.post(APIs.CHECK_EMAIL_EXISTANCE, params, new AsyncHttpResponseHandler(Looper.getMainLooper()) {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String responseString = new String(responseBody);
                Log.e(TAG, responseString);
                try {
                    JSONObject response = new JSONObject(responseString);
                    if (response.getInt("status") == 1){
                        String senderName = mAuth.getCurrentUser().getDisplayName();
                        FirebaseDatabase.getInstance()
                                .getReference("collaborations/"+project.getId()+"/invitations")
                                .push()
                                .setValue(new InviteCollaborator(mAuth.getCurrentUser().getUid(), senderName, email, project.getId(), false));
                        Utils.showToast(context, context.getString(R.string.successfully_invited));
                        //Toast.makeText(context, response.getString("msg"), Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(context, response.getString("msg"), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e(TAG, error.getMessage());
            }

        });
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        progressBar.setVisibility(View.GONE);
    }
}