package com.example.csci3130group1.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.csci3130group1.databinding.ActivityUserThreadsBinding;
import com.example.csci3130group1.models.CommunityThread;
import com.example.csci3130group1.ui.community.adapters.CommunityThreadAdapter;

import java.util.ArrayList;

public class UserThreadsActivity extends AppCompatActivity implements CommunityThreadAdapter.OnThreadInteractionListener {
    private ActivityUserThreadsBinding binding;
    private CommunityViewModel communityViewModel;
    private CommunityThreadAdapter threadAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserThreadsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        communityViewModel = new ViewModelProvider(this).get(CommunityViewModel.class);
        binding.setViewModel(communityViewModel);
        binding.setLifecycleOwner(this);

        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        observeViewModel();
    }

    private void setupToolbar() {
        // Setup back button
        binding.btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        threadAdapter = new CommunityThreadAdapter(new ArrayList<>(), this);
        binding.recyclerUserThreads.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerUserThreads.setAdapter(threadAdapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            // Refresh data - LiveData will automatically update
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void observeViewModel() {
        // Observe user threads
        communityViewModel.getUserThreads().observe(this, threads -> {
            if (threads != null && !threads.isEmpty()) {
                threadAdapter.updateThreads(threads);
                binding.emptyStateLayout.setVisibility(android.view.View.GONE);
                binding.recyclerUserThreads.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.emptyStateLayout.setVisibility(android.view.View.VISIBLE);
                binding.recyclerUserThreads.setVisibility(android.view.View.GONE);
            }
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    // CommunityThreadAdapter.OnThreadInteractionListener implementation
    @Override
    public void onThreadClick(CommunityThread thread) {
        Intent intent = new Intent(this, ThreadDetailActivity.class);
        intent.putExtra("threadId", thread.getThreadId());
        startActivity(intent);
    }

    @Override
    public void onStarClick(CommunityThread thread) {
        communityViewModel.toggleThreadStar(thread.getThreadId());
    }

    @Override
    public void onReplyClick(CommunityThread thread) {
        Intent intent = new Intent(this, ThreadDetailActivity.class);
        intent.putExtra("threadId", thread.getThreadId());
        intent.putExtra("focusReply", true);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}