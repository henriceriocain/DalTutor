package com.example.csci3130group1;



import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivityStudent extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView welcomeText;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();

        welcomeText = findViewById(R.id.welcome_text);
        logoutButton = findViewById(R.id.logout_button);
// NEW: Get username, role, and password from intent
        String username = getIntent().getStringExtra("username");
        String role = getIntent().getStringExtra("role");
        String password = getIntent().getStringExtra("password");

        // NEW: Display personalized welcome and toast
        if (username != null && role != null) {
            welcomeText.setText("Hello and welcome " + username + "! You are logged in as a " + role);
            Toast.makeText(this, username + "-" + password + "-" + role, Toast.LENGTH_LONG).show();
        }
        /* Get username from intent
        String username = getIntent().getStringExtra("username");
        if (username != null) {
            welcomeText.setText("Hello, " + username);
        }*/

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(HomeActivityStudent.this, LoginActivity.class));
            finish();
        });
    }
}


