package com.example.slagalica.ui.spojnice;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;

import androidx.lifecycle.ViewModelProvider;
import com.example.slagalica.ui.BaseActivity;

import com.example.slagalica.domain.model.SpojnicePair;
import com.example.slagalica.databinding.ActivitySpojniceBinding;

import java.util.List;

public class SpojniceActivity extends BaseActivity {

    private ActivitySpojniceBinding binding;
    private SpojniceViewModel viewModel;
    private CountDownTimer roundTimer;

    private int selectedLeftIndex = -1;
    private boolean[] leftConnected;
    private boolean[] rightConnected;

    private Button[] leftButtons;
    private Button[] rightButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpojniceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
            leftButtons[selectedLeftIndex].setBackgroundColor(Color.parseColor("#272794"));
        }

        selectedLeftIndex = index;
        leftButtons[index].setBackgroundColor(Color.parseColor("#FF9800"));
    }

    private void onRightClicked(int index) {
        if (selectedLeftIndex == -1 || rightConnected[index]) return;

        boolean correct = viewModel.checkConnection(selectedLeftIndex, index);

        if (correct) {
            leftButtons[selectedLeftIndex].setBackgroundColor(Color.parseColor("#4CAF50"));
            rightButtons[index].setBackgroundColor(Color.parseColor("#4CAF50"));
            leftConnected[selectedLeftIndex] = true;
            rightConnected[index] = true;

            if (allConnected()) {
                if (roundTimer != null) roundTimer.cancel();
                binding.tvTimer.postDelayed(() -> viewModel.finishRound(), 800);
            }
        } else {
            leftButtons[selectedLeftIndex].setBackgroundColor(Color.parseColor("#272794"));
        }

        selectedLeftIndex = -1;
    }

    private boolean allConnected() {
        for (boolean b : leftConnected) if (!b) return false;
        return true;
    }

    private void startRoundTimer() {
        if (roundTimer != null) roundTimer.cancel();
        roundTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.tvTimer.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                binding.tvTimer.setText("0");
                viewModel.finishRound();
            }
        }.start();
    }

    private void resetRoundState() {
        selectedLeftIndex = -1;
        leftConnected = new boolean[5];
        rightConnected = new boolean[5];
        int blue = Color.parseColor("#272794");
        for (Button b : leftButtons) { b.setBackgroundColor(blue); b.setVisibility(View.VISIBLE); }
        for (Button b : rightButtons) { b.setBackgroundColor(blue); b.setVisibility(View.VISIBLE); }
    }

    private void showGameFinished() {
        if (roundTimer != null) roundTimer.cancel();
        binding.tvCriterion.setText("Igra završena!\nUkupno bodova: " + viewModel.getScore().getValue());
        binding.tvRound.setVisibility(View.GONE);
        binding.tvTimer.setVisibility(View.GONE);
        for (Button b : leftButtons) b.setVisibility(View.GONE);
        for (Button b : rightButtons) b.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roundTimer != null) roundTimer.cancel();
        binding = null;
    }
}
