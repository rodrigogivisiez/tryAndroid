package com.tullyapp.tully.SLDB;

import android.provider.BaseColumns;

/**
 * Created by apple on 26/11/17.
 */

final class TullyDatabaseContract {

    private TullyDatabaseContract() {} // made non creatable

    static final class PendingUploads implements BaseColumns{
        static final String TABLE_NAME = "pending_uploads";
        static final String COLUMN_USER_ID = "user_id";
        static final String COLUMN_UPLOAD_TYPE = "upload_type";
        static final String COLUMN_FILE_PATH = "file_path";
        static final String COLUMN_DATA = "data";

        static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " ( "+
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_USER_ID + " TEXT NOT NULL,"+
                COLUMN_UPLOAD_TYPE + " TEXT NOT NULL,"+
                COLUMN_FILE_PATH + " TEXT NOT NULL,"+
                COLUMN_DATA + " TEXT NOT NULL)";
    }

    static final class UploadSession implements BaseColumns{
        static final String TABLE_NAME = "upload_session";
        static final String COLUMN_USER_ID = "user_id";
        static final String COLUMN_KEY = "key";
        static final String COLUMN_UPLOAD_TYPE = "upload_type";
        static final String COLUMN_FILE_PATH = "file_path";
        static final String COLUMN_DATA = "data";

        static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " ( "+
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_USER_ID + " TEXT NOT NULL,"+
                COLUMN_KEY + " TEXT UNIQUE NOT NULL,"+
                COLUMN_UPLOAD_TYPE + " TEXT NOT NULL,"+
                COLUMN_FILE_PATH + " TEXT NOT NULL,"+
                COLUMN_DATA + " TEXT NOT NULL)";
    }

    static final class PendingStorageDelete implements BaseColumns{
        static final String TABLE_NAME = "pending_storage_delete";
        static final String COLUMN_USER_ID = "user_id";
        static final String COLUMN_STORAGE_PATH = "storage_path";

        static final String SQL_CREATE_TABLE =
                "CREATE TABLE "+ TABLE_NAME + " ( "+
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_USER_ID + " TEXT NOT NULL,"+
                COLUMN_STORAGE_PATH + " TEXT NOT NULL)";
    }
}
