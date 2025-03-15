
package com.example.csci3130group1.ui.search_for_tutorials;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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

    private FragmentSearchForTutorialsBinding binding;
    private ListView tutorialListView;
    private List<Tutorial> tutorialResults;
    private TutorialAdapter adapter;

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
    //mock data
    private void performSearch() {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("tutorial_sessions");

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tutorialResults.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Tutorial tutorial = data.getValue(Tutorial.class);
                    if (tutorial != null) {
                        tutorialResults.add(tutorial);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load tutorials", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}