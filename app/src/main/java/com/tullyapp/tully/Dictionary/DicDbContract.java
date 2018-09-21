package com.tullyapp.tully.Dictionary;

/**
 * Created by apple on 22/12/17.
 */

public class DicDbContract {

    public DicDbContract() {
    }

    static final class WordsMaster {
        static final String TABLE_NAME = "words_master";
        static final String COLUMN_ID = "id";
        static final String COLUMN_WORD = "word";

        static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " ( "+
                        COLUMN_ID + " INTEGER PRIMARY KEY," +
                        COLUMN_WORD + " TEXT NOT NULL)";
    }

    static final class RhymMaster {
        static final String TABLE_NAME = "rhym_master";
        static final String COLUMN_ID = "id";
        static final String COLUMN_RHYM = "rhym";

        static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " ( "+
                        COLUMN_ID + " INTEGER PRIMARY KEY," +
                        COLUMN_RHYM + " TEXT NOT NULL)";
    }

    static final class WordRhym {
        static final String TABLE_NAME = "word_rhym";
        static final String COLUMN_WORD_ID = "word_id";
        static final String COLUMN_RHYM_ID = "rhym_id";

        static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " ( "+
                        COLUMN_WORD_ID + " INTEGER NOT NULL," +
                        COLUMN_RHYM_ID + " INTEGER NOT NULL)";
    }

}
