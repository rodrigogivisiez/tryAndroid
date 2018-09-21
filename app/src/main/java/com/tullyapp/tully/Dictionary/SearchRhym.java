package com.tullyapp.tully.Dictionary;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;

/**
 * Created by apple on 23/12/17.
 */

public class SearchRhym {

    private Context context;

    private OfflineRhym offlineRhym;

    public interface OfflineRhym{
        void onRhymReceive(ArrayList<String> rhymStrings);
    }

    public void setOfflineRhymListener(OfflineRhym offlineRhymListener){
        this.offlineRhym = offlineRhymListener;
    }

    public SearchRhym(Context context) {
        this.context = context;
    }

    public void rhym(String key){
        if (context !=null){
            new InitDB().execute(key);
        }
    }

    class InitDB extends AsyncTask<String, Void, ArrayList<String>> {
        @Override
        protected ArrayList<String> doInBackground(String... strings) {
            ArrayList<String> rhymStrings = DicDataManager.findRhym(context, strings[0]);

            return rhymStrings;
        }

        @Override
        protected void onPostExecute(ArrayList<String> rhymStrings) {
            super.onPostExecute(rhymStrings);
            if (offlineRhym!=null){
                offlineRhym.onRhymReceive(rhymStrings);
            }
        }
    }
}
