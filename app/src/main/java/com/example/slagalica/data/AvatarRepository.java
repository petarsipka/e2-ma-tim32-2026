package com.example.slagalica.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Reads and writes the current player's avatar as a Base64 string at
 * {@code /users/{uid}/avatar} in Realtime Database. Lives in RTDB (not Storage) so
 * it needs no extra Firebase product enabled; can move to Firestore/Storage later.
 */
public class AvatarRepository {

    /** Notified with the loaded avatar string, or {@code null} if none/error. */
    public interface AvatarCallback {
        void onAvatar(@Nullable String base64);
    }

    private final DatabaseReference usersRoot =
            FirebaseDatabase.getInstance(MatchRepository.DB_URL).getReference("users");

    @Nullable
    private String currentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /** Fetch the current player's avatar once. */
    public void loadAvatar(@NonNull AvatarCallback callback) {
        loadAvatar(currentUid(), callback);
    }

    /**
     * Fetch any player's avatar once by uid. Calls back with {@code null} if the uid
     * is missing, the avatar is unset, or the read fails.
     */
    public void loadAvatar(@Nullable String uid, @NonNull AvatarCallback callback) {
        if (uid == null) {
            callback.onAvatar(null);
            return;
        }
        usersRoot.child(uid).child("avatar").get()
                .addOnSuccessListener(snapshot -> callback.onAvatar(snapshot.getValue(String.class)))
                .addOnFailureListener(e -> callback.onAvatar(null));
    }

    /**
     * Save the avatar string. {@code onResult} reports whether the write succeeded;
     * fails fast if there is no signed-in user yet.
     */
    public void saveAvatar(@NonNull String base64, @NonNull OnResult onResult) {
        String uid = currentUid();
        if (uid == null) {
            onResult.onResult(false);
            return;
        }
        usersRoot.child(uid).child("avatar").setValue(base64)
                .addOnSuccessListener(unused -> onResult.onResult(true))
                .addOnFailureListener(e -> onResult.onResult(false));
    }

    /** Simple success/failure callback for the save. */
    public interface OnResult {
        void onResult(boolean success);
    }
}
