package com.example.slagalica.ui.chat;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;
import com.example.slagalica.data.UserRepository;
import com.example.slagalica.data.model.User;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_REGION = "region";

    private ChatViewModel viewModel;
    private ChatAdapter adapter;
    private RecyclerView rvMessages;
    private EditText etInput;
    private Button btnSend;
    private String region;
    private String senderName = "Ja";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (v, insets) -> {
                    int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                    int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                    v.setPadding(0, top, 0, bottom);
                    return WindowInsetsCompat.CONSUMED;
                }
        );
        region = getIntent().getStringExtra(EXTRA_REGION);
        if (region == null || region.isEmpty()) {
            finish();
            return;
        }

        rvMessages = findViewById(R.id.chatRecycler);
        etInput = findViewById(R.id.chatInput);
        btnSend = findViewById(R.id.chatSend);

        adapter = new ChatAdapter(null); // uid set after VM init
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Fetch current user name from Firestore, then init VM
        String uid = viewModel.currentUid();
        if (uid != null) {
            new UserRepository().getUserByUid(uid, new UserRepository.UserFetchCallback() {
                @Override public void onSuccess(User user) {
                    if (user != null) {
                        senderName = user.getUsername();
                    }
                    initChat(uid);
                }
                @Override public void onError(String error) {
                    initChat(uid);
                }
            });
        } else {
            finish();
        }

        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                viewModel.send(text, senderName);
                etInput.setText("");
            }
        });
    }

    private void initChat(String uid) {
        adapter = new ChatAdapter(uid);
        rvMessages.setAdapter(adapter);
        viewModel.init(region, senderName);
        viewModel.getMessages().observe(this, msgs -> {
            adapter.setMessages(msgs);
            rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
    }
}