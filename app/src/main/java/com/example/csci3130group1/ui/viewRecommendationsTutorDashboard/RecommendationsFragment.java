package com.example.csci3130group1.ui.viewRecommendationsTutorDashboard;

/*import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.csci3130group1.databinding.FragmentRecsBinding;

public class RecommendationsFragment extends Fragment {

    private FragmentRecsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        RecommendationsViewModel recommendationsViewModel =
                new ViewModelProvider(this).get(RecommendationsViewModel.class);

        binding = FragmentRecsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textNotifications;
        recommendationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}*/


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.csci3130group1.databinding.FragmentRecsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RecommendationsFragment extends Fragment {

    private FragmentRecsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRecsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        fetchRecommendations();

        return root;
    }

    private void fetchRecommendations() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String tutorName = snapshot.child("name").getValue(String.class);
                if (tutorName != null) {
                    findStudentsForTutor(tutorName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void findStudentsForTutor(String tutorName) {
        DatabaseReference prefsRef = FirebaseDatabase.getInstance().getReference("preferences");
        DatabaseReference regRef = FirebaseDatabase.getInstance().getReference("registrations");

        List<String> recommendedStudents = new ArrayList<>();

        prefsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot prefsSnap) {
                for (DataSnapshot studentSnap : prefsSnap.getChildren()) {
                    String favoriteTutor = studentSnap.child("favoriteTutor").getValue(String.class);
                    if (favoriteTutor != null && tutorName.equalsIgnoreCase(favoriteTutor)) {
                        String email = studentSnap.child("userEmail").getValue(String.class);
                        if (email != null && !recommendedStudents.contains(email)) {
                            recommendedStudents.add(email);
                        }
                    }
                }

                regRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot regSnap) {
                        for (DataSnapshot regEntry : regSnap.getChildren()) {
                            String regUserEmail = regEntry.child("userEmail").getValue(String.class);
                            String tutorialTitle = regEntry.child("tutorialTitle").getValue(String.class);
                            if (regUserEmail != null && tutorialTitle != null && tutorialTitle.toLowerCase().contains(tutorName.toLowerCase())) {
                                if (!recommendedStudents.contains(regUserEmail)) {
                                    recommendedStudents.add(regUserEmail);
                                }
                            }
                        }
                        displayRecommendations(recommendedStudents);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayRecommendations(List<String> students) {
        StringBuilder recommendations = new StringBuilder("Potential Students:\n");
        for (String email : students) {
            recommendations.append("• ").append(email).append("\n");
        }
        binding.textNotifications.setText(recommendations.toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}