package com.example.android_minigame.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android_minigame.Logic.Score;
import com.example.android_minigame.Logic.ScoreManager;
import com.example.android_minigame.R;

import java.util.List;

public class ScoresFragment extends Fragment {
    private ScoreManager scoreManager;
    private RecyclerView recyclerView;
    private ScoreAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scores, container, false);

        scoreManager = new ScoreManager(requireContext());
        recyclerView = view.findViewById(R.id.recyclerViewScores);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        updateScores();

        return view;
    }

    private void updateScores() {
        List<Score> topScores = scoreManager.getTopScores();
        adapter = new ScoreAdapter(topScores);
        recyclerView.setAdapter(adapter);
    }

    private class ScoreAdapter extends RecyclerView.Adapter<ScoreViewHolder> {
        private List<Score> scores;

        ScoreAdapter(List<Score> scores) {
            this.scores = scores;
        }

        @NonNull
        @Override
        public ScoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_score, parent, false);
            return new ScoreViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ScoreViewHolder holder, int position) {
            Score score = scores.get(position);
            holder.bind(score, position + 1);
        }

        @Override
        public int getItemCount() {
            return scores.size();
        }
    }

    private static class ScoreViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRank;
        TextView textViewPlayerName;
        TextView textViewScore;

        ScoreViewHolder(View itemView) {
            super(itemView);
            textViewRank = itemView.findViewById(R.id.textViewRank);
            textViewPlayerName = itemView.findViewById(R.id.textViewPlayerName);
            textViewScore = itemView.findViewById(R.id.textViewScore);
        }

        void bind(Score score, int rank) {
            textViewRank.setText(String.valueOf(rank));
            textViewPlayerName.setText(score.getPlayerName());
            textViewScore.setText(String.valueOf(score.getScore()));
        }
    }
}