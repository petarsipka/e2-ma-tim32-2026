package com.example.slagalica.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;

import com.example.slagalica.R;
import com.example.slagalica.ui.koznazna.KoZnaZnaActivity;
import com.example.slagalica.ui.asocijacije.AsocijacijeActivity;
import com.example.slagalica.ui.spojnice.SpojniceActivity;
import com.example.slagalica.ui.profile.ProfileActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button btnRegister = findViewById(R.id.mRegisterLink);
        Button btnLogin = findViewById(R.id.mLoginLink);
        Button btnKPK = findViewById(R.id.mKPKLink);
        Button btnMojBroj = findViewById(R.id.mMBLink);
        Button btnSkocko = findViewById(R.id.mSkockoLink);
        Button btnProfile = findViewById(R.id.mProfileLink);
        Button btnKoZnaZna = findViewById(R.id.mKoZnaZnaLink);
        Button btnAsocijacije = findViewById(R.id.mAsocijacijeLink);
        Button btnSpojnice = findViewById(R.id.mSpojniceLink);

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });
        btnKPK.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, KPKActivity.class);
            startActivity(intent);
        });
        btnMojBroj.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MojBrojActivity.class);
            startActivity(intent);
        });
        btnSkocko.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SkockoActivity.class);
            startActivity(intent);
        });
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
        btnKoZnaZna.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, KoZnaZnaActivity.class);
            startActivity(intent);
        });
        btnSpojnice.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SpojniceActivity.class);
            startActivity(intent);
        });
        btnAsocijacije.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AsocijacijeActivity.class);
            startActivity(intent);
        });
    }
}