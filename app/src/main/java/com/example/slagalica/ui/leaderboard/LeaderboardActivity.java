package com.example.slagalica.ui.leaderboard;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;
import com.example.slagalica.data.LeaderboardRepository;
import com.example.slagalica.data.model.LeaderboardEntry;
import com.example.slagalica.util.SystemBars;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private LeaderboardAdapter adapter;
    private LeaderboardRepository repo;
    private Button btnWeekly, btnMonthly;
    private TextView tvEmpty;
    private TextView tvDateRange;
    private LinearLayout tabContainer;
    private Button btnAdminReset;

    private boolean showingWeekly = true;
    private android.os.Handler refreshHandler = new android.os.Handler();
    private Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            if (showingWeekly) loadWeekly(); else loadMonthly();
            refreshHandler.postDelayed(this, 120000); // 2 minutes
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        SystemBars.apply(this);

        recycler = findViewById(R.id.lbRecycler);
        btnWeekly = findViewById(R.id.lbBtnWeekly);
        btnMonthly = findViewById(R.id.lbBtnMonthly);
        tvEmpty = findViewById(R.id.lbEmpty);
        tabContainer = findViewById(R.id.lbTabContainer);
        tvDateRange = findViewById(R.id.lbDateRange);
        btnAdminReset = findViewById(R.id.lbAdminReset);

        adapter = new LeaderboardAdapter(null);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        String email = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";
        if ("maksimfaks2@gmail.com".equals(email)) {
            btnAdminReset.setVisibility(View.VISIBLE);
            btnAdminReset.setOnClickListener(v -> showResetDialog());
        }
        repo = new LeaderboardRepository();

        btnWeekly.setOnClickListener(v -> {
            showingWeekly = true;
            updateTabs();
            loadWeekly();
        });

        btnMonthly.setOnClickListener(v -> {
            showingWeekly = false;
            updateTabs();
            loadMonthly();
        });

        updateTabs();
        loadWeekly();

    }
    @Override protected void onResume() {
        super.onResume();
        refreshHandler.postDelayed(refreshRunnable, 120000);
    }

    @Override protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void updateTabs() {
        if (showingWeekly) {
            btnWeekly.setBackgroundTintList(getColorStateList(R.color.accent));
            btnWeekly.setTextColor(getColor(R.color.on_accent));
            btnMonthly.setBackgroundTintList(getColorStateList(R.color.panel));
            btnMonthly.setTextColor(getColor(R.color.text));
            tvDateRange.setText(getCycleRange(showingWeekly));
        } else {
            btnWeekly.setBackgroundTintList(getColorStateList(R.color.panel));
            btnWeekly.setTextColor(getColor(R.color.text));
            btnMonthly.setBackgroundTintList(getColorStateList(R.color.accent));
            btnMonthly.setTextColor(getColor(R.color.on_accent));
            tvDateRange.setText(getCycleRange(showingWeekly));
        }
    }

    private void loadWeekly() {
        tvEmpty.setVisibility(View.GONE);
        repo.getWeekly(new LeaderboardRepository.LeaderboardCallback() {
            @Override public void onLoaded(List<LeaderboardEntry> entries) {
                if (entries.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    adapter.setEntries(new ArrayList<>());
                } else {
                    adapter.setEntries(entries);
                }
            }
            @Override public void onError(String error) {
                tvEmpty.setText("Greška: " + error);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadMonthly() {
        tvEmpty.setVisibility(View.GONE);
        repo.getMonthly(new LeaderboardRepository.LeaderboardCallback() {
            @Override public void onLoaded(List<LeaderboardEntry> entries) {
                if (entries.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    adapter.setEntries(new ArrayList<>());
                } else {
                    adapter.setEntries(entries);
                }
            }
            @Override public void onError(String error) {
                tvEmpty.setText("Greška: " + error);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
    private String getCycleRange(boolean weekly) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.", java.util.Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (weekly) {
            cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
            String start = sdf.format(cal.getTime());
            cal.add(java.util.Calendar.DAY_OF_WEEK, 6);
            String end = sdf.format(cal.getTime());
            return start + " – " + end;
        } else {
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
            String start = sdf.format(cal.getTime());
            cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
            String end = sdf.format(cal.getTime());
            return start + " – " + end;
        }
    }
    private void showResetDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Admin Reset")
                .setMessage("Reset weekly or monthly leaderboard?")
                .setPositiveButton("Weekly", (d, w) -> {
                    new LeaderboardRepository().resetWeekly(() -> runOnUiThread(() -> {
                        Toast.makeText(this, "Weekly reset", Toast.LENGTH_SHORT).show();
                        if (showingWeekly) loadWeekly(); else loadMonthly();
                    }));
                })
                .setNegativeButton("Monthly", (d, w) -> {
                    new LeaderboardRepository().resetMonthly(() -> runOnUiThread(() -> {
                        Toast.makeText(this, "Monthly reset", Toast.LENGTH_SHORT).show();
                        if (showingWeekly) loadWeekly();
                        else loadMonthly();
                    }));
                })
                .setNeutralButton("Cancel", null)
                .show();
    }
}