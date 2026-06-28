package com.example.slagalica.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.R;
import com.example.slagalica.data.MatchRepository;
import com.example.slagalica.data.model.Match;
import com.example.slagalica.util.SystemBars;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * End-of-match result: hero verdict (win/loss), per-game breakdown for both
 * players, and a full-width "Početna" button. Reads the match node once.
 */
public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_MATCH_CODE = "match_code";

    // Icon tile + glyph per game, in Match.GAME_KEYS order.
    private static final int[] TILE = {
            R.drawable.bg_tile_g0, R.drawable.bg_tile_g1, R.drawable.bg_tile_g2,
            R.drawable.bg_tile_g3, R.drawable.bg_tile_g0, R.drawable.bg_tile_g1
    };
    private static final String[] GLYPH = {"?", "⇄", "▦", "✦", "▁▄█", "123"};

    private String matchCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        SystemBars.apply(this);

        matchCode = getIntent().getStringExtra(EXTRA_MATCH_CODE);

        findViewById(R.id.btnHome).setOnClickListener(v -> goHome());

        if (matchCode == null) return;
        // Live listener: totals may still be settling when we arrive, so keep updating.
        FirebaseDatabase.getInstance(MatchRepository.DB_URL)
                .getReference("matches").child(matchCode)
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        Match m = snap.getValue(Match.class);
                        if (m != null) render(m);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void render(Match m) {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String oppUid = m.opponentOf(myUid);

        int myTotal = m.totalScore(myUid);
        int oppTotal = m.totalScore(oppUid);

        // Verdict
        TextView label = findViewById(R.id.tvVerdictLabel);
        TextView title = findViewById(R.id.tvVerdictTitle);
        if (myTotal > oppTotal) {
            label.setText("POBEDA");
            title.setText("Bravo!");
        } else if (myTotal < oppTotal) {
            label.setText("PORAZ");
            title.setText("Više sreće drugi put");
        } else {
            label.setText("NEREŠENO");
            title.setText("Nerešeno!");
        }

        // Names + avatars (roles: Domaćin / Gost)
        String myName = nameOf(m, myUid, "Vi");
        String oppName = nameOf(m, oppUid, "Protivnik");
        ((TextView) findViewById(R.id.tvMeName)).setText(myName);
        ((TextView) findViewById(R.id.tvOppName)).setText(oppName);
        ((TextView) findViewById(R.id.tvMeAv)).setText(initial(myName));
        ((TextView) findViewById(R.id.tvOppAv)).setText(initial(oppName));
        // Show each player's profile photo over the initials when they have one.
        AvatarBinder.bindUser(myUid, findViewById(R.id.ivMeAv), findViewById(R.id.tvMeAv));
        AvatarBinder.bindUser(oppUid, findViewById(R.id.ivOppAv), findViewById(R.id.tvOppAv));
        ((TextView) findViewById(R.id.tvMeTotal)).setText(String.valueOf(myTotal));
        ((TextView) findViewById(R.id.tvOppTotal)).setText(String.valueOf(oppTotal));

        // Per-game rows
        LinearLayout container = findViewById(R.id.resultRows);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < Match.GAME_KEYS.length; i++) {
            View row = inflater.inflate(R.layout.view_result_row, container, false);
            TextView icon = row.findViewById(R.id.resIcon);
            icon.setBackgroundResource(TILE[i]);
            icon.setText(GLYPH[i]);
            ((TextView) row.findViewById(R.id.resName)).setText(Match.GAME_NAMES[i]);
            ((TextView) row.findViewById(R.id.resMy))
                    .setText(String.valueOf(m.gameScore(myUid, Match.GAME_KEYS[i])));
            ((TextView) row.findViewById(R.id.resOpp))
                    .setText(String.valueOf(m.gameScore(oppUid, Match.GAME_KEYS[i])));
            container.addView(row);
        }
    }

    private static String nameOf(Match m, String uid, String fallback) {
        if (uid != null && m.players != null) {
            Match.Player p = m.players.get(uid);
            if (p != null && p.name != null) return p.name;
        }
        return fallback;
    }

    private static String initial(String name) {
        return (name == null || name.isEmpty())
                ? "?" : String.valueOf(Character.toUpperCase(name.charAt(0)));
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
