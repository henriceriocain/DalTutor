package com.example.csci3130group1.ui.TutorialManagement;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.AdapterView;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.maps.model.LatLng;
import com.example.csci3130group1.utils.LocationSpinnerUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.csci3130group1.R;
import com.example.csci3130group1.databinding.FragmentTutorialManagementBinding;
import com.example.csci3130group1.ui.search_for_tutorials.Tutorial;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.android.volley.RequestQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.example.csci3130group1.utils.TutorialTimeUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TutorialManagementFragment extends Fragment implements TutorialPreviewDialogFragment.PreviewConfirmListener {
    private static final String CREDENTIALS_FILE_PATH = "key.json";
    private static final String PUSH_NOTIFICATION_ENDPOINT ="https://fcm.googleapis.com/v1/projects/csci3130w25-project-g1/messages:send";
    
    // UI components
    private FragmentTutorialManagementBinding binding;
    private TutorialManagementViewModel sessionViewModel;
    private EditText tutorialNameInput, feeInput, dateInput, startTimeInput, endTimeInput, descriptionInput;
    private Spinner locationSpinner;
    private ArrayAdapter<String> locationAdapter;
    private TextView selectedLocationText;
    private Spinner topicSpinner;
    private TextView previewText, tutorNameDisplay, locationStatusText;
    private TextView previewButton;
    
    // State management for preview/publish flow
    private boolean isPreviewConfirmed = false;
    
    // Location data
    private String selectedAddress = "";
    private LatLng selectedLatLng = null;
    private String placeId = "";
    private List<LocationSpinnerUtils.DalPlace> allPlaces = new ArrayList<>();
    
    // Firebase
    private RequestQueue requestQueue;
    private String sessionId;
    private FirebaseAuth mAuth;
    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference tutorialRef = rootRef.child("tutorial_sessions");


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d("PlacesDebug", "onCreateView() started");
        sessionViewModel = new ViewModelProvider(this).get(TutorialManagementViewModel.class);

        binding = FragmentTutorialManagementBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Log.d("PlacesDebug", "Fragment view inflated successfully");

        mAuth = FirebaseAuth.getInstance();
        
        // Initialize UI components
        tutorialNameInput = root.findViewById(R.id.tutorial_name_input);
        topicSpinner = root.findViewById(R.id.topic_spinner);
        feeInput = root.findViewById(R.id.fee_input);
        dateInput = root.findViewById(R.id.date_input);
        startTimeInput = root.findViewById(R.id.start_time_input);
        endTimeInput = root.findViewById(R.id.end_time_input);
        descriptionInput = root.findViewById(R.id.description_input);
        
        // Load Dal places and setup dropdown
        Log.d("PlacesDebug", "Loading Dal places from assets");
        allPlaces = LocationSpinnerUtils.loadDalPlaces(requireContext());
        
        locationSpinner = root.findViewById(R.id.location_spinner);
        setupLocationSpinner();
        
        selectedLocationText = root.findViewById(R.id.selected_location_text);
        locationStatusText = root.findViewById(R.id.location_status_text);
        tutorNameDisplay = root.findViewById(R.id.tutor_name_display);
        previewText = root.findViewById(R.id.preview_text);
        previewButton = root.findViewById(R.id.preview_button);

        loadUserNameFromFirebase();
        setupDateTimePickers();
        setupLocationPicker();

        previewButton.setOnClickListener(view -> handlePreviewButtonClick());
        
        // Set tutor name click listener to navigate to profile
        tutorNameDisplay.setOnClickListener(view -> {
            Navigation.findNavController(view).navigate(R.id.navigation_profile);
        });
        
        // Add field change listeners to reset preview state
        setupFieldChangeListeners();

        FirebaseMessaging.getInstance().subscribeToTopic("History");
        Log.d("PlacesDebug", "onCreateView() completed, returning root view");
        return root;
    }


    private void setupLocationSpinner() {
        Log.d("PlacesDebug", "Setting up Location Spinner with " + allPlaces.size() + " places");
        
        LocationSpinnerUtils.setupSpinner(
            requireContext(),
            locationSpinner,
            allPlaces,
            new LocationSpinnerUtils.LocationSelectionListener() {
                @Override
                public void onLocationSelected(LocationSpinnerUtils.DalPlace place) {
                    handleDalPlaceSelection(place);
                }
                
                @Override
                public void onLocationCleared() {
                    selectedAddress = "";
                    selectedLatLng = null;
                    placeId = "";
                    selectedLocationText.setVisibility(View.GONE);
                    locationStatusText.setText("Halifax, NS locations only");
                    locationStatusText.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }
            }
        );
        
        Log.d("PlacesDebug", "Location Spinner setup completed");
    }


    private void handleDalPlaceSelection(LocationSpinnerUtils.DalPlace place) {
        Log.d("PlacesDebug", "Handling Dal place selection: " + place.name);
        
        // Store location data
        selectedAddress = place.addr;
        selectedLatLng = place.getLatLng();
        placeId = place.getPlaceId();
        
        // Update UI
        selectedLocationText.setText(place.addr);
        selectedLocationText.setVisibility(View.VISIBLE);
        locationStatusText.setText("✓ " + place.name + " selected");
        locationStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        
        Log.d("PlacesDebug", "Dal place selection completed: " + place.name + " at " + place.lat + ", " + place.lon);
    }

    private void loadUserNameFromFirebase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null) {
                        tutorNameDisplay.setText(name);
                    } else {
                        tutorNameDisplay.setText("Name not found");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("TutorialManagement", "Failed to load user name: " + error.getMessage());
                    tutorNameDisplay.setText("Error loading name");
                }
            });
        }
    }

    private void setupDateTimePickers() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        // Date picker - handle both EditText and container clicks
        View.OnClickListener dateClickListener = v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (DatePicker view, int year, int month, int dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        dateInput.setText(dateFormat.format(selectedDate.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        };

        dateInput.setOnClickListener(dateClickListener);
        
        // Also set click listener on the container for better UX
        View dateContainer = binding.getRoot().findViewById(R.id.date_input_container);
        if (dateContainer != null) {
            dateContainer.setOnClickListener(dateClickListener);
        }

        // Start time picker - handle both EditText and container clicks
        View.OnClickListener startTimeClickListener = v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    getContext(),
                    (TimePicker view, int hourOfDay, int minute) -> {
                        Calendar selectedTime = Calendar.getInstance();
                        selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedTime.set(Calendar.MINUTE, minute);
                        startTimeInput.setText(timeFormat.format(selectedTime.getTime()));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true // 24-hour format
            );
            timePickerDialog.show();
        };

        startTimeInput.setOnClickListener(startTimeClickListener);
        
        View startTimeContainer = binding.getRoot().findViewById(R.id.start_time_container);
        if (startTimeContainer != null) {
            startTimeContainer.setOnClickListener(startTimeClickListener);
        }

        // End time picker - handle both EditText and container clicks
        View.OnClickListener endTimeClickListener = v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    getContext(),
                    (TimePicker view, int hourOfDay, int minute) -> {
                        Calendar selectedTime = Calendar.getInstance();
                        selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedTime.set(Calendar.MINUTE, minute);
                        endTimeInput.setText(timeFormat.format(selectedTime.getTime()));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true // 24-hour format
            );
            timePickerDialog.show();
        };

        endTimeInput.setOnClickListener(endTimeClickListener);
        
        View endTimeContainer = binding.getRoot().findViewById(R.id.end_time_container);
        if (endTimeContainer != null) {
            endTimeContainer.setOnClickListener(endTimeClickListener);
        }
    }

    private boolean isValidHalifaxPostalCode(String postalCode) {
        String[] halifaxPrefixes = {"B3H", "B3J", "B3K", "B3L", "B3M", "B3N", "B3P", "B3S", "B3T"};
        String cleanPostal = postalCode.toUpperCase().replaceAll("\\s", "");
        
        if (cleanPostal.length() < 3) return false;
        
        String prefix = cleanPostal.substring(0, 3);
        for (String validPrefix : halifaxPrefixes) {
            if (prefix.equals(validPrefix)) return true;
        }
        return false;
    }


    private void setupLocationPicker() {
        // Legacy method - now handled by setupLocationSpinner()
    }
    
    private void setupFieldChangeListeners() {
        // Reset preview state when any field changes
        tutorialNameInput.addTextChangedListener(new SimpleTextWatcher(() -> resetPreviewState()));
        feeInput.addTextChangedListener(new SimpleTextWatcher(() -> resetPreviewState()));
        dateInput.addTextChangedListener(new SimpleTextWatcher(() -> resetPreviewState()));
        startTimeInput.addTextChangedListener(new SimpleTextWatcher(() -> resetPreviewState()));
        endTimeInput.addTextChangedListener(new SimpleTextWatcher(() -> resetPreviewState()));
        descriptionInput.addTextChangedListener(new SimpleTextWatcher(() -> resetPreviewState()));
        
        // Note: Topic spinner listener will be handled separately to avoid conflicts
    }
    
    private void resetPreviewState() {
        if (isPreviewConfirmed) {
            isPreviewConfirmed = false;
            updateButtonState();
        }
    }
    
    private void updateButtonState() {
        if (isPreviewConfirmed) {
            previewButton.setText("Publish Tutorial");
            previewButton.setBackgroundResource(R.drawable.button_background);
        } else {
            previewButton.setText("Preview");
            previewButton.setBackgroundResource(R.drawable.button_background);
        }
    }
    
    private void handlePreviewButtonClick() {
        if (isPreviewConfirmed) {
            // Button is in "Publish" state - gate by tutor profile completeness
            ensureTutorEligibleThen(this::publishSession);
        } else {
            // Button is in "Preview" state - show preview dialog
            showPreviewDialog();
        }
    }

    private void ensureTutorEligibleThen(Runnable onEligible) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                Boolean enabled = snap.child("isTutorEnabled").getValue(Boolean.class);
                Boolean complete = snap.child("tutorProfileComplete").getValue(Boolean.class);
                boolean ok = (enabled != null && enabled) && (complete != null && complete);
                if (ok) {
                    if (onEligible != null) onEligible.run();
                } else {
                    new android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Complete your tutor profile")
                            .setMessage("Add a profile photo and contact info to publish tutorials.")
                            .setPositiveButton("Edit Profile", (d, w) -> {
                                startActivity(new android.content.Intent(getContext(), com.example.csci3130group1.EditProfileActivity.class));
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Unable to verify eligibility", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showPreviewDialog() {
        String tutorialName = tutorialNameInput.getText().toString();
        String topic = topicSpinner.getSelectedItem().toString();
        String fee = feeInput.getText().toString();
        String date = dateInput.getText().toString();
        String startTime = startTimeInput.getText().toString();
        String endTime = endTimeInput.getText().toString();
        String description = descriptionInput.getText().toString();
        String tutorName = tutorNameDisplay.getText().toString();
        
        // Validate all fields before showing preview
        if (tutorialName.isEmpty() || topic.isEmpty() || topic.equals("Select a topic...") || 
            fee.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || 
            description.isEmpty() || selectedAddress.isEmpty() || tutorName.isEmpty()) {
            
            showPreviewCard("Please fill all fields before previewing.");
            return;
        }
        
        if (selectedLatLng == null || !LocationSpinnerUtils.isLocationInHalifax(selectedLatLng)) {
            showPreviewCard("Please select a valid Halifax location.");
            return;
        }
        
        // Show the preview card with tutorial details
        String previewAddress = selectedAddress.contains(",") ? selectedAddress.split(",")[0] : selectedAddress;
        String previewContent = String.format("📚 %s\n\n👨‍🏫 Tutor: %s\n📖 Subject: %s\n💰 Fee: $%s\n📅 Date: %s\n⏰ Time: %s - %s\n📍 Location: %s\n\n📝 Description:\n%s", 
            tutorialName, tutorName, topic, fee, date, startTime, endTime, previewAddress, description);
        showPreviewCard(previewContent);
        
        // Also show dialog with preview
        TutorialPreviewDialogFragment dialog = TutorialPreviewDialogFragment.newInstance(
            tutorialName, tutorName, topic, fee, date, startTime, endTime, description, previewAddress
        );
        dialog.setPreviewConfirmListener(this);
        dialog.show(getParentFragmentManager(), "tutorial_preview");
    }
    
    private void showPreviewCard(String content) {
        previewText.setText(content);
        View previewCard = binding.getRoot().findViewById(R.id.preview_card);
        if (previewCard != null) {
            previewCard.setVisibility(View.VISIBLE);
        } else {
            // Fallback to old preview text if new card not found
            previewText.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onPreviewConfirmed() {
        isPreviewConfirmed = true;
        updateButtonState();
    }
    
    // Simple TextWatcher helper class
    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable callback;
        
        public SimpleTextWatcher(Runnable callback) {
            this.callback = callback;
        }
        
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            callback.run();
        }
        
        @Override
        public void afterTextChanged(android.text.Editable s) {}
    }

    // Legacy manual address entry methods removed - now using Dal Places Autocomplete

    private void previewSession() {
        String tutorialName = tutorialNameInput.getText().toString();
        String topic = topicSpinner.getSelectedItem().toString();
        String fee = feeInput.getText().toString();
        String date = dateInput.getText().toString();
        String startTime = startTimeInput.getText().toString();
        String endTime = endTimeInput.getText().toString();
        String description = descriptionInput.getText().toString();
        String name = tutorNameDisplay.getText().toString();

        if (tutorialName.isEmpty() || topic.isEmpty() || topic.equals("Select a topic...") || fee.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || description.isEmpty() || selectedAddress.isEmpty() || name.isEmpty()) {
            previewText.setText("Preview: Please fill all fields.");
            previewText.setVisibility(View.VISIBLE);
        } else {
            if (selectedLatLng == null || !LocationSpinnerUtils.isLocationInHalifax(selectedLatLng)) {
                previewText.setText("Preview: Please select a valid Halifax location");
                previewText.setVisibility(View.VISIBLE);
                return;
            }
            
            String preview = "Preview Session:\n"
                    + "Tutorial: " + tutorialName + "\n"
                    + "Tutor: " + name + "\n"
                    + "Topic: " + topic + "\n"
                    + "Total Fee: $" + fee + "\n"
                    + "Date: " + date + "\n"
                    + "Time: " + startTime + " - " + endTime + "\n"
                    + "Description: " + description + "\n"
                    + "Location: " + selectedAddress;
            previewText.setText(preview);
            previewText.setVisibility(View.VISIBLE);
        }
    }

    private void getAccessToken(Context context, AccessTokenListener listener) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                InputStream serviceAccountStream = context.getAssets().open(CREDENTIALS_FILE_PATH);
                GoogleCredentials googleCredentials = GoogleCredentials
                        .fromStream(serviceAccountStream)
                        .createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));

                googleCredentials.refresh();
                String token = googleCredentials.getRequestMetadata().get("Authorization").get(0).replace("Bearer ", "");
                listener.onAccessTokenReceived(token);
                Log.d("token","token"+token);
            } catch (IOException e) {
                Looper.prepare();
                listener.onAccessTokenError(e);
            }
        });
        executorService.shutdown();
    }
    
    private void sendNotification(String authToken) {
        try {
            // Build the notification payload
                JSONObject JSONBody = new JSONObject();
                JSONBody.put("title", "A tutorial that matches your preferences has been posted");
                JSONBody.put("body", "Click here to see the mentioned tutorial");
                JSONObject dataJSONBody = new JSONObject();
                dataJSONBody.put("tutorialId", this.sessionId);
                dataJSONBody.put("tutorialName", tutorialNameInput.getText().toString());
                dataJSONBody.put("tutorName", tutorNameDisplay.getText().toString());
                dataJSONBody.put("topic", topicSpinner.getSelectedItem().toString());
                // Clean up address for notification
                String notificationAddress = selectedAddress;
                if (selectedAddress.contains(",")) {
                    notificationAddress = selectedAddress.split(",")[0];
                }
                dataJSONBody.put("address", notificationAddress);
                dataJSONBody.put("latitude", selectedLatLng.latitude);
                dataJSONBody.put("longitude", selectedLatLng.longitude);
                dataJSONBody.put("fee", feeInput.getText().toString());
                dataJSONBody.put("date", dateInput.getText().toString());
                dataJSONBody.put("startTime", startTimeInput.getText().toString());
                dataJSONBody.put("endTime", endTimeInput.getText().toString());
                dataJSONBody.put("description", descriptionInput.getText().toString());
                JSONObject messageJSONBody = new JSONObject();
                messageJSONBody.put("topic", topicSpinner.getSelectedItem().toString());
                messageJSONBody.put("notification", JSONBody);
                messageJSONBody.put("data", dataJSONBody);

                JSONObject pushNotificationJSONBody = new JSONObject();
                pushNotificationJSONBody.put("message", messageJSONBody);

            // Create the request
                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        PUSH_NOTIFICATION_ENDPOINT,
                        pushNotificationJSONBody,
                        response -> {
                            Toast.makeText(this.getContext(), "Notification Sent Successfully", Toast.LENGTH_SHORT).show();
                        },
                        error -> {
                            if (error.networkResponse != null) {
                                Log.e("NetworkLog", "Status Code: " + error.networkResponse.statusCode);
                                Log.e("NetworkLog", "Error Data: " + new String(error.networkResponse.data));
                            }
                            Toast.makeText(this.getContext(), "Failed to Send Notification", Toast.LENGTH_SHORT).show();
                            error.printStackTrace();
                        }) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Content-Type", "application/json; charset=UTF-8");
                        headers.put("Authorization", "Bearer " + authToken);
                        Log.d("NotificationHeaders", "Headers: " + headers.toString());
                        return headers;
                    }
                };
                // Add the request to the queue
                requestQueue.add(request);
        } catch (JSONException e) {
            Log.e("NotificationJSONException", "Error creating notification JSON: " + e.getMessage());
            Toast.makeText(this.getContext(), "Error creating notification payload", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void publishSession() {
        String tutorialName = tutorialNameInput.getText().toString();
        String topic = topicSpinner.getSelectedItem().toString();
        String fee = feeInput.getText().toString();
        String date = dateInput.getText().toString();
        String startTime = startTimeInput.getText().toString();
        String endTime = endTimeInput.getText().toString();
        String description = descriptionInput.getText().toString();
        String tutorName = tutorNameDisplay.getText().toString();
        
        // Get current user's ID
        String tutorId = null;
        if (mAuth.getCurrentUser() != null) {
            tutorId = mAuth.getCurrentUser().getUid();
        }
        
        // Clean up the address - remove Halifax, NS part
        String cleanAddress = selectedAddress;
        if (selectedAddress.contains(",")) {
            cleanAddress = selectedAddress.split(",")[0]; // Get just the street address
        }

        if (tutorialName.isEmpty() || topic.isEmpty() || topic.equals("Select a topic...") || fee.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty()  || description.isEmpty() || selectedAddress.isEmpty() || tutorName.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedLatLng == null || !LocationSpinnerUtils.isLocationInHalifax(selectedLatLng)) {
            Toast.makeText(getContext(), "Please select a valid Halifax location", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        this.sessionId = databaseRef.push().getKey();

        // Create tutorial with new improved structure
        Tutorial tutorial = new Tutorial(tutorialName, topic, fee, date, startTime, endTime, description, 
                                        cleanAddress, selectedLatLng.latitude, selectedLatLng.longitude, placeId, 
                                        tutorName, tutorId, null); // tutorDegree will be null for now

        if (sessionId != null) {
            databaseRef.child(sessionId).setValue(tutorial).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Optionally write a numeric endTimestamp for robust future checks
                    Long endTimestamp = TutorialTimeUtils.parseEndMillis(date, endTime);
                    if (endTimestamp != null) {
                        databaseRef.child(sessionId).child("endTimestamp").setValue(endTimestamp);
                    }
                    Toast.makeText(getContext(), "Session Published Successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Exception e = task.getException();
                    if (e != null) {
                        Log.e("FirebaseError", "Publishing Failed: " + e.getMessage());
                        Toast.makeText(getContext(), "Failed to Publish Session: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to Publish Session: Unknown Error", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            Toast.makeText(getContext(), "Session ID generation failed!", Toast.LENGTH_LONG).show();
        }

        requestQueue = Volley.newRequestQueue(getContext());
        getAccessToken(getContext(), new AccessTokenListener() {
            @Override
            public void onAccessTokenReceived(String token) {
                // When the token is received, send the notification
                sendNotification(token);
            }

            @Override
            public void onAccessTokenError(Exception exception) {
                // Handle the error appropriately
                Toast.makeText(getContext(), "Error getting access token: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("PlacesDebug", "Fragment onResume() - places loaded: " + allPlaces.size());
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d("PlacesDebug", "Fragment onPause()");
    }
    
    @Override
    public void onDestroyView() {
        Log.d("PlacesDebug", "Fragment onDestroyView() - cleaning up");
        super.onDestroyView();
        binding = null;
    }
}
