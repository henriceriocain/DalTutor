

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
import com.example.csci3130group1.utils.TutorialSummaryHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    
    // Flag to track if fragment is destroyed to prevent null pointer exceptions
    private volatile boolean isFragmentDestroyed = false;
    private boolean isTutor = false;
    private int reviewsLoadVersion = 0;
    private com.google.android.material.switchmaterial.SwitchMaterial switchEnableTutorTools;
    private View btnCreateTutorial;
    // Combined summary state
    private boolean tutorToolsEnabled = false;
    private int regTotal = 0, regUpcoming = 0, regCompleted = 0;
    private int tutTotal = 0, tutUpcoming = 0, tutCompleted = 0;
    private boolean regLoaded = false, tutLoaded = false;
    private boolean regUpcomingLoaded = false, tutUpcomingLoaded = false;
    private List<Tutorial> cachedRegUpcoming = new ArrayList<>();
    private List<Tutorial> cachedTutUpcoming = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Reset destruction flag when creating new view
        isFragmentDestroyed = false;

        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();

        // Single dashboard model; no tutor-specific host

        // Always initialize student-facing labels and data
        TextView upcomingHeader = binding.getRoot().findViewById(R.id.upcomingHeaderText);
        TextView summaryHeader = binding.getRoot().findViewById(R.id.summaryHeaderText);
        if (upcomingHeader != null) upcomingHeader.setText("Upcoming Registrations");
        if (summaryHeader != null) summaryHeader.setText("Registration Summary");
        // Reviews card hidden by default; will be shown if tutor tools/role detected later
        View reviewsCard = binding.getRoot().findViewById(R.id.reviews_card);
        if (reviewsCard != null) reviewsCard.setVisibility(View.GONE);
        // Load student registrations always
        loadTutorialData();

        setupButtonListeners(root);
        // Then load profile/role info; will also show tutor-specific cards if enabled
        loadUserProfile();

        // Setup tutor tools toggles
        switchEnableTutorTools = root.findViewById(R.id.switchEnableTutorTools);
        btnCreateTutorial = root.findViewById(R.id.btnCreateTutorial);
        if (switchEnableTutorTools != null) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String uid = currentUser.getUid();
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        safeUpdateUI(() -> {
                            Boolean isTutorEnabled = snapshot.child("isTutorEnabled").getValue(Boolean.class);
                            tutorToolsEnabled = isTutorEnabled != null && isTutorEnabled;
                            if (switchEnableTutorTools != null)
                                switchEnableTutorTools.setChecked(isTutorEnabled != null && isTutorEnabled);
                            if (btnCreateTutorial != null) {
                                btnCreateTutorial.setVisibility(isTutorEnabled != null && isTutorEnabled ? View.VISIBLE : View.GONE);
                            }
                            // Show/hide tutor cards
                            View tutorUpcomingCard = binding.getRoot().findViewById(R.id.tutor_upcoming_tutorials_card);
                            View tutorSummaryCard = binding.getRoot().findViewById(R.id.tutor_summary_card);
                            boolean showTutorCards = tutorToolsEnabled;
                            if (tutorUpcomingCard != null) tutorUpcomingCard.setVisibility(showTutorCards ? View.VISIBLE : View.GONE);
                            if (tutorSummaryCard != null) tutorSummaryCard.setVisibility(showTutorCards ? View.VISIBLE : View.GONE);
                            // Also control rating container and reviews card with the same toggle
                            View ratingView = binding.getRoot().findViewById(R.id.profileRating);
                            LinearLayout ratingContainer = null;
                            if (ratingView != null) {
                                View parent = (View) ratingView.getParent();
                                if (parent instanceof LinearLayout) ratingContainer = (LinearLayout) parent;
                            }
                            if (ratingContainer != null) ratingContainer.setVisibility(showTutorCards ? View.VISIBLE : View.GONE);
                            View reviewsCardInit = binding.getRoot().findViewById(R.id.reviews_card);
                            if (reviewsCardInit != null) reviewsCardInit.setVisibility(showTutorCards ? View.VISIBLE : View.GONE);
                            if (showTutorCards) {
                                loadTutorDataSecondary();
                                loadOwnTutorReviews();
                            }
                            maybeUpdateCombinedCard();
                        });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

                switchEnableTutorTools.setOnCheckedChangeListener((btn, checked) -> {
                    safeUpdateUI(() -> {
                        FirebaseDatabase.getInstance().getReference("users").child(uid).child("isTutorEnabled").setValue(checked);
                        tutorToolsEnabled = checked;
                        if (btnCreateTutorial != null) btnCreateTutorial.setVisibility(checked ? View.VISIBLE : View.GONE);
                        View tutorUpcomingCard = binding.getRoot().findViewById(R.id.tutor_upcoming_tutorials_card);
                        View tutorSummaryCard = binding.getRoot().findViewById(R.id.tutor_summary_card);
                        if (tutorUpcomingCard != null) tutorUpcomingCard.setVisibility(checked ? View.VISIBLE : View.GONE);
                        if (tutorSummaryCard != null) tutorSummaryCard.setVisibility(checked ? View.VISIBLE : View.GONE);
                        View ratingView = binding.getRoot().findViewById(R.id.profileRating);
                        LinearLayout ratingContainer = null;
                        if (ratingView != null) {
                            View parent = (View) ratingView.getParent();
                            if (parent instanceof LinearLayout) ratingContainer = (LinearLayout) parent;
                        }
                        if (ratingContainer != null) ratingContainer.setVisibility(checked ? View.VISIBLE : View.GONE);
                        View reviewsCardLocal = binding.getRoot().findViewById(R.id.reviews_card);
                        if (reviewsCardLocal != null) reviewsCardLocal.setVisibility(checked ? View.VISIBLE : View.GONE);
                        if (checked) loadTutorDataSecondary();
                        maybeUpdateCombinedCard();
                    });
                });

                if (btnCreateTutorial != null) {
                    btnCreateTutorial.setOnClickListener(v -> {
                        // Launch TutorialManagementActivity
                        Intent intent = new Intent(getActivity(), com.example.csci3130group1.TutorialManagementActivity.class);
                        startActivity(intent);
                    });
                }
            }
        }

        return root;
    }

    /**
     * Safe method to check if fragment is still alive and binding is available
     * @return true if it's safe to update UI, false otherwise
     */
    private boolean isFragmentAlive() {
        return !isFragmentDestroyed && binding != null && isAdded() && getContext() != null;
    }

    /**
     * Safe method to execute UI updates that checks fragment lifecycle
     * @param uiUpdate Runnable containing UI update code
     */
    private void safeUpdateUI(Runnable uiUpdate) {
        if (isFragmentAlive()) {
            try {
                uiUpdate.run();
            } catch (Exception e) {
                // Log error but don't crash
                android.util.Log.w("ProfileFragment", "UI update failed: " + e.getMessage());
            }
        }
    }

    private void setupButtonListeners(View root) {
        View logOutButton = root.findViewById(R.id.logout_link);
        if (logOutButton != null) logOutButton.setOnClickListener(view -> {
            mAuth.signOut();
            com.example.csci3130group1.utils.SessionRole.clear(requireContext());
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        View editProfileButton = root.findViewById(R.id.edit_profile_link);
        if (editProfileButton != null) editProfileButton.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        View viewRegistrationsLink = root.findViewById(R.id.view_tutorials_button);
        if (viewRegistrationsLink != null) viewRegistrationsLink.setOnClickListener(view -> {
            // Always open Registration History (student view)
            Intent intent = new Intent(getActivity(), TutorialHistoryActivity.class);
            startActivity(intent);
        });

        // Removed Community Activity button (available in tab)

        // Tutor: View all authored tutorials
        View viewAllTutorTutorials = root.findViewById(R.id.view_all_tutorials_button);
        if (viewAllTutorTutorials != null) {
            viewAllTutorTutorials.setOnClickListener(v -> {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    Intent intent = new Intent(getActivity(), TutorialHistoryActivity.class);
                    intent.putExtra("isTutorView", true);
                    intent.putExtra("tutorId", currentUser.getUid());
                    startActivity(intent);
                }
            });
        }
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
                safeUpdateUI(() -> {
                    String name = snapshot.child("name").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);
                    String degree = snapshot.child("degree").getValue(String.class);
                    String description = snapshot.child("description").getValue(String.class);
                    Boolean isTutorEnabledFlag = snapshot.child("isTutorEnabled").getValue(Boolean.class);
                    String profilePictureUrl = snapshot.child("profilePictureUrl").getValue(String.class);
                    String contactNumber = snapshot.child("contact").getValue(String.class);

                    if (name != null) {
                        binding.profileName.setText(name);
                    }
                    // Keep a consistent, concise welcome header (no name for long-name safety)
                    binding.profileGreeting.setText("Welcome back");
                
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
                // Role label removed from UI; no need to set text

                // Determine final perspective: prefer isTutorEnabled flag, then session/host/db
                boolean enabled = isTutorEnabledFlag != null && isTutorEnabledFlag;
                tutorToolsEnabled = enabled;

                // Drive tutor cards from the flag
                View tutorUpcomingCard = binding.getRoot().findViewById(R.id.tutor_upcoming_tutorials_card);
                View tutorSummaryCard  = binding.getRoot().findViewById(R.id.tutor_summary_card);
                if (tutorUpcomingCard != null) tutorUpcomingCard.setVisibility(enabled ? View.VISIBLE : View.GONE);
                if (tutorSummaryCard  != null) tutorSummaryCard.setVisibility(enabled ? View.VISIBLE : View.GONE);

                // Also load tutor data / reviews only when enabled
                if (enabled) {
                    loadTutorDataSecondary();
                    loadOwnTutorReviews();
                }
                if (enabled) {
                    isTutor = true;
                } else {
                    com.example.csci3130group1.utils.SessionRole.Role sess = com.example.csci3130group1.utils.SessionRole.get(requireContext());
                    if (sess != com.example.csci3130group1.utils.SessionRole.Role.UNKNOWN) {
                        isTutor = (sess == com.example.csci3130group1.utils.SessionRole.Role.TUTOR);
                    } else {
                        isTutor = (role != null && "Tutor".equalsIgnoreCase(role));
                    }
                }

                // Show rating section only when Tutor Tools are enabled
                if (isTutorEnabledFlag != null && isTutorEnabledFlag) {
                    View ratingView = binding.getRoot().findViewById(R.id.profileRating);
                    LinearLayout ratingContainer = null;
                    if (ratingView != null) {
                        View parent = (View) ratingView.getParent();
                        if (parent instanceof LinearLayout) ratingContainer = (LinearLayout) parent;
                    }
                    if (ratingContainer != null) {
                        ratingContainer.setVisibility(View.VISIBLE);
                    }
                    loadTutorRating(reviewsRef);
                } else {
                    // Ensure rating container and reviews card are hidden when disabled
                    View ratingView = binding.getRoot().findViewById(R.id.profileRating);
                    LinearLayout ratingContainer = null;
                    if (ratingView != null) {
                        View parent = (View) ratingView.getParent();
                        if (parent instanceof LinearLayout) ratingContainer = (LinearLayout) parent;
                    }
                    if (ratingContainer != null) ratingContainer.setVisibility(View.GONE);
                    View reviewsCard = binding.getRoot().findViewById(R.id.reviews_card);
                    if (reviewsCard != null) reviewsCard.setVisibility(View.GONE);
                }

                // Update labels for tutor/student perspective and load data accordingly
                TextView upcomingHeader = binding.getRoot().findViewById(R.id.upcomingHeaderText);
                TextView summaryHeader = binding.getRoot().findViewById(R.id.summaryHeaderText);
                // Reviews card visibility strictly tied to Tutor Tools enablement
                View reviewsCard2 = binding.getRoot().findViewById(R.id.reviews_card);
                boolean tutorToolsOnForReviews = isTutorEnabledFlag != null && isTutorEnabledFlag;
                if (reviewsCard2 != null) reviewsCard2.setVisibility(tutorToolsOnForReviews ? View.VISIBLE : View.GONE);
                if (tutorToolsOnForReviews) {
                    loadOwnTutorReviews();
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
                        profileContact.setText(formatPhoneNumber(contactNumber));
                        contactContainer.setVisibility(View.VISIBLE);
                    } else {
                        // Fallback for existing users who might not have contact number yet
                        profileContact.setText("Not provided");
                        contactContainer.setVisibility(View.VISIBLE);
                    }
                }
                    maybeUpdateCombinedCard();
                });
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

                safeUpdateUI(() -> binding.profileRating.setText(ratingText));
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
        final int loadVersion = ++reviewsLoadVersion;
        final Set<String> addedIds = new HashSet<>();

        LinearLayout reviewsList = binding.getRoot().findViewById(R.id.profileReviewsList);
        TextView noReviewsText = binding.getRoot().findViewById(R.id.profileNoReviewsText);
        View seeAllReviewsButton = binding.getRoot().findViewById(R.id.seeAllReviewsButton);
        TextView reviewsHeader = binding.getRoot().findViewById(R.id.reviewsHeaderText);
        if (reviewsList == null) return;

        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isFragmentAlive()) return;
                if (loadVersion != reviewsLoadVersion) return;
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
                        Intent intent = new Intent(getActivity(), com.example.csci3130group1.ReviewsActivity.class);
                        intent.putExtra("tutorId", tutorId);
                        startActivity(intent);
                    });
                }

                for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                    final String reviewId = reviewSnap.getKey();
                    // Support both legacy and current shapes
                    String reviewerName = reviewSnap.child("reviewerName").getValue(String.class);
                    String reviewText = reviewSnap.child("reviewText").getValue(String.class);
                    if (reviewText == null || reviewText.isEmpty()) {
                        reviewText = reviewSnap.child("text").getValue(String.class);
                    }
                    Double rating = reviewSnap.child("rating").getValue(Double.class);

                    String timestampText = null;
                    Object tsObj = reviewSnap.child("timestamp").getValue();
                    if (tsObj != null) {
                        try {
                            long ts;
                            if (tsObj instanceof Number) {
                                ts = ((Number) tsObj).longValue();
                            } else {
                                ts = Long.parseLong(String.valueOf(tsObj));
                            }
                            java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT);
                            timestampText = df.format(new java.util.Date(ts));
                        } catch (Exception ignored) {}
                    }

                    String fromUserId = reviewSnap.child("fromUser").getValue(String.class);
                    if (reviewerName != null && !reviewerName.isEmpty()) {
                        if (loadVersion == reviewsLoadVersion && addedIds.add(reviewId)) {
                            addReviewToList(reviewsList, reviewerName, reviewText, rating != null ? rating.floatValue() : 0f, timestampText, fromUserId);
                        }
                    } else {
                        // Fall back to looking up the reviewer's display name from users/{fromUser}
                        if (fromUserId != null && !fromUserId.isEmpty()) {
                            final String reviewTextFinal = reviewText;
                            final float ratingFinal = rating != null ? rating.floatValue() : 0f;
                            final String timestampTextFinal = timestampText;
                            final String fromUserIdFinal = fromUserId;
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(fromUserId);
                            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    String name = userSnap.child("name").getValue(String.class);
                                    if (loadVersion != reviewsLoadVersion) return;
                                    if (name == null || name.isEmpty()) {
                                        String email = userSnap.child("email").getValue(String.class);
                                        name = email != null ? email : "Anonymous";
                                    }
                                    if (addedIds.add(reviewId)) {
                                        addReviewToList(reviewsList, name, reviewTextFinal, ratingFinal, timestampTextFinal, fromUserIdFinal);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    if (loadVersion != reviewsLoadVersion) return;
                                    if (addedIds.add(reviewId)) {
                                        addReviewToList(reviewsList, "Anonymous", reviewTextFinal, ratingFinal, timestampTextFinal, fromUserIdFinal);
                                    }
                                }
                            });
                        } else {
                            if (loadVersion == reviewsLoadVersion && addedIds.add(reviewId)) {
                                addReviewToList(reviewsList, "Anonymous", reviewText, rating != null ? rating.floatValue() : 0f, timestampText, null);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (noReviewsText != null) noReviewsText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addReviewToList(LinearLayout container, String reviewerName, String reviewText, float rating, String timestamp, String fromUserId) {
        View reviewView = getLayoutInflater().inflate(R.layout.review_item, container, false);
        
        // Use the elegant ReviewItemBinder utility
        com.example.csci3130group1.utils.ReviewItemBinder.bindReviewItemLegacy(
            reviewView, reviewerName, reviewText, rating, timestamp, fromUserId, getContext());

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
                if (!isFragmentAlive()) return;
                if (snapshot.getChildrenCount() == 0) {
                    safeUpdateUI(() -> binding.tutorialStats.setText("No registrations yet."));
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
                if (!isFragmentAlive()) return;
                safeUpdateUI(() -> binding.tutorialStats.setText("Error loading registration data."));
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
                            if (TutorialSummaryHelper.isTutorialUpcoming(tutorial)) {
                                upcomingTutorials.add(tutorial);
                            }
                        }

                        updateTutorialSummary(allTutorials);
                        updateUpcomingTutorials(upcomingTutorials);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isFragmentAlive()) return;
                        safeUpdateUI(() -> binding.tutorialStats.setText("Error loading tutorial data."));
                    }
                });
    }

    // Secondary load for tutor-specific cards (keeps student registrations visible)
    private void loadTutorDataSecondary() {
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
                            if (TutorialSummaryHelper.isTutorialUpcoming(tutorial)) {
                                upcomingTutorials.add(tutorial);
                            }
                        }

                        // We need to update tutor-specific cards manually since they have different IDs
                        cachedTutUpcoming = new ArrayList<>(upcomingTutorials);
                        updateTutorSummary(allTutorials);
                        updateTutorUpcomingTutorials(upcomingTutorials);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (!isFragmentAlive()) return;
                        safeUpdateUI(() -> {
                            TextView tv = binding.getRoot().findViewById(R.id.tutorTutorialStats);
                            if (tv != null) tv.setText("Error loading tutorial data.");
                        });
                    }
                });
    }

    private void updateTutorSummary(List<Tutorial> tutorials) {
        if (!isFragmentAlive()) return;
        
        safeUpdateUI(() -> {
            TextView tv = binding.getRoot().findViewById(R.id.tutorTutorialStats);
            if (tv == null) return;
        if (tutorials.isEmpty()) {
            tv.setText("No tutorials yet.");
            tutTotal = tutUpcoming = tutCompleted = 0;
            tutLoaded = true;
            maybeUpdateCombinedCard();
            return;
        }

        int total = tutorials.size();
        int upcoming = 0;
        int completed = 0;
        for (Tutorial t : tutorials) {
            if (TutorialSummaryHelper.isTutorialUpcoming(t)) upcoming++; else completed++;
        }
        // Modern stats row for tutorials if available; otherwise fallback to legacy text
        View tutorStatsRow = binding.getRoot().findViewById(R.id.tutorStatsRow);
        TextView tutorTotalVal = binding.getRoot().findViewById(R.id.tutorTotalValue);
        TextView tutorUpcomingVal = binding.getRoot().findViewById(R.id.tutorUpcomingValue);
        TextView tutorCompletedVal = binding.getRoot().findViewById(R.id.tutorCompletedValue);
        if (tutorStatsRow != null && tutorTotalVal != null && tutorUpcomingVal != null && tutorCompletedVal != null) {
            tutorStatsRow.setVisibility(View.VISIBLE);
            tv.setVisibility(View.GONE);
            tutorTotalVal.setText(String.valueOf(total));
            tutorUpcomingVal.setText(String.valueOf(upcoming));
            tutorCompletedVal.setText(String.valueOf(completed));
        } else {
            String statsText = String.format(Locale.getDefault(), "Total Tutorials: %d\nUpcoming: %d\nCompleted: %d", total, upcoming, completed);
            tv.setText(statsText);
        }
            tutTotal = total; tutUpcoming = upcoming; tutCompleted = completed; tutLoaded = true;
            maybeUpdateCombinedCard();
        });
    }

    private void updateTutorUpcomingTutorials(List<Tutorial> upcomingTutorials) {
        if (!isFragmentAlive()) return;
        
        safeUpdateUI(() -> {
            LinearLayout upcomingCard = binding.getRoot().findViewById(R.id.tutor_upcoming_tutorials_card);
            LinearLayout upcomingList = binding.getRoot().findViewById(R.id.tutorUpcomingTutorialsList);
            if (upcomingCard == null || upcomingList == null) return;

        if (upcomingTutorials.isEmpty()) {
            upcomingCard.setVisibility(View.GONE);
            tutUpcoming = 0;
            tutUpcomingLoaded = true;
            maybeUpdateCombinedUpcomingCard();
            return;
        }
        upcomingCard.setVisibility(View.VISIBLE);
        upcomingList.removeAllViews();
        tutUpcoming = upcomingTutorials.size();
        tutUpcomingLoaded = true;
        maybeUpdateCombinedUpcomingCard();

        for (Tutorial tutorial : upcomingTutorials) {
            View tutorialCardView = getLayoutInflater().inflate(R.layout.tutorial_card_item, upcomingList, false);
            TextView tutorialName = tutorialCardView.findViewById(R.id.tutorialCardName);
            TextView tutorialTutor = tutorialCardView.findViewById(R.id.tutorialCardTutor);
            TextView tutorialDateTime = tutorialCardView.findViewById(R.id.tutorialCardDateTime);
            TextView tutorialLocation = tutorialCardView.findViewById(R.id.tutorialCardLocation);

            tutorialName.setText(tutorial.getTutorialName() != null ? tutorial.getTutorialName() : "Unnamed Tutorial");
            tutorialTutor.setText(tutorial.getTutorName() != null ? tutorial.getTutorName() : "Unknown Tutor");
            String dateTime = String.format(Locale.getDefault(), "%s at %s - %s",
                    tutorial.getDate() != null ? tutorial.getDate() : "No date",
                    tutorial.getStartTime() != null ? tutorial.getStartTime() : "TBD",
                    tutorial.getEndTime() != null ? tutorial.getEndTime() : "TBD");
            tutorialDateTime.setText(dateTime);
            tutorialLocation.setText(tutorial.getAddress() != null ? tutorial.getAddress() : "Location TBD");

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
                startActivity(intent);
            });

            upcomingList.addView(tutorialCardView);
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
                            if (!isFragmentAlive()) return;
                            loadTutorialDetails(registeredTutorialIds);
                        } else {
                            if (!isFragmentAlive()) return;
                            safeUpdateUI(() -> binding.tutorialStats.setText("No valid tutorials found."));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loadedCount[0]++;
                    if (loadedCount[0] == totalRegistrations) {
                        if (!registeredTutorialIds.isEmpty()) {
                            if (!isFragmentAlive()) return;
                            loadTutorialDetails(registeredTutorialIds);
                        } else {
                            if (!isFragmentAlive()) return;
                            safeUpdateUI(() -> binding.tutorialStats.setText("Error loading some tutorial data."));
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
                        if (TutorialSummaryHelper.isTutorialUpcoming(tutorial)) {
                            upcomingTutorials.add(tutorial);
                        }
                    }
                    
                    // When all tutorials are loaded, update UI
                    if (loadedCount[0] == totalTutorials) {
                        if (!isFragmentAlive()) return;
                        safeUpdateUI(() -> {
                            TutorialSummaryHelper.TutorialSummaryConfig config = 
                                new TutorialSummaryHelper.TutorialSummaryConfig(TutorialSummaryHelper.ViewMode.CURRENT_USER_PROFILE)
                                    .setShowUpcomingSection(true)
                                    .setShowStatsSection(true);

                            TutorialSummaryHelper.bindTutorialSummary(
                                requireContext(),
                                binding.getRoot(),
                                allTutorials,
                                config
                            );
                            
                            // we have allTutorials and upcomingTutorials already computed
                            regTotal = allTutorials.size();
                            regUpcoming = upcomingTutorials.size();
                            regCompleted = Math.max(0, regTotal - regUpcoming);
                            regLoaded = true;
                            regUpcomingLoaded = true;
                            cachedRegUpcoming = new ArrayList<>(upcomingTutorials);
                            
                            // Ensure upcoming card is hidden if no upcoming registrations
                            if (upcomingTutorials.isEmpty()) {
                                LinearLayout upcomingCard = binding.getRoot().findViewById(R.id.upcoming_tutorials_card);
                                if (upcomingCard != null) upcomingCard.setVisibility(View.GONE);
                            }
                            
                            maybeUpdateCombinedCard();
                            maybeUpdateCombinedUpcomingCard();
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loadedCount[0]++;
                    if (loadedCount[0] == totalTutorials) {
                        if (!isFragmentAlive()) return;
                        safeUpdateUI(() -> {
                            TutorialSummaryHelper.TutorialSummaryConfig config = 
                                new TutorialSummaryHelper.TutorialSummaryConfig(TutorialSummaryHelper.ViewMode.CURRENT_USER_PROFILE)
                                    .setShowUpcomingSection(true)
                                    .setShowStatsSection(true);

                            TutorialSummaryHelper.bindTutorialSummary(
                                requireContext(),
                                binding.getRoot(),
                                allTutorials,
                                config
                            );
                            
                            // we have allTutorials and upcomingTutorials already computed
                            regTotal = allTutorials.size();
                            regUpcoming = upcomingTutorials.size();
                            regCompleted = Math.max(0, regTotal - regUpcoming);
                            regLoaded = true;
                            regUpcomingLoaded = true;
                            cachedRegUpcoming = new ArrayList<>(upcomingTutorials);
                            
                            // Ensure upcoming card is hidden if no upcoming registrations
                            if (upcomingTutorials.isEmpty()) {
                                LinearLayout upcomingCard = binding.getRoot().findViewById(R.id.upcoming_tutorials_card);
                                if (upcomingCard != null) upcomingCard.setVisibility(View.GONE);
                            }
                            
                            maybeUpdateCombinedCard();
                            maybeUpdateCombinedUpcomingCard();
                        });
                    }
                }
            });
        }
    }


    private void updateTutorialSummary(List<Tutorial> tutorials) {
        if (!isFragmentAlive()) return;
        
        if (tutorials.isEmpty()) {
            safeUpdateUI(() -> {
                binding.tutorialStats.setText(isTutor ? "No tutorials yet." : "No tutorials registered yet.");
                regTotal = regUpcoming = regCompleted = 0;
                regLoaded = true;
                maybeUpdateCombinedCard();
            });
            return;
        }

        final int totalTutorials = tutorials.size();
        final int[] counts = {0, 0}; // [upcomingCount, completedCount]

        for (Tutorial tutorial : tutorials) {
            if (TutorialSummaryHelper.isTutorialUpcoming(tutorial)) {
                counts[0]++; // upcomingCount
            } else {
                counts[1]++; // completedCount
            }
        }

        safeUpdateUI(() -> {
            final int upcomingCount = counts[0];
            final int completedCount = counts[1];
            // Modern stats row for registrations if available; otherwise fallback to legacy text
            View studentStatsRow = binding.getRoot().findViewById(R.id.studentStatsRow);
            TextView studentTotalVal = binding.getRoot().findViewById(R.id.studentTotalValue);
            TextView studentUpcomingVal = binding.getRoot().findViewById(R.id.studentUpcomingValue);
            TextView studentCompletedVal = binding.getRoot().findViewById(R.id.studentCompletedValue);
            TextView legacyStudent = binding.tutorialStats;
            if (studentStatsRow != null && studentTotalVal != null && studentUpcomingVal != null && studentCompletedVal != null && legacyStudent != null) {
                studentStatsRow.setVisibility(View.VISIBLE);
                legacyStudent.setVisibility(View.GONE);
                studentTotalVal.setText(String.valueOf(totalTutorials));
                studentUpcomingVal.setText(String.valueOf(upcomingCount));
                studentCompletedVal.setText(String.valueOf(completedCount));
            } else {
                String statsText = String.format(Locale.getDefault(),
                        "Total Registrations: %d\nUpcoming: %d\nCompleted: %d",
                        totalTutorials, upcomingCount, completedCount);
                binding.tutorialStats.setText(statsText);
            }
            regTotal = totalTutorials; regUpcoming = upcomingCount; regCompleted = completedCount; regLoaded = true;
            maybeUpdateCombinedCard();
        });
    }

    private void maybeUpdateCombinedCard() {
        if (binding == null) return;
        View combined = binding.getRoot().findViewById(R.id.combined_activity_card);
        View studentCard = binding.getRoot().findViewById(R.id.student_summary_card);
        View tutorCard = binding.getRoot().findViewById(R.id.tutor_summary_card);

        boolean canCombine = tutorToolsEnabled && regLoaded && tutLoaded && regTotal > 0 && tutTotal > 0;
        if (canCombine) {
            if (studentCard != null) studentCard.setVisibility(View.GONE);
            if (tutorCard != null) tutorCard.setVisibility(View.GONE);
            if (combined != null) combined.setVisibility(View.VISIBLE);

            TextView regTotal = binding.getRoot().findViewById(R.id.combinedRegTotalValue);
            TextView regUpcoming = binding.getRoot().findViewById(R.id.combinedRegUpcomingValue);
            TextView regCompleted = binding.getRoot().findViewById(R.id.combinedRegCompletedValue);
            TextView tutTotal = binding.getRoot().findViewById(R.id.combinedTutTotalValue);
            TextView tutUpcoming = binding.getRoot().findViewById(R.id.combinedTutUpcomingValue);
            TextView tutCompleted = binding.getRoot().findViewById(R.id.combinedTutCompletedValue);
            if (regTotal != null) regTotal.setText(String.valueOf(this.regTotal));
            if (regUpcoming != null) regUpcoming.setText(String.valueOf(this.regUpcoming));
            if (regCompleted != null) regCompleted.setText(String.valueOf(this.regCompleted));
            if (tutTotal != null) tutTotal.setText(String.valueOf(this.tutTotal));
            if (tutUpcoming != null) tutUpcoming.setText(String.valueOf(this.tutUpcoming));
            if (tutCompleted != null) tutCompleted.setText(String.valueOf(this.tutCompleted));

            TextView linkRegs = binding.getRoot().findViewById(R.id.combinedViewRegistrationsLink);
            TextView linkTuts = binding.getRoot().findViewById(R.id.combinedViewTutorialsLink);
            if (linkRegs != null) {
                linkRegs.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), TutorialHistoryActivity.class);
                    startActivity(intent);
                });
            }
            if (linkTuts != null) {
                linkTuts.setOnClickListener(v -> {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        Intent intent = new Intent(getActivity(), TutorialHistoryActivity.class);
                        intent.putExtra("isTutorView", true);
                        intent.putExtra("tutorId", currentUser.getUid());
                        startActivity(intent);
                    }
                });
            }
        } else {
            if (combined != null) combined.setVisibility(View.GONE);
            if (studentCard != null) studentCard.setVisibility(View.VISIBLE);
            if (tutorCard != null) tutorCard.setVisibility(tutorToolsEnabled ? View.VISIBLE : View.GONE);
        }
    }

    private void updateUpcomingTutorials(List<Tutorial> upcomingTutorials) {
        if (!isFragmentAlive()) return;
        
        safeUpdateUI(() -> {
            LinearLayout upcomingCard = binding.getRoot().findViewById(R.id.upcoming_tutorials_card);
            LinearLayout upcomingList = binding.getRoot().findViewById(R.id.upcomingTutorialsList);
        
        if (upcomingTutorials.isEmpty()) {
            upcomingCard.setVisibility(View.GONE);
            regUpcoming = 0;
            regUpcomingLoaded = true;
            maybeUpdateCombinedUpcomingCard();
            return;
        }

        upcomingCard.setVisibility(View.VISIBLE);
        upcomingList.removeAllViews();
        regUpcoming = upcomingTutorials.size();
        regUpcomingLoaded = true;
        maybeUpdateCombinedUpcomingCard();

        for (Tutorial tutorial : upcomingTutorials) {
            // Inflate the tutorial card component
            View tutorialCardView = getLayoutInflater().inflate(R.layout.tutorial_card_item, upcomingList, false);
            
            // Get references to the views in the card
            TextView tutorialName = tutorialCardView.findViewById(R.id.tutorialCardName);
            TextView tutorialTutor = tutorialCardView.findViewById(R.id.tutorialCardTutor);
            TextView tutorialDateTime = tutorialCardView.findViewById(R.id.tutorialCardDateTime);
            TextView tutorialLocation = tutorialCardView.findViewById(R.id.tutorialCardLocation);
            
            // Set the tutorial data
            tutorialName.setText(tutorial.getTutorialName() != null ? tutorial.getTutorialName() : "Unnamed Tutorial");
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
        });
    }

    private void maybeUpdateCombinedUpcomingCard() {
        if (binding == null) return;
        View combined = binding.getRoot().findViewById(R.id.combined_upcoming_card);
        View studentUpcomingCard = binding.getRoot().findViewById(R.id.upcoming_tutorials_card);
        View tutorUpcomingCard = binding.getRoot().findViewById(R.id.tutor_upcoming_tutorials_card);

        boolean canCombine = tutorToolsEnabled && regUpcomingLoaded && tutUpcomingLoaded && regUpcoming > 0 && tutUpcoming > 0;
        if (!canCombine) {
            if (combined != null) combined.setVisibility(View.GONE);
            // single cards visibility already managed by their updaters
            return;
        }

        // Combine view: hide singles, show combined
        if (studentUpcomingCard != null) studentUpcomingCard.setVisibility(View.GONE);
        if (tutorUpcomingCard != null) tutorUpcomingCard.setVisibility(View.GONE);
        if (combined != null) combined.setVisibility(View.VISIBLE);

        LinearLayout regsList = binding.getRoot().findViewById(R.id.combinedUpcomingRegsList);
        LinearLayout tutsList = binding.getRoot().findViewById(R.id.combinedUpcomingTutsList);
        if (regsList != null) {
            regsList.removeAllViews();
            int shown = 0;
            for (Tutorial t : cachedRegUpcoming) {
                if (!TutorialSummaryHelper.isTutorialUpcoming(t)) continue;
                View v = getLayoutInflater().inflate(R.layout.tutorial_card_item, regsList, false);
                TutorialSummaryHelper.bindTutorialCard(getContext(), v, t,
                    new TutorialSummaryHelper.TutorialSummaryConfig(TutorialSummaryHelper.ViewMode.CURRENT_USER_PROFILE));
                regsList.addView(v);
                if (++shown >= 2) break;
            }
        }
        if (tutsList != null) {
            tutsList.removeAllViews();
            int shown = 0;
            for (Tutorial t : cachedTutUpcoming) {
                if (!TutorialSummaryHelper.isTutorialUpcoming(t)) continue;
                View v = getLayoutInflater().inflate(R.layout.tutorial_card_item, tutsList, false);
                TutorialSummaryHelper.bindTutorialCard(getContext(), v, t,
                    new TutorialSummaryHelper.TutorialSummaryConfig(TutorialSummaryHelper.ViewMode.CURRENT_USER_PROFILE));
                tutsList.addView(v);
                if (++shown >= 2) break;
            }
        }

        TextView regsLink = binding.getRoot().findViewById(R.id.combinedUpcomingRegsLink);
        if (regsLink != null) regsLink.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TutorialHistoryActivity.class);
            startActivity(intent);
        });
        TextView tutsLink = binding.getRoot().findViewById(R.id.combinedUpcomingTutsLink);
        if (tutsLink != null) tutsLink.setOnClickListener(v -> {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u != null) {
                Intent intent = new Intent(getActivity(), TutorialHistoryActivity.class);
                intent.putExtra("isTutorView", true);
                intent.putExtra("tutorId", u.getUid());
                startActivity(intent);
            }
        });
    }



    private String formatPhoneNumber(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1);
        }
        if (digits.length() == 10) {
            String area = digits.substring(0,3);
            String mid = digits.substring(3,6);
            String last = digits.substring(6);
            return "(" + area + ") " + mid + "-" + last;
        }
        if (digits.length() == 7) {
            return digits.substring(0,3) + "-" + digits.substring(3);
        }
        return raw;
    }
    private Tutorial createTutorialFromSnapshot(DataSnapshot snapshot, String tutorialId) {
        if (snapshot == null || !snapshot.exists()) {
            Tutorial t = new Tutorial(
                    "Unknown Tutorial",
                    "General",
                    "Free",
                    "TBD",
                    "TBD",
                    "TBD",
                    "No description available",
                    "Location TBD",
                    0.0, 0.0, "",
                    "Unknown Tutor",
                    "",
                    ""
            );
            if (tutorialId != null) t.setTutorialId(tutorialId);
            return t;
        }

        String tutorialName = snapshot.child("tutorialName").getValue(String.class);
        String topic = snapshot.child("topic").getValue(String.class);
        String fee = snapshot.child("fee").getValue(String.class);
        String date = snapshot.child("date").getValue(String.class);
        String startTime = snapshot.child("startTime").getValue(String.class);
        String endTime = snapshot.child("endTime").getValue(String.class);
        String address = snapshot.child("address").getValue(String.class);
        String tutorName = snapshot.child("tutorName").getValue(String.class);
        String description = snapshot.child("description").getValue(String.class);

        Tutorial t = new Tutorial(
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
        if (tutorialId != null) t.setTutorialId(tutorialId);
        return t;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            // Always load registrations (student view)
            loadTutorialData();

            // Always refresh profile (this will decide Tutor Tools state)
            loadUserProfile(); // see patch B to make it also load tutor cards
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Set destruction flag to prevent any future UI updates
        isFragmentDestroyed = true;
        
        // Cancel pending reviews fills
        reviewsLoadVersion++;
        
        // Clear switch listener to prevent callback after destruction
        if (switchEnableTutorTools != null) {
            switchEnableTutorTools.setOnCheckedChangeListener(null);
        }
        
        binding = null;
    }
}
