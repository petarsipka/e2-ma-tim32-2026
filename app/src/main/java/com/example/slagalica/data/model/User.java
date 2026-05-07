package com.example.slagalica.data.model;

public class User {
    private String username;
    private String email;
    private int tokens;
    private int stars;
    private String league;
    private String region;

    public User(String username, String email, int tokens, int stars, String league, String region) {
        this.username = username;
        this.email = email;
        this.tokens = tokens;
        this.stars = stars;
        this.league = league;
        this.region = region;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public int getTokens() { return tokens; }
    public int getStars() { return stars; }
    public String getLeague() { return league; }
    public String getRegion() { return region; }
}
