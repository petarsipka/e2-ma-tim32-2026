package com.example.slagalica.ui.notifications;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;
import com.example.slagalica.data.model.AppNotification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifHolder> {

    public interface OnNotifClickListener {
        void onClick(AppNotification notif);
        void onLongClick(AppNotification notif);
    }

    private List<AppNotification> notifications = new ArrayList<>();
    private OnNotifClickListener listener;

    public void setNotifications(List<AppNotification> list) {
        this.notifications = list;
        notifyDataSetChanged();
    }

    public void setListener(OnNotifClickListener listener) {
        this.listener = listener;
    }

    @NonNull @Override
    public NotifHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NotifHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NotifHolder h, int position) {
        AppNotification n = notifications.get(position);
        h.title.setText(n.title);
        h.message.setText(n.message);
        h.time.setText(formatTime(n.timestamp));
        h.typeBadge.setText(typeLabel(n.type));

        if (n.read) {
            h.title.setTypeface(null, Typeface.NORMAL);
            h.title.setTextColor(h.itemView.getContext().getColor(R.color.muted));
            h.unreadDot.setVisibility(View.GONE);
        } else {
            h.title.setTypeface(null, Typeface.BOLD);
            h.title.setTextColor(h.itemView.getContext().getColor(R.color.text));
            h.unreadDot.setVisibility(View.VISIBLE);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(n);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(n);
            return true;
        });
    }

    @Override public int getItemCount() { return notifications.size(); }

    private String typeLabel(String type) {
        switch (type) {
            case "chat": return "💬";
            case "ranking": return "🏆";
            case "rewards": return "🎁";
            default: return "📢";
        }
    }

    private String formatTime(long ts) {
        return new SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()).format(new Date(ts));
    }

    static class NotifHolder extends RecyclerView.ViewHolder {
        final TextView title, message, time, typeBadge;
        final View unreadDot;

        NotifHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notifTitle);
            message = itemView.findViewById(R.id.notifMessage);
            time = itemView.findViewById(R.id.notifTime);
            typeBadge = itemView.findViewById(R.id.notifType);
            unreadDot = itemView.findViewById(R.id.notifUnreadDot);
        }
    }
}