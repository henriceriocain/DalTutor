// Package
package com.example.csci3130group1;

// Import statements
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import android.util.Log;

// ForgotPasswordActivity class
public class ForgotPasswordActivity extends AppCompatActivity {

//    Attributes
    private EditText emailInput;
    private Button resetButton;

//    onCreate method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        emailInput = findViewById(R.id.email_input);
        resetButton = findViewById(R.id.reset_button);
        resetButton.setOnClickListener(v -> resetPassword());
    }

//    resetPassword method
    private void resetPassword() {

//        Variables
        String email = emailInput.getText().toString().trim();
        FirebaseAuth auth = FirebaseAuth.getInstance();

//        Conditionals to check for error cases
        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return;
        } else if (!isValidEmail(email)) {
            emailInput.setError("Valid email address is required");
            return;
        }

//        Sends reset email if email is within firebase
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("ForgotPassword", "Reset email sent.");
                        Toast.makeText(ForgotPasswordActivity.this,
                                "You will receive a password reset email if your email address is registered with us",
                                Toast.LENGTH_LONG).show();

//                        Adds a delay after toast and navigates to login page
                        new Handler().postDelayed(() -> {
                            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }, 2000);
                    }
                });
    }

//    isValidEmail method to check validity of emails
    public boolean isValidEmail(String email) {
//        Return the validity of the regular expression
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
}