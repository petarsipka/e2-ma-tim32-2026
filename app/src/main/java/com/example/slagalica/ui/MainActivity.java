package com.example.slagalica.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.slagalica.R;
import com.example.slagalica.ui.asocijacije.AsocijacijeActivity;
import com.example.slagalica.ui.koznazna.KoZnaZnaActivity;
import com.example.slagalica.ui.kpk.KPKActivity;
import com.example.slagalica.ui.mojbroj.MojBrojActivity;
import com.example.slagalica.ui.skocko.SkockoActivity;
import com.example.slagalica.ui.spojnice.SpojniceActivity;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
