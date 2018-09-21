package com.tullyapp.tully.Collaboration;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.tullyapp.tully.Adapters.GroupChatAdapter;

import com.tullyapp.tully.BottomSheet.BottomSheet;
import com.tullyapp.tully.FirebaseDataModels.Project;
import com.tullyapp.tully.Models.ChatMessage;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Utils.Constants;
import com.tullyapp.tully.Utils.Utils;

import java.io.IOException;
import java.util.UUID;


import static com.tullyapp.tully.Utils.Constants.INTENT_PARAM_PROJECT;
import static com.tullyapp.tully.Utils.Utils.getResizedBitmap;

/**
 * Created by Santosh on 5/9/18.
 */

public class GroupChatActivity extends AppCompatActivity implements View.OnClickListener, BottomSheet.Listener {

    private final int PICK_IMAGE_REQUEST = 71 , PICK_DOC_REQUEST = 81;

    private FirebaseListAdapter<ChatMessage> firebaseListAdapter;
    private ListView lvChats;
    private EditText etMessage;
    private TextView tvMessageTopDate;
    private BottomSheet mChatAttachment;
    private String loggedInUserName;
//    private String fileURL;
    private String userName;
    private String collaborationID;
    private Uri filePath;
    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private Project project;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);
        getSupportActionBar().setTitle(getString(R.string.group_chat));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Utils.hideSoftKeyboard(GroupChatActivity.this);
        project = (Project) getIntent().getSerializableExtra(INTENT_PARAM_PROJECT);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            collaborationID = extras.getString(Constants.COLLBORATION_ID);
        }
        
        initUI();

        checkPermissions();

        Query query = FirebaseDatabase.getInstance().getReference("collaborations/" + project.getId() + "/" + collaborationID + "/chats");
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        
        loggedInUserName = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseListOptions<ChatMessage> options = new FirebaseListOptions.Builder<ChatMessage>()
                .setQuery(query, ChatMessage.class)
                .setLayout(R.layout.activity_group_chat)
                .build();

        firebaseListAdapter = new GroupChatAdapter(this, options,tvMessageTopDate);
        lvChats.setAdapter(firebaseListAdapter);

        getUserName();
        
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(GroupChatActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(GroupChatActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.EXTERNAL_STORAGE_PERMISSION_CONSTANT);
        }
    }

    private void initUI() {
        etMessage = findViewById(R.id.et_message);
        tvMessageTopDate = findViewById(R.id.tv_message_top_date);
        ImageView ivSendMessage = findViewById(R.id.iv_send_message);
        ImageView ivAttach = findViewById(R.id.iv_attach);
        lvChats = findViewById(R.id.lv_chats);

        ivSendMessage.setOnClickListener(this);
        ivAttach.setOnClickListener(this);

        mChatAttachment = BottomSheet.newInstance(R.layout.add_chat_file_bottom_sheet);
        mChatAttachment.setmListener(this);
    }

    public void getUserName() {

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(firebaseAuth.getCurrentUser().getUid());
        databaseReference.child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){

                    if (dataSnapshot.child("artist_name").exists()){
                        userName =  (String) dataSnapshot.child("artist_name").getValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Error","Error: "+databaseError);
            }
        });

    }


    public String getLoggedInUserName() {
        return loggedInUserName;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_send_message:
                sendMessage();
                break;
            case R.id.iv_attach:
                try{
                        if (mChatAttachment.isAdded()){
                            mChatAttachment.dismiss();
                        }
                        mChatAttachment.show(getSupportFragmentManager(), mChatAttachment.getTag());

                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            default:
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseListAdapter.startListening();

    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseListAdapter.stopListening();
    }

    private void sendMessage() {
        if (etMessage.getText().toString().trim().equals("")) {
            Toast.makeText(GroupChatActivity.this,getString(R.string.please_enter_some_texts), Toast.LENGTH_SHORT).show();
        } else {

            FirebaseDatabase.getInstance()
                    .getReference("collaborations/"+project.getId()+"/"+collaborationID+"/chats")
                    .push()
                    .setValue(new ChatMessage(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                            userName,
                            etMessage.getText().toString()
                            ,"")
                    );

            etMessage.setText("");

        }
    }

    @Override
    public void onItemClicked(int id) {
        switch (id){
            case R.id.bsb_add_doc:
                chooseDoc();
                mChatAttachment.dismiss();
                break;

            case R.id.bsb_add_images:
                chooseImage();
                mChatAttachment.dismiss();
                break;
            default:

        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select)), PICK_IMAGE_REQUEST);
    }

    private void chooseDoc(){

        String[] mimeTypes = {"application/msword", "application/pdf"};

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setType(mimeTypes.length == 1 ? mimeTypes[0] : "*/*");
            if (mimeTypes.length > 0) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }
        } else {
            StringBuilder mimeTypesStr = new StringBuilder();
            for (String mimeType : mimeTypes) {
                mimeTypesStr.append(mimeType).append("|");
            }
            intent.setType(mimeTypesStr.substring(0,mimeTypesStr.length() - 1));
        }
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select)), PICK_DOC_REQUEST);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String mimeType;
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null ) {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                bitmap = getResizedBitmap(bitmap,500,500);
                if (bitmap!=null){
                    ContentResolver cr = this.getContentResolver();
                    mimeType = cr.getType(filePath);
                    //System.out.println("TYPE---> " + mimeType);
                    //imageView.setImageBitmap(bitmap);
                    uploadImage(mimeType);
                }
                else{
                    Toast.makeText(this, getString(R.string.failed_getting_image), Toast.LENGTH_SHORT).show();
                }

            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }else  if(requestCode == PICK_DOC_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null ) {
            filePath = data.getData();
            try {
                    ContentResolver cr = this.getContentResolver();
                    mimeType = cr.getType(filePath);
                    uploadImage(mimeType);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

    }




    private void uploadImage(final String mimeType) {

        if(filePath != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(getString(R.string.uploading_with_dot));
            progressDialog.setCancelable(false);
            progressDialog.show();

            final StorageReference ref;
            if(mimeType.contains("image")) {
                ref = storageReference.child("images/"+ UUID.randomUUID().toString());
            } else {
                ref = storageReference.child("docs/"+ UUID.randomUUID().toString());
            }

            UploadTask uploadTask = ref.putFile(filePath);

            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Continue with the task to get the download URL
                    return ref.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        progressDialog.dismiss();

                        String downloadURL;

                        if(mimeType.contains("pdf")){
                            downloadURL = downloadUri.toString() + "~~~pdf";
                        }else if(mimeType.contains("msword")){
                            downloadURL = downloadUri.toString() + "~~~msword";
                        }else {
                            downloadURL = downloadUri.toString();
                        }

                        FirebaseDatabase.getInstance()
                                .getReference("collaborations/"+project.getId()+"/"+collaborationID+"/chats")
                                .push()
                                .setValue(new ChatMessage(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                                        userName,
                                        etMessage.getText().toString()
                                        ,downloadURL)
                                );


                        Toast.makeText(GroupChatActivity.this, getString(R.string.uploaded), Toast.LENGTH_SHORT).show();
                    } else {
                        // Handle failures
                        Log.e("Error", "Error");
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(GroupChatActivity.this, getString(R.string.failed)+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        }



    }


}