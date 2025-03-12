package com.example.csci3130group1.ui.search_for_tutorials;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SearchForTutorialsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public SearchForTutorialsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Search for Tutorials fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}