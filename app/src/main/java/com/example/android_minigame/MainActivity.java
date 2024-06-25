package com.example.android_minigame;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.os.Vibrator;
import android.content.Context;
import android.widget.Toast;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import java.util.ArrayList;
import java.util.Random;
import android.os.Looper;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
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


    // Game mechanics constants
    private float leftLaneX, centerLaneX, rightLaneX;
    private int playerLane = CENTER_LANE;
    private static final float OBSTACLE_SPAWN_CHANCE = 0.05f;
    private static final float OBSTACLE_SPEED = 25f;
    private static final long GAME_LOOP_DELAY = 50;
    private static final long VIBRATION_DURATION = 500;
    private static final int MAX_OBSTACLES = 2;
    private static final long OBSTACLE_SPAWN_DELAY = 500; // 0.5 seconds
    private long lastObstacleSpawnTime = 0;
    private int playerWidth;
    private int playerHeight;
    private int obstacleWidth;
    private int obstacleHeight;


    // Permission request code
    private static final int VIBRATE_PERMISSION_CODE = 1001;

    private RelativeLayout mainLayout;
    private RelativeLayout gameLayout;
    private ImageView playerView;
    private ArrayList<ImageView> obstacles;
    private Button leftButton, rightButton;
    private ImageView[] heartViews;
    private int lives = INITIAL_LIVES;
    private boolean isGameRunning = false;
    private Random random = new Random();
    private Thread gameThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        float density = getResources().getDisplayMetrics().density;
        playerWidth = (int) (PLAYER_WIDTH_DP * density);
        playerHeight = (int) (PLAYER_HEIGHT_DP * density);
        obstacleWidth = (int) (OBSTACLE_WIDTH_DP * density);
        obstacleHeight = (int) (OBSTACLE_HEIGHT_DP * density);

        initializeViews();
        initializeGame();

        // Calculate lane positions after layout is drawn
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


    private void initializeGame() {
        playerLane = CENTER_LANE;
        lives = INITIAL_LIVES;
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

        // Vibrate
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }

        // Show crash notification
        runOnUiThread(() -> {
            Toast toast = Toast.makeText(this, "Crash!", Toast.LENGTH_SHORT);
            toast.show();
        });

        if (lives <= 0) {
            gameOver();
        }
    }

    private void gameOver() {
        isGameRunning = false;
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
        clearObstacles();
        initializeGame();
        startGame();
    }

    private void clearObstacles() {
        for (ImageView obstacle : obstacles) {
            mainLayout.removeView(obstacle);
        }
        obstacles.clear();
    }

    private void startGame() {
        isGameRunning = true;
        gameThread = new Thread(gameLoop);
        gameThread.start();
    }

    private void moveObstacles() {
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            ImageView obstacle = obstacles.get(i);
            float newY = obstacle.getY() + OBSTACLE_SPEED;
            obstacle.setY(newY);

            // Remove obstacle if it's off the screen
            if (newY > gameLayout.getHeight()) {
                gameLayout.removeView(obstacle);
                obstacles.remove(i);
            }
        }
    }

    private Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            while (isGameRunning) {
                runOnUiThread(() -> {
                    moveObstacles();
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
                try {
                    Thread.sleep(GAME_LOOP_DELAY);
                } catch (InterruptedException e) {
                    Log.e("MainActivity", "Game loop interrupted", e);
                }
            }
        }
    };


    @Override
    protected void onStart() {
        super.onStart();
        if (!isGameRunning) {
            startGame();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        isGameRunning = false;
        if (gameThread != null) {
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("MainActivity", "Error stopping game thread", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isGameRunning) {
            startGame();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isGameRunning = false;
        if (gameThread != null) {
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("MainActivity", "Error stopping game thread", e);
            }
        }
    }
}