package com.example.myfootprint.ui.social;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SocialViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public SocialViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is social fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}