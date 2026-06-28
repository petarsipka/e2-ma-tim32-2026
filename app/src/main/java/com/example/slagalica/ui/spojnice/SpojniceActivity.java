package com.example.slagalica.ui.spojnice;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.data.model.SpojnicePair;
import com.example.slagalica.databinding.ActivitySpojniceBinding;
import com.example.slagalica.ui.BaseActivity;
import com.example.slagalica.ui.asocijacije.AsocijacijeActivity;
import com.example.slagalica.ui.lobby.LobbyActivity;
import com.example.slagalica.ui.spojnice.SpojniceViewModel.ConnectionEntry;
import com.example.slagalica.ui.spojnice.SpojniceViewModel.PhaseState;
import com.example.slagalica.ui.widget.SpojItemView;

import java.util.List;
import java.util.Map;

public class SpojniceActivity extends BaseActivity {

    public static final String EXTRA_MATCH_CODE = "match_code";

    private static final int PAIRS = 5;
    private static final int ROUND_MS = 30000;

    private ActivitySpojniceBinding binding;
    private SpojniceViewModel viewModel;

    private int[] pairColors;
    private SpojItemView[] leftItems;
    private SpojItemView[] rightItems;

    private String matchCode;
    private boolean isHost;
    private String myUid;

    private int selectedLeftIndex = -1;
    private boolean[] clickableLeft = new boolean[PAIRS];
    private String lastPhaseKey = "";
    private boolean phaseAdvancing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpojniceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBarPadding();

        matchCode = getIntent().getStringExtra(EXTRA_MATCH_CODE);
        isHost = getIntent().getBooleanExtra(LobbyActivity.EXTRA_IS_HOST, false);

        resolveColors();
        setupStatsBar();
        setOpponent("Protivnik");

        leftItems = new SpojItemView[]{binding.btnL1, binding.btnL2, binding.btnL3, binding.btnL4, binding.btnL5};
        rightItems = new SpojItemView[]{binding.btnR1, binding.btnR2, binding.btnR3, binding.btnR4, binding.btnR5};

        viewModel = new ViewModelProvider(this).get(SpojniceViewModel.class);

        setupItemListeners();
        setupObservers();

        viewModel.init(matchCode, isHost);
        myUid = viewModel.getMyUid();
    }

    private void resolveColors() {
        pairColors = new int[]{
                ContextCompat.getColor(this, R.color.g0),
                ContextCompat.getColor(this, R.color.g1),
                ContextCompat.getColor(this, R.color.g2),
                ContextCompat.getColor(this, R.color.g3),
                ContextCompat.getColor(this, R.color.accent_end)
        };
    }

    private void setupObservers() {
        viewModel.getPhaseState().observe(this, this::onPhaseState);

        viewModel.getScore().observe(this, s ->
                binding.tvRoundPoints.setText("+" + s + " bodova"));
        // Live scoreboard shows cumulative match totals.
        viewModel.getMyTotal().observe(this, total ->
                updateScore(total, safe(viewModel.getOpponentTotal().getValue())));
        viewModel.getOpponentTotal().observe(this, oppTotal ->
                updateScore(safe(viewModel.getMyTotal().getValue()), oppTotal));
        viewModel.getOpponentName().observe(this, this::setOpponent);
        viewModel.getOpponentUid().observe(this, this::setOpponentAvatar);

        viewModel.getGameFinished().observe(this, finished -> {
            if (Boolean.TRUE.equals(finished)) showGameFinished();
        });
    }

    private void setupItemListeners() {
        for (int i = 0; i < PAIRS; i++) {
            final int index = i;
            leftItems[i].setOnClickListener(v -> onLeftClicked(index));
            rightItems[i].setOnClickListener(v -> onRightClicked(index));
        }
    }

    // ── Phase rendering ────────────────────────────────────────────────────────

    private void onPhaseState(PhaseState st) {
        if (myUid == null) myUid = viewModel.getMyUid();

        String key = st.round + ":" + st.phase;
        boolean newPhase = !key.equals(lastPhaseKey);
        if (newPhase) {
            lastPhaseKey = key;
            phaseAdvancing = false;
            renderBoard(st);
            startTimer(st.myTurn);
        } else {
            renderBoard(st);
        }
        updateTurnIndicator(st);
    }

    /** Full re-render of the board from the connections map for the current round. */
    private void renderBoard(PhaseState st) {
        SpojnicePair pairs = viewModel.pairsForRound(st.round);
        List<String> left = pairs.getLeftTerms();
        List<String> right = pairs.getRightTerms();

        boolean[] clickable = new boolean[PAIRS];
        boolean[] rightTaken = new boolean[PAIRS];
        int colorCounter = 0;
        int correctCount = 0;

        for (int i = 0; i < PAIRS; i++) {
            leftItems[i].reset();
            leftItems[i].setShown(true);
            leftItems[i].setLabel(left.get(i));
            rightItems[i].reset();
            rightItems[i].setShown(true);
            rightItems[i].setLabel(right.get(i));
        }

        Map<Integer, ConnectionEntry> conns = st.connections;
        for (int i = 0; i < PAIRS; i++) {
            ConnectionEntry e = conns.get(i);
            if (e == null) {
                clickable[i] = st.myTurn;               // open if it's my turn
            } else if (e.correct) {
                int color = pairColors[colorCounter % pairColors.length];
                colorCounter++;
                correctCount++;
                leftItems[i].markMatched(color, colorCounter);
                rightItems[e.rightIndex].markMatched(color, colorCounter);
                rightTaken[e.rightIndex] = true;
            } else { // wrong attempt
                boolean mine = myUid != null && myUid.equals(e.uid);
                if (mine) {
                    leftItems[i].markWrong();           // I used my attempt this phase
                } else if (st.myTurn) {
                    clickable[i] = true;                // Phase B: reopen opponent's miss
                } else {
                    leftItems[i].markWrong();           // watching opponent miss
                }
            }
        }

        for (int i = 0; i < PAIRS; i++) {
            leftItems[i].setClickable(clickable[i]);
            rightItems[i].setClickable(st.myTurn && !rightTaken[i]);
        }
        clickableLeft = clickable;
        selectedLeftIndex = -1;
        updateConnectedChip(correctCount);

        // My turn and nothing left to try (all paired or all my attempts used) → phase over.
        if (st.myTurn && !phaseAdvancing && countTrue(clickable) == 0) {
            triggerAdvance();
        }
    }

    private void onLeftClicked(int index) {
        if (!clickableLeft[index]) return;
        if (selectedLeftIndex != -1) leftItems[selectedLeftIndex].reset();
        selectedLeftIndex = index;
        leftItems[index].markSelected();
    }

    private void onRightClicked(int index) {
        if (selectedLeftIndex == -1) return;
        int leftIndex = selectedLeftIndex;
        selectedLeftIndex = -1;
        clickableLeft[leftIndex] = false;     // consume this attempt locally
        // Write the attempt; the RTDB echo re-renders the board with the result.
        viewModel.checkConnection(leftIndex, index);
    }

    private void startTimer(boolean myTurn) {
        if (myTurn) {
            binding.tvTimer.start(ROUND_MS, this::triggerAdvance);
        } else {
            binding.tvTimer.start(ROUND_MS, () -> {});  // display-only countdown
        }
    }

    private void triggerAdvance() {
        if (phaseAdvancing) return;
        phaseAdvancing = true;
        binding.tvTimer.cancel();
        binding.getRoot().postDelayed(() -> viewModel.advancePhase(), 800);
    }

    private void updateTurnIndicator(PhaseState st) {
        if (st.myTurn) {
            binding.tvTurnIndicator.setText("Ti si na potezu");
            binding.tvTurnIndicator.setTextColor(ContextCompat.getColor(this, R.color.correct));
        } else {
            String opp = viewModel.getOpponentName().getValue();
            binding.tvTurnIndicator.setText((opp != null ? opp : "Protivnik") + " je na potezu");
            binding.tvTurnIndicator.setTextColor(ContextCompat.getColor(this, R.color.muted));
        }
    }

    private void updateConnectedChip(int correctCount) {
        binding.tvConnected.setText("⚡ Povezano " + correctCount + " / " + PAIRS);
    }

    private void showGameFinished() {
        binding.tvTimer.cancel();
        binding.tvTimer.setVisibility(View.GONE);
        binding.tvTurnIndicator.setText("Spojnice završene!");
        binding.tvTurnIndicator.setTextColor(ContextCompat.getColor(this, R.color.text));
        binding.tvLegend.setText("Bodovi: " + viewModel.getScore().getValue());
        for (SpojItemView item : leftItems) item.setShown(false);
        for (SpojItemView item : rightItems) item.setShown(false);

        if (matchCode != null) {
            binding.getRoot().postDelayed(() -> {
                Intent intent = new Intent(this, AsocijacijeActivity.class);
                intent.putExtra(AsocijacijeActivity.EXTRA_MATCH_CODE, matchCode);
                intent.putExtra(LobbyActivity.EXTRA_IS_HOST, isHost);
                startActivity(intent);
                finish();
            }, 2000);
        }
    }

    private static int countTrue(boolean[] arr) {
        int c = 0;
        for (boolean b : arr) if (b) c++;
        return c;
    }

    private static int safe(Integer v) { return v != null ? v : 0; }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.tvTimer.cancel();
        binding = null;
    }
}
