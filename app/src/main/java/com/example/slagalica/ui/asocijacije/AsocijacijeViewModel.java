package com.example.slagalica.ui.asocijacije;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.AsocijacijeRepository;
import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.model.Asocijacija;
import com.example.slagalica.data.model.Match;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AsocijacijeViewModel extends ViewModel {

    private static final String TAG = "AsocijacijeVM";
    static final int TOTAL_ROUNDS = 2;

    /** A solve attribution stored under columns/{round}/{col} and final/{round}. */
    public static class SolveEntry {
        public String uid;
        public int points;
        public SolveEntry() {}
    }

    /** Immutable snapshot consumed by the Activity to render the whole board. */
    public static class GameState {
        public final int round;
        public final boolean myTurn;
        public final boolean[][] revealed;     // 4×4 for current round
        public final String[] columnSolvedBy;  // uid per column, null = unsolved
        public final String finalSolvedBy;     // uid or null

        GameState(int round, boolean myTurn, boolean[][] revealed,
                  String[] columnSolvedBy, String finalSolvedBy) {
            this.round = round;
            this.myTurn = myTurn;
            this.revealed = revealed;
            this.columnSolvedBy = columnSolvedBy;
            this.finalSolvedBy = finalSolvedBy;
        }
    }

    private final MutableLiveData<Integer> score = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> opponentScore = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> myTotal = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> opponentTotal = new MutableLiveData<>(0);
    private final MutableLiveData<String> opponentName = new MutableLiveData<>("Protivnik");
    private final MutableLiveData<Boolean> gameFinished = new MutableLiveData<>(false);
    private final MutableLiveData<GameState> gameState = new MutableLiveData<>();

    private final AsocijacijeRepository repository = new AsocijacijeRepository();

    private String matchCode;
    private boolean isHost;
    private String myUid;
    private String hostUid;
    private String opponentUid;
    private DatabaseReference matchRef;
    private DatabaseReference stateRef;
    private ValueEventListener matchListener;
    private ValueEventListener stateListener;

    // Cached state from RTDB
    private int curRound = 0;
    private String activeUid;
    private boolean[][] revealedCur = new boolean[4][4];
    private String[] colByCur = new String[4];
    private String finalByCur;

    // ── init ─────────────────────────────────────────────────────────────────

    public void init(String code, boolean isHost) {
        this.isHost = isHost;
        if (code == null) {
            curRound = 1;
            gameState.setValue(new GameState(1, true, new boolean[4][4], new String[4], null));
            return;
        }
        this.matchCode = code;
        this.myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference root = FirebaseDatabase.getInstance(MatchRepository.DB_URL)
                .getReference("matches").child(code);
        matchRef = root;
        stateRef = root.child("state").child("asocijacije");

        matchListener = new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snap) {
                Match m = snap.getValue(Match.class);
                if (m == null) return;
                hostUid = m.host;
                opponentUid = m.opponentOf(myUid);
                opponentScore.postValue(m.gameScore(opponentUid, Match.GAME_ASOCIJACIJE));
                myTotal.postValue(m.totalScore(myUid));
                opponentTotal.postValue(m.totalScore(opponentUid));
                if (opponentUid != null && m.players != null) {
                    Match.Player p = m.players.get(opponentUid);
                    if (p != null && p.name != null) opponentName.postValue(p.name);
                }
                seedActiveIfNeeded();
                emitState();
            }
            @Override public void onCancelled(DatabaseError e) {
                Log.e(TAG, "match listener cancelled: " + e.getMessage());
            }
        };
        matchRef.addValueEventListener(matchListener);

        stateListener = new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snap) { onGameStateUpdate(snap); }
            @Override public void onCancelled(DatabaseError e) {
                Log.e(TAG, "state listener cancelled: " + e.getMessage());
            }
        };
        stateRef.addValueEventListener(stateListener);

        if (isHost) {
            Map<String, Object> seed = new HashMap<>();
            seed.put("round", 1);
            seed.put("phase", "playing");
            // active set once host uid resolves below via seedActiveIfNeeded
            stateRef.updateChildren(seed);
        }
    }

    // ── RTDB → state ──────────────────────────────────────────────────────────

    private void onGameStateUpdate(DataSnapshot snap) {
        if (!snap.exists()) return;
        String phase = snap.child("phase").exists() ? (String) snap.child("phase").getValue() : "playing";
        if ("DONE".equals(phase)) { gameFinished.postValue(true); return; }

        int round = snap.child("round").exists()
                ? ((Long) snap.child("round").getValue()).intValue() : 1;
        activeUid = snap.child("active").exists() ? (String) snap.child("active").getValue() : null;
        seedActiveIfNeeded();

        // revealed/{round}/{key}
        revealedCur = new boolean[4][4];
        DataSnapshot revRound = snap.child("revealed").child(String.valueOf(round));
        for (DataSnapshot cell : revRound.getChildren()) {
            String key = cell.getKey();
            if (key != null && key.length() == 2) {
                int c = key.charAt(0) - '0';
                int r = key.charAt(1) - '0';
                if (c >= 0 && c < 4 && r >= 0 && r < 4) revealedCur[c][r] = true;
            }
        }

        // columns/{round}/{col}
        colByCur = new String[4];
        DataSnapshot colRound = snap.child("columns").child(String.valueOf(round));
        for (DataSnapshot col : colRound.getChildren()) {
            try {
                int c = Integer.parseInt(col.getKey());
                SolveEntry e = col.getValue(SolveEntry.class);
                if (e != null && c >= 0 && c < 4) colByCur[c] = e.uid;
            } catch (NumberFormatException ignored) {}
        }

        // final/{round}
        SolveEntry fin = snap.child("final").child(String.valueOf(round)).getValue(SolveEntry.class);
        finalByCur = fin != null ? fin.uid : null;

        curRound = round;
        recomputeScore(snap);
        emitState();
    }

    /** Host writes the starting active player (itself) once round + uids are known. */
    private void seedActiveIfNeeded() {
        if (isHost && activeUid == null && hostUid != null && curRound != 0) {
            stateRef.child("active").setValue(hostUid);
        }
    }

    private void emitState() {
        if (curRound == 0) return;
        boolean myTurn;
        if (matchCode == null) {
            myTurn = true;
        } else if (myUid == null || activeUid == null) {
            return;
        } else {
            myTurn = myUid.equals(activeUid);
        }
        gameState.postValue(new GameState(curRound, myTurn, copy(revealedCur),
                colByCur.clone(), finalByCur));
    }

    /** Sum my points across both rounds (columns + final). */
    private void recomputeScore(DataSnapshot snap) {
        int total = 0;
        for (DataSnapshot roundSnap : snap.child("columns").getChildren()) {
            for (DataSnapshot col : roundSnap.getChildren()) {
                SolveEntry e = col.getValue(SolveEntry.class);
                if (e != null && myUid != null && myUid.equals(e.uid)) total += e.points;
            }
        }
        for (DataSnapshot roundSnap : snap.child("final").getChildren()) {
            SolveEntry e = roundSnap.getValue(SolveEntry.class);
            if (e != null && myUid != null && myUid.equals(e.uid)) total += e.points;
        }
        score.postValue(total);
        if (matchCode != null && myUid != null) {
            FirebaseDatabase.getInstance(MatchRepository.DB_URL)
                    .getReference("matches").child(matchCode)
                    .child("gameScores").child(myUid).child(Match.GAME_ASOCIJACIJE).setValue(total);
        }
    }

    // ── Game actions (active player only — Activity guards) ────────────────────

    public void revealField(int col, int row) {
        if (matchCode == null) { revealedCur[col][row] = true; emitState(); return; }
        stateRef.child("revealed").child(String.valueOf(curRound))
                .child("" + col + row).setValue(true);
    }

    /** Returns earned points if correct (and writes the solve), -1 if wrong (and passes turn). */
    public int guessColumn(int col, String guess) {
        Asocijacija data = repository.getRound(curRound);
        if (colByCur[col] != null) return -1;
        if (!guess.trim().equalsIgnoreCase(data.getColumnSolutions()[col])) {
            passTurn();
            return -1;
        }
        int unrevealed = 0;
        for (int r = 0; r < 4; r++) if (!revealedCur[col][r]) unrevealed++;
        int points = 2 + unrevealed;
        writeSolve("columns/" + curRound + "/" + col, points);
        return points;
    }

    /** Returns earned points if correct, -1 if wrong (and passes turn). */
    public int guessFinal(String guess) {
        Asocijacija data = repository.getRound(curRound);
        if (finalByCur != null) return -1;
        if (!guess.trim().equalsIgnoreCase(data.getFinalSolution())) {
            passTurn();
            return -1;
        }
        int unopenedColumns = 0;
        int partialPoints = 0;
        for (int c = 0; c < 4; c++) {
            if (colByCur[c] == null) {
                boolean anyRevealed = false;
                int unrevealed = 0;
                for (int r = 0; r < 4; r++) {
                    if (revealedCur[c][r]) anyRevealed = true; else unrevealed++;
                }
                if (!anyRevealed) unopenedColumns++;
                else partialPoints += 2 + unrevealed;
            }
        }
        int points = 7 + 6 * unopenedColumns + partialPoints;
        writeSolve("final/" + curRound, points);
        return points;
    }

    private void writeSolve(String path, int points) {
        if (matchCode == null) return;
        Map<String, Object> entry = new HashMap<>();
        entry.put("uid", myUid);
        entry.put("points", points);
        stateRef.child(path.split("/")[0])  // "columns" | "final"
                .child(path.substring(path.indexOf('/') + 1))
                .setValue(entry);
    }

    /** Hand the turn to the other player (wrong guess or explicit pass). */
    public void passTurn() {
        if (matchCode == null) return;
        String next = myUid.equals(hostUid) ? opponentUid : hostUid;
        stateRef.child("active").setValue(next);
    }

    /** Host-only: advance to next round or finish the game. */
    public void advanceRound() {
        if (matchCode == null) {
            if (curRound >= TOTAL_ROUNDS) gameFinished.setValue(true);
            else { curRound++; gameState.setValue(
                    new GameState(curRound, true, new boolean[4][4], new String[4], null)); }
            return;
        }
        if (!isHost) return;
        if (curRound >= TOTAL_ROUNDS) {
            stateRef.child("phase").setValue("DONE");
        } else {
            // Round 2 starts with the guest.
            Map<String, Object> update = new HashMap<>();
            update.put("round", curRound + 1);
            update.put("active", opponentUid);  // host's opponent = guest
            update.put("phase", "playing");
            stateRef.updateChildren(update);
        }
    }

    public Asocijacija dataForRound(int round) { return repository.getRound(round); }
    public String getMyUid() { return myUid; }
    public boolean isHost() { return isHost; }

    private static boolean[][] copy(boolean[][] src) {
        boolean[][] out = new boolean[4][4];
        for (int c = 0; c < 4; c++) System.arraycopy(src[c], 0, out[c], 0, 4);
        return out;
    }

    public MutableLiveData<Integer> getScore() { return score; }
    public MutableLiveData<Integer> getOpponentScore() { return opponentScore; }
    public MutableLiveData<Integer> getMyTotal() { return myTotal; }
    public MutableLiveData<Integer> getOpponentTotal() { return opponentTotal; }
    public MutableLiveData<String> getOpponentName() { return opponentName; }
    public MutableLiveData<Boolean> getGameFinished() { return gameFinished; }
    public MutableLiveData<GameState> getGameState() { return gameState; }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (matchRef != null && matchListener != null) matchRef.removeEventListener(matchListener);
        if (stateRef != null && stateListener != null) stateRef.removeEventListener(stateListener);
    }
}
