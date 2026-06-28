package com.example.slagalica.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.ChatRepository;
import com.example.slagalica.data.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends ViewModel {

    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final ChatRepository repo = new ChatRepository();

    public void init(String region, String senderName) {
        repo.joinRegionChat(region);
        repo.addListener(new ChatRepository.ChatListener() {
            @Override public void onMessageAdded(ChatMessage msg) {
                List<ChatMessage> list = messages.getValue();
                if (list == null) list = new ArrayList<>();
                list.add(msg);
                messages.postValue(list);
            }
            @Override public void onError(String err) {
                error.postValue(err);
            }
        });
        repo.startListening();
    }

    public void send(String text, String senderName) {
        repo.sendMessage(text, senderName);
    }

    public LiveData<List<ChatMessage>> getMessages() { return messages; }
    public LiveData<String> getError() { return error; }

    public String currentUid() {
        return repo.currentUid();
    }

    @Override protected void onCleared() {
        super.onCleared();
        repo.leaveChat();
    }
}