package com.example.csci3130group1.ui.community.adapters;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class CommunityThreadAdapter extends RecyclerView.Adapter<CommunityThreadAdapter.ThreadViewHolder> {
    
    public interface OnThreadInteractionListener {
        void onThreadClick(CommunityThread thread);
        void onStarClick(CommunityThread thread);
        void onReplyClick(CommunityThread thread);
    }

    private List<CommunityThread> threads;
    private final OnThreadInteractionListener listener;
    private final java.util.Map<String, Boolean> tutorFlagCache = new java.util.HashMap<>();
    private final int defaultNameColor = android.graphics.Color.parseColor("#111827");
    // Live author-name cache + listeners to reflect profile changes
    private final com.google.firebase.database.DatabaseReference usersRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users");
    private final java.util.Map<String, com.google.firebase.database.ValueEventListener> nameListeners = new java.util.HashMap<>();
    private final java.util.Map<String, String> authorNameCache = new java.util.HashMap<>();

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
        // Maintain live name listeners for authors present in the list
        java.util.Set<String> present = new java.util.HashSet<>();
        for (CommunityThread t : newThreads) {
            String aid = t.getAuthorId();
            if (aid != null && !aid.isEmpty()) present.add(aid);
        }
        // Attach for new authors
        for (String aid : present) attachNameListenerIfNeeded(aid);
        // Detach for authors no longer present
        java.util.Set<String> tracked = new java.util.HashSet<>(nameListeners.keySet());
        for (String trackedId : tracked) {
            if (!present.contains(trackedId)) detachNameListener(trackedId);
        }
        notifyDataSetChanged();
    }

    class ThreadViewHolder extends RecyclerView.ViewHolder {
        private final TextView textAuthorName;
        private final TextView textAuthorRole;
        private final TextView textTimestamp;
        private final TextView textCategory;
        private final TextView textTitle;
        private final TextView textDescription;
        private final TextView textEditedIndicator;
        private final ImageButton btnStar;
        private final TextView textStarCount;
        private final ImageButton btnReply;
        private final TextView textReplyCount;
        private final View starContainer;
        private final View replyContainer;
        

        public ThreadViewHolder(@NonNull View itemView) {
            super(itemView);
            textAuthorName = itemView.findViewById(R.id.text_author_name);
            textAuthorRole = itemView.findViewById(R.id.text_author_role);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);
            textCategory = itemView.findViewById(R.id.text_category);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            textEditedIndicator = itemView.findViewById(R.id.text_edited);
            btnStar = itemView.findViewById(R.id.btn_star);
            textStarCount = itemView.findViewById(R.id.text_star_count);
            btnReply = itemView.findViewById(R.id.btn_reply);
            textReplyCount = itemView.findViewById(R.id.text_reply_count);
            starContainer = itemView.findViewById(R.id.star_container);
            replyContainer = itemView.findViewById(R.id.reply_container);
        }

        public void bind(CommunityThread thread, OnThreadInteractionListener listener) {
            // Prefer live-updated author name from cache; fall back to snapshot value
            String authorId = thread.getAuthorId();
            String liveName = (authorId != null) ? authorNameCache.get(authorId) : null;
            textAuthorName.setText(liveName != null ? liveName : thread.getAuthorName());
            textAuthorName.setTag(thread.getAuthorId());
            // Hide role to keep community neutral
            textAuthorRole.setVisibility(View.GONE);
            textTimestamp.setText(thread.getTimeAgo());
            if (textEditedIndicator != null) {
                boolean edited = false;
                try { edited = thread.isEdited() || thread.getEditedAt() > 0; } catch (Throwable ignored) { }
                textEditedIndicator.setVisibility(edited ? View.VISIBLE : View.GONE);
            }
            textCategory.setText(thread.getCategory());
            textTitle.setText(thread.getTitle());
            textDescription.setText(thread.getDescription());
            textStarCount.setText(String.valueOf(thread.getStarCount()));
            textReplyCount.setText(String.valueOf(thread.getReplyCount()));

            // Default styling (non-link)
            textAuthorName.setTextColor(defaultNameColor);
            textAuthorName.setClickable(false);
            textAuthorName.setOnClickListener(null);

            // Tutor link behavior only if author has tutor tools enabled
            if (authorId != null && !authorId.isEmpty()) attachNameListenerIfNeeded(authorId);
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

            // Check if current user has starred this thread
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                                  FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
            boolean isStarred = currentUserId != null && thread.isStarredByUser(currentUserId);
            btnStar.setImageResource(isStarred ? R.drawable.ic_star_filled : R.drawable.ic_star);

            // Set click listeners
            itemView.setOnClickListener(v -> listener.onThreadClick(thread));
            // Star: icon, count, and container all trigger toggle
            View.OnClickListener onStar = v -> listener.onStarClick(thread);
            btnStar.setOnClickListener(onStar);
            textStarCount.setOnClickListener(onStar);
            if (starContainer != null) starContainer.setOnClickListener(onStar);
            // Reply: icon, count, and container all open detail with reply focus
            View.OnClickListener onReply = v -> listener.onReplyClick(thread);
            btnReply.setOnClickListener(onReply);
            textReplyCount.setOnClickListener(onReply);
            if (replyContainer != null) replyContainer.setOnClickListener(onReply);
        }

        private void applyTutorLinkStylingIfEnabled(boolean enabled, String authorId) {
            if (!enabled) return;
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

        private void onAuthorNameUpdated(String authorId, String newName) {
            // Update the in-memory list so future binds use the latest name
            for (CommunityThread t : threads) {
                if (authorId.equals(t.getAuthorId())) t.setAuthorName(newName);
            }
        }
    }

    private void attachNameListenerIfNeeded(String authorId) {
        if (authorId == null || authorId.isEmpty() || nameListeners.containsKey(authorId)) return;
        com.google.firebase.database.ValueEventListener l = new com.google.firebase.database.ValueEventListener() {
            @Override public void onDataChange(com.google.firebase.database.DataSnapshot snap) {
                String newName = snap.getValue(String.class);
                if (newName == null || newName.trim().isEmpty()) return;
                authorNameCache.put(authorId, newName);
                // Reflect in local list and refresh visible items
                for (CommunityThread t : threads) {
                    if (authorId.equals(t.getAuthorId())) t.setAuthorName(newName);
                }
                notifyDataSetChanged();
            }
            @Override public void onCancelled(com.google.firebase.database.DatabaseError error) { }
        };
        usersRef.child(authorId).child("name").addValueEventListener(l);
        nameListeners.put(authorId, l);
    }

    private void detachNameListener(String authorId) {
        com.google.firebase.database.ValueEventListener l = nameListeners.remove(authorId);
        if (l != null) {
            usersRef.child(authorId).child("name").removeEventListener(l);
        }
        authorNameCache.remove(authorId);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        java.util.Set<String> ids = new java.util.HashSet<>(nameListeners.keySet());
        for (String id : ids) detachNameListener(id);
    }
}
