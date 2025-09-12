package com.example.csci3130group1.ui.community;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Toast;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

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
    private boolean filtersAnimating = false;
    private boolean filtersInitialized = false;
    private Boolean pendingExpandedState = null;

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
        setupSearch();
        setupSpinners();
        setupFiltersToggle();
        setupFab();
        setupSwipeRefresh();
        observeViewModel();

        // Notifications icon removed per new design

        View myActivity = view.findViewById(R.id.btn_my_activity);
        if (myActivity != null) {
            myActivity.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), CommunityActivity.class);
                startActivity(intent);
            });
        }
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

    private void setupSearch() {
        // Restore previous query
        String existing = communityViewModel.getSearchQuery().getValue();
        if (existing != null && binding.searchInput.getText() != null) {
            binding.searchInput.setText(existing);
        }

        // Update query reactively on text change
        binding.searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                communityViewModel.setSearchQuery(s != null ? s.toString() : "");
            }
        });

        // Handle keyboard action search to dismiss keyboard
        binding.searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });
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

    private void setupFiltersToggle() {
        // Apply initial state without animation
        Boolean init = communityViewModel.isFiltersExpanded().getValue();
        boolean isExpanded = init != null && init;
        binding.filtersContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        binding.filtersChevron.setRotation(isExpanded ? 180f : 0f);
        binding.filtersHeader.setContentDescription(isExpanded ? "Collapse filters" : "Expand filters");
        filtersInitialized = true;

        binding.filtersHeader.setOnTouchListener((v, event) -> {
            final AccelerateDecelerateInterpolator ease = new AccelerateDecelerateInterpolator();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    if (filtersAnimating) return true;
                    boolean current = Boolean.TRUE.equals(communityViewModel.isFiltersExpanded().getValue());
                    pendingExpandedState = !current;
                    filtersAnimating = true;
                    // Fade in overlay to cover any state changes
                    binding.filtersOverlay.animate().cancel();
                    binding.filtersOverlay.setClickable(true);
                    binding.filtersOverlay.setAlpha(0f);
                    binding.filtersOverlay.animate()
                            .alpha(1f)
                            .setDuration(180)
                            .setInterpolator(ease)
                            .start();
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    if (!filtersAnimating) return true;
                    boolean target = pendingExpandedState != null ? pendingExpandedState : !Boolean.TRUE.equals(communityViewModel.isFiltersExpanded().getValue());
                    // Ensure overlay is fully opaque before state change to prevent any flash
                    binding.filtersOverlay.animate().cancel();
                    binding.filtersOverlay.setAlpha(1f);
                    // Resize smoothly; wait until bounds animation finishes before fade-in
                    ChangeBounds cb = new ChangeBounds();
                    cb.setDuration(240);
                    cb.setInterpolator(ease);
                    cb.addListener(new Transition.TransitionListener() {
                        @Override public void onTransitionStart(Transition transition) { }
                        @Override public void onTransitionCancel(Transition transition) { }
                        @Override public void onTransitionPause(Transition transition) { }
                        @Override public void onTransitionResume(Transition transition) { }
                        @Override public void onTransitionEnd(Transition transition) {
                            // Reveal new content by fading overlay out after a short delay
                            binding.filtersOverlay.animate().cancel();
                            binding.filtersOverlay.animate()
                                    .alpha(0f)
                                    .setStartDelay(120)
                                    .setDuration(300)
                                    .setInterpolator(ease)
                                    .withEndAction(() -> {
                                        filtersAnimating = false;
                                        pendingExpandedState = null;
                                        binding.filtersOverlay.setClickable(false);
                                    })
                                    .start();
                        }
                    });
                    TransitionManager.beginDelayedTransition(binding.filtersCard, cb);
                    binding.filtersContent.setVisibility(target ? View.VISIBLE : View.GONE);
                    // Update chevron and a11y
                    binding.filtersChevron.animate().rotation(target ? 180f : 0f).setDuration(200).setInterpolator(ease).start();
                    binding.filtersHeader.setContentDescription(target ? "Collapse filters" : "Expand filters");
                    // Persist state
                    communityViewModel.setFiltersExpanded(target);
                    return true;
                }
                case MotionEvent.ACTION_CANCEL: {
                    // Revert overlay if gesture canceled
                    binding.filtersOverlay.animate().cancel();
                    binding.filtersOverlay.setAlpha(0f);
                    binding.filtersOverlay.setClickable(false);
                    filtersAnimating = false;
                    pendingExpandedState = null;
                    return true;
                }
            }
            return true;
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
        // Use a dedicated DialogFragment with an explicit backdrop overlay
        CreateThreadDialogFragment fragment = new CreateThreadDialogFragment();
        fragment.show(getChildFragmentManager(), "CreateThreadDialog");
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
