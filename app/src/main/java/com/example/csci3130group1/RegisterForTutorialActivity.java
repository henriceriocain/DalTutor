package com.example.csci3130group1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
    private static final int PAYPAL_REQUEST_CODE = 7171;
    private static final String CLIENT_ID = "ATuTbWBd01dbfaC69Dz6llsOmqCpQ_S0UxMWYY0X1JGmm5pBUyZWoWzJPawuVYp7cCatdZ-_qUH4qW4n";
    private static PayPalConfiguration payPalConfig = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(CLIENT_ID);
    private TextView tutorialSummaryTextView;
    private Button payWithPayPalButton;
    private Button cancelButton;
    private String tutorialId;
    private String tutorialTitle;
    private String tutorialFee;

//    onCreate() method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_for_tutorial);

//        Initializes UI elements
        tutorialSummaryTextView = findViewById(R.id.tutorial_summary_text);
        payWithPayPalButton = findViewById(R.id.pay_with_paypal_button);
        cancelButton = findViewById(R.id.cancel_button);

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

//        Free tutorial case
        boolean isFree = false;
        try {
            String cleanFee = tutorialFee.replaceAll("[^\\d.]", "");
            isFree = cleanFee.equals("0") || cleanFee.equals("0.0") || cleanFee.equals("0.00") || cleanFee.isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "Error checking if tutorial is free", e);
        }

//        Displays tutorial summary
        String summary;
        if (isFree) {
            summary = "Tutorial: " + tutorialTitle + "\n\n" + "Fee: FREE";
            payWithPayPalButton.setText("Register for Free Tutorial");
        } else {
            summary = "Tutorial: " + tutorialTitle + "\n\n" + "Fee: $" + tutorialFee;
        }
        tutorialSummaryTextView.setText(summary);

//        Sets up paypal button
        payWithPayPalButton.setOnClickListener(v -> processPayment());

//        Sets up cancel button
        cancelButton.setOnClickListener(v -> finish());
    }

//    processPayment() method
    private void processPayment() {
        try {

//            Cleans the fee
            String cleanFee = tutorialFee.replaceAll("[^\\d.]", "");

//            Debugging
            Log.d(TAG, "Processing payment with fee: " + cleanFee);

//            Free tutorial case
            if (cleanFee.equals("0") || cleanFee.equals("0.0") || cleanFee.equals("0.00") || cleanFee.isEmpty()) {
                Log.d(TAG, "Tutorial is free. Skipping PayPal and going straight to confirmation");

//                Creates mock payment data
                JSONObject mockPayment = new JSONObject();
                JSONObject response = new JSONObject();
                String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
                response.put("id", "FREE_TUTORIAL_" + System.currentTimeMillis());
                response.put("state", "approved");
                response.put("create_time", timestamp);
                mockPayment.put("response", response);

//                Saves data into firebase
                saveRegistrationToDatabase(mockPayment);

//                Shows confirmation screen
                showConfirmation(mockPayment);

                return;
            }

//            Paid tutorial case
            PayPalPayment payment = new PayPalPayment(
                    new BigDecimal(cleanFee),
                    "CAD",
                    "Tutorial: " + tutorialTitle,
                    PayPalPayment.PAYMENT_INTENT_SALE
            );

//            Creates the payment intent
            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, payPalConfig);
            intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);
            startActivityForResult(intent, PAYPAL_REQUEST_CODE);

        } catch (Exception e) {
            Log.e(TAG, "Error in processPayment: " + e.getMessage(), e);
            Toast.makeText(this, "Error processing payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

//    onActivityResult() method
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PAYPAL_REQUEST_CODE) {
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
            registrationsRef.child(registrationId).setValue(registrationData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Registration saved successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save registration", e));

//            Add registration to user's registration
            DatabaseReference userRegistrationsRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid())
                    .child("registrations");

            userRegistrationsRef.child(registrationId).setValue(true);

//            Adds students to tutorial's registered students
            DatabaseReference tutorialStudentsRef = FirebaseDatabase.getInstance()
                    .getReference("tutorial_sessions")
                    .child(tutorialId)
                    .child("registeredStudents");

            tutorialStudentsRef.child(currentUser.getUid()).setValue(true);
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