package com.tullyapp.tully.Fragments;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.MySSLSocketFactory;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;
import com.tullyapp.tully.Adapters.BeatsAdapter;
import com.tullyapp.tully.Interface.AuthToken;
import com.tullyapp.tully.Models.Beats;
import com.tullyapp.tully.R;
import com.tullyapp.tully.Services.FirebaseDatabaseOperations;
import com.tullyapp.tully.Utils.APIs;
import com.tullyapp.tully.Utils.CircleTransform;
import com.tullyapp.tully.Utils.Constants;
import com.tullyapp.tully.Utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;

import cz.msebera.android.httpclient.Header;

import static com.tullyapp.tully.Utils.Utils.isInternetAvailable;

/**
 * A simple {@link Fragment} subclass.
 */
public class MarketSongListFragment extends BaseFragment implements BeatsAdapter.MarketAudioListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener, View.OnClickListener, MediaPlayer.OnErrorListener{

    private RecyclerView recycle_view;
    private BeatsAdapter beatsAdapter;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swiperefresh;
    private String TAG = MarketSongListFragment.class.getSimpleName();
    private Handler handler = new Handler();
    private TextView tv_title, tv_subtitle, tv_projectname;
    private MediaPlayer mPlayer;
    private Beats beatObject;
    private int currentPos;
    private boolean isReleased = false;
    private boolean isPaused = false;
    private ImageView btn_play, btn_forward, profile_picture;
    private FrameLayout topbar_container;

    private AppCompatSeekBar musicProgressbar;
    private MarketSongListListener mOnPlayerSelectionSetListener;
    private FirebaseAuth mAuth;
    private LayoutInflater inflater;
    private View musicTopBar, profileBar;
    private TextView tv_artist_name, tv_artist_option, tv_genre, music_type;
    private Button btn_price;
    private DatabaseReference mDatabase;
    private boolean userScrolled = false;
    private LinearLayoutManager linearLayoutManager;
    private int visibleItemCount;
    private int totalItemCount;
    private int pastVisiblesItems;
    private int page = 0;
    private int totalPage = 0;
    private int totalCount = 0;
    private AlertDialog alertDialog;
    private AuthToken authToken;
    private Beats selectedBeat;
    private static final int REQUEST_CAMERA = 1888;
    private final int SELECT_FILE = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100;

    public MarketSongListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onAttachToParentFragment(getParentFragment());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mAuth = FirebaseAuth.getInstance();

        authToken = new AuthToken() {
            @Override
            public void onToken(String token, String callback) {
                switch (callback){
                    case "loadData":
                        loadData(token);
                        break;

                    case "makeFreePurchase":
                        makeFreePurchase(token);
                        break;
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_market_song_list, container, false);
        recycle_view = view.findViewById(R.id.recycle_view);
        swiperefresh = view.findViewById(R.id.swiperefresh);
        progressBar = view.findViewById(R.id.progressBar);
        musicProgressbar = view.findViewById(R.id.musicProgressbar);
        tv_title = view.findViewById(R.id.tv_title);
        tv_subtitle = view.findViewById(R.id.tv_subtitle);
        btn_forward = view.findViewById(R.id.btn_forward);
        btn_play = view.findViewById(R.id.btn_play);
        topbar_container = view.findViewById(R.id.topbar_container);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        inflater = LayoutInflater.from(getContext());
        beatsAdapter = new BeatsAdapter(getContext());
        linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycle_view.setLayoutManager(linearLayoutManager);
        recycle_view.setAdapter(beatsAdapter);

        swiperefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
            Utils.getAuthToken(getContext(),mAuth,authToken,"loadData");
            }
        });

        alertDialog = new AlertDialog.Builder(getContext())
        .setTitle("Beat Purchased")
        .setMessage("You will find it in your home screen and Beats Screen")
        .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
                page = 0;
                Utils.getAuthToken(getContext(),mAuth,authToken,"loadData");
            }
        }).create();

        recycle_view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    userScrolled = true;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    visibleItemCount = linearLayoutManager.getChildCount();
                    totalItemCount = linearLayoutManager.getItemCount();
                    pastVisiblesItems = linearLayoutManager.findFirstVisibleItemPosition();
                    if (userScrolled && (visibleItemCount + pastVisiblesItems) == totalItemCount) {
                        if (totalPage > page+1) {
                            if (isInternetAvailable(getContext())) {
                                page++;
                                Utils.getAuthToken(getContext(),mAuth,authToken,"loadData");
                            } else {
                                Toast.makeText(getContext(), "Internet Not Available", Toast.LENGTH_SHORT).show();
                            }
                        }
                        userScrolled = false;
                    }
                }
            }
        });

        beatsAdapter.setMarketAudioListener(this);
        musicProgressbar.setOnSeekBarChangeListener(this);
        btn_play.setOnClickListener(this);
        btn_forward.setOnClickListener(this);
        loadTopBar();
        Utils.getAuthToken(getContext(),mAuth,authToken,"loadData");
    }

    private void loadTopBar(){
        profileBar = inflater.inflate(R.layout.market_profile_row, null, false);
        tv_projectname = profileBar.findViewById(R.id.tv_projectname);
        profile_picture = profileBar.findViewById(R.id.profile_picture);
        profile_picture.setOnClickListener(this);
        tv_artist_option = profileBar.findViewById(R.id.tv_artist_option);
        tv_projectname.setText(mAuth.getCurrentUser().getDisplayName());
        try{
            Picasso.with(getContext()).load(mAuth.getCurrentUser().getPhotoUrl()).placeholder(R.drawable.default_profile_picture).transform(new CircleTransform()).into(profile_picture);
        }catch (Exception e){
            e.printStackTrace();
        }
        topbar_container.addView(profileBar);
        get_artist_option();
    }

    private void loadMusicTopBar(){
        if (musicTopBar==null) {
            musicTopBar = inflater.inflate(R.layout.music_top_container, null, false);
            topbar_container.removeView(profileBar);
            topbar_container.addView(musicTopBar);

            tv_artist_name = musicTopBar.findViewById(R.id.tv_artist_name);
            btn_price = musicTopBar.findViewById(R.id.price_btn);
            tv_genre = musicTopBar.findViewById(R.id.tv_genre);
            music_type = musicTopBar.findViewById(R.id.music_type);
        }

        tv_artist_name.setText(beatObject.getProducer_name());
        if (beatObject.isFree()){
            btn_price.setText(R.string.free);
        }
        else{
            btn_price.setText(beatObject.getPrice()+" $");
        }

        tv_genre.setText(beatObject.getGenre());
        music_type.setText(beatObject.getType());

        btn_price.setOnClickListener(this);
    }

    private void runLayoutAnimation() {
        recycle_view.getAdapter().notifyDataSetChanged();
    }

    private void loadData(String idToken){
        if (page==0) beatsAdapter.clearList();
        swiperefresh.setRefreshing(false);
        progressBar.setVisibility(View.VISIBLE);
        AsyncHttpClient client = new AsyncHttpClient();
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            client.setSSLSocketFactory(sf);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        RequestParams params = new RequestParams();
        client.addHeader(Constants.Authorization,idToken);
        params.put("page",page);
        client.get(APIs.GET_BEATS_LIST, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                String responseString = new String(bytes);
                try {
                    JSONObject response = new JSONObject(responseString);
                    JSONArray dataArray = response.getJSONArray("data");
                    totalCount = response.getInt("count");

                    if (totalCount <= 10) {
                        totalPage = 1;
                    } else {
                        totalPage = totalCount / 10;
                        int remincer = totalCount % 10;
                        if (remincer != 0) {
                            totalPage = totalPage + 1;
                        }
                    }

                    for (int k=0; k<dataArray.length(); k++){
                        JSONObject track = new JSONObject(dataArray.getJSONObject(k).getString("track"));
                        Beats beats = new Beats(
                            dataArray.getJSONObject(k).optInt("id"),
                            dataArray.getJSONObject(k).optString("name"),
                            dataArray.getJSONObject(k).optDouble("price",0D),
                            track.optString("url"),
                            track.optString("name"),
                            dataArray.getJSONObject(k).optString("producer_name"),
                            dataArray.getJSONObject(k).optString("email"),
                            track.optLong("size"),
                            dataArray.getJSONObject(k).optString("type"),
                            dataArray.getJSONObject(k).optString("genre"),
                            dataArray.getJSONObject(k).optString("price").equals("0")
                        );
                        beatsAdapter.add(beats);
                    }

                    progressBar.setVisibility(View.GONE);
                    runLayoutAnimation();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                Log.e(TAG,throwable.getMessage());
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onSearchKey(String searchKey) {}

    @Override
    public void onSearchCancelled() {}

    @Override
    public void fabButtonClicked() {}

    @Override
    public void actionEvent(int event) {

    }

    private void play(){
        progressBar.setVisibility(View.VISIBLE);
        clearPlayer();
        String path;
        if (beatObject!=null && beatObject.getTrackURL()!=null && !beatObject.getTrackURL().isEmpty()){

            loadMusicTopBar();

            tv_title.setText(beatObject.getName());
            tv_subtitle.setText(beatObject.getProducer_name());
            beatsAdapter.markAudioPlaying(beatObject, currentPos);
            path = beatObject.getTrackURL();
            if (!path.isEmpty()){
                try {
                    isPaused = false;
                    isReleased = false;
                    btn_play.setImageResource(R.drawable.stop_icon);
                    mPlayer = new MediaPlayer();
                    mPlayer.setLooping(false);
                    mPlayer.setOnPreparedListener(this);
                    mPlayer.setOnCompletionListener(this);
                    mPlayer.setOnErrorListener(this);
                    mPlayer.setDataSource(path);
                    mPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                    progressBar.setVisibility(View.GONE);
                }
            }
            else{
                Toast.makeText(getContext(), "Seems Network / Audio is broken", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        }else{
            Toast.makeText(getContext(), "Seems Audio is broken", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    private void togglePlay(){
        if (isReleased || mPlayer==null){
            play();
        }else{
            try{
                if (mPlayer.isPlaying()){
                    Log.e("ENTER","PAUSED");
                    mPlayer.pause();
                    isPaused = true;
                    handler.removeCallbacks(MediaObserver);
                    btn_play.setImageResource(R.drawable.market_play_icon);
                    beatsAdapter.markAudioPlaying(currentPos,false);
                }else if (isPaused){
                    Log.e("RESUMING","Yoo");
                    mPlayer.start();
                    isPaused = false;
                    musicProgressbar.setMax(mPlayer.getDuration());
                    btn_play.setImageResource(R.drawable.stop_icon);
                    handler.post(MediaObserver);
                    beatsAdapter.markAudioPlaying(currentPos,true);
                }
                else{
                    Log.e("ELSE","PLAY");
                    play();
                }
            }catch (Exception e){
                play();
            }
        }
    }


    private void clearPlayer(){
        handler.removeCallbacks(MediaObserver);
        if (mPlayer!=null){
            try{
                mPlayer.stop();
            }catch (Exception e){
                e.printStackTrace();
            }
            try{
                mPlayer.reset();
            }
            catch (Exception e){
                e.printStackTrace();
            }
            try {
                mPlayer.release();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        musicProgressbar.setProgress(0);
        btn_play.setImageResource(R.drawable.market_play_icon);
    }

    Runnable MediaObserver = new Runnable() {
        static final long PROGRESS_UPDATE = 500;
        private int currenttime;

        @Override
        public void run() {
            try {
                currenttime = mPlayer.getCurrentPosition();
                musicProgressbar.setProgress(currenttime);
                handler.postDelayed(this,PROGRESS_UPDATE);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onDestroy() {
        handler.removeCallbacks(MediaObserver);
        clearPlayer();
        super.onDestroy();
    }

    @Override
    public void onPlay(Beats beat, int position) {
        beatObject = beat;
        currentPos = position;
        play();
    }

    @Override
    public void onPaused(Beats beat, int position) {
        beatObject = beat;
        currentPos = position;
        handler.removeCallbacks(MediaObserver);
        try{
            if (mPlayer!=null && mPlayer.isPlaying()){
                mPlayer.pause();
            }
        }catch (Exception e){
            try{
                mPlayer.pause();
            }catch (Exception ex){
                e.printStackTrace();
            }
        }
        isPaused = true;
        btn_play.setImageResource(R.drawable.market_play_icon);
    }

    @Override
    public void onResumed(Beats beat, int position) {
        beatObject = beat;
        currentPos = position;
        try{
            if (mPlayer!=null){
                mPlayer.start();
                //if (!recordingObject.isOfProject() && currentPos!=-1){
                handler.post(MediaObserver);
                //}
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        btn_play.setImageResource(R.drawable.stop_icon);
    }

    @Override
    public void onPurchase(Beats beats, int position) {
        clearPlayer();
        mOnPlayerSelectionSetListener.onBeatsPurchaseClicked(beats);
    }

    @Override
    public void onFreePurchase(final Beats beats, int position) {
        selectedBeat = beats;
        progressBar.setVisibility(View.VISIBLE);
        Utils.getAuthToken(getContext(),mAuth,authToken,"makeFreePurchase");
    }

    private void makeFreePurchase(String token){
        AsyncHttpClient client = new AsyncHttpClient();
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            client.setSSLSocketFactory(sf);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        RequestParams params = new RequestParams();
        client.addHeader(Constants.Authorization,token);
        params.put("beat_id",selectedBeat.getId());
        client.post(APIs.SELL_FREE_BEAT, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                progressBar.setVisibility(View.GONE);
                String responseString = new String(bytes);
                try {
                    JSONObject response = new JSONObject(responseString);
                    if (response.getInt("status")==1){
                        alertDialog.show();
                    }
                    else{
                        Toast.makeText(getContext(), response.getString("msg"), Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(getContext(), "Something Went Wrong", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG,throwable.getMessage());
                Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        progressBar.setVisibility(View.GONE);
        beatsAdapter.markAudioComplete(beatObject, currentPos);
        clearPlayer();
        return false;
    }

    public interface MarketSongListListener {
        void onBeatsPurchaseClicked(Beats beats);
    }

    public void onAttachToParentFragment(Fragment fragment) {
        try {
            mOnPlayerSelectionSetListener = (MarketSongListListener)fragment;
        }
        catch (ClassCastException e) {
            throw new ClassCastException(fragment.toString() + " must implement OnPlayerSelectionSetListener");
        }
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        mPlayer.start();
        progressBar.setVisibility(View.GONE);
        int duration = mp.getDuration();
        musicProgressbar.setMax(duration);
        handler.post(MediaObserver);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        handler.removeCallbacks(MediaObserver);
        beatsAdapter.markAudioComplete(beatObject, currentPos);
        clearPlayer();
        musicProgressbar.setProgress(0);
        //tv_startTime.setText("00:00");
        isReleased = true;
        btn_play.setImageResource(R.drawable.market_play_icon);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        handler.removeCallbacks(MediaObserver);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mPlayer!=null){
            try{
                if (mPlayer.isPlaying()){
                    int currentPosition = seekBar.getProgress();
                    mPlayer.pause();
                    mPlayer.seekTo(currentPosition);
                    mPlayer.start();
                    handler.post(MediaObserver);
                }
                else{
                    int currentPosition = seekBar.getProgress();
                    mPlayer.seekTo(currentPosition);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_play:
                if (beatObject!=null)
                    togglePlay();
                break;

            case R.id.btn_forward:
                    forward();
                break;

            case R.id.profile_picture:
                    selectImage();
                break;

            case R.id.price_btn:
                clearPlayer();
                if (beatObject.isFree()) {
                    progressBar.setVisibility(View.VISIBLE);
                    Utils.getAuthToken(getContext(),mAuth,authToken,"makeFreePurchase");
                }
                else{
                    mOnPlayerSelectionSetListener.onBeatsPurchaseClicked(beatObject);
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
                    profile_picture.setImageBitmap(bm);
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
            profile_picture.setImageBitmap(thumbnail);
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

    private void forward(){
        if (beatObject!=null){
            Beats nextBeat = beatsAdapter.hasNext(currentPos);
            if (nextBeat!=null){
                onPlay(nextBeat, currentPos+1);
            }
        }
    }

    private void get_artist_option(){
        mDatabase = FirebaseDatabase.getInstance().getReference().child(mAuth.getCurrentUser().getUid()+"/profile");
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String options = (String) dataSnapshot.child("artist_option").getValue();
                    tv_artist_option.setText(options);
                }
                else{
                    tv_artist_option.setText("");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tv_artist_option.setText("");
            }
        });
    }
}
