package com.example.csci3130group1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    // Role spinner removed; unified login
    private TextView forgotPassword;
    private FirebaseAuth mAuth;
    private TextView registerText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.button2);
        registerText = findViewById(R.id.register_text);
        forgotPassword = findViewById(R.id.forgot_password);

        // Role selection removed

        loginButton.setOnClickListener(v -> authenticateUser());

        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        registerText.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

    }

    private void authenticateUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String role = null; // role removed

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return;
        }

        // Role selection removed; proceed without requiring it

        // Pass username, password, and selected role to HomeActivity
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        // Clear any previous session role for unified identity
                        com.example.csci3130group1.utils.SessionRole.clear(LoginActivity.this);

                        // Unified entry: route to StudentDashboard as the single dashboard
                        Intent intent = new Intent(LoginActivity.this, StudentDashboard.class);
                        intent.putExtra("username", email);
                        intent.putExtra("password", password);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
