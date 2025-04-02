package com.example.csci3130group1.ui.manage_preferences;

public interface AccessTokenListener {
    void onAccessTokenReceived(String token);
    void onAccessTokenError(Exception exception);
}

