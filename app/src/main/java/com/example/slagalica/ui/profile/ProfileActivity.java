package com.example.slagalica.ui.profile;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.slagalica.R;
import com.example.slagalica.data.AvatarRepository;
import com.example.slagalica.data.model.GameStat;
import com.example.slagalica.databinding.ActivityProfileBinding;
import com.example.slagalica.ui.AvatarBinder;
import com.example.slagalica.ui.BaseActivity;
import com.example.slagalica.ui.LoginActivity;
import com.example.slagalica.util.ImageUtils;
import com.example.slagalica.util.QrGenerator;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends BaseActivity {

    private ActivityProfileBinding binding;
    private ProfileViewModel viewModel;
    private FirebaseAuth auth;

    private int[] gameColors;
    private Bitmap qrBitmap;

    private final AvatarRepository avatarRepository = new AvatarRepository();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<PickVisualMediaRequest> avatarPicker;

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

        gameColors = new int[]{
                ContextCompat.getColor(this, R.color.g0),
                ContextCompat.getColor(this, R.color.g1),
                ContextCompat.getColor(this, R.color.g2),
                ContextCompat.getColor(this, R.color.g3)
        };

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        viewModel.getUserData().observe(this, user -> {
            if (user == null) {
                Toast.makeText(this, "Greška pri učitavanju profila", Toast.LENGTH_SHORT).show();
                return;
            }
            binding.tvAvatar.setText(initials(user.getUsername()));
            binding.tvUsername.setText(user.getUsername());
            binding.tvEmail.setText(user.getEmail());
            binding.tvLeague.setText("🏆 " + user.getLeague());
            binding.tvRegion.setText("📍 " + user.getRegion());
            binding.tvTokens.setText(String.valueOf(user.getTokens()));
            binding.tvStars.setText(formatThousands(user.getStars()));
            setupQr("SLAGALICA-INVITE:" + user.getUsername());
        });

        viewModel.getUserStats().observe(this, stats -> {
            binding.tvPartije.setText(String.valueOf(stats.getTotalGames()));
            binding.ringWin.setPercent(stats.getWinPercent());
            binding.tvWinLoss.setText(stats.getWins() + " pobeda · " + stats.getLosses() + " poraza");
            buildStatRows(stats.getGames());
        });

        viewModel.loadUser();
        setupAvatar();
        setupListeners();
    }

    /** Register the system photo picker and load the saved avatar (circular). */
    private void setupAvatar() {
        avatarPicker = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> { if (uri != null) onAvatarPicked(uri); });

        AvatarBinder.bindCurrentUser(binding.ivAvatar, binding.tvAvatar);
    }

    /** Encode the picked image off the main thread, then persist and display it. */
    private void onAvatarPicked(Uri uri) {
        Toast.makeText(this, "Učitavanje slike…", Toast.LENGTH_SHORT).show();
        ioExecutor.execute(() -> {
            String base64 = ImageUtils.uriToBase64(getContentResolver(), uri);
            runOnUiThread(() -> {
                if (binding == null) return;
                if (base64 == null) {
                    Toast.makeText(this, "Slika nije učitana", Toast.LENGTH_SHORT).show();
                    return;
                }
                AvatarBinder.show(binding.ivAvatar, binding.tvAvatar, ImageUtils.base64ToBitmap(base64));
                avatarRepository.saveAvatar(base64, success -> {
                    if (binding == null) return;
                    Toast.makeText(this,
                            success ? "Profilna slika sačuvana" : "Čuvanje nije uspelo",
                            Toast.LENGTH_SHORT).show();
                });
            });
        });
    }


    /** Inflate one bar row per game (Sunny pop .stat-row). */
    private void buildStatRows(List<GameStat> games) {
        LinearLayout container = binding.statRows;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (GameStat g : games) {
            View row = inflater.inflate(R.layout.view_stat_row, container, false);
            ((TextView) row.findViewById(R.id.statName)).setText(g.getName());
            ((TextView) row.findViewById(R.id.statValue)).setText(g.getValueLabel());

            int pct = Math.max(0, Math.min(100, g.getPercent()));
            View fill = row.findViewById(R.id.statFill);
            View spacer = row.findViewById(R.id.statSpacer);
            setWeight(fill, pct);
            setWeight(spacer, 100 - pct);
            fill.setBackgroundTintList(ColorStateList.valueOf(gameColors[g.getColorIndex() % gameColors.length]));

            container.addView(row);
        }
    }

    private static void setWeight(View view, int weight) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) view.getLayoutParams();
        lp.weight = weight;
        view.setLayoutParams(lp);
    }

    private void setupQr(String content) {
        qrBitmap = QrGenerator.encode(content, 512);
        if (qrBitmap != null) binding.ivQrCode.setImageBitmap(qrBitmap);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.ivQrCode.setOnClickListener(v -> showQrDialog());

        binding.btnChangeAvatar.setOnClickListener(v ->
                avatarPicker.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        binding.btnLogout.setOnClickListener(v -> {
            auth.signOut(); // Actually signs out from Firebase
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        // Settings button is intentionally inert for now (no settings screen yet).
    }

    private void showQrDialog() {
        if (qrBitmap == null) {
            Toast.makeText(this, "QR kod nije dostupan", Toast.LENGTH_SHORT).show();
            return;
        }
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_qr, null, false);
        ((ImageView) content.findViewById(R.id.ivQrLarge)).setImageBitmap(qrBitmap);
        new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton("Zatvori", null)
                .show();
    }

    /** Group thousands with a dot, matching the design ("1.240"). */
    private static String formatThousands(int value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,###", symbols).format(value);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
        binding = null;
    }
}