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

        Map<String, Object> review = new HashMap<>();
        review.put("fromUser", currentUser.getUid());
        review.put("rating", rating);
        review.put("text", text);
        review.put("timestamp", System.currentTimeMillis());

        FirebaseDatabase.getInstance().getReference("reviews")
                .child(reviewedUserId)
                .push()
                .setValue(review)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
