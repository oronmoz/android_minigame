package com.example.android_minigame.Logic;

public class Score implements Comparable<Score> {
    private String playerName;
    private int score;
    private double latitude;
    private double longitude;

    public Score(String playerName, int score, double latitude, double longitude) {
        this.playerName = playerName;
        this.score = score;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public int compareTo(Score other) {
        return Integer.compare(other.score, this.score); // For descending order
    }
}