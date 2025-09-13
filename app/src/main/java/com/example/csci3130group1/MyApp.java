package com.example.csci3130group1;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;
import java.util.TimeZone;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Enable Firebase persistence (optional but useful for offline support)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Enforce Halifax timezone across the app by default
        TimeZone.setDefault(TimeZone.getTimeZone("America/Halifax"));
    }
}
