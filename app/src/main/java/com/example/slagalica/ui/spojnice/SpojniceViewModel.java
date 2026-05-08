package com.example.slagalica.ui.spojnice;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.SpojniceRepository;
import com.example.slagalica.domain.model.SpojnicePair;

public class SpojniceViewModel extends ViewModel {

    private static final int TOTAL_ROUNDS = 2;

    private final MutableLiveData<SpojnicePair> currentPairs = new MutableLiveData<>();
    private final MutableLiveData<Integer> score = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> currentRound = new MutableLiveData<>(1);
    private final MutableLiveData<Boolean> gameFinished = new MutableLiveData<>(false);

    private final SpojniceRepository repository = new SpojniceRepository();

    public void loadRound(int round) {
        currentRound.setValue(round);
        currentPairs.setValue(repository.getRound(round));
    }

    public boolean checkConnection(int leftIndex, int rightIndex) {
        SpojnicePair pairs = currentPairs.getValue();
        if (pairs == null) return false;
        boolean correct = pairs.getCorrectMapping()[leftIndex] == rightIndex;
        if (correct) {
            int current = score.getValue() != null ? score.getValue() : 0;
            score.setValue(current + 2);
        }
        return correct;
    }

    public void finishRound() {
        int round = currentRound.getValue() != null ? currentRound.getValue() : 1;
        if (round >= TOTAL_ROUNDS) {
            gameFinished.setValue(true);
        } else {
            loadRound(round + 1);
        }
    }

    public int getTotalRounds() { return TOTAL_ROUNDS; }

    public MutableLiveData<SpojnicePair> getCurrentPairs() { return currentPairs; }
    public MutableLiveData<Integer> getScore() { return score; }
    public MutableLiveData<Integer> getCurrentRound() { return currentRound; }
    public MutableLiveData<Boolean> getGameFinished() { return gameFinished; }
}
