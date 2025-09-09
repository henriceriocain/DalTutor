

package com.example.csci3130group1.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;

import com.example.csci3130group1.EditProfileActivity;
import com.example.csci3130group1.LoginActivity;
import com.example.csci3130group1.R;
import com.example.csci3130group1.databinding.FragmentProfileBinding;
import com.example.csci3130group1.ui.search_for_tutorials.Tutorial;
import com.example.csci3130group1.TutorialDetailsActivity;
import com.example.csci3130group1.TutorialHistoryActivity;
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
    private boolean isTutor = false;
    private boolean hostedInTutorDashboard = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();

        // Determine which dashboard hosts this fragment and set initial labels + data
        hostedInTutorDashboard = getActivity() instanceof com.example.csci3130group1.TutorDashboard;

        TextView upcomingHeader = binding.getRoot().findViewById(R.id.upcomingHeaderText);
        TextView summaryHeader = binding.getRoot().findViewById(R.id.summaryHeaderText);
        Button viewAllButton = binding.getRoot().findViewById(R.id.view_tutorials_button);

        if (hostedInTutorDashboard) {
            isTutor = true;
            if (upcomingHeader != null) upcomingHeader.setText("Upcoming Tutorials");
            if (summaryHeader != null) summaryHeader.setText("Tutorial Summary");
            if (viewAllButton != null) viewAllButton.setText("View All Tutorials");
            // Show reviews section for tutors
            View reviewsCard = binding.getRoot().findViewById(R.id.reviews_card);
            if (reviewsCard != null) reviewsCard.setVisibility(View.VISIBLE);
            loadTutorData();
            loadOwnTutorReviews();
        } else {
            isTutor = false;
            if (upcomingHeader != null) upcomingHeader.setText("Upcoming Registrations");
            if (summaryHeader != null) summaryHeader.setText("Registration Summary");
            if (viewAllButton != null) viewAllButton.setText("View All Registrations");
            // Hide reviews in student dashboard profile
            View reviewsCard = binding.getRoot().findViewById(R.id.reviews_card);
            if (reviewsCard != null) reviewsCard.setVisibility(View.GONE);
            loadTutorialData();
        }

        setupButtonListeners(root);
        // Then load profile/role info, which may fine-tune tutor vs student
        loadUserProfile();

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
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        Button viewTutorialsButton = root.findViewById(R.id.view_tutorials_button);
        viewTutorialsButton.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), TutorialHistoryActivity.class);
            if (isTutor || hostedInTutorDashboard) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    intent.putExtra("isTutorView", true);
                    intent.putExtra("tutorId", currentUser.getUid());
                }
            }
            startActivity(intent);
        });

        Button communityActivityButton = root.findViewById(R.id.community_activity_button);
        communityActivityButton.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), com.example.csci3130group1.ui.community.CommunityActivity.class);
            startActivity(intent);
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
                String description = snapshot.child("description").getValue(String.class);
                String profilePictureUrl = snapshot.child("profilePictureUrl").getValue(String.class);
                String contactNumber = snapshot.child("contact").getValue(String.class);

                if (name != null) {
                    binding.profileName.setText(name);
                    binding.profileGreeting.setText("Hi, " + name + "!");
                }
                
                // Load profile picture
                ImageView profilePicture = binding.getRoot().findViewById(R.id.profilePicture);
                if (profilePictureUrl != null && !profilePictureUrl.isEmpty() && profilePicture != null) {
                    Glide.with(ProfileFragment.this)
                        .load(profilePictureUrl)
                        .circleCrop()
                        .placeholder(R.drawable.circle_background)
                        .error(R.drawable.circle_background)
                        .into(profilePicture);
                }
                
                if (role != null) {
                    binding.profileRole.setText(role);
                }

                // Determine final perspective: honor TutorDashboard host even if role is missing/different
                isTutor = hostedInTutorDashboard || (role != null && "Tutor".equalsIgnoreCase(role));

                // Show rating section only for tutors
                if (isTutor) {
                    LinearLayout ratingContainer = binding.getRoot().findViewById(R.id.profileRating).getParent() instanceof LinearLayout ?
                            (LinearLayout) binding.getRoot().findViewById(R.id.profileRating).getParent() : null;
                    if (ratingContainer != null) {
                        ratingContainer.setVisibility(View.VISIBLE);
                    }
                    loadTutorRating(reviewsRef);
                }

                // Update labels for tutor/student perspective and load data accordingly
                TextView upcomingHeader = binding.getRoot().findViewById(R.id.upcomingHeaderText);
                TextView summaryHeader = binding.getRoot().findViewById(R.id.summaryHeaderText);
                Button viewAllButton = binding.getRoot().findViewById(R.id.view_tutorials_button);
                if (isTutor) {
                    if (upcomingHeader != null) upcomingHeader.setText("Upcoming Tutorials");
                    if (summaryHeader != null) summaryHeader.setText("Tutorial Summary");
                    if (viewAllButton != null) viewAllButton.setText("View All Tutorials");
                    View reviewsCard = binding.getRoot().findViewById(R.id.reviews_card);
                    if (reviewsCard != null) reviewsCard.setVisibility(View.VISIBLE);
                    loadTutorData();
                    loadOwnTutorReviews();
                } else {
                    if (upcomingHeader != null) upcomingHeader.setText("Upcoming Registrations");
                    if (summaryHeader != null) summaryHeader.setText("Registration Summary");
                    if (viewAllButton != null) viewAllButton.setText("View All Registrations");
                    View reviewsCard = binding.getRoot().findViewById(R.id.reviews_card);
                    if (reviewsCard != null) reviewsCard.setVisibility(View.GONE);
                    loadTutorialData();
                }
                
                if (degree != null && !degree.trim().isEmpty()) {
                    binding.profileDegree.setText(degree);
                    binding.profileDegree.setVisibility(View.VISIBLE);
                }
                
                if (description != null && !description.trim().isEmpty()) {
                    binding.profileStudentDescription.setText(description);
                    LinearLayout descriptionSection = binding.getRoot().findViewById(R.id.descriptionSection);
                    descriptionSection.setVisibility(View.VISIBLE);
                }
                
                // Set contact number (required field, so should always be present)
                TextView profileContact = binding.getRoot().findViewById(R.id.profileContact);
                LinearLayout contactContainer = binding.getRoot().findViewById(R.id.contactContainer);
                if (profileContact != null && contactContainer != null) {
                    if (contactNumber != null && !contactNumber.trim().isEmpty()) {
                        profileContact.setText(contactNumber);
                        contactContainer.setVisibility(View.VISIBLE);
                    } else {
                        // Fallback for existing users who might not have contact number yet
                        profileContact.setText("Not provided");
                        contactContainer.setVisibility(View.VISIBLE);
                    }
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

    private void loadOwnTutorReviews() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String tutorId = currentUser.getUid();
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews").child(tutorId);

        LinearLayout reviewsList = binding.getRoot().findViewById(R.id.profileReviewsList);
        TextView noReviewsText = binding.getRoot().findViewById(R.id.profileNoReviewsText);
        Button seeAllReviewsButton = binding.getRoot().findViewById(R.id.seeAllReviewsButton);
        TextView reviewsHeader = binding.getRoot().findViewById(R.id.reviewsHeaderText);
        if (reviewsList == null) return;

        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewsList.removeAllViews();

                if (noReviewsText != null) noReviewsText.setVisibility(View.GONE);
                if (seeAllReviewsButton != null) seeAllReviewsButton.setVisibility(View.GONE);

                long count = snapshot.getChildrenCount();
                if (reviewsHeader != null && getContext() != null) {
                    reviewsHeader.setText(getString(R.string.reviews_count, count));
                }

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    if (noReviewsText != null) noReviewsText.setVisibility(View.VISIBLE);
                    return;
                }

                // Show 'See all reviews' when there is at least one
                if (seeAllReviewsButton != null) {
                    seeAllReviewsButton.setVisibility(View.VISIBLE);
                    seeAllReviewsButton.setOnClickListener(v -> {
                        Intent intent = new Intent(getActivity(), com.example.csci3130group1.TutorProfileActivity.class);
                        intent.putExtra("tutorId", tutorId);
                        intent.putExtra("readOnly", true);
                        startActivity(intent);
                    });
                }

                for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                    String reviewerName = reviewSnap.child("reviewerName").getValue(String.class);
                    String reviewText = reviewSnap.child("reviewText").getValue(String.class);
                    Double rating = reviewSnap.child("rating").getValue(Double.class);
                    String timestamp = reviewSnap.child("timestamp").getValue(String.class);

                    addReviewToList(reviewsList, reviewerName, reviewText, rating != null ? rating.floatValue() : 0f, timestamp);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (noReviewsText != null) noReviewsText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addReviewToList(LinearLayout container, String reviewerName, String reviewText, float rating, String timestamp) {
        View reviewView = getLayoutInflater().inflate(R.layout.review_item, container, false);
        TextView reviewerNameView = reviewView.findViewById(R.id.reviewerName);
        TextView reviewTextView = reviewView.findViewById(R.id.reviewText);
        TextView reviewRatingView = reviewView.findViewById(R.id.reviewRating);
        TextView reviewTimestampView = reviewView.findViewById(R.id.reviewTimestamp);

        reviewerNameView.setText(reviewerName != null ? reviewerName : "Anonymous");
        reviewTextView.setText(reviewText != null ? reviewText : "");
        reviewRatingView.setText(String.format(Locale.getDefault(), "%.1f ★", rating));
        if (timestamp != null && !timestamp.isEmpty()) {
            reviewTimestampView.setText(timestamp);
            reviewTimestampView.setVisibility(View.VISIBLE);
        } else {
            reviewTimestampView.setVisibility(View.GONE);
        }

        container.addView(reviewView);
    }

    private void loadTutorialData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        DatabaseReference userRegistrationsRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("registrations");

        // Load tutorial statistics and upcoming tutorials from user's registrations
        userRegistrationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() == 0) {
                    binding.tutorialStats.setText("No registrations yet.");
                    return;
                }

                List<String> registrationIds = new ArrayList<>();
                
                // Get all registration IDs from user's registrations subcollection
                for (DataSnapshot regSnap : snapshot.getChildren()) {
                    String registrationId = regSnap.getKey();
                    if (registrationId != null) {
                        registrationIds.add(registrationId);
                    }
                }
                
                // Now load all registration details in parallel
                loadAllRegistrationDetails(registrationIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.tutorialStats.setText("Error loading registration data.");
            }
        });
    }

    

    private void loadTutorData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        DatabaseReference tutorialSessionsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");

        tutorialSessionsRef.orderByChild("tutorId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Tutorial> allTutorials = new ArrayList<>();
                        List<Tutorial> upcomingTutorials = new ArrayList<>();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String tutorialId = child.getKey();

                            String tutorialName = child.child("tutorialName").getValue(String.class);
                            String topic = child.child("topic").getValue(String.class);
                            String fee = child.child("fee").getValue(String.class);
                            String date = child.child("date").getValue(String.class);
                            String startTime = child.child("startTime").getValue(String.class);
                            String endTime = child.child("endTime").getValue(String.class);
                            String address = child.child("address").getValue(String.class);
                            String tutorName = child.child("tutorName").getValue(String.class);
                            String description = child.child("description").getValue(String.class);

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
                            if (tutorialId != null) tutorial.setTutorialId(tutorialId);

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
                        binding.tutorialStats.setText("Error loading tutorial data.");
                    }
                });
    }

    private void loadAllRegistrationDetails(List<String> registrationIds) {
        List<String> registeredTutorialIds = new ArrayList<>();
        final int totalRegistrations = registrationIds.size();
        final int[] loadedCount = {0};

        for (String registrationId : registrationIds) {
            DatabaseReference registrationRef = FirebaseDatabase.getInstance()
                    .getReference("registrations").child(registrationId);
            
            registrationRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    loadedCount[0]++;
                    
                    String tutorialId = snapshot.child("tutorialId").getValue(String.class);
                    if (tutorialId != null) {
                        registeredTutorialIds.add(tutorialId);
                    }
                    
                    // When all registrations are loaded, load tutorial details
                    if (loadedCount[0] == totalRegistrations) {
                        if (!registeredTutorialIds.isEmpty()) {
                            loadTutorialDetails(registeredTutorialIds);
                        } else {
                            binding.tutorialStats.setText("No valid tutorials found.");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loadedCount[0]++;
                    if (loadedCount[0] == totalRegistrations) {
                        if (!registeredTutorialIds.isEmpty()) {
                            loadTutorialDetails(registeredTutorialIds);
                        } else {
                            binding.tutorialStats.setText("Error loading some tutorial data.");
                        }
                    }
                }
            });
        }
    }

    private void loadTutorialDetails(List<String> tutorialIds) {
        DatabaseReference tutorialSessionsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        List<Tutorial> allTutorials = new ArrayList<>();
        List<Tutorial> upcomingTutorials = new ArrayList<>();
        
        final int totalTutorials = tutorialIds.size();
        final int[] loadedCount = {0};

        for (String tutorialId : tutorialIds) {
            tutorialSessionsRef.child(tutorialId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    loadedCount[0]++;
                    
                    if (snapshot.exists()) {
                        // Create Tutorial object from tutorial_sessions data
                        Tutorial tutorial = new Tutorial();
                        tutorial.setTutorialId(tutorialId);
                        
                        // Map tutorial_sessions fields to Tutorial object
                        String tutorialName = snapshot.child("tutorialName").getValue(String.class);
                        String topic = snapshot.child("topic").getValue(String.class);
                        String fee = snapshot.child("fee").getValue(String.class);
                        String date = snapshot.child("date").getValue(String.class);
                        String startTime = snapshot.child("startTime").getValue(String.class);
                        String endTime = snapshot.child("endTime").getValue(String.class);
                        String address = snapshot.child("address").getValue(String.class);
                        String tutorName = snapshot.child("tutorName").getValue(String.class);
                        
                        // Set the fields (using reflection or creating a proper constructor)
                        // Since Tutorial class might not have setters, we'll create a new constructor call
                        // For now, let's create a simple tutorial with available data
                        tutorial = new Tutorial(
                            tutorialName != null ? tutorialName : "Unknown Tutorial",
                            topic != null ? topic : "General",
                            fee != null ? fee : "Free",
                            date != null ? date : "TBD",
                            startTime != null ? startTime : "TBD",
                            endTime != null ? endTime : "TBD",
                            "No description available",
                            address != null ? address : "Location TBD",
                            0.0, 0.0, "",
                            tutorName != null ? tutorName : "Unknown Tutor",
                            "",  // tutorId
                            ""   // tutorDegree
                        );
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
            // Try the format used in Firebase: "Sep 09, 2025"
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date tutorialDate = dateFormat.parse(tutorial.getDate());
            Date currentDate = new Date();
            
            return tutorialDate != null && tutorialDate.after(currentDate);
        } catch (ParseException e) {
            // If that fails, try the old format
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
            binding.tutorialStats.setText(isTutor ? "No tutorials yet." : "No tutorials registered yet.");
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
                (isTutor ? "Total Tutorials: %d\n" : "Total Registrations: %d\n") +
                "Upcoming: %d\nCompleted: %d",
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
            // Inflate the tutorial card component
            View tutorialCardView = getLayoutInflater().inflate(R.layout.tutorial_card_item, upcomingList, false);
            
            // Get references to the views in the card
            TextView tutorialName = tutorialCardView.findViewById(R.id.tutorialCardName);
            TextView tutorialFee = tutorialCardView.findViewById(R.id.tutorialCardFee);
            TextView tutorialTutor = tutorialCardView.findViewById(R.id.tutorialCardTutor);
            TextView tutorialDateTime = tutorialCardView.findViewById(R.id.tutorialCardDateTime);
            TextView tutorialLocation = tutorialCardView.findViewById(R.id.tutorialCardLocation);
            
            // Set the tutorial data
            tutorialName.setText(tutorial.getTutorialName() != null ? tutorial.getTutorialName() : "Unnamed Tutorial");
            tutorialFee.setText(tutorial.getFee() != null ? "$" + tutorial.getFee() : "Free");
            tutorialTutor.setText(tutorial.getTutorName() != null ? tutorial.getTutorName() : "Unknown Tutor");
            
            // Format date and time
            String dateTime = String.format(Locale.getDefault(), "%s at %s - %s",
                    tutorial.getDate() != null ? tutorial.getDate() : "No date",
                    tutorial.getStartTime() != null ? tutorial.getStartTime() : "TBD",
                    tutorial.getEndTime() != null ? tutorial.getEndTime() : "TBD");
            tutorialDateTime.setText(dateTime);
            
            tutorialLocation.setText(tutorial.getAddress() != null ? tutorial.getAddress() : "Location TBD");
            
            // Set click listener to navigate to tutorial details
            tutorialCardView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), TutorialDetailsActivity.class);
                intent.putExtra("tutorialId", tutorial.getTutorialId());
                intent.putExtra("tutorialName", tutorial.getTutorialName());
                intent.putExtra("tutorName", tutorial.getTutorName());
                intent.putExtra("fee", tutorial.getFee());
                intent.putExtra("date", tutorial.getDate());
                intent.putExtra("startTime", tutorial.getStartTime());
                intent.putExtra("endTime", tutorial.getEndTime());
                intent.putExtra("address", tutorial.getAddress());
                intent.putExtra("isAlreadyRegistered", true);
                startActivity(intent);
            });
            
            upcomingList.addView(tutorialCardView);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh profile and list when returning
        if (binding != null) {
            loadUserProfile();
            if (hostedInTutorDashboard || isTutor) {
                loadTutorData();
                loadOwnTutorReviews();
            } else {
                loadTutorialData();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
