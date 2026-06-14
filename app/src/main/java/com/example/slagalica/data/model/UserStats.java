package com.example.slagalica.data.model;

import java.util.List;

/**
 * Player statistics shown on the profile. All values are DB-backed parameters:
 * total wins/losses plus the per-game average rows. Total games played and the
 * win percentage are derived so callers never recompute them inconsistently.
 */
public class UserStats {

    private final int wins;
    private final int losses;
    private final List<GameStat> games;

    public UserStats(int wins, int losses, List<GameStat> games) {
        this.wins = wins;
        this.losses = losses;
        this.games = games;
    }

    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public List<GameStat> getGames() { return games; }

    public int getTotalGames() { return wins + losses; }

    /** Win rate as a whole percentage (0–100); 0 when no games have been played. */
    public int getWinPercent() {
        int total = getTotalGames();
        return total == 0 ? 0 : Math.round(wins * 100f / total);
    }
}
