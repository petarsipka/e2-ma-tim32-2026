package com.example.slagalica.data.model;

public class AppNotification {
    public String id = "";
    public String type = "";      // chat, ranking, rewards, other
    public String title = "";
    public String message = "";
    public long timestamp = 0;
    public boolean read = false;
    public String actionData = ""; // e.g. matchCode, region, etc.

    public AppNotification() {}

    public AppNotification(String id, String type, String title, String message, long timestamp, String actionData) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.read = false;
        this.actionData = actionData;
    }
}