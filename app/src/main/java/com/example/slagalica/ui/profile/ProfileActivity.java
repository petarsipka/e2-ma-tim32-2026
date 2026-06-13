package com.example.slagalica.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.data.model.User;
import com.example.slagalica.data.model.UserStats;
import com.example.slagalica.ui.BaseActivity;
import com.example.slagalica.ui.LoginActivity;
import com.example.slagalica.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends BaseActivity {

    private ActivityProfileBinding binding;
    private ProfileViewModel viewModel;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check auth first
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || !auth.getCurrentUser().isEmailVerified()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        viewModel.getUserData().observe(this, user -> {
            if (user == null) {
                Toast.makeText(this, "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
                return;
            }
            binding.tvUsername.setText(user.getUsername());
            binding.tvEmail.setText(user.getEmail());
            binding.tvTokens.setText(String.valueOf(user.getTokens()));
            binding.tvStars.setText(user.getStars() + " ★");
            binding.tvLeague.setText("Liga " + user.getLeague());
            binding.tvRegion.setText(user.getRegion());
        });

        viewModel.getUserStats().observe(this, stats -> {
            if (stats == null) return;
            binding.tvStatKZZ.setText(stats.getStatKZZ());
            binding.tvStatMB.setText(stats.getStatMB());
            binding.tvStatKPK.setText(stats.getStatKPK());
            binding.tvStatAsoc.setText(stats.getStatAsoc());
            binding.tvStatSkocko.setText(stats.getStatSkocko());
            binding.tvStatSpojnice.setText(stats.getStatSpojnice());
            binding.tvStatPartije.setText(String.valueOf(stats.getTotalGames()));

            if (stats.getTotalGames() > 0) {
                binding.tvStatWinLoss.setText(
                        "Pobede: " + stats.getWins() + "\n" +
                                (stats.getWins() * 100 / stats.getTotalGames()) + "%\n\n" +
                                "Porazi: " + stats.getLosses() + "\n" +
                                (stats.getLosses() * 100 / stats.getTotalGames()) + "%"
                );
            } else {
                binding.tvStatWinLoss.setText("—");
            }
        });

        viewModel.loadUser();
        setupListeners();
    }

    private void setupListeners() {
        binding.btnChangeAvatar.setOnClickListener(v ->
                Toast.makeText(this, "Izmena avatara nije još implementirana", Toast.LENGTH_SHORT).show()
        );

        binding.btnLogout.setOnClickListener(v -> {
            auth.signOut(); // Actually signs out from Firebase
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}