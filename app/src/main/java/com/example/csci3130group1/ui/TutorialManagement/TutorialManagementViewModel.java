package com.example.csci3130group1.ui.TutorialManagement;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TutorialManagementViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public TutorialManagementViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is TutorialManagement fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}