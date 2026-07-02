package com.example.slagalica.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.slagalica.data.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ChatRepository {

    public interface ChatListener {
        void onMessageAdded(ChatMessage msg);
        void onError(String error);
    }

    private DatabaseReference chatRef;
    private ChildEventListener childListener;
    private final List<ChatListener> listeners = new ArrayList<>();

    @Nullable
    public String currentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void joinRegionChat(String region) {
        leaveChat();
        if (region == null || region.isEmpty()) return;
        String safeRegion = region.replaceAll("[.#$\\[\\]]", "_");
        chatRef = FirebaseDatabase.getInstance(MatchRepository.DB_URL)
                .getReference("chats").child(safeRegion);
    }

    public void sendMessage(String text, String senderName) {
        if (chatRef == null || text == null || text.trim().isEmpty()) return;
        String uid = currentUid();
        if (uid == null) return;
        ChatMessage msg = new ChatMessage(uid, senderName, text.trim(), System.currentTimeMillis());
        chatRef.push().setValue(msg);
        String region = chatRef.getKey();
        notifyOtherPlayers(msg, region);
    }
    private void notifyOtherPlayers(ChatMessage msg, String region) {
        String myUid = currentUid();
        if (myUid == null) return;
        android.util.Log.d("CHAT_NOTIF", "Sending chat notif. My uid: " + myUid + ", region: " + region);
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("region", region)
                .get()
                .addOnSuccessListener(query -> {
                    android.util.Log.d("CHAT_NOTIF", "Firestore query returned " + query.size() + " users");
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : query) {
                        String uid = doc.getId();
                        String userRegion = doc.getString("region");
                        android.util.Log.d("CHAT_NOTIF", "Found user: " + uid + ", region: " + userRegion);
                        if (uid.equals(myUid)) continue;
                        NotificationHelper.notifyChatMessage(uid, msg.senderName, region);
                        android.util.Log.d("CHAT_NOTIF", "Sent notification to: " + uid);
                    }

                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CHAT_NOTIF", "Firestore query failed: " + e.getMessage());
                });
    }
    public void startListening() {
        if (chatRef == null || childListener != null) return;
        childListener = new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot snap, @Nullable String prev) {
                ChatMessage msg = snap.getValue(ChatMessage.class);
                if (msg != null) {
                    for (ChatListener l : listeners) l.onMessageAdded(msg);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snap, @Nullable String prev) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snap) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snap, @Nullable String prev) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                for (ChatListener l : listeners) l.onError(error.getMessage());
            }
        };
        chatRef.addChildEventListener(childListener);
    }

    public void addListener(ChatListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ChatListener listener) {
        listeners.remove(listener);
    }

    public void leaveChat() {
        if (chatRef != null && childListener != null) {
            chatRef.removeEventListener(childListener);
        }
        childListener = null;
        chatRef = null;
    }
}