package com.example.csci3130group1.ui.community;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3130group1.R;
import com.example.csci3130group1.models.CommunityNotification;

import java.util.ArrayList;
import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotifViewHolder> {
    public interface OnNotificationClickListener {
        void onNotificationClick(CommunityNotification notification);
    }

    private final List<CommunityNotification> items = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationsAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
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

    static class NotifViewHolder extends RecyclerView.ViewHolder {
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
            if ("REPLY".equals(type)) {
                title.setText(who + " replied to your thread");
            } else if ("REPLY_STAR".equals(type)) {
                title.setText(who + " starred your reply");
            } else {
                title.setText(who + " starred your thread");
            }
            subtitle.setText(n.getThreadTitle() != null ? n.getThreadTitle() : "View thread");
            time.setText(getTimeAgo(n.getTimestamp()));
            unreadDot.setVisibility(n.isRead() ? View.INVISIBLE : View.VISIBLE);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(n);
            });
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
