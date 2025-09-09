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
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private ChipGroup filters;
    private ListView listView;
    private final List<UnifiedNotification> allItems = new ArrayList<>();
    private final List<UnifiedNotification> displayItems = new ArrayList<>();
    private NotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        filters = root.findViewById(R.id.notificationFilters);
        listView = root.findViewById(R.id.notificationsList);

        adapter = new NotificationAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> onItemClicked(position));

        loadNotifications();

        if (filters != null) {
            filters.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilter());
        }
        return root;
    }

    private void loadNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        allItems.clear();
        // Business notifications (reviews, registrations)
        DatabaseReference businessRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("notifications");
        businessRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    UnifiedNotification n = UnifiedNotification.fromBusiness(child);
                    if (n != null) allItems.add(n);
                }
                applyFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        // Community notifications
        DatabaseReference communityRef = FirebaseDatabase.getInstance().getReference("community_notifications").child(uid);
        communityRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    UnifiedNotification n = UnifiedNotification.fromCommunity(child);
                    if (n != null) allItems.add(n);
                }
                applyFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void applyFilter() {
        if (getContext() == null) return;
        int checkedId = filters != null ? filters.getCheckedChipId() : View.NO_ID;
        String filter = "ALL";
        if (checkedId != View.NO_ID) {
            View chip = filters.findViewById(checkedId);
            if (chip != null) {
                int idx = filters.indexOfChild(chip);
                // Assuming order: All, Business, Community
                if (idx == 1) filter = "BUSINESS";
                else if (idx == 2) filter = "COMMUNITY";
            }
        }

        List<UnifiedNotification> filtered = new ArrayList<>();
        for (UnifiedNotification n : allItems) {
            if ("ALL".equals(filter) || n.category.equals(filter)) {
                filtered.add(n);
            }
        }

        // Sort by timestamp desc
        Collections.sort(filtered, Comparator.comparingLong((UnifiedNotification n) -> n.timestamp).reversed());

        displayItems.clear();
        displayItems.addAll(filtered);
        adapter.notifyDataSetChanged();
    }

    // Simple unified model for mixed notifications
    static class UnifiedNotification {
        String id;
        String category; // BUSINESS or COMMUNITY
        String type; // REVIEW_RECEIVED, REGISTRATION_CREATED, REPLY, STAR, etc.
        String title;
        String body;
        long timestamp;
        String tutorialId;
        String threadId;
        String replyId;
        boolean read;

        static UnifiedNotification fromBusiness(DataSnapshot snap) {
            try {
                UnifiedNotification n = new UnifiedNotification();
                n.id = snap.getKey();
                n.category = "BUSINESS";
                n.type = safeString(snap.child("type").getValue());
                n.timestamp = safeLong(snap.child("timestamp").getValue());
                if ("REVIEW_RECEIVED".equals(n.type)) {
                    String fromEmail = safeString(snap.child("fromUserEmail").getValue());
                    String rating = String.valueOf(snap.child("rating").getValue());
                    n.title = "New review received";
                    n.body = (fromEmail.isEmpty()?"Someone":fromEmail) + " rated you " + rating + "★";
                } else if ("REGISTRATION_CREATED".equals(n.type)) {
                    String studentEmail = safeString(snap.child("studentEmail").getValue());
                    String tutorialTitle = safeString(snap.child("tutorialTitle").getValue());
                    n.tutorialId = safeString(snap.child("tutorialId").getValue());
                    n.title = "New registration";
                    n.body = (studentEmail.isEmpty()?"A student":studentEmail) + " registered for " + tutorialTitle;
                } else {
                    n.title = n.type;
                    n.body = "";
                }
                if (n.timestamp == 0) n.timestamp = System.currentTimeMillis();
                Object readObj = snap.child("read").getValue();
                if (readObj instanceof Boolean) n.read = (Boolean) readObj; else n.read = false;
                return n;
            } catch (Exception e) {
                return null;
            }
        }

        static UnifiedNotification fromCommunity(DataSnapshot snap) {
            try {
                UnifiedNotification n = new UnifiedNotification();
                n.id = snap.getKey();
                n.category = "COMMUNITY";
                String type = safeString(snap.child("type").getValue());
                n.type = type.isEmpty()?"COMMUNITY":type;
                n.timestamp = safeLong(snap.child("timestamp").getValue());
                String actor = safeString(snap.child("actorName").getValue());
                String threadTitle = safeString(snap.child("threadTitle").getValue());
                n.threadId = safeString(snap.child("threadId").getValue());
                n.replyId = safeString(snap.child("replyId").getValue());
                if ("REPLY".equalsIgnoreCase(type)) {
                    n.title = "New reply";
                    n.body = actor + " replied to your thread: " + threadTitle;
                } else if ("STAR".equalsIgnoreCase(type)) {
                    n.title = "Thread starred";
                    n.body = actor + " starred your thread: " + threadTitle;
                } else {
                    n.title = "Community update";
                    n.body = threadTitle;
                }
                if (n.timestamp == 0) n.timestamp = System.currentTimeMillis();
                Object readObj = snap.child("read").getValue();
                if (readObj instanceof Boolean) n.read = (Boolean) readObj; else n.read = false;
                return n;
            } catch (Exception e) {
                return null;
            }
        }

        private static String safeString(Object v) { return v == null ? "" : String.valueOf(v); }
        private static long safeLong(Object v) {
            try { return v == null ? 0L : Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
        }
    }

    private void onItemClicked(int position) {
        if (position < 0 || position >= displayItems.size()) return;
        UnifiedNotification n = displayItems.get(position);
        // Mark as read in DB and locally
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && n.id != null) {
            DatabaseReference ref;
            if ("BUSINESS".equals(n.category)) {
                ref = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("notifications").child(n.id).child("read");
            } else {
                ref = FirebaseDatabase.getInstance().getReference("community_notifications").child(user.getUid()).child(n.id).child("read");
            }
            ref.setValue(true);
            n.read = true;
            adapter.notifyDataSetChanged();
        }
        if ("BUSINESS".equals(n.category)) {
            if ("REVIEW_RECEIVED".equals(n.type)) {
                FirebaseUser user2 = FirebaseAuth.getInstance().getCurrentUser();
                if (user2 != null) {
                    android.content.Intent i = new android.content.Intent(getContext(), com.example.csci3130group1.TutorProfileActivity.class);
                    i.putExtra("tutorId", user2.getUid());
                    i.putExtra("readOnly", true);
                    startActivity(i);
                }
            } else if ("REGISTRATION_CREATED".equals(n.type) && n.tutorialId != null && !n.tutorialId.isEmpty()) {
                android.content.Intent i = new android.content.Intent(getContext(), com.example.csci3130group1.TutorialDetailsActivity.class);
                i.putExtra("tutorialId", n.tutorialId);
                startActivity(i);
            }
        } else if ("COMMUNITY".equals(n.category)) {
            if (n.threadId != null && !n.threadId.isEmpty()) {
                android.content.Intent i = new android.content.Intent(getContext(), com.example.csci3130group1.ui.community.ThreadDetailActivity.class);
                i.putExtra("threadId", n.threadId);
                if ("REPLY".equalsIgnoreCase(n.type) && n.replyId != null && !n.replyId.isEmpty()) {
                    i.putExtra("focusReply", true);
                }
                startActivity(i);
            }
        }
    }

    private class NotificationAdapter extends android.widget.BaseAdapter {
        private final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        @Override public int getCount() { return displayItems.size(); }
        @Override public Object getItem(int position) { return displayItems.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_unified_notification, parent, false);
            }
            UnifiedNotification n = displayItems.get(position);
            android.widget.ImageView icon = v.findViewById(R.id.notifIcon);
            android.widget.TextView title = v.findViewById(R.id.notifTitle);
            android.widget.TextView body = v.findViewById(R.id.notifBody);
            android.widget.TextView time = v.findViewById(R.id.notifTime);
            View unreadDot = v.findViewById(R.id.notifUnreadDot);

            title.setText(n.title != null ? n.title : n.type);
            body.setText(n.body != null ? n.body : "");
            time.setText(df.format(new java.util.Date(n.timestamp)));

            // Icon mapping
            int res = R.drawable.ic_info; // default
            if ("BUSINESS".equals(n.category)) {
                if ("REVIEW_RECEIVED".equals(n.type)) res = R.drawable.ic_star;
                else if ("REGISTRATION_CREATED".equals(n.type)) res = R.drawable.ic_check_circle;
            } else if ("COMMUNITY".equals(n.category)) {
                if ("REPLY".equalsIgnoreCase(n.type)) res = R.drawable.ic_reply;
                else if ("STAR".equalsIgnoreCase(n.type)) res = R.drawable.ic_star;
            }
            icon.setImageResource(res);

            // Read / Unread styling
            boolean isUnread = !n.read;
            unreadDot.setVisibility(isUnread ? View.VISIBLE : View.GONE);
            title.setTypeface(null, isUnread ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            v.setAlpha(isUnread ? 1.0f : 0.92f);
            return v;
        }
    }
}
