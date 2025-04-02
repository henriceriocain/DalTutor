package com.example.csci3130group1.ui.manage_preferences;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.csci3130group1.R;
import com.example.csci3130group1.databinding.FragmentManagePreferencesBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.android.volley.toolbox.Volley;
import com.google.auth.oauth2.GoogleCredentials;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ManagePreferencesFragment extends Fragment {
    private static final String CREDENTIALS_FILE_PATH = "key.json";
    private static final String PUSH_NOTIFICATION_ENDPOINT ="https://fcm.googleapis.com/v1/projects/csci3130w25-project-g1/messages:send";
    private FragmentManagePreferencesBinding binding;
    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference usersdRef = rootRef.child("users");
    List<String> tutorNames = new ArrayList<>();
    List<String> selectedTopics;
    Button saveButton;
    Map<String, Object> prefs = new HashMap<>();
    private RequestQueue requestQueue;
    ValueEventListener eventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            tutorNames.add("Select a Tutor");
            for(DataSnapshot ds : snapshot.getChildren()) {
                if (Objects.equals(ds.child("role").getValue(String.class), "Tutor")) {
                    String name = ds.child("name").getValue(String.class);
                    tutorNames.add(name);
                }
            }
            View root = binding.getRoot();
            Spinner spinner = root.findViewById(R.id.tutor);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(root.getContext(), android.R.layout.simple_spinner_dropdown_item, tutorNames);
            spinner.setAdapter(adapter);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {}
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        usersdRef.addValueEventListener(eventListener);
        binding = FragmentManagePreferencesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        saveButton = root.findViewById(R.id.savepref);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferencesRealtime();
                getAccessToken(root.getContext(), new AccessTokenListener() {
                    @Override
                    public void onAccessTokenReceived(String token) {
                        // When the token is received, send the notification
                        sendNotification(token);
                    }

                    @Override
                    public void onAccessTokenError(Exception exception) {
                        // Handle the error appropriately
                        Toast.makeText(root.getContext(), "Error getting access token: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                        exception.printStackTrace();
                    }
                });
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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


    private void savePreferencesRealtime() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }
        View root = binding.getRoot();
        Spinner tutorSpinner = root.findViewById(R.id.tutor);
        String userId = currentUser.getUid();
        String selectedTutor = tutorSpinner.getSelectedItem().toString();

        if (selectedTutor.equalsIgnoreCase("Select a Tutor")) {
            Toast.makeText(getContext(), "Choose a favorite Tutor", Toast.LENGTH_SHORT).show();
            return;
        }

        this.selectedTopics = new ArrayList<>();
        if (binding.math.isChecked()) this.selectedTopics.add(binding.math.getText().toString());
        if (binding.CS.isChecked()) this.selectedTopics.add(binding.CS.getText().toString());
        if (binding.chem.isChecked()) this.selectedTopics.add(binding.chem.getText().toString());
        if (binding.bio.isChecked()) this.selectedTopics.add(binding.bio.getText().toString());
        if (binding.History.isChecked()) this.selectedTopics.add(binding.History.getText().toString());
        if (binding.physics.isChecked()) this.selectedTopics.add(binding.physics.getText().toString());
        if (binding.English.isChecked()) this.selectedTopics.add(binding.English.getText().toString());


        prefs.put("favoriteTutor", selectedTutor);
        prefs.put("favoriteTopics", this.selectedTopics);
        prefs.put("userEmail", currentUser.getEmail());

        // Save under preferences/userId
        rootRef.child("preferences").child(userId)
                .setValue(prefs)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "Preferences saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        //notifications
        initNotifications(selectedTopics);
        getAccessToken(root.getContext(), new AccessTokenListener() {
            @Override
            public void onAccessTokenReceived(String token) {
                // When the token is received, send the notification
                sendNotification(token);
            }

            @Override
            public void onAccessTokenError(Exception exception) {
                // Handle the error appropriately
                Toast.makeText(root.getContext(), "Error getting access token: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                exception.printStackTrace();
            }
        });
    }
    private void initNotifications(List<String> selectedTopics) {
        View root = binding.getRoot();
        requestQueue = Volley.newRequestQueue(root.getContext());
        for (int iter = 0; iter < selectedTopics.size(); iter++) {
            FirebaseMessaging.getInstance().subscribeToTopic(selectedTopics.get(iter));
        }
    }
    private void sendNotification(String authToken) {
        try {
            // Build the notification payload
            for (int iter = 0; iter < selectedTopics.size(); iter++) {
                JSONObject JSONBody = new JSONObject();
                JSONBody.put("title", "A tutorial that matches your preferences has been posted");
                JSONBody.put("body", "Click here to see the mentioned tutorial");
                JSONObject messageJSONBody = new JSONObject();
                messageJSONBody.put("topic", selectedTopics.get(iter));
                messageJSONBody.put("notification", JSONBody);
                JSONObject pushNotificationJSONBody = new JSONObject();
                pushNotificationJSONBody.put("message", messageJSONBody);

                // Log the complete JSON payload for debugging
                Log.d("NotificationBody", "JSON Body: " + pushNotificationJSONBody.toString());

                // Create the request
                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        PUSH_NOTIFICATION_ENDPOINT,
                        pushNotificationJSONBody,
                        response -> {
                            Log.d("NotificationResponse", "Response: " + response.toString());
                            Toast.makeText(this.getContext(), "Notification Sent Successfully", Toast.LENGTH_SHORT).show();
                        },
                        error -> {
                            Log.e("NotificationError", "Error Response: " + error.toString());
                            if (error.networkResponse != null) {
                                Log.e("NotificationError", "Status Code: " + error.networkResponse.statusCode);
                                Log.e("NotificationError", "Error Data: " + new String(error.networkResponse.data));
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
            }
        } catch (JSONException e) {
            Log.e("NotificationJSONException", "Error creating notification JSON: " + e.getMessage());
            Toast.makeText(this.getContext(), "Error creating notification payload", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }
}