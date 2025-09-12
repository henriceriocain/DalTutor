package com.example.csci3130group1.ui.search_for_tutorials;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SearchForTutorialsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<Boolean> filtersExpanded = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> searchExpanded = new MutableLiveData<>(true);

    public SearchForTutorialsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Search for Tutorials fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<Boolean> getFiltersExpanded() { return filtersExpanded; }
    public void setFiltersExpanded(boolean expanded) { filtersExpanded.setValue(expanded); }

    public LiveData<Boolean> getSearchExpanded() { return searchExpanded; }
    public void setSearchExpanded(boolean expanded) { searchExpanded.setValue(expanded); }
}
