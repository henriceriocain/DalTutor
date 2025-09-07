package com.example.csci3130group1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.csci3130group1.ui.search_for_tutorials.Tutorial;
import com.example.csci3130group1.ui.search_for_tutorials.TutorialAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LocationTutorialsActivity extends AppCompatActivity {

    private TextView locationNameTextView;
    private TextView tutorialCountTextView;
    private ListView tutorialsListView;
    private TextView emptyStateTextView;
    private Button backButton;
    
    private DatabaseReference tutorialsRef;
    private TutorialAdapter adapter;
    private List<Tutorial> locationTutorials;
    
    private String locationName;
    private String placeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_tutorials);

        // Initialize views
        locationNameTextView = findViewById(R.id.location_name);
        tutorialCountTextView = findViewById(R.id.tutorial_count);
        tutorialsListView = findViewById(R.id.tutorials_list);
        emptyStateTextView = findViewById(R.id.empty_state_text);
        backButton = findViewById(R.id.back_button);

        // Get data from intent
        locationName = getIntent().getStringExtra("locationName");
        placeId = getIntent().getStringExtra("placeId");

        if (locationName == null || placeId == null) {
            Toast.makeText(this, "Location information not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set location name
        locationNameTextView.setText(locationName);

        // Initialize tutorial list and adapter
        locationTutorials = new ArrayList<>();
        adapter = new TutorialAdapter(this, locationTutorials);
        tutorialsListView.setAdapter(adapter);

        // Initialize Firebase reference
        tutorialsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");

        // Load tutorials for this location
        loadTutorialsForLocation();

        // Back button functionality
        backButton.setOnClickListener(v -> finish());
    }

    private void loadTutorialsForLocation() {
        tutorialsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                locationTutorials.clear();

                for (DataSnapshot tutorialSnapshot : dataSnapshot.getChildren()) {
                    String tutorialPlaceId = tutorialSnapshot.child("placeId").getValue(String.class);

                    // Only include tutorials for this specific location
                    if (placeId.equals(tutorialPlaceId)) {
                        try {
                            // Get the tutorial ID from Firebase key
                            String firebaseTutorialId = tutorialSnapshot.getKey();
                            
                            // Extract tutorial data
                            String tutorialName = tutorialSnapshot.child("tutorialName").getValue(String.class);
                            String topic = tutorialSnapshot.child("topic").getValue(String.class);
                            String fee = tutorialSnapshot.child("fee").getValue(String.class);
                            String description = tutorialSnapshot.child("description").getValue(String.class);
                            String address = tutorialSnapshot.child("address").getValue(String.class);
                            Double latitude = tutorialSnapshot.child("latitude").getValue(Double.class);
                            Double longitude = tutorialSnapshot.child("longitude").getValue(Double.class);
                            String tutorName = tutorialSnapshot.child("tutorName").getValue(String.class);
                            String tutorDegree = tutorialSnapshot.child("tutorDegree").getValue(String.class);
                            String tutorId = tutorialSnapshot.child("tutorId").getValue(String.class);
                            String date = tutorialSnapshot.child("date").getValue(String.class);
                            String startTime = tutorialSnapshot.child("startTime").getValue(String.class);
                            String endTime = tutorialSnapshot.child("endTime").getValue(String.class);

                            // Create tutorial object using the modern constructor
                            Tutorial tutorial = new Tutorial(
                                    tutorialName != null ? tutorialName : topic,
                                    topic,
                                    fee != null ? fee : "0",
                                    date,
                                    startTime,
                                    endTime,
                                    description,
                                    address,
                                    latitude != null ? latitude : 0.0,
                                    longitude != null ? longitude : 0.0,
                                    placeId,
                                    tutorName,
                                    tutorId,
                                    tutorDegree
                            );

                            // Set the tutorial ID from Firebase
                            tutorial.setTutorialId(firebaseTutorialId);

                            locationTutorials.add(tutorial);
                        } catch (Exception e) {
                            // Skip this tutorial if there's an error parsing the data
                            continue;
                        }
                    }
                }

                // Update UI
                updateTutorialCount();
                adapter.updateTutorials(locationTutorials);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(LocationTutorialsActivity.this,
                        "Failed to load tutorials: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTutorialCount() {
        int count = locationTutorials.size();
        String countText = count == 1 ? "1 tutorial available" : count + " tutorials available";
        tutorialCountTextView.setText(countText);
        
        // Show/hide empty state
        if (count == 0) {
            emptyStateTextView.setVisibility(android.view.View.VISIBLE);
            tutorialsListView.setVisibility(android.view.View.GONE);
        } else {
            emptyStateTextView.setVisibility(android.view.View.GONE);
            tutorialsListView.setVisibility(android.view.View.VISIBLE);
        }
    }
}