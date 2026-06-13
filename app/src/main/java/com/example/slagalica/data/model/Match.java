package com.example.slagalica.data.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared state of one 1v1 match, stored as a JSON node at {@code /matches/{code}}
 * in Realtime Database. Both devices read and write the same node; the scoreboard
 * ("semafor") binds to {@link #scores}. Public no-arg fields are required for
 * Firebase deserialization via {@code DataSnapshot.getValue(Match.class)}.
 */
public class Match {

    // Stable game keys + display names, shared by every game and the result screen.
    public static final String GAME_KOZNAZNA = "koznazna";
    public static final String GAME_SPOJNICE = "spojnice";
    public static final String GAME_ASOCIJACIJE = "asocijacije";
    public static final String GAME_SKOCKO = "skocko";
    public static final String GAME_KPK = "kpk";
    public static final String GAME_MOJBROJ = "mojbroj";
    public static final String[] GAME_KEYS = {
            GAME_KOZNAZNA, GAME_SPOJNICE, GAME_ASOCIJACIJE, GAME_SKOCKO, GAME_KPK, GAME_MOJBROJ
    };
    public static final String[] GAME_NAMES = {
            "Ko zna zna", "Spojnice", "Asocijacije", "Skočko", "Korak po korak", "Moj broj"
    };

    public String host;
    public String status;                 // "waiting" | "ready" | "finished"
    public Map<String, Player> players = new HashMap<>();
    public Map<String, Long> scores = new HashMap<>();
    /** Per-game scores: uid → (gameKey → points). */
    public Map<String, Map<String, Long>> gameScores = new HashMap<>();

    public Match() {}

    /** A player entry under {@code players/{uid}}. */
    public static class Player {
        public String name;

        public Player() {}

        public Player(String name) {
            this.name = name;
        }
    }

    /** uid of the other player, or {@code null} if nobody else has joined yet. */
    public String opponentOf(String myUid) {
        if (players == null) return null;
        for (String uid : players.keySet()) {
            if (!uid.equals(myUid)) return uid;
        }
        return null;
    }

    /** Score for a uid as an int (0 if absent). */
    public int scoreOf(String uid) {
        if (scores == null || uid == null) return 0;
        Long s = scores.get(uid);
        return s != null ? s.intValue() : 0;
    }

    public int playerCount() {
        return players != null ? players.size() : 0;
    }

    /** Points a uid scored in one game (0 if absent). */
    public int gameScore(String uid, String game) {
        if (gameScores == null || uid == null) return 0;
        Map<String, Long> g = gameScores.get(uid);
        if (g == null) return 0;
        Long v = g.get(game);
        return v != null ? v.intValue() : 0;
    }

    /** Sum of a uid's scores across all six games. */
    public int totalScore(String uid) {
        int total = 0;
        for (String key : GAME_KEYS) total += gameScore(uid, key);
        return total;
    }
}
