package com.example.slagalica.ui.koznazna;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.QuestionRepository;
import com.example.slagalica.data.model.Match;
import com.example.slagalica.data.model.Question;

import java.util.List;

public class KoZnaZnaViewModel extends ViewModel {

    private final MutableLiveData<Question> currentQuestion = new MutableLiveData<>();
    private final MutableLiveData<Integer> score = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> opponentScore = new MutableLiveData<>(0);
    // Cumulative match totals (sum across all games) — drive the live scoreboard.
    private final MutableLiveData<Integer> myTotal = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> opponentTotal = new MutableLiveData<>(0);
    private final MutableLiveData<String> opponentName = new MutableLiveData<>();
    private final MutableLiveData<String> opponentUid = new MutableLiveData<>();
    private final MutableLiveData<Integer> questionIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> gameFinished = new MutableLiveData<>(false);

    private final QuestionRepository repository = new QuestionRepository();
    private List<Question> questions;

    // Multiplayer wiring; null when the screen is opened without a match (dev shortcut).
    private MatchRepository matchRepo;
    private String matchCode;
    private String myUid;

    public void loadQuestions() {
        questions = repository.getKoZnaZnaQuestions();
        score.setValue(0);
        opponentScore.setValue(0);
        questionIndex.setValue(0);
        gameFinished.setValue(false);
        currentQuestion.setValue(questions.get(0));
    }

    /**
     * Attach to a Realtime Database match: the opponent's score/name now stream
     * from the shared node, and our own score is mirrored back to it. Safe to call
     * with a null code (then the screen stays single-device).
     */
    public void init(String code) {
        if (code == null) return;
        matchCode = code;
        matchRepo = new MatchRepository();
        myUid = matchRepo.currentUid();
        matchRepo.listen(code, this::onMatchUpdate);
    }

    private void onMatchUpdate(Match match) {
        String oppUid = match.opponentOf(myUid);
        opponentUid.postValue(oppUid);
        opponentScore.postValue(match.gameScore(oppUid, Match.GAME_KOZNAZNA));
        myTotal.postValue(match.totalScore(myUid));
        opponentTotal.postValue(match.totalScore(oppUid));
        if (oppUid != null && match.players != null) {
            Match.Player opp = match.players.get(oppUid);
            if (opp != null && opp.name != null) opponentName.postValue(opp.name);
        }
    }

    public boolean checkAnswer(int selectedIndex) {
        Question q = currentQuestion.getValue();
        if (q == null) return false;
        boolean correct = selectedIndex == q.getCorrectIndex();
        int current = score.getValue() != null ? score.getValue() : 0;
        int updated = correct ? current + 10 : current - 5;
        score.setValue(updated);
        if (matchRepo != null) matchRepo.setGameScore(matchCode, Match.GAME_KOZNAZNA, updated);
        return correct;
    }

    public void nextQuestion() {
        int next = (questionIndex.getValue() != null ? questionIndex.getValue() : 0) + 1;
        if (next >= questions.size()) {
            gameFinished.setValue(true);
        } else {
            questionIndex.setValue(next);
            currentQuestion.setValue(questions.get(next));
        }
    }

    public int getTotalQuestions() {
        return questions != null ? questions.size() : 0;
    }

    public MutableLiveData<Question> getCurrentQuestion() { return currentQuestion; }
    public MutableLiveData<Integer> getScore() { return score; }
    public MutableLiveData<Integer> getOpponentScore() { return opponentScore; }
    public MutableLiveData<Integer> getMyTotal() { return myTotal; }
    public MutableLiveData<Integer> getOpponentTotal() { return opponentTotal; }
    public MutableLiveData<String> getOpponentName() { return opponentName; }
    public MutableLiveData<String> getOpponentUid() { return opponentUid; }
    public MutableLiveData<Integer> getQuestionIndex() { return questionIndex; }
    public MutableLiveData<Boolean> getGameFinished() { return gameFinished; }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (matchRepo != null) matchRepo.detach();
    }
}
