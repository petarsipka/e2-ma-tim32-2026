package com.example.slagalica.ui.kpk;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.model.Match;
import com.example.slagalica.data.model.KPKQuestion;
import com.example.slagalica.util.TextNormalizer;
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

public class KPKViewModel extends ViewModel {

    private final MutableLiveData<String> currentHint = new MutableLiveData<>();
    private final MutableLiveData<Integer> timer = new MutableLiveData<>(70);
    private final MutableLiveData<Integer> currentScore = new MutableLiveData<>(0);
    private final MutableLiveData<String> roundInfo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> gameOver = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> gameFinished = new MutableLiveData<>(false);
    private final MutableLiveData<String> opponentStatus = new MutableLiveData<>();
    private final MutableLiveData<List<String>> allHints = new MutableLiveData<>();
    private final MutableLiveData<Integer> revealedHintCount = new MutableLiveData<>(0);
    private final MutableLiveData<String> answerReveal = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> inputEnabled = new MutableLiveData<>(true);

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

    private List<KPKQuestion> questions = new ArrayList<>();
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
        public String answer1 = "";
        public String answer2 = "";
        public List<String> hints1 = new ArrayList<>();
        public List<String> hints2 = new ArrayList<>();
        public int currentStep = 0;
        public boolean ownerSolved = false;
        public boolean ownerDone = false;
        public boolean stealDone = false;
        public boolean stealerSolved = false;
        public int p1Score = 0;
        public int p2Score = 0;



        public State() {}
    }

    public void init(String code, boolean host) {
        matchCode = code;
        isHost = host;
        if (code == null) return;

        matchRepo = new MatchRepository();
        myUid = matchRepo.currentUid();
        matchRepo.listen(code, this::onMatchUpdate);

        FirebaseDatabase db = FirebaseDatabase.getInstance(MatchRepository.DB_URL);
        gameRef = db.getReference("matches").child(code).child("kpk");

        loadQuestions();
    }

    private void loadQuestions() {
        FirebaseFirestore.getInstance().collection("korakpokorak")
                .get()
                .addOnSuccessListener(query -> {
                    questions.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        questions.add(doc.toObject(KPKQuestion.class));
                    }
                    Collections.shuffle(questions);
                    if (questions.size() < 2) loadFallback();
                    if (isHost) initializeState();
                    attachListener();
                })
                .addOnFailureListener(e -> {
                    loadFallback();
                    if (isHost) initializeState();
                    attachListener();
                });
    }

    private void loadFallback() {
        questions = new ArrayList<>();
        List<String> h1 = new ArrayList<>();
        h1.add("Životinja"); h1.add("Najveći kopneni sisar"); h1.add("Ima dugačku surlu");
        h1.add("Afrika i Azija"); h1.add("Sivi ili afrički"); h1.add("Dumbo"); h1.add("Pamtiti kao ___");
        questions.add(new KPKQuestion("Slon", h1));

        List<String> h2 = new ArrayList<>();
        h2.add("Reka"); h2.add("Protiče kroz Novi Sad"); h2.add("Druga najduža reka u Evropi");
        h2.add("Ušće u Crno more"); h2.add("Bečka opera nosi njeno ime"); h2.add("Protiče kroz 10 zemalja"); h2.add("U Beogradu deli ušće sa Savom");
        questions.add(new KPKQuestion("Dunav", h2));
    }

    private void initializeState() {
        State s = new State();
        s.round = 1;
        s.phase = "playing";
        s.activeUid = myUid;
        s.roundOwnerUid = myUid;
        KPKQuestion q1 = questions.get(0);
        KPKQuestion q2 = questions.get(1);
        s.answer1 = q1.getAnswer();
        s.answer2 = q2.getAnswer();
        s.hints1 = new ArrayList<>(q1.getHints());
        s.hints2 = new ArrayList<>(q2.getHints());
        s.currentStep = 1;
        gameRef.setValue(s);
    }

    private void attachListener() {
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
        n.answer1 = s.answer1;
        n.answer2 = s.answer2;
        if (s.hints1 != null) n.hints1 = new ArrayList<>(s.hints1);
        if (s.hints2 != null) n.hints2 = new ArrayList<>(s.hints2);
        n.currentStep = s.currentStep;
        n.ownerSolved = s.ownerSolved;
        n.ownerDone = s.ownerDone;
        n.stealDone = s.stealDone;
        n.stealerSolved = s.stealerSolved;
        n.p1Score = s.p1Score;
        n.p2Score = s.p2Score;
        return n;
    }

    private void applyState(State s) {
        if (isFinished) return;
        if (s.round != lastAppliedRound) {
            lastAppliedRound = s.round;
            answerReveal.setValue("");
        }
        boolean myTurn = myUid.equals(s.activeUid);

        List<String> hints = new ArrayList<>();
        List<String> source = s.round == 1 ? s.hints1 : s.hints2;
        for (int i = 0; i < s.currentStep && i < source.size(); i++) {
            hints.add(source.get(i));
        }
        allHints.setValue(hints);
        revealedHintCount.setValue(s.currentStep);
        if (!hints.isEmpty()) currentHint.setValue(hints.get(hints.size() - 1));

        roundInfo.setValue("Runda " + s.round + " / 2");
        currentScore.setValue(s.p1Score + s.p2Score);

        if ("finished".equals(s.phase)) {
            lastTimerRound = -1;
            lastTimerPhase = "";
            status("Kraj! P1: " + s.p1Score + " | P2: " + s.p2Score);
            inputEnabled.setValue(false);
            gameOver.setValue(true);
            gameFinished.setValue(true);
            pushFinalScores(s.p1Score, s.p2Score);
            return;
        } else if ("reveal".equals(s.phase)) {
            lastTimerRound = -1;
            lastTimerPhase = "";
            String ans = s.round == 1 ? s.answer1 : s.answer2;
            answerReveal.setValue(ans);
            status("Tačno: " + ans);
            inputEnabled.setValue(false);
            if (isHost && pauseTimer == null) {
                pauseTimer = new android.os.CountDownTimer(10000, 1000) {
                    @Override public void onTick(long ms) {}
                    @Override public void onFinish() {
                        pauseTimer = null;
                        advanceFromReveal(s);
                    }
                }.start();
            }
            return;
        } if ("playing".equals(s.phase) || "steal".equals(s.phase)) {
            int secs = "steal".equals(s.phase) ? 10 : 70;
            if (s.round != lastTimerRound || !s.phase.equals(lastTimerPhase)) {
                lastTimerRound = s.round;
                lastTimerPhase = s.phase;
                startLocalTimer(secs, s);
            }
        } else {
            lastTimerRound = -1;
            lastTimerPhase = "";
            cancelLocalTimer();
        }

        inputEnabled.setValue(myTurn && ("playing".equals(s.phase) || "steal".equals(s.phase)));

        if (myTurn && ("playing".equals(s.phase) || "steal".equals(s.phase))) {
            int secs = "steal".equals(s.phase) ? 10 : 70;
            if (s.round != lastTimerRound || !s.phase.equals(lastTimerPhase)) {
                lastTimerRound = s.round;
                lastTimerPhase = s.phase;
                startLocalTimer(secs, s);
            }
        } else {
            lastTimerRound = -1;
            lastTimerPhase = "";
            cancelLocalTimer();
        }
    }

    private void status(String msg) {
        opponentStatus.setValue(msg);
    }

    private void startLocalTimer(int seconds, State stateSnapshot) {
        cancelLocalTimer();
        timer.setValue(seconds);
        countDownTimer = new android.os.CountDownTimer(seconds * 1000L, 1000) {
            private int lastElapsedRevealed = -1;

            @Override public void onTick(long ms) {
                if (isFinished) { cancel(); return; }
                int rem = (int) (ms / 1000);
                timer.setValue(rem);
                int elapsed = seconds - rem;

                // Trigger reveal every 10s (at 60s, 50s, 40s, 30s, 20s, 10s left)
                if ("playing".equals(stateSnapshot.phase) && elapsed > 0 && elapsed % 10 == 0 && elapsed != lastElapsedRevealed) {
                    lastElapsedRevealed = elapsed;
                    revealHintInState();
                }
            }
            @Override public void onFinish() {
                if (isFinished) return;
                timer.setValue(0);
                onLocalTimeUp();
            }
        }.start();
    }

    private void revealHintInState() {
        gameRef.get().addOnSuccessListener(snap -> {
            State s = snap.getValue(State.class);
            if (s == null || isFinished || !"playing".equals(s.phase)) return;
            if (!myUid.equals(s.activeUid)) return; // Only active player pushes the update

            if (s.currentStep < 7) {
                State n = copy(s);
                n.currentStep++;
                gameRef.setValue(n);
            }
        });
    }

    private void onLocalTimeUp() {
        gameRef.get().addOnSuccessListener(snap -> {
            State s = snap.getValue(State.class);
            if (s == null || isFinished) return;
            if (!myUid.equals(s.activeUid)) return;

            State n = copy(s);
            if ("playing".equals(n.phase)) {
                n.ownerDone = true;
                n.phase = "steal";
                n.activeUid = n.roundOwnerUid.equals(hostUid) ? oppUid : hostUid;
            } else if ("steal".equals(n.phase)) {
                n.stealDone = true;
                n.phase = "reveal";
                n.activeUid = "";
                computeRoundScores(n);
            }
            gameRef.setValue(n);
        });
    }



    private void cancelLocalTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }



    private void advanceFromReveal(State s) {
        if (s.round < 2) {
            State n = new State();
            n.round = 2;
            n.phase = "playing";
            n.roundOwnerUid = oppUid;
            n.activeUid = oppUid;
            n.answer1 = s.answer1;
            n.answer2 = s.answer2;
            n.hints1 = s.hints1;
            n.hints2 = s.hints2;
            n.p1Score = s.p1Score;
            n.p2Score = s.p2Score;
            n.currentStep = 1;
            gameRef.setValue(n);
        } else {
            State n = copy(s);
            n.phase = "finished";
            n.activeUid = "";
            gameRef.setValue(n);
        }
    }

    public void submitAnswer(String answer) {
        if (matchCode == null) return;
        gameRef.get().addOnSuccessListener(snap -> {
            State s = snap.getValue(State.class);
            if (s == null || !myUid.equals(s.activeUid)) return;

            String correct = s.round == 1 ? s.answer1 : s.answer2;
            boolean correctAnswer = TextNormalizer.matches(answer, correct);
            State n = copy(s);

            if ("playing".equals(s.phase)) {
                if (correctAnswer) {
                    n.ownerSolved = true;
                    n.ownerDone = true;
                    n.phase = "reveal";
                    n.activeUid = "";
                    computeRoundScores(n);
                    cancelLocalTimer();
                }
                // FIX: Removed else block — wrong answer does nothing, owner keeps trying
            } else if ("steal".equals(s.phase)) {
                if (correctAnswer) n.stealerSolved = true;
                n.stealDone = true;
                n.phase = "reveal";
                n.activeUid = "";
                computeRoundScores(n);
                cancelLocalTimer();
            }

            gameRef.setValue(n);
        });
    }

    private void computeRoundScores(State n) {
        boolean ownerIsP1 = n.roundOwnerUid.equals(hostUid);
        int ownerPoints = 0;
        int stealerPoints = 0;

        if (n.ownerSolved) {
            int stepUsed = Math.min(n.currentStep, 7);
            ownerPoints = Math.max(20 - (stepUsed - 1) * 2, 8);
        }
        if (n.stealerSolved) {
            stealerPoints = 5;
        }

        if (ownerIsP1) {
            n.p1Score += ownerPoints;
            n.p2Score += stealerPoints;
        } else {
            n.p1Score += stealerPoints;
            n.p2Score += ownerPoints;
        }
        currentScore.setValue(n.p1Score + n.p2Score);

    }

    private void pushFinalScores(int p1, int p2) {
        if (matchRepo == null) return;
        int myScore = isHost ? p1 : p2;
        matchRepo.setGameScore(matchCode, Match.GAME_KPK, myScore);
    }

    public LiveData<String> getCurrentHint() { return currentHint; }
    public LiveData<Integer> getTimer() { return timer; }
    public LiveData<Integer> getCurrentScore() { return currentScore; }
    public LiveData<String> getRoundInfo() { return roundInfo; }
    public LiveData<Boolean> getGameOver() { return gameOver; }
    public LiveData<Boolean> getGameFinished() { return gameFinished; }
    public LiveData<String> getOpponentStatus() { return opponentStatus; }
    public LiveData<List<String>> getAllHints() { return allHints; }
    public LiveData<Integer> getRevealedHintCount() { return revealedHintCount; }
    public LiveData<Boolean> getInputEnabled() { return inputEnabled; }
    public LiveData<String> getAnswerReveal() { return answerReveal; }
    public LiveData<Integer> getMyTotal() { return myTotal; }
    public LiveData<Integer> getOpponentTotal() { return opponentTotal; }
    public LiveData<String> getOpponentName() { return opponentName; }
    public LiveData<String> getOpponentUid() { return opponentUid; }

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