package com.example.csci3130group1.ui.TutorialManagement;



import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.csci3130group1.R;
import com.example.csci3130group1.databinding.FragmentTutorialManagementBinding;
import com.example.csci3130group1.ui.search_for_tutorials.Tutorial;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.android.volley.RequestQueue;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TutorialManagementFragment extends Fragment {
    private static final String CREDENTIALS_FILE_PATH = "key.json";
    private static final String PUSH_NOTIFICATION_ENDPOINT ="https://fcm.googleapis.com/v1/projects/csci3130w25-project-g1/messages:send";
    private FragmentTutorialManagementBinding binding;
    private TutorialManagementViewModel sessionViewModel;
    private EditText topicInput, feeInput, dateInput, timeInput, durationInput, descriptionInput, cityInput, provinceInput, countryInput, nameInput, degreeInput;
    private TextView previewText;
    private Button previewButton, publishButton;
    private RequestQueue requestQueue;
    private String sessionId;
    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference tutorialRef = rootRef.child("tutorial_sessions");
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        sessionViewModel = new ViewModelProvider(this).get(TutorialManagementViewModel.class);

        binding = FragmentTutorialManagementBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        topicInput = root.findViewById(R.id.topic_input);
        feeInput = root.findViewById(R.id.fee_input);
        dateInput = root.findViewById(R.id.date_input);
        timeInput = root.findViewById(R.id.time_input);
        durationInput = root.findViewById(R.id.duration_input);
        descriptionInput = root.findViewById(R.id.description_input);
        cityInput = root.findViewById(R.id.city_input);
        provinceInput = root.findViewById(R.id.province_input);
        countryInput = root.findViewById(R.id.country_input);
        nameInput = root.findViewById(R.id.name_input);
        degreeInput = root.findViewById(R.id.degree_input);
        previewText = root.findViewById(R.id.preview_text);
        previewButton = root.findViewById(R.id.preview_button);
        publishButton = root.findViewById(R.id.publish_button);

        previewButton.setOnClickListener(view -> previewSession());
        publishButton.setOnClickListener(view -> publishSession());

        FirebaseMessaging.getInstance().subscribeToTopic("History");
        return root;
    }

    private void previewSession() {
        String topic = topicInput.getText().toString();
        String fee = feeInput.getText().toString();
        String date = dateInput.getText().toString();
        String time = timeInput.getText().toString();
        String duration = durationInput.getText().toString();
        String description = descriptionInput.getText().toString();
        String city = cityInput.getText().toString();
        String province = provinceInput.getText().toString();
        String country = countryInput.getText().toString();
        String name = nameInput.getText().toString();
        String degree = degreeInput.getText().toString();

        if (topic.isEmpty() || fee.isEmpty() || date.isEmpty() || time.isEmpty() || duration.isEmpty() || description.isEmpty() || city.isEmpty() || province.isEmpty() || country.isEmpty() || name.isEmpty() || degree.isEmpty()) {
            previewText.setText("Preview: Please fill all fields.");
            previewText.setVisibility(View.VISIBLE);
        } else {
            String preview = "Preview Session:\n"
                    + "Tutor: " + name + "\n"
                    + "Degree: " + degree + "\n"
                    + "Topic: " + topic + "\n"
                    + "Fee: $" + fee + "\n"
                    + "Date: " + date + "\n"
                    + "Time: " + time + "\n"
                    + "Duration: " + duration + " minutes\n"
                    + "Description: " + description + "\n"
                    + "City: " + city + "\n"
                    + "Province: " + province + "\n"
                    + "Country: " + country;
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
                JSONObject messageJSONBody = new JSONObject();
                messageJSONBody.put("topic", topicInput.getText().toString());
                messageJSONBody.put("notification", JSONBody);
                messageJSONBody.put("data", dataJSONBody);

                JSONObject pushNotificationJSONBody = new JSONObject();
                pushNotificationJSONBody.put("message", messageJSONBody);

            // Log the complete JSON payload for debugging

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
        String topic = topicInput.getText().toString();
        String fee = feeInput.getText().toString();
        String date = dateInput.getText().toString();
        String time = timeInput.getText().toString();
        String duration = durationInput.getText().toString();
        String description = descriptionInput.getText().toString();
        String city = cityInput.getText().toString();
        String province = provinceInput.getText().toString();
        String country = countryInput.getText().toString();
        String name = nameInput.getText().toString();
        String degree = degreeInput.getText().toString();

        if (topic.isEmpty() || fee.isEmpty() || date.isEmpty() || time.isEmpty() || duration.isEmpty()  || description.isEmpty() || city.isEmpty() || province.isEmpty() || country.isEmpty() || name.isEmpty() || degree.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        this.sessionId = databaseRef.push().getKey();

        Tutorial tutorial = new Tutorial(topic, fee, duration, description, city, province, country, name, degree);

        if (sessionId != null) {
            databaseRef.child(sessionId).setValue(tutorial).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
