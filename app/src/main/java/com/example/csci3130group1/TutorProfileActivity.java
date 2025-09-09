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
import java.util.HashSet;
import java.util.Set;

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
    private TextView tutorReviewsHeader;
    private LinearLayout upcomingTutorialsList;
    private LinearLayout upcomingTutorialsCard;
    private TextView tutorialStats;
    private Button viewAllTutorialsButton;
    private int reviewsLoadVersion = 0;

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

        boolean readOnly = getIntent().getBooleanExtra("readOnly", false);

        // Get current user ID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        initializeViews();
        setupClickListeners();
        loadTutorProfile();
        loadTutorReviews();
        loadTutorTutorialData();

        // Hide actions in read-only mode (tutor viewing own profile from dashboard)
        if (readOnly && addReviewButton != null) {
            addReviewButton.setVisibility(View.GONE);
        }
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
        tutorReviewsHeader = findViewById(R.id.tutorReviewsHeader);
        upcomingTutorialsList = findViewById(R.id.upcomingTutorialsList);
        upcomingTutorialsCard = findViewById(R.id.upcoming_tutorials_card);
        tutorialStats = findViewById(R.id.tutorialStats);
        viewAllTutorialsButton = findViewById(R.id.view_all_tutorials_button);
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

        viewAllTutorialsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, TutorialHistoryActivity.class);
            intent.putExtra("tutorId", tutorId);
            intent.putExtra("isTutorView", true);
            startActivity(intent);
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
                    String description = snapshot.child("description").getValue(String.class);
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
                    if (description != null && !description.trim().isEmpty()) {
                        tutorDescription.setText(description);
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
        final int loadVersion = ++reviewsLoadVersion;
        final Set<String> addedIds = new HashSet<>();
        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (loadVersion != reviewsLoadVersion) return;
                reviewsList.removeAllViews();
                
                if (snapshot.getChildrenCount() == 0) {
                    if (tutorReviewsHeader != null) tutorReviewsHeader.setText(getString(R.string.reviews_count, 0));
                    noReviewsText.setVisibility(View.VISIBLE);
                    return;
                }

                noReviewsText.setVisibility(View.GONE);
                if (tutorReviewsHeader != null) tutorReviewsHeader.setText(getString(R.string.reviews_count, (int) snapshot.getChildrenCount()));
                
                for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                    final String reviewId = reviewSnap.getKey();
                    // Flexible read: support legacy keys
                    String reviewerName = reviewSnap.child("reviewerName").getValue(String.class);
                    String reviewText = reviewSnap.child("reviewText").getValue(String.class);
                    if (reviewText == null || reviewText.isEmpty()) {
                        reviewText = reviewSnap.child("text").getValue(String.class);
                    }
                    Double rating = reviewSnap.child("rating").getValue(Double.class);
                    // Handle timestamp as Long (or String fallback)
                    String timestampText = null;
                    Object tsObj = reviewSnap.child("timestamp").getValue();
                    if (tsObj != null) {
                        try {
                            long ts;
                            if (tsObj instanceof Number) {
                                ts = ((Number) tsObj).longValue();
                            } else {
                                ts = Long.parseLong(String.valueOf(tsObj));
                            }
                            java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT);
                            timestampText = df.format(new java.util.Date(ts));
                        } catch (Exception ignored) {}
                    }

                    if (reviewerName != null && !reviewerName.isEmpty()) {
                        if (loadVersion == reviewsLoadVersion && addedIds.add(reviewId)) {
                            addReviewToList(reviewerName, reviewText, rating != null ? rating.floatValue() : 0, timestampText);
                        }
                    } else {
                        String fromUserId = reviewSnap.child("fromUser").getValue(String.class);
                        if (fromUserId != null && !fromUserId.isEmpty()) {
                            final String reviewTextFinal = reviewText;
                            final float ratingFinal = rating != null ? rating.floatValue() : 0f;
                            final String timestampTextFinal = timestampText;
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(fromUserId);
                            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    if (loadVersion != reviewsLoadVersion) return;
                                    String name = userSnap.child("name").getValue(String.class);
                                    if (name == null || name.isEmpty()) {
                                        String email = userSnap.child("email").getValue(String.class);
                                        name = email != null ? email : "Anonymous";
                                    }
                                    if (addedIds.add(reviewId)) {
                                        addReviewToList(name, reviewTextFinal, ratingFinal, timestampTextFinal);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    if (loadVersion != reviewsLoadVersion) return;
                                    if (addedIds.add(reviewId)) {
                                        addReviewToList("Anonymous", reviewTextFinal, ratingFinal, timestampTextFinal);
                                    }
                                }
                            });
                        } else {
                            if (loadVersion == reviewsLoadVersion && addedIds.add(reviewId)) {
                                addReviewToList("Anonymous", reviewText, rating != null ? rating.floatValue() : 0, timestampText);
                            }
                        }
                    }
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

    private void loadTutorTutorialData() {
        DatabaseReference tutorialSessionsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        
        tutorialSessionsRef.orderByChild("tutorId").equalTo(tutorId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.getChildrenCount() == 0) {
                        updateTutorialSummary(new ArrayList<>());
                        updateUpcomingTutorials(new ArrayList<>());
                        return;
                    }

                    List<Tutorial> allTutorials = new ArrayList<>();
                    List<Tutorial> upcomingTutorials = new ArrayList<>();

                    for (DataSnapshot tutorialSnap : snapshot.getChildren()) {
                        String tutorialId = tutorialSnap.getKey();
                        Tutorial tutorial = createTutorialFromSnapshot(tutorialSnap, tutorialId);
                        allTutorials.add(tutorial);
                        
                        if (isTutorialUpcoming(tutorial)) {
                            upcomingTutorials.add(tutorial);
                        }
                    }

                    updateTutorialSummary(allTutorials);
                    updateUpcomingTutorials(upcomingTutorials);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    tutorialStats.setText("Error loading tutorial data.");
                }
            });
    }

    private Tutorial createTutorialFromSnapshot(DataSnapshot snapshot, String tutorialId) {
        String tutorialName = snapshot.child("tutorialName").getValue(String.class);
        String topic = snapshot.child("topic").getValue(String.class);
        String fee = snapshot.child("fee").getValue(String.class);
        String date = snapshot.child("date").getValue(String.class);
        String startTime = snapshot.child("startTime").getValue(String.class);
        String endTime = snapshot.child("endTime").getValue(String.class);
        String address = snapshot.child("address").getValue(String.class);
        String tutorName = snapshot.child("tutorName").getValue(String.class);
        String description = snapshot.child("description").getValue(String.class);

        Tutorial tutorial = new Tutorial(
            tutorialName != null ? tutorialName : "Unknown Tutorial",
            topic != null ? topic : "General",
            fee != null ? fee : "Free",
            date != null ? date : "TBD",
            startTime != null ? startTime : "TBD",
            endTime != null ? endTime : "TBD",
            description != null ? description : "No description available",
            address != null ? address : "Location TBD",
            0.0, 0.0, "",
            tutorName != null ? tutorName : "Unknown Tutor",
            "",
            ""
        );
        tutorial.setTutorialId(tutorialId);
        return tutorial;
    }

    private boolean isTutorialUpcoming(Tutorial tutorial) {
        if (tutorial.getDate() == null) return false;
        
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date tutorialDate = dateFormat.parse(tutorial.getDate());
            Date currentDate = new Date();
            
            return tutorialDate != null && tutorialDate.after(currentDate);
        } catch (ParseException e) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date tutorialDate = dateFormat.parse(tutorial.getDate());
                Date currentDate = new Date();
                
                return tutorialDate != null && tutorialDate.after(currentDate);
            } catch (ParseException e2) {
                return false;
            }
        }
    }

    private void updateTutorialSummary(List<Tutorial> tutorials) {
        if (tutorials.isEmpty()) {
            tutorialStats.setText("No tutorials created yet.");
            return;
        }

        int totalTutorials = tutorials.size();
        int upcomingCount = 0;
        int pastCount = 0;

        for (Tutorial tutorial : tutorials) {
            if (isTutorialUpcoming(tutorial)) {
                upcomingCount++;
            } else {
                pastCount++;
            }
        }

        String statsText = String.format(Locale.getDefault(),
                "Total Tutorials: %d\nUpcoming: %d\nPast: %d",
                totalTutorials, upcomingCount, pastCount);
        
        tutorialStats.setText(statsText);
    }

    private void updateUpcomingTutorials(List<Tutorial> upcomingTutorials) {
        if (upcomingTutorials.isEmpty()) {
            upcomingTutorialsCard.setVisibility(View.GONE);
            return;
        }

        upcomingTutorialsCard.setVisibility(View.VISIBLE);
        upcomingTutorialsList.removeAllViews();

        for (Tutorial tutorial : upcomingTutorials) {
            View tutorialCardView = getLayoutInflater().inflate(R.layout.tutorial_card_item, upcomingTutorialsList, false);
            
            TextView tutorialName = tutorialCardView.findViewById(R.id.tutorialCardName);
            TextView tutorialFee = tutorialCardView.findViewById(R.id.tutorialCardFee);
            TextView tutorialTutor = tutorialCardView.findViewById(R.id.tutorialCardTutor);
            TextView tutorialDateTime = tutorialCardView.findViewById(R.id.tutorialCardDateTime);
            TextView tutorialLocation = tutorialCardView.findViewById(R.id.tutorialCardLocation);
            
            tutorialName.setText(tutorial.getTutorialName() != null ? tutorial.getTutorialName() : "Unnamed Tutorial");
            tutorialFee.setText(tutorial.getFee() != null ? "$" + tutorial.getFee() : "Free");
            tutorialTutor.setText(tutorial.getTutorName() != null ? tutorial.getTutorName() : "Unknown Tutor");
            
            String dateTime = String.format(Locale.getDefault(), "%s at %s - %s",
                    tutorial.getDate() != null ? tutorial.getDate() : "No date",
                    tutorial.getStartTime() != null ? tutorial.getStartTime() : "TBD",
                    tutorial.getEndTime() != null ? tutorial.getEndTime() : "TBD");
            tutorialDateTime.setText(dateTime);
            
            tutorialLocation.setText(tutorial.getAddress() != null ? tutorial.getAddress() : "Location TBD");
            
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
                startActivity(intent);
            });
            
            upcomingTutorialsList.addView(tutorialCardView);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh reviews when returning from ReviewActivity
        loadTutorReviews();
    }
}
