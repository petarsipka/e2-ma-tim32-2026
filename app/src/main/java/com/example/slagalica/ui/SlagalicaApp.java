package com.example.slagalica.ui;

import android.app.Application;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;

/**
 * App entry point. Signs the device in anonymously so every player gets a stable
 * Firebase {@code uid} before entering a match lobby — there is no login/register
 * for this checkpoint, access to a match is anonymous.
 */
public class SlagalicaApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously()
                .addOnFailureListener(e -> Log.e("SlagalicaApp", "Anonymous sign-in failed", e));
        }
    }
}
