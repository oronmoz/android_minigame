package com.example.android_minigame.Logic.workers;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class GameWorker extends Worker {
    private static final String TAG = "GameWorker";
    private long gameLoopDelay;
    private static GameCallback callback;

    public GameWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.gameLoopDelay = workerParams.getInputData().getLong("GAME_LOOP_DELAY", 16);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "GameWorker started");
        int tickCount = 0;
        long lastUpdateTime = SystemClock.elapsedRealtime();

        while (!isStopped()) {
            long currentTime = SystemClock.elapsedRealtime();
            long deltaTime = currentTime - lastUpdateTime;

            if (deltaTime >= gameLoopDelay) {
                if (callback != null) {
                    try {
                        callback.onGameTick(deltaTime);
                        tickCount++;
                        if (tickCount % 60 == 0) {
                            Log.d(TAG, "Game tick: " + tickCount);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in game tick", e);
                    }
                } else {
                    Log.w(TAG, "Callback is null, stopping worker");
                    return Result.success();
                }
                lastUpdateTime = currentTime;
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Game loop interrupted", e);
                    return Result.failure();
                }
            }
        }
        Log.d(TAG, "GameWorker stopped");
        return Result.success();
    }

    public static void setCallback(GameCallback cb) {
        callback = cb;
        Log.d(TAG, "Callback set");
    }

    public interface GameCallback {
        void onGameTick(long deltaTime);
    }
}