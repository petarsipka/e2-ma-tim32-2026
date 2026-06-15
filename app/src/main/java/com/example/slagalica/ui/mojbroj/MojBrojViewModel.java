package com.example.slagalica.ui.mojbroj;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.model.Match;
import com.example.slagalica.data.model.MojBrojQuestion;
import com.example.slagalica.util.ExpressionEvaluator;
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

public class MojBrojViewModel extends ViewModel {

    private final MutableLiveData<Integer> timer = new MutableLiveData<>(60);
    private final MutableLiveData<Integer> currentScore = new MutableLiveData<>(0);
    private final MutableLiveData<String> roundInfo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> gameOver = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> gameFinished = new MutableLiveData<>(false);
    private final MutableLiveData<String> status = new MutableLiveData<>();
    private final MutableLiveData<Integer> targetNumber = new MutableLiveData<>();
    private final MutableLiveData<List<Integer>> availableNumbers = new MutableLiveData<>();
    private final MutableLiveData<String> currentExpression = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> inputEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<String> answerReveal = new MutableLiveData<>("");
    private final MutableLiveData<String> player1AnswerDisplay = new MutableLiveData<>("P1: ???");
    private final MutableLiveData<String> player2AnswerDisplay = new MutableLiveData<>("P2: ???");

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

    private List<MojBrojQuestion> questions = new ArrayList<>();
    private boolean isFinished = false;
    private android.os.CountDownTimer countDownTimer;
    private android.os.CountDownTimer pauseTimer;

    private String lastTokenType = "NONE";
    private int openParenCount = 0;
    private int lastAppliedRound = -1;
    private int lastTimerRound = -1;
    private String lastTimerPhase = "";
    private final MutableLiveData<boolean[]> usedIndices = new MutableLiveData<>(new boolean[6]);
    private final List<Integer> indexStack = new ArrayList<>(); // To track which index to restore on backspace


    public static class State {
        public int round = 1;
        public String phase = "init";      // init -> inputting -> reveal -> finished
        public int target1 = 0;
        public List<Integer> numbers1 = new ArrayList<>();
        public int target2 = 0;
        public List<Integer> numbers2 = new ArrayList<>();
        // Player inputs (hidden from each other during inputting)
        public String p1Expression = "";
        public int p1Result = 0;
        public boolean p1Exact = false;
        public boolean p1Submitted = false;
        public String p2Expression = "";
        public int p2Result = 0;
        public boolean p2Exact = false;
        public boolean p2Submitted = false;
        public int p1Score = 0;
        public int p2Score = 0;
        public String solution1 = "";
        public String solution2 = "";

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
        gameRef = db.getReference("matches").child(code).child("mojbroj");

        loadQuestions();
    }

    private void loadQuestions() {
        FirebaseFirestore.getInstance().collection("mojbroj")
                .get()
                .addOnSuccessListener(query -> {
                    questions.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        questions.add(doc.toObject(MojBrojQuestion.class));
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
        List<Integer> n1 = new ArrayList<>();
        n1.add(1); n1.add(5); n1.add(7); n1.add(9); n1.add(10); n1.add(25);
        questions.add(new MojBrojQuestion(143, n1, "(10+5)*7+9-1"));

        List<Integer> n2 = new ArrayList<>();
        n2.add(3); n2.add(4); n2.add(6); n2.add(8); n2.add(15); n2.add(50);
        questions.add(new MojBrojQuestion(312, n2, "(50+15)*6-8+4"));
    }

    private void initializeState() {
        State s = new State();
        s.round = 1;
        s.phase = "inputting";
        MojBrojQuestion q1 = questions.get(0);
        MojBrojQuestion q2 = questions.get(1);
        s.target1 = q1.getTarget();
        s.numbers1 = new ArrayList<>(q1.getNumbers());
        s.solution1 = q1.getSolution();
        s.target2 = q2.getTarget();
        s.numbers2 = new ArrayList<>(q2.getNumbers());
        s.solution2 = q2.getSolution();
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
        n.target1 = s.target1;
        if (s.numbers1 != null) n.numbers1 = new ArrayList<>(s.numbers1);
        n.target2 = s.target2;
        if (s.numbers2 != null) n.numbers2 = new ArrayList<>(s.numbers2);
        n.p1Expression = s.p1Expression;
        n.p1Result = s.p1Result;
        n.p1Exact = s.p1Exact;
        n.p1Submitted = s.p1Submitted;
        n.p2Expression = s.p2Expression;
        n.p2Result = s.p2Result;
        n.p2Exact = s.p2Exact;
        n.p2Submitted = s.p2Submitted;
        n.p1Score = s.p1Score;
        n.p2Score = s.p2Score;
        n.solution1 = s.solution1;
        n.solution2 = s.solution2;
        return n;
    }

    private void applyState(State s) {
        if (isFinished) return;

        if (s.round != lastAppliedRound) {
            lastAppliedRound = s.round;
            answerReveal.setValue("");
            usedIndices.setValue(new boolean[6]);
            indexStack.clear();
        }

        int target = s.round == 1 ? s.target1 : s.target2;
        List<Integer> nums = s.round == 1 ? s.numbers1 : s.numbers2;
        targetNumber.setValue(target);
        availableNumbers.setValue(nums);

        roundInfo.setValue("Runda " + s.round + " / 2");
        player1AnswerDisplay.setValue("P1: " + ("reveal".equals(s.phase) || "finished".equals(s.phase) ? s.p1Expression + " = " + s.p1Result : "???"));
        player2AnswerDisplay.setValue("P2: " + ("reveal".equals(s.phase) || "finished".equals(s.phase) ? s.p2Expression + " = " + s.p2Result : "???"));

        if ("finished".equals(s.phase)) {
            status.setValue("Kraj! P1: " + s.p1Score + " | P2: " + s.p2Score);
            inputEnabled.setValue(false);
            gameOver.setValue(true);
            gameFinished.setValue(true);
            pushFinalScores(s.p1Score, s.p2Score);
            return;
        } else if ("reveal".equals(s.phase)) {
            inputEnabled.setValue(false);
            computeAndSetReveal(s);
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
        } if ("inputting".equals(s.phase)) {
            status.setValue("Unesite izraz...");
            boolean iAmP1 = myUid.equals(hostUid);
            boolean alreadySubmitted = iAmP1 ? s.p1Submitted : s.p2Submitted;
            inputEnabled.setValue(!alreadySubmitted);

            if (s.round != lastTimerRound || !s.phase.equals(lastTimerPhase)) {
                lastTimerRound = s.round;
                lastTimerPhase = s.phase;
                startLocalTimer(60, s);
            }
        }
    }

    private void startLocalTimer(int seconds, State snapshot) {
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
                onLocalTimeUp();
            }
        }.start();
    }

    private void cancelLocalTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void onLocalTimeUp() {
        gameRef.get().addOnSuccessListener(snap -> {
            State s = snap.getValue(State.class);
            if (s == null || isFinished) return;

            boolean iAmP1 = myUid.equals(hostUid);

            if (iAmP1 && !s.p1Submitted) {
                s.p1Submitted = true;
                s.p1Result = 0;
                s.p1Exact = false;
            } else if (!iAmP1 && !s.p2Submitted) {
                s.p2Submitted = true;
                s.p2Result = 0;
                s.p2Exact = false;
            }

            if (s.p1Submitted && s.p2Submitted) {
                s.phase = "reveal";
                computeRoundScores(s);
            }

            gameRef.setValue(s);
        });
    }

    private void advanceFromReveal(State s) {
        answerReveal.setValue("");
        if (s.round < 2) {
            State n = new State();
            n.round = 2;
            n.phase = "inputting";
            n.target1 = s.target1;
            n.numbers1 = s.numbers1;
            n.target2 = s.target2;
            n.numbers2 = s.numbers2;
            n.p1Score = s.p1Score;
            n.p2Score = s.p2Score;
            n.solution1 = s.solution1;
            n.solution2 = s.solution2;
            gameRef.setValue(n);
        } else {
            State n = copy(s);
            n.phase = "finished";
            gameRef.setValue(n);
            gameFinished.setValue(true);
        }
    }

    private void computeAndSetReveal(State s) {
        int target = s.round == 1 ? s.target1 : s.target2;
        int p1Diff = Math.abs(s.p1Result - target);
        int p2Diff = Math.abs(s.p2Result - target);
        int p1Pts = 0, p2Pts = 0;
        int roundOwner = s.round;

        if (s.p1Exact && s.p2Exact) { p1Pts = 10; p2Pts = 10; }
        else if (s.p1Exact) { p1Pts = 10; }
        else if (s.p2Exact) { p2Pts = 10; }
        else if (s.p1Result == 0 && s.p2Result == 0) { p1Pts = 0; p2Pts = 0; }
        else if (s.p1Result == s.p2Result && s.p1Result != 0) {
            if (roundOwner == 1) p1Pts = 5; else p2Pts = 5;
        } else {
            if (p1Diff < p2Diff) p1Pts = 5;
            else if (p2Diff < p1Diff) p2Pts = 5;
            else {
                if (roundOwner == 1) p1Pts = 5; else p2Pts = 5;
            }
        }

        String sol = s.round == 1 ? s.solution1 : s.solution2;
        if (sol == null || sol.isEmpty()) sol = "N/A";
        String reveal = "Cilj: " + target + "  |  Rešenje: " + sol + "\n" +
                "P1: " + s.p1Result + (s.p1Exact ? " ✓" : "") + " (" + p1Pts + "b)  |  " +
                "P2: " + s.p2Result + (s.p2Exact ? " ✓" : "") + " (" + p2Pts + "b)";
        answerReveal.setValue(reveal);
    }

    private void computeRoundScores(State n) {
        int target = n.round == 1 ? n.target1 : n.target2;
        int p1Diff = Math.abs(n.p1Result - target);
        int p2Diff = Math.abs(n.p2Result - target);
        int p1Pts = 0, p2Pts = 0;

        // Round owner: round 1 = P1 (host), round 2 = P2 (guest)
        int roundOwner = n.round;  // 1 or 2

        if (n.p1Exact && n.p2Exact) {
            p1Pts = 10; p2Pts = 10;
        } else if (n.p1Exact) {
            p1Pts = 10;
        } else if (n.p2Exact) {
            p2Pts = 10;
        } else if (n.p1Result == 0 && n.p2Result == 0) {
            p1Pts = 0; p2Pts = 0;
        } else if (n.p1Result == n.p2Result && n.p1Result != 0) {
            // Same non-zero result: round owner gets 5
            if (roundOwner == 1) p1Pts = 5; else p2Pts = 5;
        } else {
            if (p1Diff < p2Diff) p1Pts = 5;
            else if (p2Diff < p1Diff) p2Pts = 5;
            else {
                // Same difference (both off by same amount): round owner gets 5
                if (roundOwner == 1) p1Pts = 5; else p2Pts = 5;
            }
        }

        n.p1Score += p1Pts;
        n.p2Score += p2Pts;
    }

    // ── Expression builder (local only) ──

    public void appendToExpression(String token) {
        boolean isNumber = token.matches("\\d+");
        boolean isOp = token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/");
        boolean isLParen = token.equals("(");
        boolean isRParen = token.equals(")");

        boolean valid = false;
        if (lastTokenType.equals("NONE") || lastTokenType.equals("OP") || lastTokenType.equals("LPAREN")) {
            valid = isNumber || isLParen;
        } else if (lastTokenType.equals("NUMBER") || lastTokenType.equals("RPAREN")) {
            valid = isOp || isRParen;
        }
        if (isRParen && valid) valid = openParenCount > 0;
        if (!valid) return;

        String cur = currentExpression.getValue();
        if (cur == null) cur = "";
        if (isOp || isRParen) currentExpression.setValue(cur + " " + token);
        else currentExpression.setValue(cur + token);

        // Add -1 to indexStack for non-number tokens
        if (!isNumber) {
            indexStack.add(-1);
        }

        if (isNumber) lastTokenType = "NUMBER";
        else if (isLParen) { openParenCount++; lastTokenType = "LPAREN"; }
        else if (isRParen) { openParenCount--; lastTokenType = "RPAREN"; }
        else lastTokenType = "OP";
    }

    public void clearExpression() {
        currentExpression.setValue("");
        usedIndices.setValue(new boolean[6]);
        indexStack.clear();
        lastTokenType = "NONE";
        openParenCount = 0;
    }

    private void clearExpressionLocal() {
        currentExpression.setValue("");
        lastTokenType = "NONE";
        openParenCount = 0;
    }
    public void appendNumber(int index) {
        List<Integer> numbers = availableNumbers.getValue();
        if (numbers == null || index >= numbers.size()) return;

        boolean[] used = usedIndices.getValue();
        if (used == null || used[index]) return;

        // Check if a number is valid at this position (logic from appendToExpression)
        if (!(lastTokenType.equals("NONE") || lastTokenType.equals("OP") || lastTokenType.equals("LPAREN"))) {
            return;
        }

        String token = String.valueOf(numbers.get(index));
        appendToExpression(token);

        // Update usage tracking
        used[index] = true;
        usedIndices.setValue(used);
        indexStack.add(index);
    }
    public void backspace() {
        String expr = currentExpression.getValue();
        if (expr == null || expr.isEmpty() ||indexStack.isEmpty()) return;
        int lastIdx = indexStack.remove(indexStack.size() - 1);
        if (lastIdx != -1) {
            boolean[] used = usedIndices.getValue();
            if (used != null) {
                used[lastIdx] = false;
                usedIndices.setValue(used);
            }
        }
        String trimmed = expr.trim();
        int len = trimmed.length();
        if (len == 0) return;
        char lastChar = trimmed.charAt(len - 1);
        int i = len - 1;
        while (i >= 0 && Character.isDigit(trimmed.charAt(i))) i--;
        String newExpr = (i == len - 1) ? trimmed.substring(0, len - 1) : trimmed.substring(0, i + 1);
        if (lastChar == '(') openParenCount--;
        else if (lastChar == ')') openParenCount++;
        currentExpression.setValue(newExpr);
        updateLastTokenType();
    }

    private void updateLastTokenType() {
        String expr = currentExpression.getValue();
        if (expr == null || expr.trim().isEmpty()) {
            lastTokenType = "NONE";
            openParenCount = 0;
            return;
        }
        String trimmed = expr.trim();
        char last = trimmed.charAt(trimmed.length() - 1);
        if (Character.isDigit(last)) lastTokenType = "NUMBER";
        else if (last == '(') lastTokenType = "LPAREN";
        else if (last == ')') lastTokenType = "RPAREN";
        else lastTokenType = "OP";
    }

    public void submitExpression() {
        if (matchCode == null) return;
        gameRef.get().addOnSuccessListener(snap -> {
            State s = snap.getValue(State.class);
            if (s == null || !"inputting".equals(s.phase)) return;
            boolean iAmP1 = myUid.equals(hostUid);
            if (iAmP1 && s.p1Submitted) return;
            if (!iAmP1 && s.p2Submitted) return;

            String expr = currentExpression.getValue();
            boolean empty = (expr == null || expr.trim().isEmpty());
            List<Integer> allowedNumbers = s.round == 1 ? s.numbers1 : s.numbers2;
            int target = s.round == 1 ? s.target1 : s.target2;
            ExpressionEvaluator.Result result = empty ? new ExpressionEvaluator.Result(0, false, "", new ArrayList<>())
                    : ExpressionEvaluator.evaluate(expr, allowedNumbers);
            State n = copy(s);

            if (iAmP1) {
                n.p1Expression = empty ? "" : expr;
                n.p1Result = result.valid ? result.value : 0;
                n.p1Exact = result.valid && result.value == target;
                n.p1Submitted = true;
            } else {
                n.p2Expression = empty ? "" : expr;
                n.p2Result = result.valid ? result.value : 0;
                n.p2Exact = result.valid && result.value == target;
                n.p2Submitted = true;
            }

            if (n.p1Submitted && n.p2Submitted) {
                n.phase = "reveal";
                computeRoundScores(n);
            }

            cancelLocalTimer();
            gameRef.setValue(n);
            clearExpressionLocal();
        });
    }

    private void pushFinalScores(int p1, int p2) {
        if (matchRepo == null) return;
        int myScore = isHost ? p1 : p2;
        matchRepo.setGameScore(matchCode, Match.GAME_MOJBROJ, myScore);
    }

    public LiveData<Integer> getTimer() { return timer; }
    public LiveData<Integer> getCurrentScore() { return currentScore; }
    public LiveData<String> getRoundInfo() { return roundInfo; }
    public LiveData<Boolean> getGameOver() { return gameOver; }
    public LiveData<Boolean> getGameFinished() { return gameFinished; }
    public LiveData<String> getStatus() { return status; }
    public LiveData<Integer> getTargetNumber() { return targetNumber; }
    public LiveData<List<Integer>> getAvailableNumbers() { return availableNumbers; }
    public LiveData<String> getCurrentExpression() { return currentExpression; }
    public LiveData<Boolean> getInputEnabled() { return inputEnabled; }
    public LiveData<String> getAnswerReveal() { return answerReveal; }
    public LiveData<String> getPlayer1AnswerDisplay() { return player1AnswerDisplay; }
    public LiveData<String> getPlayer2AnswerDisplay() { return player2AnswerDisplay; }
    public LiveData<Integer> getMyTotal() { return myTotal; }
    public LiveData<Integer> getOpponentTotal() { return opponentTotal; }
    public LiveData<String> getOpponentName() { return opponentName; }
    public LiveData<String> getOpponentUid() { return opponentUid; }
    public MutableLiveData<boolean[]> getUsedIndices() { return usedIndices; }

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