package com.example.csci3130group1.ui.TutorialManagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.csci3130group1.databinding.FragmentTutorialManagementBinding;

public class TutorialManagementFragment extends Fragment {

    private FragmentTutorialManagementBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        TutorialManagementViewModel tutorialManagementViewModel =
                new ViewModelProvider(this).get(TutorialManagementViewModel.class);

        binding = FragmentTutorialManagementBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textDashboard;
        tutorialManagementViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}