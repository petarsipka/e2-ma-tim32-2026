package com.example.slagalica.data;

import com.example.slagalica.data.model.GameStat;
import com.example.slagalica.data.model.User;
import com.example.slagalica.data.model.UserStats;

import java.util.Arrays;
import java.util.List;

public class UserTemporaryDB {

    public User getCurrentUser() {
        return new User(
            "Petar Petrović",
            "petar.petrovic@gmail.com",
            150,
            45,
                User.League.Pro,
            "Srbija"
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
        return new UserStats(25, 17, games);
    }
}
