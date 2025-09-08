package com.example.csci3130group1.ui.community.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.csci3130group1.R;
import com.example.csci3130group1.models.CommunityThread;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class CommunityThreadAdapter extends RecyclerView.Adapter<CommunityThreadAdapter.ThreadViewHolder> {
    
    public interface OnThreadInteractionListener {
        void onThreadClick(CommunityThread thread);
        void onStarClick(CommunityThread thread);
        void onReplyClick(CommunityThread thread);
    }

    private List<CommunityThread> threads;
    private final OnThreadInteractionListener listener;

    public CommunityThreadAdapter(List<CommunityThread> threads, OnThreadInteractionListener listener) {
        this.threads = threads;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ThreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_thread, parent, false);
        return new ThreadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ThreadViewHolder holder, int position) {
        CommunityThread thread = threads.get(position);
        holder.bind(thread, listener);
    }

    @Override
    public int getItemCount() {
        return threads.size();
    }

    public void updateThreads(List<CommunityThread> newThreads) {
        this.threads = newThreads;
        notifyDataSetChanged();
    }

    static class ThreadViewHolder extends RecyclerView.ViewHolder {
        private final TextView textAuthorName;
        private final TextView textAuthorRole;
        private final TextView textTimestamp;
        private final TextView textCategory;
        private final TextView textTitle;
        private final TextView textDescription;
        private final ImageButton btnStar;
        private final TextView textStarCount;
        private final ImageButton btnReply;
        private final TextView textReplyCount;
        

        public ThreadViewHolder(@NonNull View itemView) {
            super(itemView);
            textAuthorName = itemView.findViewById(R.id.text_author_name);
            textAuthorRole = itemView.findViewById(R.id.text_author_role);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);
            textCategory = itemView.findViewById(R.id.text_category);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            btnStar = itemView.findViewById(R.id.btn_star);
            textStarCount = itemView.findViewById(R.id.text_star_count);
            btnReply = itemView.findViewById(R.id.btn_reply);
            textReplyCount = itemView.findViewById(R.id.text_reply_count);
        }

        public void bind(CommunityThread thread, OnThreadInteractionListener listener) {
            textAuthorName.setText(thread.getAuthorName());
            textAuthorRole.setText(thread.getAuthorRole());
            textTimestamp.setText(thread.getTimeAgo());
            textCategory.setText(thread.getCategory());
            textTitle.setText(thread.getTitle());
            textDescription.setText(thread.getDescription());
            textStarCount.setText(String.valueOf(thread.getStarCount()));
            textReplyCount.setText(String.valueOf(thread.getReplyCount()));

            // Set role color
            if ("Tutor".equalsIgnoreCase(thread.getAuthorRole())) {
                textAuthorRole.setTextColor(Color.parseColor("#1976D2")); // Blue for tutors
            } else {
                textAuthorRole.setTextColor(Color.parseColor("#A0522D")); // Brand accent for students
            }

            // Check if current user has starred this thread
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                                  FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
            boolean isStarred = currentUserId != null && thread.isStarredByUser(currentUserId);
            btnStar.setImageResource(isStarred ? R.drawable.ic_star_filled : R.drawable.ic_star);

            // Set click listeners
            itemView.setOnClickListener(v -> listener.onThreadClick(thread));
            btnStar.setOnClickListener(v -> listener.onStarClick(thread));
            btnReply.setOnClickListener(v -> listener.onReplyClick(thread));
        }
    }
}
