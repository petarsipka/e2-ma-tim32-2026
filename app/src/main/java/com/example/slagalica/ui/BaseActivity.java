package com.example.slagalica.ui;

import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slagalica.R;
import com.example.slagalica.data.UserRepository;
import com.example.slagalica.data.model.User;

public abstract class BaseActivity extends AppCompatActivity {

    /** Wire up the shared in-game header: currency chips, quit, and the "me" side
     *  of the scoreboard. Call once from onCreate after setContentView. */
    protected void setupStatsBar() {
        TextView tvTokens = findViewById(R.id.tvNavTokens);
        TextView tvStars = findViewById(R.id.tvNavStars);
        TextView tvLeague = findViewById(R.id.tvNavLeague);
        if (tvTokens == null) return;

        User user = new UserRepository().getCurrentUser();
        tvTokens.setText("🪙 " + user.getTokens());
        tvStars.setText("⭐ " + user.getStars());
        String leagueShort = user.getLeague().replace(" liga", "").replace("\n", " ");
        tvLeague.setText("🏆 " + leagueShort);

        View quitBtn = findViewById(R.id.btnNavQuit);
        if (quitBtn != null) quitBtn.setOnClickListener(v -> finish());

        // "Me" side of the match scoreboard.
        TextView meName = findViewById(R.id.scoreMeName);
        if (meName != null) {
            String me = user.getUsername();
            meName.setText(me);
            setText(R.id.scoreMeAv, initials(me));
        }
    }

    /** Set the opponent shown on the scoreboard (name + derived initials). */
    protected void setOpponent(String name) {
        if (findViewById(R.id.scoreOppName) == null) return;
        setText(R.id.scoreOppName, name);
        setText(R.id.scoreOppAv, initials(name));
    }

    /** Update both players' points on the scoreboard. */
    protected void updateScore(int mePoints, int opponentPoints) {
        if (findViewById(R.id.scoreMePts) == null) return;
        setText(R.id.scoreMePts, String.valueOf(mePoints));
        setText(R.id.scoreOppPts, String.valueOf(opponentPoints));
    }

    private void setText(int id, String value) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(value);
    }

    /** First letters of up to two words, e.g. "Marko Novak" -> "MN", "Marko_NS" -> "MN". */
    protected static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("[\\s_]+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
            if (sb.length() == 2) break;
        }
        return sb.toString();
    }
}
