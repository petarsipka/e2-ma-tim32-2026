package com.example.slagalica.data;

import com.example.slagalica.data.model.User;
import com.example.slagalica.data.model.UserStats;

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
        return new UserStats(
            "3/5 tačnih odgovora  ·  60% uspešnosti\nProsek: 20 bodova po partiji",
            "70% tačnih pogodaka\nProsek: 8 bodova po partiji",
            "1. korak: 80%\n2. korak: 60%\n3. korak: 35%\nProsek: 11 bodova po partiji",
            "4/5 rešenih asocijacija  ·  80% uspešnosti\nProsek: 23 bodova po partiji",
            "65% kombinacija pogođeno\nProsek: 7 bodova po partiji",
            "85% pojmova uspešno povezano\nProsek: 9 bodova po partiji",
            42,
            25,
            17
        );
    }
}
