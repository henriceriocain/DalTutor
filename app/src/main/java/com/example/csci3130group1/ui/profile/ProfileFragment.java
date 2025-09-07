/*package com.example.csci3130group1.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.csci3130group1.LoginActivity;
import com.example.csci3130group1.R;
import com.example.csci3130group1.databinding.FragmentProfileBinding;
import com.example.csci3130group1.ui.profile.ProfileViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        mAuth = FirebaseAuth.getInstance();

        final Button logOutButton = root.findViewById(R.id.logout_button);
        logOutButton.setOnClickListener(view -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            onDestroy();
        });


        loadUserInfoAndRatings();

        return root;
    }

    private void loadUserInfoAndRatings() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews").child(userId);

        // Load profile info
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String role = snapshot.child("role").getValue(String.class);

                if (name != null) binding.profileName.setText(name);
                if (role != null) binding.profileRole.setText("Role: " + role);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Load and calculate average rating
        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float total = 0;
                int count = 0;

                for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                    Float rating = reviewSnap.child("rating").getValue(Float.class);
                    if (rating != null) {
                        total += rating;
                        count++;
                    }
                }

                float average = count > 0 ? total / count : 0;
                String ratingText = count > 0 ?
                        String.format("Average Rating: %.1f ★ (%d reviews)", average, count) :
                        "No ratings yet";

                binding.profileRating.setText(ratingText);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
*/


package com.example.csci3130group1.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.csci3130group1.LoginActivity;
import com.example.csci3130group1.R;
import com.example.csci3130group1.databinding.FragmentProfileBinding;
import com.example.csci3130group1.ui.search_for_tutorials.Tutorial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();

        setupButtonListeners(root);
        loadUserProfile();
        loadTutorialData();

        return root;
    }

    private void setupButtonListeners(View root) {
        Button logOutButton = root.findViewById(R.id.logout_button);
        logOutButton.setOnClickListener(view -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        Button editProfileButton = root.findViewById(R.id.edit_profile_button);
        editProfileButton.setOnClickListener(view -> {
            // TODO: Navigate to edit profile activity/fragment
            Toast.makeText(getContext(), "Edit Profile - Coming Soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        String userEmail = currentUser.getEmail();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews").child(userId);

        // Set email immediately
        if (userEmail != null) {
            binding.profileEmail.setText(userEmail);
        }

        // Load profile info
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String role = snapshot.child("role").getValue(String.class);
                String degree = snapshot.child("degree").getValue(String.class);

                if (name != null) {
                    binding.profileName.setText(name);
                    binding.profileGreeting.setText("Hi, " + name + "!");
                }
                
                if (role != null) {
                    binding.profileRole.setText(role);
                    
                    // Show rating section only for tutors
                    if ("Tutor".equalsIgnoreCase(role)) {
                        binding.profileRating.setVisibility(View.VISIBLE);
                        loadTutorRating(reviewsRef);
                    }
                }
                
                if (degree != null && !degree.trim().isEmpty()) {
                    binding.profileDegree.setText(degree);
                    binding.profileDegree.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadTutorRating(DatabaseReference reviewsRef) {
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

                float average = count > 0 ? total / count : 0;
                String ratingText = count > 0 ?
                        String.format("Average Rating: %.1f ★ (%d reviews)", average, count) :
                        "No ratings yet";

                binding.profileRating.setText(ratingText);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadTutorialData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        DatabaseReference tutorialsRef = FirebaseDatabase.getInstance().getReference("tutorials");
        DatabaseReference registrationsRef = FirebaseDatabase.getInstance().getReference("registrations");

        // Load tutorial statistics and upcoming tutorials
        registrationsRef.orderByChild("studentId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> registeredTutorialIds = new ArrayList<>();
                        
                        for (DataSnapshot regSnap : snapshot.getChildren()) {
                            String tutorialId = regSnap.child("tutorialId").getValue(String.class);
                            if (tutorialId != null) {
                                registeredTutorialIds.add(tutorialId);
                            }
                        }
                        
                        if (!registeredTutorialIds.isEmpty()) {
                            loadTutorialDetails(registeredTutorialIds);
                        } else {
                            binding.tutorialStats.setText("No tutorials registered yet.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        binding.tutorialStats.setText("Error loading tutorial data.");
                    }
                });
    }

    private void loadTutorialDetails(List<String> tutorialIds) {
        DatabaseReference tutorialsRef = FirebaseDatabase.getInstance().getReference("tutorials");
        List<Tutorial> allTutorials = new ArrayList<>();
        List<Tutorial> upcomingTutorials = new ArrayList<>();
        
        final int totalTutorials = tutorialIds.size();
        final int[] loadedCount = {0};

        for (String tutorialId : tutorialIds) {
            tutorialsRef.child(tutorialId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    loadedCount[0]++;
                    
                    Tutorial tutorial = snapshot.getValue(Tutorial.class);
                    if (tutorial != null) {
                        tutorial.setTutorialId(tutorialId);
                        allTutorials.add(tutorial);
                        
                        // Check if tutorial is upcoming
                        if (isTutorialUpcoming(tutorial)) {
                            upcomingTutorials.add(tutorial);
                        }
                    }
                    
                    // When all tutorials are loaded, update UI
                    if (loadedCount[0] == totalTutorials) {
                        updateTutorialSummary(allTutorials);
                        updateUpcomingTutorials(upcomingTutorials);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loadedCount[0]++;
                    if (loadedCount[0] == totalTutorials) {
                        updateTutorialSummary(allTutorials);
                        updateUpcomingTutorials(upcomingTutorials);
                    }
                }
            });
        }
    }

    private boolean isTutorialUpcoming(Tutorial tutorial) {
        if (tutorial.getDate() == null) return false;
        
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date tutorialDate = dateFormat.parse(tutorial.getDate());
            Date currentDate = new Date();
            
            return tutorialDate != null && tutorialDate.after(currentDate);
        } catch (ParseException e) {
            return false;
        }
    }

    private void updateTutorialSummary(List<Tutorial> tutorials) {
        if (tutorials.isEmpty()) {
            binding.tutorialStats.setText("No tutorials registered yet.");
            return;
        }

        int totalTutorials = tutorials.size();
        int upcomingCount = 0;
        int completedCount = 0;

        for (Tutorial tutorial : tutorials) {
            if (isTutorialUpcoming(tutorial)) {
                upcomingCount++;
            } else {
                completedCount++;
            }
        }

        String statsText = String.format(Locale.getDefault(),
                "Total Tutorials: %d\nUpcoming: %d\nCompleted: %d",
                totalTutorials, upcomingCount, completedCount);
        
        binding.tutorialStats.setText(statsText);
    }

    private void updateUpcomingTutorials(List<Tutorial> upcomingTutorials) {
        LinearLayout upcomingCard = binding.getRoot().findViewById(R.id.upcoming_tutorials_card);
        LinearLayout upcomingList = binding.getRoot().findViewById(R.id.upcomingTutorialsList);
        
        if (upcomingTutorials.isEmpty()) {
            upcomingCard.setVisibility(View.GONE);
            return;
        }

        upcomingCard.setVisibility(View.VISIBLE);
        upcomingList.removeAllViews();

        for (Tutorial tutorial : upcomingTutorials) {
            TextView tutorialItem = new TextView(getContext());
            String tutorialText = String.format(Locale.getDefault(),
                    "• %s\n  %s at %s\n  Fee: %s",
                    tutorial.getTutorialName() != null ? tutorial.getTutorialName() : "Unnamed Tutorial",
                    tutorial.getDate() != null ? tutorial.getDate() : "No date",
                    tutorial.getStartTime() != null ? tutorial.getStartTime() : "No time",
                    tutorial.getFee() != null ? tutorial.getFee() : "Free");
            
            tutorialItem.setText(tutorialText);
            tutorialItem.setTextSize(16);
            tutorialItem.setTextColor(getResources().getColor(android.R.color.black, null));
            tutorialItem.setPadding(0, 0, 0, 16);
            
            upcomingList.addView(tutorialItem);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


