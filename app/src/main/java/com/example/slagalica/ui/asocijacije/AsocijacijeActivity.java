package com.example.slagalica.ui.asocijacije;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.slagalica.ui.BaseActivity;

import com.example.slagalica.R;
import com.example.slagalica.databinding.ActivityAsocijacijeBinding;
import com.example.slagalica.data.model.Asocijacija;

public class AsocijacijeActivity extends BaseActivity {

    private ActivityAsocijacijeBinding binding;
    private AsocijacijeViewModel viewModel;

    private Button[][] fieldButtons = new Button[4][4];
    private Button[] solButtons = new Button[4];
    private EditText[] guessEdits = new EditText[4];
    private Button[] guessButtons = new Button[4];

    private static final int TIMER_DURATION = 120000;

    // Sunny pop palette — resolved from res/values/colors.xml (design tokens)
    private int HIDDEN;     // accent
    private int REVEALED;   // open white tile (panel)
    private int SOLVED;     // correct green
    private int SOL_BASE;   // accent2
    private int FINAL_BASE; // accent (asoc-final = accent gradient)
    private int DARK_TEXT;  // text
    private int WHITE;

    private void resolveColors() {
        HIDDEN = ContextCompat.getColor(this, R.color.accent);
        REVEALED = ContextCompat.getColor(this, R.color.panel);
        SOLVED = ContextCompat.getColor(this, R.color.correct);
        SOL_BASE = ContextCompat.getColor(this, R.color.accent2);
        FINAL_BASE = ContextCompat.getColor(this, R.color.accent);
        DARK_TEXT = ContextCompat.getColor(this, R.color.text);
        WHITE = ContextCompat.getColor(this, R.color.white);
    }

    private void style(Button b, int bg, int fg) {
        b.setBackgroundTintList(ColorStateList.valueOf(bg));
        b.setTextColor(fg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAsocijacijeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        resolveColors();
        setupStatsBar();
        viewModel = new ViewModelProvider(this).get(AsocijacijeViewModel.class);

        bindViews();
        setupObservers();
        setupListeners();

        viewModel.loadRound(1);
    }

    private void bindViews() {
        fieldButtons[0][0] = binding.btnC1F1;
        fieldButtons[0][1] = binding.btnC1F2;
        fieldButtons[0][2] = binding.btnC1F3;
        fieldButtons[0][3] = binding.btnC1F4;
        fieldButtons[1][0] = binding.btnC2F1;
        fieldButtons[1][1] = binding.btnC2F2;
        fieldButtons[1][2] = binding.btnC2F3;
        fieldButtons[1][3] = binding.btnC2F4;
        fieldButtons[2][0] = binding.btnC3F1;
        fieldButtons[2][1] = binding.btnC3F2;
        fieldButtons[2][2] = binding.btnC3F3;
        fieldButtons[2][3] = binding.btnC3F4;
        fieldButtons[3][0] = binding.btnC4F1;
        fieldButtons[3][1] = binding.btnC4F2;
        fieldButtons[3][2] = binding.btnC4F3;
        fieldButtons[3][3] = binding.btnC4F4;

        solButtons[0] = binding.btnSolC1;
        solButtons[1] = binding.btnSolC2;
        solButtons[2] = binding.btnSolC3;
        solButtons[3] = binding.btnSolC4;

        guessEdits[0] = binding.etGuessC1;
        guessEdits[1] = binding.etGuessC2;
        guessEdits[2] = binding.etGuessC3;
        guessEdits[3] = binding.etGuessC4;

        guessButtons[0] = binding.btnGuessC1;
        guessButtons[1] = binding.btnGuessC2;
        guessButtons[2] = binding.btnGuessC3;
        guessButtons[3] = binding.btnGuessC4;
    }

    private void setupObservers() {
        viewModel.getCurrentRoundData().observe(this, this::applyRoundData);

        viewModel.getScore().observe(this, s ->
                binding.tvScore.setText("Bodovi: " + s));

        viewModel.getCurrentRound().observe(this, r ->
                binding.tvRound.setText("Runda " + r + " / 2"));

        viewModel.getGameFinished().observe(this, finished -> {
            if (finished) {
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
        // Reset grid to hidden state
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                fieldButtons[c][r].setText("?");
                style(fieldButtons[c][r], HIDDEN, WHITE);
                fieldButtons[c][r].setEnabled(true);
            }
            solButtons[c].setText("???");
            style(solButtons[c], SOL_BASE, WHITE);
            guessEdits[c].setText("");
            guessEdits[c].setError(null);
            guessEdits[c].setEnabled(true);
            guessButtons[c].setEnabled(true);
        }
        binding.etGuessFinal.setText("");
        binding.etGuessFinal.setError(null);
        binding.etGuessFinal.setEnabled(true);
        binding.btnGuessFinal.setEnabled(true);
        style(binding.btnFinalSolution, FINAL_BASE, WHITE);

        cancelTimer();
        startTimer(TIMER_DURATION);
    }

    private void setupListeners() {
        for (int c = 0; c < 4; c++) {
            final int col = c;
            for (int r = 0; r < 4; r++) {
                final int row = r;
                fieldButtons[c][r].setOnClickListener(v -> onFieldClicked(col, row));
            }
            guessButtons[c].setOnClickListener(v -> onGuessColumn(col));
        }
        binding.btnGuessFinal.setOnClickListener(v -> onGuessFinal());
    }

    private void onFieldClicked(int col, int row) {
        if (viewModel.isColumnSolved(col) || viewModel.isFieldRevealed(col, row)) return;

        Asocijacija data = viewModel.getCurrentRoundData().getValue();
        if (data == null) return;

        viewModel.revealField(col, row);
        fieldButtons[col][row].setText(data.getColumnFields()[col][row]);
        style(fieldButtons[col][row], REVEALED, DARK_TEXT);
        fieldButtons[col][row].setEnabled(false);
    }

    private void onGuessColumn(int col) {
        if (viewModel.isColumnSolved(col)) return;

        String guess = guessEdits[col].getText().toString();
        int points = viewModel.guessColumn(col, guess);

        if (points >= 0) {
            Asocijacija data = viewModel.getCurrentRoundData().getValue();
            solButtons[col].setText(data.getColumnSolutions()[col]);
            style(solButtons[col], SOLVED, WHITE);
            revealRemainingFields(col, data);
            guessEdits[col].setEnabled(false);
            guessButtons[col].setEnabled(false);
            guessEdits[col].setError(null);
        } else {
            guessEdits[col].setError("Netačno");
        }
    }

    private void revealRemainingFields(int col, Asocijacija data) {
        for (int r = 0; r < 4; r++) {
            if (!viewModel.isFieldRevealed(col, r)) {
                fieldButtons[col][r].setText(data.getColumnFields()[col][r]);
                style(fieldButtons[col][r], SOLVED, WHITE);
                fieldButtons[col][r].setEnabled(false);
            }
        }
    }

    private void onGuessFinal() {
        if (viewModel.isFinalSolved()) return;

        String guess = binding.etGuessFinal.getText().toString();
        int points = viewModel.guessFinal(guess);

        if (points >= 0) {
            Asocijacija data = viewModel.getCurrentRoundData().getValue();
            binding.btnFinalSolution.setText(data.getFinalSolution());
            style(binding.btnFinalSolution, SOLVED, WHITE);
            binding.etGuessFinal.setEnabled(false);
            binding.btnGuessFinal.setEnabled(false);
            Toast.makeText(this, "Bravo! +" + points + " bodova", Toast.LENGTH_SHORT).show();

            // Auto-advance after 2 seconds
            binding.getRoot().postDelayed(() -> viewModel.advanceRound(), 2000);
        } else {
            binding.etGuessFinal.setError("Netačno");
        }
    }

    private void startTimer(long durationMs) {
        binding.tvTimer.start(durationMs, () -> viewModel.advanceRound());
    }

    private void cancelTimer() {
        binding.tvTimer.cancel();
    }

    private void disableAllInput() {
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) fieldButtons[c][r].setEnabled(false);
            guessEdits[c].setEnabled(false);
            guessButtons[c].setEnabled(false);
        }
        binding.etGuessFinal.setEnabled(false);
        binding.btnGuessFinal.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
        binding = null;
    }
}
