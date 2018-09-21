package com.tullyapp.tully;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tullyapp.tully.Utils.Configuration;
import com.tullyapp.tully.Utils.PreferenceKeys;
import com.tullyapp.tully.Utils.PreferenceUtil;

import static com.tullyapp.tully.Utils.Constants.TUTS_HOME;
import static com.tullyapp.tully.Utils.Constants.TUTS_LYRICS;
import static com.tullyapp.tully.Utils.Constants.TUTS_MARKET_PLACE;
import static com.tullyapp.tully.Utils.Constants.TUTS_PLAY;
import static com.tullyapp.tully.Utils.Constants.TUTS_RECORDING;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = SplashActivity.class.getSimpleName();
    private DatabaseReference mDatabase;
    private boolean IS_ARTIST_PROFILE_READY = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        IS_ARTIST_PROFILE_READY = PreferenceUtil.getPref(this).getBoolean(PreferenceKeys.IS_ARTIST_PROFILE_READY, false);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user !=null){
            Log.e("UID", user.getUid());
            mDatabase = FirebaseDatabase.getInstance().getReference().child(user.getUid());
            /*user.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                @Override
                public void onComplete(@NonNull Task<GetTokenResult> task) {
                    if (task.isSuccessful()){
                        Log.e(TAG,task.getResult().getToken());
                    }
                    else{
                        Log.e(TAG,"FAILED");
                    }
                }
            });*/
            getConfiguration();
        }else{
            NavigateTo(LoginActivity.class);
        }
    }

    private void checkProfile(){
        mDatabase.child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    PreferenceUtil.getPref(SplashActivity.this).edit().putBoolean(PreferenceKeys.IS_ARTIST_PROFILE_READY,true).apply();
                    NavigateTo(HomeActivity.class);
                }
                else{
                    NavigateTo(ArtistCreationActivity.class);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Err",databaseError.getMessage());
                NavigateTo(HomeActivity.class);
            }
        });
    }

    private void getConfiguration(){
        mDatabase.child("settings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){

                    if (dataSnapshot.child("marketPlace").exists()){
                        Configuration.market_watch = (boolean) dataSnapshot.child("marketPlace").getValue();
                    }
                    if (dataSnapshot.child("pushNotification").exists()){
                        Configuration.pushNotification = (boolean) dataSnapshot.child("pushNotification").getValue();
                    }

                    final String TUTS_SCREEN = "tutorial_screens";

                    if (dataSnapshot.child(TUTS_SCREEN).exists()){
                        String[] tutorial_screens = {TUTS_HOME, TUTS_LYRICS, TUTS_PLAY, TUTS_RECORDING, TUTS_MARKET_PLACE};
                        for (String screen : tutorial_screens){
                            if (dataSnapshot.child(TUTS_SCREEN).child(screen).exists()){
                                boolean boo = (boolean) dataSnapshot.child(TUTS_SCREEN).child(screen).getValue();
                                switch (screen){
                                    case TUTS_HOME:
                                        Configuration.home_tuts = boo;
                                        break;

                                    case TUTS_LYRICS:
                                        Configuration.lyrics_tuts = boo;
                                        break;

                                    case TUTS_PLAY:
                                        Configuration.play_tuts = boo;
                                        break;

                                    case TUTS_MARKET_PLACE:
                                        Configuration.marketplace_tuts = boo;
                                        break;

                                    case TUTS_RECORDING:
                                        Configuration.recording_tuts = boo;
                                        break;
                                }
                            }
                        }
                    }
                }

                if (IS_ARTIST_PROFILE_READY){
                    NavigateTo(HomeActivity.class);
                }
                else{
                    checkProfile();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                checkProfile();
            }
        });
    }

    private void NavigateTo(final Class classname){
        Intent intent = new Intent(SplashActivity.this,classname);
        startActivity(intent);
        SplashActivity.this.finish();
    }
}
