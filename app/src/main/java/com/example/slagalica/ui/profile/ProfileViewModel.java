package com.example.slagalica.ui.profile;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.UserRepository;
import com.example.slagalica.data.model.User;
import com.example.slagalica.data.model.UserStats;

public class ProfileViewModel extends ViewModel {

    private final MutableLiveData<User> userData = new MutableLiveData<>();
    private final MutableLiveData<UserStats> userStats = new MutableLiveData<>();
    private final UserRepository repository = new UserRepository();

    public MutableLiveData<User> getUserData() {
        return userData;
    }

    public MutableLiveData<UserStats> getUserStats() {
        return userStats;
    }

    public void loadUser() {
        userData.setValue(repository.getCurrentUser());
        userStats.setValue(repository.getUserStats());
    }
}
