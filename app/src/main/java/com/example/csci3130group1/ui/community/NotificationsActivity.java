package com.example.csci3130group1.ui.community;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3130group1.R;
import com.example.csci3130group1.models.CommunityNotification;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {
    private CommunityViewModel communityViewModel;
    private NotificationsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        communityViewModel = new ViewModelProvider(this).get(CommunityViewModel.class);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView btnMarkAll = findViewById(R.id.btnMarkAllRead);
        RecyclerView recycler = findViewById(R.id.recyclerNotifications);
        View empty = findViewById(R.id.emptyState);

        adapter = new NotificationsAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        btnBack.setOnClickListener(v -> onBackPressed());
        btnMarkAll.setOnClickListener(v -> communityViewModel.markAllNotificationsRead());

        communityViewModel.getNotifications().observe(this, list -> {
            List<CommunityNotification> items = list != null ? list : new ArrayList<>();
            adapter.update(items);
            empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}
