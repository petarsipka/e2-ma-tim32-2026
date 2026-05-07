package com.example.slagalica.ui.profile;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private ProfileViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        viewModel.getUserData().observe(this, user -> {
            binding.tvUsername.setText(user.getUsername());
            binding.tvEmail.setText(user.getEmail());
            binding.tvTokens.setText(String.valueOf(user.getTokens()));
            binding.tvStars.setText(user.getStars() + " ★");
            binding.tvLeague.setText(user.getLeague());
            binding.tvRegion.setText(user.getRegion());
        });

        viewModel.getUserStats().observe(this, stats -> {
            binding.tvStatKZZ.setText(stats.getStatKZZ());
            binding.tvStatMB.setText(stats.getStatMB());
            binding.tvStatKPK.setText(stats.getStatKPK());
            binding.tvStatAsoc.setText(stats.getStatAsoc());
            binding.tvStatSkocko.setText(stats.getStatSkocko());
            binding.tvStatSpojnice.setText(stats.getStatSpojnice());
            binding.tvStatPartije.setText(String.valueOf(stats.getTotalGames()));
            binding.tvStatWinLoss.setText(
                "Pobede: " + stats.getWins() + "\n" +
                (stats.getWins() * 100 / stats.getTotalGames()) + "%\n\n" +
                "Porazi: " + stats.getLosses() + "\n" +
                (stats.getLosses() * 100 / stats.getTotalGames()) + "%"
            );
        });

        viewModel.loadUser();
        setupListeners();
    }

    private void setupListeners() {
        binding.btnChangeAvatar.setOnClickListener(v ->
            Toast.makeText(this, "Izmena avatara nije još implementirana", Toast.LENGTH_SHORT).show()
        );
        binding.btnLogout.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
