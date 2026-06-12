package com.example.slagalica.data;

import com.example.slagalica.data.model.GameStat;
import com.example.slagalica.data.model.User;
import com.example.slagalica.data.model.UserStats;

import java.util.Arrays;
import java.util.List;

/**
 * Stand-in for the backend: every value here (profile fields, per-game averages)
 * is a parameter that will later come from the database. The UI binds to these
 * objects and never hard-codes profile content.
 */
public class UserRepository {

    public User getCurrentUser() {
        return new User(
            "Marko_NS",
            "marko.ns@email.com",
            5,
            1240,
            "III liga · Srebrna",
            "Vojvodina"
        );
    }

    public UserStats getUserStats() {
        List<GameStat> games = Arrays.asList(
            new GameStat("Ko zna zna", "30–42", 78, 0),
            new GameStat("Spojnice", "12–18", 64, 1),
            new GameStat("Asocijacije", "18–30", 71, 3),
            new GameStat("Skočko", "20–35", 82, 2),
            new GameStat("Korak po korak", "10–22", 55, 0),
            new GameStat("Moj broj", "0–10", 40, 1)
        );
        return new UserStats(96, 46, games);
    }
}
