package com.example.slagalica.ui.koznazna;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;

import androidx.lifecycle.ViewModelProvider;
import com.example.slagalica.ui.BaseActivity;

import com.example.slagalica.domain.model.Question;
import com.example.slagalica.databinding.ActivityKoZnaZnaBinding;

public class KoZnaZnaActivity extends BaseActivity {

    private ActivityKoZnaZnaBinding binding;
    private KoZnaZnaViewModel viewModel;
    private CountDownTimer questionTimer;
    private boolean answered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityKoZnaZnaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupStatsBar();
        viewModel = new ViewModelProvider(this).get(KoZnaZnaViewModel.class);

        setupObservers();
        setupAnswerButtons();
        viewModel.loadQuestions();
    }

    private void setupObservers() {
        viewModel.getCurrentQuestion().observe(this, question -> {
            answered = false;
            binding.tvQuestion.setText(question.getText());
            String[] answers = question.getAnswers();
            binding.btnA.setText("A.  " + answers[0]);
            binding.btnB.setText("B.  " + answers[1]);
            binding.btnC.setText("C.  " + answers[2]);
            binding.btnD.setText("D.  " + answers[3]);
            resetButtons();
            startQuestionTimer();
        });

        viewModel.getScore().observe(this, score ->
            binding.tvScore.setText("Bodovi: " + score)
        );

        viewModel.getQuestionIndex().observe(this, index ->
            binding.tvQuestionNumber.setText("Pitanje " + (index + 1) + " / " + viewModel.getTotalQuestions())
        );

        viewModel.getGameFinished().observe(this, finished -> {
            if (Boolean.TRUE.equals(finished)) showGameFinished();
        });
    }

    private void setupAnswerButtons() {
        binding.btnA.setOnClickListener(v -> onAnswerSelected(0));
        binding.btnB.setOnClickListener(v -> onAnswerSelected(1));
        binding.btnC.setOnClickListener(v -> onAnswerSelected(2));
        binding.btnD.setOnClickListener(v -> onAnswerSelected(3));
        binding.btnSkip.setOnClickListener(v -> onSkip());
    }

    private void onAnswerSelected(int index) {
        if (answered) return;
        answered = true;
        if (questionTimer != null) questionTimer.cancel();

        Question question = viewModel.getCurrentQuestion().getValue();
        if (question == null) return;

        boolean correct = viewModel.checkAnswer(index);
        int correctIndex = question.getCorrectIndex();

        getButtonAt(index).setBackgroundColor(Color.parseColor("#F44336"));
        getButtonAt(correctIndex).setBackgroundColor(Color.parseColor("#4CAF50"));

        binding.tvTimer.setText("0");
        binding.tvTimer.postDelayed(() -> viewModel.nextQuestion(), 1500);
    }

    private void onSkip() {
        if (answered) return;
        answered = true;
        if (questionTimer != null) questionTimer.cancel();
        binding.tvTimer.setText("0");
        viewModel.nextQuestion();
    }

    private void startQuestionTimer() {
        if (questionTimer != null) questionTimer.cancel();
        questionTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.tvTimer.setText(String.valueOf(millisUntilFinished / 1000 + 1));
            }

            @Override
            public void onFinish() {
                binding.tvTimer.setText("0");
                if (!answered) {
                    answered = true;
                    binding.tvTimer.postDelayed(() -> viewModel.nextQuestion(), 500);
                }
            }
        }.start();
    }

    private void showGameFinished() {
        if (questionTimer != null) questionTimer.cancel();
        binding.tvQuestion.setText("Igra završena!\nUkupno bodova: " + viewModel.getScore().getValue());
        binding.tvQuestionNumber.setVisibility(View.GONE);
        binding.tvTimer.setVisibility(View.GONE);
        binding.btnA.setVisibility(View.GONE);
        binding.btnB.setVisibility(View.GONE);
        binding.btnC.setVisibility(View.GONE);
        binding.btnD.setVisibility(View.GONE);
        binding.btnSkip.setVisibility(View.GONE);
    }

    private void resetButtons() {
        int blue = Color.parseColor("#272794");
        binding.btnA.setBackgroundColor(blue);
        binding.btnB.setBackgroundColor(blue);
        binding.btnC.setBackgroundColor(blue);
        binding.btnD.setBackgroundColor(blue);
        binding.btnA.setEnabled(true);
        binding.btnB.setEnabled(true);
        binding.btnC.setEnabled(true);
        binding.btnD.setEnabled(true);
    }

    private Button getButtonAt(int index) {
        switch (index) {
            case 0: return binding.btnA;
            case 1: return binding.btnB;
            case 2: return binding.btnC;
            default: return binding.btnD;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (questionTimer != null) questionTimer.cancel();
        binding = null;
    }
}
