package com.example.android_minigame.Logic;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.android_minigame.Logic.workers.GameWorker;
import com.example.android_minigame.R;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameManager implements GameWorker.GameCallback {
    private static final String TAG = "GameManager";
    private static final int LANES = 5;
    private static final int LEFT_LANE = 0;
    private static final int CENTER_LANE = 2;
    private static final int RIGHT_LANE = 4;
    private static final int INITIAL_LIVES = 3;
    private static final float OBSTACLE_SPAWN_CHANCE = 0.3f;
    private static final float OBSTACLE_SPEED = 1000f;
    private static final long VIBRATION_DURATION = 500;
    private static final int MAX_OBSTACLES = 3;
    private static final long OBSTACLE_SPAWN_DELAY = 500; // 0.5 seconds
    private static final float DIAMOND_SPAWN_CHANCE = 0.2f;
    private static final float DIAMOND_SPEED = 800f;
    private static final int MAX_DIAMONDS = 1;
    private static final int DISTANCE_INCREMENT = 1;


    private TextView scoreTextView;
    private TextView odometerTextView;
    private int score;
    private int distance;
    private Context context;
    private RelativeLayout gameLayout;
    private ImageView playerView;
    private CopyOnWriteArrayList<ImageView> obstacles;
    private CopyOnWriteArrayList<ImageView> diamonds;
    private ImageView[] heartViews;
    private float[] lanePositions;
    private int playerLane;
    private int lives;
    private long lastObstacleSpawnTime;
    private boolean isGameRunning;
    private Random random;
    private MediaPlayer crashSound;
    private int playerWidth;
    private int playerHeight;
    private int obstacleWidth;
    private int obstacleHeight;
    private GameCallback gameCallback;
    private long lastUpdateTime;

    public interface GameCallback {
        void onGameOver();

        void onLivesUpdated(int lives);

        void onScoreUpdated(int score);

        void onDistanceUpdated(int distance);
    }

    public GameManager(Context context, RelativeLayout gameLayout, ImageView playerView,
                       ImageView[] heartViews, TextView scoreTextView, TextView odometerTextView,
                       int playerWidth, int playerHeight, int obstacleWidth, int obstacleHeight) {
        this.context = context;
        this.gameLayout = gameLayout;
        this.playerView = playerView;
        this.heartViews = heartViews;
        this.playerWidth = playerWidth;
        this.playerHeight = playerHeight;
        this.obstacleWidth = obstacleWidth;
        this.obstacleHeight = obstacleHeight;
        this.obstacles = new CopyOnWriteArrayList<>();
        this.diamonds = new CopyOnWriteArrayList<>();
        this.odometerTextView = odometerTextView;
        this.distance = 0;
        this.random = new Random();
        initializeSounds();
    }

    public void setGameCallback(GameCallback callback) {
        this.gameCallback = callback;
    }

    private void initializeGame() {
        playerLane = CENTER_LANE;
        lives = INITIAL_LIVES;
        lastObstacleSpawnTime = 0;
        distance = 0;
        score = 0;
        updateOdometer();
        updateScore();
        updateLives();
        updatePlayerPosition();
        clearObstacles();
    }

    private void initializeSounds() {
        try {
            crashSound = MediaPlayer.create(context, R.raw.crash_sound);
            if (crashSound == null) {
                Log.e(TAG, "Failed to create MediaPlayer for crash sound");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing sounds", e);
        }
    }

    public void calculateLanePositions() {
        int gameWidth = gameLayout.getWidth();
        float laneWidth = gameWidth / (float) LANES;

        lanePositions = new float[LANES];
        for (int i = 0; i < LANES; i++) {
            lanePositions[i] = (i + 0.5f) * laneWidth;
        }

        updatePlayerPosition();
        Log.d(TAG, "Lane positions calculated: " + Arrays.toString(lanePositions));
    }

    public void updatePlayerPosition() {
        if (lanePositions != null && playerLane >= 0 && playerLane < LANES) {
            float targetX = lanePositions[playerLane];
            playerView.setX(targetX - (playerWidth / 2f));
        }
    }

    public void movePlayer(int direction) {
        int newLane = playerLane + direction;
        if (newLane >= 0 && newLane < LANES) {
            playerLane = newLane;
            updatePlayerPosition();
        }
    }

    private void updateLives() {
        for (int i = 0; i < heartViews.length; i++) {
            heartViews[i].setVisibility(i < lives ? View.VISIBLE : View.INVISIBLE);
        }
        if (gameCallback != null) {
            gameCallback.onLivesUpdated(lives);
        }
    }

    public void spawnObstacle() {
        if (lanePositions == null || lanePositions.length == 0) {
            Log.e(TAG, "Lane positions not initialized");
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            ImageView obstacle = new ImageView(context);
            obstacle.setImageResource(R.drawable.dripstone);
            obstacle.setTag("uncollided");

            int lane = random.nextInt(LANES);
            float targetX = lanePositions[lane];
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(obstacleWidth, obstacleHeight);
            params.leftMargin = (int) (targetX - (obstacleWidth / 2f));
            params.topMargin = -obstacleHeight;
            gameLayout.addView(obstacle, params);
            obstacles.add(obstacle);
            Log.d(TAG, "Obstacle added to layout");
            Log.d(TAG, "Obstacle spawned in lane " + lane);
        });
    }

    public void spawnDiamond() {
        if (lanePositions == null || lanePositions.length == 0) {
            Log.e(TAG, "Lane positions not initialized");
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            ImageView diamond = new ImageView(context);
            Glide.with(context)
                    .asGif()
                    .load(R.drawable.diamonds)
                    .into(diamond);
            diamond.setTag("uncollected");

            int lane = random.nextInt(LANES);
            float targetX = lanePositions[lane];
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(obstacleWidth, obstacleHeight);
            params.leftMargin = (int) (targetX - (obstacleWidth / 2f));
            params.topMargin = -obstacleHeight;
            gameLayout.addView(diamond, params);
            diamonds.add(diamond);
            Log.d(TAG, "Diamond spawned in lane " + lane);
        });
    }

    public boolean checkCollision() {
        for (ImageView obstacle : obstacles) {
            if ("collided".equals(obstacle.getTag())) {
                continue;
            }

            float obstacleLeft = obstacle.getX();
            float obstacleRight = obstacleLeft + obstacleWidth;
            float obstacleTop = obstacle.getY();
            float obstacleBottom = obstacleTop + obstacleHeight;

            float playerLeft = playerView.getX();
            float playerRight = playerLeft + playerWidth;
            float playerTop = playerView.getY();
            float playerBottom = playerTop + playerHeight;

            if (playerLeft < obstacleRight && playerRight > obstacleLeft &&
                    playerTop < obstacleBottom && playerBottom > obstacleTop) {
                obstacle.setTag("collided");
                Log.d(TAG, "Collision detected!");
                return true;
            }
        }
        return false;
    }

    public void handleCollision() {
        new Handler(Looper.getMainLooper()).post(() -> {
            lives--;
            updateLives();

            // Show crash notification using Toast
            showCrashNotification();

            playCrashSound();
            vibrate();

            if (lives <= 0) {
                gameOver();
            }
        });
    }

    private void showCrashNotification() {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                Toast.makeText(context, "Crash!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Showing crash notification Toast");
            });
        } else {
            Log.e(TAG, "Context is not an Activity, can't show Toast");
        }
    }

    private void playCrashSound() {
        if (crashSound != null) {
            try {
                crashSound.seekTo(0);
                crashSound.start();
                Log.d(TAG, "Crash sound started playing");
            } catch (Exception e) {
                Log.e(TAG, "Error playing crash sound", e);
            }
        } else {
            Log.e(TAG, "Crash sound is null, can't play");
        }
    }

    private void vibrate() {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.VIBRATE)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    private void gameOver() {
        isGameRunning = false;
        if (gameCallback != null) {
            gameCallback.onGameOver();
        }
    }

    public void startGame() {
        if (!isGameRunning) {
            isGameRunning = true;
            lastUpdateTime = SystemClock.elapsedRealtime();
            Log.d(TAG, "Game started. isGameRunning set to true");
            initializeGame();
        }
    }

    public void stopGame() {
        isGameRunning = false;
        Log.d(TAG, "Game stopped. isGameRunning set to false");
    }

    public boolean isGameRunning() {
        return isGameRunning;
    }

    public void clearObstacles() {
        for (ImageView obstacle : obstacles) {
            gameLayout.removeView(obstacle);
        }
        obstacles.clear();
    }

    public void moveObstacles(float deltaSeconds) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (ImageView obstacle : obstacles) {
                float newY = obstacle.getY() + (OBSTACLE_SPEED * deltaSeconds);
                obstacle.setY(newY);

                if (newY > gameLayout.getHeight()) {
                    gameLayout.removeView(obstacle);
                    obstacles.remove(obstacle);
                }
            }
        });
    }

    @Override
    public void onGameTick(long deltaTime) {
        if (isGameRunning) {
            update(deltaTime);
        }
    }

    public void update(long deltaTime) {
        float deltaSeconds = deltaTime / 1000f;
        moveObstacles(deltaSeconds);
        moveDiamonds(deltaSeconds);

        // Increase distance
        distance += DISTANCE_INCREMENT;
        updateOdometer();

        long currentTime = SystemClock.elapsedRealtime();

        if (currentTime - lastUpdateTime > OBSTACLE_SPAWN_DELAY) {
            float randomValue = random.nextFloat();
            if (randomValue < OBSTACLE_SPAWN_CHANCE && obstacles.size() < MAX_OBSTACLES) {
                spawnObstacle();
                lastUpdateTime = currentTime;
            } else if (randomValue < OBSTACLE_SPAWN_CHANCE + DIAMOND_SPAWN_CHANCE && diamonds.size() < MAX_DIAMONDS) {
                spawnDiamond();
                lastUpdateTime = currentTime;
            }
        }

        if (checkCollision()) {
            handleCollision();
        }
        checkDiamondCollection();
    }

    // Update the updateOdometer method
    private void updateOdometer() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (odometerTextView != null) {
                odometerTextView.setText("Distance: " + distance + " m");
            }
            if (gameCallback != null) {
                gameCallback.onDistanceUpdated(distance);
            }
        });
    }

    // Add a method to update the score
    private void updateScore() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (scoreTextView != null) {
                scoreTextView.setText("Score: " + score);
            }
            if (gameCallback != null) {
                gameCallback.onScoreUpdated(score);
            }
        });
    }

    public void moveDiamonds(float deltaSeconds) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (ImageView diamond : diamonds) {
                float newY = diamond.getY() + (DIAMOND_SPEED * deltaSeconds);
                diamond.setY(newY);

                if (newY > gameLayout.getHeight()) {
                    gameLayout.removeView(diamond);
                    diamonds.remove(diamond);
                }
            }
        });
    }

    public void checkDiamondCollection() {
        if (diamonds == null) return;
        for (ImageView diamond : diamonds) {
            if ("collected".equals(diamond.getTag())) {
                continue;
            }

            float diamondLeft = diamond.getX();
            float diamondRight = diamondLeft + obstacleWidth;
            float diamondTop = diamond.getY();
            float diamondBottom = diamondTop + obstacleHeight;

            float playerLeft = playerView.getX();
            float playerRight = playerLeft + playerWidth;
            float playerTop = playerView.getY();
            float playerBottom = playerTop + playerHeight;

            if (playerLeft < diamondRight && playerRight > diamondLeft &&
                    playerTop < diamondBottom && playerBottom > diamondTop) {
                diamond.setTag("collected");
                collectDiamond(diamond);
            }
        }
    }

    private void collectDiamond(final ImageView diamond) {
        score++;
        updateScore();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                gameLayout.removeView(diamond);
                diamonds.remove(diamond);
                //showDiamondCollectionNotification();
            }
        });
    }

    private void showDiamondCollectionNotification() {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                Toast.makeText(context, "Diamond Collected!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Showing diamond collection notification Toast");
            });
        } else {
            Log.e(TAG, "Context is not an Activity, can't show Toast");
        }
    }

    public void release() {
        if (crashSound != null) {
            crashSound.release();
            crashSound = null;
        }
    }
}