package com.example.csci3130group1.ui.community.adapters;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class CommunityReplyAdapter extends RecyclerView.Adapter<CommunityReplyAdapter.ReplyViewHolder> {
    
    public interface OnReplyInteractionListener {
        void onReplyStar(CommunityReply reply);
        void onReplyDelete(CommunityReply reply);
    }

    private List<CommunityReply> replies;
    private final OnReplyInteractionListener listener;
    private final java.util.Map<String, Boolean> tutorFlagCache = new java.util.HashMap<>();
    private final int defaultNameColor = android.graphics.Color.parseColor("#111827");

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

    class ReplyViewHolder extends RecyclerView.ViewHolder {
        private final TextView textAuthorName;
        private final TextView textAuthorRole;
        private final TextView textTimestamp;
        private final TextView textContent;
        private final ImageButton btnStar;
        private final ImageButton btnDelete;
        private final TextView textStarCount;
        private final View starContainer;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            textAuthorName = itemView.findViewById(R.id.text_reply_author_name);
            textAuthorRole = itemView.findViewById(R.id.text_reply_author_role);
            textTimestamp = itemView.findViewById(R.id.text_reply_timestamp);
            textContent = itemView.findViewById(R.id.text_reply_content);
            btnStar = itemView.findViewById(R.id.btn_reply_star);
            btnDelete = itemView.findViewById(R.id.btn_reply_delete);
            textStarCount = itemView.findViewById(R.id.text_reply_star_count);
            starContainer = itemView.findViewById(R.id.reply_star_container);
        }

        public void bind(CommunityReply reply, OnReplyInteractionListener listener) {
            textAuthorName.setText(reply.getAuthorName());
            textAuthorName.setTag(reply.getAuthorId());
            // Hide role to keep community neutral
            textAuthorRole.setVisibility(View.GONE);
            textTimestamp.setText(reply.getTimeAgo());
            textContent.setText(reply.getContent());
            textStarCount.setText(String.valueOf(reply.getStarCount()));

            // No role-based coloring

            // Reset link styling by default
            textAuthorName.setTextColor(defaultNameColor);
            textAuthorName.setClickable(false);
            textAuthorName.setOnClickListener(null);

            // Link to tutor profile only if tutor tools enabled for author
            String authorId = reply.getAuthorId();
            if (authorId != null && !authorId.isEmpty()) {
                Boolean cached = tutorFlagCache.get(authorId);
                if (cached != null) {
                    applyTutorLinkStylingIfEnabled(cached, authorId);
                } else {
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                            .child(authorId).child("isTutorEnabled");
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Boolean enabled = snapshot.getValue(Boolean.class);
                            tutorFlagCache.put(authorId, enabled != null && enabled);
                            // Ensure this holder still represents same author
                            Object tag = textAuthorName.getTag();
                            if (tag != null && authorId.equals(tag)) {
                                applyTutorLinkStylingIfEnabled(enabled != null && enabled, authorId);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) { /* no-op */ }
                    });
                }
            }

            // Check if current user has starred this reply
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                                  FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
            boolean isStarred = currentUserId != null && reply.isStarredByUser(currentUserId);
            btnStar.setImageResource(isStarred ? R.drawable.ic_star_filled : R.drawable.ic_star);

            // Set click listeners: icon, count, and container all star
            View.OnClickListener onStar = v -> listener.onReplyStar(reply);
            btnStar.setOnClickListener(onStar);
            textStarCount.setOnClickListener(onStar);
            if (starContainer != null) starContainer.setOnClickListener(onStar);

            // Show delete icon if reply belongs to current user
            if (currentUserId != null && currentUserId.equals(reply.getAuthorId())) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> listener.onReplyDelete(reply));
            } else {
                btnDelete.setVisibility(View.GONE);
            }
        }

        private void applyTutorLinkStylingIfEnabled(boolean enabled, String authorId) {
            if (!enabled) return;
            // Apply link color and click listener to open TutorProfileActivity
            textAuthorName.setTextColor(itemView.getResources().getColor(R.color.brown_primary));
            textAuthorName.setClickable(true);
            textAuthorName.setOnClickListener(v -> {
                android.content.Context ctx = itemView.getContext();
                android.content.Intent i = new android.content.Intent(ctx, com.example.csci3130group1.TutorProfileActivity.class);
                i.putExtra("tutorId", authorId);
                i.putExtra("readOnly", true);
                ctx.startActivity(i);
            });
        }
    }
}
