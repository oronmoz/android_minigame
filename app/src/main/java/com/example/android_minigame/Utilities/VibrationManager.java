package com.example.android_minigame.Utilities;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class VibrationManager {
    private static VibrationManager instance;
    private Vibrator vibrator;

    private VibrationManager(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static VibrationManager getInstance(Context context) {
        if (instance == null) {
            instance = new VibrationManager(context.getApplicationContext());
        }
        return instance;
    }

    public void vibrate(long duration) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}