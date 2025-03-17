package com.example.csci3130group1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// TutorialDetailsActivity class
public class TutorialDetailsActivity extends AppCompatActivity {

//    Attributes
    private TextView titleTextView, locationTextView, feeTextView, dateTextView, timeTextView, durationTextView;
    private Button backButton;
    private DatabaseReference tutorialRef;
    private String tutorialId;

//    onCreate method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        Loads page
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial_details);
        titleTextView = findViewById(R.id.tutorial_title);
        locationTextView = findViewById(R.id.tutorial_location);
        feeTextView = findViewById(R.id.tutorial_fee);
        dateTextView = findViewById(R.id.tutorial_date);
        timeTextView = findViewById(R.id.tutorial_time);
        durationTextView = findViewById(R.id.tutorial_duration);
        backButton = findViewById(R.id.back_button);
//        Gets tutorialID from intent
        tutorialId = getIntent().getStringExtra("tutorialId");
        if (tutorialId == null) {
            Toast.makeText(this, "Tutorial details not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
//        Loads tutorials details
        tutorialRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions").child(tutorialId);
        loadTutorialDetails();
//        Back button functionality
        backButton.setOnClickListener(v -> finish());
    }

//    loadTutorialDetails method to load tutorial details
    private void loadTutorialDetails() {
        tutorialRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

//                If data exists, extract info...
                if (dataSnapshot.exists()) {
                    String title = dataSnapshot.child("title").getValue(String.class);
                    String location = dataSnapshot.child("location").getValue(String.class);
                    String fee = dataSnapshot.child("fee").getValue(String.class);
                    String duration = dataSnapshot.child("duration").getValue(String.class);
//                    Other variables
                    String date = "N/A";
                    String time = "N/A";
//                    Formatting
                    titleTextView.setText(title);
                    locationTextView.setText("Location: " + (location != null ? location : "N/A"));
                    feeTextView.setText("Fee: $" + (fee != null ? fee : "N/A"));
                    dateTextView.setText("Date: " + date);
                    timeTextView.setText("Time: " + time);
                    durationTextView.setText("Duration: " + (duration != null ? duration + " minutes" : "N/A"));
                } else {
                    Toast.makeText(TutorialDetailsActivity.this,
                            "Tutorial details not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

//            onCancelled method in the event of a failure
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TutorialDetailsActivity.this,
                        "Failed to load tutorial: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}