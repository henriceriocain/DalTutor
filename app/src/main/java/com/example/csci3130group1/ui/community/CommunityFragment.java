package com.example.csci3130group1.ui.community;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3130group1.R;
import com.example.csci3130group1.databinding.FragmentCommunityBinding;
import com.example.csci3130group1.models.CommunityThread;
import com.example.csci3130group1.ui.community.adapters.CommunityThreadAdapter;

import java.util.ArrayList;

public class CommunityFragment extends Fragment implements CommunityThreadAdapter.OnThreadInteractionListener {
    private FragmentCommunityBinding binding;
    private CommunityViewModel communityViewModel;
    private CommunityThreadAdapter threadAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCommunityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        communityViewModel = new ViewModelProvider(this).get(CommunityViewModel.class);
        binding.setViewModel(communityViewModel);
        binding.setLifecycleOwner(this);

        setupRecyclerView();
        setupSpinners();
        setupFab();
        setupSwipeRefresh();
        observeViewModel();
    }

    private void setupRecyclerView() {
        threadAdapter = new CommunityThreadAdapter(new ArrayList<>(), this);
        binding.recyclerThreads.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerThreads.setAdapter(threadAdapter);
    }

    private void setupSpinners() {
        // Sort options
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                communityViewModel.getSortOptions());
        binding.spinnerSort.setAdapter(sortAdapter);
        
        binding.spinnerSort.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSort = communityViewModel.getSortOptions()[position];
            communityViewModel.setSortOption(selectedSort);
        });

        // Category filter
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                communityViewModel.getCategories());
        binding.spinnerCategory.setAdapter(categoryAdapter);
        
        binding.spinnerCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCategory = communityViewModel.getCategories()[position];
            communityViewModel.setCategoryFilter(selectedCategory);
        });

        // Time filter
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                communityViewModel.getTimeFilters());
        binding.spinnerTime.setAdapter(timeAdapter);
        
        binding.spinnerTime.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTime = communityViewModel.getTimeFilters()[position];
            communityViewModel.setTimeFilter(selectedTime);
        });
        
        // Restore spinner states from ViewModel
        restoreFilterStates();
        
        // Observe filter changes to update spinner displays
        observeFilterStates();
    }

    private void restoreFilterStates() {
        // Restore Sort spinner
        String currentSort = communityViewModel.getSelectedSortOption().getValue();
        if (currentSort != null) {
            binding.spinnerSort.setText(currentSort, false);
        }
        
        // Restore Category spinner
        String currentCategory = communityViewModel.getSelectedCategoryFilter().getValue();
        if (currentCategory != null) {
            binding.spinnerCategory.setText(currentCategory, false);
        }
        
        // Restore Time spinner
        String currentTime = communityViewModel.getSelectedTimeFilter().getValue();
        if (currentTime != null) {
            binding.spinnerTime.setText(currentTime, false);
        }
    }

    private void observeFilterStates() {
        // Observe sort option changes
        communityViewModel.getSelectedSortOption().observe(getViewLifecycleOwner(), sortOption -> {
            if (sortOption != null && !sortOption.equals(binding.spinnerSort.getText().toString())) {
                binding.spinnerSort.setText(sortOption, false);
            }
        });
        
        // Observe category filter changes
        communityViewModel.getSelectedCategoryFilter().observe(getViewLifecycleOwner(), categoryFilter -> {
            if (categoryFilter != null && !categoryFilter.equals(binding.spinnerCategory.getText().toString())) {
                binding.spinnerCategory.setText(categoryFilter, false);
            }
        });
        
        // Observe time filter changes
        communityViewModel.getSelectedTimeFilter().observe(getViewLifecycleOwner(), timeFilter -> {
            if (timeFilter != null && !timeFilter.equals(binding.spinnerTime.getText().toString())) {
                binding.spinnerTime.setText(timeFilter, false);
            }
        });
    }

    private void setupFab() {
        binding.fabCreateThread.setOnClickListener(v -> showCreateThreadDialog());
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            // Refresh data - LiveData will automatically update
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void observeViewModel() {
        // Observe threads
        communityViewModel.getThreads().observe(getViewLifecycleOwner(), threads -> {
            if (threads != null && !threads.isEmpty()) {
                threadAdapter.updateThreads(threads);
                binding.emptyStateLayout.setVisibility(View.GONE);
                binding.recyclerThreads.setVisibility(View.VISIBLE);
            } else {
                binding.emptyStateLayout.setVisibility(View.VISIBLE);
                binding.recyclerThreads.setVisibility(View.GONE);
            }
            binding.swipeRefresh.setRefreshing(false);
        });

        // Observe loading state
        communityViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.loadingLayout.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe error messages
        communityViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                communityViewModel.clearMessages();
            }
        });

        // Observe success messages
        communityViewModel.getSuccessMessage().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                Toast.makeText(getContext(), success, Toast.LENGTH_SHORT).show();
                communityViewModel.clearMessages();
            }
        });
    }

    private void showCreateThreadDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_create_thread, null);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        // Setup category spinner
        android.widget.AutoCompleteTextView categorySpinner = dialogView.findViewById(R.id.spinner_thread_category);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                communityViewModel.getThreadCategories());
        categorySpinner.setAdapter(adapter);
        categorySpinner.setText(communityViewModel.getThreadCategories()[0], false);

        // Setup buttons
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        
        dialogView.findViewById(R.id.btn_create).setOnClickListener(v -> {
            String title = ((com.google.android.material.textfield.TextInputEditText) 
                           dialogView.findViewById(R.id.edit_thread_title)).getText().toString();
            String description = ((com.google.android.material.textfield.TextInputEditText) 
                                 dialogView.findViewById(R.id.edit_thread_description)).getText().toString();
            String category = categorySpinner.getText().toString();

            communityViewModel.createThread(title, description, category);
            dialog.dismiss();
        });

        dialog.show();
    }

    // CommunityThreadAdapter.OnThreadInteractionListener implementation
    @Override
    public void onThreadClick(CommunityThread thread) {
        Intent intent = new Intent(getContext(), ThreadDetailActivity.class);
        intent.putExtra("threadId", thread.getThreadId());
        startActivity(intent);
    }

    @Override
    public void onStarClick(CommunityThread thread) {
        communityViewModel.toggleThreadStar(thread.getThreadId());
    }

    @Override
    public void onReplyClick(CommunityThread thread) {
        Intent intent = new Intent(getContext(), ThreadDetailActivity.class);
        intent.putExtra("threadId", thread.getThreadId());
        intent.putExtra("focusReply", true);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}