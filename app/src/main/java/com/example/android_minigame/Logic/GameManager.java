package com.example.android_minigame.Logic;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.android_minigame.Logic.workers.GameWorker;
import com.example.android_minigame.R;
import com.example.android_minigame.Utilities.SoundManager;
import com.example.android_minigame.Utilities.VibrationManager;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameManager implements GameWorker.GameCallback {
    private static final String TAG = "GameManager";

    // Game Absolute Constants
    private static final int LANES = 5;
    private static final int CENTER_LANE = 2;
    private static final int INITIAL_LIVES = 3;
    private static final long VIBRATION_DURATION = 500;
    private static final int MAX_OBSTACLES = 3;
    private static final int MAX_DIAMONDS = 1;
    private static final float TILT_THRESHOLD = 3.0f;
    private static final float SMOOTHING_FACTOR = 0.1f;
    private static final int DIAMOND_VALUE = 50;

    // Easy Difficulty Constants
    private static final float EASY_OBSTACLE_SPAWN_CHANCE = 0.3f;
    private static final float EASY_OBSTACLE_SPEED = 700f;
    private static final long EASY_OBSTACLE_SPAWN_DELAY = 1000;
    private static final float EASY_DIAMOND_SPAWN_CHANCE = 0.4f;
    private static final float EASY_DIAMOND_SPEED = 500f;
    private static final int EASY_DISTANCE_INCREMENT = 1;

    // Hard Difficulty Constants
    private static final float HARD_OBSTACLE_SPAWN_CHANCE = 0.6f;
    private static final float HARD_OBSTACLE_SPEED = 1400f;
    private static final long HARD_OBSTACLE_SPAWN_DELAY = 500;
    private static final float HARD_DIAMOND_SPAWN_CHANCE = 0.2f;
    private static final float HARD_DIAMOND_SPEED = 1000f;
    private static final int HARD_DISTANCE_INCREMENT = 2;

    // Game settings
    private float OBSTACLE_SPAWN_CHANCE;
    private float OBSTACLE_SPEED;
    private long OBSTACLE_SPAWN_DELAY;
    private float DIAMOND_SPAWN_CHANCE;
    private float DIAMOND_SPEED;
    private int DISTANCE_INCREMENT;

    private Context context;
    private Random random;
    private GameCallback gameCallback;
    private MediaPlayer crashSound;
    private ScoreManager scoreManager;
    private SoundManager soundManager;
    private VibrationManager vibrationManager;


    //Game Views
    private RelativeLayout gameLayout;
    private TextView scoreTextView;
    private TextView odometerTextView;
    private ImageView playerView;
    private ImageView[] heartViews;

    //Game Values
    private String gameMode;
    private boolean isTilted = false;
    private float lastTiltValue = 0f;
    private int score;
    private int distance;
    private CopyOnWriteArrayList<ImageView> obstacles;
    private CopyOnWriteArrayList<ImageView> diamonds;
    private float[] lanePositions;
    private int playerLane;
    private int lives;
    private long lastObstacleSpawnTime;
    private boolean isGameRunning;
    private int playerWidth;
    private int playerHeight;
    private int obstacleWidth;
    private int obstacleHeight;
    private long lastUpdateTime;
    private float smoothedTilt = 0f;
    private int difficultyFactor = 0;

    public interface GameCallback {
        void onLivesUpdated(int lives);
        void onScoreUpdated(int score);
        void onDistanceUpdated(int distance);

        void onGameOver(int finalScore);

        void onNewHighScore(int finalScore);
    }

    public GameManager(Context context, RelativeLayout gameLayout, ImageView playerView,
                       ImageView[] heartViews, TextView scoreTextView, TextView odometerTextView,
                       int playerWidth, int playerHeight, int obstacleWidth, int obstacleHeight,
                       String difficulty, String gameMode) {
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
        this.vibrationManager = VibrationManager.getInstance(context);
        this.soundManager = SoundManager.getInstance(context);
        initializeSounds();
        this.gameMode = gameMode;
        setDifficulty(difficulty);
    }

    private void setDifficulty(String difficulty) {
        if ("Hard".equals(difficulty)) {
            OBSTACLE_SPAWN_CHANCE = HARD_OBSTACLE_SPAWN_CHANCE;
            OBSTACLE_SPEED = HARD_OBSTACLE_SPEED;
            OBSTACLE_SPAWN_DELAY = HARD_OBSTACLE_SPAWN_DELAY;
            DIAMOND_SPAWN_CHANCE = HARD_DIAMOND_SPAWN_CHANCE;
            DIAMOND_SPEED = HARD_DIAMOND_SPEED;
            DISTANCE_INCREMENT = HARD_DISTANCE_INCREMENT;
            difficultyFactor = 2;
        } else {
            // Default to Easy
            OBSTACLE_SPAWN_CHANCE = EASY_OBSTACLE_SPAWN_CHANCE;
            OBSTACLE_SPEED = EASY_OBSTACLE_SPEED;
            OBSTACLE_SPAWN_DELAY = EASY_OBSTACLE_SPAWN_DELAY;
            DIAMOND_SPAWN_CHANCE = EASY_DIAMOND_SPAWN_CHANCE;
            DIAMOND_SPEED = EASY_DIAMOND_SPEED;
            DISTANCE_INCREMENT = EASY_DISTANCE_INCREMENT;
            difficultyFactor = 1;
        }
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

    public void adjustPlayerPositionForSensorMode() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) playerView.getLayoutParams();
        if ("Sensor".equals(gameMode)) {
            params.bottomMargin = (int) (gameLayout.getHeight() * 0.1); // 10% from bottom
        } else {
            params.bottomMargin = 0;
        }
        playerView.setLayoutParams(params);
    }

    public void updatePlayerPosition() {
        if (lanePositions != null && playerLane >= 0 && playerLane < LANES) {
            float targetX = lanePositions[playerLane];
            playerView.setX(targetX - (playerWidth / 2f));
        }
    }

    public void setScoreManager(ScoreManager scoreManager) {
        this.scoreManager = scoreManager;
    }

    public void handleSensorInput(float tiltValue) {
        if ("Sensor".equals(gameMode)) {
            // Apply exponential smoothing
            smoothedTilt = SMOOTHING_FACTOR * tiltValue + (1 - SMOOTHING_FACTOR) * smoothedTilt;

            int newLane = calculateLaneFromTilt(smoothedTilt);
            if (newLane != playerLane) {
                playerLane = newLane;
                updatePlayerPosition();
            }
        }
    }

    private int calculateLaneFromTilt(float tiltValue) {
        int laneDifference = Math.round(tiltValue / TILT_THRESHOLD);
        int newLane = CENTER_LANE - laneDifference;
        return Math.max(0, Math.min(LANES - 1, newLane));
    }

    public void movePlayer(int direction) {
        if ("TwoButtons".equals(gameMode)) {
            int newLane = playerLane + direction;
            if (newLane >= 0 && newLane < LANES) {
                playerLane = newLane;
                updatePlayerPosition();
            }
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
        try {
            soundManager.playCrashSound();
            ; // 500ms vibration
        } catch (Exception e) {
            Log.e(TAG, "Error during sound", e);
        }
    }


    private void vibrate() {
        try {
            vibrationManager.vibrate(500); // 500ms vibration
        } catch (Exception e) {
            Log.e(TAG, "Error during vibration", e);
        }
    }

    public void gameOver() {
        isGameRunning = false;
        Log.d("GameManager", "Game Over. Score: " + score);
        boolean isHighScore = scoreManager.isHighScore(score);
        Log.d("GameManager", "Is High Score: " + isHighScore);
        if (isHighScore) {
            if (gameCallback != null) {
                Log.d("GameManager", "Calling onNewHighScore");
                gameCallback.onNewHighScore(score);
            }
        } else {
            if (gameCallback != null) {
                Log.d("GameManager", "Calling onGameOver");
                gameCallback.onGameOver(score);
            }
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
        score = score + (DIAMOND_VALUE * difficultyFactor);
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

    public void release() {
        if (crashSound != null) {
            crashSound.release();
            crashSound = null;
        }
    }
}