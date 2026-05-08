package com.example.slagalica.data;

import com.example.slagalica.data.model.Question;

import java.util.Arrays;
import java.util.List;

public class QuestionRepository {

    public List<Question> getKoZnaZnaQuestions() {
        return Arrays.asList(
            new Question(
                "Koji je glavni grad Australije?",
                new String[]{"Sydney", "Melbourne", "Canberra", "Perth"},
                2
            ),
            new Question(
                "Ko je napisao delo 'Hamlet'?",
                new String[]{"Molière", "Shakespeare", "Dante", "Goethe"},
                1
            ),
            new Question(
                "Hemijski simbol 'Au' označava koji element?",
                new String[]{"Srebro", "Aluminijum", "Platina", "Zlato"},
                3
            ),
            new Question(
                "Koja je najveća planeta Sunčevog sistema?",
                new String[]{"Saturn", "Jupiter", "Uran", "Neptun"},
                1
            ),
            new Question(
                "Koje godine je počeo Prvi svetski rat?",
                new String[]{"1912", "1916", "1914", "1918"},
                2
            )
        );
    }
}
