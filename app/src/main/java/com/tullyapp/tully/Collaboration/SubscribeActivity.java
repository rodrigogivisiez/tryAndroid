package com.tullyapp.tully.Collaboration;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.tullyapp.tully.R;

/**
 * Created by Santosh on 5/9/18.
 */
public class SubscribeActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnSubscribe;
    private ImageView ivCancel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);
        getSupportActionBar().hide();

        initUI();
    }

    private void initUI() {
        btnSubscribe = (Button) findViewById(R.id.btn_subscribe);
        ivCancel = (ImageView) findViewById(R.id.iv_cancel);
        btnSubscribe.setOnClickListener(this);
        ivCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_subscribe:
                Intent intent = new Intent(SubscribeActivity.this, CollaboratorPaymentActivity.class);
                startActivity(intent);
                finish();
                break;

            case R.id.iv_cancel:
                finish();
                break;
            default:
        }
    }
}