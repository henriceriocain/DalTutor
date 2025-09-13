package com.example.csci3130group1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.example.csci3130group1.utils.TutorialSummaryHelper;

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
    private boolean readOnlyMode = false;
    private boolean isCurrentUserStudent = false;
    
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
    private TextView addReviewLink;
    private LinearLayout reviewsList;
    private TextView noReviewsText;
    private TextView tutorReviewsHeader;
    private TextView mostRecentReviewLabel;
    private LinearLayout upcomingTutorialsList;
    private com.google.android.material.card.MaterialCardView upcomingTutorialsCard;
    private TextView tutorialStats;
    private TextView viewAllTutorialsButton;
    private TextView viewAllReviewsButton;
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

        readOnlyMode = getIntent().getBooleanExtra("readOnly", false);

        // Get current user ID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        initializeViews();
        // Hide by default until role is loaded
        if (addReviewLink != null) addReviewLink.setVisibility(View.GONE);
        setupClickListeners();
        loadTutorProfile();
        loadTutorReviews();
        loadTutorTutorialData();

        // Hide only if viewing own profile; students can review in read-only views
        if (readOnlyMode && addReviewLink != null && currentUserId != null && currentUserId.equals(tutorId)) {
            addReviewLink.setVisibility(View.GONE);
        }

        // Determine current user's role to control review permissions
        if (currentUser != null) {
            final String currentIdFinal = currentUserId;
            final String tutorIdFinal = tutorId;
            final boolean readOnlyFinal = readOnlyMode;
            com.example.csci3130group1.utils.SessionRole.resolveWithFallback(this, currentUser, role -> {
                isCurrentUserStudent = role == com.example.csci3130group1.utils.SessionRole.Role.STUDENT;
                // If role is unknown, optimistically show button (enforce on click)
                boolean roleKnown = role != com.example.csci3130group1.utils.SessionRole.Role.UNKNOWN;
                boolean showBase = !currentIdFinal.equals(tutorIdFinal);
                boolean show = (isCurrentUserStudent || !roleKnown) && showBase;
                if (addReviewLink != null) addReviewLink.setVisibility(show ? View.VISIBLE : View.GONE);
                if (show) updateReviewLinkLabel();
            });
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
        addReviewLink = findViewById(R.id.addReviewLink);
        reviewsList = findViewById(R.id.reviewsList);
        noReviewsText = findViewById(R.id.noReviewsText);
        tutorReviewsHeader = findViewById(R.id.tutorReviewsHeader);
        mostRecentReviewLabel = findViewById(R.id.mostRecentReviewLabel);
        upcomingTutorialsList = findViewById(R.id.upcomingTutorialsList);
        upcomingTutorialsCard = findViewById(R.id.upcoming_tutorials_card);
        tutorialStats = findViewById(R.id.tutorialStats);
        viewAllTutorialsButton = findViewById(R.id.view_all_tutorials_button);
        viewAllReviewsButton = findViewById(R.id.view_all_reviews_button);
    }

    private void updateReviewLinkLabel() {
        if (tutorId == null || currentUserId == null || addReviewLink == null) return;
        DatabaseReference rref = FirebaseDatabase.getInstance().getReference("reviews").child(tutorId);
        rref.orderByChild("fromUser").equalTo(currentUserId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean has = snapshot.getChildrenCount() > 0;
                    addReviewLink.setText(has ? "Update your review" : "Add review");
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { /* ignore */ }
            });
    }

    private void setupClickListeners() {
        addReviewLink.setOnClickListener(v -> {
            // Prevent self-review
            if (currentUserId != null && currentUserId.equals(tutorId)) {
                Toast.makeText(this, "You cannot review yourself", Toast.LENGTH_SHORT).show();
                return;
            }
            // Already known student? proceed
            if (isCurrentUserStudent) {
                Intent reviewIntent = new Intent(this, ReviewActivity.class);
                reviewIntent.putExtra("reviewedUserId", tutorId);
                startActivity(reviewIntent);
                return;
            }
            // Resolve role on-demand if unknown and enforce
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Please log in to add a review", Toast.LENGTH_SHORT).show();
                return;
            }
            // Allow anyone to review tutors - restriction removed
            Intent reviewIntent = new Intent(TutorProfileActivity.this, ReviewActivity.class);
            reviewIntent.putExtra("reviewedUserId", tutorId);
            startActivity(reviewIntent);
        });

        viewAllTutorialsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, TutorialHistoryActivity.class);
            intent.putExtra("tutorId", tutorId);
            intent.putExtra("isTutorView", true);
            startActivity(intent);
        });

        if (viewAllReviewsButton != null) {
            viewAllReviewsButton.setOnClickListener(v -> {
                Intent i = new Intent(this, ReviewsActivity.class);
                i.putExtra("tutorId", tutorId);
                startActivity(i);
            });
        }
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

                    // Set contact number (format as (123) 456-7891)
                    if (contactNumber != null && !contactNumber.trim().isEmpty()) {
                        tutorContact.setText(formatPhone(contactNumber));
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

    private String formatPhone(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1); // strip country code 1 for North America
        }
        if (digits.length() == 10) {
            String area = digits.substring(0, 3);
            String mid = digits.substring(3, 6);
            String last = digits.substring(6);
            return String.format(Locale.US, "(%s) %s-%s", area, mid, last);
        }
        // Fallback: return original if not 10 digits
        return raw;
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
                    tutorRating.setText("No reviews");
                    ratingContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tutorRating.setText("No reviews");
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
        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (loadVersion != reviewsLoadVersion) return;
                reviewsList.removeAllViews();
                
                if (snapshot.getChildrenCount() == 0) {
                    if (tutorReviewsHeader != null) tutorReviewsHeader.setText(getString(R.string.reviews_count, 0));
                    noReviewsText.setVisibility(View.VISIBLE);
                    if (viewAllReviewsButton != null) viewAllReviewsButton.setVisibility(View.GONE);
                    if (mostRecentReviewLabel != null) mostRecentReviewLabel.setVisibility(View.GONE);
                    return;
                }

                noReviewsText.setVisibility(View.GONE);
                if (tutorReviewsHeader != null) tutorReviewsHeader.setText(getString(R.string.reviews_count, (int) snapshot.getChildrenCount()));
                // Find most recent review by timestamp
                DataSnapshot latest = null;
                long maxTs = Long.MIN_VALUE;
                for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                    Object tsObj = reviewSnap.child("timestamp").getValue();
                    long ts = 0L;
                    if (tsObj instanceof Number) ts = ((Number) tsObj).longValue();
                    if (latest == null || ts > maxTs) { latest = reviewSnap; maxTs = ts; }
                }

                if (latest != null) {
                    String reviewerName = latest.child("reviewerName").getValue(String.class);
                    String reviewText = latest.child("reviewText").getValue(String.class);
                    if (reviewText == null || reviewText.isEmpty()) reviewText = latest.child("text").getValue(String.class);
                    Double rating = latest.child("rating").getValue(Double.class);
                    Long completedCount = null;
                    try { completedCount = latest.child("completedSessionsWithTutor").getValue(Long.class); } catch (Exception ignored) {}
                    String timestampText = null;
                    Object tsObj = latest.child("timestamp").getValue();
                    if (tsObj instanceof Number) {
                        long ts = ((Number) tsObj).longValue();
                        java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT);
                        df.setTimeZone(java.util.TimeZone.getTimeZone("America/Halifax"));
                        timestampText = df.format(new java.util.Date(ts));
                    }

                    String fromUserId = latest.child("fromUser").getValue(String.class);
                    
                    if (reviewerName != null && !reviewerName.isEmpty()) {
                        addReviewToList(reviewerName, reviewText, rating != null ? rating.floatValue() : 0f, timestampText, completedCount != null ? completedCount.intValue() : 0, fromUserId);
                    } else {
                        if (fromUserId != null && !fromUserId.isEmpty()) {
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(fromUserId);
                            final String reviewTextFinal = reviewText;
                            final float ratingFinal = rating != null ? rating.floatValue() : 0f;
                            final String timestampFinal = timestampText;
                            final int completedFinal = completedCount != null ? completedCount.intValue() : 0;
                            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    String name = userSnap.child("name").getValue(String.class);
                                    if (name == null || name.isEmpty()) {
                                        String email = userSnap.child("email").getValue(String.class);
                                        name = email != null ? email : "Anonymous";
                                    }
                                    addReviewToList(name, reviewTextFinal, ratingFinal, timestampFinal, completedFinal, fromUserId);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {
                                    addReviewToList("Anonymous", reviewTextFinal, ratingFinal, timestampFinal, completedFinal, fromUserId);
                                }
                            });
                        } else {
                            addReviewToList("Anonymous", reviewText, rating != null ? rating.floatValue() : 0f, timestampText, completedCount != null ? completedCount.intValue() : 0, null);
                        }
                    }
                }

                if (viewAllReviewsButton != null) viewAllReviewsButton.setVisibility(View.VISIBLE);
                if (mostRecentReviewLabel != null) mostRecentReviewLabel.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                noReviewsText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addReviewToList(String reviewerName, String reviewText, float rating, String timestamp, int completedCount) {
        addReviewToList(reviewerName, reviewText, rating, timestamp, completedCount, null);
    }

    private void addReviewToList(String reviewerName, String reviewText, float rating, String timestamp, int completedCount, String fromUserId) {
        View reviewView = LayoutInflater.from(this).inflate(R.layout.review_item, reviewsList, false);
        
        // Use ReviewItemBinder for consistent behavior including orange links for tutors
        long timestampMillis = 0;
        if (timestamp != null) {
            try {
                // Try to parse back from formatted string - not ideal but maintains compatibility
                java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT);
                df.setTimeZone(java.util.TimeZone.getTimeZone("America/Halifax"));
                java.util.Date date = df.parse(timestamp);
                if (date != null) timestampMillis = date.getTime();
            } catch (Exception e) {
                timestampMillis = 0; // Fallback to no timestamp
            }
        }
        
        com.example.csci3130group1.utils.ReviewItemBinder.bindReviewItem(
            reviewView, 
            reviewerName, 
            reviewText, 
            (double) rating, 
            timestampMillis, 
            fromUserId, 
            this, 
            completedCount > 0 ? completedCount : null
        );

        reviewsList.addView(reviewView);
    }

    private void loadTutorTutorialData() {
        DatabaseReference tutorialSessionsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        
        tutorialSessionsRef.orderByChild("tutorId").equalTo(tutorId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Tutorial> allTutorials = new ArrayList<>();

                    for (DataSnapshot tutorialSnap : snapshot.getChildren()) {
                        String tutorialId = tutorialSnap.getKey();
                        Tutorial tutorial = createTutorialFromSnapshot(tutorialSnap, tutorialId);
                        allTutorials.add(tutorial);
                    }

                    // Use the reusable TutorialSummaryHelper
                    TutorialSummaryHelper.TutorialSummaryConfig config = 
                        new TutorialSummaryHelper.TutorialSummaryConfig(TutorialSummaryHelper.ViewMode.OTHER_TUTOR_PROFILE)
                            .setTutorId(tutorId)
                            .setShowUpcomingSection(true)
                            .setShowStatsSection(true);

                    TutorialSummaryHelper.bindTutorialSummary(
                        TutorProfileActivity.this,
                        findViewById(android.R.id.content),
                        allTutorials,
                        config
                    );
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (tutorialStats != null) {
                        tutorialStats.setText("Error loading tutorial data.");
                    }
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


    @Override
    protected void onResume() {
        super.onResume();
        // Refresh reviews and stats when returning from ReviewActivity
        loadTutorReviews();
        loadTutorStats();
        updateReviewLinkLabel();
    }
}
