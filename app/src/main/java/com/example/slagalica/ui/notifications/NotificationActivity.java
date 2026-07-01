package com.example.slagalica.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;
import com.example.slagalica.data.model.AppNotification;
import com.example.slagalica.ui.chat.ChatActivity;
import com.example.slagalica.ui.leaderboard.LeaderboardActivity;
import com.example.slagalica.util.SystemBars;

import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private NotificationAdapter adapter;
    private NotificationViewModel viewModel;
    private TextView tvEmpty;
    private Button btnMarkAll;
    private Spinner spinnerFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        SystemBars.apply(this);

        recycler = findViewById(R.id.notifRecycler);
        tvEmpty = findViewById(R.id.notifEmpty);
        btnMarkAll = findViewById(R.id.notifMarkAll);
        spinnerFilter = findViewById(R.id.notifFilter);

        adapter = new NotificationAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(NotificationViewModel.class);

        adapter.setListener(new NotificationAdapter.OnNotifClickListener() {
            @Override public void onClick(AppNotification notif) {
                viewModel.markAsRead(notif.id);
                handleNotificationAction(notif);
            }
            @Override public void onLongClick(AppNotification notif) {
                showNotifOptions(notif);
            }
        });

        btnMarkAll.setOnClickListener(v -> viewModel.markAllAsRead());
        Button btnTest = findViewById(R.id.notifTest);
        if (btnTest != null) {
            btnTest.setOnClickListener(v -> {
                String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                        ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                if (uid != null) {
                    com.example.slagalica.data.NotificationHelper.sendNotification(
                            uid, "rewards", "Test nagrada", "Ovo je test obaveštenja!", ""
                    );
                }
            });
        }

        setupFilter();

        viewModel.init();
        viewModel.getNotifications().observe(this, notifs -> {
            String filter = spinnerFilter.getSelectedItem().toString();
            List<AppNotification> filtered = viewModel.getFiltered(
                    filter.equals("Nepročitane") ? "unread" :
                            filter.equals("Pročitane") ? "read" : "all"
            );
            adapter.setNotifications(filtered);
            tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void setupFilter() {
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Sve", "Nepročitane", "Pročitane"});
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                viewModel.getNotifications().observe(NotificationActivity.this, notifs -> {
                    String filter = pos == 1 ? "unread" : pos == 2 ? "read" : "all";
                    List<AppNotification> filtered = viewModel.getFiltered(filter);
                    adapter.setNotifications(filtered);
                    tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void handleNotificationAction(AppNotification notif) {
        switch (notif.type) {
            case "chat":
                if (notif.actionData != null && !notif.actionData.isEmpty()) {
                    Intent i = new Intent(this, ChatActivity.class);
                    i.putExtra(ChatActivity.EXTRA_REGION, notif.actionData);
                    startActivity(i);
                }
                break;
            case "ranking":
            case "rewards":
                startActivity(new Intent(this, LeaderboardActivity.class));
                break;
        }
    }

    private void showNotifOptions(AppNotification notif) {
        new AlertDialog.Builder(this)
                .setTitle(notif.title)
                .setMessage(notif.message)
                .setPositiveButton("Označi kao pročitanu", (d, w) -> viewModel.markAsRead(notif.id))
                .setNegativeButton("Zatvori", null)
                .show();
    }
}