package com.example.csci3130group1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editUsername, editDegree, editStudentDescription;
    private TextView currentEmail;
    private Button saveButton, cancelButton;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        
        userId = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Initialize views
        initializeViews();
        
        // Load current user data
        loadCurrentUserData();
        
        // Set up button listeners
        setupButtonListeners();
    }

    private void initializeViews() {
        editUsername = findViewById(R.id.editUsername);
        editDegree = findViewById(R.id.editDegree);
        editStudentDescription = findViewById(R.id.editStudentDescription);
        currentEmail = findViewById(R.id.currentEmail);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
    }

    private void loadCurrentUserData() {
        // Set email from Firebase Auth
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            currentEmail.setText(currentUser.getEmail());
        }

        // Load user data from database
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Load existing data
                    String name = snapshot.child("name").getValue(String.class);
                    String degree = snapshot.child("degree").getValue(String.class);
                    String studentDescription = snapshot.child("studentDescription").getValue(String.class);

                    // Set the data in the fields
                    if (name != null) {
                        editUsername.setText(name);
                    }
                    if (degree != null) {
                        editDegree.setText(degree);
                    }
                    if (studentDescription != null) {
                        editStudentDescription.setText(studentDescription);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this, "Error loading profile data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupButtonListeners() {
        saveButton.setOnClickListener(view -> saveProfileChanges());
        cancelButton.setOnClickListener(view -> finish());
    }

    private void saveProfileChanges() {
        // Get the entered values
        String username = editUsername.getText().toString().trim();
        String degree = editDegree.getText().toString().trim();
        String studentDescription = editStudentDescription.getText().toString().trim();

        // Validate required fields
        if (username.isEmpty()) {
            editUsername.setError("Username is required");
            editUsername.requestFocus();
            return;
        }

        // Create update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", username);
        
        // Only add degree if it's not empty
        if (!degree.isEmpty()) {
            updates.put("degree", degree);
        } else {
            // Remove degree field if empty
            updates.put("degree", null);
        }
        
        // Only add student description if it's not empty
        if (!studentDescription.isEmpty()) {
            updates.put("studentDescription", studentDescription);
        } else {
            // Remove student description field if empty
            updates.put("studentDescription", null);
        }

        // Save to Firebase
        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to profile tab
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfileActivity.this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                    saveButton.setText("Save Changes");
                });
    }

    @Override
    public void onBackPressed() {
        // Allow back navigation without saving
        super.onBackPressed();
    }
}