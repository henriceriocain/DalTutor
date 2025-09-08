package com.example.csci3130group1.ui.community;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.csci3130group1.R;
import com.example.csci3130group1.databinding.FragmentCommunityBinding;
import com.example.csci3130group1.models.CommunityThread;
import com.example.csci3130group1.ui.community.adapters.CommunityThreadAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommunityFragment extends Fragment implements CommunityThreadAdapter.OnThreadInteractionListener {
    private FragmentCommunityBinding binding;
    private CommunityViewModel communityViewModel;
    private CommunityThreadAdapter threadAdapter;

    // Non-filtering adapter that always shows all items
    public static class NoFilterArrayAdapter<T> extends ArrayAdapter<T> {
        private final List<T> allItems;

        public NoFilterArrayAdapter(@NonNull Context ctx, int layout, @NonNull List<T> items) {
            super(ctx, layout, new ArrayList<>(items));
            this.allItems = new ArrayList<>(items);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults res = new FilterResults();
                    res.values = allItems;
                    res.count = allItems.size();
                    return res;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear();
                    //noinspection unchecked
                    addAll((List<T>) results.values);
                    notifyDataSetChanged();
                }

                @Override
                public CharSequence convertResultToString(Object value) {
                    return value == null ? "" : value.toString();
                }
            };
        }
    }

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
        // Use non-filtering adapters to prevent dropdown filtering issues
        NoFilterArrayAdapter<String> sortAdapter = new NoFilterArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                Arrays.asList(communityViewModel.getSortOptions()));
        binding.spinnerSort.setAdapter(sortAdapter);
        binding.spinnerSort.setOnItemClickListener((parent, view, position, id) -> 
                communityViewModel.setSortOption(communityViewModel.getSortOptions()[position]));

        NoFilterArrayAdapter<String> categoryAdapter = new NoFilterArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                Arrays.asList(communityViewModel.getCategories()));
        binding.spinnerCategory.setAdapter(categoryAdapter);
        binding.spinnerCategory.setOnItemClickListener((parent, view, position, id) -> 
                communityViewModel.setCategoryFilter(communityViewModel.getCategories()[position]));

        NoFilterArrayAdapter<String> timeAdapter = new NoFilterArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                Arrays.asList(communityViewModel.getTimeFilters()));
        binding.spinnerTime.setAdapter(timeAdapter);
        binding.spinnerTime.setOnItemClickListener((parent, view, position, id) -> 
                communityViewModel.setTimeFilter(communityViewModel.getTimeFilters()[position]));

        // Restore selection WITHOUT re-setting adapters
        String currentSort = communityViewModel.getSelectedSortOption().getValue();
        if (currentSort != null) {
            binding.spinnerSort.setText(currentSort, false);
        }

        String currentCategory = communityViewModel.getSelectedCategoryFilter().getValue();
        if (currentCategory != null) {
            binding.spinnerCategory.setText(currentCategory, false);
        }

        String currentTime = communityViewModel.getSelectedTimeFilter().getValue();
        if (currentTime != null) {
            binding.spinnerTime.setText(currentTime, false);
        }

        // Observe filter changes
        observeFilterStates();
    }


    private void observeFilterStates() {
        // Observe filter changes and update display text (no adapter reset needed)
        communityViewModel.getSelectedSortOption().observe(getViewLifecycleOwner(), sortOption -> {
            if (sortOption != null && !sortOption.equals(binding.spinnerSort.getText().toString())) {
                binding.spinnerSort.setText(sortOption, false);
            }
        });
        
        communityViewModel.getSelectedCategoryFilter().observe(getViewLifecycleOwner(), categoryFilter -> {
            if (categoryFilter != null && !categoryFilter.equals(binding.spinnerCategory.getText().toString())) {
                binding.spinnerCategory.setText(categoryFilter, false);
            }
        });
        
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