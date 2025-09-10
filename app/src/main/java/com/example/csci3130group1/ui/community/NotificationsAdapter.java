package com.example.csci3130group1.ui.community;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3130group1.R;
import com.example.csci3130group1.TutorProfileActivity;
import com.example.csci3130group1.models.CommunityNotification;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotifViewHolder> {
    public interface OnNotificationClickListener {
        void onNotificationClick(CommunityNotification notification);
    }

    private final List<CommunityNotification> items = new ArrayList<>();
    private final OnNotificationClickListener listener;
    private final Map<String, Boolean> tutorFlagCache = new HashMap<>();

    public NotificationsAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_community, parent, false);
        return new NotifViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void update(List<CommunityNotification> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    class NotifViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView subtitle;
        private final TextView time;
        private final ImageView unreadDot;

        NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textTitle);
            subtitle = itemView.findViewById(R.id.textSubtitle);
            time = itemView.findViewById(R.id.textTime);
            unreadDot = itemView.findViewById(R.id.unreadDot);
        }

        void bind(CommunityNotification n, OnNotificationClickListener listener) {
            Context ctx = itemView.getContext();
            String type = n.getType();
            String who = n.getActorName() != null ? n.getActorName() : "Someone";
            String rest;
            if ("REPLY".equals(type)) { rest = " replied to your thread"; }
            else if ("REPLY_STAR".equals(type)) { rest = " starred your reply"; }
            else { rest = " starred your thread"; }

            // default: plain text
            title.setText(who + rest);
            title.setMovementMethod(null);
            title.setHighlightColor(android.graphics.Color.TRANSPARENT);

            // Try to linkify actor name only if tutor tools enabled
            final String actorId = n.getActorUserId();
            if (actorId != null && n.getActorName() != null && !n.getActorName().isEmpty()) {
                Boolean cached = tutorFlagCache.get(actorId);
                if (cached != null) {
                    applyActorLinkIfEnabled(cached, n.getActorName(), rest, actorId);
                } else {
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                            .child(actorId).child("isTutorEnabled");
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(DataSnapshot snapshot) {
                            Boolean enabled = snapshot.getValue(Boolean.class);
                            tutorFlagCache.put(actorId, enabled != null && enabled);
                            // Re-apply only if still bound to same item
                            applyActorLinkIfEnabled(enabled != null && enabled, n.getActorName(), rest, actorId);
                        }
                        @Override public void onCancelled(DatabaseError error) { /* no-op */ }
                    });
                }
            }
            subtitle.setText(n.getThreadTitle() != null ? n.getThreadTitle() : "View thread");
            time.setText(getTimeAgo(n.getTimestamp()));
            unreadDot.setVisibility(n.isRead() ? View.INVISIBLE : View.VISIBLE);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(n);
            });
        }

        private void applyActorLinkIfEnabled(boolean enabled, String actorName, String rest, String actorId) {
            if (!enabled) return;
            String full = actorName + rest;
            SpannableString sp = new SpannableString(full);
            int start = 0;
            int end = actorName.length();
            // Clickable, non-underlined, and colored like profile links
            final int linkColor = itemView.getResources().getColor(R.color.brown_primary);
            sp.setSpan(new ClickableSpan() {
                @Override public void onClick(@NonNull View widget) {
                    Context ctx = widget.getContext();
                    Intent i = new Intent(ctx, TutorProfileActivity.class);
                    i.putExtra("tutorId", actorId);
                    i.putExtra("readOnly", true);
                    ctx.startActivity(i);
                }
                @Override public void updateDrawState(@NonNull android.text.TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                    ds.setColor(linkColor);
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            title.setText(sp);
            title.setMovementMethod(LinkMovementMethod.getInstance());
            title.setHighlightColor(android.graphics.Color.TRANSPARENT);
        }

        private String getTimeAgo(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            long minutes = diff / 60000;
            long hours = minutes / 60;
            long days = hours / 24;
            if (days > 0) return days + "d";
            if (hours > 0) return hours + "h";
            if (minutes > 0) return minutes + "m";
            return "now";
        }
    }
}
