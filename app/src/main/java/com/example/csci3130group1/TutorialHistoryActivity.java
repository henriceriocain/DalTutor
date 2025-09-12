package com.example.csci3130group1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

public class TutorialHistoryActivity extends AppCompatActivity {

    private LinearLayout upcomingTutorialsList;
    private LinearLayout pastTutorialsList;
    private TextView upcomingEmptyMessage;
    private TextView pastEmptyMessage;
    private TextView upcomingCount;
    private TextView pastCount;
    private TextView historyTitle;
    
    private boolean isTutorView = false;
    private String tutorId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial_history);

        // Check if this is being called for a tutor's own tutorials
        isTutorView = getIntent().getBooleanExtra("isTutorView", false);
        tutorId = getIntent().getStringExtra("tutorId");

        initializeViews();
        loadTutorialHistory();
    }

    private void initializeViews() {
        upcomingTutorialsList = findViewById(R.id.upcomingTutorialsList);
        pastTutorialsList = findViewById(R.id.pastTutorialsList);
        upcomingEmptyMessage = findViewById(R.id.upcomingEmptyMessage);
        pastEmptyMessage = findViewById(R.id.pastEmptyMessage);
        upcomingCount = findViewById(R.id.upcomingCount);
        pastCount = findViewById(R.id.pastCount);
        historyTitle = findViewById(R.id.historyTitle);
        
        // Set appropriate title and terminology based on view type
        if (isTutorView) {
            historyTitle.setText("Tutorial History");
        } else {
            historyTitle.setText("Registration History");
        }
    }

    private void loadTutorialHistory() {
        if (isTutorView && tutorId != null) {
            loadTutorTutorials();
        } else {
            loadStudentRegistrations();
        }
    }
    
    private void loadStudentRegistrations() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view tutorial history", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference userRegistrationsRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("registrations");

        userRegistrationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() == 0) {
                    showEmptyState();
                    return;
                }

                List<String> registrationIds = new ArrayList<>();
                for (DataSnapshot regSnap : snapshot.getChildren()) {
                    String registrationId = regSnap.getKey();
                    if (registrationId != null) {
                        registrationIds.add(registrationId);
                    }
                }

                loadAllRegistrationDetails(registrationIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TutorialHistoryActivity.this, "Error loading tutorial history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTutorTutorials() {
        DatabaseReference tutorialSessionsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        
        tutorialSessionsRef.orderByChild("tutorId").equalTo(tutorId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.getChildrenCount() == 0) {
                        showEmptyState();
                        return;
                    }

                    List<String> tutorialIds = new ArrayList<>();
                    for (DataSnapshot tutorialSnap : snapshot.getChildren()) {
                        String tutorialId = tutorialSnap.getKey();
                        if (tutorialId != null) {
                            tutorialIds.add(tutorialId);
                        }
                    }

                    loadTutorialDetails(tutorialIds);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(TutorialHistoryActivity.this, "Error loading tutorial history", Toast.LENGTH_SHORT).show();
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

                    if (loadedCount[0] == totalRegistrations) {
                        if (!registeredTutorialIds.isEmpty()) {
                            loadTutorialDetails(registeredTutorialIds);
                        } else {
                            showEmptyState();
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
                            showEmptyState();
                        }
                    }
                }
            });
        }
    }

    private void loadTutorialDetails(List<String> tutorialIds) {
        DatabaseReference tutorialSessionsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        List<Tutorial> upcomingTutorials = new ArrayList<>();
        List<Tutorial> pastTutorials = new ArrayList<>();

        final int totalTutorials = tutorialIds.size();
        final int[] loadedCount = {0};

        for (String tutorialId : tutorialIds) {
            tutorialSessionsRef.child(tutorialId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    loadedCount[0]++;

                    if (snapshot.exists()) {
                        Tutorial tutorial = createTutorialFromSnapshot(snapshot, tutorialId);
                        
                        if (isTutorialUpcoming(tutorial)) {
                            upcomingTutorials.add(tutorial);
                        } else {
                            pastTutorials.add(tutorial);
                        }
                    }

                    if (loadedCount[0] == totalTutorials) {
                        displayTutorials(upcomingTutorials, pastTutorials);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loadedCount[0]++;
                    if (loadedCount[0] == totalTutorials) {
                        displayTutorials(upcomingTutorials, pastTutorials);
                    }
                }
            });
        }
    }

    private Tutorial createTutorialFromSnapshot(DataSnapshot snapshot, String tutorialId) {
        String tutorialName = snapshot.child("tutorialName").getValue(String.class);
        String topic = snapshot.child("topic").getValue(String.class);
        String fee = snapshot.child("fee").getValue(String.class);
        String date = snapshot.child("date").getValue(String.class);
        String startTime = snapshot.child("startTime").getValue(String.class);
        String endTime = snapshot.child("endTime").getValue(String.class);
        String address = snapshot.child("address").getValue(String.class);
        String tutorName = snapshot.child("tutorName").getValue(String.class);
        String tutorId = snapshot.child("tutorId").getValue(String.class);
        String description = snapshot.child("description").getValue(String.class);

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
                tutorId != null ? tutorId : "",
                ""
        );
        tutorial.setTutorialId(tutorialId);
        return tutorial;
    }

    private boolean isTutorialUpcoming(Tutorial tutorial) {
        if (tutorial.getDate() == null) return false;

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date tutorialDate = dateFormat.parse(tutorial.getDate());
            Date currentDate = new Date();

            return tutorialDate != null && tutorialDate.after(currentDate);
        } catch (ParseException e) {
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

    private void displayTutorials(List<Tutorial> upcomingTutorials, List<Tutorial> pastTutorials) {
        displayUpcomingTutorials(upcomingTutorials);
        displayPastTutorials(pastTutorials);
    }

    private void displayUpcomingTutorials(List<Tutorial> tutorials) {
        upcomingCount.setText(String.format(Locale.getDefault(), "Upcoming (%d)", tutorials.size()));
        
        if (tutorials.isEmpty()) {
            findViewById(R.id.upcomingEmptyState).setVisibility(View.VISIBLE);
            upcomingTutorialsList.setVisibility(View.GONE);
        } else {
            findViewById(R.id.upcomingEmptyState).setVisibility(View.GONE);
            upcomingTutorialsList.setVisibility(View.VISIBLE);
            upcomingTutorialsList.removeAllViews();

            for (Tutorial tutorial : tutorials) {
                View tutorialCard = createTutorialCard(tutorial);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                layoutParams.bottomMargin = (int) (20 * getResources().getDisplayMetrics().density);
                tutorialCard.setLayoutParams(layoutParams);
                upcomingTutorialsList.addView(tutorialCard);
            }
        }
    }

    private void displayPastTutorials(List<Tutorial> tutorials) {
        pastCount.setText(String.format(Locale.getDefault(), "Past (%d)", tutorials.size()));
        
        if (tutorials.isEmpty()) {
            findViewById(R.id.pastEmptyState).setVisibility(View.VISIBLE);
            pastTutorialsList.setVisibility(View.GONE);
        } else {
            findViewById(R.id.pastEmptyState).setVisibility(View.GONE);
            pastTutorialsList.setVisibility(View.VISIBLE);
            pastTutorialsList.removeAllViews();

            for (Tutorial tutorial : tutorials) {
                View tutorialCard = createTutorialCard(tutorial);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                layoutParams.bottomMargin = (int) (20 * getResources().getDisplayMetrics().density);
                tutorialCard.setLayoutParams(layoutParams);
                pastTutorialsList.addView(tutorialCard);
            }
        }
    }

    private View createTutorialCard(Tutorial tutorial) {
        View tutorialCardView = getLayoutInflater().inflate(R.layout.tutorial_card_item, null, false);

        TextView tutorialName = tutorialCardView.findViewById(R.id.tutorialCardName);
        TextView tutorialTutor = tutorialCardView.findViewById(R.id.tutorialCardTutor);
        TextView tutorialDateTime = tutorialCardView.findViewById(R.id.tutorialCardDateTime);
        TextView tutorialLocation = tutorialCardView.findViewById(R.id.tutorialCardLocation);
        TextView capacityBadge = tutorialCardView.findViewById(R.id.capacityBadge);

        tutorialName.setText(tutorial.getTutorialName() != null ? tutorial.getTutorialName() : "Unnamed Tutorial");
        tutorialTutor.setText(tutorial.getTutorName() != null ? tutorial.getTutorName() : "Unknown Tutor");
        // Make tutor name a link to profile if tutorId is available
        if (tutorial.getTutorId() != null && !tutorial.getTutorId().isEmpty()) {
            tutorialTutor.setTextColor(getResources().getColor(com.example.csci3130group1.R.color.brown_primary));
            tutorialTutor.setClickable(true);
            tutorialTutor.setOnClickListener(v -> {
                Intent i = new Intent(this, TutorProfileActivity.class);
                i.putExtra("tutorId", tutorial.getTutorId());
                i.putExtra("readOnly", true);
                startActivity(i);
            });
        }

        String dateTime = String.format(Locale.getDefault(), "%s at %s - %s",
                tutorial.getDate() != null ? tutorial.getDate() : "No date",
                tutorial.getStartTime() != null ? tutorial.getStartTime() : "TBD",
                tutorial.getEndTime() != null ? tutorial.getEndTime() : "TBD");
        tutorialDateTime.setText(dateTime);

        tutorialLocation.setText(tutorial.getAddress() != null ? tutorial.getAddress() : "Location TBD");

        tutorialCardView.setOnClickListener(v -> {
            Intent intent = new Intent(this, TutorialDetailsActivity.class);
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

        // Live capacity badge updates
        attachCapacityListener(capacityBadge, tutorial.getTutorialId());

        return tutorialCardView;
    }

    private void attachCapacityListener(TextView badge, String tutorialId) {
        if (badge == null || tutorialId == null || tutorialId.isEmpty()) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("tutorial_sessions").child(tutorialId);
        ref.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long capVal = snapshot.child("capacity").getValue(Long.class);
                long cap = capVal != null ? capVal : 0L;
                long registered = snapshot.child("registeredStudents").getChildrenCount();
                if (cap > 0) {
                    badge.setVisibility(android.view.View.VISIBLE);
                    badge.setText(String.format(java.util.Locale.getDefault(), "%d/%d", registered, cap));
                    long remaining = Math.max(cap - registered, 0);
                    badge.setTextColor(getCapacityColor(remaining, cap));
                } else {
                    badge.setVisibility(android.view.View.GONE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // Capacity color scale similar to search results
    private int getCapacityColor(long remaining, long capacity) {
        if (capacity <= 0) return android.graphics.Color.parseColor("#6B7280");
        float ratio = Math.max(0f, Math.min(1f, remaining / (float) capacity));
        int green = android.graphics.Color.parseColor("#059669");
        int yellow = android.graphics.Color.parseColor("#F59E0B");
        int red = android.graphics.Color.parseColor("#DC2626");
        if (ratio >= 0.5f) {
            float t = (ratio - 0.5f) / 0.5f; // 0..1 yellow->green
            return lerpColor(yellow, green, t);
        } else {
            float t = ratio / 0.5f; // 0..1 red->yellow
            return lerpColor(red, yellow, t);
        }
    }

    private int lerpColor(int startColor, int endColor, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (android.graphics.Color.alpha(startColor) + (android.graphics.Color.alpha(endColor) - android.graphics.Color.alpha(startColor)) * t);
        int r = (int) (android.graphics.Color.red(startColor) + (android.graphics.Color.red(endColor) - android.graphics.Color.red(startColor)) * t);
        int g = (int) (android.graphics.Color.green(startColor) + (android.graphics.Color.green(endColor) - android.graphics.Color.green(startColor)) * t);
        int b = (int) (android.graphics.Color.blue(startColor) + (android.graphics.Color.blue(endColor) - android.graphics.Color.blue(startColor)) * t);
        return android.graphics.Color.argb(a, r, g, b);
    }

    private void showEmptyState() {
        upcomingCount.setText("Upcoming (0)");
        pastCount.setText("Past (0)");
        findViewById(R.id.upcomingEmptyState).setVisibility(View.VISIBLE);
        findViewById(R.id.pastEmptyState).setVisibility(View.VISIBLE);
        upcomingTutorialsList.setVisibility(View.GONE);
        pastTutorialsList.setVisibility(View.GONE);
        
        // Update empty messages based on view type
        if (isTutorView) {
            upcomingEmptyMessage.setText("No upcoming tutorials");
            pastEmptyMessage.setText("No past tutorials yet");
        } else {
            upcomingEmptyMessage.setText("No upcoming registrations");
            pastEmptyMessage.setText("No past registrations yet");
        }
    }
}
