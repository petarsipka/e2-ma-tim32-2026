package com.example.slagalica.data.model;

public class UserStats {
    private String statKZZ;
    private String statMB;
    private String statKPK;
    private String statAsoc;
    private String statSkocko;
    private String statSpojnice;
    private int totalGames;
    private int wins;
    private int losses;

    public UserStats(String statKZZ, String statMB, String statKPK, String statAsoc,
                     String statSkocko, String statSpojnice, int totalGames, int wins, int losses) {
        this.statKZZ = statKZZ;
        this.statMB = statMB;
        this.statKPK = statKPK;
        this.statAsoc = statAsoc;
        this.statSkocko = statSkocko;
        this.statSpojnice = statSpojnice;
        this.totalGames = totalGames;
        this.wins = wins;
        this.losses = losses;
    }

    public String getStatKZZ() { return statKZZ; }
    public String getStatMB() { return statMB; }
    public String getStatKPK() { return statKPK; }
    public String getStatAsoc() { return statAsoc; }
    public String getStatSkocko() { return statSkocko; }
    public String getStatSpojnice() { return statSpojnice; }
    public int getTotalGames() { return totalGames; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
}
