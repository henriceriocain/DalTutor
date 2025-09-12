package com.example.csci3130group1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

// RegistrationConfirmationActivity class
public class RegistrationConfirmationActivity extends AppCompatActivity {

//    Attributes
    private TextView confirmationTextView;
    // Tutorial details views (modern UI)
    private TextView receiptTutorialName;
    private TextView receiptTutorialSubject;
    private TextView receiptTutorialDate;
    private TextView receiptTutorialTime;
    private TextView receiptTutorialLocation;
    private TextView receiptTutorialFee;
    private TextView receiptTutorName;
    // Receipt fields
    private TextView receiptType;
    private TextView receiptId;
    private TextView receiptDate;
    private TextView receiptAmount;
    private Button doneButton;

//    onCreate() method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_confirmation);

//        Initializes elements
        confirmationTextView = findViewById(R.id.confirmation_text);
        // Bind modern tutorial details views (shared component)
        receiptTutorialName = findViewById(R.id.tutorialName);
        receiptTutorialSubject = findViewById(R.id.tutorialSubject);
        receiptTutorialDate = findViewById(R.id.tutorialDate);
        receiptTutorialTime = findViewById(R.id.tutorialTime);
        receiptTutorialLocation = findViewById(R.id.tutorialLocation);
        receiptTutorialFee = findViewById(R.id.tutorialFee);
        // Tutor name card is separate on details; not present here
        // Bind receipt fields
        receiptType = findViewById(R.id.receiptType);
        receiptId = findViewById(R.id.receiptId);
        receiptDate = findViewById(R.id.receiptDate);
        receiptAmount = findViewById(R.id.receiptAmount);
        doneButton = findViewById(R.id.done_button);

//        Data from intent
        String tutorialId = getIntent().getStringExtra("tutorialId");
        String tutorialTitle = getIntent().getStringExtra("tutorialTitle");
        String tutorialFee = getIntent().getStringExtra("tutorialFee");
        String paymentId = getIntent().getStringExtra("paymentId");
        String paymentTime = getIntent().getStringExtra("paymentTime");

//        Current user data
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        confirmationTextView.setText("Receipt");

//        Payment time
        String formattedDate = formatPaymentTime(paymentTime);

//        Payment details based on fee
        setReceiptDetails(tutorialFee, paymentId, formattedDate);

        // Fallback: if any key fields are missing, try to load from registrations
        if (tutorialId != null && (tutorialFee == null || paymentId == null || paymentTime == null)) {
            loadLatestRegistrationForUser(tutorialId);
        }

//        Fetches tutorial details from firebase
        if (tutorialId != null) {
            DatabaseReference tutorialRef = FirebaseDatabase.getInstance()
                    .getReference("tutorial_sessions")
                    .child(tutorialId);

            String finalFormattedDate = formattedDate;
            tutorialRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                    Extracts tutorial details
                    if (dataSnapshot.exists()) {
                        // Use new Firebase structure
                        String nameNew = dataSnapshot.child("tutorialName").getValue(String.class);
                        String tutorName = dataSnapshot.child("tutorName").getValue(String.class);
                        String address = dataSnapshot.child("address").getValue(String.class);
                        String placeId = dataSnapshot.child("placeId").getValue(String.class);
                        String topic = dataSnapshot.child("topic").getValue(String.class);
                        String date = dataSnapshot.child("date").getValue(String.class);
                        String startTime = dataSnapshot.child("startTime").getValue(String.class);
                        String endTime = dataSnapshot.child("endTime").getValue(String.class);

                        String displayName = nameNew != null && !nameNew.isEmpty() ? nameNew : tutorialTitle;
                        if (receiptTutorialName != null) receiptTutorialName.setText(displayName);
                        // Tutor name not shown in this card for consistency with details component include
                        if (topic != null && !topic.isEmpty() && receiptTutorialSubject != null) {
                            receiptTutorialSubject.setText(topic);
                            receiptTutorialSubject.setVisibility(android.view.View.VISIBLE);
                        }
                        if (receiptTutorialDate != null) receiptTutorialDate.setText(date != null ? date : "—");
                        if (receiptTutorialTime != null) {
                            String tm = (startTime != null ? startTime : "");
                            if (endTime != null && !endTime.isEmpty() && tm.length() > 0) tm += " – " + endTime;
                            receiptTutorialTime.setText(tm.length() > 0 ? tm : "—");
                        }
                        if (address != null && !address.isEmpty() && receiptTutorialLocation != null) {
                            String loc = address + (placeId != null && !placeId.isEmpty() ? " · " + placeId : "");
                            receiptTutorialLocation.setText(loc);
                        }
                        if (receiptTutorialFee != null) {
                            if (tutorialFee != null && !tutorialFee.trim().isEmpty() && !tutorialFee.equals("0") && !tutorialFee.equals("0.0") && !tutorialFee.equals("0.00")) {
                                receiptTutorialFee.setText("$" + tutorialFee);
                            } else {
                                receiptTutorialFee.setText("FREE");
                            }
                        }
                    }
                }

//                onCancelled() method
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    if (receiptTutorialName != null) receiptTutorialName.setText(tutorialTitle != null ? tutorialTitle : "Tutorial");
                }
            });
        } else {

//            If tutorialID is null, default info
            if (receiptTutorialName != null) receiptTutorialName.setText(tutorialTitle != null ? tutorialTitle : "Tutorial");
        }

//        Done button functionality
        doneButton.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String email = user != null ? user.getEmail() : null;
            Intent intent = new Intent(RegistrationConfirmationActivity.this, StudentDashboard.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("username", email);
            intent.putExtra("role", "Student");
            startActivity(intent);
            finish();
        });
    }

    private void setReceiptDetails(String tutorialFee, String paymentId, String formattedDate) {
        boolean isFree = tutorialFee != null && (tutorialFee.equals("0") || tutorialFee.equals("0.0") || tutorialFee.equals("0.00"));
        if (receiptType != null) receiptType.setText(isFree ? "Tutorial Type: Free" : "Tutorial Type: Paid");
        if (receiptId != null) receiptId.setText((isFree ? "Registration ID: " : "Payment ID: ") + (paymentId != null ? paymentId : "N/A"));
        if (receiptDate != null) receiptDate.setText((isFree ? "Registration Date: " : "Payment Date: ") + (formattedDate != null ? formattedDate : "N/A"));
        if (receiptAmount != null) receiptAmount.setText("Amount Paid: $" + (isFree ? "0" : (tutorialFee != null ? tutorialFee : "N/A")) + " CAD");
    }

    private String formatPaymentTime(String paymentTime) {
        if (paymentTime == null) return null;
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(paymentTime);

            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.US);
            outputFormat.setTimeZone(TimeZone.getDefault());
            return outputFormat.format(date);
        } catch (ParseException e) {
            return paymentTime;
        }
    }

    private void loadLatestRegistrationForUser(String tutorialId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference regs = FirebaseDatabase.getInstance().getReference("registrations");
        regs.orderByChild("userId").equalTo(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long latestTs = -1;
                        String latestFee = null;
                        String latestPaymentId = null;
                        Long latestTimestamp = null;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String tId = child.child("tutorialId").getValue(String.class);
                            if (tId == null || !tId.equals(tutorialId)) continue;
                            Long ts = child.child("timestamp").getValue(Long.class);
                            if (ts != null && ts > latestTs) {
                                latestTs = ts;
                                latestFee = child.child("fee").getValue(String.class);
                                latestPaymentId = child.child("paymentId").getValue(String.class);
                                latestTimestamp = ts;
                            }
                        }
                        if (latestTs > 0) {
                            String formatted = null;
                            if (latestTimestamp != null) {
                                Date date = new Date(latestTimestamp);
                                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.US);
                                outputFormat.setTimeZone(TimeZone.getDefault());
                                formatted = outputFormat.format(date);
                            }
                            setReceiptDetails(latestFee, latestPaymentId, formatted);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }
}
