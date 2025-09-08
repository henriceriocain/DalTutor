package com.example.csci3130group1.ui.community;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.csci3130group1.R;
import com.example.csci3130group1.models.CommunityReply;
import com.example.csci3130group1.models.CommunityThread;
import com.example.csci3130group1.ui.community.adapters.CommunityThreadAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.util.ArrayMap;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class CommunityActivity extends AppCompatActivity implements CommunityThreadAdapter.OnThreadInteractionListener {
    private CommunityViewModel viewModel;
    private CommunityThreadAdapter threadAdapter;
    private UserRepliesAdapter repliesAdapter;
    private androidx.recyclerview.widget.RecyclerView recycler;
    private View empty;
    private android.widget.ImageView emptyIcon;
    private android.widget.Button btnPosts, btnReplies, btnStarred;

    private enum Tab { POSTS, REPLIES, STARRED }
    private Tab currentTab = Tab.POSTS;

    private LiveData<List<CommunityThread>> myPostsLive;
    private LiveData<List<CommunityReply>> myRepliesLive;
    private LiveData<List<CommunityThread>> myStarredLive;
    private final Map<String, String> threadTitleCache = new ArrayMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_activity);

        viewModel = new ViewModelProvider(this).get(CommunityViewModel.class);

        // No back button in header; rely on system back or tab nav

        recycler = findViewById(R.id.recycler);
        empty = findViewById(R.id.emptyState);
        emptyIcon = findViewById(R.id.emptyIcon);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        // Tabs
        btnPosts = findViewById(R.id.btnPosts);
        btnReplies = findViewById(R.id.btnReplies);
        btnStarred = findViewById(R.id.btnStarred);

        btnPosts.setOnClickListener(v -> switchTab(Tab.POSTS));
        btnReplies.setOnClickListener(v -> switchTab(Tab.REPLIES));
        btnStarred.setOnClickListener(v -> switchTab(Tab.STARRED));

        // Adapters
        threadAdapter = new CommunityThreadAdapter(new ArrayList<>(), this);
        repliesAdapter = new UserRepliesAdapter();

        // Observe data
        myPostsLive = viewModel.getUserThreads();
        myRepliesLive = viewModel.getUserReplies();
        myStarredLive = viewModel.getStarredThreads();

        myPostsLive.observe(this, threads -> {
            if (currentTab == Tab.POSTS) updateThreads(threads);
        });
        myRepliesLive.observe(this, replies -> {
            if (currentTab == Tab.REPLIES) updateReplies(replies);
        });
        myStarredLive.observe(this, threads -> {
            if (currentTab == Tab.STARRED) updateThreads(threads);
        });

        // Initial
        switchTab(Tab.POSTS);
    }

    private void switchTab(Tab tab) {
        currentTab = tab;
        highlightTab(tab);
        if (tab == Tab.POSTS) {
            recycler.setAdapter(threadAdapter);
            updateThreads(myPostsLive.getValue());
        } else if (tab == Tab.REPLIES) {
            recycler.setAdapter(repliesAdapter);
            updateReplies(myRepliesLive.getValue());
        } else {
            recycler.setAdapter(threadAdapter);
            updateThreads(myStarredLive.getValue());
        }
    }

    private void highlightTab(Tab tab) {
        btnPosts.setSelected(tab == Tab.POSTS);
        btnReplies.setSelected(tab == Tab.REPLIES);
        btnStarred.setSelected(tab == Tab.STARRED);
    }

    private void updateThreads(List<CommunityThread> threads) {
        List<CommunityThread> list = threads != null ? threads : new ArrayList<>();
        threadAdapter.updateThreads(list);
        boolean isEmpty = list.isEmpty();
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (isEmpty) {
            // Hide icon for Starred empty state, show for others
            if (currentTab == Tab.STARRED) {
                if (emptyIcon != null) emptyIcon.setVisibility(View.GONE);
            } else {
                if (emptyIcon != null) emptyIcon.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateReplies(List<CommunityReply> replies) {
        List<UserRepliesAdapter.Item> items = new ArrayList<>();
        if (replies != null) {
            for (CommunityReply r : replies) {
                String title = threadTitleCache.get(r.getThreadId());
                if (title == null) fetchThreadTitle(r.getThreadId());
                items.add(new UserRepliesAdapter.Item(r.getReplyId(), r.getThreadId(), title, r.getContent(), r.getTimestamp()));
            }
        }
        repliesAdapter.update(items);
        boolean isEmpty = items.isEmpty();
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (isEmpty) {
            // Show icon for Replies empty state
            if (emptyIcon != null) emptyIcon.setVisibility(View.VISIBLE);
        }
    }

    private void fetchThreadTitle(String threadId) {
        FirebaseDatabase.getInstance().getReference("community_threads").child(threadId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String title = snapshot.child("title").getValue(String.class);
                        threadTitleCache.put(threadId, title != null ? title : "Thread");
                        repliesAdapter.updateThreadTitle(threadId, threadTitleCache.get(threadId));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { }
                });
    }

    // CommunityThreadAdapter.OnThreadInteractionListener
    @Override
    public void onThreadClick(CommunityThread thread) {
        android.content.Intent intent = new android.content.Intent(this, ThreadDetailActivity.class);
        intent.putExtra("threadId", thread.getThreadId());
        startActivity(intent);
    }

    @Override
    public void onStarClick(CommunityThread thread) {
        viewModel.toggleThreadStar(thread.getThreadId());
    }

    @Override
    public void onReplyClick(CommunityThread thread) {
        android.content.Intent intent = new android.content.Intent(this, ThreadDetailActivity.class);
        intent.putExtra("threadId", thread.getThreadId());
        intent.putExtra("focusReply", true);
        startActivity(intent);
    }
}
