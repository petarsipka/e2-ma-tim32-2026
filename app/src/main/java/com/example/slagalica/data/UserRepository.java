package com.example.slagalica.data;

import com.example.slagalica.data.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Firestore-backed access to the {@code users} collection: create a user document
 * and look users up by username (used by registration / login). Demo profile data
 * for the UI lives separately in {@link UserTemporaryDB} until this is fully wired.
 */
public class UserRepository {
    private final FirebaseFirestore db;

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void saveUser(String uid, User user, UserCallback callback) {
        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getEmailByUsername(String username, UsernameCallback callback) {
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty() && query.getDocuments().get(0).getString("email") != null) {
                        callback.onFound(query.getDocuments().get(0).getString("email"));
                    } else {
                        callback.onNotFound();
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void checkUsernameExists(String username, UserCallback callback) {
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) callback.onSuccess();
                    else callback.onError("Username already taken.");
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getUserByUid(String uid, UserFetchCallback callback) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }



    public interface UserCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface UsernameCallback {
        void onFound(String email);
        void onNotFound();
        void onError(String error);
    }
    public interface UserFetchCallback {
        void onSuccess(User user);
        void onError(String error);
    }
}
