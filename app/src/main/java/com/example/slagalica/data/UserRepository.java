package com.example.slagalica.data;

import com.example.slagalica.data.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

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

    public interface UserCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface UsernameCallback {
        void onFound(String email);
        void onNotFound();
        void onError(String error);
    }
}