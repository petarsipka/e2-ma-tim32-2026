package com.example.slagalica.data.model;

import java.util.List;

public class MojBrojQuestion {
    private int target;
    private List<Integer> numbers;
    private String solution;

    public MojBrojQuestion() {}

    public MojBrojQuestion(int target, List<Integer> numbers, String solution) {
        this.target = target;
        this.numbers = numbers;
        this.solution = solution;
    }

    public int getTarget() { return target; }
    public void setTarget(int target) { this.target = target; }
    public List<Integer> getNumbers() { return numbers; }
    public void setNumbers(List<Integer> numbers) { this.numbers = numbers; }
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
}