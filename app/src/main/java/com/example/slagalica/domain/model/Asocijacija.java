package com.example.slagalica.domain.model;

public class Asocijacija {
    private String[][] columnFields;
    private String[] columnSolutions;
    private String finalSolution;

    public Asocijacija(String[][] columnFields, String[] columnSolutions, String finalSolution) {
        this.columnFields = columnFields;
        this.columnSolutions = columnSolutions;
        this.finalSolution = finalSolution;
    }

    public String[][] getColumnFields() { return columnFields; }
    public String[] getColumnSolutions() { return columnSolutions; }
    public String getFinalSolution() { return finalSolution; }
}
