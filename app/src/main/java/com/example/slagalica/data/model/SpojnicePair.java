package com.example.slagalica.data.model;

import java.util.List;

public class SpojnicePair {
    private String criterion;
    private List<String> leftTerms;
    private List<String> rightTerms;
    private int[] correctMapping;

    public SpojnicePair(String criterion, List<String> leftTerms, List<String> rightTerms, int[] correctMapping) {
        this.criterion = criterion;
        this.leftTerms = leftTerms;
        this.rightTerms = rightTerms;
        this.correctMapping = correctMapping;
    }

    public String getCriterion() { return criterion; }
    public List<String> getLeftTerms() { return leftTerms; }
    public List<String> getRightTerms() { return rightTerms; }
    public int[] getCorrectMapping() { return correctMapping; }
}
