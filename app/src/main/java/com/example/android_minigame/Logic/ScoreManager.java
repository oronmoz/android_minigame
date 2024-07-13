package com.example.android_minigame.Logic;

import com.example.android_minigame.Utilities.SharedPreferencesManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class ScoreManager {
    private static final String KEY_SCORES = "Scores";
    private static final int MAX_SCORES = 10;

    private Gson gson;

    public ScoreManager() {
        gson = new Gson();
    }

    public List<Score> getTopScores() {
        String json = SharedPreferencesManager.getInstance().getString(KEY_SCORES, null);
        Type type = new TypeToken<ArrayList<Score>>() {
        }.getType();
        List<Score> scores = gson.fromJson(json, type);

        if (scores == null) {
            scores = new ArrayList<>();
        }

        Collections.sort(scores);
        return scores.subList(0, Math.min(scores.size(), MAX_SCORES));
    }

    public boolean isHighScore(int score) {
        List<Score> topScores = getTopScores();
        return topScores.isEmpty() || topScores.size() < MAX_SCORES || score > topScores.get(topScores.size() - 1).getScore();
    }

    public void addScore(String playerName, int score, double latitude, double longitude) {
        List<Score> scores = getTopScores();
        scores.add(new Score(playerName, score, latitude, longitude));
        Collections.sort(scores);

        if (scores.size() > MAX_SCORES) {
            scores = scores.subList(0, MAX_SCORES);
        }

        String json = gson.toJson(scores);
        SharedPreferencesManager.getInstance().putString(KEY_SCORES, json);
    }
}