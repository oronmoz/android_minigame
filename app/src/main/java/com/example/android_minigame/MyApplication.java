package com.example.android_minigame;

import android.app.Application;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.example.android_minigame.Utilities.SharedPreferencesManager;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize WorkManager
        WorkManager.initialize(
                this,
                new Configuration.Builder()
                        .setMinimumLoggingLevel(android.util.Log.INFO)
                        .build()
        );

        // Initialize SharedPreferencesManager
        SharedPreferencesManager.init(this);
    }
}