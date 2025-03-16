package com.example.csci3130group1.ui.TutorialManagement;



import android.os.Bundle;
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

import com.example.csci3130group1.R;
import com.example.csci3130group1.databinding.FragmentTutorialManagementBinding;
import com.example.csci3130group1.ui.search_for_tutorials.Tutorial;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class TutorialManagementFragment extends Fragment {

    private FragmentTutorialManagementBinding binding;
    private TutorialManagementViewModel sessionViewModel;
    private EditText topicInput, feeInput, dateInput, timeInput, durationInput, descriptionInput, cityInput, provinceInput, countryInput, nameInput, degreeInput;
    private TextView previewText;
    private Button previewButton, publishButton;

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
        String sessionId = databaseRef.push().getKey();

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}