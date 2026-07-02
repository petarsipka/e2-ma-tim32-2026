package com.example.slagalica.data;

import com.example.slagalica.data.model.AppNotification;
import com.google.firebase.database.FirebaseDatabase;

public class NotificationHelper {

    private static final String DB_URL = MatchRepository.DB_URL;

    public static void sendNotification(String targetUid, String type, String title, String message, String actionData) {
        if (targetUid == null || targetUid.isEmpty()) return;

        AppNotification notif = new AppNotification(
                "", type, title, message, System.currentTimeMillis(), actionData
        );

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("notifications")
                .child(targetUid)
                .push()
                .setValue(notif);
    }

    // Convenience methods for specific notification types
    public static void notifyChatMessage(String targetUid, String senderName, String region) {
        sendNotification(targetUid, "chat", "Nova poruka u četu",
                senderName + ": vam je poslao/la poruku u regionalnom četu", region);
    }

    public static void notifyRankingReward(String targetUid, int place, boolean weekly) {
        String period = weekly ? "nedeljnoj" : "mesečnoj";
        String title = "Nagrada za rang listu!";
        String msg = "Osvojili ste " + place + ". mesto na " + period + " rang listi!";
        sendNotification(targetUid, "rewards", title, msg, "");
    }

    public static void notifyLeagueChange(String targetUid, String newLeague, boolean promoted) {
        String action = promoted ? "napredovali" : "pali";
        sendNotification(targetUid, "other", "Promena lige!",
                "Upravo ste " + action + "! Sad ste " + newLeague + " liga!", "");
    }

    public static void notifyGameInvite(String targetUid, String senderName, String matchCode) {
        sendNotification(targetUid, "other", "Poziv za partiju",
                senderName + " vas poziva na prijateljsku partiju!", matchCode);
    }
}