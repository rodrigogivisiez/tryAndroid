package com.tullyapp.tully.SLDB;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by apple on 26/11/17.
 */

public class TullyDbHelper extends SQLiteOpenHelper {

    private static final String DATABAE_NAME = "Tully.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TAG = TullyDbHelper.class.getSimpleName();

    TullyDbHelper(Context context) {
        super(context, DATABAE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.e(TAG,"onCreate");
        db.execSQL(TullyDatabaseContract.PendingUploads.SQL_CREATE_TABLE);
        db.execSQL(TullyDatabaseContract.UploadSession.SQL_CREATE_TABLE);
        db.execSQL(TullyDatabaseContract.PendingStorageDelete.SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
