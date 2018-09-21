package com.tullyapp.tully.SLDB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.firebase.auth.FirebaseAuth;
import com.tullyapp.tully.Models.RemainingUpload;
import com.tullyapp.tully.Models.StorageDelete;
import com.tullyapp.tully.SLDB.TullyDatabaseContract.PendingUploads;

import java.util.ArrayList;

import static com.tullyapp.tully.SLDB.TullyDatabaseContract.PendingStorageDelete;
import static com.tullyapp.tully.SLDB.TullyDatabaseContract.UploadSession;

/**
 * Created by apple on 26/11/17.
 */

public class DataManager {
    private static DataManager ourInstance = null;

    private static final String[] columns_PendingUploads = {PendingUploads._ID, PendingUploads.COLUMN_USER_ID, PendingUploads.COLUMN_UPLOAD_TYPE,  PendingUploads.COLUMN_FILE_PATH, PendingUploads.COLUMN_DATA};
    private static final String[] columns_UploadSession = {UploadSession._ID, UploadSession.COLUMN_USER_ID, UploadSession.COLUMN_UPLOAD_TYPE,  UploadSession.COLUMN_FILE_PATH, UploadSession.COLUMN_DATA};
    private static final String[] columns_PendingStorageDelete = {PendingStorageDelete._ID, PendingStorageDelete.COLUMN_USER_ID, PendingStorageDelete.COLUMN_STORAGE_PATH};
    private static FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private ArrayList<RemainingUpload> pendingUploadsList = new ArrayList<>();
    private ArrayList<RemainingUpload> sessionUploadsList = new ArrayList<>();
    private ArrayList<StorageDelete> storageDeleteArrayList = new ArrayList<>();

    public synchronized static DataManager getInstance(){
        if (ourInstance == null){
            ourInstance = new DataManager();
        }
        return ourInstance;
    }

    /*
    ------------------------------
    Pending Storage Deletes
    ------------------------------
    */

    public static ArrayList<StorageDelete> loadStorageDeletes(Context context){
        TullyDbHelper dbHelper = new TullyDbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        if (mAuth.getCurrentUser()!=null){
            final String USER_ID = mAuth.getCurrentUser().getUid();
            String selection = PendingStorageDelete.COLUMN_USER_ID + " LIKE ? ";
            String[] selectionArgs = {USER_ID};

            Cursor cursor = db.query(PendingStorageDelete.TABLE_NAME, columns_PendingStorageDelete, selection, selectionArgs, null, null, null);

            int _idPos = cursor.getColumnIndex(PendingStorageDelete._ID);
            int storagePathPos = cursor.getColumnIndex(PendingStorageDelete.COLUMN_STORAGE_PATH);

            DataManager dm = getInstance();
            dm.storageDeleteArrayList.clear();

            while (cursor.moveToNext()){
                StorageDelete storageDelete = new StorageDelete(
                        cursor.getInt(_idPos),
                        cursor.getString(storagePathPos)
                );

                dm.storageDeleteArrayList.add(storageDelete);
            }

            cursor.close();

            db.close();
            dbHelper.close();

            return dm.storageDeleteArrayList;
        }

        db.close();
        dbHelper.close();

        return new ArrayList<>();
    }

    public static void addPendingStorageDelete(Context context, StorageDelete storageDelete){
        TullyDbHelper dbHelper = new TullyDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (mAuth.getCurrentUser()!=null){
            final String USER_ID = mAuth.getCurrentUser().getUid();

            values.put(PendingStorageDelete.COLUMN_USER_ID, USER_ID);
            values.put(PendingStorageDelete.COLUMN_STORAGE_PATH, storageDelete.getStorage_path());

            db.insert(PendingStorageDelete.TABLE_NAME, null, values);
        }

        db.close();
        dbHelper.close();
    }

    public static void deletePendingStorageDelete(Context context, int id){
        TullyDbHelper dbHelper = new TullyDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (mAuth.getCurrentUser()!=null) {
            final String USER_ID = mAuth.getCurrentUser().getUid();

            String selection = PendingUploads._ID + " = ? AND " + PendingUploads.COLUMN_USER_ID + " LIKE ? ";
            String[] selectionArgs = {Integer.toString(id), USER_ID};

            db.delete(PendingStorageDelete.TABLE_NAME, selection, selectionArgs);

        }

        db.close();
        dbHelper.close();
    }


    /*
    ------------------------------
    Pending / Offlined Uploads
    ------------------------------
    */

    public static ArrayList<RemainingUpload> loadPendingUploadsList(Context context){
        TullyDbHelper dbHelper = new TullyDbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        if (mAuth.getCurrentUser()!=null) {
            final String USER_ID = mAuth.getCurrentUser().getUid();
            String selection = PendingUploads.COLUMN_USER_ID + " LIKE ? ";
            String[] selectionArgs = {USER_ID};

            Cursor cursor = db.query(PendingUploads.TABLE_NAME, columns_PendingUploads, selection, selectionArgs, null, null, null);

            int _idPos = cursor.getColumnIndex(PendingUploads._ID);
            int upload_typePos = cursor.getColumnIndex(PendingUploads.COLUMN_UPLOAD_TYPE);
            int pathPos = cursor.getColumnIndex(PendingUploads.COLUMN_FILE_PATH);
            int dataPos = cursor.getColumnIndex(PendingUploads.COLUMN_DATA);

            DataManager dm = getInstance();
            dm.pendingUploadsList.clear();

            while (cursor.moveToNext()) {
                RemainingUpload remainingUpload = new RemainingUpload(
                        cursor.getInt(_idPos),
                        cursor.getString(upload_typePos),
                        cursor.getString(pathPos),
                        cursor.getString(dataPos)
                );
                dm.pendingUploadsList.add(remainingUpload);
            }

            cursor.close();

            db.close();
            dbHelper.close();

            return dm.pendingUploadsList;
        }

        db.close();
        dbHelper.close();

        return new ArrayList<>();
    }

    public static void AddPendingUpload(Context context, RemainingUpload remainingUpload){
        TullyDbHelper dbHelper = new TullyDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (mAuth.getCurrentUser()!=null) {
            final String USER_ID = mAuth.getCurrentUser().getUid();

            ContentValues values = new ContentValues();

            values.put(PendingUploads.COLUMN_USER_ID, USER_ID);
            values.put(PendingUploads.COLUMN_UPLOAD_TYPE, remainingUpload.getUpload_type());
            values.put(PendingUploads.COLUMN_FILE_PATH, remainingUpload.getFile_path());
            values.put(PendingUploads.COLUMN_DATA, remainingUpload.getData());

            db.insert(PendingUploads.TABLE_NAME, null, values);

            db.close();
            dbHelper.close();
        }
    }

    public static void deletePendingUpload(Context context, int id){
        TullyDbHelper dbHelper = new TullyDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String selection = PendingUploads._ID + " = ?";
        String[] selectionArgs = {Integer.toString(id)};

        db.delete(PendingUploads.TABLE_NAME, selection, selectionArgs);

        db.close();
        dbHelper.close();
    }


    /*
    ------------------------------
    Session Uploads
    ------------------------------
    */

    public static ArrayList<RemainingUpload> loadSessionUploads(Context context){

        if (mAuth.getCurrentUser()!=null) {

            TullyDbHelper dbHelper = new TullyDbHelper(context);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            final String USER_ID = mAuth.getCurrentUser().getUid();
            String selection = UploadSession.COLUMN_USER_ID + " LIKE ? ";
            String[] selectionArgs = {USER_ID};

            Cursor cursor = db.query(UploadSession.TABLE_NAME, columns_UploadSession, selection, selectionArgs, null, null, null);

            int _idPos = cursor.getColumnIndex(UploadSession._ID);
            int upload_typePos = cursor.getColumnIndex(UploadSession.COLUMN_UPLOAD_TYPE);
            int pathPos = cursor.getColumnIndex(UploadSession.COLUMN_FILE_PATH);
            int dataPos = cursor.getColumnIndex(UploadSession.COLUMN_DATA);

            DataManager dm = getInstance();
            dm.sessionUploadsList.clear();

            while (cursor.moveToNext()){
                RemainingUpload remainingUpload = new RemainingUpload(
                        cursor.getInt(_idPos),
                        cursor.getString(upload_typePos),
                        cursor.getString(pathPos),
                        cursor.getString(dataPos)
                );
                dm.sessionUploadsList.add(remainingUpload);
            }

            cursor.close();

            db.close();
            dbHelper.close();

            return dm.sessionUploadsList;
        }

        return new ArrayList<>();
    }

    public static void AddSessionUpload(Context context, RemainingUpload remainingUpload){
        TullyDbHelper dbHelper = new TullyDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        if (mAuth.getCurrentUser()!=null) {
            final String USER_ID = mAuth.getCurrentUser().getUid();
            values.put(UploadSession.COLUMN_KEY,remainingUpload.getKey());
            values.put(UploadSession.COLUMN_USER_ID, USER_ID);
            values.put(UploadSession.COLUMN_UPLOAD_TYPE,remainingUpload.getUpload_type());
            values.put(UploadSession.COLUMN_FILE_PATH,remainingUpload.getFile_path());
            values.put(UploadSession.COLUMN_DATA,remainingUpload.getData());
            db.insert(UploadSession.TABLE_NAME, null, values );
        }

        db.close();
        dbHelper.close();
    }

    public static void deleteSessionUpload(Context context, String key){
        TullyDbHelper dbHelper = new TullyDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (mAuth.getCurrentUser()!=null) {
            final String USER_ID = mAuth.getCurrentUser().getUid();

            String selection = UploadSession.COLUMN_KEY + " LIKE ? AND " + UploadSession.COLUMN_USER_ID + " LIKE ?";
            String[] selectionArgs = {key, USER_ID};

            db.delete(UploadSession.TABLE_NAME, selection, selectionArgs);
        }

        db.close();
        dbHelper.close();
    }

    public static void deleteSessionUpload(Context context, int id){
        TullyDbHelper dbHelper = new TullyDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (mAuth.getCurrentUser()!=null) {
            final String USER_ID = mAuth.getCurrentUser().getUid();

            String selection = UploadSession._ID + " = ? AND " + UploadSession.COLUMN_USER_ID + " LIKE ? ";
            String[] selectionArgs = {Integer.toString(id), USER_ID};

            db.delete(UploadSession.TABLE_NAME, selection, selectionArgs);
        }

        db.close();
        dbHelper.close();
    }
}
