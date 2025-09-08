package com.example.csci3130group1.ui.search_for_tutorials;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3130group1.GoogleMapActivity;
import com.example.csci3130group1.R;
import com.example.csci3130group1.adapters.TutorialSearchAdapter;
import com.example.csci3130group1.adapters.TutorSearchAdapter;
import com.example.csci3130group1.databinding.FragmentSearchForTutorialsBinding;
import com.example.csci3130group1.models.TutorialSession;
import com.example.csci3130group1.models.TutorProfile;
import android.widget.Button;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchForTutorialsFragment extends Fragment {
    private FragmentSearchForTutorialsBinding binding;
    
    // UI Components
    private CardView mapsCard;
    private Button searchTutorialsButton, searchTutorsButton;
    private TextInputEditText searchInput, feeFilter;
    private AutoCompleteTextView topicFilter, locationFilter;
    private Button searchButton;
    private CardView resultsCard;
    private RecyclerView resultsRecyclerView;
    private View emptyStateLayout;
    
    // Adapters
    private TutorialSearchAdapter tutorialAdapter;
    private TutorSearchAdapter tutorAdapter;
    
    // Data
    private List<TutorialSession> allTutorials;
    private List<TutorProfile> allTutors;
    private boolean searchingForTutorials = true; // true for tutorials, false for tutors
    
    // Firebase
    private DatabaseReference tutorialSessionsRef;
    private DatabaseReference usersRef;
    private DatabaseReference reviewsRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchForTutorialsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        
        initializeFirebaseReferences();
        initializeViews(root);
        setupClickListeners();
        setupTopicFilter();
        setupLocationFilter();
        setupSearchInput();
        
        // Load initial data
        loadAllTutorials();
        
        return root;
    }
    
    private void initializeFirebaseReferences() {
        tutorialSessionsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");
    }
    
    private void initializeViews(View root) {
        // Initialize UI components
        mapsCard = root.findViewById(R.id.mapsCard);
        searchTutorialsButton = root.findViewById(R.id.searchTutorialsButton);
        searchTutorsButton = root.findViewById(R.id.searchTutorsButton);
        searchInput = root.findViewById(R.id.searchInput);
        topicFilter = root.findViewById(R.id.topicFilter);
        locationFilter = root.findViewById(R.id.locationFilter);
        feeFilter = root.findViewById(R.id.feeFilter);
        searchButton = root.findViewById(R.id.searchButton);
        resultsCard = root.findViewById(R.id.resultsCard);
        resultsRecyclerView = root.findViewById(R.id.resultsRecyclerView);
        emptyStateLayout = root.findViewById(R.id.emptyStateLayout);
        
        // Initialize data lists
        allTutorials = new ArrayList<>();
        allTutors = new ArrayList<>();
        
        // Initialize adapters
        tutorialAdapter = new TutorialSearchAdapter(requireContext());
        tutorAdapter = new TutorSearchAdapter(requireContext());
        
        // Setup RecyclerView
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        resultsRecyclerView.setAdapter(tutorialAdapter); // Start with tutorials
        
        // Set initial button selection state
        searchTutorialsButton.setSelected(true);
        searchTutorsButton.setSelected(false);
    }

    private void setupClickListeners() {
        // Maps card click listener
        mapsCard.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), GoogleMapActivity.class);
            startActivity(intent);
        });
        
        // Search mode toggle buttons
        searchTutorialsButton.setOnClickListener(v -> {
            if (!searchingForTutorials) {
                switchToTutorialSearch();
            }
        });
        
        searchTutorsButton.setOnClickListener(v -> {
            if (searchingForTutorials) {
                switchToTutorSearch();
            }
        });
        
        // Search button
        searchButton.setOnClickListener(v -> performSearch());
    }
    
    private void switchToTutorialSearch() {
        searchingForTutorials = true;
        
        // Update button selection states
        searchTutorialsButton.setSelected(true);
        searchTutorsButton.setSelected(false);
        
        // Switch adapter
        resultsRecyclerView.setAdapter(tutorialAdapter);
        
        // Update results header
        binding.resultsHeader.setText("Tutorial Results");
        
        // Clear current results and show tutorials if available
        if (!allTutorials.isEmpty()) {
            performSearch();
        }
    }
    
    private void switchToTutorSearch() {
        searchingForTutorials = false;
        
        // Update button selection states
        searchTutorsButton.setSelected(true);
        searchTutorialsButton.setSelected(false);
        
        // Switch adapter
        resultsRecyclerView.setAdapter(tutorAdapter);
        
        // Update results header
        binding.resultsHeader.setText("Tutor Results");
        
        // Load tutors if not already loaded
        if (allTutors.isEmpty()) {
            loadAllTutors();
        } else {
            performSearch();
        }
    }

    private void setupTopicFilter() {
        // Common computer science topics
        String[] topics = {
            "Computer Science", "Mathematics", "Physics", "Chemistry", "Biology",
            "Engineering", "Business", "Economics", "Psychology", "History",
            "English", "French", "Spanish", "Philosophy", "Statistics"
        };
        
        ArrayAdapter<String> topicAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            topics
        );
        topicFilter.setAdapter(topicAdapter);
    }
    
    private void setupLocationFilter() {
        List<String> locationOptions = new ArrayList<>();
        locationOptions.add("All Locations");
        
        try {
            // Load DAL locations from JSON file
            InputStream inputStream = requireContext().getAssets().open("dal_locations.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            
            String json = new String(buffer, "UTF-8");
            JSONArray jsonArray = new JSONArray(json);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject locationJson = jsonArray.getJSONObject(i);
                String name = locationJson.getString("name");
                String address = locationJson.getString("addr");
                
                // Format: "Building Name - Address"
                String displayText = name + " - " + address;
                locationOptions.add(displayText);
            }
            
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to load DAL locations: " + e.getMessage());
            // Add some fallback locations
            locationOptions.add("Computer Science Building");
            locationOptions.add("Killam Library");
            locationOptions.add("Student Union Building");
        }
        
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            locationOptions
        );
        locationFilter.setAdapter(locationAdapter);
        locationFilter.setText("All Locations", false);
    }
    
    private void setupSearchInput() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                // Auto-search as user types (with debouncing)
                searchButton.post(() -> {
                    if (s.toString().trim().length() > 2 || s.toString().trim().isEmpty()) {
                        performSearch();
                    }
                });
            }
        });
    }

    private void loadAllTutorials() {
        tutorialSessionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTutorials.clear();
                Log.d(TAG, "Loading " + snapshot.getChildrenCount() + " tutorials from Firebase");
                
                for (DataSnapshot tutorialSnapshot : snapshot.getChildren()) {
                    TutorialSession tutorial = tutorialSnapshot.getValue(TutorialSession.class);
                    if (tutorial != null) {
                        tutorial.setTutorialId(tutorialSnapshot.getKey());
                        allTutorials.add(tutorial);
                    }
                }
                
                Log.d(TAG, "Loaded " + allTutorials.size() + " tutorials");
                if (searchingForTutorials) {
                    performSearch();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load tutorials: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load tutorials", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadAllTutors() {
        // First, get all tutors who have at least one tutorial
        tutorialSessionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot tutorialSnapshot) {
                Set<String> tutorIds = new HashSet<>();
                
                // Collect unique tutor IDs from tutorials
                for (DataSnapshot tutorial : tutorialSnapshot.getChildren()) {
                    String tutorId = tutorial.child("tutorId").getValue(String.class);
                    if (tutorId != null && !tutorId.trim().isEmpty()) {
                        tutorIds.add(tutorId);
                    }
                }
                
                Log.d(TAG, "Found " + tutorIds.size() + " unique tutors with tutorials");
                
                // Load tutor profiles for each tutor with tutorials
                loadTutorProfiles(new ArrayList<>(tutorIds));
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load tutor IDs: " + error.getMessage());
            }
        });
    }
    
    private void loadTutorProfiles(List<String> tutorIds) {
        if (tutorIds.isEmpty()) {
            return;
        }
        
        allTutors.clear();
        final int totalTutors = tutorIds.size();
        final int[] loadedCount = {0};
        
        for (String tutorId : tutorIds) {
            usersRef.child(tutorId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                    loadedCount[0]++;
                    
                    if (userSnapshot.exists()) {
                        // Create TutorProfile from user data
                        String name = userSnapshot.child("name").getValue(String.class);
                        String email = userSnapshot.child("email").getValue(String.class);
                        String contact = userSnapshot.child("contact").getValue(String.class);
                        String degree = userSnapshot.child("degree").getValue(String.class);
                        String description = userSnapshot.child("description").getValue(String.class);
                        String profilePictureUrl = userSnapshot.child("profilePictureUrl").getValue(String.class);
                        
                        TutorProfile tutorProfile = new TutorProfile(tutorId, name, email, contact, 
                                                                    degree, description, profilePictureUrl);
                        
                        // Count tutorials for this tutor
                        countTutorialsAndLoadRating(tutorProfile);
                        
                        allTutors.add(tutorProfile);
                    }
                    
                    // When all tutors are loaded, perform search if we're in tutor mode
                    if (loadedCount[0] == totalTutors) {
                        Log.d(TAG, "Loaded " + allTutors.size() + " tutor profiles");
                        if (!searchingForTutorials) {
                            performSearch();
                        }
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loadedCount[0]++;
                    if (loadedCount[0] == totalTutors && !searchingForTutorials) {
                        performSearch();
                    }
                }
            });
        }
    }
    
    private void countTutorialsAndLoadRating(TutorProfile tutorProfile) {
        // Count tutorials
        tutorialSessionsRef.orderByChild("tutorId").equalTo(tutorProfile.getTutorId())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    tutorProfile.setTutorialCount((int) snapshot.getChildrenCount());
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        
        // Load rating
        reviewsRef.child(tutorProfile.getTutorId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float total = 0;
                int count = 0;
                
                for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                    Double rating = reviewSnap.child("rating").getValue(Double.class);
                    if (rating != null) {
                        total += rating.floatValue();
                        count++;
                    }
                }
                
                if (count > 0) {
                    tutorProfile.setAverageRating(total / count);
                    tutorProfile.setReviewCount(count);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    
    private void performSearch() {
        String query = searchInput.getText().toString().trim().toLowerCase();
        String topicQuery = topicFilter.getText().toString().trim().toLowerCase();
        String locationQuery = locationFilter.getText().toString().trim().toLowerCase();
        String feeQuery = feeFilter.getText().toString().trim();
        
        if (searchingForTutorials) {
            performTutorialSearch(query, topicQuery, locationQuery, feeQuery);
        } else {
            performTutorSearch(query, topicQuery);
        }
    }
    
    private void performTutorialSearch(String query, String topicQuery, String locationQuery, String feeQuery) {
        List<TutorialSession> filteredTutorials = new ArrayList<>();
        
        for (TutorialSession tutorial : allTutorials) {
            boolean matches = true;
            
            // Search query filter (matches name, topic, tutor name, or description)
            if (!query.isEmpty()) {
                boolean queryMatch = 
                    (tutorial.getTutorialName() != null && tutorial.getTutorialName().toLowerCase().contains(query)) ||
                    (tutorial.getTopic() != null && tutorial.getTopic().toLowerCase().contains(query)) ||
                    (tutorial.getTutorName() != null && tutorial.getTutorName().toLowerCase().contains(query)) ||
                    (tutorial.getDescription() != null && tutorial.getDescription().toLowerCase().contains(query));
                
                if (!queryMatch) matches = false;
            }
            
            // Topic filter
            if (matches && !topicQuery.isEmpty()) {
                if (tutorial.getTopic() == null || !tutorial.getTopic().toLowerCase().contains(topicQuery)) {
                    matches = false;
                }
            }
            
            // Location filter - handle "All Locations" and building name matching
            if (matches && !locationQuery.isEmpty() && !locationQuery.equals("all locations")) {
                boolean locationMatch = false;
                if (tutorial.getAddress() != null) {
                    // Check if address contains the location query
                    locationMatch = tutorial.getAddress().toLowerCase().contains(locationQuery);
                    
                    // Also check if it's a building name match (extract building name from location selection)
                    if (!locationMatch && locationQuery.contains(" - ")) {
                        String buildingName = locationQuery.split(" - ")[0].toLowerCase();
                        locationMatch = tutorial.getAddress().toLowerCase().contains(buildingName);
                    }
                }
                if (!locationMatch) matches = false;
            }
            
            // Fee filter
            if (matches && !feeQuery.isEmpty()) {
                try {
                    double maxFee = Double.parseDouble(feeQuery);
                    double tutorialFee = tutorial.getFee() != null ? Double.parseDouble(tutorial.getFee()) : 0;
                    if (tutorialFee > maxFee) {
                        matches = false;
                    }
                } catch (NumberFormatException e) {
                    // Invalid fee format, ignore filter
                }
            }
            
            if (matches) {
                filteredTutorials.add(tutorial);
            }
        }
        
        updateTutorialResults(filteredTutorials);
    }
    
    private void performTutorSearch(String query, String topicQuery) {
        List<TutorProfile> filteredTutors = new ArrayList<>();
        
        for (TutorProfile tutor : allTutors) {
            boolean matches = true;
            
            // Search query filter (matches name, degree, or description)
            if (!query.isEmpty()) {
                boolean queryMatch = 
                    (tutor.getName() != null && tutor.getName().toLowerCase().contains(query)) ||
                    (tutor.getDegree() != null && tutor.getDegree().toLowerCase().contains(query)) ||
                    (tutor.getDescription() != null && tutor.getDescription().toLowerCase().contains(query));
                
                if (!queryMatch) matches = false;
            }
            
            // Topic filter (check if tutor teaches this topic)
            if (matches && !topicQuery.isEmpty()) {
                // Check if tutor has tutorials in this topic
                boolean teachesTopicMatches = false;
                for (TutorialSession tutorial : allTutorials) {
                    if (tutorial.getTutorId().equals(tutor.getTutorId()) &&
                        tutorial.getTopic() != null && 
                        tutorial.getTopic().toLowerCase().contains(topicQuery)) {
                        teachesTopicMatches = true;
                        break;
                    }
                }
                if (!teachesTopicMatches) matches = false;
            }
            
            if (matches) {
                filteredTutors.add(tutor);
            }
        }
        
        updateTutorResults(filteredTutors);
    }
    
    private void updateTutorialResults(List<TutorialSession> tutorials) {
        tutorialAdapter.updateTutorials(tutorials);
        
        if (tutorials.isEmpty()) {
            resultsCard.setVisibility(View.VISIBLE);
            resultsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            resultsCard.setVisibility(View.VISIBLE);
            resultsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
        
        binding.resultsHeader.setText("Tutorial Results (" + tutorials.size() + ")");
    }
    
    private void updateTutorResults(List<TutorProfile> tutors) {
        tutorAdapter.updateTutors(tutors);
        
        if (tutors.isEmpty()) {
            resultsCard.setVisibility(View.VISIBLE);
            resultsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            resultsCard.setVisibility(View.VISIBLE);
            resultsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
        
        binding.resultsHeader.setText("Tutor Results (" + tutors.size() + ")");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}