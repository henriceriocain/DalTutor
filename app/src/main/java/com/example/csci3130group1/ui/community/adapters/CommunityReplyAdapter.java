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
        void onReplyTo(CommunityReply reply);
    }

    private List<CommunityReply> replies;
    private final OnReplyInteractionListener listener;
    private final java.util.Map<String, Boolean> tutorFlagCache = new java.util.HashMap<>();
    private final int defaultNameColor = android.graphics.Color.parseColor("#111827");
    private final java.util.Map<String, CommunityReply> replyById = new java.util.HashMap<>();
    private static final int MAX_DEPTH = 4; // 0..4 -> 5 levels

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
        replyById.clear();
        for (CommunityReply r : newReplies) if (r.getReplyId() != null) replyById.put(r.getReplyId(), r);
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
        private final TextView textInReplyTo;
        private final TextView btnReplyTo;

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
            textInReplyTo = itemView.findViewById(R.id.text_in_reply_to);
            btnReplyTo = itemView.findViewById(R.id.btn_reply_to);
        }

        public void bind(CommunityReply reply, OnReplyInteractionListener listener) {
            textAuthorName.setText(reply.getAuthorName());
            textAuthorName.setTag(reply.getAuthorId());
            // Hide role to keep community neutral
            textAuthorRole.setVisibility(View.GONE);
            textTimestamp.setText(reply.getTimeAgo());
            textStarCount.setText(String.valueOf(reply.getStarCount()));

            // Indentation by depth (Apple/OpenAI-like subtle structure)
            int depth = Math.max(0, reply.getDepth());
            int indentDp = 12 * depth; // 12dp per level
            ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            if (lp instanceof RecyclerView.LayoutParams) {
                RecyclerView.LayoutParams rlp = (RecyclerView.LayoutParams) lp;
                if (android.os.Build.VERSION.SDK_INT >= 17) {
                    rlp.setMarginStart(dp(itemView, indentDp));
                } else {
                    rlp.leftMargin = dp(itemView, indentDp);
                }
                itemView.setLayoutParams(rlp);
            } else {
                itemView.setPadding(dp(itemView, indentDp), itemView.getPaddingTop(), itemView.getPaddingRight(), itemView.getPaddingBottom());
            }

            // In-reply-to label
            if (reply.getParentReplyId() != null && !reply.getParentReplyId().isEmpty()) {
                CommunityReply parent = replyById.get(reply.getParentReplyId());
                String who = parent != null && parent.getAuthorName() != null ? parent.getAuthorName() : "original reply";
                textInReplyTo.setText("\u21AA In reply to " + who);
                textInReplyTo.setVisibility(View.VISIBLE);
            } else {
                textInReplyTo.setVisibility(View.GONE);
            }

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

            // Deleted state handling (OpenAI/Apple-like subtle style)
            if (reply.isDeleted()) {
                textContent.setText("This message has been deleted");
                textContent.setTextColor(android.graphics.Color.parseColor("#6B7280"));
                textContent.setTypeface(null, android.graphics.Typeface.ITALIC);
                if (starContainer != null) starContainer.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
                if (btnReplyTo != null) btnReplyTo.setVisibility(View.GONE);
                itemView.setAlpha(0.96f);
            } else {
                textContent.setText(reply.getContent());
                textContent.setTextColor(android.graphics.Color.parseColor("#111827"));
                textContent.setTypeface(null, android.graphics.Typeface.NORMAL);
                if (starContainer != null) starContainer.setVisibility(View.VISIBLE);
                itemView.setAlpha(1.0f);
            }

            // Check if current user has starred this reply
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                                  FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
            boolean isStarred = currentUserId != null && reply.isStarredByUser(currentUserId);
            btnStar.setImageResource(isStarred ? R.drawable.ic_star_filled : R.drawable.ic_star);

            // Set click listeners: icon, count, and container all star
            if (!reply.isDeleted()) {
                View.OnClickListener onStar = v -> listener.onReplyStar(reply);
                btnStar.setOnClickListener(onStar);
                textStarCount.setOnClickListener(onStar);
                if (starContainer != null) starContainer.setOnClickListener(onStar);
            } else {
                btnStar.setOnClickListener(null);
                textStarCount.setOnClickListener(null);
                if (starContainer != null) starContainer.setOnClickListener(null);
            }

            // Reply action
            if (!reply.isDeleted() && depth < MAX_DEPTH) {
                btnReplyTo.setVisibility(View.VISIBLE);
                btnReplyTo.setOnClickListener(v -> listener.onReplyTo(reply));
            } else {
                btnReplyTo.setVisibility(View.GONE);
            }

            // Show delete icon if reply belongs to current user
            if (!reply.isDeleted() && currentUserId != null && currentUserId.equals(reply.getAuthorId())) {
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

        private int dp(View v, int value) {
            float d = v.getResources().getDisplayMetrics().density;
            return (int) (value * d);
        }
    }
}
