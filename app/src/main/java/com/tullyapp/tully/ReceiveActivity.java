package com.tullyapp.tully;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static com.tullyapp.tully.Utils.Utils.getContentName;

public class ReceiveActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        if (Intent.ACTION_VIEW.equals(getIntent().getAction()))
        {
            Uri u = getIntent().getData();
            String scheme = u.getScheme();
            if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                InputStream is = null;
                try {
                    Log.e("FILENAME",getContentName(getContentResolver(),getIntent().getData()));
                    is = getContentResolver().openInputStream(getIntent().getData());
                    Log.e("AVAILABLE",is.available()+"");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {

                // handle as file uri

            }

            Log.e("DETAIL3",getIntent().getData().getEncodedPath());
            Log.e("DETAIL4",getIntent().getData().getLastPathSegment());
            Log.e("PATH",getIntent().getData().getPath());

            File file = new File(getIntent().getData().getPath());
            Log.e("EXIST",file.exists()+"");

            // do what you want with the file...
        }
    }


    private void saveFile(){
        //File file = new File(getFilesDir(), filename);
    }
}
