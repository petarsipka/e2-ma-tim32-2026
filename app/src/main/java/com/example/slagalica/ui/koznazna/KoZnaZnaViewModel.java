package com.example.slagalica.ui.koznazna;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.QuestionRepository;
import com.example.slagalica.data.model.Question;

import java.util.List;
import java.util.Random;

public class KoZnaZnaViewModel extends ViewModel {

    private final MutableLiveData<Question> currentQuestion = new MutableLiveData<>();
    private final MutableLiveData<Integer> score = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> opponentScore = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> questionIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> gameFinished = new MutableLiveData<>(false);

    private final QuestionRepository repository = new QuestionRepository();
    private final Random random = new Random();
    private List<Question> questions;

    public void loadQuestions() {
        questions = repository.getKoZnaZnaQuestions();
        score.setValue(0);
        opponentScore.setValue(0);
        questionIndex.setValue(0);
        gameFinished.setValue(false);
        currentQuestion.setValue(questions.get(0));
    }

    public boolean checkAnswer(int selectedIndex) {
        Question q = currentQuestion.getValue();
        if (q == null) return false;
        boolean correct = selectedIndex == q.getCorrectIndex();
        int current = score.getValue() != null ? score.getValue() : 0;
        score.setValue(correct ? current + 10 : current - 5);
        return correct;
    }

    public void nextQuestion() {
        simulateOpponent();
        int next = (questionIndex.getValue() != null ? questionIndex.getValue() : 0) + 1;
        if (next >= questions.size()) {
            gameFinished.setValue(true);
        } else {
            questionIndex.setValue(next);
            currentQuestion.setValue(questions.get(next));
        }
    }

    /** Simulated opponent: answers each question, ~60% correct (+10) else -5. */
    private void simulateOpponent() {
        int current = opponentScore.getValue() != null ? opponentScore.getValue() : 0;
        opponentScore.setValue(random.nextInt(100) < 60 ? current + 10 : current - 5);
    }

    public int getTotalQuestions() {
        return questions != null ? questions.size() : 0;
    }

    public MutableLiveData<Question> getCurrentQuestion() { return currentQuestion; }
    public MutableLiveData<Integer> getScore() { return score; }
    public MutableLiveData<Integer> getOpponentScore() { return opponentScore; }
    public MutableLiveData<Integer> getQuestionIndex() { return questionIndex; }
    public MutableLiveData<Boolean> getGameFinished() { return gameFinished; }
}
