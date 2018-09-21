package com.tullyapp.tully;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.tullyapp.tully.Utils.FingerprintHandler;
import com.tullyapp.tully.Utils.PreferenceKeys;
import com.tullyapp.tully.Utils.PreferenceUtil;

import static com.tullyapp.tully.Utils.Constants.REQUEST_ONE_TOUCH;

public class SwitchTouchSignInActivity extends AppCompatActivity {

    private SwitchCompat touch_switch;
    private String oneTouchAuth;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch_touch_sign_in);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("One Touch Sign In");

        touch_switch = findViewById(R.id.touch_switch);

        oneTouchAuth = PreferenceUtil.getPref(this).getString(PreferenceKeys.ONE_TOUCH_AUTH,"");
        if (!oneTouchAuth.isEmpty()){
            touch_switch.setChecked(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean boo = FingerprintHandler.fingerPrintisHardwareDetected(SwitchTouchSignInActivity.this);
            if (!boo){
                touch_switch.setEnabled(false);
                Toast.makeText(this, "Finger Print Recognizer hardware not detected", Toast.LENGTH_SHORT).show();
            }
            else{
                touch_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked){
                            intent = new Intent(SwitchTouchSignInActivity.this,OneTouchLoginActivity.class);
                            startActivityForResult(intent,REQUEST_ONE_TOUCH);
                        }else{
                            PreferenceUtil.getPref(SwitchTouchSignInActivity.this).edit().remove(PreferenceKeys.ONE_TOUCH_AUTH).apply();
                        }
                    }
                });
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQUEST_ONE_TOUCH:
                if (resultCode == Activity.RESULT_OK){
                    touch_switch.setChecked(true);
                }
                else{
                    touch_switch.setChecked(false);
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }
}
