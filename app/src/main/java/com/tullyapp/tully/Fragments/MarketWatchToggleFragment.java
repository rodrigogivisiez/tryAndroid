package com.tullyapp.tully.Fragments;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.tullyapp.tully.Adapters.ArtistOptionAdapter;
import com.tullyapp.tully.Dialogs.TutorialScreen;
import com.tullyapp.tully.Models.ArtistOption;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.CircleTransform;
import com.tullyapp.tully.Utils.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static com.tullyapp.tully.Utils.Constants.TUTORIAL_SCREENS;
import static com.tullyapp.tully.Utils.Constants.TUTS_MARKET_PLACE;


public class MarketWatchToggleFragment extends BaseFragment implements View.OnClickListener {

    private SwitchCompat watch_switch;
    private FrameLayout confirm_layout;
    private FrameLayout market_watch_layout;
    private boolean isConfirmed = false;
    private ImageView profilePicture;
    private TextView tv_projectname, tv_learn_more;
    private RecyclerView recycle_view_artist_option;
    private ArrayList<ArtistOption> artistOptions = new ArrayList<>();
    private ArtistOptionAdapter artistOptionAdapter;
    private FirebaseAuth mAuth;
    private static final int REQUEST_CAMERA = 1888;
    private final int SELECT_FILE = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100;
    private DatabaseReference mDatabase;

    public MarketWatchToggleFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_market_watch_toggle, container, false);

        profilePicture = view.findViewById(R.id.profile_picture);
        tv_projectname = view.findViewById(R.id.tv_projectname);
        tv_learn_more = view.findViewById(R.id.tv_learn_more);

        recycle_view_artist_option = view.findViewById(R.id.recycle_view_artist_option);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recycle_view_artist_option.setLayoutManager(layoutManager);

        watch_switch = view.findViewById(R.id.watch_switch);
        market_watch_layout = view.findViewById(R.id.market_watch_layout);
        confirm_layout = view.findViewById(R.id.confirm_layout);

        if (Configuration.market_watch){
            watch_switch.setChecked(true);
            //market_watch_layout.setVisibility(View.INVISIBLE);
            //confirm_layout.setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profilePicture.setOnClickListener(this);
        artistOptionAdapter = new ArtistOptionAdapter(getContext(),artistOptions);
        recycle_view_artist_option.setAdapter(artistOptionAdapter);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid());
        tv_projectname.setText(mAuth.getCurrentUser().getDisplayName());
        watch_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked){
                mDatabase.child("settings").child("marketPlace").setValue(true);
                Configuration.market_watch = true;
                market_watch_layout.setVisibility(View.INVISIBLE);
                confirm_layout.setVisibility(View.VISIBLE);
            }
            }
        });

        tv_learn_more.setOnClickListener(this);

        try{
            Picasso.with(getContext()).load(mAuth.getCurrentUser().getPhotoUrl()).placeholder(R.drawable.default_profile_picture).transform(new CircleTransform()).into(profilePicture);
        }catch (Exception e){
            e.printStackTrace();
        }
        fetchArtistOptions();

        if (!Configuration.marketplace_tuts) showTutorailDialog(TUTS_MARKET_PLACE);
    }


    private void showTutorailDialog(String tut) {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        TutorialScreen tutorialScreen = TutorialScreen.newInstance(tut);
        tutorialScreen.show(ft,TutorialScreen.class.getSimpleName());
        tutorialScreen.setOnTutorialClosed(new TutorialScreen.OnTutorialClosed() {
            @Override
            public void onTutsClosed(String tut) {
                mDatabase.child(TUTORIAL_SCREENS).child(tut).setValue(true);
                Configuration.marketplace_tuts = true;
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case SELECT_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    onSelectFromGalleryResult(data);
                }
                break;

            case REQUEST_CAMERA:
                if (resultCode == Activity.RESULT_OK) {
                    onCaptureImageResult(data);
                }
                break;
        }
    }

    private void selectImage() {
        final CharSequence[] items = { "Take Photo", "Choose from Library", "Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result= ProfileFragment.Utility.checkPermission(getContext());

                if (items[item].equals("Take Photo")) {
                    if(result)
                        cameraIntent();

                } else if (items[item].equals("Choose from Library")) {
                    if(result)
                        galleryIntent();

                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void cameraIntent(){
        if (ContextCompat.checkSelfPermission(getContext(),android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
        else{
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, REQUEST_CAMERA);
            }
            else {
                Toast.makeText(getContext(), "Camera Permission failed", Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                galleryIntent();
            }
        }
    }

    private void onSelectFromGalleryResult(Intent data) {
        Bitmap bm;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), data.getData());
                bm=getResizedBitmap(bm,500,500);
                if (bm!=null){
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    bm.compress(Bitmap.CompressFormat.JPEG, 80, bytes);
                    FirebaseDatabaseOperations.uploadProfilePic(getContext(),bytes.toByteArray());
                    CircleTransform circleTransform = new CircleTransform();
                    bm = circleTransform.transform(bm);
                    profilePicture.setImageBitmap(bm);
                }
                else{
                    Toast.makeText(getContext(), "Failed getting image", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            catch (Exception e){
                e.printStackTrace();
                Toast.makeText(getContext(), "Failed getting image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        try{
            int width = bm.getWidth();
            int height = bm.getHeight();

            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) newHeight / (float) newWidth;

            int finalWidth = newWidth;
            int finalHeight = newHeight;

            if (ratioMax > 1) {
                finalWidth = (int) ((float)newHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float)newWidth / ratioBitmap);
            }

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, finalWidth, finalHeight, true);

            Log.e("SIZE",resizedBitmap.getWidth()+","+resizedBitmap.getHeight());

            return resizedBitmap;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail = getResizedBitmap(thumbnail,500,500);
        if (thumbnail!=null){
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, bytes);
            File destination = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".jpg");
            FileOutputStream fo;
            try {
                destination.createNewFile();
                fo = new FileOutputStream(destination);
                fo.write(bytes.toByteArray());
                fo.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FirebaseDatabaseOperations.uploadProfilePic(getContext(),bytes.toByteArray());
            CircleTransform circleTransform = new CircleTransform();
            thumbnail = circleTransform.transform(thumbnail);
            profilePicture.setImageBitmap(thumbnail);
        }
        else{
            Toast.makeText(getContext(),"Image failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void galleryIntent(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select File"),SELECT_FILE);
    }

    private void fetchArtistOptions(){
        try{
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid()+"/profile");
            mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()){
                        String artistOption = (String) dataSnapshot.child("artist_option").getValue();
                        if (artistOption!=null && !artistOption.isEmpty()){
                            String[] vals = artistOption.split(",");
                            artistOptions.clear();
                            if (vals.length>0){
                                for (String anArtistOptionsArray : vals) {
                                    artistOptions.add(new ArtistOption(R.drawable.profile_ico_profile, anArtistOptionsArray));
                                }
                                artistOptionAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                    else{

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.profile_picture:
                selectImage();
                break;

            case R.id.tv_learn_more:
                market_watch_layout.setVisibility(View.VISIBLE);
                confirm_layout.setVisibility(View.INVISIBLE);
                break;
        }
    }

    @Override
    public void onSearchKey(String searchKey) {

    }

    @Override
    public void onSearchCancelled() {

    }

    @Override
    public void fabButtonClicked() {

    }

    @Override
    public void actionEvent(int event) {

    }
}
