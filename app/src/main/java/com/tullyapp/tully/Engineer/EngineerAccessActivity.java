package com.tullyapp.tully.Engineer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.tullyapp.tully.Models.EngineerAccessModel;
import com.tullyapp.tully.R;

import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_INVITE_ENGINEER;

public class EngineerAccessActivity extends AppCompatActivity implements EngineerInviteFragment.FragmentEvent, EngineerAccessFragment.EngineerActions {

    private static final String TAG = EngineerAccessActivity.class.getSimpleName();
    private FirebaseAuth mAuth;
    private FragmentManager fManager;
    private EngineerInviteFragment engineerInviteFragment;
    private boolean flag = false;
    private EngineerAccessFragment engineerAccessFragment;
    private EngineerSettingsFragment engineerSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_engineer_access);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Engineer Access");
        mAuth = FirebaseAuth.getInstance();
        fManager = getSupportFragmentManager();
        flag = false;
        boolean openInvite = getIntent().getBooleanExtra(INTENT_PARAM_INVITE_ENGINEER, false);
        if (mAuth.getCurrentUser()!=null){
            if (openInvite){
                flag = true;
                engineerInviteFragment = EngineerInviteFragment.newInstance(true);
                engineerInviteFragment.setFragmentEvent(this);
                addFragment(engineerInviteFragment,false);
            }
            else{
                engineerAccessFragment = EngineerAccessFragment.newInstance();
                engineerAccessFragment.setEngineerActions(this);
                addFragment(engineerAccessFragment,false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add) {
            if (!flag){
                flag = true;
                engineerInviteFragment = EngineerInviteFragment.newInstance(true);
                engineerInviteFragment.setFragmentEvent(this);
                addFragment(engineerInviteFragment,true);
            }
            return true;
        }
        else if (id == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = fManager.beginTransaction();
        //fragmentTransaction.setCustomAnimations(R.anim.slide_in_up, R.animator.fade_out, R.animator.fade_out, R.animator.fade_out);
        if (addToBackStack) {
            fragmentTransaction.replace(R.id.container, fragment);
            fragmentTransaction.addToBackStack(null);
        } else {
            fragmentTransaction.replace(R.id.container, fragment);
        }
        fragmentTransaction.commit();
        //fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onBack() {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        flag = false;
    }

    @Override
    public void onListEngineer() {
        engineerAccessFragment = EngineerAccessFragment.newInstance();
        engineerAccessFragment.setEngineerActions(this);
        addFragment(engineerAccessFragment,false);
    }

    @Override
    public void onEngineerIndividual(EngineerAccessModel engineerAccessModel) {
        engineerSettingsFragment = EngineerSettingsFragment.newInstance(engineerAccessModel);
        addFragment(engineerSettingsFragment,true);
    }

}
