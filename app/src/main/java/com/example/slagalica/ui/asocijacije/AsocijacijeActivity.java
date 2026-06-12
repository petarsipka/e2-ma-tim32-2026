package com.example.slagalica.ui.asocijacije;

import android.content.Intent;
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
import com.example.slagalica.ui.asocijacije.AsocijacijeViewModel.GameState;
import com.example.slagalica.ui.lobby.LobbyActivity;

public class AsocijacijeActivity extends BaseActivity {

    public static final String EXTRA_MATCH_CODE = "match_code";
    private static final int TIMER_DURATION = 120000;
    private static final String[] COL_LETTER = {"A", "B", "C", "D"};

    private ActivityAsocijacijeBinding binding;
    private AsocijacijeViewModel viewModel;

    private final TextView[][] fieldCells = new TextView[4][4];
    private final TextView[] solCells = new TextView[4];

    private int MUTED, FAINT, TEXT, WHITE;
    private int[] colColors;

    private String matchCode;
    private boolean isHost;
    private String myUid;

    // Local turn state
    private GameState lastState;
    private boolean openedThisTurn;
    private boolean prevMyTurn;
    private int lastRound = -1;
    private boolean advanceScheduled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAsocijacijeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        matchCode = getIntent().getStringExtra(EXTRA_MATCH_CODE);
        isHost = getIntent().getBooleanExtra(LobbyActivity.EXTRA_IS_HOST, false);

        resolveColors();
        setupStatsBar();
        setOpponent("Protivnik");

        viewModel = new ViewModelProvider(this).get(AsocijacijeViewModel.class);
        bindViews();
        setupListeners();
        setupObservers();

        viewModel.init(matchCode, isHost);
        myUid = viewModel.getMyUid();
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
        binding.btnPass.setOnClickListener(v -> {
            if (lastState != null && lastState.myTurn && openedThisTurn) viewModel.passTurn();
        });
    }

    private void setupObservers() {
        viewModel.getGameState().observe(this, this::onGameState);

        viewModel.getScore().observe(this, s ->
                binding.tvRoundPoints.setText("+" + s + " bodova"));
        // Live scoreboard shows cumulative match totals.
        viewModel.getMyTotal().observe(this, total ->
                updateScore(total, safe(viewModel.getOpponentTotal().getValue())));
        viewModel.getOpponentTotal().observe(this, oppTotal ->
                updateScore(safe(viewModel.getMyTotal().getValue()), oppTotal));
        viewModel.getOpponentName().observe(this, this::setOpponent);

        viewModel.getGameFinished().observe(this, finished -> {
            if (Boolean.TRUE.equals(finished)) showGameFinished();
        });
    }

    // ── Render from state ──────────────────────────────────────────────────────

    private void onGameState(GameState st) {
        if (myUid == null) myUid = viewModel.getMyUid();
        lastState = st;

        boolean roundChanged = st.round != lastRound;
        if (roundChanged) {
            lastRound = st.round;
            openedThisTurn = false;
            advanceScheduled = false;
            binding.tvTimer.cancel();
            binding.tvTimer.start(TIMER_DURATION, this::onTimerExpired);
        }
        // Fresh turn for me → must open a field again.
        if (st.myTurn && !prevMyTurn) openedThisTurn = false;
        prevMyTurn = st.myTurn;

        renderBoard(st);
        updateTurnIndicator(st);

        // Final solved → reveal everything; host advances after a short reveal.
        if (st.finalSolvedBy != null && isHost && !advanceScheduled) {
            advanceScheduled = true;
            binding.tvTimer.cancel();
            binding.getRoot().postDelayed(() -> viewModel.advanceRound(), 8000);
        }
    }

    private void renderBoard(GameState st) {
        Asocijacija data = viewModel.dataForRound(st.round);
        boolean finalDone = st.finalSolvedBy != null;

        for (int c = 0; c < 4; c++) {
            boolean colSolved = st.columnSolvedBy[c] != null;
            if (colSolved || finalDone) {
                styleSolvedSol(solCells[c], data.getColumnSolutions()[c], colColors[c]);
                for (int r = 0; r < 4; r++) styleOpenField(fieldCells[c][r], data.getColumnFields()[c][r]);
            } else {
                styleClosedSol(solCells[c], COL_LETTER[c]);
                for (int r = 0; r < 4; r++) {
                    if (st.revealed[c][r]) styleOpenField(fieldCells[c][r], data.getColumnFields()[c][r]);
                    else styleClosedField(fieldCells[c][r], COL_LETTER[c] + (r + 1));
                }
            }
        }

        if (finalDone) {
            styleFinalBanner(data.getFinalSolution().toUpperCase(), true);
        } else {
            styleFinalBanner(maskSolution(data.getFinalSolution()), false);
        }

        updateInteractionStates(st);
        updateSolvedChip(st);
    }

    /** Enable/disable cells + pass button based on turn and whether I've opened a field. */
    private void updateInteractionStates(GameState st) {
        boolean canOpen = st.myTurn && !openedThisTurn && st.finalSolvedBy == null;
        boolean canGuess = st.myTurn && openedThisTurn && st.finalSolvedBy == null;

        for (int c = 0; c < 4; c++) {
            boolean colSolved = st.columnSolvedBy[c] != null;
            for (int r = 0; r < 4; r++) {
                boolean openable = canOpen && !colSolved && !st.revealed[c][r];
                fieldCells[c][r].setEnabled(openable);
            }
            solCells[c].setEnabled(canGuess && !colSolved);
        }
        binding.btnFinalSolution.setClickable(canGuess);
        binding.btnPass.setVisibility(st.myTurn && openedThisTurn ? View.VISIBLE : View.GONE);
    }

    private void onFieldClicked(int col, int row) {
        if (lastState == null || !lastState.myTurn || openedThisTurn) return;
        if (lastState.columnSolvedBy[col] != null || lastState.revealed[col][row]) return;

        openedThisTurn = true;
        Asocijacija data = viewModel.dataForRound(lastState.round);
        styleOpenField(fieldCells[col][row], data.getColumnFields()[col][row]);
        viewModel.revealField(col, row);
        updateInteractionStates(lastState);   // show pass button, enable guessing
    }

    private void onSolutionClicked(int col) {
        if (lastState == null || !lastState.myTurn || !openedThisTurn) return;
        if (lastState.columnSolvedBy[col] != null) return;
        showGuessDialog("Rešenje za kolonu " + COL_LETTER[col], guess -> {
            int points = viewModel.guessColumn(col, guess);
            if (points >= 0) Toast.makeText(this, "Tačno! +" + points, Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "Netačno — protivnik je na potezu", Toast.LENGTH_SHORT).show();
        });
    }

    private void onFinalClicked() {
        if (lastState == null || !lastState.myTurn || !openedThisTurn) return;
        if (lastState.finalSolvedBy != null) return;
        showGuessDialog("Konačno rešenje", guess -> {
            int points = viewModel.guessFinal(guess);
            if (points >= 0) Toast.makeText(this, "Bravo! +" + points + " bodova", Toast.LENGTH_LONG).show();
            else Toast.makeText(this, "Netačno — protivnik je na potezu", Toast.LENGTH_SHORT).show();
        });
    }

    private void onTimerExpired() {
        // Only the host advances the round so both devices stay in lock-step.
        if (isHost && !advanceScheduled) {
            advanceScheduled = true;
            viewModel.advanceRound();
        }
    }

    private void updateTurnIndicator(GameState st) {
        if (st.myTurn) {
            binding.tvTurnIndicator.setText(openedThisTurn
                    ? "▶ Ti si na potezu — pogađaj ili završi potez"
                    : "▶ Ti si na potezu — otvori polje");
            binding.tvTurnIndicator.setTextColor(ContextCompat.getColor(this, R.color.correct));
        } else {
            String opp = viewModel.getOpponentName().getValue();
            binding.tvTurnIndicator.setText("⏳ " + (opp != null ? opp : "Protivnik") + " je na potezu");
            binding.tvTurnIndicator.setTextColor(MUTED);
        }
    }

    // ── Cell styling (unchanged from single-player) ────────────────────────────

    private void styleClosedField(TextView cell, String placeholder) {
        cell.setBackgroundResource(R.drawable.bg_asoc_cell_closed);
        cell.setBackgroundTintList(null);
        cell.setTypeface(ResourcesCompat.getFont(this, R.font.baloo2), android.graphics.Typeface.BOLD);
        cell.setTextColor(MUTED);
        cell.setTextSize(13);
        cell.setText(placeholder);
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
    }

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

    private void updateSolvedChip(GameState st) {
        int solved = 0;
        for (int c = 0; c < 4; c++) if (st.columnSolvedBy[c] != null) solved++;
        binding.tvSolvedColumns.setText("⚡ " + solvedColumnsLabel(solved));
    }

    private static String solvedColumnsLabel(int n) {
        if (n == 1) return "1 kolona rešena";
        if (n >= 2 && n <= 4) return n + " kolone rešene";
        return n + " kolona rešeno";
    }

    private void showGameFinished() {
        binding.tvTimer.cancel();
        binding.tvTimer.showZero();
        binding.tvTurnIndicator.setText("Asocijacije završene!");
        binding.tvTurnIndicator.setTextColor(TEXT);
        binding.btnPass.setVisibility(View.GONE);
        disableAllInput();

        // Last implemented game → show the match result.
        // (Skočko/KPK/Moj broj are not implemented yet; they score 0 on the result.)
        if (matchCode != null) {
            binding.getRoot().postDelayed(() -> {
                Intent intent = new Intent(this, com.example.slagalica.ui.ResultActivity.class);
                intent.putExtra(com.example.slagalica.ui.ResultActivity.EXTRA_MATCH_CODE, matchCode);
                startActivity(intent);
                finish();
            }, 1500);
        }
    }

    private void disableAllInput() {
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) fieldCells[c][r].setEnabled(false);
            solCells[c].setEnabled(false);
        }
        binding.btnFinalSolution.setClickable(false);
    }

    private static int safe(Integer v) { return v != null ? v : 0; }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.tvTimer.cancel();
        binding = null;
    }
}
