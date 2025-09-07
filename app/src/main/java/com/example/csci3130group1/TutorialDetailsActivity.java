package com.example.csci3130group1;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
    private TextView tutorialDetailText;
    private Button backButton;
    private Button registerButton;
    private DatabaseReference tutorialRef;
    private String tutorialId;
    private String tutorialTitle;
    private String tutorialFee;

//    onCreate() method
    @Override
    protected void onCreate(Bundle savedInstanceState) {

//        Loads page
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial_details_activity);
        tutorialDetailText = findViewById(R.id.tutorial_detail_text);
        backButton = findViewById(R.id.back_button);
        registerButton = findViewById(R.id.register_button);

//        Gets tutorialID from intent
        tutorialId = getIntent().getStringExtra("tutorialId");
        if (tutorialId == null) {
            Toast.makeText(this, "Tutorial details not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

//        Loads tutorial details
        tutorialRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions").child(tutorialId);
        loadTutorialDetails();

//        Back button functionality
        backButton.setOnClickListener(v -> finish());

//        Registration button
        registerButton.setOnClickListener(v -> {
            try {

//                Debugging
                Log.d("TutorialDetails", "Creating intent to RegisterForTutorialActivity");
                Log.d("TutorialDetails", "tutorialId: " + tutorialId);
                Log.d("TutorialDetails", "tutorialTitle: " + tutorialTitle);
                Log.d("TutorialDetails", "tutorialFee: " + tutorialFee);

//                Navigates to registration and payment
                Intent registerIntent = new Intent(TutorialDetailsActivity.this, RegisterForTutorialActivity.class);
                registerIntent.putExtra("tutorialId", tutorialId);
                registerIntent.putExtra("tutorialTitle", tutorialTitle);
                registerIntent.putExtra("tutorialFee", tutorialFee);

//                Sets component from debugging errors
                registerIntent.setComponent(new ComponentName(getPackageName(),
                        "com.example.csci3130group1.RegisterForTutorialActivity"));

                startActivity(registerIntent);

//                Toast message
                Toast.makeText(TutorialDetailsActivity.this,
                        "Launching registration page...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {

//                Logging exceptions
                Log.e("TutorialDetails", "Error launching RegisterForTutorialActivity", e);
                Toast.makeText(TutorialDetailsActivity.this,
                        "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

//    loadTutorialDetails() method
    private void loadTutorialDetails() {
        tutorialRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

//                Extracts information
                if (dataSnapshot.exists()) {

//                    Gets NEW Firebase fields only
                    String tutorialName = dataSnapshot.child("tutorialName").getValue(String.class);
                    String topic = dataSnapshot.child("topic").getValue(String.class);
                    String description = dataSnapshot.child("description").getValue(String.class);
                    String fee = dataSnapshot.child("fee").getValue(String.class);
                    String date = dataSnapshot.child("date").getValue(String.class);
                    String startTime = dataSnapshot.child("startTime").getValue(String.class);
                    String endTime = dataSnapshot.child("endTime").getValue(String.class);
                    String address = dataSnapshot.child("address").getValue(String.class);
                    String tutorName = dataSnapshot.child("tutorName").getValue(String.class);
                    String tutorId = dataSnapshot.child("tutorId").getValue(String.class);
                    String placeId = dataSnapshot.child("placeId").getValue(String.class);
                    Double latitude = dataSnapshot.child("latitude").getValue(Double.class);
                    Double longitude = dataSnapshot.child("longitude").getValue(Double.class);

//                    Store title and fee for registration
                    tutorialTitle = (tutorialName != null) ? tutorialName : topic;
                    tutorialFee = fee;
                    StringBuilder details = new StringBuilder();

//                    Tutorial Name
                    details.append("Tutorial: ");
                    if (tutorialName != null) {
                        details.append(tutorialName);
                    } else if (topic != null) {
                        details.append(topic);
                    } else {
                        details.append("Unknown Tutorial");
                    }
                    details.append("\n\n");

//                    Subject (if different from tutorial name)
                    if (topic != null && !topic.equals(tutorialName)) {
                        details.append("Subject: ").append(topic).append("\n\n");
                    }

//                    Tutor
                    details.append("Tutor: ");
                    if (tutorName != null) {
                        details.append(tutorName);
                    } else {
                        details.append("N/A");
                    }
                    details.append("\n\n");

//                    Date
                    details.append("Date: ");
                    if (date != null) {
                        details.append(date);
                    } else {
                        details.append("N/A");
                    }
                    details.append("\n\n");

//                    Time
                    details.append("Time: ");
                    if (startTime != null && endTime != null) {
                        details.append(startTime).append(" - ").append(endTime);
                    } else {
                        details.append("N/A");
                    }
                    details.append("\n\n");

//                    Location
                    details.append("Location: ");
                    if (address != null) {
                        details.append(address);
                    } else {
                        details.append("N/A");
                    }
                    details.append("\n\n");

//                    Fee
                    details.append("Fee: $");
                    if (fee != null) {
                        details.append(fee);
                    } else {
                        details.append("0");
                    }
                    details.append("\n\n");

//                    Description
                    if (description != null && !description.isEmpty()) {
                        details.append("Description:\n").append(description);
                    }

                    tutorialDetailText.setText(details.toString());
                    registerButton.setEnabled(fee != null && !fee.isEmpty());

                } else {
                    Toast.makeText(TutorialDetailsActivity.this,
                            "Tutorial details not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

//            onCancelled() method
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