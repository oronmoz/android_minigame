package com.example.android_minigame;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.os.Vibrator;
import android.content.Context;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import androidx.work.Configuration;
import androidx.work.ExistingWorkPolicy;
import androidx.work.WorkManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkRequest;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;

import com.example.android_minigame.workers.GameWorker;

public class MainActivity extends AppCompatActivity implements GameWorker.GameCallback {

    private static final String PREFS_NAME = "MyGamePrefs";
    private static final String WORKMANAGER_INITIALIZED = "WorkManagerInitialized";
    private WorkManager workManager;
    private UUID currentWorkerId;
    private WorkRequest gameWorkRequest;

    // Layout constants
    private static final int LANES = 3;
    private static final int LEFT_LANE = 0;
    private static final int CENTER_LANE = 1;
    private static final int RIGHT_LANE = 2;
    private static final int INITIAL_LIVES = 3;
    private static final float PLAYER_WIDTH_DP = 115.25f;
    private static final float PLAYER_HEIGHT_DP = 135.5f;
    private static final float OBSTACLE_WIDTH_DP = 40f;
    private static final float OBSTACLE_HEIGHT_DP = 144f;
    public static final long GAME_LOOP_DELAY = 50;

    // Game mechanics constants
    private float leftLaneX, centerLaneX, rightLaneX;
    private int playerLane = CENTER_LANE;
    private static final float OBSTACLE_SPAWN_CHANCE = 0.1f;
    private static final float OBSTACLE_SPEED = 1000f;
    private static final long VIBRATION_DURATION = 500;
    private static final int MAX_OBSTACLES = 2;
    private static final long OBSTACLE_SPAWN_DELAY = 500; // 0.5 seconds
    private long lastObstacleSpawnTime = 0;
    private int playerWidth;
    private int playerHeight;
    private int obstacleWidth;
    private int obstacleHeight;


    // Permission request code
    private static final int PERMISSION_REQUEST_VIBRATE = 1001;

    private RelativeLayout mainLayout;
    private RelativeLayout gameLayout;
    private ImageView playerView;
    private ArrayList<ImageView> obstacles;
    private Button leftButton, rightButton;
    private ImageView[] heartViews;
    private int lives = INITIAL_LIVES;
    private boolean isGameRunning = false;
    private Random random = new Random();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        workManager = WorkManager.getInstance(this);

        setContentView(R.layout.activity_main);

        float density = getResources().getDisplayMetrics().density;
        playerWidth = (int) (PLAYER_WIDTH_DP * density);
        playerHeight = (int) (PLAYER_HEIGHT_DP * density);
        obstacleWidth = (int) (OBSTACLE_WIDTH_DP * density);
        obstacleHeight = (int) (OBSTACLE_HEIGHT_DP * density);

        initializeViews();
        initializeGame();
        requestVibratePermission();

        gameLayout.post(this::calculateLanePositions);

    }

    private void calculateLanePositions() {
        int gameWidth = gameLayout.getWidth();
        float laneWidth = gameWidth / 3f;

        // Calculate the center of each lane
        leftLaneX = laneWidth / 2;
        centerLaneX = gameWidth / 2f;
        rightLaneX = gameWidth - (laneWidth / 2);

        // Initial player position
        updatePlayerPosition();
    }

    private void initializeViews() {
        mainLayout = findViewById(R.id.main);
        gameLayout = findViewById(R.id.gameLayout);
        playerView = findViewById(R.id.main_IMG_player);
        leftButton = findViewById(R.id.main_Button_arrow1);
        rightButton = findViewById(R.id.main_Button_arrow2);
        heartViews = new ImageView[]{
                findViewById(R.id.main_IMG_heart1),
                findViewById(R.id.main_IMG_heart2),
                findViewById(R.id.main_IMG_heart3)
        };

        leftButton.setOnClickListener(v -> movePlayer(-1));
        rightButton.setOnClickListener(v -> movePlayer(1));

        obstacles = new ArrayList<>();
    }

    private void requestVibratePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.VIBRATE},
                    PERMISSION_REQUEST_VIBRATE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_VIBRATE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Vibration permission granted
                Toast.makeText(this, "Vibration permission granted", Toast.LENGTH_SHORT).show();
                // You could initialize vibration here if needed
                initializeVibration();
            } else {
                // Vibration permission denied, handle accordingly
                Toast.makeText(this, "Vibration permission denied. Some features may be limited.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeVibration() {
        // This method could be used to set up vibration-related features
        // For example, you might want to create and store a Vibrator instance:
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            // Vibrator is available and ready to use
            Log.d("MainActivity", "Vibrator initialized");
        } else {
            Log.d("MainActivity", "Vibrator not available on this device");
        }
    }

    private void initializeGame() {
        playerLane = CENTER_LANE;
        lives = INITIAL_LIVES;
        lastObstacleSpawnTime = 0;
        updateLives();
        updatePlayerPosition();
        clearObstacles();
    }

    private void updatePlayerPosition() {
        float targetX;
        switch (playerLane) {
            case LEFT_LANE:
                targetX = leftLaneX;
                break;
            case RIGHT_LANE:
                targetX = rightLaneX;
                break;
            case CENTER_LANE:
            default:
                targetX = centerLaneX;
                break;
        }

        playerView.setX(targetX - (playerWidth / 2f));
    }

    private void movePlayer(int direction) {
        int newLane = playerLane + direction;
        if (newLane >= LEFT_LANE && newLane <= RIGHT_LANE) {
            playerLane = newLane;
            updatePlayerPosition();
        }
    }

    private void updateLives() {
        for (int i = 0; i < heartViews.length; i++) {
            heartViews[i].setVisibility(i < lives ? View.VISIBLE : View.INVISIBLE);
        }
    }

private void spawnObstacle() {
    Log.d("MainActivity", "Spawning obstacle");
    runOnUiThread(() -> {
        ImageView obstacle = new ImageView(this);
        obstacle.setImageResource(R.drawable.dripstone);
        obstacle.setTag("uncollided");

        int lane = random.nextInt(LANES);
        float targetX;
        switch (lane) {
            case LEFT_LANE:
                targetX = leftLaneX;
                break;
            case RIGHT_LANE:
                targetX = rightLaneX;
                break;
            case CENTER_LANE:
            default:
                targetX = centerLaneX;
                break;
        }
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(obstacleWidth, obstacleHeight);
        params.leftMargin = (int) (targetX - (obstacleWidth / 2f));
        params.topMargin = -obstacleHeight;
        gameLayout.addView(obstacle, params);
        obstacles.add(obstacle);
        Log.d("MainActivity", "Obstacle spawned in lane: " + lane);
    });
}

    private boolean checkCollision() {
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
                return true;
            }
        }
        return false;
    }

    private void handleCollision() {
        lives--;
        updateLives();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }

        runOnUiThread(() -> Toast.makeText(this, "Crash!", Toast.LENGTH_SHORT).show());

        if (lives <= 0) {
            gameOver();
        }
    }

    private void gameOver() {
        stopGame();

        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Game Over")
                    .setMessage("You've lost all your lives!")
                    .setCancelable(false)
                    .setPositiveButton("Restart", (dialog, id) -> {
                        dialog.dismiss();
                        restartGame();
                    });

            AlertDialog gameOverDialog = builder.create();
            gameOverDialog.show();
        });
    }

    private void restartGame() {
        stopGame();
        clearObstacles();
        initializeGame();
        startGame();
        GameWorker.setCallback(this);
    }

    private void clearObstacles() {
        for (ImageView obstacle : obstacles) {
            gameLayout.removeView(obstacle);
        }
        obstacles.clear();
    }

    private void startGame() {
        isGameRunning = true;
        Data inputData = new Data.Builder()
                .putLong("GAME_LOOP_DELAY", 16L) // 16ms for ~60 FPS
                .build();

        OneTimeWorkRequest gameWorkRequest = new OneTimeWorkRequest.Builder(GameWorker.class)
                .setInputData(inputData)
                .build();

        currentWorkerId = gameWorkRequest.getId();

        workManager.enqueueUniqueWork(
                "GameWork",
                ExistingWorkPolicy.REPLACE,
                gameWorkRequest
        );

        workManager.getWorkInfoByIdLiveData(currentWorkerId)
                .observe(this, workInfo -> {
                    if (workInfo != null) {
                        Log.d("MainActivity", "WorkInfo state: " + workInfo.getState());
                        if (workInfo.getState() == WorkInfo.State.RUNNING) {
                            Log.d("MainActivity", "GameWorker is running");
                        }
                    }
                });

        GameWorker.setCallback(this);
    }

    private void moveObstacles(float deltaSeconds) {
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            ImageView obstacle = obstacles.get(i);
            float newY = obstacle.getY() + (OBSTACLE_SPEED * deltaSeconds);
            obstacle.setY(newY);

            // Remove obstacle if it's off the screen
            if (newY > gameLayout.getHeight()) {
                gameLayout.removeView(obstacle);
                obstacles.remove(i);
            }
        }
    }

    @Override
    public void onGameTick(long deltaTime) {
        runOnUiThread(() -> {
            float deltaSeconds = deltaTime / 1000f;
            moveObstacles(deltaSeconds);

            // Spawn new obstacles
            if (System.currentTimeMillis() - lastObstacleSpawnTime > OBSTACLE_SPAWN_DELAY
                    && obstacles.size() < MAX_OBSTACLES) {
                if (random.nextFloat() < OBSTACLE_SPAWN_CHANCE) {
                    spawnObstacle();
                    lastObstacleSpawnTime = System.currentTimeMillis();
                }
            }

            if (checkCollision()) {
                handleCollision();
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (!isGameRunning) {
            startGame();
            GameWorker.setCallback(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopGame();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!isGameRunning) {
            startGame();
        }
    }

    private void stopGame() {
        isGameRunning = false;
        if (currentWorkerId != null) {
            workManager.cancelWorkById(currentWorkerId);
            Log.d("MainActivity", "Game stopped, WorkManager task cancelled");
        }
        GameWorker.setCallback(null);  // Remove the callback
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopGame();
    }
}