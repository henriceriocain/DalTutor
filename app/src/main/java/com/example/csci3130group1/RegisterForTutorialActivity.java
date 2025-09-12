package com.example.csci3130group1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// RegisterForTutorialActivity class
public class RegisterForTutorialActivity extends AppCompatActivity {

//    Attributes
    private static final String TAG = "RegisterForTutorial";
    private static PayPalConfiguration payPalConfig = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId("ATuTbWBd01dbfaC69Dz6llsOmqCpQ_S0UxMWYY0X1JGmm5pBUyZWoWzJPawuVYp7cCatdZ-_qUH4qW4n");
    private Button payWithPayPalButton;
    private Button cancelButton;
    private String tutorialId;
    private String tutorialTitle;
    private String tutorialFee;
    private View paypalDisclaimerCard;
    // Views for copied tutorial details component
    private TextView registerTutorialName;
    private TextView registerTutorialSubject;
    private TextView registerTutorialDate;
    private TextView registerTutorialTime;
    private TextView registerTutorialLocation;
    private TextView registerTutorialFee;
    private TextView registerTutorName;
    private View registerAvailabilityRow;
    private TextView registerTutorialCapacity;
    private TextView registerTutorialSpotsLeft;
    private View registerDescriptionSection;
    private TextView registerTutorialDescription;

//    onCreate() method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_for_tutorial);

//        Initializes UI elements
        payWithPayPalButton = findViewById(R.id.pay_with_paypal_button);
        cancelButton = findViewById(R.id.cancel_button);
        paypalDisclaimerCard = findViewById(R.id.paypal_disclaimer_card);
        // Bind tutorial details component views (shared component)
        registerTutorialName = findViewById(R.id.tutorialName);
        registerTutorialSubject = findViewById(R.id.tutorialSubject);
        registerTutorialDate = findViewById(R.id.tutorialDate);
        registerTutorialTime = findViewById(R.id.tutorialTime);
        registerTutorialLocation = findViewById(R.id.tutorialLocation);
        registerTutorialFee = findViewById(R.id.tutorialFee);
        // Tutor name row not part of shared component; skip binding
        registerAvailabilityRow = findViewById(R.id.availabilityRow);
        registerTutorialCapacity = findViewById(R.id.tutorialCapacity);
        registerTutorialSpotsLeft = findViewById(R.id.tutorialSpotsLeft);
        registerDescriptionSection = findViewById(R.id.descriptionSection);
        registerTutorialDescription = findViewById(R.id.tutorialDescription);

//        Starts Paypal service
        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, payPalConfig);
        startService(intent);

        tutorialId = getIntent().getStringExtra("tutorialId");
        tutorialTitle = getIntent().getStringExtra("tutorialTitle");
        tutorialFee = getIntent().getStringExtra("tutorialFee");

        if (tutorialId == null || tutorialTitle == null || tutorialFee == null) {
            Toast.makeText(this, "Tutorial information is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

//        Load detailed tutorial information from Firebase
        loadTutorialDetails();

//        Sets up paypal button
        payWithPayPalButton.setOnClickListener(v -> processPayment());

//        Sets up cancel button
        cancelButton.setOnClickListener(v -> finish());
    }


//    loadTutorialDetails() method to get info from firebase
    private void loadTutorialDetails() {
        DatabaseReference tutorialRef = FirebaseDatabase.getInstance()
                .getReference("tutorial_sessions")
                .child(tutorialId);

        tutorialRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

//                Extracts all available details based on current structure
                if (dataSnapshot.exists()) {
                    String tutorialName = dataSnapshot.child("tutorialName").getValue(String.class);
                    String tutorName = dataSnapshot.child("tutorName").getValue(String.class);
                    String address = dataSnapshot.child("address").getValue(String.class);
                    String placeId = dataSnapshot.child("placeId").getValue(String.class);
                    String description = dataSnapshot.child("description").getValue(String.class);
                    String date = dataSnapshot.child("date").getValue(String.class);
                    String startTime = dataSnapshot.child("startTime").getValue(String.class);
                    String endTime = dataSnapshot.child("endTime").getValue(String.class);
                    String topic = dataSnapshot.child("topic").getValue(String.class);
                    String feeStr = dataSnapshot.child("fee").getValue(String.class);

                    if (tutorialName != null && !tutorialName.isEmpty()) {
                        tutorialTitle = tutorialName;
                    }
                    if (feeStr != null && !feeStr.isEmpty()) {
                        tutorialFee = feeStr;
                    }

                    StringBuilder summary = new StringBuilder();
                    summary.append("Tutorial: ").append(tutorialTitle).append("\n");
                    if (registerTutorialName != null) registerTutorialName.setText(tutorialTitle);
                    // Tutor name lives in separate card on details; we don't render it here

                    if ((date != null && !date.isEmpty()) || (startTime != null && !startTime.isEmpty())) {
                        summary.append("When: ");
                        if (date != null && !date.isEmpty()) summary.append(date);
                        if (startTime != null && !startTime.isEmpty()) {
                            summary.append(date != null && !date.isEmpty() ? ", " : "");
                            summary.append(startTime);
                            if (endTime != null && !endTime.isEmpty()) summary.append(" – ").append(endTime);
                        }
                        summary.append("\n");
                        if (registerTutorialDate != null) registerTutorialDate.setText(date != null ? date : "—");
                        if (registerTutorialTime != null) {
                            String tm = (startTime != null ? startTime : "");
                            if (endTime != null && !endTime.isEmpty() && tm.length() > 0) tm += " – " + endTime;
                            registerTutorialTime.setText(tm.length() > 0 ? tm : "—");
                        }
                    }

                    if (address != null && !address.isEmpty()) {
                        summary.append("Where: ").append(address);
                        if (placeId != null && !placeId.isEmpty()) summary.append(" (" ).append(placeId).append(")");
                        summary.append("\n");
                        if (registerTutorialLocation != null) registerTutorialLocation.setText(address);
                    }

                    if (topic != null && !topic.isEmpty()) {
                        summary.append("Topic: ").append(topic).append("\n");
                        if (registerTutorialSubject != null) {
                            registerTutorialSubject.setText(topic);
                            registerTutorialSubject.setVisibility(View.VISIBLE);
                        }
                    }

                    boolean isFree = isTutorialFree(tutorialFee);
                    if (isFree) {
                        summary.append("Fee: FREE");
                        payWithPayPalButton.setText("Register");
                        if (registerTutorialFee != null) registerTutorialFee.setText("FREE");
                    } else {
                        summary.append("Fee: $").append(tutorialFee);
                        if (registerTutorialFee != null) registerTutorialFee.setText("$" + tutorialFee);
                    }
                    // Show disclaimer regardless of free/paid, per requirement
                    if (paypalDisclaimerCard != null) paypalDisclaimerCard.setVisibility(View.VISIBLE);

                    if (description != null && !description.isEmpty()) {
                        summary.append("\n\nDescription: ").append(description);
                        if (registerDescriptionSection != null) registerDescriptionSection.setVisibility(View.VISIBLE);
                        if (registerTutorialDescription != null) registerTutorialDescription.setText(description);
                    } else {
                        if (registerDescriptionSection != null) registerDescriptionSection.setVisibility(View.GONE);
                    }

                    // Capacity + spots left
                    Long capVal = dataSnapshot.child("capacity").getValue(Long.class);
                    long capacity = capVal != null ? capVal : 0L;
                    long registeredCount = dataSnapshot.child("registeredStudents").getChildrenCount();
                    if (capacity > 0) {
                        if (registerAvailabilityRow != null) registerAvailabilityRow.setVisibility(View.VISIBLE);
                        long remaining = Math.max(capacity - registeredCount, 0);
                        if (registerTutorialCapacity != null) registerTutorialCapacity.setText(String.valueOf(capacity));
                        if (registerTutorialSpotsLeft != null) {
                            registerTutorialSpotsLeft.setText(remaining + " of " + capacity);
                            registerTutorialSpotsLeft.setTextColor(getCapacityColor(remaining, capacity));
                        }
                    } else {
                        if (registerAvailabilityRow != null) registerAvailabilityRow.setVisibility(View.GONE);
                    }

                    // Legacy summary text view removed; UI populated via shared component fields
                } else {
                    displayBasicSummary();
                }
            }

//            onCancelled() method
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load tutorial details: " + databaseError.getMessage());
                displayBasicSummary();
            }
        });
    }

//    displayBasicSummary() method to get base case info from firebase
    private void displayBasicSummary() {
        String summary;
        boolean isFree = isTutorialFree(tutorialFee);

        if (isFree) {
            summary = "Tutorial: " + tutorialTitle + "\n\n" + "Fee: FREE";
            payWithPayPalButton.setText("Register");
        } else {
            summary = "Tutorial: " + tutorialTitle + "\n\n" + "Fee: $" + tutorialFee;
        }
        if (paypalDisclaimerCard != null) paypalDisclaimerCard.setVisibility(View.VISIBLE);
        // Legacy summary text view removed; UI populated via shared component fields
    }

//    isTutorialFree() method to check if tutorial is free
    private boolean isTutorialFree(String fee) {
        try {
            String cleanFee = fee.replaceAll("[^\\d.]", "");
            return cleanFee.equals("0") || cleanFee.equals("0.0") ||
                    cleanFee.equals("0.00") || cleanFee.isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "Error checking if tutorial is free", e);
            return false;
        }
    }

//    processPayment() method
    private void processPayment() {
        try {
            String cleanFee = tutorialFee.replaceAll("[^\\d.]", "");
            Log.d(TAG, "Processing payment with fee: " + cleanFee);

            // Always check capacity before proceeding (free or paid)
            checkCapacityAndProceed(isTutorialFree(tutorialFee), cleanFee);

        } catch (Exception e) {
            Log.e(TAG, "Error in processPayment: " + e.getMessage(), e);
            Toast.makeText(this, "Error processing payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCapacityAndProceed(boolean isFree, String cleanFee) {
        DatabaseReference tutorialRef = FirebaseDatabase.getInstance()
                .getReference("tutorial_sessions")
                .child(tutorialId);

        tutorialRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long capVal = snapshot.child("capacity").getValue(Long.class);
                long capacity = capVal != null ? capVal : 0L; // 0/unset = unlimited
                long registeredCount = snapshot.child("registeredStudents").getChildrenCount();

                if (capacity > 0 && registeredCount >= capacity) {
                    Toast.makeText(RegisterForTutorialActivity.this, "This tutorial is full.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (isFree) {
                    try {
                        JSONObject mockPayment = new JSONObject();
                        JSONObject response = new JSONObject();
                        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
                        response.put("id", "FREE_TUTORIAL_" + System.currentTimeMillis());
                        response.put("state", "approved");
                        response.put("create_time", timestamp);
                        mockPayment.put("response", response);

                        saveRegistrationToDatabase(mockPayment);
                        showConfirmation(mockPayment);
                    } catch (JSONException e) {
                        Toast.makeText(RegisterForTutorialActivity.this, "Error preparing registration", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Paid flow — start PayPal
                    PayPalPayment payment = new PayPalPayment(
                            new BigDecimal(cleanFee),
                            "CAD",
                            "Tutorial: " + tutorialTitle,
                            PayPalPayment.PAYMENT_INTENT_SALE
                    );
                    Intent intent = new Intent(RegisterForTutorialActivity.this, PaymentActivity.class);
                    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, payPalConfig);
                    intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);
                    startActivityForResult(intent, 7171);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RegisterForTutorialActivity.this, "Unable to check availability", Toast.LENGTH_SHORT).show();
            }
        });
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
        int a = (int) (android.graphics.Color.alpha(startColor) + (android.graphics.Color.alpha(endColor) - android.graphics.Color.alpha(startColor)) * t);
        int r = (int) (android.graphics.Color.red(startColor) + (android.graphics.Color.red(endColor) - android.graphics.Color.red(startColor)) * t);
        int g = (int) (android.graphics.Color.green(startColor) + (android.graphics.Color.green(endColor) - android.graphics.Color.green(startColor)) * t);
        int b = (int) (android.graphics.Color.blue(startColor) + (android.graphics.Color.blue(endColor) - android.graphics.Color.blue(startColor)) * t);
        return android.graphics.Color.argb(a, r, g, b);
    }

//    onActivityResult() method
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 7171) {
            if (resultCode == RESULT_OK) {
                PaymentConfirmation confirmation = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if (confirmation != null) {

//                    Tries to get payment details, stores in firebase and navigates to confirmation screen
                    try {
                        String paymentDetails = confirmation.toJSONObject().toString(4);
                        JSONObject paymentJson = new JSONObject(paymentDetails);
                        Log.d(TAG, "Payment details: " + paymentDetails);
                        saveRegistrationToDatabase(paymentJson);
                        showConfirmation(paymentJson);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: ", e);
                        Toast.makeText(this, "Error processing payment", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show();
            } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
                Toast.makeText(this, "Invalid payment configuration", Toast.LENGTH_SHORT).show();
            }
        }
    }

//    saveRegistrationToDatabase() method
    private void saveRegistrationToDatabase(JSONObject paymentJson) {

//        Gets current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User authentication required", Toast.LENGTH_SHORT).show();
            return;
        }

//        Firebase ref
        DatabaseReference registrationsRef = FirebaseDatabase.getInstance().getReference("registrations");
        String registrationId = registrationsRef.push().getKey();

//        Registration data
        Map<String, Object> registrationData = new HashMap<>();
        registrationData.put("tutorialId", tutorialId);
        registrationData.put("tutorialTitle", tutorialTitle);
        registrationData.put("fee", tutorialFee);
        registrationData.put("userId", currentUser.getUid());
        registrationData.put("userEmail", currentUser.getEmail());
        registrationData.put("timestamp", new Date().getTime());

//        Tries to add payment ID
        try {
            JSONObject response = paymentJson.getJSONObject("response");
            if (response.has("id")) {
                registrationData.put("paymentId", response.getString("id"));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to extract payment ID", e);
        }

//        Saves to firebase
        if (registrationId != null) {
            // Reserve a seat atomically under the tutorial node to enforce capacity
            DatabaseReference tutorialRef = FirebaseDatabase.getInstance()
                    .getReference("tutorial_sessions").child(tutorialId);

            tutorialRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData currentData) {
                    // Read capacity
                    Long capVal = currentData.child("capacity").getValue(Long.class);
                    long capacity = capVal != null ? capVal : 0L; // 0/unset = unlimited

                    // Ensure registeredStudents map exists
                    com.google.firebase.database.MutableData regNode = currentData.child("registeredStudents");
                    long count = 0;
                    for (com.google.firebase.database.MutableData ignored : regNode.getChildren()) {
                        count++;
                    }

                    // If already registered, abort
                    Boolean already = regNode.child(currentUser.getUid()).getValue(Boolean.class);
                    if (already != null && already) {
                        return com.google.firebase.database.Transaction.abort();
                    }

                    // Enforce capacity if set
                    if (capacity > 0 && count >= capacity) {
                        return com.google.firebase.database.Transaction.abort();
                    }

                    // Reserve seat
                    regNode.child(currentUser.getUid()).setValue(Boolean.TRUE);
                    return com.google.firebase.database.Transaction.success(currentData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                    if (committed) {
                        // Proceed to write registration records
                        registrationsRef.child(registrationId).setValue(registrationData)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Registration saved successfully"))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to save registration", e));

                        // Add registration to user's registration
                        DatabaseReference userRegistrationsRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(currentUser.getUid())
                                .child("registrations");
                        userRegistrationsRef.child(registrationId).setValue(true);

                        // Notify the tutor who owns this tutorial about the new registration
                        DatabaseReference notifyRef = FirebaseDatabase.getInstance()
                                .getReference("tutorial_sessions").child(tutorialId);
                        notifyRef.child("tutorId").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                String tutorId = snapshot.getValue(String.class);
                                if (tutorId != null) {
                                    Map<String, Object> notif = new HashMap<>();
                                    notif.put("type", "REGISTRATION_CREATED");
                                    notif.put("registrationId", registrationId);
                                    notif.put("tutorialId", tutorialId);
                                    notif.put("tutorialTitle", tutorialTitle);
                                    notif.put("studentUserId", currentUser.getUid());
                                    notif.put("studentEmail", currentUser.getEmail());
                                    notif.put("timestamp", new Date().getTime());
                                    notif.put("read", false);

                                    FirebaseDatabase.getInstance().getReference("users")
                                            .child(tutorId)
                                            .child("notifications")
                                            .push()
                                            .setValue(notif);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) { }
                        });
                    } else {
                        Toast.makeText(RegisterForTutorialActivity.this, "Unable to register: session is full or already registered.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

//    showConfirmation() method
    private void showConfirmation(JSONObject paymentJson) {

//        Intent info
        Intent intent = new Intent(this, RegistrationConfirmationActivity.class);
        intent.putExtra("tutorialId", tutorialId);
        intent.putExtra("tutorialTitle", tutorialTitle);
        intent.putExtra("tutorialFee", tutorialFee);

//        Tries to add payment ID
        try {
            JSONObject response = paymentJson.getJSONObject("response");
            if (response.has("id")) {
                intent.putExtra("paymentId", response.getString("id"));
            }
            if (response.has("create_time")) {
                intent.putExtra("paymentTime", response.getString("create_time"));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to extract payment details", e);
        }

//        Closes paypal
        stopService(new Intent(this, PayPalService.class));

//        Opens confirmation activity
        startActivity(intent);

//        Finish current activity
        finish();
    }

    //    onDestroy() method
    @Override
    protected void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }
}
