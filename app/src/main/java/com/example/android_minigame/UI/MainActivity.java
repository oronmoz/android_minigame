package com.example.android_minigame.UI;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.android_minigame.Logic.GameManager;
import com.example.android_minigame.Logic.workers.GameWorker;
import com.example.android_minigame.R;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements GameWorker.GameCallback, GameManager.GameCallback {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_VIBRATE = 1001;

    private WorkManager workManager;
    private UUID currentWorkerId;
    private ScoreManager scoreManager;

    private RelativeLayout mainLayout;
    private RelativeLayout gameLayout;

    private TextView scoreTextView;
    private TextView odometerTextView;
    private ImageView playerView;
    private Button leftButton, rightButton;
    private ImageView[] heartViews;

    private int playerWidth;
    private int playerHeight;
    private int obstacleWidth;
    private int obstacleHeight;
    private GameManager gameManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        workManager = WorkManager.getInstance(this);

        float density = getResources().getDisplayMetrics().density;
        playerWidth = (int) (70f * density);
        playerHeight = (int) (80f * density);
        obstacleWidth = (int) (30f * density);
        obstacleHeight = (int) (100f * density);

        initializeViews();
        requestVibratePermission();

        gameLayout.post(this::calculateLanePositions);
    }

    private void initializeGameManager() {
        gameManager = new GameManager(this, gameLayout, playerView, heartViews,
                scoreTextView, odometerTextView, playerWidth, playerHeight, obstacleWidth, obstacleHeight);
        gameManager.setGameCallback(this);
    }

    private void initializeViews() {
        mainLayout = findViewById(R.id.main);
        scoreTextView = findViewById(R.id.scoreTextView);
        odometerTextView = findViewById(R.id.odometerTextView);
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

        initializeGameManager();
    }

    private void calculateLanePositions() {
        gameManager.calculateLanePositions();
    }

    private void startGameWorker() {
        workManager.cancelAllWorkByTag("GameWorker");
        GameWorker.setCallback(gameManager);
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
                        Log.d(TAG, "WorkInfo state: " + workInfo.getState());
                        if (workInfo.getState() == WorkInfo.State.RUNNING) {
                            Log.d(TAG, "GameWorker is running");
                        }
                    }
                });
    }

    @Override
    public void onGameTick(long deltaTime) {
        runOnUiThread(() -> {
            if (gameManager.isGameRunning()) {
                gameManager.onGameTick(deltaTime);
            }
        });
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
                Toast.makeText(this, "Vibration permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Vibration permission denied. Some features may be limited.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void movePlayer(int direction) {
        gameManager.movePlayer(direction);
    }

    private void startGame() {
        gameManager.startGame();
        startGameWorker();
        Data inputData = new Data.Builder()
                .putLong("GAME_LOOP_DELAY", 16L)
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
                        Log.d(TAG, "WorkInfo state: " + workInfo.getState());
                        if (workInfo.getState() == WorkInfo.State.RUNNING) {
                            Log.d(TAG, "GameWorker is running");
                        }
                    }
                });
    }

    @Override
    public void onGameOver(int finalScore) {
        if (scoreManager.isHighScore(finalScore)) {
            showHighScoreDialog(finalScore);
        } else {
            showGameOverDialog();
        }
    }

    private void showHighScoreDialog(int finalScore) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New High Score!");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String playerName = input.getText().toString();
            scoreManager.addScore(playerName, finalScore);
            showGameOverDialog();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            showGameOverDialog();
        });

        builder.show();
    }

    private void showGameOverDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game Over")
                .setMessage("Your game has ended.")
                .setPositiveButton("Restart", (dialog, id) -> restartGame())
                .setNegativeButton("Main Menu", (dialog, id) -> {
                    finish();
                    Intent intent = new Intent(MainActivity.this, MenuActivity.class);
                    startActivity(intent);
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onLivesUpdated(int lives) {
        runOnUiThread(() -> {
            for (int i = 0; i < heartViews.length; i++) {
                heartViews[i].setVisibility(i < lives ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }

    @Override
    public void onScoreUpdated(int score) {
        runOnUiThread(() -> {
            scoreTextView.setText("Score: " + score);
        });
    }

    @Override
    public void onDistanceUpdated(int distance) {
        runOnUiThread(() -> {
            odometerTextView.setText("Distance: " + distance + " m");
        });
    }

    private void restartGame() {
        stopGame();
        startGame();
    }

    private void stopGame() {
        gameManager.stopGame();
        if (currentWorkerId != null) {
            workManager.cancelWorkById(currentWorkerId);
            Log.d(TAG, "Game stopped, WorkManager task cancelled");
        }
        GameWorker.setCallback(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!gameManager.isGameRunning()) {
            startGame();
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
        if (!gameManager.isGameRunning()) {
            startGame();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gameManager.release();
        stopGame();
    }
}