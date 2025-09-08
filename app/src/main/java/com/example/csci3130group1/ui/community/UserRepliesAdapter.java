package com.example.csci3130group1.ui.community;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3130group1.R;

import java.util.ArrayList;
import java.util.List;

public class UserRepliesAdapter extends RecyclerView.Adapter<UserRepliesAdapter.VH> {
    static class Item {
        final String replyId;
        final String threadId;
        String threadTitle;
        final String content;
        final long timestamp;
        Item(String replyId, String threadId, String threadTitle, String content, long timestamp) {
            this.replyId = replyId; this.threadId = threadId; this.threadTitle = threadTitle;
            this.content = content; this.timestamp = timestamp;
        }
    }

    private final List<Item> items = new ArrayList<>();

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_reply, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) { h.bind(items.get(pos)); }

    @Override public int getItemCount() { return items.size(); }

    public void update(List<Item> newItems) {
        items.clear(); if (newItems != null) items.addAll(newItems); notifyDataSetChanged();
    }

    public void updateThreadTitle(String threadId, String title) {
        boolean changed = false;
        for (Item i : items) { if (i.threadId.equals(threadId)) { i.threadTitle = title; changed = true; } }
        if (changed) notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        private final TextView title; private final TextView snippet; private final TextView time;
        VH(@NonNull View itemView) {
            super(itemView); title = itemView.findViewById(R.id.textThreadTitle); snippet = itemView.findViewById(R.id.textReplySnippet); time = itemView.findViewById(R.id.textTime);
        }
        void bind(Item it) {
            Context ctx = itemView.getContext();
            title.setText(it.threadTitle != null ? it.threadTitle : "Thread");
            snippet.setText(it.content);
            time.setText(getTimeAgo(it.timestamp));
            itemView.setOnClickListener(v -> {
                Intent i = new Intent(ctx, ThreadDetailActivity.class);
                i.putExtra("threadId", it.threadId);
                i.putExtra("focusReply", true);
                ctx.startActivity(i);
            });
        }
        private String getTimeAgo(long ts) {
            long d = System.currentTimeMillis() - ts; long m = d/60000, h=m/60, day=h/24; if (day>0) return day+"d"; if (h>0) return h+"h"; if (m>0) return m+"m"; return "now";
        }
    }
}

