package com.example.slagalica.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.R;
import com.example.slagalica.data.AuthRepository;
import com.example.slagalica.data.UserRepository;
import com.example.slagalica.data.model.User;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etUsername, etPassword, etRepeatPassword;
    private Spinner spinnerRegion;
    private Button btnRegister;
    private TextView rgLoginLink;
    private String selectedRegion = "";

    private AuthRepository authRepo;
    private UserRepository userRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepo = new AuthRepository();
        userRepo = new UserRepository();

        etEmail = findViewById(R.id.rgInsertEmail);
        etUsername = findViewById(R.id.rgInsertUsername);
        etPassword = findViewById(R.id.rgInsertPassword);
        etRepeatPassword = findViewById(R.id.rgInsertRepeatPassword);
        spinnerRegion = findViewById(R.id.rgSpinnerRegion);
        btnRegister = findViewById(R.id.rgRegisterButton);
        rgLoginLink = findViewById(R.id.rgLoginLink);

        setupSpinner();

        btnRegister.setOnClickListener(v -> attemptRegister());

        rgLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.regions, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRegion.setAdapter(adapter);

        spinnerRegion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRegion = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void attemptRegister() {
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        String repeat = etRepeatPassword.getText().toString();

        if (email.isEmpty() || username.isEmpty() || password.isEmpty() || selectedRegion.isEmpty()) {
            Toast.makeText(this, "Popuni sva polja.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(repeat)) {
            Toast.makeText(this, "Lozinke se ne poklapaju.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Lozinka mora imati najmanje 6 karaktera.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);

        userRepo.checkUsernameExists(username, new UserRepository.UserCallback() {
            @Override
            public void onSuccess() {
                authRepo.register(email, password, new AuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        String uid = authRepo.getCurrentUser().getUid();
                        User user = new User(username, email, selectedRegion);
                        userRepo.saveUser(uid, user, new UserRepository.UserCallback() {
                            @Override
                            public void onSuccess() {
                                authRepo.signOut();
                                Toast.makeText(RegisterActivity.this,
                                        "Registracija uspešna! Verifikuj email pre prijave.",
                                        Toast.LENGTH_LONG).show();
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish();
                            }
                            @Override
                            public void onError(String error) {
                                btnRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "Greška pri čuvanju: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    @Override
                    public void onError(String error) {
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Registracija neuspešna: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
            @Override
            public void onError(String error) {
                btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}