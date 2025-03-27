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

//                    Gets all fields of data
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String topic = dataSnapshot.child("topic").getValue(String.class);
                    String city = dataSnapshot.child("city").getValue(String.class);
                    String province = dataSnapshot.child("province").getValue(String.class);
                    String country = dataSnapshot.child("country").getValue(String.class);
                    String degree = dataSnapshot.child("degree").getValue(String.class);
                    String description = dataSnapshot.child("description").getValue(String.class);
                    String duration = dataSnapshot.child("duration").getValue(String.class);
                    String fee = dataSnapshot.child("fee").getValue(String.class);

//                    Old data fields from US10
                    String title = dataSnapshot.child("title").getValue(String.class);
                    String location = dataSnapshot.child("location").getValue(String.class);

//                    Stores title and fee
                    tutorialTitle = (topic != null) ? topic : title;
                    tutorialFee = fee;
                    StringBuilder details = new StringBuilder();

//                    Tutorial title
                    if (topic != null) {
                        details.append("Tutorial Topic: ").append(topic).append("\n\n");
                    } else if (title != null) {
                        details.append("Tutorial Title: ").append(title).append("\n\n");
                    }

//                    Tutor name
                    if (name != null) {
                        details.append("Tutor: ").append(name).append("\n\n");
                    }

//                    Location
                    details.append("Location: ");
                    if (city != null) {
                        details.append(city);
                        if (province != null) details.append(", ").append(province);
                        if (country != null) details.append(", ").append(country);
                    } else if (location != null) {
                        details.append(location);
                    } else {
                        details.append("N/A");
                    }
                    details.append("\n\n");

//                    Degree
                    if (degree != null) {
                        details.append("Degree: ").append(degree).append("\n\n");
                    }

//                    Fee
                    details.append("Fee: $");
                    if (fee != null) {
                        details.append(fee);
                    } else {
                        details.append("N/A");
                    }
                    details.append("\n\n");

//                    Duration
                    details.append("Duration: ");
                    if (duration != null) {
                        details.append(duration).append(" minutes");
                    } else {
                        details.append("N/A");
                    }
                    details.append("\n\n");

//                    Description
                    if (description != null) {
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