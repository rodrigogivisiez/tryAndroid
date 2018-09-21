package com.tullyapp.tully.Dictionary;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by apple on 22/12/17.
 */

public class DicDataManager {

    private static DicDataManager ourInstance;
    private static final String TAG = DicDataManager.class.getSimpleName();

    private DicDataManager() {}

    public static DicDataManager getInstance(){
        if (ourInstance==null){
            ourInstance = new DicDataManager();
        }
        return ourInstance;
    }

    public void createDatabase(Context context){
        try {
            DictionaryDbHelper dictionaryDbHelper = new DictionaryDbHelper(context);
            dictionaryDbHelper.createDataBase();
        }
        catch (IOException mIOException) {
            Log.e(TAG, mIOException.toString() + "  UnableToCreateDatabase");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /*public void open(){
        try {
            dictionaryDbHelper.openDataBase();
            dictionaryDbHelper.close();
        } catch (SQLException mSQLException) {
            Log.e(TAG, "open >>"+ mSQLException.toString());
        }
    }*/

    /*public void close() {
        dictionaryDbHelper.close();
    }*/


    static ArrayList<String> findRhym(Context context, String word){
        ArrayList<String> rhyms = new ArrayList<>();
        try{
            DictionaryDbHelper dictionaryDbHelper = new DictionaryDbHelper(context);
            SQLiteDatabase mDb = dictionaryDbHelper.getReadableDatabase();
            Cursor cursor = mDb.rawQuery("SELECT rm.id as id, rm.rhym as rhym FROM rhym_master rm JOIN word_rhym wr ON wr.rhym_id = rm.id JOIN words_master wm ON wm.id = wr.word_id WHERE wm.word like '"+word+"'",null);

            //int id_pos = cursor.getColumnIndex(DicDbContract.WordsMaster.COLUMN_ID);
            int word_pos = cursor.getColumnIndex(DicDbContract.RhymMaster.COLUMN_RHYM);


            while (cursor.moveToNext()) {
                //Log.e("DATA",cursor.getInt(id_pos)+" : "+cursor.getString(word_pos));
                rhyms.add(cursor.getString(word_pos));
            }

            cursor.close();
            dictionaryDbHelper.close();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            return rhyms;
        }
    }

}
