package com.example.slagalica.ui.spojnice;

import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.data.model.SpojnicePair;
import com.example.slagalica.databinding.ActivitySpojniceBinding;
import com.example.slagalica.ui.BaseActivity;
import com.example.slagalica.ui.widget.SpojItemView;

import java.util.List;

public class SpojniceActivity extends BaseActivity {

    private static final int PAIRS_PER_ROUND = 5;
    /** Placeholder opponent until live matchmaking exists (mirrors the design mock). */
    private static final int DEMO_OPPONENT_SCORE = 6;

    private ActivitySpojniceBinding binding;
    private SpojniceViewModel viewModel;

    private int selectedLeftIndex = -1;
    private boolean[] leftResolved;   // left tile locked: correctly matched OR wrongly guessed
    private boolean[] rightConnected; // right tile correctly matched (only correct pairs lock a song)
    private int matchCount;

    // Sunny pop: each matched pair gets its own colour (no lines). Resolved from tokens.
    private int[] pairColors;

    private SpojItemView[] leftItems;
    private SpojItemView[] rightItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpojniceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        resolveColors();
        setupStatsBar();
        setOpponent("Jelena_K");

        leftItems = new SpojItemView[]{binding.btnL1, binding.btnL2, binding.btnL3, binding.btnL4, binding.btnL5};
        rightItems = new SpojItemView[]{binding.btnR1, binding.btnR2, binding.btnR3, binding.btnR4, binding.btnR5};

        viewModel = new ViewModelProvider(this).get(SpojniceViewModel.class);

        setupObservers();
        setupItemListeners();
        viewModel.loadRound(1);
    }

    private void resolveColors() {
        pairColors = new int[]{
                ContextCompat.getColor(this, R.color.g0),
                ContextCompat.getColor(this, R.color.g1),
                ContextCompat.getColor(this, R.color.g2),
                ContextCompat.getColor(this, R.color.g3),
                ContextCompat.getColor(this, R.color.accent_end)
        };
    }

    private void setupObservers() {
        viewModel.getCurrentPairs().observe(this, pairs -> {
            resetRoundState();
            populateColumns(pairs);
            startRoundTimer();
        });

        viewModel.getScore().observe(this, score -> {
            updateScore(score, DEMO_OPPONENT_SCORE);
            binding.tvRoundPoints.setText("+" + score + " bodova");
        });

        viewModel.getGameFinished().observe(this, finished -> {
            if (Boolean.TRUE.equals(finished)) showGameFinished();
        });
    }

    private void populateColumns(SpojnicePair pairs) {
        List<String> left = pairs.getLeftTerms();
        List<String> right = pairs.getRightTerms();
        for (int i = 0; i < PAIRS_PER_ROUND; i++) {
            leftItems[i].setLabel(left.get(i));
            rightItems[i].setLabel(right.get(i));
        }
    }

    private void setupItemListeners() {
        for (int i = 0; i < PAIRS_PER_ROUND; i++) {
            final int index = i;
            leftItems[i].setOnClickListener(v -> onLeftClicked(index));
            rightItems[i].setOnClickListener(v -> onRightClicked(index));
        }
    }

    private void onLeftClicked(int index) {
        if (leftResolved[index]) return;

        if (selectedLeftIndex != -1 && !leftResolved[selectedLeftIndex]) {
            leftItems[selectedLeftIndex].reset();
        }

        selectedLeftIndex = index;
        leftItems[index].markSelected();
    }

    private void onRightClicked(int index) {
        if (selectedLeftIndex == -1 || rightConnected[index]) return;

        boolean correct = viewModel.checkConnection(selectedLeftIndex, index);

        if (correct) {
            int pairNumber = matchCount + 1;
            int pairColor = pairColors[matchCount % pairColors.length];
            matchCount++;
            leftItems[selectedLeftIndex].markMatched(pairColor, pairNumber);
            rightItems[index].markMatched(pairColor, pairNumber);
            rightConnected[index] = true;
            updateConnectedChip();
        } else {
            // Wrong pairing: lock the chosen performer with red ✕ — it can't be tried again.
            leftItems[selectedLeftIndex].markWrong();
        }

        leftResolved[selectedLeftIndex] = true;
        selectedLeftIndex = -1;

        if (allResolved()) {
            binding.tvTimer.cancel();
            binding.tvTimer.postDelayed(() -> viewModel.finishRound(), 800);
        }
    }

    private boolean allResolved() {
        for (boolean b : leftResolved) if (!b) return false;
        return true;
    }

    private void startRoundTimer() {
        binding.tvTimer.start(30000, () -> viewModel.finishRound());
    }

    private void resetRoundState() {
        selectedLeftIndex = -1;
        matchCount = 0;
        leftResolved = new boolean[PAIRS_PER_ROUND];
        rightConnected = new boolean[PAIRS_PER_ROUND];
        for (SpojItemView item : leftItems) { item.reset(); item.setShown(true); }
        for (SpojItemView item : rightItems) { item.reset(); item.setShown(true); }
        updateConnectedChip();
    }

    private void updateConnectedChip() {
        binding.tvConnected.setText("⚡ Povezano " + matchCount + " / " + PAIRS_PER_ROUND);
    }

    private void showGameFinished() {
        binding.tvTimer.cancel();
        binding.tvTimer.setVisibility(View.GONE);
        binding.tvLegend.setText("Igra završena! Ukupno bodova: " + viewModel.getScore().getValue());
        for (SpojItemView item : leftItems) item.setShown(false);
        for (SpojItemView item : rightItems) item.setShown(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.tvTimer.cancel();
        binding = null;
    }
}
