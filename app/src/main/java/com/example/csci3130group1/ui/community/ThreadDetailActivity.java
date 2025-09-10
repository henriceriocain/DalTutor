package com.example.csci3130group1.ui.community;

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
import com.example.csci3130group1.TutorProfileActivity;
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

        // No explicit back button; rely on system back
        setupRecyclerView();
        setupReplyInput();
        loadThreadData();
        observeViewModel();

        if (focusReply) {
            binding.editReplyText.requestFocus();
        }
    }

    // No-op toolbar setup; header shows title only

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
                // Null check to prevent crash during activity initialization
                if (binding == null) return;
                
                if (!snapshot.exists()) {
                    finish();
                    return;
                }
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
        // Hide role to keep community neutral
        binding.textThreadAuthorRole.setVisibility(android.view.View.GONE);
        binding.textThreadTimestamp.setText(currentThread.getTimeAgo());
        binding.textThreadCategory.setText(currentThread.getCategory());
        binding.textThreadTitle.setText(currentThread.getTitle());
        binding.textThreadDescription.setText(currentThread.getDescription());

        // Make author name a link only if tutor tools are enabled for the author
        wireAuthorAsTutorLinkIfEligible(currentThread.getAuthorId());
        
        updateStarDisplay();
        updateReplyCount();

        // No role-based coloring

        // Setup star button
        binding.btnThreadStar.setOnClickListener(v -> {
            communityViewModel.toggleThreadStar(threadId);
        });

        // Setup delete button (visible only if authored by current user)
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId != null && currentUserId.equals(currentThread.getAuthorId())) {
            binding.btnDeleteThread.setVisibility(android.view.View.VISIBLE);
            binding.btnDeleteThread.setOnClickListener(v -> showDeleteThreadDialog());
        } else {
            binding.btnDeleteThread.setVisibility(android.view.View.GONE);
        }
    }

    private void wireAuthorAsTutorLinkIfEligible(String authorId) {
        if (authorId == null || authorId.isEmpty()) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(authorId).child("isTutorEnabled");
        // Default state: reset color and clickability
        binding.textThreadAuthorName.setTextColor(android.graphics.Color.parseColor("#111827"));
        binding.textThreadAuthorName.setClickable(false);
        binding.textThreadAuthorName.setOnClickListener(null);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean enabled = snapshot.getValue(Boolean.class);
                if (enabled != null && enabled) {
                    // Apply link styling (dark yellow) and click to tutor profile
                    binding.textThreadAuthorName.setTextColor(getResources().getColor(
                            com.example.csci3130group1.R.color.brown_primary
                    ));
                    binding.textThreadAuthorName.setClickable(true);
                    binding.textThreadAuthorName.setOnClickListener(v -> {
                        android.content.Intent intent = new android.content.Intent(ThreadDetailActivity.this, TutorProfileActivity.class);
                        intent.putExtra("tutorId", authorId);
                        intent.putExtra("readOnly", true);
                        startActivity(intent);
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) { /* no-op */ }
        });
    }

    private void showDeleteThreadDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete thread?")
                .setMessage("This will remove the thread and its replies.")
                .setPositiveButton("Delete", (d, w) -> {
                    communityViewModel.deleteThread(threadId);
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                if ("Thread deleted".equals(success)) {
                    finish();
                    return;
                }
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
    public void onReplyDelete(CommunityReply reply) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete reply?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    communityViewModel.deleteReply(reply.getReplyId(), reply.getThreadId());
                })
                .setNegativeButton("Cancel", null)
                .show();
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
