package com.example.slagalica.data.model;

public class ChatMessage {
    public String senderUid = "";
    public String senderName = "";
    public String text = "";
    public long timestamp = 0;

    public ChatMessage() {}

    public ChatMessage(String senderUid, String senderName, String text, long timestamp) {
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }
}