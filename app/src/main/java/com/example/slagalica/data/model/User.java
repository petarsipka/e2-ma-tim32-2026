package com.example.slagalica.data.model;


public class User {
    public enum League {Bronzana, Srebrna, Zlatna, Platinasta, Dijamantska, Legendarna}
    private String username;
    private String email;
    private int tokens;
    private int stars;
    private League league;
    private String region;
    private int totalGames;
    private int wins;
    private String avatar;

    public User() {}
    public User(String username, String email, String region) {
        this.username = username;
        this.email = email;
        this.region = region;
        this.stars = 0;
        this.tokens = 5;
        this.league = League.Bronzana;
        this.totalGames = 0;
        this.wins = 0;
        this.avatar = "";
    }
    public User(String username, String email, int tokens, int stars, League league, String region) {
        this.username = username;
        this.email = email;
        this.tokens = tokens;
        this.stars = stars;
        this.league = league;
        this.region = region;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }
    public int getTokens() { return tokens; }
    public void setTokens(int tokens) { this.tokens = tokens; }
    public League getLeague() { return league; }
    public void setLeague(League league) { this.league = league; }
    public int getTotalGames() { return totalGames; }
    public void setTotalGames(int totalGames) { this.totalGames = totalGames; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}
