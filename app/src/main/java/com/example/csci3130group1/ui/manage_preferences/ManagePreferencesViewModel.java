package com.example.csci3130group1.ui.manage_preferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ManagePreferencesViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ManagePreferencesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Manage Preference fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}