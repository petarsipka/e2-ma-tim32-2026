package com.example.slagalica.data;

import com.example.slagalica.data.model.Asocijacija;

public class AsocijacijeRepository {

    public Asocijacija getRound(int round) {
        if (round == 1) return getRound1();
        return getRound2();
    }

    private Asocijacija getRound1() {
        return new Asocijacija(
            new String[][]{
                {"Gitara", "Bas", "Bubnjevi", "Klavijature"},
                {"Ringo", "John", "Paul", "George"},
                {"Baza", "Tenor", "Alt", "Sopran"},
                {"Do", "Re", "Mi", "Fa"}
            },
            new String[]{"Instrumenti", "Bitlsi", "Glasovi", "Note"},
            "Muzika"
        );
    }

    private Asocijacija getRound2() {
        return new Asocijacija(
            new String[][]{
                {"Gol", "Penalt", "Korner", "Ofsajd"},
                {"Forhend", "Bekhend", "Servis", "Volej"},
                {"Trojka", "Slobodno", "Dribling", "Blokada"},
                {"Kraul", "Leđno", "Prsno", "Delfin"}
            },
            new String[]{"Fudbal", "Tenis", "Košarka", "Plivanje"},
            "Sport"
        );
    }
}
