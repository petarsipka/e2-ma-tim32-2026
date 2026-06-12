package com.example.slagalica.ui.koznazna;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.slagalica.ui.BaseActivity;
import com.example.slagalica.ui.lobby.LobbyActivity;
import com.example.slagalica.ui.spojnice.SpojniceActivity;

import com.example.slagalica.R;
import com.example.slagalica.data.model.Question;
import com.example.slagalica.databinding.ActivityKoZnaZnaBinding;

public class KoZnaZnaActivity extends BaseActivity {

    /** Intent extra: the match code shared by both devices (null = single-device). */
    public static final String EXTRA_MATCH_CODE = "match_code";

    private ActivityKoZnaZnaBinding binding;
    private KoZnaZnaViewModel viewModel;
    private boolean answered;
    private String matchCode;
    private boolean isHost;

    private LinearLayout[] rows;
    private TextView[] keys;
    private TextView[] texts;
    private TextView[] marks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityKoZnaZnaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupStatsBar();
        setOpponent("Protivnik");
        viewModel = new ViewModelProvider(this).get(KoZnaZnaViewModel.class);

        rows = new LinearLayout[]{binding.ansA, binding.ansB, binding.ansC, binding.ansD};
        keys = new TextView[]{binding.keyA, binding.keyB, binding.keyC, binding.keyD};
        texts = new TextView[]{binding.txtA, binding.txtB, binding.txtC, binding.txtD};
        marks = new TextView[]{binding.markA, binding.markB, binding.markC, binding.markD};

        matchCode = getIntent().getStringExtra(EXTRA_MATCH_CODE);
        isHost = getIntent().getBooleanExtra(LobbyActivity.EXTRA_IS_HOST, false);

        setupObservers();
        setupAnswerButtons();
        viewModel.loadQuestions();
        viewModel.init(matchCode);
    }

    private void setupObservers() {
        viewModel.getCurrentQuestion().observe(this, question -> {
            answered = false;
            binding.tvQuestion.setText(question.getText());
            String[] answers = question.getAnswers();
            for (int i = 0; i < 4; i++) {
                texts[i].setText(answers[i]);
            }
            resetAnswers();
            buildDots();
            startQuestionTimer();
        });

        // Live scoreboard shows cumulative match totals, not just this game.
        viewModel.getMyTotal().observe(this, total ->
            updateScore(total, safe(viewModel.getOpponentTotal().getValue()))
        );
        viewModel.getOpponentTotal().observe(this, oppTotal ->
            updateScore(safe(viewModel.getMyTotal().getValue()), oppTotal)
        );

        viewModel.getOpponentName().observe(this, this::setOpponent);

        viewModel.getQuestionIndex().observe(this, index -> {
            binding.tvQuestionNumber.setText(
                "Pitanje " + (index + 1) + " / " + viewModel.getTotalQuestions()
                    + " · tačno +10, netačno −5");
            updateDots(index);
        });

        viewModel.getGameFinished().observe(this, finished -> {
            if (Boolean.TRUE.equals(finished)) showGameFinished();
        });
    }

    private void setupAnswerButtons() {
        for (int i = 0; i < 4; i++) {
            final int index = i;
            rows[i].setOnClickListener(v -> onAnswerSelected(index));
        }
        binding.btnSkip.setOnClickListener(v -> onSkip());
    }

    private void onAnswerSelected(int index) {
        if (answered) return;
        answered = true;
        binding.tvTimer.cancel();

        Question question = viewModel.getCurrentQuestion().getValue();
        if (question == null) return;

        boolean correct = viewModel.checkAnswer(index);
        int correctIndex = question.getCorrectIndex();

        styleRow(correctIndex, true);
        if (!correct) styleRow(index, false);

        for (LinearLayout row : rows) row.setClickable(false);

        binding.tvTimer.cancel();
        binding.tvTimer.showZero();
        binding.tvTimer.postDelayed(() -> viewModel.nextQuestion(), 1500);
    }

    private void onSkip() {
        if (answered) return;
        answered = true;
        binding.tvTimer.cancel();
        binding.tvTimer.showZero();
        viewModel.nextQuestion();
    }

    private void startQuestionTimer() {
        binding.tvTimer.start(5000, () -> {
            if (!answered) {
                answered = true;
                binding.tvTimer.postDelayed(() -> viewModel.nextQuestion(), 500);
            }
        });
    }

    private void showGameFinished() {
        binding.tvTimer.cancel();
        binding.tvQuestion.setText("Ko zna zna završeno!\nBodovi: " + viewModel.getScore().getValue());
        binding.tvQuestionNumber.setVisibility(View.GONE);
        binding.tvTimer.setVisibility(View.GONE);
        binding.qDots.setVisibility(View.GONE);
        for (LinearLayout row : rows) row.setVisibility(View.GONE);
        binding.btnSkip.setVisibility(View.GONE);

        // Chain to next game (Spojnice).
        if (matchCode != null) {
            binding.getRoot().postDelayed(() -> {
                Intent intent = new Intent(this, SpojniceActivity.class);
                intent.putExtra(SpojniceActivity.EXTRA_MATCH_CODE, matchCode);
                intent.putExtra(LobbyActivity.EXTRA_IS_HOST, isHost);
                startActivity(intent);
                finish();
            }, 2000);
        }
    }

    /** Reset all answer rows to the neutral (.ans) look. */
    private void resetAnswers() {
        int muted = ContextCompat.getColor(this, R.color.muted);
        int text = ContextCompat.getColor(this, R.color.text);
        for (int i = 0; i < 4; i++) {
            rows[i].setBackgroundResource(R.drawable.bg_answer);
            rows[i].setClickable(true);
            keys[i].setBackgroundResource(R.drawable.bg_answer_key);
            keys[i].setTextColor(muted);
            texts[i].setTextColor(text);
            marks[i].setText("");
        }
    }

    /** Apply the correct (.ans.correct) or wrong (.ans.wrong) feedback to a row. */
    private void styleRow(int index, boolean correct) {
        int white = ContextCompat.getColor(this, R.color.white);
        int color = ContextCompat.getColor(this, correct ? R.color.correct : R.color.wrong);
        rows[index].setBackgroundResource(correct ? R.drawable.bg_answer_correct : R.drawable.bg_answer_wrong);
        keys[index].setBackgroundResource(correct ? R.drawable.bg_answer_key_correct : R.drawable.bg_answer_key_wrong);
        keys[index].setTextColor(white);
        texts[index].setTextColor(color);
        marks[index].setText(correct ? "✓" : "✕");
        marks[index].setTextColor(color);
    }

    /** Build the question-progress dots to match the question count. */
    private void buildDots() {
        int total = viewModel.getTotalQuestions();
        if (binding.qDots.getChildCount() == total) return;
        binding.qDots.removeAllViews();
        int size = dp(9);
        int gap = dp(4);
        for (int i = 0; i < total; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginStart(i == 0 ? 0 : gap);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.dot_todo);
            binding.qDots.addView(dot);
        }
        Integer index = viewModel.getQuestionIndex().getValue();
        updateDots(index != null ? index : 0);
    }

    /** Colour dots up to and including the current question as done/current. */
    private void updateDots(int currentIndex) {
        for (int i = 0; i < binding.qDots.getChildCount(); i++) {
            binding.qDots.getChildAt(i).setBackgroundResource(
                i <= currentIndex ? R.drawable.dot_done : R.drawable.dot_todo);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int safe(Integer value) {
        return value != null ? value : 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.tvTimer.cancel();
        binding = null;
    }
}
