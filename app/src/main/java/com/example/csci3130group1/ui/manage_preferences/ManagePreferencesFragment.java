package com.example.csci3130group1.ui.manage_preferences;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ManagePreferencesFragment extends Fragment {
    private FragmentManagePreferencesBinding binding;
    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference usersdRef = rootRef.child("users");
    List<String> tutorNames = new ArrayList<>();
    List<String> selectedTopics;
    Button saveButton;
    Map<String, Object> prefs = new HashMap<>();
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
                initNotifications();
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
        initNotifications();
    }
    private void initNotifications() {
        for (int iter = 0; iter < selectedTopics.size(); iter++) {
            FirebaseMessaging.getInstance().subscribeToTopic(selectedTopics.get(iter));
        }
    }
}