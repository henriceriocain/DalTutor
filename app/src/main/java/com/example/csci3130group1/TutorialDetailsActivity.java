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
    
    // New structured UI components
    private TextView tutorialName;
    private TextView tutorialSubject;
    private TextView tutorialDate;
    private TextView tutorialTime;
    private TextView tutorialLocation;
    private TextView tutorialFeeView;
    private TextView tutorialDescription;
    private LinearLayout descriptionSection;
    private Button backButton;
    private Button registerButton;
    private DatabaseReference tutorialRef;
    private String tutorialId;
    private String tutorialTitle;
    private String tutorialFeeString;
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
        
        // Initialize new structured UI components
        tutorialName = findViewById(R.id.tutorialName);
        tutorialSubject = findViewById(R.id.tutorialSubject);
        tutorialDate = findViewById(R.id.tutorialDate);
        tutorialTime = findViewById(R.id.tutorialTime);
        tutorialLocation = findViewById(R.id.tutorialLocation);
        tutorialFeeView = findViewById(R.id.tutorialFee);
        tutorialDescription = findViewById(R.id.tutorialDescription);
        descriptionSection = findViewById(R.id.descriptionSection);
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
                    Intent intent = new Intent(this, TutorProfileActivity.class);
                    intent.putExtra("tutorId", currentTutorId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Tutor information not available", Toast.LENGTH_SHORT).show();
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
                Log.d("TutorialDetails", "tutorialFee: " + tutorialFeeString);

//                Navigates to registration and payment
                Intent registerIntent = new Intent(TutorialDetailsActivity.this, RegisterForTutorialActivity.class);
                registerIntent.putExtra("tutorialId", tutorialId);
                registerIntent.putExtra("tutorialTitle", tutorialTitle);
                registerIntent.putExtra("tutorialFee", tutorialFeeString);

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
                    tutorialFeeString = fee;
                    
                    // Populate structured UI components
                    populateModernTutorialDetails(tutorialName, topic, date, startTime, endTime, address, fee, description);
                    
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
    
    private void populateModernTutorialDetails(String tutorialName, String topic, String date, 
                                              String startTime, String endTime, String address, 
                                              String fee, String description) {
        // Tutorial Name
        if (this.tutorialName != null) {
            if (tutorialName != null && !tutorialName.trim().isEmpty()) {
                this.tutorialName.setText(tutorialName);
            } else if (topic != null && !topic.trim().isEmpty()) {
                this.tutorialName.setText(topic);
            } else {
                this.tutorialName.setText("Unknown Tutorial");
            }
        }
        
        // Subject (if different from tutorial name)
        if (tutorialSubject != null) {
            if (topic != null && !topic.trim().isEmpty() && !topic.equals(tutorialName)) {
                tutorialSubject.setText(topic);
                tutorialSubject.setVisibility(android.view.View.VISIBLE);
            } else {
                tutorialSubject.setVisibility(android.view.View.GONE);
            }
        }
        
        // Date
        if (tutorialDate != null) {
            tutorialDate.setText(date != null ? date : "TBD");
        }
        
        // Time
        if (tutorialTime != null) {
            if (startTime != null && endTime != null) {
                tutorialTime.setText(startTime + " - " + endTime);
            } else {
                tutorialTime.setText("TBD");
            }
        }
        
        // Location
        if (tutorialLocation != null) {
            tutorialLocation.setText(address != null ? address : "Location TBD");
        }
        
        // Fee
        if (this.tutorialFeeView != null) {
            if (fee != null && !fee.trim().isEmpty()) {
                this.tutorialFeeView.setText("$" + fee);
            } else {
                this.tutorialFeeView.setText("Free");
            }
        }
        
        // Description
        if (tutorialDescription != null && descriptionSection != null) {
            if (description != null && !description.trim().isEmpty()) {
                tutorialDescription.setText(description);
                descriptionSection.setVisibility(android.view.View.VISIBLE);
            } else {
                descriptionSection.setVisibility(android.view.View.GONE);
            }
        }
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