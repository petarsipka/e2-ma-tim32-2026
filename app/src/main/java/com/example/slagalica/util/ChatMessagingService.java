package com.example.slagalica.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.slagalica.R;
import com.example.slagalica.ui.chat.ChatActivity;
import com.example.slagalica.ui.leaderboard.LeaderboardActivity;
import com.example.slagalica.ui.notifications.NotificationActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ChatMessagingService extends FirebaseMessagingService {

    private static final String CHAT_CHANNEL_ID = "chat_channel";
    private static final String RANKING_CHANNEL_ID = "ranking_channel";
    private static final String REWARDS_CHANNEL_ID = "rewards_channel";
    private static final String OTHER_CHANNEL_ID = "other_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);

            nm.createNotificationChannel(new NotificationChannel(
                    CHAT_CHANNEL_ID, "Čet obaveštenja", NotificationManager.IMPORTANCE_DEFAULT));
            nm.createNotificationChannel(new NotificationChannel(
                    RANKING_CHANNEL_ID, "Rang lista obaveštenja", NotificationManager.IMPORTANCE_DEFAULT));
            nm.createNotificationChannel(new NotificationChannel(
                    REWARDS_CHANNEL_ID, "Nagrade obaveštenja", NotificationManager.IMPORTANCE_HIGH));
            nm.createNotificationChannel(new NotificationChannel(
                    OTHER_CHANNEL_ID, "Ostala obaveštenja", NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String type = message.getData().get("type");
        String title = message.getData().get("title");
        String body = message.getData().get("body");
        String actionData = message.getData().get("actionData");

        if (title == null) title = "Slagalica";
        if (body == null) body = "";
        if (type == null) type = "other";

        showNotification(type, title, body, actionData);
    }

    private void showNotification(String type, String title, String body, String actionData) {
        Intent intent;
        String channelId;

        switch (type) {
            case "chat":
                channelId = CHAT_CHANNEL_ID;
                intent = new Intent(this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_REGION, actionData);
                break;
            case "ranking":
                channelId = RANKING_CHANNEL_ID;
                intent = new Intent(this, LeaderboardActivity.class);
                break;
            case "rewards":
                channelId = REWARDS_CHANNEL_ID;
                intent = new Intent(this, LeaderboardActivity.class);
                break;
            default:
                channelId = OTHER_CHANNEL_ID;
                intent = new Intent(this, NotificationActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}