package com.example.csci3130group1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReviewsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View emptyState;
    private android.widget.AutoCompleteTextView sortDropdown;
    private ReviewAdapter adapter;
    private final List<ReviewItem> allReviews = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews_list);

        recyclerView = findViewById(R.id.reviewsRecycler);
        emptyState = findViewById(R.id.emptyState);
        sortDropdown = findViewById(R.id.sortDropdown);

        adapter = new ReviewAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.reviews_sort_options,
                android.R.layout.simple_list_item_1
        );
        sortDropdown.setAdapter(spinnerAdapter);
        sortDropdown.setOnItemClickListener((parent, view, position, id) -> applySort());
        // Default selection
        sortDropdown.setText(getResources().getStringArray(R.array.reviews_sort_options)[0], false);

        loadReviews();
    }

    private void loadReviews() {
        String tutorId = getIntent().getStringExtra("tutorId");
        if (tutorId == null || tutorId.isEmpty()) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                tutorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }
        }
        if (tutorId == null || tutorId.isEmpty()) {
            showEmpty();
            return;
        }

        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews").child(tutorId);
        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allReviews.clear();
                for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                    ReviewItem item = ReviewItem.fromSnapshot(reviewSnap);
                    if (item != null) allReviews.add(item);
                }
                applySort();
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showEmpty();
            }
        });
    }

    private void showEmpty() {
        allReviews.clear();
        adapter.notifyDataSetChanged();
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
    }

    private void updateEmptyState() {
        if (emptyState != null) emptyState.setVisibility(allReviews.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void applySort() {
        int pos = 0;
        CharSequence sel = sortDropdown.getText();
        if (sel != null) {
            String s = sel.toString();
            String[] opts = getResources().getStringArray(R.array.reviews_sort_options);
            for (int i = 0; i < opts.length; i++) {
                if (opts[i].equals(s)) { pos = i; break; }
            }
        }
        // 0: Newest, 1: Oldest, 2: Rating High→Low, 3: Rating Low→High
        if (pos == 0) {
            Collections.sort(allReviews, Comparator.comparingLong((ReviewItem r) -> r.timestamp).reversed());
        } else if (pos == 1) {
            Collections.sort(allReviews, Comparator.comparingLong(r -> r.timestamp));
        } else if (pos == 2) {
            Collections.sort(allReviews, Comparator.comparingDouble((ReviewItem r) -> r.rating).reversed()
                    .thenComparing(Comparator.comparingLong((ReviewItem r) -> r.timestamp).reversed()));
        } else if (pos == 3) {
            Collections.sort(allReviews, Comparator.comparingDouble((ReviewItem r) -> r.rating)
                    .thenComparing(Comparator.comparingLong(r -> r.timestamp)));
        }
        adapter.notifyDataSetChanged();
    }

    private class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.VH> {
        private final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

        class VH extends RecyclerView.ViewHolder {
            TextView reviewerName, reviewText, reviewRating, reviewTimestamp;
            VH(@NonNull View itemView) {
                super(itemView);
                reviewerName = itemView.findViewById(R.id.reviewerName);
                reviewText = itemView.findViewById(R.id.reviewText);
                reviewRating = itemView.findViewById(R.id.reviewRating);
                reviewTimestamp = itemView.findViewById(R.id.reviewTimestamp);
            }
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.review_item, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            ReviewItem r = allReviews.get(position);
            holder.reviewerName.setText(r.reviewerName != null && !r.reviewerName.isEmpty() ? r.reviewerName : (r.reviewerEmail != null ? r.reviewerEmail : "Anonymous"));
            holder.reviewText.setText(r.text != null ? r.text : "");
            holder.reviewRating.setText(String.format(java.util.Locale.getDefault(), "%.1f ★", r.rating));
            if (r.timestamp > 0) {
                holder.reviewTimestamp.setText(df.format(new java.util.Date(r.timestamp)));
                holder.reviewTimestamp.setVisibility(View.VISIBLE);
            } else {
                holder.reviewTimestamp.setVisibility(View.GONE);
            }
        }

        @Override public int getItemCount() { return allReviews.size(); }
    }

    private static class ReviewItem {
        String id;
        String fromUserId;
        String reviewerName;
        String reviewerEmail;
        double rating;
        String text;
        long timestamp;

        static ReviewItem fromSnapshot(DataSnapshot snap) {
            try {
                ReviewItem r = new ReviewItem();
                r.id = snap.getKey();
                r.fromUserId = val(snap.child("fromUser").getValue());
                r.reviewerName = val(snap.child("reviewerName").getValue());
                r.text = val(snap.child("reviewText").getValue());
                if (r.text == null || r.text.isEmpty()) r.text = val(snap.child("text").getValue());
                Object ratingObj = snap.child("rating").getValue();
                r.rating = ratingObj instanceof Number ? ((Number) ratingObj).doubleValue() : parseDoubleSafe(val(ratingObj));
                Object tsObj = snap.child("timestamp").getValue();
                r.timestamp = tsObj instanceof Number ? ((Number) tsObj).longValue() : parseLongSafe(val(tsObj));
                return r;
            } catch (Exception e) {
                return null;
            }
        }

        private static String val(Object v) { return v == null ? null : String.valueOf(v); }
        private static double parseDoubleSafe(String s) { try { return s == null ? 0.0 : Double.parseDouble(s); } catch (Exception e) { return 0.0; } }
        private static long parseLongSafe(String s) { try { return s == null ? 0L : Long.parseLong(s); } catch (Exception e) { return 0L; } }
    }
}
