package com.example.slagalica.data.model;

public class LeaderboardEntry {
    public String uid = "";
    public String username = "";
    public String region = "";
    public int stars = 0;
    public int leagueOrdinal = 0; // for league icon/color

    public LeaderboardEntry() {}

    public LeaderboardEntry(String uid, String username, String region, int stars, int leagueOrdinal) {
        this.uid = uid;
        this.username = username;
        this.region = region;
        this.stars = stars;
        this.leagueOrdinal = leagueOrdinal;
    }
}