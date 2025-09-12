package com.example.csci3130group1.utils;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.example.csci3130group1.R;
import com.example.csci3130group1.TutorProfileActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;

public class ReviewItemBinder {
    private static final Map<String, Boolean> tutorFlagCache = new HashMap<>();

    public static void bindReviewItem(View reviewView, String reviewerName, String reviewText, 
                                    double rating, long timestamp, String fromUserId, Context context) {
        bindReviewItem(reviewView, reviewerName, reviewText, rating, timestamp, fromUserId, context, null);
    }

    // Overload with optional completed tutorials count for credibility display
    public static void bindReviewItem(View reviewView, String reviewerName, String reviewText,
                                      double rating, long timestamp, String fromUserId, Context context,
                                      Integer completedCount) {
        TextView reviewerNameView = reviewView.findViewById(R.id.reviewerName);
        TextView reviewTextView = reviewView.findViewById(R.id.reviewText);
        TextView reviewRatingView = reviewView.findViewById(R.id.reviewRating);
        TextView reviewTimestampView = reviewView.findViewById(R.id.reviewTimestamp);
        TextView reviewerCredView = reviewView.findViewById(R.id.reviewerCredibility);

        // Basic data binding
        reviewerNameView.setText(reviewerName != null && !reviewerName.isEmpty() ? reviewerName : "Anonymous");
        reviewTextView.setText(reviewText != null && !reviewText.isEmpty() ? reviewText : "No review text provided.");
        reviewRatingView.setText(String.format(java.util.Locale.getDefault(), "%.1f", rating));
        
        if (timestamp > 0) {
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            reviewTimestampView.setText(df.format(new java.util.Date(timestamp)));
            reviewTimestampView.setVisibility(View.VISIBLE);
        } else {
            reviewTimestampView.setVisibility(View.GONE);
        }

        // Reviewer credibility (completed tutorials with this tutor)
        if (reviewerCredView != null) {
            if (completedCount != null && completedCount > 0) {
                String label = completedCount == 1 ? "1 completed tutorial with this tutor" : completedCount + " completed tutorials with this tutor";
                reviewerCredView.setText(label);
                reviewerCredView.setVisibility(View.VISIBLE);
            } else {
                reviewerCredView.setVisibility(View.GONE);
            }
        }

        // Handle tutor profile links
        setupTutorLink(reviewerNameView, fromUserId, context);
    }

    public static void bindReviewItemLegacy(View reviewView, String reviewerName, String reviewText, 
                                          float rating, String timestamp, String fromUserId, Context context) {
        TextView reviewerNameView = reviewView.findViewById(R.id.reviewerName);
        TextView reviewTextView = reviewView.findViewById(R.id.reviewText);
        TextView reviewRatingView = reviewView.findViewById(R.id.reviewRating);
        TextView reviewTimestampView = reviewView.findViewById(R.id.reviewTimestamp);

        // Basic data binding (legacy format)
        reviewerNameView.setText(reviewerName != null ? reviewerName : "Anonymous");
        reviewTextView.setText(reviewText != null ? reviewText : "");
        reviewRatingView.setText(String.format(java.util.Locale.getDefault(), "%.1f ★", rating));
        
        if (timestamp != null && !timestamp.isEmpty()) {
            reviewTimestampView.setText(timestamp);
            reviewTimestampView.setVisibility(View.VISIBLE);
        } else {
            reviewTimestampView.setVisibility(View.GONE);
        }

        // Handle tutor profile links
        setupTutorLink(reviewerNameView, fromUserId, context);
    }

    private static void setupTutorLink(TextView reviewerNameView, String fromUserId, Context context) {
        if (fromUserId == null || fromUserId.isEmpty()) {
            return; // No user ID, can't create tutor link
        }

        // Reset styling first
        reviewerNameView.setTextColor(0xFF111827); // Default color from review_item.xml
        reviewerNameView.setClickable(false);
        reviewerNameView.setOnClickListener(null);
        
        // Set a tag so we can verify this view still represents the same user when async callback returns
        reviewerNameView.setTag(fromUserId);
        
        // Check cache first
        if (tutorFlagCache.containsKey(fromUserId)) {
            boolean isEnabled = tutorFlagCache.get(fromUserId);
            Object tag = reviewerNameView.getTag();
            if (tag != null && fromUserId.equals(tag)) {
                applyTutorLinkStylingIfEnabled(reviewerNameView, isEnabled, fromUserId, context);
            }
        } else {
            // Look up tutor status
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                    .child(fromUserId).child("isTutorEnabled");
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Boolean enabled = snapshot.getValue(Boolean.class);
                    tutorFlagCache.put(fromUserId, enabled != null && enabled);
                    // Ensure this view still represents same user
                    Object tag = reviewerNameView.getTag();
                    if (tag != null && fromUserId.equals(tag)) {
                        applyTutorLinkStylingIfEnabled(reviewerNameView, enabled != null && enabled, fromUserId, context);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) { /* no-op */ }
            });
        }
    }

    private static void applyTutorLinkStylingIfEnabled(TextView textView, boolean enabled, String authorId, Context context) {
        if (!enabled) return;
        // Apply link color and click listener to open TutorProfileActivity
        textView.setTextColor(context.getResources().getColor(R.color.brown_primary));
        textView.setClickable(true);
        textView.setOnClickListener(v -> {
            Intent i = new Intent(context, TutorProfileActivity.class);
            i.putExtra("tutorId", authorId);
            i.putExtra("readOnly", true);
            context.startActivity(i);
        });
    }
}
