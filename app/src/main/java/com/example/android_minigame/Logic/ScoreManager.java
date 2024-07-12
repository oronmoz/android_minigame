package com.example.android_minigame.Logic;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoreManager {
    private static final String PREF_NAME = "ScorePreferences";
    private static final String KEY_SCORES = "Scores";
    private static final int MAX_SCORES = 10;

    private SharedPreferences sharedPreferences;
    private Gson gson;

    public ScoreManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<Score> getTopScores() {
        String json = sharedPreferences.getString(KEY_SCORES, null);
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
        return topScores.size() < MAX_SCORES || score > topScores.get(topScores.size() - 1).getScore();
    }

    public void addScore(String playerName, int score) {
        List<Score> scores = getTopScores();
        scores.add(new Score(playerName, score));
        Collections.sort(scores);

        if (scores.size() > MAX_SCORES) {
            scores = scores.subList(0, MAX_SCORES);
        }

        String json = gson.toJson(scores);
        sharedPreferences.edit().putString(KEY_SCORES, json).apply();
    }
}