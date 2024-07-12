package com.example.android_minigame.UI;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android_minigame.R;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button btnChooseMode1 = findViewById(R.id.btn_choose_mode_1);
        Button btnChooseMode2 = findViewById(R.id.btn_choose_mode_2);
        Button btnStartGame = findViewById(R.id.btn_start_game);
        Button btnLeaderboard = findViewById(R.id.btn_leaderboard);

        btnChooseMode1.setOnClickListener(v -> {
            // TODO: Implement mode selection functionality
        });

        btnChooseMode2.setOnClickListener(v -> {
            // TODO: Implement mode selection functionality
        });

        btnStartGame.setOnClickListener(v -> startGame());

        btnLeaderboard.setOnClickListener(v -> openLeaderboard());
    }

    private void startGame() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void openLeaderboard() {
        Intent intent = new Intent(this, LeaderboardActivity.class);
        startActivity(intent);
    }
}