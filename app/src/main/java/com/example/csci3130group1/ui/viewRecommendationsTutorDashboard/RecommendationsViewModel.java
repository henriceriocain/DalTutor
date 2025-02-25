package com.example.csci3130group1.ui.viewRecommendationsTutorDashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RecommendationsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public RecommendationsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is viewRecommendationsTutorDashboard fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}