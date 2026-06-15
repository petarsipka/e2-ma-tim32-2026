package com.example.slagalica.ui.skocko;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.ui.BaseActivity;
import com.example.slagalica.ui.kpk.KPKActivity;
import com.example.slagalica.ui.lobby.LobbyActivity;
import com.example.slagalica.ui.widget.GameTimerView;

import java.util.List;

public class SkockoActivity extends BaseActivity {

    public static final String EXTRA_MATCH_CODE = "match_code";

    private GameTimerView timer;
    private TextView skRoundNum, skStatus;
    private LinearLayout[] symbolRows = new LinearLayout[6];
    private LinearLayout[] feedbackRows = new LinearLayout[6];
    private LinearLayout skStealRow, skStealFeedback, skAnswerRow;
    private TextView skAnswerLabel;
    private ImageButton skOptionSkocko, skOptionTref, skOptionPik, skOptionSrce, skOptionKaro, skOptionZvezda;
    private TextView skBackspace;
    private Button skSubmit;
    private ImageView[] skStealSlots;
    private View[] skStealFbs;

    private SkockoViewModel viewModel;
    private String matchCode;
    private boolean isHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skocko);

        matchCode = getIntent().getStringExtra(EXTRA_MATCH_CODE);
        isHost = getIntent().getBooleanExtra(LobbyActivity.EXTRA_IS_HOST, false);

        setupStatsBar();
        setOpponent("Protivnik");

        viewModel = new ViewModelProvider(this).get(SkockoViewModel.class);

        initViews();
        setupClickListeners();
        observeViewModel();

        viewModel.init(matchCode, isHost);
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
        skStealSlots = new ImageView[]{
                findViewById(R.id.skStealSlot1),
                findViewById(R.id.skStealSlot2),
                findViewById(R.id.skStealSlot3),
                findViewById(R.id.skStealSlot4)
        };
        skStealFbs = new View[]{
                findViewById(R.id.skStealFb1),
                findViewById(R.id.skStealFb2),
                findViewById(R.id.skStealFb3),
                findViewById(R.id.skStealFb4)
        };
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

        viewModel.getCurrentGuessDisplay().observe(this, this::renderCurrentGuess);
        viewModel.getGuessHistory().observe(this, this::renderGuessHistory);
        viewModel.getAnswerReveal().observe(this, this::renderAnswerReveal);

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

    private void renderCurrentGuess(String[] guess) {
        if (viewModel.isStealPhase()) {
            for (int i = 0; i < 4; i++) {
                if (guess[i] != null && !guess[i].isEmpty()) {
                    skStealSlots[i].setImageResource(symbolToDrawable(guess[i]));
                } else {
                    skStealSlots[i].setImageDrawable(null);
                }
            }
        } else {
            List<SkockoViewModel.GuessResult> history = viewModel.getGuessHistory().getValue();
            int row = (history == null) ? 0 : history.size();
            LinearLayout targetRow = symbolRows[Math.min(row, 5)];
            for (int i = 0; i < 4; i++) {
                ImageView slot = (ImageView) targetRow.getChildAt(i);
                if (guess[i] != null && !guess[i].isEmpty()) {
                    slot.setImageResource(symbolToDrawable(guess[i]));
                } else {
                    slot.setImageDrawable(null);
                }
            }
        }
    }

    private void renderGuessHistory(List<SkockoViewModel.GuessResult> history) {
        // Clear all main rows
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 4; c++) {
                ((ImageView) symbolRows[r].getChildAt(c)).setImageDrawable(null);
            }
            for (int c = 0; c < 4; c++) {
                feedbackRows[r].getChildAt(c).setBackgroundResource(R.drawable.feedback_none);
            }
        }
        // Clear steal row via explicit IDs
        for (int i = 0; i < 4; i++) {
            skStealSlots[i].setImageDrawable(null);
            skStealFbs[i].setBackgroundResource(R.drawable.feedback_none);
        }

        // Show steal row if currently in steal phase OR if history has a steal entry
        boolean showSteal = viewModel.isStealPhase() || (history != null && hasStealEntry(history));
        skStealRow.setVisibility(showSteal ? LinearLayout.VISIBLE : LinearLayout.GONE);
        skStealFeedback.setVisibility(showSteal ? LinearLayout.VISIBLE : LinearLayout.GONE);

        if (history == null) return;
        for (SkockoViewModel.GuessResult result : history) {
            if (result.rowIndex < 6) {
                LinearLayout row = symbolRows[result.rowIndex];
                LinearLayout fbRow = feedbackRows[result.rowIndex];
                for (int i = 0; i < 4; i++) {
                    ((ImageView) row.getChildAt(i)).setImageResource(symbolToDrawable(result.guess[i]));
                }
                int[] feedbackDrawables = buildFeedbackDrawables(result.exact, result.partial);
                for (int i = 0; i < 4; i++) {
                    fbRow.getChildAt(i).setBackgroundResource(feedbackDrawables[i]);
                }
            } else {
                // Steal entry: use explicit IDs
                for (int i = 0; i < 4; i++) {
                    skStealSlots[i].setImageResource(symbolToDrawable(result.guess[i]));
                }
                int[] feedbackDrawables = buildFeedbackDrawables(result.exact, result.partial);
                for (int i = 0; i < 4; i++) {
                    skStealFbs[i].setBackgroundResource(feedbackDrawables[i]);
                }
            }
        }
    }

    private boolean hasStealEntry(List<SkockoViewModel.GuessResult> history) {
        for (SkockoViewModel.GuessResult r : history) {
            if (r.rowIndex >= 6) return true;
        }
        return false;
    }

    private void renderAnswerReveal(String reveal) {
        if (reveal == null || reveal.isEmpty()) {
            skAnswerRow.setVisibility(LinearLayout.INVISIBLE);
            skAnswerLabel.setVisibility(TextView.INVISIBLE);
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

    private void showGameFinished() {
        if (matchCode != null) {
            findViewById(android.R.id.content).postDelayed(() -> {
                Intent i = new Intent(this, KPKActivity.class);
                i.putExtra(KPKActivity.EXTRA_MATCH_CODE, matchCode);
                i.putExtra(LobbyActivity.EXTRA_IS_HOST, isHost);
                startActivity(i);
                finish();
            }, 2000);
        }
    }

    private void showGameOverDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Skočko završen!")
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