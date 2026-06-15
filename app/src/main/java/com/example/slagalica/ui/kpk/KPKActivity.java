package com.example.slagalica.ui.kpk;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.ui.BaseActivity;
import com.example.slagalica.ui.lobby.LobbyActivity;
import com.example.slagalica.ui.mojbroj.MojBrojActivity;
import com.example.slagalica.ui.widget.GameTimerView;

public class KPKActivity extends BaseActivity {

    public static final String EXTRA_MATCH_CODE = "match_code";

    private GameTimerView timer;
    private TextView kbkRoundNum, kbkPointsCounter, kbkOpponentCheck, kbkAnswerReveal;
    private TextView[] hintViews = new TextView[7];
    private EditText kbkInputAnswer;
    private Button kbkSendButton;

    private KPKViewModel viewModel;
    private String matchCode;
    private boolean isHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kpkactivity);

        matchCode = getIntent().getStringExtra(EXTRA_MATCH_CODE);
        isHost = getIntent().getBooleanExtra(LobbyActivity.EXTRA_IS_HOST, false);

        setupStatsBar();
        setOpponent("Protivnik");

        viewModel = new ViewModelProvider(this).get(KPKViewModel.class);

        initViews();
        observeViewModel();

        kbkSendButton.setOnClickListener(v -> {
            String answer = kbkInputAnswer.getText().toString().trim();
            if (!answer.isEmpty()) {
                viewModel.submitAnswer(answer);
                kbkInputAnswer.setText("");
            }
        });

        viewModel.init(matchCode, isHost);
    }

    private void initViews() {
        timer = findViewById(R.id.kbkTimer);
        kbkRoundNum = findViewById(R.id.kbkRoundNum);
        kbkPointsCounter = findViewById(R.id.kbkPointsCounter);
        kbkInputAnswer = findViewById(R.id.kbkInputAnswer);
        kbkSendButton = findViewById(R.id.kbkSendButton);
        kbkOpponentCheck = findViewById(R.id.kbkOpponentCheck);
        kbkAnswerReveal = findViewById(R.id.kbkAnswerReveal);

        hintViews[0] = findViewById(R.id.kbkHint1);
        hintViews[1] = findViewById(R.id.kbkHint2);
        hintViews[2] = findViewById(R.id.kbkHint3);
        hintViews[3] = findViewById(R.id.kbkHint4);
        hintViews[4] = findViewById(R.id.kbkHint5);
        hintViews[5] = findViewById(R.id.kbkHint6);
        hintViews[6] = findViewById(R.id.kbkHint7);

        for (int i = 0; i < hintViews.length; i++) {
            hintViews[i].setText("");
            hintViews[i].setAlpha(0.4f);
        }
    }

    private void observeViewModel() {
        viewModel.getAllHints().observe(this, hints -> {
            if (hints == null) return;
            for (int i = 0; i < hintViews.length; i++) {
                if (i < hints.size()) {
                    String hint = hints.get(i);
                    if (!hint.isEmpty()) {
                        hintViews[i].setText(hint);
                        hintViews[i].setAlpha(1.0f);
                    }
                } else {
                    hintViews[i].setText("");
                    hintViews[i].setAlpha(0.4f);
                }
            }
        });

        viewModel.getTimer().observe(this, seconds -> timer.setText(seconds + "s"));
        viewModel.getCurrentScore().observe(this, score -> kbkPointsCounter.setText(String.valueOf(score)));
        viewModel.getRoundInfo().observe(this, round -> kbkRoundNum.setText(round));
        viewModel.getOpponentStatus().observe(this, status -> kbkOpponentCheck.setText(status));

        viewModel.getInputEnabled().observe(this, enabled -> {
            kbkInputAnswer.setEnabled(enabled);
            kbkSendButton.setEnabled(enabled);
            if (!enabled) kbkInputAnswer.setText("");
        });

        viewModel.getAnswerReveal().observe(this, answer -> {
            if (answer == null || answer.isEmpty()) {
                kbkAnswerReveal.setVisibility(View.GONE);
            } else {
                kbkAnswerReveal.setText("Tačan odgovor: " + answer);
                kbkAnswerReveal.setVisibility(View.VISIBLE);
            }
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
    }

    private void showGameFinished() {
        if (matchCode != null) {
            findViewById(android.R.id.content).postDelayed(() -> {
                Intent i = new Intent(this, MojBrojActivity.class);
                i.putExtra(MojBrojActivity.EXTRA_MATCH_CODE, matchCode);
                i.putExtra(LobbyActivity.EXTRA_IS_HOST, isHost);
                startActivity(i);
                finish();
            }, 2000);
        }
    }

    private void showGameOverDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Korak po korak završen!")
                .setMessage("Prelazim na sledeću igru…")
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