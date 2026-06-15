package com.example.slagalica.ui.skocko;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.model.Match;
import com.example.slagalica.data.model.SkockoQuestion;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkockoViewModel extends ViewModel {

    private final MutableLiveData<Integer> timer = new MutableLiveData<>(30);
    private final MutableLiveData<String> roundInfo = new MutableLiveData<>();
    private final MutableLiveData<String> status = new MutableLiveData<>();
    private final MutableLiveData<Boolean> inputEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<String> answerReveal = new MutableLiveData<>("");
    private final MutableLiveData<List<GuessResult>> guessHistory = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String[]> currentGuessDisplay = new MutableLiveData<>(new String[]{"", "", "", ""});
    private final MutableLiveData<Boolean> gameOver = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> gameFinished = new MutableLiveData<>(false);

    private final MutableLiveData<Integer> myTotal = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> opponentTotal = new MutableLiveData<>(0);
    private final MutableLiveData<String> opponentName = new MutableLiveData<>();
    private final MutableLiveData<String> opponentUid = new MutableLiveData<>();

    private MatchRepository matchRepo;
    private DatabaseReference gameRef;
    private ValueEventListener gameListener;
    private String matchCode;
    private String myUid;
    private String oppUid;
    private String hostUid;
    private boolean isHost;

    private List<SkockoQuestion> questions = new ArrayList<>();
    private String[] currentGuess = new String[4];
    private int guessPosition = 0;
    private boolean isStealPhase = false;
    private boolean isFinished = false;
    private android.os.CountDownTimer countDownTimer;
    private android.os.CountDownTimer pauseTimer;
    private int lastAppliedRound = -1;
    private int lastTimerRound = -1;
    private String lastTimerPhase = "";

    public static class State {
        public int round = 1;
        public String phase = "init";
        public String activeUid = "";
        public String roundOwnerUid = "";
        public String solution1 = "";
        public String solution2 = "";
        public int ownerAttempts = 0;
        public boolean ownerSolved = false;
        public boolean ownerDone = false;
        public boolean stealDone = false;
        public boolean stealerSolved = false;
        public int p1Score = 0;
        public int p2Score = 0;
        public List<HistoryEntry> history = new ArrayList<>();

        public State() {}
    }

    public static class HistoryEntry {
        public String guess = "";
        public int exact = 0;
        public int partial = 0;
        public String playerUid = "";
        public int row = 0;

        public HistoryEntry() {}
    }

    public static class GuessResult {
        public final String[] guess;
        public final int exact;
        public final int partial;
        public final int rowIndex;

        public GuessResult(String[] guess, int exact, int partial, int rowIndex) {
            this.guess = guess.clone();
            this.exact = exact;
            this.partial = partial;
            this.rowIndex = rowIndex;
        }
    }

    public void init(String code, boolean host) {
        matchCode = code;
        isHost = host;
        if (code == null) return;

        matchRepo = new MatchRepository();
        myUid = matchRepo.currentUid();
        matchRepo.listen(code, this::onMatchUpdate);

        FirebaseDatabase db = FirebaseDatabase.getInstance(MatchRepository.DB_URL);
        gameRef = db.getReference("matches").child(code).child("skocko");

        loadQuestions();
    }

    private void loadQuestions() {
        FirebaseFirestore.getInstance().collection("skocko")
                .get()
                .addOnSuccessListener(query -> {
                    questions.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        questions.add(doc.toObject(SkockoQuestion.class));
                    }
                    Collections.shuffle(questions);
                    if (questions.size() < 2) loadFallback();
                    if (isHost) initializeState();
                    attachGameListener();
                })
                .addOnFailureListener(e -> {
                    loadFallback();
                    if (isHost) initializeState();
                    attachGameListener();
                });
    }

    private void loadFallback() {
        questions = new ArrayList<>();
        questions.add(new SkockoQuestion("SCTK"));
        questions.add(new SkockoQuestion("PZKS"));
    }

    private void initializeState() {
        State s = new State();
        s.round = 1;
        s.phase = "main";
        s.activeUid = myUid;
        s.roundOwnerUid = myUid;
        s.solution1 = questions.get(0).getSolution();
        s.solution2 = questions.get(1).getSolution();
        gameRef.setValue(s);
    }

    private void attachGameListener() {
        gameListener = new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snap) {
                State st = snap.getValue(State.class);
                if (st != null) applyState(st);
            }
            @Override public void onCancelled(DatabaseError error) {}
        };
        gameRef.addValueEventListener(gameListener);
    }

    private void onMatchUpdate(Match match) {
        oppUid = match.opponentOf(myUid);
        hostUid = match.host;
        opponentUid.postValue(oppUid);
        myTotal.postValue(match.totalScore(myUid));
        opponentTotal.postValue(match.totalScore(oppUid));
        if (oppUid != null && match.players != null && match.players.get(oppUid) != null) {
            opponentName.postValue(match.players.get(oppUid).name);
        }
    }

    private State copy(State s) {
        State n = new State();
        n.round = s.round;
        n.phase = s.phase;
        n.activeUid = s.activeUid;
        n.roundOwnerUid = s.roundOwnerUid;
        n.solution1 = s.solution1;
        n.solution2 = s.solution2;
        n.ownerAttempts = s.ownerAttempts;
        n.ownerSolved = s.ownerSolved;
        n.ownerDone = s.ownerDone;
        n.stealDone = s.stealDone;
        n.stealerSolved = s.stealerSolved;
        n.p1Score = s.p1Score;
        n.p2Score = s.p2Score;
        if (s.history != null) n.history = new ArrayList<>(s.history);
        return n;
    }

    private void applyState(State s) {
        if (isFinished) return;
        if (s.round != lastAppliedRound) {
            lastAppliedRound = s.round;
            answerReveal.setValue("");  // clear reveal on round change
            clearGuessLocal();
        }

        boolean myTurn = myUid.equals(s.activeUid);
        isStealPhase = "steal".equals(s.phase);

        List<GuessResult> list = new ArrayList<>();
        if (s.history != null) {
            for (HistoryEntry e : s.history) {
                String[] g = new String[4];
                for (int i = 0; i < 4 && i < e.guess.length(); i++) g[i] = String.valueOf(e.guess.charAt(i));
                list.add(new GuessResult(g, e.exact, e.partial, e.row));
            }
        }
        guessHistory.setValue(list);

        roundInfo.setValue("Runda " + s.round + " / 2");

        if ("finished".equals(s.phase)) {
            status.setValue("Kraj! P1: " + s.p1Score + " | P2: " + s.p2Score);
            inputEnabled.setValue(false);
            gameOver.setValue(true);
            pushFinalScores(s.p1Score, s.p2Score);
            gameFinished.setValue(true);
            return;
        } else if ("reveal".equals(s.phase)) {
            String sol = s.round == 1 ? s.solution1 : s.solution2;
            answerReveal.setValue(sol);
            status.setValue("Rešenje: " + sol);
            inputEnabled.setValue(false);
            if (isHost && pauseTimer == null) {
                pauseTimer = new android.os.CountDownTimer(8000, 1000) {
                    @Override public void onTick(long ms) {}
                    @Override public void onFinish() {
                        pauseTimer = null;
                        advanceFromReveal(s);
                    }
                }.start();
            }
            return;
        } if ("main".equals(s.phase) || "steal".equals(s.phase)) {
            if (s.round != lastTimerRound || !s.phase.equals(lastTimerPhase)) {
                lastTimerRound = s.round;
                lastTimerPhase = s.phase;
                int secs = "steal".equals(s.phase) ? 10 : 30;
                startLocalTimer(secs, s);
            }
        } else {
            lastTimerRound = -1;
            lastTimerPhase = "";
            cancelLocalTimer();
        }

        inputEnabled.setValue(myTurn && ("main".equals(s.phase) || "steal".equals(s.phase)));

        if (myTurn && ("main".equals(s.phase) || "steal".equals(s.phase))) {
            int secs = "steal".equals(s.phase) ? 10 : 30;
            startLocalTimer(secs, s);
        } else {
            cancelLocalTimer();
        }

        if (!myTurn) clearGuessLocal();
    }

    private void startLocalTimer(int seconds, State stateSnapshot) {
        cancelLocalTimer();
        timer.setValue(seconds);
        countDownTimer = new android.os.CountDownTimer(seconds * 1000L, 1000) {
            @Override public void onTick(long ms) {
                if (isFinished) { cancel(); return; }
                timer.setValue((int) (ms / 1000));
            }
            @Override public void onFinish() {
                if (isFinished) return;
                timer.setValue(0);
                onLocalTimeUp(stateSnapshot);
            }
        }.start();
    }

    private void cancelLocalTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void onLocalTimeUp(State s) {
        State n = copy(s);
        if ("main".equals(n.phase)) {
            n.ownerDone = true;
            if (!n.ownerSolved) {
                n.phase = "steal";
                n.activeUid = n.roundOwnerUid.equals(hostUid) ? oppUid : hostUid;
            } else {
                n.phase = "reveal";
                n.activeUid = "";
                computeRoundScores(n);
            }
        } else if ("steal".equals(n.phase)) {
            n.stealDone = true;
            n.phase = "reveal";
            n.activeUid = "";
            computeRoundScores(n);
        }
        gameRef.setValue(n);
    }

    private void advanceFromReveal(State s) {
        if (s.round < 2) {
            State n = new State();
            n.round = 2;
            n.phase = "main";
            n.roundOwnerUid = oppUid;
            n.activeUid = oppUid;
            n.solution1 = s.solution1;
            n.solution2 = s.solution2;
            n.p1Score = s.p1Score;
            n.p2Score = s.p2Score;
            gameRef.setValue(n);
        } else {
            State n = copy(s);
            n.phase = "finished";
            n.activeUid = "";
            gameRef.setValue(n);
        }
    }

    public void addSymbol(String symbol) {
        if (guessPosition >= 4) return;
        currentGuess[guessPosition] = symbol;
        guessPosition++;
        updateGuessDisplay();
    }

    public void backspace() {
        if (guessPosition <= 0) return;
        guessPosition--;
        currentGuess[guessPosition] = null;
        updateGuessDisplay();
    }

    public void clearGuess() {
        clearGuessLocal();
    }

    private void clearGuessLocal() {
        currentGuess = new String[4];
        guessPosition = 0;
        updateGuessDisplay();
    }

    private void updateGuessDisplay() {
        String[] d = new String[4];
        for (int i = 0; i < 4; i++) d[i] = currentGuess[i] != null ? currentGuess[i] : "";
        currentGuessDisplay.setValue(d);
    }

    public void submitGuess() {
        if (matchCode == null || guessPosition < 4) return;
        gameRef.get().addOnSuccessListener(snap -> {
            State s = snap.getValue(State.class);
            if (s == null || !myUid.equals(s.activeUid)) return;
            if (!"main".equals(s.phase) && !"steal".equals(s.phase)) return;

            String solution = s.round == 1 ? s.solution1 : s.solution2;
            int[] eval = evaluate(currentGuess, solution);
            boolean solved = eval[0] == 4;

            State n = copy(s);
            HistoryEntry he = new HistoryEntry();
            he.guess = String.join("", currentGuess);
            he.exact = eval[0];
            he.partial = eval[1];
            he.playerUid = myUid;


            boolean imOwner = myUid.equals(n.roundOwnerUid);

            if ("main".equals(s.phase)) {
                he.row = n.history.size();
                n.ownerAttempts++;
                if (imOwner) n.ownerSolved = solved;
                if (solved) {
                    n.ownerDone = true;
                    n.phase = "reveal";
                    n.activeUid = "";
                    computeRoundScores(n);
                } else if (n.ownerAttempts >= 6) {
                    n.ownerDone = true;
                    n.phase = "steal";
                    n.activeUid = n.roundOwnerUid.equals(hostUid) ? oppUid : hostUid;
                } else {
                    n.activeUid = myUid;
                }
            } else {
                he.row = 6;
                if (solved) n.stealerSolved = true;
                n.stealDone = true;
                n.phase = "reveal";
                n.activeUid = "";
                computeRoundScores(n);
            }
            n.history.add(he);
            cancelLocalTimer();
            gameRef.setValue(n);
            clearGuessLocal();
        });
    }

    private int[] evaluate(String[] guess, String solution) {
        int exact = 0, partial = 0;
        boolean[] solUsed = new boolean[4];
        boolean[] gueUsed = new boolean[4];
        for (int i = 0; i < 4; i++) {
            if (guess[i] != null && guess[i].equals(String.valueOf(solution.charAt(i)))) {
                exact++;
                solUsed[i] = true;
                gueUsed[i] = true;
            }
        }
        for (int i = 0; i < 4; i++) {
            if (gueUsed[i]) continue;
            for (int j = 0; j < 4; j++) {
                if (solUsed[j]) continue;
                if (guess[i] != null && guess[i].equals(String.valueOf(solution.charAt(j)))) {
                    partial++;
                    solUsed[j] = true;
                    gueUsed[i] = true;
                    break;
                }
            }
        }
        return new int[]{exact, partial};
    }

    private void computeRoundScores(State n) {
        boolean ownerIsP1 = n.roundOwnerUid.equals(hostUid);
        int ownerPoints = 0;
        int stealerPoints = 0;

        if (n.ownerSolved) {
            ownerPoints = getPointsForAttempt(n.ownerAttempts);
        }
        if (n.stealerSolved) {
            stealerPoints = 10;
        }

        if (ownerIsP1) {
            n.p1Score += ownerPoints;
            n.p2Score += stealerPoints;
        } else {
            n.p1Score += stealerPoints;
            n.p2Score += ownerPoints;
        }
    }

    private int getPointsForAttempt(int attempt) {
        if (attempt <= 2) return 20;
        if (attempt <= 4) return 15;
        return 10;
    }

    private void pushFinalScores(int p1, int p2) {
        if (matchRepo == null) return;
        int myScore = isHost ? p1 : p2;
        matchRepo.setGameScore(matchCode, Match.GAME_SKOCKO, myScore);
    }

    public LiveData<Integer> getTimer() { return timer; }
    public LiveData<String> getRoundInfo() { return roundInfo; }
    public LiveData<String> getStatus() { return status; }
    public LiveData<Boolean> getInputEnabled() { return inputEnabled; }
    public LiveData<String> getAnswerReveal() { return answerReveal; }
    public LiveData<List<GuessResult>> getGuessHistory() { return guessHistory; }
    public LiveData<String[]> getCurrentGuessDisplay() { return currentGuessDisplay; }
    public LiveData<Boolean> getGameOver() { return gameOver; }
    public LiveData<Boolean> getGameFinished() { return gameFinished; }
    public LiveData<Integer> getMyTotal() { return myTotal; }
    public LiveData<Integer> getOpponentTotal() { return opponentTotal; }
    public LiveData<String> getOpponentName() { return opponentName; }
    public LiveData<String> getOpponentUid() { return opponentUid; }
    public boolean isStealPhase() { return isStealPhase; }

    public void cleanup() {
        isFinished = true;
        cancelLocalTimer();
        if (pauseTimer != null) { pauseTimer.cancel(); pauseTimer = null; }
        if (matchRepo != null) matchRepo.detach();
        if (gameRef != null && gameListener != null) gameRef.removeEventListener(gameListener);
    }

    @Override protected void onCleared() {
        super.onCleared();
        cleanup();
    }
}