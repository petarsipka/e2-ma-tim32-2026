package com.example.slagalica.ui.profile;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.UserRepository;
import com.example.slagalica.data.model.User;
import com.example.slagalica.data.model.UserStats;
import com.google.firebase.auth.FirebaseAuth;

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
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            userData.postValue(null);
            return;
        }

        repository.getUserByUid(uid, new UserRepository.UserFetchCallback() {
            @Override
            public void onSuccess(User user) {
                userData.postValue(user);
            }
            @Override
            public void onError(String error) {
                userData.postValue(null);
            }
        });

        // Stats placeholder for now
        userStats.postValue(new UserStats());
    }
}