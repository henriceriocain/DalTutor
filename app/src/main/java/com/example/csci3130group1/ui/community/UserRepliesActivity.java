package com.example.csci3130group1.ui.community;

import android.os.Bundle;
import android.util.ArrayMap;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3130group1.R;
import com.example.csci3130group1.models.CommunityReply;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserRepliesActivity extends AppCompatActivity {
    private CommunityViewModel communityViewModel;
    private UserRepliesAdapter adapter;
    private final Map<String, String> threadTitleCache = new ArrayMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_replies);

        communityViewModel = new ViewModelProvider(this).get(CommunityViewModel.class);
        RecyclerView recycler = findViewById(R.id.recyclerUserReplies);
        View empty = findViewById(R.id.emptyState);
        ImageButton back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> onBackPressed());

        adapter = new UserRepliesAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        communityViewModel.getUserReplies().observe(this, replies -> {
            List<UserRepliesAdapter.Item> items = new ArrayList<>();
            if (replies != null) {
                for (CommunityReply r : replies) {
                    String title = threadTitleCache.get(r.getThreadId());
                    if (title == null) {
                        fetchThreadTitle(r.getThreadId());
                    }
                    items.add(new UserRepliesAdapter.Item(r.getReplyId(), r.getThreadId(), title, r.getContent(), r.getTimestamp()));
                }
            }
            adapter.update(items);
            empty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        });
    }

    private void fetchThreadTitle(String threadId) {
        FirebaseDatabase.getInstance().getReference("community_threads").child(threadId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String title = snapshot.child("title").getValue(String.class);
                        threadTitleCache.put(threadId, title != null ? title : "Thread");
                        adapter.updateThreadTitle(threadId, threadTitleCache.get(threadId));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) { }
                });
    }
}
