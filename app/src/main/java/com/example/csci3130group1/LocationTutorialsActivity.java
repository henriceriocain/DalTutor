package com.example.csci3130group1;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3130group1.adapters.TutorialSearchAdapter;
import com.example.csci3130group1.models.TutorialSession;
import com.example.csci3130group1.utils.TutorialTimeUtils;
import com.example.csci3130group1.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationTutorialsActivity extends AppCompatActivity {

    private TextView locationNameTextView;
    private TextView tutorialCountTextView;
    private TextView emptyStateTextView;
    private Button backButton;

    private RecyclerView tutorialsRecycler;
    private TutorialSearchAdapter adapter;

    private TextInputEditText searchInput;
    private AutoCompleteTextView topicFilter;
    private TextInputEditText feeFilter;

    private DatabaseReference tutorialsRef;
    private final List<TutorialSession> allLocationTutorials = new ArrayList<>();

    private String locationName;
    private String placeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_tutorials);

        // Initialize views
        locationNameTextView = findViewById(R.id.location_name);
        tutorialCountTextView = findViewById(R.id.tutorial_count);
        emptyStateTextView = findViewById(R.id.empty_state_text);
        backButton = findViewById(R.id.back_button);

        // Filters
        searchInput = findViewById(R.id.searchInput);
        topicFilter = findViewById(R.id.topicFilter);
        feeFilter = findViewById(R.id.feeFilter);

        // Recycler setup
        tutorialsRecycler = findViewById(R.id.resultsRecyclerView);
        tutorialsRecycler.setLayoutManager(new LinearLayoutManager(this));
        tutorialsRecycler.setNestedScrollingEnabled(false);
        adapter = new TutorialSearchAdapter(this);
        // Provide current user ID so details screen can reflect registration
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            adapter.setCurrentUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
        tutorialsRecycler.setAdapter(adapter);

        // Get data from intent
        locationName = getIntent().getStringExtra("locationName");
        placeId = getIntent().getStringExtra("placeId");

        if (locationName == null || placeId == null) {
            Toast.makeText(this, "Location information not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set location name
        locationNameTextView.setText(locationName);

        // Initialize Firebase reference
        tutorialsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");

        // Setup topic dropdown and filter listeners
        setupTopicDropdown();
        setupFilterListeners();

        // Load tutorials for this location
        loadTutorialsForLocation();

        // Back button functionality
        backButton.setOnClickListener(v -> finish());
    }

    private void setupTopicDropdown() {
        String[] topics = {
                "", // All Topics
                "Computer Science", "Mathematics", "Physics", "Chemistry", "Biology",
                "Engineering", "Business", "Economics", "Psychology", "History",
                "English", "French", "Spanish", "Philosophy", "Statistics",
                "Accounting", "Finance", "Marketing", "Management", "Sociology",
                "Political Science", "Geography", "Anthropology", "Art", "Music",
                "Theatre", "Literature", "Health Sciences", "Nursing", "Medicine",
                "Law", "Architecture", "Environmental Science", "Data Science",
                "Other"
        };
        ArrayAdapter<String> topicAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                topics
        );
        topicFilter.setAdapter(topicAdapter);
        topicFilter.setText("", false);
    }

    private void setupFilterListeners() {
        // Text search
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { applyFilters(); }
        });

        // Topic change
        topicFilter.setOnItemClickListener((parent, view, position, id) -> applyFilters());

        // Fee change
        feeFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { applyFilters(); }
        });
    }

    private void loadTutorialsForLocation() {
        tutorialsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allLocationTutorials.clear();
                long now = System.currentTimeMillis();

                for (DataSnapshot tutorialSnapshot : dataSnapshot.getChildren()) {
                    String tutorialPlaceId = tutorialSnapshot.child("placeId").getValue(String.class);

                    if (!placeId.equals(tutorialPlaceId)) continue;

                    // Upcoming-only filter
                    boolean isUpcoming = false;
                    Long endTimestamp = tutorialSnapshot.child("endTimestamp").getValue(Long.class);
                    if (endTimestamp != null) {
                        isUpcoming = endTimestamp >= now;
                    } else {
                        String date = tutorialSnapshot.child("date").getValue(String.class);
                        String endTime = tutorialSnapshot.child("endTime").getValue(String.class);
                        isUpcoming = TutorialTimeUtils.isUpcoming(date, endTime, now);
                    }
                    if (!isUpcoming) continue;

                    try {
                        TutorialSession t = tutorialSnapshot.getValue(TutorialSession.class);
                        if (t != null) {
                            t.setTutorialId(tutorialSnapshot.getKey());
                            allLocationTutorials.add(t);
                        }
                    } catch (Exception ignore) {}
                }

                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(LocationTutorialsActivity.this,
                        "Failed to load tutorials: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        String query = safeLower(searchInput.getText());
        String topic = topicFilter.getText() != null ? topicFilter.getText().toString().trim().toLowerCase(Locale.getDefault()) : "";
        String feeText = feeFilter.getText() != null ? feeFilter.getText().toString().trim() : "";

        List<TutorialSession> filtered = new ArrayList<>();
        for (TutorialSession t : allLocationTutorials) {
            boolean matches = true;

            // Query matches tutorialName or topic or description
            if (!query.isEmpty()) {
                boolean q = (t.getTutorialName() != null && t.getTutorialName().toLowerCase(Locale.getDefault()).contains(query)) ||
                            (t.getTopic() != null && t.getTopic().toLowerCase(Locale.getDefault()).contains(query)) ||
                            (t.getDescription() != null && t.getDescription().toLowerCase(Locale.getDefault()).contains(query));
                if (!q) matches = false;
            }

            // Topic filter
            if (matches && !topic.isEmpty()) {
                if (t.getTopic() == null || !t.getTopic().toLowerCase(Locale.getDefault()).contains(topic)) {
                    matches = false;
                }
            }

            // Fee filter (max)
            if (matches && !feeText.isEmpty()) {
                try {
                    double maxFee = Double.parseDouble(feeText);
                    double tutorialFee = t.getFee() != null ? Double.parseDouble(t.getFee()) : 0;
                    if (tutorialFee > maxFee) matches = false;
                } catch (NumberFormatException ignored) {}
            }

            if (matches) filtered.add(t);
        }

        adapter.updateTutorials(filtered);
        updateTutorialCount(filtered.size());
        updateEmptyState(filtered.isEmpty());
    }

    private String safeLower(CharSequence s) {
        return s == null ? "" : s.toString().trim().toLowerCase(Locale.getDefault());
    }

    private void updateTutorialCount(int count) {
        String countText = count == 1 ? "1 tutorial available" : count + " tutorials available";
        tutorialCountTextView.setText(countText);
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyStateTextView.setVisibility(android.view.View.VISIBLE);
            tutorialsRecycler.setVisibility(android.view.View.GONE);
        } else {
            emptyStateTextView.setVisibility(android.view.View.GONE);
            tutorialsRecycler.setVisibility(android.view.View.VISIBLE);
        }
    }
}
