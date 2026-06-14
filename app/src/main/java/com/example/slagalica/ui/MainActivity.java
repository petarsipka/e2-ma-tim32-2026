package com.example.slagalica.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.slagalica.R;
import com.example.slagalica.ui.asocijacije.AsocijacijeActivity;
import com.example.slagalica.ui.koznazna.KoZnaZnaActivity;
import com.example.slagalica.ui.kpk.KPKActivity;
import com.example.slagalica.ui.mojbroj.MojBrojActivity;
import com.example.slagalica.ui.spojnice.SpojniceActivity;
import com.example.slagalica.data.model.User;
import com.example.slagalica.ui.profile.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends BaseActivity {

    private TextView tvGreetingName, tvTokens, tvStars, tvLeague;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || !auth.getCurrentUser().isEmailVerified()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        tvGreetingName = findViewById(R.id.tvGreetingName);
        tvTokens = findViewById(R.id.tvTokens);
        tvStars = findViewById(R.id.tvStars);
        tvLeague = findViewById(R.id.tvLeague);

        android.view.View btnGoToProfile = findViewById(R.id.btnGoToProfile);
        btnGoToProfile.setOnClickListener(v -> startActivity(new Intent(this, com.example.slagalica.ui.profile.ProfileActivity.class)));

        Button btnDuel = findViewById(R.id.mLoginLink);
        View cardKZZ = findViewById(R.id.mKoZnaZnaLink);
        View cardSpojnice = findViewById(R.id.mSpojniceLink);
        View cardAsocijacije = findViewById(R.id.mAsocijacijeLink);
        View cardKPK = findViewById(R.id.mKPKLink);
        View cardMB = findViewById(R.id.mMBLink);
        View cardSkocko = findViewById(R.id.mSkockoLink);

        btnDuel.setOnClickListener(v -> startActivity(new Intent(this, com.example.slagalica.ui.lobby.LobbyActivity.class)));
        cardKZZ.setOnClickListener(v -> startActivity(new Intent(this, KoZnaZnaActivity.class)));
        cardSpojnice.setOnClickListener(v -> startActivity(new Intent(this, SpojniceActivity.class)));
        cardAsocijacije.setOnClickListener(v -> startActivity(new Intent(this, AsocijacijeActivity.class)));
        cardKPK.setOnClickListener(v -> startActivity(new Intent(this, KPKActivity.class)));
        cardMB.setOnClickListener(v -> startActivity(new Intent(this, MojBrojActivity.class)));
        cardSkocko.setOnClickListener(v -> startActivity(new Intent(this, SkockoActivity.class)));

        loadUserData();

    }
    private void loadUserData() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            displayUser(user);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void displayUser(User user) {
        // "Zdravo, [username]"
        tvGreetingName.setText(user.getUsername());

        // Tokens
        tvTokens.setText("🪙 " + user.getTokens());

        // Stars
        tvStars.setText("⭐ " + user.getStars());

        // League

        tvLeague.setText("🏆 " + user.getLeague().toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload here (not just onCreate) so a photo set on the profile screen shows
        // immediately when returning to the home header.
        ImageView avatar = findViewById(R.id.ivHomeAvatar);
        if (avatar != null) AvatarBinder.bindCurrentUserOrPlaceholder(avatar);
    }
}
