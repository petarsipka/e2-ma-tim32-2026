package com.example.slagalica.data;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthRepository {
    private final FirebaseAuth auth;

    public AuthRepository() {
        auth = FirebaseAuth.getInstance();
    }

    public void register(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        user.sendEmailVerification()
                                .addOnSuccessListener(unused -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onError("Account created but failed to send verification email: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void login(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null && user.isEmailVerified()) {
                        callback.onSuccess();
                    } else if (user != null) {
                        auth.signOut();
                        callback.onError("Please verify your email before logging in.");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void changePassword(String oldPassword, String newPassword, AuthCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            callback.onError("You must be logged in to change password.");
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(unused2 -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError("Old password is incorrect."));
    }

    public void sendPasswordResetEmail(String email, AuthCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void signOut() {
        auth.signOut();
    }

    public interface AuthCallback {
        void onSuccess();
        void onError(String error);
    }
}