package com.tullyapp.tully;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.tullyapp.tully.Services.ReceivedCopyToTully;

public class ReceivedCopyToTullyActivity extends AppCompatActivity {

    private static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 21;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_received_copy_to_tully);
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser()!=null){
            receivedCopyToTully();
        }
    }

    private void receivedCopyToTully(){
        if (Intent.ACTION_VIEW.equals(getIntent().getAction()) || Intent.ACTION_SEND.equals(getIntent().getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    filePermissionsGranted();
                }
                else{
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_REQUEST_CODE);
                }
            }else{
                filePermissionsGranted();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            filePermissionsGranted();
        }
    }

    private void filePermissionsGranted(){
        Toast.makeText(this, "Uploading received file, might take few seconds", Toast.LENGTH_LONG).show();
        ReceivedCopyToTully.startReceivingCopyToTully(getApplicationContext(),getIntent());
        Intent intent = new Intent(ReceivedCopyToTullyActivity.this,HomeActivity.class);
        startActivity(intent);
        ReceivedCopyToTullyActivity.this.finish();
    }
}
