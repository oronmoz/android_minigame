package com.example.android_minigame.Fragments;

import android.content.Context;
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

    public interface OnScoreSelectedListener {
        void onScoreSelected(Score score);
    }

    private OnScoreSelectedListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnScoreSelectedListener) {
            listener = (OnScoreSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnScoreSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scores, container, false);

        scoreManager = new ScoreManager();
        recyclerView = view.findViewById(R.id.recyclerViewScores);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        updateScores();

        return view;
    }

    private void updateScores() {
        List<Score> topScores = scoreManager.getTopScores();
        adapter = new ScoreAdapter(topScores, score -> {
            if (listener != null) {
                listener.onScoreSelected(score);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private static class ScoreAdapter extends RecyclerView.Adapter<ScoreViewHolder> {
        private List<Score> scores;
        private OnScoreClickListener listener;

        interface OnScoreClickListener {
            void onScoreClick(Score score);
        }

        ScoreAdapter(List<Score> scores, OnScoreClickListener listener) {
            this.scores = scores;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ScoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_score, parent, false);
            return new ScoreViewHolder(view, listener);
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
        ScoreAdapter.OnScoreClickListener listener;

        ScoreViewHolder(View itemView, ScoreAdapter.OnScoreClickListener listener) {
            super(itemView);
            textViewRank = itemView.findViewById(R.id.textViewRank);
            textViewPlayerName = itemView.findViewById(R.id.textViewPlayerName);
            textViewScore = itemView.findViewById(R.id.textViewScore);
            this.listener = listener;
        }

        void bind(final Score score, int rank) {
            textViewRank.setText(String.valueOf(rank));
            textViewPlayerName.setText(score.getPlayerName());
            textViewScore.setText(String.valueOf(score.getScore()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onScoreClick(score);
                }
            });
        }
    }
}