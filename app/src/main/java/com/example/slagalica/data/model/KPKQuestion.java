package com.example.slagalica.data.model;

import java.util.List;

public class KPKQuestion {
    private String answer;
    private List<String> hints; // 7 hints, from hardest to easiest
    private int round; // which player starts this round

    public KPKQuestion() {}

    public KPKQuestion(String answer, List<String> hints) {
        this.answer = answer;
        this.hints = hints;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<String> getHints() { return hints; }
    public void setHints(List<String> hints) { this.hints = hints; }
}