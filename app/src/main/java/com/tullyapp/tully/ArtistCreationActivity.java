package com.tullyapp.tully;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tullyapp.tully.FirebaseDataModels.Profile;
import com.tullyapp.tully.Models.CollaborationSubscription;
import com.tullyapp.tully.Utils.ExtendedNumberPicker;
import com.tullyapp.tully.Utils.PreferenceKeys;
import com.tullyapp.tully.Utils.PreferenceUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.tullyapp.tully.Utils.Utils.showAlert;

public class ArtistCreationActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private Button btn_saveprofile;
    private EditText et_artistname;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ArrayList artist_options;
    private ProgressBar progressBar;
    private String selectedGenere = "";
    private ExtendedNumberPicker numberPicker;
    private String[] genere;
    private AppCompatCheckBox checkBox1, checkBox2, checkBox3, checkBox4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_creation);
        mAuth = FirebaseAuth.getInstance();
        initUI();
    }

    private void initUI(){
        et_artistname = findViewById(R.id.et_artistname);

        btn_saveprofile = findViewById(R.id.btn_saveprofile);

        btn_saveprofile.setOnClickListener(this);

        artist_options = new ArrayList();
        progressBar = findViewById(R.id.progressBar);

        genere = getResources().getStringArray(R.array.genere);

        numberPicker = findViewById(R.id.numberPicker);
        numberPicker.setDisplayedValues( null );
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(genere.length-1);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setDisplayedValues( genere );

        checkBox1 = findViewById(R.id.checkbox1);
        checkBox2 = findViewById(R.id.checkbox2);
        checkBox3 = findViewById(R.id.checkbox3);
        checkBox4 = findViewById(R.id.checkbox4);

        checkBox1.setOnCheckedChangeListener(this);
        checkBox2.setOnCheckedChangeListener(this);
        checkBox3.setOnCheckedChangeListener(this);
        checkBox4.setOnCheckedChangeListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_saveprofile:
                createProfile();
                break;
        }
    }

    private void createProfile(){

        selectedGenere = genere[numberPicker.getValue()];

        String artistName = et_artistname.getText().toString();

        if (artistName.isEmpty())
            showAlert(ArtistCreationActivity.this,"Validation Error","Artist name is required");

        else if (artist_options.size()==0)
            showAlert(ArtistCreationActivity.this,"Validation Error","Atleast one artist option is required");

        else if (selectedGenere.isEmpty())
            showAlert(ArtistCreationActivity.this,"Validation Error","Please select genere from the list");

        else{
            progressBar.setVisibility(View.VISIBLE);
            final Profile profile = new Profile();
            profile.setArtist_name(artistName);
            profile.setEmail(mAuth.getCurrentUser().getEmail());
            profile.setGenre(selectedGenere);
            profile.setArtist_option(android.text.TextUtils.join(",", artist_options));

            UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder().setDisplayName(artistName).build();
            mAuth.getCurrentUser().updateProfile(profileUpdate).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    createProfileinDb(profile);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    createProfileinDb(profile);
                }
            });
        }
    }

    private void createProfileinDb(Profile profile){
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());

        Map<String, Object> settings = new HashMap<>();
        settings.put("pushNotification",true);
        settings.put("touchId",true);

        mDatabase.child("settings").updateChildren(settings);
        //mDatabase.child("settings").child("collaboration_subscription").child("is_subscribe").setValue(false);
        mDatabase.child("settings").child("collaboration_subscription").setValue(new CollaborationSubscription("android", false));

        mDatabase.child("profile").setValue(profile);
        PreferenceUtil.getPref(ArtistCreationActivity.this).edit().putBoolean(PreferenceKeys.IS_ARTIST_PROFILE_READY,true).apply();
        progressBar.setVisibility(View.GONE);
        Intent intent = new Intent(ArtistCreationActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(intent);
        ArtistCreationActivity.this.finish();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        try{
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        if (compoundButton.isChecked()){
            artist_options.add(compoundButton.getText().toString());
        }else{
            artist_options.remove(compoundButton.getText().toString());
        }
    }
}
