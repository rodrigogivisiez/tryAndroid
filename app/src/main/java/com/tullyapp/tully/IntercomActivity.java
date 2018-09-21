package com.tullyapp.tully;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import io.intercom.android.sdk.Intercom;

public class IntercomActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intercom);
        Intercom.client().setLauncherVisibility(Intercom.Visibility.VISIBLE);
    }
}
