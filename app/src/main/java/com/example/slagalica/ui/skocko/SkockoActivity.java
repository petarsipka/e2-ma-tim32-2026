package com.example.slagalica.ui.skocko;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.ui.widget.GameTimerView;

import java.util.List;

public class SkockoActivity extends AppCompatActivity {

    private GameTimerView timer;
    private TextView skRoundNum, skStatus;
    private LinearLayout[] symbolRows = new LinearLayout[6];
    private LinearLayout[] feedbackRows = new LinearLayout[6];
    private LinearLayout skStealRow, skStealFeedback, skAnswerRow;
    private TextView skAnswerLabel;
    private ImageButton skOptionSkocko, skOptionTref, skOptionPik, skOptionSrce, skOptionKaro, skOptionZvezda;
    private TextView skBackspace;
    private Button skSubmit;

    private SkockoViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skocko);

        viewModel = new ViewModelProvider(this).get(SkockoViewModel.class);

        initViews();
        setupClickListeners();
        observeViewModel();

        new AlertDialog.Builder(this)
                .setTitle("Debug: Test mode")
                .setMessage("Which player are you testing as?")
                .setCancelable(false)
                .setPositiveButton("Player 1", (d, w) -> {
                    viewModel.setLocalPlayerRole(1);
                    viewModel.startGame();
                })
                .setNegativeButton("Player 2", (d, w) -> {
                    viewModel.setLocalPlayerRole(2);
                    viewModel.startGame();
                })
                .show();
    }

    private void initViews() {
        timer = findViewById(R.id.skTimer);
        skRoundNum = findViewById(R.id.skRoundNum);
        skStatus = findViewById(R.id.skStatus);

        symbolRows[0] = findViewById(R.id.skSymbolRow1);
        symbolRows[1] = findViewById(R.id.skSymbolRow2);
        symbolRows[2] = findViewById(R.id.skSymbolRow3);
        symbolRows[3] = findViewById(R.id.skSymbolRow4);
        symbolRows[4] = findViewById(R.id.skSymbolRow5);
        symbolRows[5] = findViewById(R.id.skSymbolRow6);

        feedbackRows[0] = findViewById(R.id.skFeedback1);
        feedbackRows[1] = findViewById(R.id.skFeedback2);
        feedbackRows[2] = findViewById(R.id.skFeedback3);
        feedbackRows[3] = findViewById(R.id.skFeedback4);
        feedbackRows[4] = findViewById(R.id.skFeedback5);
        feedbackRows[5] = findViewById(R.id.skFeedback6);

        skStealRow = findViewById(R.id.skStealRow);
        skStealFeedback = findViewById(R.id.skStealFeedback);
        skAnswerRow = findViewById(R.id.skAnswerRow);
        skAnswerLabel = findViewById(R.id.skAnswerLabel);

        skOptionSkocko = findViewById(R.id.skOptionSkocko);
        skOptionTref = findViewById(R.id.skOptionTref);
        skOptionPik = findViewById(R.id.skOptionPik);
        skOptionSrce = findViewById(R.id.skOptionSrce);
        skOptionKaro = findViewById(R.id.skOptionKaro);
        skOptionZvezda = findViewById(R.id.skOptionZvezda);

        skBackspace = findViewById(R.id.skBackspace);
        skSubmit = findViewById(R.id.skSubmit);
    }

    private void setupClickListeners() {
        skOptionSkocko.setOnClickListener(v -> viewModel.addSymbol("S"));
        skOptionTref.setOnClickListener(v -> viewModel.addSymbol("T"));
        skOptionPik.setOnClickListener(v -> viewModel.addSymbol("P"));
        skOptionSrce.setOnClickListener(v -> viewModel.addSymbol("R"));
        skOptionKaro.setOnClickListener(v -> viewModel.addSymbol("K"));
        skOptionZvezda.setOnClickListener(v -> viewModel.addSymbol("Z"));

        skBackspace.setOnClickListener(v -> viewModel.backspace());
        skBackspace.setOnLongClickListener(v -> {
            viewModel.clearGuess();
            return true;
        });

        skSubmit.setOnClickListener(v -> viewModel.submitGuess());
    }

    private void observeViewModel() {
        viewModel.getTimer().observe(this, seconds -> timer.setText(seconds + "s"));
        viewModel.getRoundInfo().observe(this, skRoundNum::setText);
        viewModel.getStatus().observe(this, skStatus::setText);

        viewModel.getCurrentGuessDisplay().observe(this, guess -> {
            renderCurrentGuess(guess);
        });

        viewModel.getGuessHistory().observe(this, history -> {
            renderGuessHistory(history);
        });

        viewModel.getAnswerReveal().observe(this, reveal -> {
            if (reveal == null || reveal.isEmpty()) {
                skAnswerRow.setVisibility(LinearLayout.INVISIBLE);
                skAnswerLabel.setVisibility(TextView.INVISIBLE);
                // Clear answer row
                for (int i = 0; i < 4; i++) {
                    ((ImageView) skAnswerRow.getChildAt(i)).setImageDrawable(null);
                }
            } else {
                skAnswerRow.setVisibility(LinearLayout.VISIBLE);
                skAnswerLabel.setVisibility(TextView.VISIBLE);
                for (int i = 0; i < 4 && i < reveal.length(); i++) {
                    ImageView slot = (ImageView) skAnswerRow.getChildAt(i);
                    slot.setImageResource(symbolToDrawable(String.valueOf(reveal.charAt(i))));
                }
            }
        });

        viewModel.getInputEnabled().observe(this, enabled -> {
            skOptionSkocko.setEnabled(enabled);
            skOptionTref.setEnabled(enabled);
            skOptionPik.setEnabled(enabled);
            skOptionSrce.setEnabled(enabled);
            skOptionKaro.setEnabled(enabled);
            skOptionZvezda.setEnabled(enabled);
            skBackspace.setEnabled(enabled);
            skSubmit.setEnabled(enabled);
        });

        viewModel.getGameOver().observe(this, isOver -> {
            if (isOver) showGameOverDialog();
        });
    }

    private void renderCurrentGuess(String[] guess) {
        LinearLayout targetRow;

        if (viewModel.isStealPhase()) {
            targetRow = skStealRow;
        } else {
            List<SkockoViewModel.GuessResult> history = viewModel.getGuessHistory().getValue();
            int row = (history == null) ? 0 : history.size();
            targetRow = symbolRows[row];
        }

        for (int i = 0; i < 4; i++) {
            ImageView slot = (ImageView) targetRow.getChildAt(i);
            if (guess[i] != null && !guess[i].isEmpty()) {
                slot.setImageResource(symbolToDrawable(guess[i]));
            } else {
                slot.setImageDrawable(null);
            }
        }
    }

    private void renderGuessHistory(List<SkockoViewModel.GuessResult> history) {
        // Clear all rows first
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 4; c++) {
                ((ImageView) symbolRows[r].getChildAt(c)).setImageDrawable(null);
            }
            for (int c = 0; c < 4; c++) {
                feedbackRows[r].getChildAt(c).setBackgroundResource(R.drawable.feedback_none);
            }
        }
        for (int c = 0; c < 4; c++) {
            ((ImageView) skStealRow.getChildAt(c)).setImageDrawable(null);
        }
        for (int c = 0; c < 4; c++) {
            skStealFeedback.getChildAt(c).setBackgroundResource(R.drawable.feedback_none);
        }

        // Render history
        for (SkockoViewModel.GuessResult result : history) {
            LinearLayout row;
            LinearLayout fbRow;

            if (result.rowIndex < 6) {
                row = symbolRows[result.rowIndex];
                fbRow = feedbackRows[result.rowIndex];
            } else {
                row = skStealRow;
                fbRow = skStealFeedback;
                skStealRow.setVisibility(LinearLayout.VISIBLE);
                skStealFeedback.setVisibility(LinearLayout.VISIBLE);
            }

            for (int i = 0; i < 4; i++) {
                ((ImageView) row.getChildAt(i)).setImageResource(symbolToDrawable(result.guess[i]));
            }

            int[] feedbackDrawables = buildFeedbackDrawables(result.exact, result.partial);
            for (int i = 0; i < 4; i++) {
                fbRow.getChildAt(i).setBackgroundResource(feedbackDrawables[i]);
            }
        }
    }

    private int[] buildFeedbackDrawables(int exact, int partial) {
        int[] drawables = new int[4];
        int idx = 0;
        for (int i = 0; i < exact && idx < 4; i++) drawables[idx++] = R.drawable.feedback_exact;
        for (int i = 0; i < partial && idx < 4; i++) drawables[idx++] = R.drawable.feedback_partial;
        for (int i = idx; i < 4; i++) drawables[i] = R.drawable.feedback_none;
        return drawables;
    }

    private int symbolToDrawable(String symbol) {
        switch (symbol) {
            case "S": return R.drawable.skocko;
            case "T": return R.drawable.tref;
            case "P": return R.drawable.pik;
            case "R": return R.drawable.srce;
            case "K": return R.drawable.karo;
            case "Z": return R.drawable.zvezda;
            default: return R.drawable.skocko_symbol_slot;
        }
    }

    private void showGameOverDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Partija završena!")
                .setMessage("Igrač 1: " + viewModel.getScorePlayer1() + "\nIgrač 2: " + viewModel.getScorePlayer2())
                .setPositiveButton("OK", (d, w) -> finish())
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