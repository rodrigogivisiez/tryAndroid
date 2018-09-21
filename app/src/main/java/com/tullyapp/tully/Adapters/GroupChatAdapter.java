package com.tullyapp.tully.Adapters;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.tullyapp.tully.Collaboration.GroupChatActivity;
import com.tullyapp.tully.Models.ChatMessage;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Utils.Constants;
import com.tullyapp.tully.Utils.FileDownloader;
import com.tullyapp.tully.Utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import io.intercom.android.sdk.utilities.Private;

import static com.facebook.FacebookSdk.getApplicationContext;


/**
 * Created by Santosh on 6/9/18.
 */

public class GroupChatAdapter extends FirebaseListAdapter<ChatMessage> {

    private GroupChatActivity groupChatActivity;
    TextView tvMessageTopDate;

    public GroupChatAdapter(GroupChatActivity groupChatActivity, @NonNull FirebaseListOptions<ChatMessage> options, TextView tvMessageTopDate) {
        super(options);
        this.groupChatActivity = groupChatActivity;
        this.tvMessageTopDate = tvMessageTopDate;
    }

    @Override
    protected void populateView(View v, final ChatMessage model, int position) {
        TextView tvMessage = (TextView) v.findViewById(R.id.tv_message);
        TextView tvMessageUserName  = (TextView) v.findViewById(R.id.tv_message_user_name);
        TextView tvMessageTime = (TextView) v.findViewById(R.id.tv_message_time);
        ImageView ivContent = (ImageView) v.findViewById(R.id.iv_content);
        final ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        LinearLayout llFileLayout = (LinearLayout) v.findViewById(R.id.file_layout);
        TextView tvFileName = (TextView) v.findViewById(R.id.tv_file_name);

        if (model.getMessageText().equals("")) {
            tvMessage.setText("");
            ivContent.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            llFileLayout.setVisibility(View.GONE);
            if (model.getFileURL().contains(Constants.IMAGES)) {
                Picasso.with(groupChatActivity)
                        .load(model.getFileURL())
                        .into(ivContent, new Callback() {
                            @Override
                            public void onSuccess() {
                                progressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError() {
                                progressBar.setVisibility(View.GONE);
                            }

                        });
            } else {
                progressBar.setVisibility(View.GONE);
                ivContent.setVisibility(View.GONE);
                llFileLayout.setVisibility(View.VISIBLE);
                tvFileName.setText(groupChatActivity.getString(R.string.tully_file_) + DateFormat.format(Constants.DATE_FORMAT_YYYYMMDD_HR_MM_SS, model.getMessageTime()));
            }

        } else {
            ivContent.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            llFileLayout.setVisibility(View.GONE);
            tvMessage.setText(model.getMessageText());
        }


        tvMessageUserName.setText(model.getMessageUser());

        String todayDate="", messagelistDate="" , yesterdayDate="";

        try{
            Date c = Calendar.getInstance().getTime();
            SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT_MONTH_DAY_YEAR);
            todayDate = df.format(c);

            messagelistDate = (String) DateFormat.format(Constants.DATE_FORMAT_MONTH_DAY_YEAR, model.getMessageTime());

            yesterdayDate = (String) DateFormat.format(Constants.DATE_FORMAT_MONTH_DAY_YEAR, Utils.yesterday());

        }catch (Exception e){
            e.printStackTrace();
        }


        if (todayDate.equalsIgnoreCase(messagelistDate)){
            tvMessageTopDate.setText(groupChatActivity.getResources().getString(R.string.today));
        }else if (yesterdayDate.equalsIgnoreCase(messagelistDate)){
            tvMessageTopDate.setText(groupChatActivity.getResources().getString(R.string.yesterday));
        }else {
            tvMessageTopDate.setText(messagelistDate);
        }

        tvMessageTime.setText(DateFormat.format(Constants.TIME_FORMAT_H_MM_A, model.getMessageTime()));

        ivContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadImageFromUrl(model.getFileURL());
            }
        });

        llFileLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadImageFromUrl(model.getFileURL());
            }
        });

    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ChatMessage chatMessage = getItem(position);

        if (chatMessage.getMessageUserId().equals(groupChatActivity.getLoggedInUserName())) {
            view = groupChatActivity.getLayoutInflater().inflate(R.layout.item_group_chat_out_message, viewGroup, false);
        } else {
            view = groupChatActivity.getLayoutInflater().inflate(R.layout.item_group_chat_in_message, viewGroup, false);
        }

        //generating view
        populateView(view, chatMessage, position);

        return view;
    }

    @Override
    public int getViewTypeCount() {
        // return the total number of view types. this value should never change
        // at runtime
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        // return a value between 0 and (getViewTypeCount - 1)
        return position % 2;
    }


    public void downloadImageFromUrl(String file_url){
        if (ActivityCompat.checkSelfPermission(groupChatActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(groupChatActivity,groupChatActivity.getString(R.string.no_permission),Toast.LENGTH_SHORT).show();
        } else {
            MyAsyncTask asyncTask = new MyAsyncTask();
            asyncTask.execute(file_url);
        }
    }


    class MyAsyncTask extends AsyncTask<String, String, String>{

        @Override
        protected String doInBackground(String... f_url) {
            try {

                File filename;
                URL url = new URL(f_url[0]);
                String fileUrl = f_url[0];

                String dateTimeCurrent = "";

                    try{
                        Date c = Calendar.getInstance().getTime();
                        SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT_DD_MM_YYYY_HR_MM_SS);
                        dateTimeCurrent = df.format(c);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                String path1 = android.os.Environment.getExternalStorageDirectory()
                        .toString();
                Log.i("in save()", "after mkdir");

                if (fileUrl.contains("images")) {

                    File file = new File(path1 + "/" + Constants.TULLY + "/images");
                    if (!file.exists())
                        file.mkdirs();
                    filename = new File(file.getAbsolutePath() + "/" + groupChatActivity.getResources().getString(R.string.tully_file_)+dateTimeCurrent + ".JPG");

                }else {

                    String extentionOfFile = ".pdf";

                    try{
                        String currentString = fileUrl;
                        String[] separated = currentString.split("~~~");
                        separated[1] = separated[1].trim();
                        if(fileUrl.contains("pdf")){
                            fileUrl = separated[0];
                            extentionOfFile = ".pdf";
                        }else if(fileUrl.contains("msword")){
                            fileUrl = separated[0];
                            extentionOfFile = ".msword";
                        }else {
                            fileUrl = separated[0];
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    File file = new File(path1 + "/" + Constants.TULLY + "/documents");
                    if (!file.exists())
                        file.mkdirs();
                    filename = new File(file.getAbsolutePath() + "/" + groupChatActivity.getResources().getString(R.string.tully_file_)+dateTimeCurrent + extentionOfFile);
                }


                Log.i("in save()", "after file");

                FileDownloader.downloadFile(fileUrl, filename);

            }
            catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }



        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            Toast.makeText(groupChatActivity,groupChatActivity.getString(R.string.download_successfully),Toast.LENGTH_SHORT).show();

        }

    }




}