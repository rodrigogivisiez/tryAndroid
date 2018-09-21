package com.tullyapp.tully.Utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.tullyapp.tully.Interface.AuthToken;
import com.tullyapp.tully.LoginActivity;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.tullyapp.tully.HomeActivity.ACTION_KILL_HOME;

/**
 * Created by macbookpro on 02/09/17.
 */

public class Utils {

    public static boolean isValidEmail(CharSequence target) {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public static void showAlert(Context context, String title, String message) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public static void showMessage(Context context, String title, String message) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public static void logOutSequence(Context context, FirebaseAuth mAuth){
        PreferenceUtil.getPref(context).edit().remove(PreferenceKeys.IS_ARTIST_PROFILE_READY).apply();
        PreferenceUtil.getPref(context).edit().remove(PreferenceKeys.ID_TOKEN).apply();
        PreferenceUtil.getPref(context).edit().remove(PreferenceKeys.ID_TOKEN_EXPIRATION).apply();
        PreferenceUtil.getPref(context).edit().remove(PreferenceKeys.ENGINEER_INTRO_FINISHED).apply();
        mAuth.signOut();
        LoginManager.getInstance().logOut();
        Intent localIntent = new Intent(ACTION_KILL_HOME);
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        ((Activity)context).finish();
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    public static boolean isGooglePlayServicesAvailable(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(context);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    //Check internet connection
    public static boolean isInternetAvailable(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            // test for connection
            if (cm.getActiveNetworkInfo() != null
                    && cm.getActiveNetworkInfo().isAvailable()
                    && cm.getActiveNetworkInfo().isConnected()) {
                return true;
            } else {
                Log.e("", "Internet Connection Not Available");
                return false;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getContentName(ContentResolver resolver, Uri uri) {
        try {
            Cursor cursor = resolver.query(uri, null, null, null, null);
            cursor.moveToFirst();
            int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            if (nameIndex >= 0) {
                return cursor.getString(nameIndex);
            } else {
                return null;
            }
        }catch (Exception e){
            return null;
        }
    }

    public static boolean showNoInternetPopup(final Context context) {
        boolean available = isInternetAvailable(context);
        if (!available) {
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(context);
            builder.setTitle("No Internet connection")
                    .setMessage("Please enable internet and start app again")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((Activity) context).finish();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        return available;
    }

    public static void hideSoftKeyboard(Context context) {
        ((Activity) context).getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
    }

    public static void hideKeyboard(Activity activity) {
        try{
            InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            // check if no view has focus:
            View currentFocusedView = activity.getCurrentFocus();
            if (currentFocusedView != null) {
                inputManager.hideSoftInputFromWindow(currentFocusedView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static String getFileName() {
        Calendar c = Calendar.getInstance();
        String fileName;
        fileName = "and-" + c.getTimeInMillis();
        return fileName;
    }

    public static File getDirectory(Context context, String dir) {
        String dirPath = context.getFilesDir().getAbsolutePath() + File.separator + dir;
        File projDir = new File(dirPath);
        if (!projDir.exists())
            projDir.mkdirs();

        return projDir;
    }

    public static void shareAudio(Context context, Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("audio/*");
        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        context.startActivity(Intent.createChooser(shareIntent, "Share"));
    }

    public static void getAuthToken(final Context context, FirebaseAuth mAuth, final AuthToken authToken, final String callback){
        boolean valid = true;
        String idToken = PreferenceUtil.getPref(context).getString(PreferenceKeys.ID_TOKEN,"");
        long timestamp = PreferenceUtil.getPref(context).getLong(PreferenceKeys.ID_TOKEN_EXPIRATION,-1);
        if (idToken.isEmpty()){
            valid = false;
        }
        else{
            long diff = timestamp - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
            if (diff<100){
                valid = false;
            }
        }
        if (valid){
            authToken.onToken(idToken,callback);
        }
        else{
            if (mAuth.getCurrentUser()!=null){
                mAuth.getCurrentUser().getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()){
                            PreferenceUtil.getPref(context).edit().putString(PreferenceKeys.ID_TOKEN,task.getResult().getToken()).apply();
                            PreferenceUtil.getPref(context).edit().putLong(PreferenceKeys.ID_TOKEN_EXPIRATION,task.getResult().getExpirationTimestamp()).apply();
                            authToken.onToken(task.getResult().getToken(),callback);
                        }
                    }
                });
            }
        }
    }

    @SuppressLint("DefaultLocale")
    public static String formatAudioTime(long millis) {

        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }

    public static String getMime(String path) {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path));
    }

    public static Date yesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }


    public static Date tomorrow() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, +1);
        return cal.getTime();
    }

    public static void openWebPage(String url,Context context) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
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

}
