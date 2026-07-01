package com.example.slagalica.data;

import androidx.annotation.NonNull;

import com.example.slagalica.data.model.AppNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {

    public interface NotificationListener {
        void onNotificationsLoaded(List<AppNotification> notifications);
        void onNewNotification(AppNotification notification);
        void onError(String error);
    }

    private DatabaseReference notifRef;
    private ChildEventListener childListener;
    private ValueEventListener valueListener;
    private final List<NotificationListener> listeners = new ArrayList<>();

    @NonNull
    public String currentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "";
    }

    public void init() {
        String uid = currentUid();
        if (uid.isEmpty()) return;
        notifRef = FirebaseDatabase.getInstance(MatchRepository.DB_URL)
                .getReference("notifications").child(uid);
    }

    public void loadAll() {
        if (notifRef == null) return;
        notifRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AppNotification> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    AppNotification n = child.getValue(AppNotification.class);
                    if (n != null) {
                        n.id = child.getKey();
                        list.add(0, n); // newest first
                    }
                }
                for (NotificationListener l : listeners) l.onNotificationsLoaded(list);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                for (NotificationListener l : listeners) l.onError(error.getMessage());
            }
        });
    }

    public void startListening() {
        if (notifRef == null || childListener != null) return;
        childListener = new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot snap, String prev) {
                AppNotification n = snap.getValue(AppNotification.class);
                if (n != null) {
                    n.id = snap.getKey();
                    for (NotificationListener l : listeners) l.onNewNotification(n);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snap, String prev) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snap) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snap, String prev) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                for (NotificationListener l : listeners) l.onError(error.getMessage());
            }
        };
        notifRef.addChildEventListener(childListener);
    }

    public void markAsRead(String notifId) {
        if (notifRef == null || notifId == null) return;
        notifRef.child(notifId).child("read").setValue(true);
    }

    public void markAllAsRead() {
        if (notifRef == null) return;
        notifRef.get().addOnSuccessListener(snap -> {
            for (DataSnapshot child : snap.getChildren()) {
                child.getRef().child("read").setValue(true);
            }
        });
    }

    public void addNotification(AppNotification notification) {
        if (notifRef == null) return;
        notifRef.push().setValue(notification);
    }

    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }

    public void detach() {
        if (notifRef != null && childListener != null) {
            notifRef.removeEventListener(childListener);
        }
        childListener = null;
        listeners.clear();
    }
}