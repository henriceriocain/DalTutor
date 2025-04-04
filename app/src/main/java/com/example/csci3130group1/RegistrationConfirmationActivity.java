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
    private TextView tutorialDetailsTextView;
    private TextView paymentDetailsTextView;
    private Button doneButton;

//    onCreate() method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_confirmation);

//        Initializes elements
        confirmationTextView = findViewById(R.id.confirmation_text);
        tutorialDetailsTextView = findViewById(R.id.tutorial_details_text);
        paymentDetailsTextView = findViewById(R.id.payment_details_text);
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

        confirmationTextView.setText("Registration Successful!");

//        Payment time
        String formattedDate = "N/A";
        if (paymentTime != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = inputFormat.parse(paymentTime);

                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.US);
                outputFormat.setTimeZone(TimeZone.getDefault());
                formattedDate = outputFormat.format(date);
            } catch (ParseException e) {
                formattedDate = paymentTime;
            }
        }

//        Payment details based on fee
        String paymentDetails;
        if (tutorialFee.equals("0") || tutorialFee.equals("0.0") || tutorialFee.equals("0.00")) {
            paymentDetails = "Tutorial Type: Free\n" +
                    "Registration ID: " + (paymentId != null ? paymentId : "N/A") + "\n" +
                    "Registration Date: " + formattedDate;
        } else {
            paymentDetails = "Payment ID: " + (paymentId != null ? paymentId : "N/A") + "\n" +
                    "Payment Date: " + formattedDate + "\n" +
                    "Amount Paid: $" + tutorialFee + " CAD";
        }
        paymentDetailsTextView.setText(paymentDetails);

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
                        String tutorName = dataSnapshot.child("name").getValue(String.class);
                        String location = dataSnapshot.child("location").getValue(String.class);
                        String city = dataSnapshot.child("city").getValue(String.class);
                        String province = dataSnapshot.child("province").getValue(String.class);
                        String duration = dataSnapshot.child("duration").getValue(String.class);
                        StringBuilder details = new StringBuilder();
                        details.append("Tutorial: ").append(tutorialTitle).append("\n\n");

                        if (tutorName != null) {
                            details.append("Tutor: ").append(tutorName).append("\n\n");
                        }

                        details.append("Location: ");
                        if (city != null) {
                            details.append(city);
                            if (province != null) details.append(", ").append(province);
                        } else if (location != null) {
                            details.append(location);
                        } else {
                            details.append("N/A");
                        }
                        details.append("\n\n");

                        if (duration != null) {
                            details.append("Duration: ").append(duration).append(" minutes");
                        }

                        tutorialDetailsTextView.setText(details.toString());
                    }
                }

//                onCancelled() method
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    String basicDetails = "Tutorial: " + tutorialTitle;
                    tutorialDetailsTextView.setText(basicDetails);
                }
            });
        } else {

//            If tutorialID is null, default info
            String basicDetails = "Tutorial: " + tutorialTitle;
            tutorialDetailsTextView.setText(basicDetails);
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
}
