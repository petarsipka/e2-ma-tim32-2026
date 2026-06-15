package com.example.slagalica.ui.mojbroj;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.ui.BaseActivity;
import com.example.slagalica.ui.ResultActivity;
import com.example.slagalica.ui.lobby.LobbyActivity;
import com.example.slagalica.ui.widget.GameTimerView;

public class MojBrojActivity extends BaseActivity {

    public static final String EXTRA_MATCH_CODE = "match_code";

    private GameTimerView timer;
    private TextView mbWantedNum, mbPlayer1Answer, mbPlayer2Answer, mbInsertAnswer;
    private TextView[] numberTiles = new TextView[6];
    private TextView mbLParen, mbRParen, mbPlus, mbMinus, mbMultiply, mbDivide;
    private Button mbSendButton;
    private LinearLayout mbAnswerRevealBox;
    private TextView mbAnswerRevealTarget, mbAnswerRevealSolution, mbAnswerRevealP1, mbAnswerRevealP2;
    private TextView mbBackspace;
    private TextView mbRoundNum;

    private MojBrojViewModel viewModel;
    private String matchCode;
    private boolean isHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moj_broj);

        matchCode = getIntent().getStringExtra(EXTRA_MATCH_CODE);
        isHost = getIntent().getBooleanExtra(LobbyActivity.EXTRA_IS_HOST, false);

        setupStatsBar();
        setOpponent("Protivnik");

        viewModel = new ViewModelProvider(this).get(MojBrojViewModel.class);

        initViews();
        setupClickListeners();
        observeViewModel();

        viewModel.init(matchCode, isHost);
    }

    private void initViews() {
        timer = findViewById(R.id.mbTimer);
        mbWantedNum = findViewById(R.id.mbWantedNum);
        mbPlayer1Answer = findViewById(R.id.mbPlayer1Answer);
        mbPlayer2Answer = findViewById(R.id.mbPlayer2Answer);
        mbInsertAnswer = findViewById(R.id.mbInsertAnswer);
        mbSendButton = findViewById(R.id.mbSendButton);

        numberTiles[0] = findViewById(R.id.mbUsedNum1);
        numberTiles[1] = findViewById(R.id.mbUsedNum2);
        numberTiles[2] = findViewById(R.id.mbUsedNum3);
        numberTiles[3] = findViewById(R.id.mbUsedNum4);
        numberTiles[4] = findViewById(R.id.mbUsedNum5);
        numberTiles[5] = findViewById(R.id.mbUsedNum6);

        mbLParen = findViewById(R.id.mbLParen);
        mbRParen = findViewById(R.id.mbRParen);
        mbPlus = findViewById(R.id.mbPlus);
        mbMinus = findViewById(R.id.mbMinus);
        mbMultiply = findViewById(R.id.mbMultiply);
        mbDivide = findViewById(R.id.mbDivide);
        mbAnswerRevealBox = findViewById(R.id.mbAnswerRevealBox);
        mbAnswerRevealTarget = findViewById(R.id.mbAnswerRevealTarget);
        mbAnswerRevealSolution = findViewById(R.id.mbAnswerRevealSolution);
        mbAnswerRevealP1 = findViewById(R.id.mbAnswerRevealP1);
        mbAnswerRevealP2 = findViewById(R.id.mbAnswerRevealP2);
        mbBackspace = findViewById(R.id.mbBackspace);
        mbRoundNum = findViewById(R.id.mbRoundNum);
    }

    private void setupClickListeners() {
        for (int i = 0; i < numberTiles.length; i++) {
            final int idx = i;
            numberTiles[i].setOnClickListener(v -> {
                String text = numberTiles[idx].getText().toString();
                if (!text.isEmpty()) viewModel.appendToExpression(text);
            });
        }

        mbLParen.setOnClickListener(v -> viewModel.appendToExpression("("));
        mbRParen.setOnClickListener(v -> viewModel.appendToExpression(")"));
        mbPlus.setOnClickListener(v -> viewModel.appendToExpression("+"));
        mbMinus.setOnClickListener(v -> viewModel.appendToExpression("-"));
        mbMultiply.setOnClickListener(v -> viewModel.appendToExpression("*"));
        mbDivide.setOnClickListener(v -> viewModel.appendToExpression("/"));

        mbSendButton.setOnClickListener(v -> viewModel.submitExpression());
        mbBackspace.setOnClickListener(v -> viewModel.backspace());
        mbBackspace.setOnLongClickListener(v -> {
            viewModel.clearExpression();
            return true;
        });

        // Prevent accidental clear when tapping the expression bar
        mbInsertAnswer.setOnClickListener(null);
        for (int i = 0; i < numberTiles.length; i++) {
            final int idx = i;
            numberTiles[idx].setOnClickListener(v -> {
                viewModel.appendNumber(idx); // Use index instead of text
            });
        }
    }

    private void observeViewModel() {
        viewModel.getTimer().observe(this, seconds -> timer.setText(seconds + "s"));
        viewModel.getTargetNumber().observe(this, target -> mbWantedNum.setText(String.valueOf(target)));

        viewModel.getAvailableNumbers().observe(this, numbers -> {
            if (numbers == null) return;
            for (int i = 0; i < numberTiles.length && i < numbers.size(); i++) {
                numberTiles[i].setText(String.valueOf(numbers.get(i)));
            }
        });

        viewModel.getCurrentExpression().observe(this, expr -> {
            mbInsertAnswer.setText(expr.isEmpty() ? "Unesite rešenje…" : expr);
        });

        viewModel.getPlayer1AnswerDisplay().observe(this, mbPlayer1Answer::setText);
        viewModel.getPlayer2AnswerDisplay().observe(this, mbPlayer2Answer::setText);
        viewModel.getRoundInfo().observe(this, round -> mbRoundNum.setText(round));

        viewModel.getAnswerReveal().observe(this, reveal -> {
            if (reveal == null || reveal.isEmpty()) {
                mbAnswerRevealBox.setVisibility(View.GONE);
                mbInsertAnswer.setVisibility(View.VISIBLE);
                mbBackspace.setVisibility(View.VISIBLE);
            } else {
                mbAnswerRevealBox.setVisibility(View.VISIBLE);
                mbInsertAnswer.setVisibility(View.GONE);
                mbBackspace.setVisibility(View.GONE);

                String[] lines = reveal.split("\n");
                if (lines.length >= 1) {
                    String[] targetSol = lines[0].split("  \\|  ");
                    if (targetSol.length >= 2) {
                        mbAnswerRevealTarget.setText(targetSol[0].trim());
                        mbAnswerRevealSolution.setText(targetSol[1].trim());
                    }
                }
                if (lines.length >= 2) {
                    String[] scores = lines[1].split("  \\|  ");
                    if (scores.length >= 2) {
                        mbAnswerRevealP1.setText(scores[0].trim());
                        mbAnswerRevealP2.setText(scores[1].trim());
                    }
                }
            }
        });

        viewModel.getInputEnabled().observe(this, enabled -> {
            mbSendButton.setEnabled(enabled);
            for (TextView tv : numberTiles) tv.setEnabled(enabled);
            mbLParen.setEnabled(enabled);
            mbRParen.setEnabled(enabled);
            mbPlus.setEnabled(enabled);
            mbMinus.setEnabled(enabled);
            mbMultiply.setEnabled(enabled);
            mbDivide.setEnabled(enabled);
            mbBackspace.setEnabled(enabled);
        });

        viewModel.getMyTotal().observe(this, total ->
                updateScore(total, safe(viewModel.getOpponentTotal().getValue())));
        viewModel.getOpponentTotal().observe(this, oppTotal ->
                updateScore(safe(viewModel.getMyTotal().getValue()), oppTotal));
        viewModel.getOpponentName().observe(this, this::setOpponent);
        viewModel.getOpponentUid().observe(this, this::setOpponentAvatar);

        viewModel.getGameFinished().observe(this, finished -> {
            if (Boolean.TRUE.equals(finished)) showGameFinished();
        });

        viewModel.getGameOver().observe(this, isOver -> {
            if (Boolean.TRUE.equals(isOver)) showGameOverDialog();
        });
        viewModel.getUsedIndices().observe(this, used -> {
            for (int i = 0; i < numberTiles.length; i++) {
                boolean isUsed = used[i];
                numberTiles[i].setEnabled(!isUsed);
                numberTiles[i].setAlpha(isUsed ? 0.4f : 1.0f); // Grays it out
            }
        });
    }

    private void showGameFinished() {
        if (matchCode != null) {
            findViewById(android.R.id.content).postDelayed(() -> {
                Intent i = new Intent(this, ResultActivity.class);
                i.putExtra(ResultActivity.EXTRA_MATCH_CODE, matchCode);
                startActivity(i);
                finish();
            }, 2000);
        }
    }

    private void showGameOverDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Moj broj završen!")
                .setMessage("Prelazim na rezultate…")
                .setPositiveButton("OK", (d, w) -> d.dismiss())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.cleanup();
        if (timer != null) timer.cancel();
    }

    private static int safe(Integer value) {
        return value != null ? value : 0;
    }
}