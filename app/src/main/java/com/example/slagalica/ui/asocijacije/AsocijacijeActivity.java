package com.example.slagalica.ui.asocijacije;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.data.model.Asocijacija;
import com.example.slagalica.databinding.ActivityAsocijacijeBinding;
import com.example.slagalica.ui.BaseActivity;

public class AsocijacijeActivity extends BaseActivity {

    private static final int TIMER_DURATION = 120000;
    /** Placeholder opponent until live matchmaking exists (mirrors the design mock). */
    private static final int DEMO_OPPONENT_SCORE = 12;
    private static final String[] COL_LETTER = {"A", "B", "C", "D"};

    private ActivityAsocijacijeBinding binding;
    private AsocijacijeViewModel viewModel;

    private final TextView[][] fieldCells = new TextView[4][4];
    private final TextView[] solCells = new TextView[4];

    // Sunny pop palette — resolved from res/values/colors.xml (design tokens).
    private int MUTED, FAINT, TEXT, WHITE;
    private int[] colColors; // per-column solved colour: g0..g3

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAsocijacijeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        resolveColors();
        setupStatsBar();
        setOpponent("Jelena_K");
        viewModel = new ViewModelProvider(this).get(AsocijacijeViewModel.class);

        bindViews();
        setupObservers();
        setupListeners();

        viewModel.loadRound(1);
    }

    private void resolveColors() {
        MUTED = ContextCompat.getColor(this, R.color.muted);
        FAINT = ContextCompat.getColor(this, R.color.faint);
        TEXT = ContextCompat.getColor(this, R.color.text);
        WHITE = ContextCompat.getColor(this, R.color.white);
        colColors = new int[]{
                ContextCompat.getColor(this, R.color.g0),
                ContextCompat.getColor(this, R.color.g1),
                ContextCompat.getColor(this, R.color.g2),
                ContextCompat.getColor(this, R.color.g3)
        };
    }

    private void bindViews() {
        fieldCells[0] = new TextView[]{binding.btnC1F1, binding.btnC1F2, binding.btnC1F3, binding.btnC1F4};
        fieldCells[1] = new TextView[]{binding.btnC2F1, binding.btnC2F2, binding.btnC2F3, binding.btnC2F4};
        fieldCells[2] = new TextView[]{binding.btnC3F1, binding.btnC3F2, binding.btnC3F3, binding.btnC3F4};
        fieldCells[3] = new TextView[]{binding.btnC4F1, binding.btnC4F2, binding.btnC4F3, binding.btnC4F4};

        solCells[0] = binding.btnSolC1;
        solCells[1] = binding.btnSolC2;
        solCells[2] = binding.btnSolC3;
        solCells[3] = binding.btnSolC4;
    }

    private void setupObservers() {
        viewModel.getCurrentRoundData().observe(this, this::applyRoundData);

        viewModel.getScore().observe(this, score -> {
            updateScore(score, DEMO_OPPONENT_SCORE);
            binding.tvRoundPoints.setText("+" + score + " bodova");
        });

        viewModel.getGameFinished().observe(this, finished -> {
            if (Boolean.TRUE.equals(finished)) {
                cancelTimer();
                binding.tvTimer.showZero();
                Toast.makeText(this,
                        "Igra završena! Ukupno: " + viewModel.getScore().getValue(),
                        Toast.LENGTH_LONG).show();
                disableAllInput();
            }
        });
    }

    private void applyRoundData(Asocijacija data) {
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                styleClosedField(fieldCells[c][r], COL_LETTER[c] + (r + 1));
            }
            styleClosedSol(solCells[c], COL_LETTER[c]);
        }
        styleFinalBanner(maskSolution(data.getFinalSolution()), false);
        updateSolvedChip();

        cancelTimer();
        startTimer(TIMER_DURATION);
    }

    private void setupListeners() {
        for (int c = 0; c < 4; c++) {
            final int col = c;
            for (int r = 0; r < 4; r++) {
                final int row = r;
                fieldCells[c][r].setOnClickListener(v -> onFieldClicked(col, row));
            }
            solCells[c].setOnClickListener(v -> onSolutionClicked(col));
        }
        binding.btnFinalSolution.setOnClickListener(v -> onFinalClicked());
    }

    private void onFieldClicked(int col, int row) {
        if (viewModel.isColumnSolved(col) || viewModel.isFieldRevealed(col, row)) return;

        Asocijacija data = viewModel.getCurrentRoundData().getValue();
        if (data == null) return;

        viewModel.revealField(col, row);
        styleOpenField(fieldCells[col][row], data.getColumnFields()[col][row]);
    }

    private void onSolutionClicked(int col) {
        if (viewModel.isColumnSolved(col)) return;
        showGuessDialog("Rešenje za kolonu " + COL_LETTER[col], guess -> onGuessColumn(col, guess));
    }

    private void onGuessColumn(int col, String guess) {
        int points = viewModel.guessColumn(col, guess);
        if (points >= 0) {
            Asocijacija data = viewModel.getCurrentRoundData().getValue();
            styleSolvedSol(solCells[col], data.getColumnSolutions()[col], colColors[col]);
            revealRemainingFields(col, data);
            updateSolvedChip();
        } else {
            Toast.makeText(this, "Netačno", Toast.LENGTH_SHORT).show();
        }
    }

    private void onFinalClicked() {
        if (viewModel.isFinalSolved()) return;
        showGuessDialog("Konačno rešenje", this::onGuessFinal);
    }

    private void onGuessFinal(String guess) {
        int points = viewModel.guessFinal(guess);
        if (points >= 0) {
            Asocijacija data = viewModel.getCurrentRoundData().getValue();
            styleFinalBanner(data.getFinalSolution().toUpperCase(), true);
            revealEverything(data);
            Toast.makeText(this, "Bravo! +" + points + " bodova", Toast.LENGTH_SHORT).show();
            binding.getRoot().postDelayed(() -> viewModel.advanceRound(), 10000);
        } else {
            Toast.makeText(this, "Netačno", Toast.LENGTH_SHORT).show();
        }
    }

    /** Final solution solved: open every field and colour every column solution. */
    private void revealEverything(Asocijacija data) {
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                styleOpenField(fieldCells[c][r], data.getColumnFields()[c][r]);
            }
            styleSolvedSol(solCells[c], data.getColumnSolutions()[c], colColors[c]);
        }
        updateSolvedChip();
    }

    private void revealRemainingFields(int col, Asocijacija data) {
        for (int r = 0; r < 4; r++) {
            if (!viewModel.isFieldRevealed(col, r)) {
                styleOpenField(fieldCells[col][r], data.getColumnFields()[col][r]);
            }
        }
    }

    // --- Cell styling (Sunny pop .asoc-cell / .asoc-sol) ---

    private void styleClosedField(TextView cell, String placeholder) {
        cell.setBackgroundResource(R.drawable.bg_asoc_cell_closed);
        cell.setBackgroundTintList(null);
        cell.setTypeface(ResourcesCompat.getFont(this, R.font.baloo2), android.graphics.Typeface.BOLD);
        cell.setTextColor(MUTED);
        cell.setTextSize(13);
        cell.setText(placeholder);
        cell.setEnabled(true);
    }

    private void styleOpenField(TextView cell, String value) {
        cell.setBackgroundResource(R.drawable.bg_asoc_cell_open);
        cell.setBackgroundTintList(null);
        cell.setTypeface(ResourcesCompat.getFont(this, R.font.nunito), android.graphics.Typeface.BOLD);
        cell.setTextColor(TEXT);
        cell.setTextSize(12);
        cell.setText(value);
        cell.setEnabled(false);
    }

    private void styleClosedSol(TextView cell, String letter) {
        cell.setBackgroundResource(R.drawable.bg_asoc_cell_closed);
        cell.setBackgroundTintList(null);
        cell.setTextColor(FAINT);
        cell.setText(letter);
        cell.setEnabled(true);
    }

    private void styleSolvedSol(TextView cell, String solution, int color) {
        cell.setBackgroundResource(R.drawable.bg_asoc_sol_solved);
        cell.setBackgroundTintList(ColorStateList.valueOf(color));
        cell.setTextColor(WHITE);
        cell.setText(solution.toUpperCase());
        cell.setEnabled(false);
    }

    private void styleFinalBanner(String value, boolean solved) {
        binding.tvFinalValue.setText(value);
        binding.btnFinalSolution.setClickable(!solved);
    }

    /** Fully masked, no letter hints: "Sloboda" -> "_ _ _ _ _ _ _" (spaces kept). */
    private static String maskSolution(String solution) {
        if (solution == null || solution.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < solution.length(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(solution.charAt(i) == ' ' ? ' ' : '_');
        }
        return sb.toString();
    }

    private void showGuessDialog(String title, java.util.function.Consumer<String> onConfirm) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setHint("Ukucaj rešenje…");
        input.setSingleLine(true);

        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(container)
                .setPositiveButton("Potvrdi", (d, w) -> onConfirm.accept(input.getText().toString()))
                .setNegativeButton("Odustani", null)
                .show();
    }

    private void updateSolvedChip() {
        int solved = 0;
        for (int c = 0; c < 4; c++) if (viewModel.isColumnSolved(c)) solved++;
        binding.tvSolvedColumns.setText("⚡ " + solvedColumnsLabel(solved));
    }

    /** Serbian agreement for the solved-columns chip. */
    private static String solvedColumnsLabel(int n) {
        if (n == 1) return "1 kolona rešena";
        if (n >= 2 && n <= 4) return n + " kolone rešene";
        return n + " kolona rešeno";
    }

    private void startTimer(long durationMs) {
        binding.tvTimer.start(durationMs, () -> viewModel.advanceRound());
    }

    private void cancelTimer() {
        binding.tvTimer.cancel();
    }

    private void disableAllInput() {
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) fieldCells[c][r].setEnabled(false);
            solCells[c].setEnabled(false);
        }
        binding.btnFinalSolution.setClickable(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
        binding = null;
    }
}
