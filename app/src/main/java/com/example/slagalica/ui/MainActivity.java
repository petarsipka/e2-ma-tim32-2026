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
    }
}