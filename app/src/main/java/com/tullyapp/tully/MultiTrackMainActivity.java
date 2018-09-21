package com.tullyapp.tully;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.tullyapp.tully.Multitrack.MultiTrackProject;

import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT_ID;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT_MAIN_FILE;
import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT_NAME;

public class MultiTrackMainActivity extends AppCompatActivity {

    //private MenuItem action_record, action_music, action_copy;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_track_main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mAuth = FirebaseAuth.getInstance();
        String projectId = getIntent().getStringExtra(INTENT_PARAM_PROJECT_ID);
        String mainFile = getIntent().getStringExtra(INTENT_PARAM_PROJECT_MAIN_FILE);
        String projectName = getIntent().getStringExtra(INTENT_PARAM_PROJECT_NAME);
        if (projectId ==null || mAuth.getCurrentUser()==null)
            onBackPressed();
        else{
            addFragment(MultiTrackProject.newInstance(projectId, projectName, mainFile),false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /*getMenuInflater().inflate(R.menu.multitrack_menu, menu);
        action_record = menu.findItem(R.id.action_record);
        action_music = menu.findItem(R.id.action_music);
        action_copy = menu.findItem(R.id.action_copy);*/
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()){

            case R.id.action_record:
                break;

            case R.id.action_music:
                break;

            case R.id.action_copy:
                break;

            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
        return true;
    }

    private void addFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_up, R.animator.fade_out, R.animator.fade_out, R.animator.fade_out);
        if (addToBackStack) {
            fragmentTransaction.add(R.id.container, fragment);
            fragmentTransaction.addToBackStack(null);
        } else {
            fragmentTransaction.replace(R.id.container, fragment);
        }
        //fragmentTransaction.commit();
        fragmentTransaction.commit();
    }

}