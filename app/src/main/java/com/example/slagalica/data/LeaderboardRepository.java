package com.example.slagalica.data;

import com.example.slagalica.data.model.LeaderboardEntry;
import com.example.slagalica.data.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface LeaderboardCallback {
        void onLoaded(List<LeaderboardEntry> entries);
        void onError(String error);
    }

    public void getWeekly(LeaderboardCallback cb) {
        db.collection("leaderboard_weekly")
                .orderBy("stars", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(query -> {
                    List<LeaderboardEntry> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        list.add(toEntry(doc));
                    }
                    cb.onLoaded(list);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public void getMonthly(LeaderboardCallback cb) {
        db.collection("leaderboard_monthly")
                .orderBy("stars", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(query -> {
                    List<LeaderboardEntry> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        list.add(toEntry(doc));
                    }
                    cb.onLoaded(list);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public void getMonthlyByRegion(String region, LeaderboardCallback cb) {
        if (region == null || region.isEmpty()) {
            cb.onLoaded(new ArrayList<>());
            return;
        }
        db.collection("leaderboard_monthly")
                .whereEqualTo("region", region)
                .orderBy("stars", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(query -> {
                    List<LeaderboardEntry> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        list.add(toEntry(doc));
                    }
                    cb.onLoaded(list);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    private LeaderboardEntry toEntry(QueryDocumentSnapshot doc) {
        LeaderboardEntry e = new LeaderboardEntry();
        e.uid = doc.getId();
        e.username = doc.getString("username");
        e.region = doc.getString("region");
        Long s = doc.getLong("stars");
        e.stars = s != null ? s.intValue() : 0;
        Long l = doc.getLong("leagueOrdinal");
        e.leagueOrdinal = l != null ? l.intValue() : 0;
        return e;
    }

    // Call this after each match to update leaderboard
    public void recordMatchResult(String uid, int starsEarned) {
        // Update weekly
        updateStars("leaderboard_weekly", uid, starsEarned);
        // Update monthly
        updateStars("leaderboard_monthly", uid, starsEarned);
    }

    private void updateStars(String collection, String uid, int delta) {
        db.collection(collection).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long current = doc.getLong("stars");
                        int newStars = (current != null ? current.intValue() : 0) + delta;
                        db.collection(collection).document(uid)
                                .update("stars", Math.max(0, newStars));
                    } else {
                        // Fetch user data to create entry
                        new UserRepository().getUserByUid(uid, new UserRepository.UserFetchCallback() {
                            @Override public void onSuccess(User user) {
                                db.collection(collection).document(uid).set(new java.util.HashMap<String, Object>() {{
                                    put("username", user.getUsername());
                                    put("region", user.getRegion());
                                    put("stars", Math.max(0, delta));
                                    put("leagueOrdinal", user.getLeague().ordinal());
                                }});
                            }
                            @Override public void onError(String error) {}
                        });
                    }
                });
    }
    public void updateUserStars(String uid, int delta) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    Long current = doc.getLong("stars");
                    int newStars = (current != null ? current.intValue() : 0) + delta;
                    db.collection("users").document(uid).update("stars", Math.max(0, newStars));
                });
    }
    // Manual reset
    public void resetWeekly(Runnable onDone) {
        deleteCollection("leaderboard_weekly", onDone);
    }

    public void resetMonthly(Runnable onDone) {
        deleteCollection("leaderboard_monthly", onDone);
    }

    private void deleteCollection(String name, Runnable onDone) {
        db.collection(name).get().addOnSuccessListener(query -> {
            for (QueryDocumentSnapshot doc : query) {
                doc.getReference().delete();
            }
            if (onDone != null) onDone.run();
        }).addOnFailureListener(e -> {
            if (onDone != null) onDone.run();
        });
    }
}