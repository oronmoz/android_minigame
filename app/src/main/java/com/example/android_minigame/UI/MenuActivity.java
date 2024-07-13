package com.example.android_minigame.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.android_minigame.R;

public class MenuActivity extends AppCompatActivity {

    private ImageButton difficultyButton;
    private ImageButton gameModeButton;
    private Button btnStartGame;
    private Button btnLeaderboard;
    private TextView difficultyText;
    private TextView gameModeText;

    private boolean isHardDifficulty = false;
    private boolean isSensorMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        difficultyButton = findViewById(R.id.difficultyButton);
        gameModeButton = findViewById(R.id.gameModeButton);
        btnStartGame = findViewById(R.id.btn_start_game);
        btnLeaderboard = findViewById(R.id.btn_leaderboard);
        difficultyText = findViewById(R.id.difficultyText);
        gameModeText = findViewById(R.id.gameModeText);

        setupDifficultySelection();
        setupGameModeSelection();

        btnStartGame.setOnClickListener(v -> startGame());
        btnLeaderboard.setOnClickListener(v -> openLeaderboard());
    }

    private void setupDifficultySelection() {
        difficultyButton.setImageResource(R.drawable.normal);
        difficultyButton.setOnClickListener(v -> {
            isHardDifficulty = !isHardDifficulty;
            difficultyButton.setImageResource(isHardDifficulty ? R.drawable.hard : R.drawable.normal);
            difficultyText.setText(isHardDifficulty ? R.string.hard : R.string.normal_difficulty);
            updateStartButtonState();
        });
    }

    private void setupGameModeSelection() {
        Glide.with(this).asGif().load(R.drawable.sensor_off).into(gameModeButton);
        gameModeButton.setOnClickListener(v -> {
            isSensorMode = !isSensorMode;
            Glide.with(this)
                    .asGif()
                    .load(isSensorMode ? R.drawable.sensor_on : R.drawable.sensor_off)
                    .into(gameModeButton);
            gameModeText.setText(isSensorMode ? R.string.sensor : R.string.two_buttons);
            updateStartButtonState();
        });
    }

    private void updateStartButtonState() {
        btnStartGame.setEnabled(true);
    }

    private void startGame() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("DIFFICULTY", isHardDifficulty ? "Hard" : "Normal");
        intent.putExtra("GAME_MODE", isSensorMode ? "Sensor" : "TwoButtons");
        startActivity(intent);
    }

    private void openLeaderboard() {
        Intent intent = new Intent(this, LeaderboardActivity.class);
        startActivity(intent);
    }
}