package com.example.slagalica.ui.kpk;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.model.KPKQuestion;
import com.example.slagalica.util.TextNormalizer;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KPKViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<KPKQuestion> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int currentRound = 1;        // 1 or 2
    private int currentStep = 0;         // 0-6 (which hint is being revealed)
    private int scorePlayer1 = 0;
    private int scorePlayer2 = 0;
    private int currentPlayer = 1;       // Whose turn to play the 70s round
    private boolean isStealPhase = false;
    private boolean isFinished = false;

    // For local testing: if true, we simulate both players on one device
    private boolean localTestMode = true;
    private int testPlayerRole = 1;      // Which player "we" are in test mode

    private final MutableLiveData<String> currentHint = new MutableLiveData<>();
    private final MutableLiveData<Integer> timer = new MutableLiveData<>(70);
    private final MutableLiveData<Integer> currentScore = new MutableLiveData<>(0);
    private final MutableLiveData<String> roundInfo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> gameOver = new MutableLiveData<>(false);
    private final MutableLiveData<String> opponentStatus = new MutableLiveData<>();
    private final MutableLiveData<List<String>> allHints = new MutableLiveData<>(); // For UI to display all at once
    private final MutableLiveData<Integer> revealedHintCount = new MutableLiveData<>(0);
    private final MutableLiveData<String> answerReveal = new MutableLiveData<>("");

    private android.os.CountDownTimer countDownTimer;

    private boolean isPaused = false;
    private android.os.CountDownTimer pauseTimer;
    private final MutableLiveData<Boolean> inputEnabled = new MutableLiveData<>(true);

    private int localPlayerRole = 1; // 1 or 2

    public void startGame() {
        isFinished = false;
        currentRound = 1;
        currentPlayer = 1;
        scorePlayer1 = 0;
        scorePlayer2 = 0;
        currentScore.setValue(0);
        gameOver.setValue(false);
        isStealPhase = false;

        // Load questions from Firestore
        db.collection("korakpokorak")
                .get()
                .addOnSuccessListener(query -> {
                    questions.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        KPKQuestion q = doc.toObject(KPKQuestion.class);
                        questions.add(q);
                    }
                    Collections.shuffle(questions);

                    if (!questions.isEmpty()) {
                        startRound();
                    } else {
                        opponentStatus.setValue("Nema pitanja u bazi!");
                    }
                })
                .addOnFailureListener(e -> {
                    loadFallbackQuestions();
                    startRound();
                });
    }

    private void loadFallbackQuestions() {
        questions = new ArrayList<>();
        List<String> hints1 = new ArrayList<>();
        hints1.add("Životinja");
        hints1.add("Najveći kopneni sisar");
        hints1.add("Ima dugačku surlu");
        hints1.add("Afrika i Azija");
        hints1.add("Sivi ili afrički");
        hints1.add("Dumbo");
        hints1.add("Pamtiti kao ___");
        questions.add(new KPKQuestion("Slon", hints1));

        List<String> hints2 = new ArrayList<>();
        hints2.add("Reka");
        hints2.add("Protiče kroz Novi Sad");
        hints2.add("Druga najduža reka u Evropi");
        hints2.add("Ušće u Crno more");
        hints2.add("Bečka opera nosi njeno ime");
        hints2.add("Protiče kroz 10 zemalja");
        hints2.add("U Beogradu deli ušće sa Savom");
        questions.add(new KPKQuestion("Dunav", hints2));
    }

    private void startRound() {
        if (isFinished) return;

        currentStep = 0;
        isStealPhase = false;

        // Determine which player starts this round
        currentPlayer = (currentRound == 1) ? 1 : 2;

        answerReveal.setValue(""); // hide answer box when new round starts
        roundInfo.setValue("Runda " + currentRound + " / 2  •  Igrač " + currentPlayer);
        opponentStatus.setValue("Vi ste Igrač " + localPlayerRole + "  •  Na potezu: Igrač " + currentPlayer);

        // Reset revealed hints
        revealedHintCount.setValue(0);
        allHints.setValue(new ArrayList<>());

        // Reveal first hint immediately
        revealNextHint();
        startTimer(70);
    }
    private void showAnswerThenAdvance(String answer) {
        if (isFinished) return;
        isPaused = true;
        if (countDownTimer != null) countDownTimer.cancel();

        // Show the Serbian answer in the status
        answerReveal.setValue(answer);
        opponentStatus.setValue(""); // clear the small status text
        inputEnabled.setValue(false);

        pauseTimer = new android.os.CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                if (isFinished) return;

                isPaused = false;
                inputEnabled.setValue(true);

                if (currentRound < 2) {
                    currentRound++;
                    currentQuestionIndex++;
                    isStealPhase = false;
                    startRound();
                } else {
                    finishGame();
                }
            }
        }.start();
    }
    private void revealNextHint() {
        if (currentQuestionIndex >= questions.size() || isFinished) return;

        KPKQuestion q = questions.get(currentQuestionIndex);
        List<String> hints = q.getHints();

        if (currentStep < hints.size()) {
            currentHint.setValue(hints.get(currentStep));
            revealedHintCount.setValue(currentStep + 1);

            // Also update all hints list for UI
            List<String> currentAll = allHints.getValue();
            if (currentAll == null) currentAll = new ArrayList<>();
            // Ensure list has enough entries
            while (currentAll.size() <= currentStep) currentAll.add("");
            currentAll.set(currentStep, hints.get(currentStep));
            allHints.setValue(currentAll);
        }
        currentStep++;
    }

    private void startTimer(int seconds) {
        if (countDownTimer != null) countDownTimer.cancel();

        timer.setValue(seconds);
        countDownTimer = new android.os.CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isFinished) {
                    cancel();
                    return;
                }
                int remaining = (int) (millisUntilFinished / 1000);
                timer.setValue(remaining);

                // Reveal hint every 10 seconds (at 60, 50, 40, 30, 20, 10)
                int elapsed = seconds - remaining;
                if (elapsed > 0 && elapsed % 10 == 0 && currentStep < 7) {
                    revealNextHint();
                }
            }

            @Override
            public void onFinish() {
                if (isFinished) return;
                timer.setValue(0);
                onTimeUp();
            }
        }.start();
    }

    public void submitAnswer(String answer) {
        if (isFinished || currentQuestionIndex >= questions.size()) return;
        // Don't cancel timer on wrong answer — only cancel on correct answer or time up

        KPKQuestion q = questions.get(currentQuestionIndex);
        String correctAnswer = q.getAnswer();

        if (TextNormalizer.matches(answer, correctAnswer)) {
            // Correct: cancel timer, end round
            if (countDownTimer != null) countDownTimer.cancel();
            handleCorrectAnswer(correctAnswer);
        } else {
            // Wrong: keep timer running, allow more guesses
            if (isStealPhase) {
                // Steal phase: only one chance, fail immediately
                if (countDownTimer != null) countDownTimer.cancel();
                opponentStatus.setValue("Krađa neuspešna!");
                showAnswerThenAdvance(correctAnswer);
            } else {
                // Normal phase: wrong guess, keep going
                opponentStatus.setValue("Pogrešno! Pokušajte ponovo");
                // Timer keeps running, input stays enabled
            }
        }
    }

    private void handleCorrectAnswer(String correctAnswer) {
        int points;

        if (isStealPhase) {
            // Steal: 5 points
            points = 5;
            opponentStatus.setValue("Krađa uspešna! +" + points + " bodova");
        } else {
            // Normal: 20, 18, 16, 14, 12, 10, 8 based on step used
            int stepUsed = Math.min(currentStep, 7); // currentStep was already incremented after reveal
            points = Math.max(20 - (stepUsed - 1) * 2, 8);
            opponentStatus.setValue("Tačno! +" + points + " bodova");
        }

        // Add to appropriate player
        if (currentPlayer == 1) {
            scorePlayer1 += points;
        } else {
            scorePlayer2 += points;
        }

        // Update display score (show current player's total or both?)
        currentScore.setValue(scorePlayer1 + scorePlayer2);


        showAnswerThenAdvance(correctAnswer);
    }

    private void handleWrongAnswer(String correctAnswer) {
        if (isStealPhase) {
            // Steal failed, move to next round or end
            opponentStatus.setValue("Krađa neuspešna!");
            showAnswerThenAdvance(correctAnswer);
        } else {
            // Normal round wrong answer: opponent gets steal chance
            opponentStatus.setValue("Pogrešno! Igrač " + (currentPlayer == 1 ? 2 : 1) + " ima 10s za krađu");
            isStealPhase = true;
            currentPlayer = (currentPlayer == 1) ? 2 : 1; // Switch to other player for steal
            startTimer(10);
        }
    }

    private void onTimeUp() {
        if (isFinished || isPaused) return;

        if (isStealPhase) {
            // Steal time expired
            opponentStatus.setValue("Vreme za krađu isteklo!");
            KPKQuestion q = questions.get(currentQuestionIndex);
            showAnswerThenAdvance(q.getAnswer());
        } else {
            // Normal time expired: opponent gets steal chance
            opponentStatus.setValue("Vreme isteklo! Igrač " + (currentPlayer == 1 ? 2 : 1) + " ima 10s za krađu");
            isStealPhase = true;
            currentPlayer = (currentPlayer == 1) ? 2 : 1;
            startTimer(10);
        }
    }

    private void finishGame() {
        isFinished = true;
        if (countDownTimer != null) countDownTimer.cancel();
        opponentStatus.setValue("Kraj! Igrač 1: " + scorePlayer1 + " | Igrač 2: " + scorePlayer2);
        gameOver.setValue(true);
    }

    public void cleanup() {
        isFinished = true;
        if (countDownTimer != null) countDownTimer.cancel();
        if (pauseTimer != null) pauseTimer.cancel();
    }

    // Getters
    public LiveData<String> getCurrentHint() { return currentHint; }
    public LiveData<Integer> getTimer() { return timer; }
    public LiveData<Integer> getCurrentScore() { return currentScore; }
    public LiveData<String> getRoundInfo() { return roundInfo; }
    public LiveData<Boolean> getGameOver() { return gameOver; }
    public LiveData<String> getOpponentStatus() { return opponentStatus; }
    public LiveData<List<String>> getAllHints() { return allHints; }
    public LiveData<Integer> getRevealedHintCount() { return revealedHintCount; }
    public LiveData<Boolean> getInputEnabled() { return inputEnabled; }
    public int getScorePlayer1() { return scorePlayer1; }
    public int getScorePlayer2() { return scorePlayer2; }
    public LiveData<String> getAnswerReveal() { return answerReveal; }
    public void setLocalPlayerRole(int role) {
        this.localPlayerRole = role;
    }
}