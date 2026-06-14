package com.example.slagalica.ui.kpk;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.ui.widget.GameTimerView;

import java.util.List;

public class KPKActivity extends AppCompatActivity {

    private GameTimerView timer;
    private TextView kbkRoundNum, kbkPointsCounter, kbkOpponentCheck, kbkAnswerReveal;
    private TextView[] hintViews = new TextView[7];
    private EditText kbkInputAnswer;
    private Button kbkSendButton;

    private KPKViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kpkactivity);

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

        // Show role picker before starting
        new AlertDialog.Builder(this)
                .setTitle("Debug: Test mode")
                .setMessage("Which player are you testing as?")
                .setCancelable(false)
                .setPositiveButton("Player 1", (dialog, which) -> {
                    viewModel.setLocalPlayerRole(1);
                    viewModel.startGame();
                })
                .setNegativeButton("Player 2", (dialog, which) -> {
                    viewModel.setLocalPlayerRole(2);
                    viewModel.startGame();
                })
                .show();
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

        // Initialize all hints to empty/placeholder
        for (int i = 0; i < hintViews.length; i++) {
            hintViews[i].setText("");
            hintViews[i].setAlpha(0.4f); // Dim unrevealed hints
        }
    }

    private void observeViewModel() {
        // Update all hints at once for better UI control
        viewModel.getAllHints().observe(this, hints -> {
            if (hints == null) return;
            for (int i = 0; i < hintViews.length; i++) {
                if (i < hints.size()) {
                    String hint = hints.get(i);
                    if (!hint.isEmpty()) {
                        hintViews[i].setText(hint);
                        hintViews[i].setAlpha(1.0f); // Fully visible when revealed
                    }
                } else {
                    hintViews[i].setText("");
                    hintViews[i].setAlpha(0.4f); // Dim unrevealed
                }
            }
        });

        viewModel.getTimer().observe(this, seconds -> {
            timer.setText(seconds + "s");
        });

        viewModel.getCurrentScore().observe(this, score -> {
            kbkPointsCounter.setText(String.valueOf(score));
        });

        viewModel.getRoundInfo().observe(this, round -> {
            kbkRoundNum.setText(round);
        });

        viewModel.getOpponentStatus().observe(this, status -> {
            kbkOpponentCheck.setText(status);
        });

        viewModel.getGameOver().observe(this, isOver -> {
            if (isOver) {
                showGameOverDialog();
            }
        });

        // Disable/enable input during the 10s answer reveal pause
        viewModel.getInputEnabled().observe(this, enabled -> {
            kbkInputAnswer.setEnabled(enabled);
            kbkSendButton.setEnabled(enabled);
            if (!enabled) {
                kbkInputAnswer.setText("");
            }
        });
        // Show/hide answer reveal box
        viewModel.getAnswerReveal().observe(this, answer -> {
            if (answer == null || answer.isEmpty()) {
                kbkAnswerReveal.setVisibility(View.GONE);
            } else {
                kbkAnswerReveal.setText("Tačan odgovor: " + answer);
                kbkAnswerReveal.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showGameOverDialog() {
        int p1 = viewModel.getScorePlayer1();
        int p2 = viewModel.getScorePlayer2();

        new AlertDialog.Builder(this)
                .setTitle("Partija završena!")
                .setMessage("Igrač 1: " + p1 + "\nIgrač 2: " + p2)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.cleanup();
        if (timer != null) timer.cancel();
    }
}