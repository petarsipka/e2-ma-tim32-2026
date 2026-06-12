package com.example.slagalica.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.slagalica.data.model.Match;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Owns one match node at {@code /matches/{code}} in Realtime Database: create it
 * (host), join it (guest), keep a live listener on it, and push score updates.
 *
 * <p>One instance tracks at most one active listener, so each screen that needs
 * to follow a match (lobby, game ViewModel) should hold its own instance and call
 * {@link #detach()} when done.
 */
public class MatchRepository {

    /** Code alphabet without easily-confused characters (no O/0, I/1). */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LEN = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Explicit database URL. Required because {@code google-services.json} predates
     * the Realtime Database and has no {@code firebase_url}, so the SDK would
     * otherwise default to the wrong (US) instance instead of this europe-west1 one.
     * Public so game ViewModels can reuse it when opening their own state references.
     */
    public static final String DB_URL =
            "https://slagalica-f21cc-default-rtdb.europe-west1.firebasedatabase.app";

    /** Notified whenever the match node changes. */
    public interface MatchListener {
        void onMatch(@NonNull Match match);
    }

    private final DatabaseReference matchesRoot =
            FirebaseDatabase.getInstance(DB_URL).getReference("matches");

    private DatabaseReference activeRef;
    private ValueEventListener activeListener;

    @Nullable
    public String currentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Create a new match as host and return its join code. The host is written as
     * the first player with score 0.
     */
    public String createMatch(String hostName) {
        String code = generateCode();
        String uid = currentUid();

        Map<String, Object> player = new HashMap<>();
        player.put("name", hostName);

        Map<String, Object> players = new HashMap<>();
        players.put(uid, player);

        Map<String, Object> scores = new HashMap<>();
        scores.put(uid, 0);

        Map<String, Object> data = new HashMap<>();
        data.put("host", uid);
        data.put("status", "waiting");
        data.put("players", players);
        data.put("scores", scores);

        matchesRoot.child(code).setValue(data);
        return code;
    }

    /**
     * Join an existing match as guest. {@code onMissing} runs if no match exists
     * for {@code code}.
     */
    public void joinMatch(String code, String guestName, @Nullable Runnable onMissing) {
        String uid = currentUid();
        DatabaseReference ref = matchesRoot.child(code);
        ref.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                if (onMissing != null) onMissing.run();
                return;
            }
            ref.child("players").child(uid).child("name").setValue(guestName);
            ref.child("scores").child(uid).setValue(0);
            ref.child("status").setValue("ready");
        }).addOnFailureListener(e -> {
            if (onMissing != null) onMissing.run();
        });
    }

    /** Start listening to the match node; replaces any previous listener. */
    public void listen(String code, MatchListener listener) {
        detach();
        activeRef = matchesRoot.child(code);
        activeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Match match = snapshot.getValue(Match.class);
                if (match != null) listener.onMatch(match);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MatchRepository", "listen cancelled: " + error.getMessage());
            }
        };
        activeRef.addValueEventListener(activeListener);
    }

    /** Push the local player's current score into the shared node. */
    public void setScore(String code, int score) {
        String uid = currentUid();
        if (uid == null) return;
        matchesRoot.child(code).child("scores").child(uid).setValue(score);
    }

    /** Push the local player's score for a single game (used by the result screen). */
    public void setGameScore(String code, String gameKey, int score) {
        String uid = currentUid();
        if (uid == null) return;
        matchesRoot.child(code).child("gameScores").child(uid).child(gameKey).setValue(score);
    }

    /** Stop listening to the match node. Safe to call multiple times. */
    public void detach() {
        if (activeRef != null && activeListener != null) {
            activeRef.removeEventListener(activeListener);
        }
        activeRef = null;
        activeListener = null;
    }

    private static String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
