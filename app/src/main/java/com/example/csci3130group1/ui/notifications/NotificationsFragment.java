package com.example.csci3130group1.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.csci3130group1.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class NotificationsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);

        ChipGroup filters = root.findViewById(R.id.notificationFilters);
        ListView list = root.findViewById(R.id.notificationsList);

        // Placeholder content
        String[] demo = new String[]{
                "New registration for your tutorial",
                "You received a new rating",
                "Community reply on your post"
        };
        list.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, demo));

        // Placeholder filter handling
        if (filters != null) {
            filters.setOnCheckedStateChangeListener((group, checkedIds) -> {
                // no-op for now
            });
        }

        return root;
    }
}

