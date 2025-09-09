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
    private Button submitButton;
    private String reviewedUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        reviewedUserId = getIntent().getStringExtra("reviewedUserId");
        ratingBar = findViewById(R.id.rating_bar);
        reviewText = findViewById(R.id.review_text);
        submitButton = findViewById(R.id.submit_review);

        submitButton.setOnClickListener(v -> submitReview());
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

                Map<String, Object> review = new HashMap<>();
                review.put("fromUser", currentUser.getUid());
                review.put("rating", rating);
                review.put("text", text);
                review.put("reviewText", text); // duplicate for newer readers
                if (reviewerName != null && !reviewerName.isEmpty()) {
                    review.put("reviewerName", reviewerName);
                }
                review.put("timestamp", System.currentTimeMillis());

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
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                // Fallback: submit without reviewerName if user read fails
                Map<String, Object> review = new HashMap<>();
                review.put("fromUser", currentUser.getUid());
                review.put("rating", rating);
                review.put("text", text);
                review.put("reviewText", text);
                review.put("timestamp", System.currentTimeMillis());

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
            }
        });
    }
}
