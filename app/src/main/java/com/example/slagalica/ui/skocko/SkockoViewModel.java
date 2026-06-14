package com.example.slagalica.ui.skocko;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.model.SkockoQuestion;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkockoViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<SkockoQuestion> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int currentRound = 1;
    private int scorePlayer1 = 0;
    private int scorePlayer2 = 0;
    private boolean isFinished = false;
    private boolean isPaused = false;
    private int currentPlayer = 1;
    private boolean player1Done = false;
    private boolean player2Done = false;
    private boolean player1Solved = false;
    private boolean player2Solved = false;
    private int player1Attempts = 0;
    private int player2Attempts = 0;
    private boolean isStealPhase = false;
    private int localPlayerRole = 1;

    private String[] currentGuess = new String[4];
    private int guessPosition = 0;
    private int currentAttemptRow = 0; // 0-5 for main rows, 6 for steal row

    private final MutableLiveData<Integer> timer = new MutableLiveData<>(30);
    private final MutableLiveData<Integer> currentScore = new MutableLiveData<>(0);
    private final MutableLiveData<String> roundInfo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> gameOver = new MutableLiveData<>(false);
    private final MutableLiveData<String> status = new MutableLiveData<>();
    private final MutableLiveData<Boolean> inputEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<String> answerReveal = new MutableLiveData<>("");
    private final MutableLiveData<List<GuessResult>> guessHistory = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String[]> currentGuessDisplay = new MutableLiveData<>(new String[]{"", "", "", ""});

    private android.os.CountDownTimer countDownTimer;
    private android.os.CountDownTimer pauseTimer;

    public static class GuessResult {
        public final String[] guess;
        public final int exact;
        public final int partial;
        public final int rowIndex; // which row this guess appears on

        public GuessResult(String[] guess, int exact, int partial, int rowIndex) {
            this.guess = guess.clone();
            this.exact = exact;
            this.partial = partial;
            this.rowIndex = rowIndex;
        }
    }

    public void startGame() {
        isFinished = false;
        currentRound = 1;
        currentQuestionIndex = 0;
        scorePlayer1 = 0;
        scorePlayer2 = 0;
        currentScore.setValue(0);
        gameOver.setValue(false);
        answerReveal.setValue("");

        db.collection("skocko")
                .get()
                .addOnSuccessListener(query -> {
                    questions.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        SkockoQuestion q = doc.toObject(SkockoQuestion.class);
                        questions.add(q);
                    }
                    Collections.shuffle(questions);
                    if (questions.isEmpty()) loadFallbackQuestions();
                    startRound();
                })
                .addOnFailureListener(e -> {
                    loadFallbackQuestions();
                    startRound();
                });
    }

    private void loadFallbackQuestions() {
        questions = new ArrayList<>();
        questions.add(new SkockoQuestion("SCTK"));
        questions.add(new SkockoQuestion("PZKS"));
    }

    private void startRound() {
        if (isFinished) return;

        // Reset everything for new round
        player1Done = false;
        player2Done = false;
        player1Solved = false;
        player2Solved = false;
        player1Attempts = 0;
        player2Attempts = 0;
        isStealPhase = false;
        currentAttemptRow = 0;
        guessHistory.setValue(new ArrayList<>());
        answerReveal.setValue("");
        clearGuess();

        currentPlayer = (currentRound == 1) ? 1 : 2;

        roundInfo.setValue("Runda " + currentRound + " / 2");
        status.setValue("Igrač " + currentPlayer + " na potezu");

        startTimer(30);
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
        currentGuess = new String[4];
        guessPosition = 0;
        updateGuessDisplay();
    }

    private void updateGuessDisplay() {
        String[] display = new String[4];
        for (int i = 0; i < 4; i++) {
            display[i] = currentGuess[i] != null ? currentGuess[i] : "";
        }
        currentGuessDisplay.setValue(display);
    }

    public void submitGuess() {
        if (isFinished || isPaused || guessPosition < 4) return;

        SkockoQuestion q = questions.get(currentQuestionIndex);
        String solution = q.getSolution();

        int exact = 0;
        int partial = 0;
        boolean[] solutionUsed = new boolean[4];
        boolean[] guessUsed = new boolean[4];

        for (int i = 0; i < 4; i++) {
            if (currentGuess[i].equals(String.valueOf(solution.charAt(i)))) {
                exact++;
                solutionUsed[i] = true;
                guessUsed[i] = true;
            }
        }

        for (int i = 0; i < 4; i++) {
            if (guessUsed[i]) continue;
            for (int j = 0; j < 4; j++) {
                if (solutionUsed[j]) continue;
                if (currentGuess[i].equals(String.valueOf(solution.charAt(j)))) {
                    partial++;
                    solutionUsed[j] = true;
                    guessUsed[i] = true;
                    break;
                }
            }
        }

        boolean solved = (exact == 4);
        int attempts = (currentPlayer == 1) ? ++player1Attempts : ++player2Attempts;

        // Record this guess in history at current row
        List<GuessResult> history = guessHistory.getValue();
        if (history == null) history = new ArrayList<>();
        history.add(new GuessResult(currentGuess.clone(), exact, partial, currentAttemptRow));
        guessHistory.setValue(history);

        if (solved) {
            if (countDownTimer != null) countDownTimer.cancel();
            if (currentPlayer == 1) {
                player1Solved = true; player1Done = true;
            } else {
                player2Solved = true; player2Done = true;
            }
            endPlayerTurn();
        } else {
            currentAttemptRow++;
            if (isStealPhase) {
                // Steal failed
                if (countDownTimer != null) countDownTimer.cancel();
                if (currentPlayer == 1) player1Done = true; else player2Done = true;
                endPlayerTurn();
            } else if (attempts >= 6) {
                // Out of attempts, go to steal or end
                if (countDownTimer != null) countDownTimer.cancel();
                if (currentPlayer == 1) player1Done = true; else player2Done = true;
                startStealOrEnd();
            } else {
                // Continue guessing
                clearGuess();
                status.setValue("Igrač " + currentPlayer + " • Pokušaj " + (attempts + 1) + "/6");
            }
        }
    }

    private void startStealOrEnd() {
        int other = (currentPlayer == 1) ? 2 : 1;
        boolean otherDone = (other == 1) ? player1Done : player2Done;

        if (!otherDone) {
            // Start steal phase
            isStealPhase = true;
            currentPlayer = other;
            currentAttemptRow = 6; // steal row
            clearGuess();
            status.setValue("Igrač " + currentPlayer + " krade (1 pokušaj, 10s)");
            startTimer(10);
        } else {
            endPlayerTurn();
        }
    }

    private void onTimeUp() {
        if (isFinished || isPaused) return;

        if (currentPlayer == 1) { player1Done = true; if (player1Attempts == 0) player1Attempts = 6; }
        else { player2Done = true; if (player2Attempts == 0) player2Attempts = 6; }

        if (isStealPhase) {
            endPlayerTurn();
        } else {
            startStealOrEnd();
        }
    }

    private void endPlayerTurn() {
        if (countDownTimer != null) countDownTimer.cancel();

        // Calculate points
        int p1Points = 0, p2Points = 0;
        if (player1Solved) p1Points = getPointsForAttempt(player1Attempts);
        if (player2Solved) p2Points = getPointsForAttempt(player2Attempts);

        // Steal bonus: if solved on steal (attempt 1 during steal phase)
        if (isStealPhase && ((currentPlayer == 1 && player1Solved) || (currentPlayer == 2 && player2Solved))) {
            if (currentPlayer == 1) { p1Points = 10; p2Points = 0; }
            else { p2Points = 10; p1Points = 0; }
        }

        scorePlayer1 += p1Points;
        scorePlayer2 += p2Points;
        currentScore.setValue(scorePlayer1 + scorePlayer2);

        // Show answer
        SkockoQuestion q = questions.get(currentQuestionIndex);
        answerReveal.setValue(q.getSolution());

        status.setValue("P1: " + p1Points + "b | P2: " + p2Points + "b");
        inputEnabled.setValue(false);

        // Pause then next round or game over
        pauseTimer = new android.os.CountDownTimer(8000, 1000) {
            @Override public void onTick(long ms) {}
            @Override
            public void onFinish() {
                if (isFinished) return;
                answerReveal.setValue("");
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

    private int getPointsForAttempt(int attempt) {
        if (attempt <= 2) return 20;
        if (attempt <= 4) return 15;
        return 10;
    }

    private void finishGame() {
        isFinished = true;
        if (countDownTimer != null) countDownTimer.cancel();
        if (pauseTimer != null) pauseTimer.cancel();
        status.setValue("Kraj! P1: " + scorePlayer1 + " | P2: " + scorePlayer2);
        gameOver.setValue(true);
    }

    public void cleanup() {
        isFinished = true;
        if (countDownTimer != null) countDownTimer.cancel();
        if (pauseTimer != null) pauseTimer.cancel();
    }

    public void setLocalPlayerRole(int role) { this.localPlayerRole = role; }

    public LiveData<Integer> getTimer() { return timer; }
    public LiveData<Integer> getCurrentScore() { return currentScore; }
    public LiveData<String> getRoundInfo() { return roundInfo; }
    public LiveData<Boolean> getGameOver() { return gameOver; }
    public LiveData<String> getStatus() { return status; }
    public LiveData<Boolean> getInputEnabled() { return inputEnabled; }
    public LiveData<String> getAnswerReveal() { return answerReveal; }
    public LiveData<List<GuessResult>> getGuessHistory() { return guessHistory; }
    public LiveData<String[]> getCurrentGuessDisplay() { return currentGuessDisplay; }
    public int getScorePlayer1() { return scorePlayer1; }
    public int getScorePlayer2() { return scorePlayer2; }
    public boolean isStealPhase() { return isStealPhase; }
}