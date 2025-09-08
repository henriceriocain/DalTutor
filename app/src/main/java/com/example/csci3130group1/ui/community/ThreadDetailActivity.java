package com.example.csci3130group1.ui.community;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.csci3130group1.databinding.ActivityThreadDetailBinding;
import com.example.csci3130group1.models.CommunityThread;
import com.example.csci3130group1.models.CommunityReply;
import com.example.csci3130group1.repositories.CommunityRepository;
import com.example.csci3130group1.ui.community.adapters.CommunityReplyAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ThreadDetailActivity extends AppCompatActivity implements CommunityReplyAdapter.OnReplyInteractionListener {
    private ActivityThreadDetailBinding binding;
    private CommunityViewModel communityViewModel;
    private CommunityReplyAdapter replyAdapter;
    private String threadId;
    private CommunityThread currentThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityThreadDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        threadId = getIntent().getStringExtra("threadId");
        boolean focusReply = getIntent().getBooleanExtra("focusReply", false);

        if (threadId == null) {
            finish();
            return;
        }

        communityViewModel = new ViewModelProvider(this).get(CommunityViewModel.class);
        binding.setViewModel(communityViewModel);
        binding.setLifecycleOwner(this);

        setupToolbar();
        setupRecyclerView();
        setupReplyInput();
        loadThreadData();
        observeViewModel();

        if (focusReply) {
            binding.editReplyText.requestFocus();
        }
    }

    private void setupToolbar() {
        // Setup back button
        binding.btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        replyAdapter = new CommunityReplyAdapter(new ArrayList<>(), this);
        binding.recyclerReplies.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerReplies.setAdapter(replyAdapter);
    }

    private void setupReplyInput() {
        binding.btnSendReply.setOnClickListener(v -> {
            String replyText = binding.editReplyText.getText().toString().trim();
            if (!replyText.isEmpty()) {
                communityViewModel.createReply(threadId, replyText);
                binding.editReplyText.setText("");
            }
        });
    }

    private void loadThreadData() {
        DatabaseReference threadRef = FirebaseDatabase.getInstance()
                .getReference("community_threads")
                .child(threadId);

        threadRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                currentThread = snapshot.getValue(CommunityThread.class);
                if (currentThread != null) {
                    currentThread.setThreadId(threadId);
                    binding.setThread(currentThread);
                    updateThreadUI();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ThreadDetailActivity.this, 
                    "Failed to load thread: " + error.getMessage(), 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateThreadUI() {
        binding.textThreadAuthorName.setText(currentThread.getAuthorName());
        binding.textThreadAuthorRole.setText(currentThread.getAuthorRole());
        binding.textThreadTimestamp.setText(currentThread.getTimeAgo());
        binding.textThreadCategory.setText(currentThread.getCategory());
        binding.textThreadTitle.setText(currentThread.getTitle());
        binding.textThreadDescription.setText(currentThread.getDescription());
        
        updateStarDisplay();
        updateReplyCount();

        // Set role color
        if ("Tutor".equalsIgnoreCase(currentThread.getAuthorRole())) {
            binding.textThreadAuthorRole.setTextColor(Color.parseColor("#1976D2"));
        } else {
            binding.textThreadAuthorRole.setTextColor(Color.parseColor("#388E3C"));
        }

        // Setup star button
        binding.btnThreadStar.setOnClickListener(v -> {
            communityViewModel.toggleThreadStar(threadId);
        });
    }

    private void updateStarDisplay() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                              FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        boolean isStarred = currentUserId != null && currentThread.isStarredByUser(currentUserId);
        binding.btnThreadStar.setImageResource(isStarred ? 
            com.example.csci3130group1.R.drawable.ic_star_filled : 
            com.example.csci3130group1.R.drawable.ic_star);
        
        int starCount = currentThread.getStarCount();
        binding.textThreadStarCount.setText(starCount == 1 ? "1 star" : starCount + " stars");
    }

    private void updateReplyCount() {
        int replyCount = currentThread.getReplyCount();
        binding.textThreadReplyCount.setText(replyCount == 1 ? "1 reply" : replyCount + " replies");
    }

    private void observeViewModel() {
        // Observe replies
        communityViewModel.getThreadReplies(threadId).observe(this, replies -> {
            if (replies != null && !replies.isEmpty()) {
                replyAdapter.updateReplies(replies);
                binding.emptyRepliesLayout.setVisibility(android.view.View.GONE);
                binding.recyclerReplies.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.emptyRepliesLayout.setVisibility(android.view.View.VISIBLE);
                binding.recyclerReplies.setVisibility(android.view.View.GONE);
            }
        });

        // Observe error messages
        communityViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                communityViewModel.clearMessages();
            }
        });

        // Observe success messages
        communityViewModel.getSuccessMessage().observe(this, success -> {
            if (success != null) {
                Toast.makeText(this, success, Toast.LENGTH_SHORT).show();
                communityViewModel.clearMessages();
            }
        });
    }

    // CommunityReplyAdapter.OnReplyInteractionListener implementation
    @Override
    public void onReplyStar(CommunityReply reply) {
        communityViewModel.toggleReplyStar(reply.getReplyId());
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