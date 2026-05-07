package com.example.slagalica.data;

import com.example.slagalica.data.model.SpojnicePair;

import java.util.Arrays;

public class SpojniceRepository {

    public SpojnicePair getRound(int round) {
        if (round == 1) return getRound1();
        return getRound2();
    }

    private SpojnicePair getRound1() {
        return new SpojnicePair(
            "Poveži gradove sa državama",
            Arrays.asList("Pariz", "Berlin", "Rim", "Madrid", "Atina"),
            Arrays.asList("Španija", "Italija", "Grčka", "Nemačka", "Francuska"),
            new int[]{4, 3, 1, 0, 2}
            // Pariz→Francuska(4), Berlin→Nemačka(3), Rim→Italija(1), Madrid→Španija(0), Atina→Grčka(2)
        );
    }

    private SpojnicePair getRound2() {
        return new SpojnicePair(
            "Poveži izvođače sa pesmama",
            Arrays.asList("Ed Sheeran", "Adele", "Eminem", "Lady Gaga", "Coldplay"),
            Arrays.asList("Lose Yourself", "Rolling in the Deep", "Shape of You", "Bad Romance", "Yellow"),
            new int[]{2, 1, 0, 3, 4}
            // Ed Sheeran→Shape of You(2), Adele→Rolling in the Deep(1), Eminem→Lose Yourself(0),
            // Lady Gaga→Bad Romance(3), Coldplay→Yellow(4)
        );
    }
}
