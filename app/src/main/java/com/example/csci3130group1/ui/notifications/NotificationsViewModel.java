package com.example.csci3130group1.ui.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NotificationsViewModel extends ViewModel {
    private final MutableLiveData<Boolean> scopeExpanded = new MutableLiveData<>(true);

    public LiveData<Boolean> getScopeExpanded() { return scopeExpanded; }
    public void setScopeExpanded(boolean expanded) { scopeExpanded.setValue(expanded); }
}

