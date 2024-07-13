package com.example.android_minigame.Utilities;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {

    private static volatile SharedPreferencesManager instance = null;

    private static final String PREF_NAME = "ScorePreferences";
    private SharedPreferences sharedPreferences;

    private SharedPreferencesManager(Context context) {
        this.sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static SharedPreferencesManager init(Context context) {
        if (instance == null) {
            synchronized (SharedPreferencesManager.class) {
                if (instance == null) {
                    instance = new SharedPreferencesManager(context);
                }
            }
        }
        return getInstance();
    }

    public static SharedPreferencesManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SharedPreferencesManager must be initialized first");
        }
        return instance;
    }

    public void putString(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }
}