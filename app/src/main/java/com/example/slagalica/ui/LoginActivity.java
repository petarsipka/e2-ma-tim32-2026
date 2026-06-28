package com.example.slagalica.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.example.slagalica.R;

import com.example.slagalica.data.AuthRepository;
import com.example.slagalica.data.UserRepository;
import com.example.slagalica.util.SystemBars;

public class LoginActivity extends AppCompatActivity {
    private EditText lgInsertCredentials, lgInsertPassword;
    private Button lgLoginButton;
    private TextView rgLoginLink;
    private AuthRepository authRepo;
    private UserRepository userRepo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        SystemBars.apply(this);

        userRepo = new UserRepository();
        authRepo = new AuthRepository();
        // Check if already logged in
        if (authRepo.getCurrentUser() != null && authRepo.getCurrentUser().isEmailVerified()) {
            goToMain();
            return;
        }

        // Wire views to new IDs
        lgInsertCredentials = findViewById(R.id.lgInsertCredentials);
        lgInsertPassword = findViewById(R.id.lgInsertPassword);
        lgLoginButton = findViewById(R.id.lgLoginButton);
        rgLoginLink = findViewById(R.id.rgLoginLink);
        lgLoginButton.setOnClickListener(v -> attemptLogin());

        rgLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void attemptLogin() {
        String input = lgInsertCredentials.getText().toString().trim();
        String password = lgInsertPassword.getText().toString();

        if (input.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Popuni sva polja.", Toast.LENGTH_SHORT).show();
            return;
        }

        lgLoginButton.setEnabled(false);

        if (input.contains("@")) {
            loginWithEmail(input, password);
        } else {
            userRepo.getEmailByUsername(input, new UserRepository.UsernameCallback() {
                @Override
                public void onFound(String email) {
                    loginWithEmail(email, password);
                }
                @Override
                public void onNotFound() {
                    lgLoginButton.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Korisničko ime ne postoji.", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onError(String error) {
                    lgLoginButton.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Greška: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private void loginWithEmail(String email, String password) {
        authRepo.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                goToMain();
            }
            @Override
            public void onError(String error) {
                lgLoginButton.setEnabled(true);
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    private void goToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}