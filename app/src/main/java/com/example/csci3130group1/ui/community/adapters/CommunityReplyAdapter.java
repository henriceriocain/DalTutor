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
import com.example.csci3130group1.models.CommunityReply;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class CommunityReplyAdapter extends RecyclerView.Adapter<CommunityReplyAdapter.ReplyViewHolder> {
    
    public interface OnReplyInteractionListener {
        void onReplyStar(CommunityReply reply);
    }

    private List<CommunityReply> replies;
    private final OnReplyInteractionListener listener;

    public CommunityReplyAdapter(List<CommunityReply> replies, OnReplyInteractionListener listener) {
        this.replies = replies;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_reply, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        CommunityReply reply = replies.get(position);
        holder.bind(reply, listener);
    }

    @Override
    public int getItemCount() {
        return replies.size();
    }

    public void updateReplies(List<CommunityReply> newReplies) {
        this.replies = newReplies;
        notifyDataSetChanged();
    }

    static class ReplyViewHolder extends RecyclerView.ViewHolder {
        private final TextView textAuthorName;
        private final TextView textAuthorRole;
        private final TextView textTimestamp;
        private final TextView textContent;
        private final ImageButton btnStar;
        private final TextView textStarCount;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            textAuthorName = itemView.findViewById(R.id.text_reply_author_name);
            textAuthorRole = itemView.findViewById(R.id.text_reply_author_role);
            textTimestamp = itemView.findViewById(R.id.text_reply_timestamp);
            textContent = itemView.findViewById(R.id.text_reply_content);
            btnStar = itemView.findViewById(R.id.btn_reply_star);
            textStarCount = itemView.findViewById(R.id.text_reply_star_count);
        }

        public void bind(CommunityReply reply, OnReplyInteractionListener listener) {
            textAuthorName.setText(reply.getAuthorName());
            textAuthorRole.setText(reply.getAuthorRole());
            textTimestamp.setText(reply.getTimeAgo());
            textContent.setText(reply.getContent());
            textStarCount.setText(String.valueOf(reply.getStarCount()));

            // Set role color
            if ("Tutor".equalsIgnoreCase(reply.getAuthorRole())) {
                textAuthorRole.setTextColor(Color.parseColor("#1976D2")); // Blue for tutors
            } else {
                textAuthorRole.setTextColor(Color.parseColor("#388E3C")); // Green for students
            }

            // Check if current user has starred this reply
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                                  FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
            boolean isStarred = currentUserId != null && reply.isStarredByUser(currentUserId);
            btnStar.setImageResource(isStarred ? R.drawable.ic_star_filled : R.drawable.ic_star);

            // Set click listener
            btnStar.setOnClickListener(v -> listener.onReplyStar(reply));
        }
    }
}