package com.example.csci3130group1.ui.search_for_tutorials;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.csci3130group1.R;
import com.example.csci3130group1.databinding.FragmentSearchForTutorialsBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SearchForTutorialsFragment extends Fragment {

    private static final String TAG = "SearchForTutorials";
    private FragmentSearchForTutorialsBinding binding;
    private ListView tutorialListView;
    private List<Tutorial> tutorialResults;
    private TutorialAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchForTutorialsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize UI elements
        tutorialListView = binding.tutorialListView;
        tutorialResults = new ArrayList<>();
        adapter = new TutorialAdapter(requireContext(), tutorialResults);
        tutorialListView.setAdapter(adapter);

        // Search button click listener
        binding.searchButton.setOnClickListener(v -> performSearch());

        return root;
    }

    // Retrieve data from Firebase and then update & filter the tutorials.
    private void performSearch() {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");
        Log.d(TAG, "Performing search: fetching tutorials from Firebase");

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tutorialResults.clear();
                Log.d(TAG, "DataSnapshot received with " + snapshot.getChildrenCount() + " items.");

                for (DataSnapshot data : snapshot.getChildren()) {
                    Tutorial tutorial = data.getValue(Tutorial.class);
                    if (tutorial != null) {
                        tutorialResults.add(tutorial);
                        Log.d(TAG, "Loaded tutorial: " + tutorial.getTitle());
                    }
                }
                Log.d(TAG, "Total tutorials loaded: " + tutorialResults.size());

                // Update the adapter with the newly loaded data.
                adapter.updateTutorials(tutorialResults);

                // After updating, filter the tutorials based on user inputs.
                filterTutorials();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load tutorials: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load tutorials", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Retrieve filter criteria from the input fields and apply the filter.
    private void filterTutorials() {
        String locationFilter = binding.locationInput.getText().toString().trim();
        String feeFilter = binding.feeInput.getText().toString().trim();
        String durationFilter = binding.durationInput.getText().toString().trim();

        Log.d(TAG, "Filtering tutorials with location: " + locationFilter +
                ", fee: " + feeFilter + ", duration: " + durationFilter);

        adapter.filter(locationFilter, feeFilter, durationFilter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
