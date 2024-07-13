package com.example.android_minigame.UI;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
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
import com.example.android_minigame.Logic.ScoreManager;
import com.example.android_minigame.Logic.workers.GameWorker;
import com.example.android_minigame.R;
import com.example.android_minigame.Utilities.SoundManager;
import com.example.android_minigame.Utilities.VibrationManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements GameWorker.GameCallback, GameManager.GameCallback, SensorEventListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_VIBRATE = 1001;
    private static final int PERMISSION_REQUEST_LOCATION = 1002;
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;

    private WorkManager workManager;
    private UUID currentWorkerId;
    private ScoreManager scoreManager;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SoundManager soundManager;
    private VibrationManager vibrationManager;

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
    private String difficulty;
    private String gameMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        workManager = WorkManager.getInstance(this);
        scoreManager = new ScoreManager();
        soundManager = SoundManager.getInstance(this);
        vibrationManager = VibrationManager.getInstance(this);

        difficulty = getIntent().getStringExtra("DIFFICULTY");
        gameMode = getIntent().getStringExtra("GAME_MODE");

        float density = getResources().getDisplayMetrics().density;
        playerWidth = (int) (70f * density);
        playerHeight = (int) (80f * density);
        obstacleWidth = (int) (30f * density);
        obstacleHeight = (int) (100f * density);

        initializeViews();
        adjustLayoutForGameMode();
        initializeSensor();
        requestVibratePermission();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermission();

        gameLayout.post(this::calculateLanePositions);
        gameManager.adjustPlayerPositionForSensorMode();
    }

    private void initializeGameManager() {
        scoreManager = new ScoreManager();
        gameManager = new GameManager(this, gameLayout, playerView, heartViews,
                scoreTextView, odometerTextView, playerWidth, playerHeight, obstacleWidth, obstacleHeight,
                difficulty, gameMode);
        gameManager.setGameCallback(this);
        gameManager.setScoreManager(scoreManager);
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

    private void initializeSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void calculateLanePositions() {
        gameManager.calculateLanePositions();
    }

    private void adjustLayoutForGameMode() {
        RelativeLayout buttonContainer = findViewById(R.id.buttonContainer);
        RelativeLayout gameLayout = findViewById(R.id.gameLayout);

        if ("Sensor".equals(gameMode)) {
            // Hide the button container
            buttonContainer.setVisibility(View.GONE);

            // Extend game layout to full screen
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) gameLayout.getLayoutParams();
            params.removeRule(RelativeLayout.ABOVE);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            gameLayout.setLayoutParams(params);
        } else {
            // Show the button container for non-sensor modes
            buttonContainer.setVisibility(View.VISIBLE);

            // Set game layout above button container
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) gameLayout.getLayoutParams();
            params.addRule(RelativeLayout.ABOVE, R.id.buttonContainer);
            params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            gameLayout.setLayoutParams(params);
        }
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

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        } else {
            startLocationUpdates();
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

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        lastKnownLocation = location;
                    }
                });
    }

    private void movePlayer(int direction) {
        if ("TwoButtons".equals(gameMode)) {
            gameManager.movePlayer(direction);
        }
    }

    private void updateUIForGameMode() {
        if ("Sensor".equals(gameMode)) {
            leftButton.setVisibility(View.GONE);
            rightButton.setVisibility(View.GONE);
        } else {
            leftButton.setVisibility(View.VISIBLE);
            rightButton.setVisibility(View.VISIBLE);
        }
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

    @Override
    public void onGameOver(int finalScore) {
        runOnUiThread(() -> showGameOverDialog(finalScore));
    }


    private void openLeaderboard() {
        Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onNewHighScore(int finalScore) {
        Log.d("MainActivity", "onNewHighScore called with score: " + finalScore);
        runOnUiThread(() -> showHighScoreDialog(finalScore));
    }

    private void showHighScoreDialog(int finalScore) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New High Score!");
        builder.setMessage("Congratulations! You've achieved a high score of " + finalScore);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_high_score, null);
        final EditText input = dialogView.findViewById(R.id.edit_text_player_name);
        final TextView errorText = dialogView.findViewById(R.id.text_error_message);
        builder.setView(dialogView);

        builder.setCancelable(false); // Prevent dialog from closing on outside touch

        final AlertDialog dialog = builder.create();

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", (dialogInterface, i) -> {
            // This will be overridden
        });

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialogInterface, i) -> {
            dialog.dismiss();
            returnToMenu();
        });

        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String playerName = input.getText().toString().trim();
            if (playerName.isEmpty()) {
                errorText.setText("Add your name:");
                errorText.setVisibility(View.VISIBLE);
            } else {
                if (lastKnownLocation != null) {
                    scoreManager.addScore(playerName, finalScore, lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                } else {
                    scoreManager.addScore(playerName, finalScore, 0, 0); // Use default coordinates if location is unavailable
                }
                dialog.dismiss();
                openLeaderboard();
            }
        });
    }

    private void showGameOverDialog(int finalScore) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game Over")
                .setMessage("Your final score: " + finalScore)
                .setPositiveButton("Restart", (dialog, id) -> restartGame())
                .setNegativeButton("Main Menu", (dialog, id) -> returnToMenu());

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void returnToMenu() {
        Intent intent = new Intent(MainActivity.this, MenuActivity.class);
        startActivity(intent);
        finish();
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
        if ("Sensor".equals(gameMode)) {
            sensorManager.unregisterListener(this);
        }
        stopGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ("Sensor".equals(gameMode)) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (!gameManager.isGameRunning()) {
            startGame();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float tiltValue = event.values[0]; // X-axis tilt
            gameManager.handleSensorInput(tiltValue);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do Nothing
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundManager != null) {
            soundManager.release();
        }
        gameManager.release();
        stopGame();
    }
}