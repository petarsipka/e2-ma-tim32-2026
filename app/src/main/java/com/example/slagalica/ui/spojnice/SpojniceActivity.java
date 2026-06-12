package com.example.slagalica.ui.spojnice;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.slagalica.ui.BaseActivity;

import com.example.slagalica.R;
import com.example.slagalica.data.model.SpojnicePair;
import com.example.slagalica.databinding.ActivitySpojniceBinding;

import java.util.List;

public class SpojniceActivity extends BaseActivity {

    private ActivitySpojniceBinding binding;
    private SpojniceViewModel viewModel;

    private int selectedLeftIndex = -1;
    private boolean[] leftConnected;
    private boolean[] rightConnected;

    // Sunny pop palette: each matched pair gets its own colour (no lines).
    // Resolved from design tokens in res/values/colors.xml.
    private int BASE;     // accent
    private int SELECT;   // accent2
    private int[] pairColors;  // g0, g1, g2, g3, accent-end
    private int matchCount;

    private void resolveColors() {
        BASE = ContextCompat.getColor(this, R.color.accent);
        SELECT = ContextCompat.getColor(this, R.color.accent2);
        pairColors = new int[]{
                ContextCompat.getColor(this, R.color.g0),
                ContextCompat.getColor(this, R.color.g1),
                ContextCompat.getColor(this, R.color.g2),
                ContextCompat.getColor(this, R.color.g3),
                ContextCompat.getColor(this, R.color.accent_end)
        };
    }

    private Button[] leftButtons;
    private Button[] rightButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpojniceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        resolveColors();
        setupStatsBar();
        leftButtons = new Button[]{binding.btnL1, binding.btnL2, binding.btnL3, binding.btnL4, binding.btnL5};
        rightButtons = new Button[]{binding.btnR1, binding.btnR2, binding.btnR3, binding.btnR4, binding.btnR5};

        viewModel = new ViewModelProvider(this).get(SpojniceViewModel.class);

        setupObservers();
        setupButtonListeners();
        viewModel.loadRound(1);
    }

    private void setupObservers() {
        viewModel.getCurrentPairs().observe(this, pairs -> {
            resetRoundState();
            populateColumns(pairs);
            startRoundTimer();
        });

        viewModel.getScore().observe(this, score ->
            binding.tvScore.setText("Bodovi: " + score)
        );

        viewModel.getCurrentRound().observe(this, round ->
            binding.tvRound.setText("Runda " + round + " / " + viewModel.getTotalRounds())
        );

        viewModel.getGameFinished().observe(this, finished -> {
            if (Boolean.TRUE.equals(finished)) showGameFinished();
        });
    }

    private void populateColumns(SpojnicePair pairs) {
        binding.tvCriterion.setText(pairs.getCriterion());
        List<String> left = pairs.getLeftTerms();
        List<String> right = pairs.getRightTerms();
        for (int i = 0; i < 5; i++) {
            leftButtons[i].setText(left.get(i));
            rightButtons[i].setText(right.get(i));
        }
    }

    private void setupButtonListeners() {
        for (int i = 0; i < 5; i++) {
            final int index = i;
            leftButtons[i].setOnClickListener(v -> onLeftClicked(index));
            rightButtons[i].setOnClickListener(v -> onRightClicked(index));
        }
    }

    private void onLeftClicked(int index) {
        if (leftConnected[index]) return;

        if (selectedLeftIndex != -1 && !leftConnected[selectedLeftIndex]) {
            tint(leftButtons[selectedLeftIndex], BASE);
        }

        selectedLeftIndex = index;
        tint(leftButtons[index], SELECT);
    }

    private void onRightClicked(int index) {
        if (selectedLeftIndex == -1 || rightConnected[index]) return;

        boolean correct = viewModel.checkConnection(selectedLeftIndex, index);

        if (correct) {
            int pairColor = pairColors[matchCount % pairColors.length];
            matchCount++;
            tint(leftButtons[selectedLeftIndex], pairColor);
            tint(rightButtons[index], pairColor);
            leftConnected[selectedLeftIndex] = true;
            rightConnected[index] = true;

            if (allConnected()) {
                binding.tvTimer.cancel();
                binding.tvTimer.postDelayed(() -> viewModel.finishRound(), 800);
            }
        } else {
            tint(leftButtons[selectedLeftIndex], BASE);
        }

        selectedLeftIndex = -1;
    }

    private boolean allConnected() {
        for (boolean b : leftConnected) if (!b) return false;
        return true;
    }

    private void startRoundTimer() {
        binding.tvTimer.start(30000, () -> viewModel.finishRound());
    }

    private void resetRoundState() {
        selectedLeftIndex = -1;
        matchCount = 0;
        leftConnected = new boolean[5];
        rightConnected = new boolean[5];
        for (Button b : leftButtons) { tint(b, BASE); b.setVisibility(View.VISIBLE); }
        for (Button b : rightButtons) { tint(b, BASE); b.setVisibility(View.VISIBLE); }
    }

    private void tint(Button b, int color) {
        b.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private void showGameFinished() {
        binding.tvTimer.cancel();
        binding.tvCriterion.setText("Igra završena!\nUkupno bodova: " + viewModel.getScore().getValue());
        binding.tvRound.setVisibility(View.GONE);
        binding.tvTimer.setVisibility(View.GONE);
        for (Button b : leftButtons) b.setVisibility(View.GONE);
        for (Button b : rightButtons) b.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.tvTimer.cancel();
        binding = null;
    }
}
