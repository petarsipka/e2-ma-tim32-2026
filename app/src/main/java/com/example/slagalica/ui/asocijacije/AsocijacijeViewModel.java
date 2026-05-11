package com.example.slagalica.ui.asocijacije;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.AsocijacijeRepository;
import com.example.slagalica.data.model.Asocijacija;

public class AsocijacijeViewModel extends ViewModel {

    private final MutableLiveData<Asocijacija> currentRoundData = new MutableLiveData<>();
    private final MutableLiveData<Integer> score = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> currentRound = new MutableLiveData<>(1);
    private final MutableLiveData<Boolean> gameFinished = new MutableLiveData<>(false);

    private final AsocijacijeRepository repository = new AsocijacijeRepository();

    private boolean[] columnSolved = new boolean[4];
    private boolean[][] fieldRevealed = new boolean[4][4];
    private boolean finalSolved = false;

    public void loadRound(int round) {
        columnSolved = new boolean[4];
        fieldRevealed = new boolean[4][4];
        finalSolved = false;
        currentRoundData.setValue(repository.getRound(round));
    }

    public void revealField(int col, int row) {
        fieldRevealed[col][row] = true;
    }

    // Returns points earned if correct, -1 if wrong
    public int guessColumn(int col, String guess) {
        Asocijacija data = currentRoundData.getValue();
        if (data == null || columnSolved[col]) return -1;
        if (!guess.trim().equalsIgnoreCase(data.getColumnSolutions()[col])) return -1;

        int unrevealed = 0;
        for (int r = 0; r < 4; r++) {
            if (!fieldRevealed[col][r]) unrevealed++;
        }
        int points = 2 + unrevealed;
        columnSolved[col] = true;
        score.setValue(score.getValue() + points);
        return points;
    }

    // Returns points earned if correct, -1 if wrong
    public int guessFinal(String guess) {
        Asocijacija data = currentRoundData.getValue();
        if (data == null || finalSolved) return -1;
        if (!guess.trim().equalsIgnoreCase(data.getFinalSolution())) return -1;

        int unopenedColumns = 0;
        int partialPoints = 0;
        for (int c = 0; c < 4; c++) {
            if (!columnSolved[c]) {
                boolean anyRevealed = false;
                for (int r = 0; r < 4; r++) {
                    if (fieldRevealed[c][r]) { anyRevealed = true; break; }
                }
                if (!anyRevealed) {
                    unopenedColumns++;
                } else {
                    int unrevealed = 0;
                    for (int r = 0; r < 4; r++) {
                        if (!fieldRevealed[c][r]) unrevealed++;
                    }
                    partialPoints += 2 + unrevealed;
                }
            }
        }

        int points = 7 + 6 * unopenedColumns + partialPoints;
        finalSolved = true;
        score.setValue(score.getValue() + points);
        return points;
    }

    public void advanceRound() {
        int round = currentRound.getValue();
        if (round < 2) {
            currentRound.setValue(round + 1);
            loadRound(round + 1);
        } else {
            gameFinished.setValue(true);
        }
    }

    public boolean isColumnSolved(int col) { return columnSolved[col]; }
    public boolean isFieldRevealed(int col, int row) { return fieldRevealed[col][row]; }
    public boolean isFinalSolved() { return finalSolved; }

    public LiveData<Asocijacija> getCurrentRoundData() { return currentRoundData; }
    public LiveData<Integer> getScore() { return score; }
    public LiveData<Integer> getCurrentRound() { return currentRound; }
    public LiveData<Boolean> getGameFinished() { return gameFinished; }
}
