package com.example.csci3130group1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.csci3130group1.ui.search_for_tutorials.Tutorial;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TutorProfileActivity extends AppCompatActivity {

    private String tutorId;
    private String currentUserId;
    
    // UI Components
    private ImageView tutorProfilePicture;
    private TextView tutorName;
    private TextView tutorDegree;
    private TextView tutorialCount;
    private TextView tutorRating;
    private LinearLayout ratingContainer;
    private TextView tutorDescription;
    private LinearLayout descriptionSection;
    private TextView tutorContact;
    private LinearLayout contactContainer;
    private Button addReviewButton;
    private LinearLayout reviewsList;
    private TextView noReviewsText;
    private LinearLayout tutorialsList;
    private TextView noTutorialsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_profile);

        // Get tutor ID from intent
        tutorId = getIntent().getStringExtra("tutorId");
        if (tutorId == null || tutorId.isEmpty()) {
            Toast.makeText(this, "Error loading tutor profile", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get current user ID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        initializeViews();
        setupClickListeners();
        loadTutorProfile();
        loadTutorReviews();
        loadTutorTutorials();
    }

    private void initializeViews() {
        tutorProfilePicture = findViewById(R.id.tutorProfilePicture);
        tutorName = findViewById(R.id.tutorName);
        tutorDegree = findViewById(R.id.tutorDegree);
        tutorialCount = findViewById(R.id.tutorialCount);
        tutorRating = findViewById(R.id.tutorRating);
        ratingContainer = findViewById(R.id.ratingContainer);
        tutorDescription = findViewById(R.id.tutorDescription);
        descriptionSection = findViewById(R.id.descriptionSection);
        tutorContact = findViewById(R.id.tutorContact);
        contactContainer = findViewById(R.id.contactContainer);
        addReviewButton = findViewById(R.id.addReviewButton);
        reviewsList = findViewById(R.id.reviewsList);
        noReviewsText = findViewById(R.id.noReviewsText);
        tutorialsList = findViewById(R.id.tutorialsList);
        noTutorialsText = findViewById(R.id.noTutorialsText);
    }

    private void setupClickListeners() {
        addReviewButton.setOnClickListener(v -> {
            if (currentUserId != null && !currentUserId.equals(tutorId)) {
                Intent reviewIntent = new Intent(this, ReviewActivity.class);
                reviewIntent.putExtra("reviewedUserId", tutorId);
                startActivity(reviewIntent);
            } else if (currentUserId != null && currentUserId.equals(tutorId)) {
                Toast.makeText(this, "You cannot review yourself", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please log in to add a review", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTutorProfile() {
        DatabaseReference tutorRef = FirebaseDatabase.getInstance().getReference("users").child(tutorId);
        
        tutorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String degree = snapshot.child("degree").getValue(String.class);
                    String tutorDescriptionText = snapshot.child("tutorDescription").getValue(String.class);
                    String contactNumber = snapshot.child("contact").getValue(String.class);
                    String profilePictureUrl = snapshot.child("profilePictureUrl").getValue(String.class);

                    // Set name
                    if (name != null && !name.trim().isEmpty()) {
                        tutorName.setText(name);
                    }
                    
                    // Load profile picture
                    if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                        Glide.with(TutorProfileActivity.this)
                            .load(profilePictureUrl)
                            .circleCrop()
                            .placeholder(R.drawable.circle_background)
                            .error(R.drawable.circle_background)
                            .into(tutorProfilePicture);
                    }

                    // Set degree
                    if (degree != null && !degree.trim().isEmpty()) {
                        tutorDegree.setText(degree);
                        tutorDegree.setVisibility(View.VISIBLE);
                    }

                    // Set description
                    if (tutorDescriptionText != null && !tutorDescriptionText.trim().isEmpty()) {
                        tutorDescription.setText(tutorDescriptionText);
                        descriptionSection.setVisibility(View.VISIBLE);
                    }

                    // Set contact number
                    if (contactNumber != null && !contactNumber.trim().isEmpty()) {
                        tutorContact.setText(contactNumber);
                        contactContainer.setVisibility(View.VISIBLE);
                    }

                    // Load rating and tutorial count
                    loadTutorStats();
                } else {
                    Toast.makeText(TutorProfileActivity.this, "Tutor not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TutorProfileActivity.this, "Error loading tutor profile", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadTutorStats() {
        // Load rating
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews").child(tutorId);
        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
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
                    tutorRating.setText(String.format(Locale.getDefault(), "%.1f (%d)", average, count));
                    ratingContainer.setVisibility(View.VISIBLE);
                } else {
                    tutorRating.setText("No reviews yet");
                    ratingContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tutorRating.setText("No reviews yet");
                ratingContainer.setVisibility(View.VISIBLE);
            }
        });

        // Load tutorial count
        DatabaseReference tutorialSessionsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        tutorialSessionsRef.orderByChild("tutorId").equalTo(tutorId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long count = snapshot.getChildrenCount();
                    String countText = count == 1 ? "1 tutorial" : count + " tutorials";
                    tutorialCount.setText(countText);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    tutorialCount.setText("Tutorials available");
                }
            });
    }

    private void loadTutorReviews() {
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews").child(tutorId);
        
        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewsList.removeAllViews();
                
                if (snapshot.getChildrenCount() == 0) {
                    noReviewsText.setVisibility(View.VISIBLE);
                    return;
                }

                noReviewsText.setVisibility(View.GONE);
                
                for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                    String reviewerName = reviewSnap.child("reviewerName").getValue(String.class);
                    String reviewText = reviewSnap.child("reviewText").getValue(String.class);
                    Double rating = reviewSnap.child("rating").getValue(Double.class);
                    String timestamp = reviewSnap.child("timestamp").getValue(String.class);

                    addReviewToList(reviewerName, reviewText, rating != null ? rating.floatValue() : 0, timestamp);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noReviewsText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addReviewToList(String reviewerName, String reviewText, float rating, String timestamp) {
        View reviewView = LayoutInflater.from(this).inflate(R.layout.review_item, reviewsList, false);
        
        TextView reviewerNameView = reviewView.findViewById(R.id.reviewerName);
        TextView reviewTextView = reviewView.findViewById(R.id.reviewText);
        TextView reviewRatingView = reviewView.findViewById(R.id.reviewRating);
        TextView reviewTimestampView = reviewView.findViewById(R.id.reviewTimestamp);

        reviewerNameView.setText(reviewerName != null ? reviewerName : "Anonymous");
        reviewTextView.setText(reviewText != null ? reviewText : "");
        reviewRatingView.setText(String.format(Locale.getDefault(), "%.1f ★", rating));
        
        if (timestamp != null) {
            reviewTimestampView.setText(timestamp);
            reviewTimestampView.setVisibility(View.VISIBLE);
        }

        reviewsList.addView(reviewView);
    }

    private void loadTutorTutorials() {
        DatabaseReference tutorialSessionsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        
        tutorialSessionsRef.orderByChild("tutorId").equalTo(tutorId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    tutorialsList.removeAllViews();
                    
                    if (snapshot.getChildrenCount() == 0) {
                        noTutorialsText.setVisibility(View.VISIBLE);
                        return;
                    }

                    noTutorialsText.setVisibility(View.GONE);
                    List<Tutorial> tutorials = new ArrayList<>();

                    for (DataSnapshot tutorialSnap : snapshot.getChildren()) {
                        String tutorialId = tutorialSnap.getKey();
                        String tutorialName = tutorialSnap.child("tutorialName").getValue(String.class);
                        String topic = tutorialSnap.child("topic").getValue(String.class);
                        String fee = tutorialSnap.child("fee").getValue(String.class);
                        String date = tutorialSnap.child("date").getValue(String.class);
                        String startTime = tutorialSnap.child("startTime").getValue(String.class);
                        String endTime = tutorialSnap.child("endTime").getValue(String.class);
                        String address = tutorialSnap.child("address").getValue(String.class);
                        String tutorName = tutorialSnap.child("tutorName").getValue(String.class);

                        Tutorial tutorial = new Tutorial(
                            tutorialName != null ? tutorialName : "Unknown Tutorial",
                            topic != null ? topic : "General",
                            fee != null ? fee : "Free",
                            date != null ? date : "TBD",
                            startTime != null ? startTime : "TBD",
                            endTime != null ? endTime : "TBD",
                            "No description available",
                            address != null ? address : "Location TBD",
                            0.0, 0.0, "",
                            tutorName != null ? tutorName : "Unknown Tutor",
                            "",  // tutorId
                            ""   // tutorDegree
                        );
                        tutorial.setTutorialId(tutorialId);
                        tutorials.add(tutorial);
                    }

                    addTutorialsToList(tutorials);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    noTutorialsText.setVisibility(View.VISIBLE);
                }
            });
    }

    private void addTutorialsToList(List<Tutorial> tutorials) {
        for (Tutorial tutorial : tutorials) {
            View tutorialCardView = getLayoutInflater().inflate(R.layout.tutorial_card_item, tutorialsList, false);
            
            TextView tutorialName = tutorialCardView.findViewById(R.id.tutorialCardName);
            TextView tutorialFee = tutorialCardView.findViewById(R.id.tutorialCardFee);
            TextView tutorialTutor = tutorialCardView.findViewById(R.id.tutorialCardTutor);
            TextView tutorialDateTime = tutorialCardView.findViewById(R.id.tutorialCardDateTime);
            TextView tutorialLocation = tutorialCardView.findViewById(R.id.tutorialCardLocation);

            tutorialName.setText(tutorial.getTutorialName());
            tutorialFee.setText(tutorial.getFee() != null ? "$" + tutorial.getFee() : "Free");
            tutorialTutor.setText(tutorial.getTutorName());
            
            String dateTime = String.format(Locale.getDefault(), "%s at %s - %s",
                    tutorial.getDate() != null ? tutorial.getDate() : "No date",
                    tutorial.getStartTime() != null ? tutorial.getStartTime() : "TBD",
                    tutorial.getEndTime() != null ? tutorial.getEndTime() : "TBD");
            tutorialDateTime.setText(dateTime);
            
            tutorialLocation.setText(tutorial.getAddress() != null ? tutorial.getAddress() : "Location TBD");

            // Set click listener to navigate to tutorial details
            tutorialCardView.setOnClickListener(v -> {
                Intent intent = new Intent(TutorProfileActivity.this, TutorialDetailsActivity.class);
                intent.putExtra("tutorialId", tutorial.getTutorialId());
                intent.putExtra("tutorialName", tutorial.getTutorialName());
                intent.putExtra("tutorName", tutorial.getTutorName());
                intent.putExtra("fee", tutorial.getFee());
                intent.putExtra("date", tutorial.getDate());
                intent.putExtra("startTime", tutorial.getStartTime());
                intent.putExtra("endTime", tutorial.getEndTime());
                intent.putExtra("address", tutorial.getAddress());
                // Note: Don't pass isAlreadyRegistered as true since this is just browsing tutor's tutorials
                startActivity(intent);
            });

            tutorialsList.addView(tutorialCardView);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh reviews when returning from ReviewActivity
        loadTutorReviews();
    }
}