package com.example.csci3130group1;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Enable Firebase persistence (optional but useful for offline support)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}

