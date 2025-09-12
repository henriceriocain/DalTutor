package com.example.csci3130group1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ReviewActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private EditText reviewText;
    private android.widget.TextView submitButton;
    private android.widget.TextView cancelButton;
    private android.widget.TextView ratingValue;
    private android.widget.TextView headerTitle;
    private String reviewedUserId;
    private final java.text.SimpleDateFormat dfA = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
    private final java.text.SimpleDateFormat dfB = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        reviewedUserId = getIntent().getStringExtra("reviewedUserId");
        ratingBar = findViewById(R.id.rating_bar);
        ratingValue = findViewById(R.id.rating_value);
        reviewText = findViewById(R.id.review_text);
        submitButton = findViewById(R.id.submit_review);
        cancelButton = findViewById(R.id.cancel_button);
        headerTitle = findViewById(R.id.header_title);

        submitButton.setOnClickListener(v -> submitReview());
        if (cancelButton != null) cancelButton.setOnClickListener(v -> finish());
        if (ratingValue != null && ratingBar != null) {
            ratingValue.setText(String.format(java.util.Locale.getDefault(), "%.1f", ratingBar.getRating()));
            ratingBar.setOnRatingBarChangeListener((rb, rating, fromUser) -> {
                ratingValue.setText(String.format(java.util.Locale.getDefault(), "%.1f", rating));
            });
        }

        // Set tutor name in header if available
        if (reviewedUserId != null && headerTitle != null) {
            FirebaseDatabase.getInstance().getReference("users").child(reviewedUserId).child("name")
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                            String name = snapshot.getValue(String.class);
                            if (name != null && !name.trim().isEmpty()) {
                                headerTitle.setText("Rate " + name);
                            } else {
                                headerTitle.setText("Rate this tutor");
                            }
                        }

                        @Override
                        public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                            headerTitle.setText("Rate this tutor");
                        }
                    });
        }
    }

    private void submitReview() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || reviewedUserId == null) {
            Toast.makeText(this, "Invalid session", Toast.LENGTH_SHORT).show();
            return;
        }

        float rating = ratingBar.getRating();
        String text = reviewText.getText().toString();

        // Enhance: also store reviewerName and reviewText for easier display
        com.google.firebase.database.DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        userRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot userSnap) {
                String reviewerName = userSnap.child("name").getValue(String.class);

                // First compute completed tutorials count with this tutor, then submit including the count
                computeCompletedSessionsWithTutor(currentUser.getUid(), reviewedUserId, count -> {
                    Map<String, Object> review = new HashMap<>();
                    review.put("fromUser", currentUser.getUid());
                    review.put("rating", rating);
                    review.put("text", text);
                    review.put("reviewText", text); // duplicate for newer readers
                    if (reviewerName != null && !reviewerName.isEmpty()) {
                        review.put("reviewerName", reviewerName);
                    }
                    review.put("timestamp", System.currentTimeMillis());
                    review.put("completedSessionsWithTutor", count);

                    // Create review and also notify the reviewed tutor
                    com.google.firebase.database.DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews").child(reviewedUserId);
                    String reviewId = reviewsRef.push().getKey();
                    if (reviewId == null) {
                        Toast.makeText(ReviewActivity.this, "Failed to generate review id", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reviewsRef.child(reviewId)
                            .setValue(review)
                            .addOnSuccessListener(aVoid -> {
                                // Push a notification to the reviewed tutor
                                Map<String, Object> notif = new HashMap<>();
                                notif.put("type", "REVIEW_RECEIVED");
                                notif.put("fromUserId", currentUser.getUid());
                                notif.put("fromUserEmail", currentUser.getEmail());
                                notif.put("reviewId", reviewId);
                                notif.put("rating", rating);
                                notif.put("timestamp", System.currentTimeMillis());
                                notif.put("read", false);

                                FirebaseDatabase.getInstance().getReference("users")
                                        .child(reviewedUserId)
                                        .child("notifications")
                                        .push()
                                        .setValue(notif);

                                Toast.makeText(ReviewActivity.this, "Review submitted!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(ReviewActivity.this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                // Fallback: still compute completed count, submit without reviewerName if user read fails
                computeCompletedSessionsWithTutor(currentUser.getUid(), reviewedUserId, count -> {
                    Map<String, Object> review = new HashMap<>();
                    review.put("fromUser", currentUser.getUid());
                    review.put("rating", rating);
                    review.put("text", text);
                    review.put("reviewText", text);
                    review.put("timestamp", System.currentTimeMillis());
                    review.put("completedSessionsWithTutor", count);

                    com.google.firebase.database.DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews").child(reviewedUserId);
                    String reviewId = reviewsRef.push().getKey();
                    if (reviewId == null) {
                        Toast.makeText(ReviewActivity.this, "Failed to generate review id", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reviewsRef.child(reviewId)
                            .setValue(review)
                            .addOnSuccessListener(aVoid -> {
                                Map<String, Object> notif = new HashMap<>();
                                notif.put("type", "REVIEW_RECEIVED");
                                notif.put("fromUserId", currentUser.getUid());
                                notif.put("fromUserEmail", currentUser.getEmail());
                                notif.put("reviewId", reviewId);
                                notif.put("rating", rating);
                                notif.put("timestamp", System.currentTimeMillis());
                                notif.put("read", false);

                                FirebaseDatabase.getInstance().getReference("users")
                                        .child(reviewedUserId)
                                        .child("notifications")
                                        .push()
                                        .setValue(notif);

                                Toast.makeText(ReviewActivity.this, "Review submitted!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(ReviewActivity.this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
            }
        });
    }

    private interface CountCallback { void onCount(int count); }

    private void computeCompletedSessionsWithTutor(String studentId, String tutorId, CountCallback cb) {
        com.google.firebase.database.DatabaseReference sessionsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        sessionsRef.orderByChild("tutorId").equalTo(tutorId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                int count = 0;
                long now = System.currentTimeMillis();
                for (com.google.firebase.database.DataSnapshot s : snapshot.getChildren()) {
                    // Must be registered
                    com.google.firebase.database.DataSnapshot reg = s.child("registeredStudents").child(studentId);
                    Boolean isReg = reg.getValue(Boolean.class);
                    if (isReg == null || !isReg) continue;

                    // Consider completed (not upcoming)
                    boolean upcoming = false;
                    try {
                        Long endTs = s.child("endTimestamp").getValue(Long.class);
                        if (endTs != null) {
                            upcoming = endTs > now;
                        } else {
                            String date = s.child("date").getValue(String.class);
                            if (date != null) {
                                java.util.Date d = null;
                                try { d = dfA.parse(date); } catch (Exception ignore) {}
                                if (d == null) { try { d = dfB.parse(date); } catch (Exception ignore2) {} }
                                if (d != null) {
                                    java.util.Calendar c = java.util.Calendar.getInstance();
                                    c.setTime(d);
                                    c.set(java.util.Calendar.HOUR_OF_DAY, 23);
                                    c.set(java.util.Calendar.MINUTE, 59);
                                    c.set(java.util.Calendar.SECOND, 59);
                                    c.set(java.util.Calendar.MILLISECOND, 999);
                                    upcoming = c.getTimeInMillis() > now;
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    if (!upcoming) count++;
                }
                cb.onCount(count);
            }
            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                cb.onCount(0);
            }
        });
    }
}
