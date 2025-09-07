package com.example.csci3130group1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editUsername, editDegree, editContactNumber, editStudentDescription;
    private TextView currentEmail;
    private ImageView profilePicturePreview;
    private Button saveButton, cancelButton, changeProfilePictureButton;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private StorageReference storageRef;
    private String userId;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

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
        storageRef = FirebaseStorage.getInstance().getReference().child("profile_pictures");
        
        // Initialize image picker
        initializeImagePicker();
        
        // Handle back button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

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
        editContactNumber = findViewById(R.id.editContactNumber);
        editStudentDescription = findViewById(R.id.editStudentDescription);
        currentEmail = findViewById(R.id.currentEmail);
        profilePicturePreview = findViewById(R.id.profilePicturePreview);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        changeProfilePictureButton = findViewById(R.id.changeProfilePictureButton);
    }
    
    private void initializeImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        // Log the URI for debugging
                        Toast.makeText(this, "Selected image URI: " + selectedImageUri.toString(), Toast.LENGTH_SHORT).show();
                        
                        // Display the selected image
                        try {
                            Glide.with(this)
                                .load(selectedImageUri)
                                .circleCrop()
                                .placeholder(R.drawable.circle_background)
                                .error(R.drawable.circle_background)
                                .into(profilePicturePreview);
                        } catch (Exception e) {
                            Toast.makeText(this, "Error loading image preview: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Image selection cancelled", Toast.LENGTH_SHORT).show();
                }
            }
        );
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
                    String contactNumber = snapshot.child("contact").getValue(String.class);
                    String studentDescription = snapshot.child("studentDescription").getValue(String.class);
                    String profilePictureUrl = snapshot.child("profilePictureUrl").getValue(String.class);

                    // Set the data in the fields
                    if (name != null) {
                        editUsername.setText(name);
                    }
                    if (degree != null) {
                        editDegree.setText(degree);
                    }
                    if (contactNumber != null) {
                        editContactNumber.setText(contactNumber);
                    }
                    if (studentDescription != null) {
                        editStudentDescription.setText(studentDescription);
                    }
                    
                    // Load existing profile picture
                    if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                        Glide.with(EditProfileActivity.this)
                            .load(profilePictureUrl)
                            .circleCrop()
                            .into(profilePicturePreview);
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
        changeProfilePictureButton.setOnClickListener(view -> openImagePicker());
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Profile Picture"));
    }

    private void saveProfileChanges() {
        // Get the entered values
        String username = editUsername.getText().toString().trim();
        String degree = editDegree.getText().toString().trim();
        String contactNumber = editContactNumber.getText().toString().trim();
        String studentDescription = editStudentDescription.getText().toString().trim();

        // Validate required fields
        if (username.isEmpty()) {
            editUsername.setError("Username is required");
            editUsername.requestFocus();
            return;
        }
        
        if (contactNumber.isEmpty()) {
            editContactNumber.setError("Contact number is required");
            editContactNumber.requestFocus();
            return;
        }
        
        // Validate phone number format
        if (!isValidPhoneNumber(contactNumber)) {
            editContactNumber.setError("Please enter exactly 10 digits");
            editContactNumber.requestFocus();
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
        
        // Contact number is required, so always add it
        updates.put("contact", contactNumber);
        
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
        
        // If user selected a new image, upload it first
        if (selectedImageUri != null) {
            uploadProfilePicture(updates);
        } else {
            // No new image, just update the profile
            updateProfile(updates);
        }
    }
    
    private void uploadProfilePicture(Map<String, Object> updates) {
        if (selectedImageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            saveButton.setText("Save Changes");
            return;
        }
        
        // Check if we can access the image
        try {
            getContentResolver().openInputStream(selectedImageUri).close();
        } catch (Exception e) {
            Toast.makeText(this, "Cannot access selected image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            saveButton.setText("Save Changes");
            return;
        }
        
        String fileName = "profile_" + userId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference profilePicRef = storageRef.child(fileName);
        
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
        
        profilePicRef.putFile(selectedImageUri)
            .addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                saveButton.setText("Uploading " + (int)progress + "%");
            })
            .addOnSuccessListener(taskSnapshot -> {
                Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                // Get download URL
                profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    updates.put("profilePictureUrl", uri.toString());
                    updateProfile(updates);
                }).addOnFailureListener(e -> {
                    Toast.makeText(EditProfileActivity.this, "Error getting image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                    saveButton.setText("Save Changes");
                });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(EditProfileActivity.this, "Error uploading image: " + e.getMessage() + "\n\nPlease check if Firebase Storage is enabled in your Firebase console.", Toast.LENGTH_LONG).show();
                saveButton.setEnabled(true);
                saveButton.setText("Save Changes");
            });
    }
    
    private void updateProfile(Map<String, Object> updates) {
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
    
    private boolean isValidPhoneNumber(String phoneNumber) {
        // Must be exactly 10 digits, nothing else
        return phoneNumber.matches("\\d{10}");
    }

}