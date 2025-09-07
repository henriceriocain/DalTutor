package com.example.csci3130group1;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

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
    private boolean isAlreadyRegistered;
    
    // Tutor info card components
    private CardView tutorInfoCard;
    private TextView tutorName;
    private TextView tutorDegree;
    private TextView tutorialCount;
    private TextView tutorRating;
    private LinearLayout ratingContainer;
    private String currentTutorId;

//    onCreate() method
    @Override
    protected void onCreate(Bundle savedInstanceState) {

//        Loads page
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial_details_activity);
        tutorialDetailText = findViewById(R.id.tutorial_detail_text);
        backButton = findViewById(R.id.back_button);
        registerButton = findViewById(R.id.register_button);
        
        // Initialize tutor info card components
        tutorInfoCard = findViewById(R.id.tutorInfoCard);
        tutorName = findViewById(R.id.tutorName);
        tutorDegree = findViewById(R.id.tutorDegree);
        tutorialCount = findViewById(R.id.tutorialCount);
        tutorRating = findViewById(R.id.tutorRating);
        ratingContainer = findViewById(R.id.ratingContainer);
        
        // Set click listener for tutor card
        if (tutorInfoCard != null) {
            tutorInfoCard.setOnClickListener(v -> {
                if (currentTutorId != null) {
                    // TODO: Navigate to TutorProfileActivity
                    Toast.makeText(this, "Navigate to tutor profile: " + currentTutorId, Toast.LENGTH_SHORT).show();
                }
            });
        }

//        Gets tutorialID from intent
        tutorialId = getIntent().getStringExtra("tutorialId");
        isAlreadyRegistered = getIntent().getBooleanExtra("isAlreadyRegistered", false);
        if (tutorialId == null) {
            Toast.makeText(this, "Tutorial details not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

//        Loads tutorial details
        tutorialRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions").child(tutorialId);
        loadTutorialDetails();

//        Configure register button based on registration status
        if (isAlreadyRegistered) {
            registerButton.setText("Already Registered");
            registerButton.setEnabled(false);
            registerButton.setAlpha(0.6f);
            // Change back button text when coming from profile
            backButton.setText("Back to Profile");
        }

//        Back button functionality
        backButton.setOnClickListener(v -> finish());

//        Registration button
        if (!isAlreadyRegistered) {
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
                    
                    // Store tutor ID for navigation
                    currentTutorId = tutorId;

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
                    
                    // Populate tutor info card
                    loadTutorInfo(tutorId, tutorName);

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
    
    private void loadTutorInfo(String tutorId, String tutorNameFromTutorial) {
        if (tutorId == null || tutorId.isEmpty()) {
            // Fallback to tutorial tutor name if no tutorId
            if (tutorName != null && tutorNameFromTutorial != null) {
                tutorName.setText(tutorNameFromTutorial);
            }
            return;
        }
        
        DatabaseReference tutorRef = FirebaseDatabase.getInstance().getReference("users").child(tutorId);
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews").child(tutorId);
        DatabaseReference tutorialSessionsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        
        // Load tutor basic info
        tutorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && tutorName != null) {
                    String name = snapshot.child("name").getValue(String.class);
                    String degree = snapshot.child("degree").getValue(String.class);
                    
                    tutorName.setText(name != null ? name : tutorNameFromTutorial);
                    
                    if (degree != null && !degree.trim().isEmpty() && tutorDegree != null) {
                        tutorDegree.setText(degree);
                        tutorDegree.setVisibility(android.view.View.VISIBLE);
                    }
                } else if (tutorName != null) {
                    tutorName.setText(tutorNameFromTutorial != null ? tutorNameFromTutorial : "Unknown Tutor");
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (tutorName != null) {
                    tutorName.setText(tutorNameFromTutorial != null ? tutorNameFromTutorial : "Unknown Tutor");
                }
            }
        });
        
        // Load tutor rating
        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (tutorRating != null && ratingContainer != null) {
                    float total = 0;
                    int count = 0;
                    
                    for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                        Double ratingNumber = reviewSnap.child("rating").getValue(Double.class);
                        if (ratingNumber != null) {
                            total += ratingNumber.floatValue();
                            count++;
                        }
                    }
                    
                    if (count > 0) {
                        float average = total / count;
                        tutorRating.setText(String.format("%.1f (%d)", average, count));
                        ratingContainer.setVisibility(android.view.View.VISIBLE);
                    } else {
                        tutorRating.setText("No reviews yet");
                        ratingContainer.setVisibility(android.view.View.VISIBLE);
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        
        // Count total tutorials by this tutor
        tutorialSessionsRef.orderByChild("tutorId").equalTo(tutorId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (tutorialCount != null) {
                        long count = snapshot.getChildrenCount();
                        String countText = count == 1 ? "1 tutorial" : count + " tutorials";
                        tutorialCount.setText(countText);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (tutorialCount != null) {
                        tutorialCount.setText("Tutorials available");
                    }
                }
            });
    }
}