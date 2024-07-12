package com.example.android_minigame.UI;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.android_minigame.Fragments.ScoresFragment;
import com.example.android_minigame.R;

public class LeaderboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        Button btnBackToMenu = findViewById(R.id.btn_back_to_menu);
        btnBackToMenu.setOnClickListener(v -> finish());

        // Load the ScoresFragment and MapFragment
        loadFragment(new ScoresFragment(), R.id.scores_container);
        loadFragment(new com.example.android_minigame.UI.MapFragment(), R.id.map_container);
    }

    private void loadFragment(Fragment fragment, int containerId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(containerId, fragment);
        fragmentTransaction.commit();
    }
}