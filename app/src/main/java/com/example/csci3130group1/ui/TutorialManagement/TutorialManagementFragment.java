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
    private EditText tutorialNameInput, feeInput, dateInput, startTimeInput, endTimeInput, descriptionInput, capacityInput;
    private Spinner locationSpinner;
    private ArrayAdapter<String> locationAdapter;
    private TextView selectedLocationText;
    private Spinner topicSpinner;
    private TextView tutorNameDisplay, locationStatusText;
    private TextView scheduleHint;
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
        capacityInput = root.findViewById(R.id.capacity_input);
        scheduleHint = root.findViewById(R.id.schedule_hint);
        
        // Load Dal places and setup dropdown
        Log.d("PlacesDebug", "Loading Dal places from assets");
        allPlaces = LocationSpinnerUtils.loadDalPlaces(requireContext());
        
        locationSpinner = root.findViewById(R.id.location_spinner);
        setupLocationSpinner();
        
        selectedLocationText = root.findViewById(R.id.selected_location_text);
        locationStatusText = root.findViewById(R.id.location_status_text);
        tutorNameDisplay = root.findViewById(R.id.tutor_name_display);
        previewButton = root.findViewById(R.id.preview_button);

        loadUserNameFromFirebase();
        setupDateTimePickers();
        setupLocationPicker();

        previewButton.setOnClickListener(view -> handlePreviewButtonClick());
        
        // Set tutor name click listener to navigate to profile
        tutorNameDisplay.setOnClickListener(view -> {
            // In this Activity-hosted fragment, just return to previous screen
            if (getActivity() != null) getActivity().finish();
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
        java.util.TimeZone halifaxTz = java.util.TimeZone.getTimeZone("America/Halifax");
        Calendar calendar = Calendar.getInstance(halifaxTz);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        dateFormat.setTimeZone(halifaxTz);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeFormat.setTimeZone(halifaxTz);

        // Date picker - handle both EditText and container clicks
        View.OnClickListener dateClickListener = v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (DatePicker view, int year, int month, int dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance(halifaxTz);
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
                        Calendar selectedTime = Calendar.getInstance(halifaxTz);
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
                        Calendar selectedTime = Calendar.getInstance(halifaxTz);
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
        dateInput.addTextChangedListener(new SimpleTextWatcher(() -> { resetPreviewState(); validateSchedulingLeadTime(); }));
        startTimeInput.addTextChangedListener(new SimpleTextWatcher(() -> { resetPreviewState(); validateSchedulingLeadTime(); }));
        endTimeInput.addTextChangedListener(new SimpleTextWatcher(() -> { resetPreviewState(); validateSchedulingLeadTime(); }));
        descriptionInput.addTextChangedListener(new SimpleTextWatcher(() -> resetPreviewState()));
        if (capacityInput != null) {
            capacityInput.addTextChangedListener(new SimpleTextWatcher(() -> resetPreviewState()));
        }
        
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
            // Button is in "Publish" state - publish directly
            publishSession();
        } else {
            // Button is in "Preview" state - show preview dialog
            if (!validateSchedulingLeadTime()) return;
            showPreviewDialog();
        }
    }
    
    private void showPreviewDialog() {
        String tutorialName = tutorialNameInput.getText().toString();
        String topic = topicSpinner.getSelectedItem().toString();
        String fee = feeInput.getText().toString();
        String date = dateInput.getText().toString();
        String startTime = startTimeInput.getText().toString();
        String endTime = endTimeInput.getText().toString();
        String description = descriptionInput.getText().toString();
        String capacityStr = capacityInput != null ? capacityInput.getText().toString().trim() : "";
        Integer capacity = null;
        if (!capacityStr.isEmpty()) {
            try {
                int parsed = Integer.parseInt(capacityStr);
                if (parsed > 0) capacity = parsed; else capacity = null; // treat 0/neg as unlimited
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Capacity must be a number", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        String tutorName = tutorNameDisplay.getText().toString();
        
        // Validate all fields before showing preview
        if (tutorialName.isEmpty() || topic.isEmpty() || topic.equals("Select a topic...") || 
            fee.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || 
            description.isEmpty() || selectedAddress.isEmpty() || tutorName.isEmpty()) {
            
            Toast.makeText(getContext(), "Please fill all fields before previewing.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedLatLng == null || !LocationSpinnerUtils.isLocationInHalifax(selectedLatLng)) {
            Toast.makeText(getContext(), "Please select a valid Halifax location.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show dialog with preview
        String previewAddress = selectedAddress.contains(",") ? selectedAddress.split(",")[0] : selectedAddress;
        TutorialPreviewDialogFragment dialog = TutorialPreviewDialogFragment.newInstance(
            tutorialName, tutorName, topic, fee, date, startTime, endTime, description, previewAddress, capacity
        );
        dialog.setPreviewConfirmListener(this);
        dialog.show(getParentFragmentManager(), "tutorial_preview");
    }
    
    
    @Override
    public void onPreviewConfirmed() {
        // Directly publish the tutorial when confirmed from preview
        publishSessionAndNavigate();
    }
    
    private void publishSessionAndNavigate() {
        // Call the existing publish method
        publishSession();
        // Close this Activity to return to previous screen (Profile tab)
        if (getActivity() != null) {
            getActivity().finish();
        }
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
        String capacityStr = capacityInput != null ? capacityInput.getText().toString().trim() : "";
        Integer capacity = null;
        if (!capacityStr.isEmpty()) {
            try {
                int parsed = Integer.parseInt(capacityStr);
                if (parsed > 0) capacity = parsed; else capacity = null;
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Capacity must be a number", Toast.LENGTH_SHORT).show();
                return;
            }
        }
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

        // Validate schedule lead time and order
        if (!validateSchedulingLeadTime()) {
            return;
        }

        if (selectedLatLng == null || !LocationSpinnerUtils.isLocationInHalifax(selectedLatLng)) {
            Toast.makeText(getContext(), "Please select a valid Halifax location", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        this.sessionId = databaseRef.push().getKey();

        // Create tutorial with new improved structure
        Tutorial tutorial = new Tutorial(
                tutorialName, topic, fee, date, startTime, endTime, description,
                cleanAddress, selectedLatLng.latitude, selectedLatLng.longitude, placeId,
                tutorName, tutorId, null, capacity // tutorDegree null for now, capacity optional
        );

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
        validateSchedulingLeadTime();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d("PlacesDebug", "Fragment onPause()");
    }

    // Minimum lead time before start (in ms)
    private static final long MIN_LEAD_MILLIS = 60L * 60L * 1000L; // 1 hour

    /**
     * Validates that the scheduled start time is at least MIN_LEAD_MILLIS in the future
     * and that end time is after start time. Shows an inline hint in the Schedule section
     * and returns false if invalid.
     */
    private boolean validateSchedulingLeadTime() {
        if (scheduleHint == null) return true;

        String date = dateInput != null && dateInput.getText() != null ? dateInput.getText().toString().trim() : "";
        String start = startTimeInput != null && startTimeInput.getText() != null ? startTimeInput.getText().toString().trim() : "";
        String end = endTimeInput != null && endTimeInput.getText() != null ? endTimeInput.getText().toString().trim() : "";

        // Hide by default
        scheduleHint.setVisibility(View.GONE);
        scheduleHint.setTextColor(0xFF6F7780); // neutral

        if (date.isEmpty() || start.isEmpty() || end.isEmpty()) {
            return true; // defer until fields are filled
        }

        Long startMillis = com.example.csci3130group1.utils.TutorialTimeUtils.parseStartMillis(date, start);
        Long endMillis = com.example.csci3130group1.utils.TutorialTimeUtils.parseEndMillis(date, end);

        if (startMillis == null || endMillis == null) {
            scheduleHint.setText("Enter a valid date and time in Halifax time.");
            scheduleHint.setTextColor(0xFFDC2626); // red-600
            scheduleHint.setVisibility(View.VISIBLE);
            return false;
        }

        if (endMillis <= startMillis) {
            scheduleHint.setText("End time must be after start time.");
            scheduleHint.setTextColor(0xFFDC2626);
            scheduleHint.setVisibility(View.VISIBLE);
            return false;
        }

        long now = System.currentTimeMillis();
        if (startMillis - now < MIN_LEAD_MILLIS) {
            scheduleHint.setText("Start time must be at least 1 hour from now (Halifax time).");
            scheduleHint.setTextColor(0xFFDC2626);
            scheduleHint.setVisibility(View.VISIBLE);
            return false;
        }

        // Provide a positive, subtle confirmation once all fields are valid
        scheduleHint.setText("");
        scheduleHint.setTextColor(0xFF059669); // green-600
        scheduleHint.setVisibility(View.VISIBLE);
        return true;
    }
    
    @Override
    public void onDestroyView() {
        Log.d("PlacesDebug", "Fragment onDestroyView() - cleaning up");
        super.onDestroyView();
        binding = null;
    }
}
