package com.example.slagalica.ui.mojbroj;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.model.MojBrojQuestion;
import com.example.slagalica.util.ExpressionEvaluator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MojBrojViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<MojBrojQuestion> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int currentRound = 1;
    private int scorePlayer1 = 0;
    private int scorePlayer2 = 0;
    private boolean isFinished = false;
    private boolean isPaused = false;
    private int currentPlayer = 1;
    private boolean player1Played = false;
    private boolean player2Played = false;
    private int player1Result = 0;
    private int player2Result = 0;
    private boolean player1Exact = false;
    private boolean player2Exact = false;
    private int localPlayerRole = 1;

    private final MutableLiveData<Integer> timer = new MutableLiveData<>(60);
    private final MutableLiveData<Integer> currentScore = new MutableLiveData<>(0);
    private final MutableLiveData<String> roundInfo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> gameOver = new MutableLiveData<>(false);
    private final MutableLiveData<String> status = new MutableLiveData<>();
    private final MutableLiveData<Integer> targetNumber = new MutableLiveData<>();
    private final MutableLiveData<List<Integer>> availableNumbers = new MutableLiveData<>();
    private final MutableLiveData<String> currentExpression = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> inputEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<String> answerReveal = new MutableLiveData<>("");
    private final MutableLiveData<String> player1AnswerDisplay = new MutableLiveData<>("P1: ???");
    private final MutableLiveData<String> player2AnswerDisplay = new MutableLiveData<>("P2: ???");
    private String lastTokenType = "NONE"; // "NUMBER", "OP", "LPAREN", "RPAREN"

    private android.os.CountDownTimer countDownTimer;
    private android.os.CountDownTimer pauseTimer;
    private int openParenCount = 0;

    public void startGame() {
        isFinished = false;
        currentRound = 1;
        currentPlayer = 1;
        scorePlayer1 = 0;
        scorePlayer2 = 0;
        currentScore.setValue(0);
        gameOver.setValue(false);
        player1Played = false;
        player2Played = false;
        player1Result = 0;
        player2Result = 0;
        player1Exact = false;
        player2Exact = false;

        db.collection("mojbroj")
                .get()
                .addOnSuccessListener(query -> {
                    questions.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        MojBrojQuestion q = doc.toObject(MojBrojQuestion.class);
                        questions.add(q);
                    }
                    Collections.shuffle(questions);

                    if (!questions.isEmpty()) {
                        startRound();
                    } else {
                        loadFallbackQuestions();
                        startRound();
                    }
                })
                .addOnFailureListener(e -> {
                    loadFallbackQuestions();
                    startRound();
                });
    }

    private void loadFallbackQuestions() {
        questions = new ArrayList<>();
        List<Integer> nums1 = new ArrayList<>();
        nums1.add(1); nums1.add(5); nums1.add(7); nums1.add(9);
        nums1.add(10); nums1.add(25);
        questions.add(new MojBrojQuestion(143, nums1, "(10+5)*7+9-1"));

        List<Integer> nums2 = new ArrayList<>();
        nums2.add(3); nums2.add(4); nums2.add(6); nums2.add(8);
        nums2.add(15); nums2.add(50);
        questions.add(new MojBrojQuestion(312, nums2, "(50+15)*6-8+4"));
    }

    private void startRound() {
        if (isFinished) return;
        player1Played = false;
        player2Played = false;
        player1Result = 0;
        player2Result = 0;
        player1Exact = false;
        player2Exact = false;
        player1AnswerDisplay.setValue("P1: ???");
        player2AnswerDisplay.setValue("P2: ???");
        answerReveal.setValue("");
        currentExpression.setValue("");
        lastTokenType = "NONE";
        openParenCount = 0;

        currentPlayer = (currentRound == 1) ? 1 : 2;

        roundInfo.setValue("Runda " + currentRound + " / 2  •  Prvi: Igrač " + currentPlayer);
        status.setValue("Vi ste Igrač " + localPlayerRole + "  •  Na potezu: Igrač " + currentPlayer);

        MojBrojQuestion q = questions.get(currentQuestionIndex);
        targetNumber.setValue(q.getTarget());
        availableNumbers.setValue(new ArrayList<>(q.getNumbers()));

        startTimer(60);
    }

    private void startTimer(int seconds) {
        if (countDownTimer != null) countDownTimer.cancel();

        timer.setValue(seconds);
        inputEnabled.setValue(true);
        countDownTimer = new android.os.CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isFinished || isPaused) { cancel(); return; }
                timer.setValue((int) (millisUntilFinished / 1000));
            }
            @Override
            public void onFinish() {
                if (isFinished || isPaused) return;
                timer.setValue(0);
                onTimeUp();
            }
        }.start();
    }

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

        if (isRParen && valid) {
            valid = openParenCount > 0;
        }

        if (!valid) return;

        String current = currentExpression.getValue();
        if (current == null) current = "";

        if (isOp || isRParen) {
            currentExpression.setValue(current + " " + token);
        } else {
            currentExpression.setValue(current + token);
        }

        if (isNumber) lastTokenType = "NUMBER";
        else if (isLParen) {
            openParenCount++;
            lastTokenType = "LPAREN";
        } else if (isRParen) {
            openParenCount--;
            lastTokenType = "RPAREN";
        } else lastTokenType = "OP";
    }

    public void clearExpression() {
        currentExpression.setValue("");
        lastTokenType = "NONE";
        openParenCount = 0;
    }

    public void submitExpression() {
        if (isFinished || isPaused) return;
        if (countDownTimer != null) countDownTimer.cancel();

        String expr = currentExpression.getValue();
        if (expr == null || expr.trim().isEmpty()) {
            recordResult(0, false);
            return;
        }

        MojBrojQuestion q = questions.get(currentQuestionIndex);
        ExpressionEvaluator.Result result = ExpressionEvaluator.evaluate(expr, q.getNumbers());

        if (!result.valid) {
            recordResult(0, false);
        } else {
            boolean exact = (result.value == q.getTarget());
            recordResult(result.value, exact);
        }
    }

    private void recordResult(int value, boolean exact) {
        if (currentPlayer == 1) {
            player1Result = value;
            player1Exact = exact;
            player1Played = true;
            player1AnswerDisplay.setValue("P1: " + value + (exact ? " ✓" : ""));
        } else {
            player2Result = value;
            player2Exact = exact;
            player2Played = true;
            player2AnswerDisplay.setValue("P2: " + value + (exact ? " ✓" : ""));
        }

        int other = (currentPlayer == 1) ? 2 : 1;
        boolean otherPlayed = (other == 1) ? player1Played : player2Played;

        if (!otherPlayed) {
            currentPlayer = other;
            status.setValue("Na potezu: Igrač " + currentPlayer);
            currentExpression.setValue("");
            lastTokenType = "NONE";
            openParenCount = 0;
            startTimer(60);
        } else {
            scoreRound();
        }
    }

    private void scoreRound() {
        MojBrojQuestion q = questions.get(currentQuestionIndex);
        int target = q.getTarget();
        int p1Diff = Math.abs(player1Result - target);
        int p2Diff = Math.abs(player2Result - target);
        int p1Points = 0;
        int p2Points = 0;

        if (player1Exact && player2Exact) {
            p1Points = 10; p2Points = 10;
        } else if (player1Exact) {
            p1Points = 10;
        } else if (player2Exact) {
            p2Points = 10;
        } else if (player1Result == 0 && player2Result == 0) {
            p1Points = 0; p2Points = 0;
        } else if (player1Result == player2Result && player1Result != 0) {
            int owner = (currentRound == 1) ? 1 : 2;
            if (owner == 1) p1Points = 5; else p2Points = 5;
        } else {
            if (p1Diff < p2Diff) p1Points = 5;
            else if (p2Diff < p1Diff) p2Points = 5;
            else {
                int owner = (currentRound == 1) ? 1 : 2;
                if (owner == 1) p1Points = 5; else p2Points = 5;
            }
        }

        scorePlayer1 += p1Points;
        scorePlayer2 += p2Points;
        currentScore.setValue(scorePlayer1 + scorePlayer2);

        String sol = q.getSolution();
        if (sol == null || sol.isEmpty()) sol = "N/A";
        String reveal = "Cilj: " + target + "  |  Rešenje: " + sol +
                "\nP1: " + player1Result + (player1Exact ? " ✓" : "") + " (" + p1Points + "b)  |  " +
                "P2: " + player2Result + (player2Exact ? " ✓" : "") + " (" + p2Points + "b)";
        showAnswerThenAdvance(reveal);
    }

    private void showAnswerThenAdvance(String revealText) {
        if (isFinished) return;
        isPaused = true;
        if (countDownTimer != null) countDownTimer.cancel();

        answerReveal.setValue(revealText);
        status.setValue("Runda završena");
        inputEnabled.setValue(false);

        pauseTimer = new android.os.CountDownTimer(10000, 1000) {
            @Override public void onTick(long ms) {}
            @Override
            public void onFinish() {
                if (isFinished) return;
                isPaused = false;
                answerReveal.setValue("");
                inputEnabled.setValue(true);

                if (currentRound < 2) {
                    currentRound++;
                    currentQuestionIndex++;
                    startRound();
                } else {
                    finishGame();
                }
            }
        }.start();
    }

    private void onTimeUp() {
        if (isFinished || isPaused) return;
        recordResult(0, false);
    }

    private void finishGame() {
        isFinished = true;
        if (countDownTimer != null) countDownTimer.cancel();
        if (pauseTimer != null) pauseTimer.cancel();
        status.setValue("Kraj! Igrač 1: " + scorePlayer1 + " | Igrač 2: " + scorePlayer2);
        gameOver.setValue(true);
    }

    public void cleanup() {
        isFinished = true;
        if (countDownTimer != null) countDownTimer.cancel();
        if (pauseTimer != null) pauseTimer.cancel();
    }
    public void backspace() {
        String expr = currentExpression.getValue();
        if (expr == null || expr.isEmpty()) return;

        String trimmed = expr.trim();
        int len = trimmed.length();
        if (len == 0) return;

        // Check what we're removing
        char lastChar = trimmed.charAt(len - 1);

        int i = len - 1;
        while (i >= 0 && Character.isDigit(trimmed.charAt(i))) i--;
        String newExpr;
        if (i == len - 1) {
            // Single char removed
            newExpr = trimmed.substring(0, len - 1);
        } else {
            // Multi-digit number
            newExpr = trimmed.substring(0, i + 1);
        }

        // Update paren count based on what was removed
        if (lastChar == '(') openParenCount--;
        else if (lastChar == ')') openParenCount++;

        currentExpression.setValue(newExpr);
        updateLastTokenType();
    }
    private void updateLastTokenType() {
        String expr = currentExpression.getValue();
        if (expr == null || expr.trim().isEmpty()) {
            lastTokenType = "NONE";
            openParenCount = 0; // <-- ADD: full reset when empty
            return;
        }
        String trimmed = expr.trim();
        char last = trimmed.charAt(trimmed.length() - 1);
        if (Character.isDigit(last)) lastTokenType = "NUMBER";
        else if (last == '(') lastTokenType = "LPAREN";
        else if (last == ')') lastTokenType = "RPAREN";
        else lastTokenType = "OP";
    }

    public void setLocalPlayerRole(int role) { this.localPlayerRole = role; }

    public LiveData<Integer> getTimer() { return timer; }
    public LiveData<Integer> getCurrentScore() { return currentScore; }
    public LiveData<String> getRoundInfo() { return roundInfo; }
    public LiveData<Boolean> getGameOver() { return gameOver; }
    public LiveData<String> getStatus() { return status; }
    public LiveData<Integer> getTargetNumber() { return targetNumber; }
    public LiveData<List<Integer>> getAvailableNumbers() { return availableNumbers; }
    public LiveData<String> getCurrentExpression() { return currentExpression; }
    public LiveData<Boolean> getInputEnabled() { return inputEnabled; }
    public LiveData<String> getAnswerReveal() { return answerReveal; }
    public LiveData<String> getPlayer1AnswerDisplay() { return player1AnswerDisplay; }
    public LiveData<String> getPlayer2AnswerDisplay() { return player2AnswerDisplay; }
    public int getScorePlayer1() { return scorePlayer1; }
    public int getScorePlayer2() { return scorePlayer2; }
}