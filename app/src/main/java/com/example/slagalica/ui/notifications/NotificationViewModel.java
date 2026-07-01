package com.example.slagalica.ui.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.NotificationRepository;
import com.example.slagalica.data.model.AppNotification;

import java.util.ArrayList;
import java.util.List;

public class NotificationViewModel extends ViewModel {

    private final MutableLiveData<List<AppNotification>> notifications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final NotificationRepository repo = new NotificationRepository();

    public void init() {
        repo.init();
        repo.addListener(new NotificationRepository.NotificationListener() {
            @Override public void onNotificationsLoaded(List<AppNotification> list) {
                notifications.postValue(list);
            }
            @Override public void onNewNotification(AppNotification notif) {
                List<AppNotification> current = notifications.getValue();
                if (current == null) current = new ArrayList<>();
                current.add(0, notif);
                notifications.postValue(current);
            }
            @Override public void onError(String err) {
                error.postValue(err);
            }
        });
        repo.loadAll();
        repo.startListening();
    }

    public void markAsRead(String notifId) {
        repo.markAsRead(notifId);
        List<AppNotification> current = notifications.getValue();
        if (current != null) {
            for (AppNotification n : current) {
                if (n.id.equals(notifId)) n.read = true;
            }
            notifications.postValue(current);
        }
    }

    public void markAllAsRead() {
        repo.markAllAsRead();
        List<AppNotification> current = notifications.getValue();
        if (current != null) {
            for (AppNotification n : current) n.read = true;
            notifications.postValue(current);
        }
    }

    public List<AppNotification> getFiltered(String filter) {
        List<AppNotification> current = notifications.getValue();
        if (current == null) return new ArrayList<>();
        if ("unread".equals(filter)) {
            List<AppNotification> filtered = new ArrayList<>();
            for (AppNotification n : current) if (!n.read) filtered.add(n);
            return filtered;
        } else if ("read".equals(filter)) {
            List<AppNotification> filtered = new ArrayList<>();
            for (AppNotification n : current) if (n.read) filtered.add(n);
            return filtered;
        }
        return new ArrayList<>(current);
    }

    public LiveData<List<AppNotification>> getNotifications() { return notifications; }
    public LiveData<String> getError() { return error; }

    @Override protected void onCleared() {
        super.onCleared();
        repo.detach();
    }
}