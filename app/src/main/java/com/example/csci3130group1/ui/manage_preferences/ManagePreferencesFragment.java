package com.example.csci3130group1.ui.manage_preferences;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.csci3130group1.R;
import com.example.csci3130group1.databinding.FragmentManagePreferencesBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;


public class ManagePreferencesFragment extends Fragment {

    private FragmentManagePreferencesBinding binding;
    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference usersdRef = rootRef.child("users");
    ArrayList<String> tutorNames = new ArrayList<>();
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
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {}
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        usersdRef.addValueEventListener(eventListener);
        binding = FragmentManagePreferencesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        setTutorSpinner(root);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setTutorSpinner(View view) {
        Spinner spinner = view.findViewById(R.id.tutor);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, tutorNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
}