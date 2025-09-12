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

    private android.view.View filtersContainer;
    private com.google.android.material.button.MaterialButton btnAll;
    private com.google.android.material.button.MaterialButton btnBusiness;
    private com.google.android.material.button.MaterialButton btnCommunity;
    private android.widget.TextView scopeHeader;
    private android.widget.TextView btnMarkAll;
    private android.widget.TextView btnClear;
    private android.view.View scopeCard;
    private android.view.View actionsStandalone;
    private android.widget.TextView btnMarkAllStandalone;
    private android.widget.TextView btnClearStandalone;
    private androidx.recyclerview.widget.RecyclerView recyclerView;
    private View emptyState;
    private final List<UnifiedNotification> allItems = new ArrayList<>();
    private final List<UnifiedNotification> displayItems = new ArrayList<>();
    private NotificationAdapter adapter;
    private String currentFilter = "ALL";
    private boolean filtersForcedCommunity = false; // when Tutor Tools disabled, hide filters and scope to community

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        filtersContainer = root.findViewById(R.id.filterToggleGroup);
        btnAll = root.findViewById(R.id.btn_all);
        btnBusiness = root.findViewById(R.id.btn_business);
        btnCommunity = root.findViewById(R.id.btn_community);
        scopeHeader = root.findViewById(R.id.scopeHeader);
        btnMarkAll = root.findViewById(R.id.btnMarkAllRead);
        btnClear = root.findViewById(R.id.btnClear);
        scopeCard = root.findViewById(R.id.scopeCard);
        actionsStandalone = root.findViewById(R.id.notificationActionsStandalone);
        btnMarkAllStandalone = root.findViewById(R.id.btnMarkAllReadStandalone);
        btnClearStandalone = root.findViewById(R.id.btnClearStandalone);
        recyclerView = root.findViewById(R.id.notificationsRecycler);
        emptyState = root.findViewById(R.id.emptyState);

        adapter = new NotificationAdapter();
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Setup individual button click listeners
        setupFilterButtons();

        // Before loading, configure visibility of Business tab by Tutor Tools flag
        configureBusinessTabVisibility(root);

        loadNotifications();

        if (btnMarkAll != null) btnMarkAll.setOnClickListener(v -> markAllReadForScope());
        if (btnClear != null) btnClear.setOnClickListener(v -> clearForScope());
        if (btnMarkAllStandalone != null) btnMarkAllStandalone.setOnClickListener(v -> markAllReadForScope());
        if (btnClearStandalone != null) btnClearStandalone.setOnClickListener(v -> clearForScope());
        return root;
    }

    private void setupFilterButtons() {
        if (btnAll != null) {
            btnAll.setOnClickListener(v -> {
                selectFilter("ALL");
                applyFilter();
            });
        }
        if (btnBusiness != null) {
            btnBusiness.setOnClickListener(v -> {
                selectFilter("BUSINESS");
                applyFilter();
            });
        }
        if (btnCommunity != null) {
            btnCommunity.setOnClickListener(v -> {
                selectFilter("COMMUNITY");
                applyFilter();
            });
        }
        
        // Set initial selection
        selectFilter("ALL");
    }

    private void selectFilter(String filter) {
        currentFilter = filter;
        
        // Update visual states
        if (btnAll != null) {
            btnAll.setChecked("ALL".equals(filter));
        }
        if (btnBusiness != null) {
            btnBusiness.setChecked("BUSINESS".equals(filter));
        }
        if (btnCommunity != null) {
            btnCommunity.setChecked("COMMUNITY".equals(filter));
        }
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
        // When filters are hidden for non-tutor users, scope to COMMUNITY
        if (filtersContainer == null || filtersContainer.getVisibility() != View.VISIBLE || filtersForcedCommunity) {
            currentFilter = "COMMUNITY";
            selectFilter("COMMUNITY");
        }

        List<UnifiedNotification> filtered = new ArrayList<>();
        for (UnifiedNotification n : allItems) {
            if ("ALL".equals(currentFilter) || n.category.equals(currentFilter)) {
                filtered.add(n);
            }
        }

        // Sort by timestamp desc
        Collections.sort(filtered, Comparator.comparingLong((UnifiedNotification n) -> n.timestamp).reversed());

        displayItems.clear();
        displayItems.addAll(filtered);
        adapter.notifyDataSetChanged();
        if (emptyState != null) {
            emptyState.setVisibility(displayItems.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void configureBusinessTabVisibility(View root) {
        if (btnBusiness == null || filtersContainer == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            btnBusiness.setVisibility(View.GONE);
            filtersContainer.setVisibility(View.GONE);
            if (scopeHeader != null) scopeHeader.setVisibility(View.GONE);
            // Show standalone actions (light grey) and hide card
            if (scopeCard != null) scopeCard.setVisibility(View.GONE);
            if (actionsStandalone != null) actionsStandalone.setVisibility(View.VISIBLE);
            if (btnMarkAllStandalone != null) btnMarkAllStandalone.setTextColor(0xFF6B7280);
            if (btnClearStandalone != null) btnClearStandalone.setTextColor(0xFF6B7280);
            filtersForcedCommunity = true;
            currentFilter = "COMMUNITY";
            return;
        }
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("isTutorEnabled")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean enabled = snapshot.getValue(Boolean.class);
                        boolean show = enabled != null && enabled;
                        btnBusiness.setVisibility(show ? View.VISIBLE : View.GONE);
                        if (show) {
                            // Show full filters for tutors
                            filtersContainer.setVisibility(View.VISIBLE);
                            if (scopeHeader != null) scopeHeader.setVisibility(View.VISIBLE);
                            // Show card with in-card actions; hide standalone actions
                            if (scopeCard != null) scopeCard.setVisibility(View.VISIBLE);
                            if (actionsStandalone != null) actionsStandalone.setVisibility(View.GONE);
                            if (btnMarkAll != null) btnMarkAll.setTextColor(0xFF111827);
                            if (btnClear != null) btnClear.setTextColor(0xFF111827);
                            filtersForcedCommunity = false;
                            // Ensure a default selection exists
                            selectFilter("ALL");
                        } else {
                            // Hide filters entirely for non-tutors; scope to community
                            filtersContainer.setVisibility(View.GONE);
                            if (scopeHeader != null) scopeHeader.setVisibility(View.GONE);
                            if (scopeCard != null) scopeCard.setVisibility(View.GONE);
                            if (actionsStandalone != null) actionsStandalone.setVisibility(View.VISIBLE);
                            if (btnMarkAllStandalone != null) btnMarkAllStandalone.setTextColor(0xFF6B7280);
                            if (btnClearStandalone != null) btnClearStandalone.setTextColor(0xFF6B7280);
                            filtersForcedCommunity = true;
                            currentFilter = "COMMUNITY";
                        }
                        applyFilter();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        btnBusiness.setVisibility(View.GONE);
                        filtersContainer.setVisibility(View.GONE);
                        filtersForcedCommunity = true;
                        currentFilter = "COMMUNITY";
                        applyFilter();
                    }
                });
    }

    private void markAllReadForScope() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        List<String> targets = new ArrayList<>();
        if ("ALL".equals(currentFilter) || "BUSINESS".equals(currentFilter)) targets.add("BUSINESS");
        if ("ALL".equals(currentFilter) || "COMMUNITY".equals(currentFilter)) targets.add("COMMUNITY");

        for (String t : targets) {
            if ("BUSINESS".equals(t)) {
                DatabaseReference bizRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("notifications");
                bizRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            child.getRef().child("read").setValue(true);
                        }
                        // Update UI locally
                        for (UnifiedNotification n : allItems) if ("BUSINESS".equals(n.category)) n.read = true;
                        adapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { }
                });
            } else if ("COMMUNITY".equals(t)) {
                DatabaseReference comRef = FirebaseDatabase.getInstance().getReference("community_notifications").child(uid);
                comRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            child.getRef().child("read").setValue(true);
                        }
                        for (UnifiedNotification n : allItems) if ("COMMUNITY".equals(n.category)) n.read = true;
                        adapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { }
                });
            }
        }
    }

    private void clearForScope() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        boolean clearBusiness = "ALL".equals(currentFilter) || "BUSINESS".equals(currentFilter);
        boolean clearCommunity = "ALL".equals(currentFilter) || "COMMUNITY".equals(currentFilter);

        if (clearBusiness) {
            DatabaseReference bizRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("notifications");
            bizRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        child.getRef().removeValue();
                    }
                    // Remove from UI model
                    allItems.removeIf(n -> "BUSINESS".equals(n.category));
                    applyFilter();
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { }
            });
        }

        if (clearCommunity) {
            DatabaseReference comRef = FirebaseDatabase.getInstance().getReference("community_notifications").child(uid);
            comRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        child.getRef().removeValue();
                    }
                    allItems.removeIf(n -> "COMMUNITY".equals(n.category));
                    applyFilter();
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
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
        String actorUserId; // for COMMUNITY
        String actorName;   // for COMMUNITY
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
                } else if ("REGISTRATION_CANCELLED".equals(n.type)) {
                    String studentEmail = safeString(snap.child("studentEmail").getValue());
                    String tutorialTitle = safeString(snap.child("tutorialTitle").getValue());
                    n.tutorialId = safeString(snap.child("tutorialId").getValue());
                    n.title = "Registration cancelled";
                    n.body = (studentEmail.isEmpty()?"A student":studentEmail) + " cancelled their registration for " + tutorialTitle;
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
                // Only accept direct user-related types; ignore generic updates
                if (!("REPLY".equalsIgnoreCase(type) || "STAR".equalsIgnoreCase(type) || "REPLY_STAR".equalsIgnoreCase(type) || "REPLY_TO_REPLY".equalsIgnoreCase(type))) {
                    return null;
                }
                n.timestamp = safeLong(snap.child("timestamp").getValue());
                String actor = safeString(snap.child("actorName").getValue());
                n.actorName = actor;
                n.actorUserId = safeString(snap.child("actorUserId").getValue());
                String threadTitle = safeString(snap.child("threadTitle").getValue());
                n.threadId = safeString(snap.child("threadId").getValue());
                n.replyId = safeString(snap.child("replyId").getValue());
                if ("REPLY".equalsIgnoreCase(type)) {
                    n.title = "New reply";
                    n.body = actor + " replied to your thread: " + threadTitle;
                } else if ("REPLY_TO_REPLY".equalsIgnoreCase(type)) {
                    n.title = "New reply";
                    n.body = actor + " replied to your reply";
                } else if ("STAR".equalsIgnoreCase(type)) {
                    n.title = "Thread starred";
                    n.body = actor + " starred your thread: " + threadTitle;
                } else {
                    // REPLY_STAR
                    n.title = "Reply starred";
                    n.body = actor + " starred your reply";
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
            } else if (("REGISTRATION_CREATED".equals(n.type) || "REGISTRATION_CANCELLED".equals(n.type)) && n.tutorialId != null && !n.tutorialId.isEmpty()) {
                android.content.Intent i = new android.content.Intent(getContext(), com.example.csci3130group1.TutorialDetailsActivity.class);
                i.putExtra("tutorialId", n.tutorialId);
                startActivity(i);
            }
        } else if ("COMMUNITY".equals(n.category)) {
            if (n.threadId != null && !n.threadId.isEmpty()) {
                android.content.Intent i = new android.content.Intent(getContext(), com.example.csci3130group1.ui.community.ThreadDetailActivity.class);
                i.putExtra("threadId", n.threadId);
                if (("REPLY".equalsIgnoreCase(n.type) || "REPLY_TO_REPLY".equalsIgnoreCase(n.type)) && n.replyId != null && !n.replyId.isEmpty()) {
                    i.putExtra("focusReply", true);
                }
                startActivity(i);
            }
        }
    }

    private class NotificationAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<NotificationAdapter.VH> {
        private final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

        class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.TextView title;
            android.widget.TextView body;
            android.widget.TextView time;
            View dot;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.notifTitle);
                body = v.findViewById(R.id.notifBody);
                time = v.findViewById(R.id.notifTime);
                dot = v.findViewById(R.id.notifDot);
                v.setOnClickListener(_v -> {
                    int pos = getAdapterPosition();
                    if (pos != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                        onItemClicked(pos);
                    }
                });
            }
        }

        @Override public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_unified_notification, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(VH holder, int position) {
            UnifiedNotification n = displayItems.get(position);
            holder.title.setText(n.title != null ? n.title : n.type);
            // Default body
            holder.body.setText(n.body != null ? n.body : "");
            holder.body.setMovementMethod(null);
            holder.body.setHighlightColor(android.graphics.Color.TRANSPARENT);
            // Linkify actor name for community notifications when tutor tools enabled
            if ("COMMUNITY".equals(n.category) && n.actorUserId != null && n.actorName != null && !n.actorName.isEmpty() && n.body != null && n.body.startsWith(n.actorName)) {
                // Fetch tutor flag and apply span if enabled
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
                        .child(n.actorUserId).child("isTutorEnabled")
                        .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                                Boolean enabled = snapshot.getValue(Boolean.class);
                                if (enabled != null && enabled) {
                                    String rest = n.body.substring(n.actorName.length());
                                    android.text.SpannableString sp = new android.text.SpannableString(n.actorName + rest);
                                    int start = 0; int end = n.actorName.length();
                                    final int linkColor = holder.itemView.getResources().getColor(com.example.csci3130group1.R.color.brown_primary);
                                    sp.setSpan(new android.text.style.ClickableSpan() {
                                        @Override public void onClick(@NonNull android.view.View widget) {
                                            android.content.Context ctx = widget.getContext();
                                            android.content.Intent i = new android.content.Intent(ctx, com.example.csci3130group1.TutorProfileActivity.class);
                                            i.putExtra("tutorId", n.actorUserId);
                                            i.putExtra("readOnly", true);
                                            ctx.startActivity(i);
                                        }
                                        @Override public void updateDrawState(@NonNull android.text.TextPaint ds) {
                                            super.updateDrawState(ds);
                                            ds.setUnderlineText(false);
                                            ds.setColor(linkColor);
                                        }
                                    }, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    holder.body.setText(sp);
                                    holder.body.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                                    holder.body.setHighlightColor(android.graphics.Color.TRANSPARENT);
                                }
                            }
                            @Override public void onCancelled(com.google.firebase.database.DatabaseError error) { }
                        });
            }
            holder.time.setText(df.format(new java.util.Date(n.timestamp)));
            boolean isUnread = !n.read;
            // Dot color: red unread, grey read
            holder.dot.setBackgroundResource(isUnread ? R.drawable.badge_red_dot : R.drawable.badge_grey_dot);
            holder.title.setTypeface(null, isUnread ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            holder.itemView.setAlpha(isUnread ? 1.0f : 0.96f);
        }
        @Override public int getItemCount() { return displayItems.size(); }
    }
}
