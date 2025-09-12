package com.example.csci3130group1;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.util.Log;
import java.text.DateFormat;
import java.util.Date;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// TutorialDetailsActivity class
public class TutorialDetailsActivity extends AppCompatActivity {

//    Attributes
    
    // New structured UI components
    private TextView tutorialName;
    private TextView tutorialSubject;
    private TextView tutorialDate;
    private TextView tutorialTime;
    private TextView tutorialLocation;
    private TextView tutorialFeeView;
    private TextView tutorialDescription;
    private LinearLayout descriptionSection;
    private TextView registerButton;
    private TextView receiptLink;
    private LinearLayout availabilityRow;
    private TextView tutorialCapacity;
    private TextView tutorialSpotsLeft;
    private DatabaseReference tutorialRef;
    private String tutorialId;
    private String tutorialTitle;
    private String tutorialFeeString;
    private boolean isAlreadyRegistered;
    private Integer capacity = null;
    
    // Tutor info card components
    private CardView tutorInfoCard;
    private TextView tutorName;
    private TextView tutorDegree;
    private TextView tutorialCount;
    private TextView tutorRating;
    private LinearLayout ratingContainer;
    private String currentTutorId;
    // Tutor-only registered students section
    private com.google.android.material.card.MaterialCardView registeredStudentsCard;
    private LinearLayout registeredStudentsList;
    private TextView noRegisteredStudentsText;
    // Author-only cancel
    private LinearLayout cancelContainer;
    private TextView cancelPolicyText;
    private TextView cancelTutorialLink;
    private boolean isAuthor = false;
    private long registeredCount = 0;

//    onCreate() method
    @Override
    protected void onCreate(Bundle savedInstanceState) {

//        Loads page
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorial_details_activity);
        
        // Initialize new structured UI components
        tutorialName = findViewById(R.id.tutorialName);
        tutorialSubject = findViewById(R.id.tutorialSubject);
        tutorialDate = findViewById(R.id.tutorialDate);
        tutorialTime = findViewById(R.id.tutorialTime);
        tutorialLocation = findViewById(R.id.tutorialLocation);
        tutorialFeeView = findViewById(R.id.tutorialFee);
        tutorialDescription = findViewById(R.id.tutorialDescription);
        descriptionSection = findViewById(R.id.descriptionSection);
        availabilityRow = findViewById(R.id.availabilityRow);
        tutorialCapacity = findViewById(R.id.tutorialCapacity);
        tutorialSpotsLeft = findViewById(R.id.tutorialSpotsLeft);
        registerButton = findViewById(R.id.register_button);
        receiptLink = findViewById(R.id.receipt_link);
        registeredStudentsCard = findViewById(R.id.registered_students_card);
        registeredStudentsList = findViewById(R.id.registeredStudentsList);
        noRegisteredStudentsText = findViewById(R.id.noRegisteredStudentsText);
        cancelContainer = findViewById(R.id.cancel_container);
        cancelPolicyText = findViewById(R.id.cancel_policy_text);
        cancelTutorialLink = findViewById(R.id.cancel_tutorial_link);
        
        // Initialize tutor info card components
        tutorInfoCard = findViewById(R.id.tutorInfoCard);
        tutorName = findViewById(R.id.tutorName);
        tutorDegree = findViewById(R.id.tutorDegree);
        tutorialCount = findViewById(R.id.tutorialCount);
        tutorRating = findViewById(R.id.tutorRating);
        ratingContainer = findViewById(R.id.ratingContainer);
        
        // Set click listener for tutor card
        if (tutorInfoCard != null) {
            tutorInfoCard.setOnClickListener(v -> {
                if (currentTutorId != null) {
                    Intent intent = new Intent(this, TutorProfileActivity.class);
                    intent.putExtra("tutorId", currentTutorId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Tutor information not available", Toast.LENGTH_SHORT).show();
                }
            });
        }

//        Gets tutorialID from intent
        tutorialId = getIntent().getStringExtra("tutorialId");
        isAlreadyRegistered = getIntent().getBooleanExtra("isAlreadyRegistered", false);
        if (tutorialId == null) {
            Toast.makeText(this, "Tutorial details not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

//        Loads tutorial details
        tutorialRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions").child(tutorialId);
        loadTutorialDetails();

//        Configure register button based on registration status
        if (isAlreadyRegistered) {
            registerButton.setText("Already Registered");
            registerButton.setEnabled(false);
            registerButton.setAlpha(0.6f);
        }

//        Registration button
        if (!isAlreadyRegistered) {
            registerButton.setOnClickListener(v -> {
            try {

//                Debugging
                Log.d("TutorialDetails", "Creating intent to RegisterForTutorialActivity");
                Log.d("TutorialDetails", "tutorialId: " + tutorialId);
                Log.d("TutorialDetails", "tutorialTitle: " + tutorialTitle);
                Log.d("TutorialDetails", "tutorialFee: " + tutorialFeeString);

//                Navigates to registration and payment
                Intent registerIntent = new Intent(TutorialDetailsActivity.this, RegisterForTutorialActivity.class);
                registerIntent.putExtra("tutorialId", tutorialId);
                registerIntent.putExtra("tutorialTitle", tutorialTitle);
                registerIntent.putExtra("tutorialFee", tutorialFeeString);

//                Sets component from debugging errors
                registerIntent.setComponent(new ComponentName(getPackageName(),
                        "com.example.csci3130group1.RegisterForTutorialActivity"));

                startActivity(registerIntent);

//                Toast message
                Toast.makeText(TutorialDetailsActivity.this,
                        "Launching registration page...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {

//                Logging exceptions
                Log.e("TutorialDetails", "Error launching RegisterForTutorialActivity", e);
                Toast.makeText(TutorialDetailsActivity.this,
                        "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            });
        }
    }

//    loadTutorialDetails() method
    private void loadTutorialDetails() {
        tutorialRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

//                Extracts information
                if (dataSnapshot.exists()) {

//                    Gets NEW Firebase fields only
                    String tutorialName = dataSnapshot.child("tutorialName").getValue(String.class);
                    String topic = dataSnapshot.child("topic").getValue(String.class);
                    String description = dataSnapshot.child("description").getValue(String.class);
                    String fee = dataSnapshot.child("fee").getValue(String.class);
                    String date = dataSnapshot.child("date").getValue(String.class);
                    String startTime = dataSnapshot.child("startTime").getValue(String.class);
                    String endTime = dataSnapshot.child("endTime").getValue(String.class);
                    String address = dataSnapshot.child("address").getValue(String.class);
                    String tutorName = dataSnapshot.child("tutorName").getValue(String.class);
                    String tutorId = dataSnapshot.child("tutorId").getValue(String.class);
                    String placeId = dataSnapshot.child("placeId").getValue(String.class);
                    Double latitude = dataSnapshot.child("latitude").getValue(Double.class);
                    Double longitude = dataSnapshot.child("longitude").getValue(Double.class);
                    Long capVal = dataSnapshot.child("capacity").getValue(Long.class);
                    capacity = (capVal != null) ? capVal.intValue() : null;
                    
                    // Store tutor ID for navigation
                    currentTutorId = tutorId;

//                    Store title and fee for registration
                    tutorialTitle = (tutorialName != null) ? tutorialName : topic;
                    tutorialFeeString = fee;
                    
                    // Populate structured UI components
                    populateModernTutorialDetails(tutorialName, topic, date, startTime, endTime, address, fee, description);
                    
                    registerButton.setEnabled(fee != null && !fee.isEmpty());
                    
                    // Populate tutor info card
                    loadTutorInfo(tutorId, tutorName);

                    // Show registered students to everyone
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    isAuthor = currentUser != null && tutorId != null && tutorId.equals(currentUser.getUid());
                    
                    if (isAuthor) {
                        // Hide register button for creators
                        if (registerButton != null) {
                            registerButton.setVisibility(android.view.View.GONE);
                        }
                        if (cancelContainer != null) cancelContainer.setVisibility(android.view.View.VISIBLE);
                        if (cancelTutorialLink != null) {
                            cancelTutorialLink.setOnClickListener(v -> {
                                if (registeredCount > 0) {
                                    Toast.makeText(TutorialDetailsActivity.this, "Cannot cancel: students are registered.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                com.example.csci3130group1.ui.tutorials.CancelTutorialDialogFragment dialog = new com.example.csci3130group1.ui.tutorials.CancelTutorialDialogFragment();
                                dialog.setOnConfirmListener(() -> TutorialDetailsActivity.this.deleteTutorial());
                                dialog.show(getSupportFragmentManager(), "CancelTutorialDialog");
                            });
                        }
                    }
                    
                    // Show registered students to everyone
                    if (registeredStudentsCard != null) {
                        registeredStudentsCard.setVisibility(android.view.View.VISIBLE);
                    }
                    loadRegisteredStudents(isAuthor);

                } else {
                    Toast.makeText(TutorialDetailsActivity.this,
                            "Tutorial details not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

//            onCancelled() method
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TutorialDetailsActivity.this,
                        "Failed to load tutorial: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadRegisteredStudents(boolean isAuthor) {
        if (tutorialId == null) return;
        DatabaseReference registeredRef = FirebaseDatabase.getInstance()
                .getReference("tutorial_sessions")
                .child(tutorialId)
                .child("registeredStudents");

        registeredRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (registeredStudentsList == null) return;
                registeredStudentsList.removeAllViews();

                // Header count
                TextView header = findViewById(R.id.registeredStudentsHeader);
                if (header != null) {
                    header.setText("Registered Students (" + snapshot.getChildrenCount() + ")");
                }

                registeredCount = snapshot.getChildrenCount();
                updateAvailabilityUI();
                // Toggle receipt link visibility for the current user
                FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
                boolean amRegistered = me != null && snapshot.hasChild(me.getUid());
                if (receiptLink != null) {
                    receiptLink.setVisibility(amRegistered ? android.view.View.VISIBLE : android.view.View.GONE);
                    if (amRegistered) {
                        receiptLink.setOnClickListener(v -> {
                            Intent receiptIntent = new Intent(TutorialDetailsActivity.this, RegistrationConfirmationActivity.class);
                            receiptIntent.putExtra("tutorialId", tutorialId);
                            receiptIntent.putExtra("tutorialTitle", tutorialTitle);
                            receiptIntent.putExtra("tutorialFee", tutorialFeeString);
                            startActivity(receiptIntent);
                        });
                    }
                }
                // Update cancel UI state for authors
                if (isAuthor && cancelTutorialLink != null) {
                    boolean canCancel = registeredCount == 0;
                    cancelTutorialLink.setEnabled(canCancel);
                    cancelTutorialLink.setAlpha(canCancel ? 1.0f : 0.5f);
                }

                if (!snapshot.exists() || registeredCount == 0) {
                    if (noRegisteredStudentsText != null) noRegisteredStudentsText.setVisibility(android.view.View.VISIBLE);
                    return;
                }
                if (noRegisteredStudentsText != null) noRegisteredStudentsText.setVisibility(android.view.View.GONE);

                for (DataSnapshot child : snapshot.getChildren()) {
                    String studentId = child.getKey();
                    if (studentId != null) {
                        loadStudentInfo(studentId, isAuthor);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (noRegisteredStudentsText != null) noRegisteredStudentsText.setVisibility(android.view.View.VISIBLE);
            }
        });
    }

    private void updateAvailabilityUI() {
        if (availabilityRow == null || tutorialCapacity == null || tutorialSpotsLeft == null) return;

        if (capacity == null || capacity <= 0) {
            // Unlimited capacity
            availabilityRow.setVisibility(android.view.View.GONE);
            if (!isAlreadyRegistered && registerButton != null) {
                registerButton.setEnabled(true);
                registerButton.setAlpha(1.0f);
                registerButton.setText("Register for Tutorial");
            }
            return;
        }

        availabilityRow.setVisibility(android.view.View.VISIBLE);
        tutorialCapacity.setText(String.valueOf(capacity));
        long spotsLeft = Math.max(capacity - registeredCount, 0);
        tutorialSpotsLeft.setText(String.valueOf(spotsLeft));
        // Colorize spots left from green (more empty) to red (full)
        tutorialSpotsLeft.setTextColor(getCapacityColor(spotsLeft, capacity));

        if (!isAlreadyRegistered && registerButton != null) {
            if (spotsLeft <= 0) {
                registerButton.setEnabled(false);
                registerButton.setText("Full");
                registerButton.setAlpha(0.6f);
            } else {
                registerButton.setEnabled(true);
                registerButton.setText("Register for Tutorial");
                registerButton.setAlpha(1.0f);
            }
        }
    }

    // Map remaining/capacity to a color ranging from green (safe) to red (urgent)
    private int getCapacityColor(long remaining, long capacity) {
        if (capacity <= 0) return Color.parseColor("#6B7280");
        float ratio = Math.max(0f, Math.min(1f, remaining / (float) capacity));
        int green = Color.parseColor("#059669"); // emerald-600
        int yellow = Color.parseColor("#F59E0B"); // amber-500
        int red = Color.parseColor("#DC2626"); // red-600

        if (ratio >= 0.5f) {
            float t = (ratio - 0.5f) / 0.5f; // 0..1 from yellow->green
            return lerpColor(yellow, green, t);
        } else {
            float t = ratio / 0.5f; // 0..1 from red->yellow
            return lerpColor(red, yellow, t);
        }
    }

    private int lerpColor(int startColor, int endColor, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (Color.alpha(startColor) + (Color.alpha(endColor) - Color.alpha(startColor)) * t);
        int r = (int) (Color.red(startColor) + (Color.red(endColor) - Color.red(startColor)) * t);
        int g = (int) (Color.green(startColor) + (Color.green(endColor) - Color.green(startColor)) * t);
        int b = (int) (Color.blue(startColor) + (Color.blue(endColor) - Color.blue(startColor)) * t);
        return Color.argb(a, r, g, b);
    }

    private void deleteTutorial() {
        if (tutorialId == null) return;
        // Safety: only allow when zero registrations
        if (registeredCount > 0) {
            Toast.makeText(this, "Cannot cancel: students are registered.", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference sessionRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions").child(tutorialId);
        sessionRef.removeValue((error, ref) -> {
            if (error == null) {
                Toast.makeText(this, "Tutorial cancelled.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to cancel: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadStudentInfo(String studentId, boolean isAuthor) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(studentId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                String contact = snapshot.child("contact").getValue(String.class);
                String role = snapshot.child("role").getValue(String.class);
                boolean isTutor = "Tutor".equalsIgnoreCase(role);
                
                if (isAuthor) {
                    // Authors see detailed view
                    addStudentRow(studentId, name, email, contact);
                } else {
                    // Non-authors see simplified view
                    addSimpleStudentRow(studentId, name, isTutor);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addStudentRow(String studentId, String name, String email, String contact) {
        if (registeredStudentsList == null) return;
        android.view.View item = getLayoutInflater().inflate(R.layout.registered_student_item, registeredStudentsList, false);
        TextView nameView = item.findViewById(R.id.studentName);
        TextView emailView = item.findViewById(R.id.studentEmail);
        TextView contactView = item.findViewById(R.id.studentContact);
        TextView paymentIdView = item.findViewById(R.id.studentPaymentId);
        TextView registeredAtView = item.findViewById(R.id.studentRegisteredAt);

        nameView.setText(name != null ? name : "Unknown Student");
        emailView.setText(email != null ? email : "");
        contactView.setText(formatPhone(contact));

        // Default placeholders until we load registration info
        paymentIdView.setText("Payment ID: N/A");
        registeredAtView.setText("--");

        // Load registration info for this student for this tutorial
        DatabaseReference regRef = FirebaseDatabase.getInstance().getReference("registrations");
        regRef.orderByChild("tutorialId").equalTo(tutorialId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot regSnap : snapshot.getChildren()) {
                            String uid = regSnap.child("userId").getValue(String.class);
                            if (studentId.equals(uid)) {
                                String paymentId = regSnap.child("paymentId").getValue(String.class);
                                Long ts = regSnap.child("timestamp").getValue(Long.class);
                                if (paymentId != null && !paymentId.isEmpty()) {
                                    paymentIdView.setText("Payment ID: " + paymentId);
                                } else {
                                    paymentIdView.setText("Payment ID: N/A");
                                }
                                if (ts != null && ts > 0) {
                                    String formatted = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(ts));
                                    registeredAtView.setText(formatted);
                                }
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        registeredStudentsList.addView(item);
    }

    private void addSimpleStudentRow(String studentId, String name, boolean isTutor) {
        if (registeredStudentsList == null) return;
        android.view.View item = getLayoutInflater().inflate(R.layout.registered_student_simple_item, registeredStudentsList, false);
        TextView nameView = item.findViewById(R.id.studentName);

        nameView.setText(name != null ? name : "Unknown Student");
        
        if (isTutor) {
            // Make tutor names orange and clickable
            nameView.setTextColor(android.graphics.Color.parseColor("#A0522D"));
            nameView.setClickable(true);
            nameView.setFocusable(true);
            nameView.setOnClickListener(v -> {
                // Navigate to tutor profile
                android.content.Intent intent = new android.content.Intent(this, TutorProfileActivity.class);
                intent.putExtra("tutorId", studentId);
                startActivity(intent);
            });
        } else {
            // Regular styling for non-tutors - keep default background from layout
            nameView.setTextColor(android.graphics.Color.parseColor("#111827"));
            nameView.setClickable(false);
            nameView.setFocusable(false);
        }

        registeredStudentsList.addView(item);
    }

    private String formatPhone(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "Not provided";
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 11 && digits.startsWith("1")) digits = digits.substring(1);
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
    
    private void populateModernTutorialDetails(String tutorialName, String topic, String date, 
                                              String startTime, String endTime, String address, 
                                              String fee, String description) {
        // Tutorial Name
        if (this.tutorialName != null) {
            if (tutorialName != null && !tutorialName.trim().isEmpty()) {
                this.tutorialName.setText(tutorialName);
            } else if (topic != null && !topic.trim().isEmpty()) {
                this.tutorialName.setText(topic);
            } else {
                this.tutorialName.setText("Unknown Tutorial");
            }
        }
        
        // Subject (if different from tutorial name)
        if (tutorialSubject != null) {
            if (topic != null && !topic.trim().isEmpty() && !topic.equals(tutorialName)) {
                tutorialSubject.setText(topic);
                tutorialSubject.setVisibility(android.view.View.VISIBLE);
            } else {
                tutorialSubject.setVisibility(android.view.View.GONE);
            }
        }
        
        // Date
        if (tutorialDate != null) {
            tutorialDate.setText(date != null ? date : "TBD");
        }
        
        // Time
        if (tutorialTime != null) {
            if (startTime != null && endTime != null) {
                tutorialTime.setText(startTime + " - " + endTime);
            } else {
                tutorialTime.setText("TBD");
            }
        }
        
        // Location
        if (tutorialLocation != null) {
            tutorialLocation.setText(address != null ? address : "Location TBD");
        }
        
        // Fee
        if (this.tutorialFeeView != null) {
            com.example.csci3130group1.utils.FeeStyleUtils.apply(this.tutorialFeeView, fee, this);
        }
        
        // Description
        if (tutorialDescription != null && descriptionSection != null) {
            if (description != null && !description.trim().isEmpty()) {
                tutorialDescription.setText(description);
                descriptionSection.setVisibility(android.view.View.VISIBLE);
            } else {
                descriptionSection.setVisibility(android.view.View.GONE);
            }
        }
    }
    
    private void loadTutorInfo(String tutorId, String tutorNameFromTutorial) {
        if (tutorId == null || tutorId.isEmpty()) {
            // Fallback to tutorial tutor name if no tutorId
            if (tutorName != null && tutorNameFromTutorial != null) {
                tutorName.setText(tutorNameFromTutorial);
            }
            return;
        }
        
        DatabaseReference tutorRef = FirebaseDatabase.getInstance().getReference("users").child(tutorId);
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews").child(tutorId);
        DatabaseReference tutorialSessionsRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        
        // Load tutor basic info
        tutorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && tutorName != null) {
                    String name = snapshot.child("name").getValue(String.class);
                    String degree = snapshot.child("degree").getValue(String.class);
                    
                    tutorName.setText(name != null ? name : tutorNameFromTutorial);
                    
                    if (degree != null && !degree.trim().isEmpty() && tutorDegree != null) {
                        tutorDegree.setText(degree);
                        tutorDegree.setVisibility(android.view.View.VISIBLE);
                    }
                } else if (tutorName != null) {
                    tutorName.setText(tutorNameFromTutorial != null ? tutorNameFromTutorial : "Unknown Tutor");
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (tutorName != null) {
                    tutorName.setText(tutorNameFromTutorial != null ? tutorNameFromTutorial : "Unknown Tutor");
                }
            }
        });
        
        // Load tutor rating
        reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (tutorRating != null && ratingContainer != null) {
                    float total = 0;
                    int count = 0;
                    
                    for (DataSnapshot reviewSnap : snapshot.getChildren()) {
                        Double ratingNumber = reviewSnap.child("rating").getValue(Double.class);
                        if (ratingNumber != null) {
                            total += ratingNumber.floatValue();
                            count++;
                        }
                    }
                    
                    if (count > 0) {
                        float average = total / count;
                        tutorRating.setText(String.format("%.1f (%d)", average, count));
                        ratingContainer.setVisibility(android.view.View.VISIBLE);
                    } else {
                        tutorRating.setText("No reviews yet");
                        ratingContainer.setVisibility(android.view.View.VISIBLE);
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        
        // Count total tutorials by this tutor
        tutorialSessionsRef.orderByChild("tutorId").equalTo(tutorId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (tutorialCount != null) {
                        long count = snapshot.getChildrenCount();
                        String countText = count == 1 ? "1 tutorial" : count + " tutorials";
                        tutorialCount.setText(countText);
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (tutorialCount != null) {
                        tutorialCount.setText("Tutorials available");
                    }
                }
            });
    }
}
