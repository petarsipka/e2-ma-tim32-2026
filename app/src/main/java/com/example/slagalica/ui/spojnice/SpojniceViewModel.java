package com.example.slagalica.ui.spojnice;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.SpojniceRepository;
import com.example.slagalica.data.model.Match;
import com.example.slagalica.data.model.SpojnicePair;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SpojniceViewModel extends ViewModel {

    private static final String TAG = "SpojniceVM";
    static final int TOTAL_ROUNDS = 2;
    static final int PAIRS_COUNT = 5;

    /** One connection attempt for a left item, written under connections/{round}/{leftIndex}. */
    public static class ConnectionEntry {
        public String uid;
        public int rightIndex;
        public boolean correct;

        public ConnectionEntry() {}
    }

    /** Immutable snapshot of the current phase, consumed by the Activity to render. */
    public static class PhaseState {
        public final int round;
        public final String phase;       // "A" | "B"
        public final boolean myTurn;
        public final Map<Integer, ConnectionEntry> connections; // for this round

        PhaseState(int round, String phase, boolean myTurn, Map<Integer, ConnectionEntry> conns) {
            this.round = round;
            this.phase = phase;
            this.myTurn = myTurn;
            this.connections = conns;
        }
    }

    // LiveData exposed to the Activity
    private final MutableLiveData<Integer> score = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> opponentScore = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> myTotal = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> opponentTotal = new MutableLiveData<>(0);
    private final MutableLiveData<String> opponentName = new MutableLiveData<>("Protivnik");
    private final MutableLiveData<String> opponentUidLive = new MutableLiveData<>();
    private final MutableLiveData<Boolean> gameFinished = new MutableLiveData<>(false);
    private final MutableLiveData<PhaseState> phaseState = new MutableLiveData<>();

    private final SpojniceRepository repository = new SpojniceRepository();

    // Multiplayer wiring
    private String matchCode;
    private String myUid;
    private String hostUid;
    private String opponentUid;
    private DatabaseReference matchRef;
    private DatabaseReference stateRef;
    private ValueEventListener matchListener;
    private ValueEventListener stateListener;

    // Cached state (single source of truth from RTDB)
    private int curRound = 0;     // 0 = not loaded yet
    private String curPhase = "A";
    private final Map<Integer, Map<Integer, ConnectionEntry>> connectionsByRound = new HashMap<>();

    // ── init ─────────────────────────────────────────────────────────────────

    public void init(String code, boolean isHost) {
        if (code == null) {
            // Single-device fallback: round 1, phase A, always my turn, no DB.
            curRound = 1;
            phaseState.setValue(new PhaseState(1, "A", true, new HashMap<>()));
            return;
        }
        this.matchCode = code;
        this.myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference root = FirebaseDatabase.getInstance(MatchRepository.DB_URL)
                .getReference("matches").child(code);
        matchRef = root;
        stateRef = root.child("state").child("spojnice");

        matchListener = new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snap) {
                Match m = snap.getValue(Match.class);
                if (m == null) return;
                hostUid = m.host;
                opponentUid = m.opponentOf(myUid);
                opponentUidLive.postValue(opponentUid);
                opponentScore.postValue(m.gameScore(opponentUid, Match.GAME_SPOJNICE));
                myTotal.postValue(m.totalScore(myUid));
                opponentTotal.postValue(m.totalScore(opponentUid));
                if (opponentUid != null && m.players != null) {
                    Match.Player p = m.players.get(opponentUid);
                    if (p != null && p.name != null) opponentName.postValue(p.name);
                }
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

        if (isHost) writePhase(1, "A");
    }

    // ── RTDB → state ──────────────────────────────────────────────────────────

    private void onGameStateUpdate(DataSnapshot snap) {
        if (!snap.exists()) return;

        String phase = snap.child("phase").exists() ? (String) snap.child("phase").getValue() : "A";
        if ("DONE".equals(phase)) { gameFinished.postValue(true); return; }

        int round = snap.child("round").exists()
                ? ((Long) snap.child("round").getValue()).intValue() : 1;

        // Parse connections/{round}/{leftIndex}
        connectionsByRound.clear();
        for (DataSnapshot roundSnap : snap.child("connections").getChildren()) {
            int r;
            try { r = Integer.parseInt(roundSnap.getKey()); } catch (NumberFormatException e) { continue; }
            Map<Integer, ConnectionEntry> map = new HashMap<>();
            for (DataSnapshot leftSnap : roundSnap.getChildren()) {
                try {
                    ConnectionEntry e = leftSnap.getValue(ConnectionEntry.class);
                    if (e != null) map.put(Integer.parseInt(leftSnap.getKey()), e);
                } catch (NumberFormatException ignored) {}
            }
            connectionsByRound.put(r, map);
        }

        curRound = round;
        curPhase = phase;
        recomputeScore();
        emitState();
    }

    /** Build and publish a PhaseState once uids and a round are known. */
    private void emitState() {
        if (curRound == 0) return;
        boolean myTurn;
        if (matchCode == null) {
            myTurn = true;
        } else if (hostUid == null || myUid == null || opponentUid == null) {
            return; // wait until uids resolve
        } else {
            String guestUid = myUid.equals(hostUid) ? opponentUid : myUid;
            // Round 1: A=host, B=guest. Round 2: A=guest, B=host.
            String activeUid = ("A".equals(curPhase) == (curRound == 1)) ? hostUid : guestUid;
            myTurn = myUid.equals(activeUid);
        }
        Map<Integer, ConnectionEntry> conns = connectionsByRound.get(curRound);
        if (conns == null) conns = new HashMap<>();
        phaseState.postValue(new PhaseState(curRound, curPhase, myTurn, conns));
    }

    private void recomputeScore() {
        int total = 0;
        for (Map<Integer, ConnectionEntry> roundMap : connectionsByRound.values()) {
            for (ConnectionEntry e : roundMap.values()) {
                if (e.correct && myUid != null && myUid.equals(e.uid)) total += 2;
            }
        }
        score.postValue(total);
        if (matchCode != null && myUid != null) {
            FirebaseDatabase.getInstance(MatchRepository.DB_URL)
                    .getReference("matches").child(matchCode)
                    .child("gameScores").child(myUid).child(Match.GAME_SPOJNICE).setValue(total);
        }
    }

    // ── Game actions ──────────────────────────────────────────────────────────

    /** Validate a pairing and write it under connections/{round}/{leftIndex}. */
    public boolean checkConnection(int leftIndex, int rightIndex) {
        SpojnicePair pairs = repository.getRound(curRound);
        boolean correct = pairs.getCorrectMapping()[leftIndex] == rightIndex;
        if (matchCode != null && myUid != null) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("uid", myUid);
            entry.put("rightIndex", rightIndex);
            entry.put("correct", correct);
            stateRef.child("connections").child(String.valueOf(curRound))
                    .child(String.valueOf(leftIndex)).setValue(entry);
        } else {
            // Single-device fallback: keep a local connection so the board updates.
            Map<Integer, ConnectionEntry> map = connectionsByRound.computeIfAbsent(curRound, k -> new HashMap<>());
            ConnectionEntry e = new ConnectionEntry();
            e.uid = myUid; e.rightIndex = rightIndex; e.correct = correct;
            map.put(leftIndex, e);
            emitState();
        }
        return correct;
    }

    /**
     * Called by the ACTIVE player when their phase ends (timer or all attempts used).
     * Phase A with unconnected pairs → hand over to the other player (Phase B);
     * otherwise advance the round or finish the game.
     */
    public void advancePhase() {
        if (matchCode == null) {
            if (curRound >= TOTAL_ROUNDS) { gameFinished.setValue(true); }
            else { curRound++; phaseState.setValue(new PhaseState(curRound, "A", true, new HashMap<>())); }
            return;
        }
        int correctCount = countCorrect(curRound);
        if ("A".equals(curPhase) && correctCount < PAIRS_COUNT) {
            writePhase(curRound, "B");
        } else if (curRound >= TOTAL_ROUNDS) {
            stateRef.child("phase").setValue("DONE");
        } else {
            writePhase(curRound + 1, "A");
        }
    }

    private int countCorrect(int round) {
        Map<Integer, ConnectionEntry> map = connectionsByRound.get(round);
        if (map == null) return 0;
        int c = 0;
        for (ConnectionEntry e : map.values()) if (e.correct) c++;
        return c;
    }

    private void writePhase(int round, String phase) {
        Map<String, Object> update = new HashMap<>();
        update.put("round", round);
        update.put("phase", phase);
        update.put("phaseStartedAt", ServerValue.TIMESTAMP);
        stateRef.updateChildren(update);
    }

    public SpojnicePair pairsForRound(int round) { return repository.getRound(round); }

    // ── Getters ───────────────────────────────────────────────────────────────

    public MutableLiveData<Integer> getScore() { return score; }
    public MutableLiveData<Integer> getOpponentScore() { return opponentScore; }
    public MutableLiveData<Integer> getMyTotal() { return myTotal; }
    public MutableLiveData<Integer> getOpponentTotal() { return opponentTotal; }
    public MutableLiveData<String> getOpponentName() { return opponentName; }
    public MutableLiveData<String> getOpponentUid() { return opponentUidLive; }
    public MutableLiveData<Boolean> getGameFinished() { return gameFinished; }
    public MutableLiveData<PhaseState> getPhaseState() { return phaseState; }
    public String getMyUid() { return myUid; }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (matchRef != null && matchListener != null) matchRef.removeEventListener(matchListener);
        if (stateRef != null && stateListener != null) stateRef.removeEventListener(stateListener);
    }
}
