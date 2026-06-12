package com.example.slagalica.ui;

import android.content.Intent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.data.UserTemporaryDB;
import com.example.slagalica.data.model.User;
import com.example.slagalica.ui.profile.ProfileActivity;

public abstract class BaseActivity extends AppCompatActivity {

    protected void setupStatsBar() {
        TextView tvTokens = findViewById(com.example.slagalica.R.id.tvNavTokens);
        TextView tvStars = findViewById(com.example.slagalica.R.id.tvNavStars);
        TextView tvLeague = findViewById(com.example.slagalica.R.id.tvNavLeague);
        if (tvTokens == null) return;

        User user = new UserTemporaryDB().getCurrentUser();
        tvTokens.setText("🪙 " + user.getTokens());
        tvStars.setText("⭐ " + user.getStars());
        String leagueShort = user.getLeague().toString().replace(" liga", "");
        tvLeague.setText("🏆 " + leagueShort);

        android.view.View profileBtn = findViewById(com.example.slagalica.R.id.btnNavProfile);
        if (profileBtn != null) {
            profileBtn.setOnClickListener(v -> {
                if (!(this instanceof ProfileActivity)) {
                    startActivity(new Intent(this, ProfileActivity.class));
                }
            });
        }
    }
}
