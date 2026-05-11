package com.example.slagalica.data.model;

public class Question {
    private String text;
    private String[] answers;
    private int correctIndex;

    public Question(String text, String[] answers, int correctIndex) {
        this.text = text;
        this.answers = answers;
        this.correctIndex = correctIndex;
    }

    public String getText() { return text; }
    public String[] getAnswers() { return answers; }
    public int getCorrectIndex() { return correctIndex; }
}
