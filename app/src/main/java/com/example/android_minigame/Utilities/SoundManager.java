package com.example.android_minigame.Utilities;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import com.example.android_minigame.R;

public class SoundManager {
    private static final String TAG = "SoundManager";
    private static SoundManager instance;
    private SoundPool soundPool;
    private int crashSoundId;
    private boolean isLoaded;

    private SoundManager(Context context) {
        try {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(audioAttributes)
                    .build();

            soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
                isLoaded = status == 0;
                Log.d(TAG, "Sound loaded: " + isLoaded);
            });

            crashSoundId = soundPool.load(context, R.raw.crash_sound, 1);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SoundManager", e);
        }
    }

    public static SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
        return instance;
    }

    public void playCrashSound() {
        if (isLoaded) {
            try {
                soundPool.play(crashSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
            } catch (Exception e) {
                Log.e(TAG, "Error playing crash sound", e);
            }
        } else {
            Log.w(TAG, "Crash sound not loaded yet");
        }
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        instance = null;
    }
}