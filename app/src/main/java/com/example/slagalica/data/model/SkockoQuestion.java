package com.example.slagalica.data.model;

public class SkockoQuestion {
    private String solution; // 4 chars, e.g. "SCTK" = Skocko, Tref, Pik, Karo

    public SkockoQuestion() {}

    public SkockoQuestion(String solution) {
        this.solution = solution;
    }

    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
}