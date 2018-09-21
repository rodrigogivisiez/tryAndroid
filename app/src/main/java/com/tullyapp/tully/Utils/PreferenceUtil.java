package com.tullyapp.tully.Utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Kathan Shah on 04/09/17.
 */

public class PreferenceUtil {
    private static final String PREFERENCE_FILE = "com.tullyapp.tully.android";
    private static final String PASSWORD = "tully.android";
    private static SharedPreferences preferences;

    public synchronized static SharedPreferences getPref(Context context) {
        if (preferences == null)
            preferences = new ObscuredSharedPreferences(context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE), PASSWORD);
        return preferences;
    }
}
