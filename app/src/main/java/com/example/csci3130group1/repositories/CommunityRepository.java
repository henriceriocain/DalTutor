package com.example.csci3130group1.repositories;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.csci3130group1.models.CommunityThread;
import com.example.csci3130group1.models.CommunityReply;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class CommunityRepository {
    private static CommunityRepository instance;
    private final DatabaseReference database;
    private final DatabaseReference threadsRef;
    private final DatabaseReference repliesRef;
    private final DatabaseReference usersRef;
    private final DatabaseReference notificationsRef;

    private CommunityRepository() {
        database = FirebaseDatabase.getInstance().getReference();
        threadsRef = database.child("community_threads");
        repliesRef = database.child("community_replies");
        usersRef = database.child("users");
        notificationsRef = database.child("community_notifications");
    }

    public static synchronized CommunityRepository getInstance() {
        if (instance == null) {
            instance = new CommunityRepository();
        }
        return instance;
    }

    // Thread operations
    public interface ThreadCreationCallback {
        void onSuccess(String threadId);
        void onFailure(String error);
    }

    public void createThread(CommunityThread thread, ThreadCreationCallback callback) {
        String threadId = threadsRef.push().getKey();
        if (threadId != null) {
            thread.setThreadId(threadId);
            threadsRef.child(threadId).setValue(thread)
                .addOnSuccessListener(aVoid -> callback.onSuccess(threadId))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        } else {
            callback.onFailure("Failed to generate thread ID");
        }
    }

    public LiveData<List<CommunityThread>> getThreads(String sortBy, String filterCategory, String timeFilter) {
        return new ThreadsLiveData(sortBy, filterCategory, timeFilter);
    }

    private class ThreadsLiveData extends LiveData<List<CommunityThread>> {
        private final String sortBy;
        private final String filterCategory;
        private final String timeFilter;
        private ValueEventListener valueEventListener;
        private Query query;

        public ThreadsLiveData(String sortBy, String filterCategory, String timeFilter) {
            this.sortBy = sortBy;
            this.filterCategory = filterCategory;
            this.timeFilter = timeFilter;
        }

        @Override
        protected void onActive() {
            super.onActive();
            startListening();
        }

        @Override
        protected void onInactive() {
            super.onInactive();
            stopListening();
        }

        private void startListening() {
            query = threadsRef;
            
            // Apply time filter to Firebase query for efficiency
            if (timeFilter != null && !timeFilter.equals("All Time")) {
                long timeThreshold = getTimeThreshold(timeFilter);
                query = query.orderByChild("timestamp").startAt(timeThreshold);
            }

            valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<CommunityThread> threads = new ArrayList<>();
                    for (DataSnapshot threadSnapshot : snapshot.getChildren()) {
                        CommunityThread thread = threadSnapshot.getValue(CommunityThread.class);
                        if (thread != null) {
                            thread.setThreadId(threadSnapshot.getKey());
                            
                            // Apply category filter
                            if (filterCategory == null || filterCategory.equals("All Categories") || 
                                filterCategory.equals(thread.getCategory())) {
                                threads.add(thread);
                            }
                        }
                    }
                    
                    // Sort threads
                    sortThreads(threads, sortBy);
                    setValue(threads);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    setValue(new ArrayList<>());
                }
            };

            query.addValueEventListener(valueEventListener);
        }

        private void stopListening() {
            if (query != null && valueEventListener != null) {
                query.removeEventListener(valueEventListener);
            }
        }
    }

    public LiveData<List<CommunityThread>> getUserThreads(String userId) {
        MutableLiveData<List<CommunityThread>> threadsLiveData = new MutableLiveData<>();
        
        threadsRef.orderByChild("authorId").equalTo(userId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<CommunityThread> threads = new ArrayList<>();
                    for (DataSnapshot threadSnapshot : snapshot.getChildren()) {
                        CommunityThread thread = threadSnapshot.getValue(CommunityThread.class);
                        if (thread != null) {
                            thread.setThreadId(threadSnapshot.getKey());
                            threads.add(thread);
                        }
                    }
                    
                    // Sort by newest first
                    Collections.sort(threads, (t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));
                    threadsLiveData.setValue(threads);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    threadsLiveData.setValue(new ArrayList<>());
                }
            });

        return threadsLiveData;
    }

    public LiveData<List<CommunityThread>> getStarredThreads(String userId) {
        MutableLiveData<List<CommunityThread>> live = new MutableLiveData<>();
        threadsRef.orderByChild("stars/" + userId).equalTo(true)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<CommunityThread> threads = new ArrayList<>();
                        for (DataSnapshot threadSnap : snapshot.getChildren()) {
                            CommunityThread t = threadSnap.getValue(CommunityThread.class);
                            if (t != null) {
                                t.setThreadId(threadSnap.getKey());
                                threads.add(t);
                            }
                        }
                        Collections.sort(threads, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        live.setValue(threads);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        live.setValue(new ArrayList<>());
                    }
                });
        return live;
    }

    // Reply operations
    public interface ReplyCreationCallback {
        void onSuccess(String replyId);
        void onFailure(String error);
    }

    public void createReply(CommunityReply reply, ReplyCreationCallback callback) {
        String replyId = repliesRef.push().getKey();
        if (replyId != null) {
            reply.setReplyId(replyId);
            
            // Create reply and update thread reply count
            Map<String, Object> updates = new HashMap<>();
            updates.put("/community_replies/" + replyId, reply);
            updates.put("/community_threads/" + reply.getThreadId() + "/replies/" + replyId, true);
            
            database.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update reply count
                    updateThreadReplyCount(reply.getThreadId());
                    // Notifications for reply: thread author and, if nested, parent reply author
                    threadsRef.child(reply.getThreadId()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            CommunityThread thread = snapshot.getValue(CommunityThread.class);
                            if (thread == null) { callback.onSuccess(replyId); return; }

                            java.util.Set<String> recipients = new java.util.HashSet<>();
                            String actorId = reply.getAuthorId();

                            String threadAuthorId = thread.getAuthorId();
                            if (threadAuthorId != null && !threadAuthorId.equals(actorId)) {
                                recipients.add(threadAuthorId);
                                createNotificationForReply(threadAuthorId, thread, reply);
                            }

                            String parentId = reply.getParentReplyId();
                            if (parentId != null && !parentId.isEmpty()) {
                                repliesRef.child(parentId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot parentSnap) {
                                        CommunityReply parent = parentSnap.getValue(CommunityReply.class);
                                        if (parent != null) {
                                            String parentAuthorId = parent.getAuthorId();
                                            if (parentAuthorId != null && !parentAuthorId.equals(actorId) && !recipients.contains(parentAuthorId)) {
                                                createNotificationForReplyToReply(parentAuthorId, thread, reply);
                                            }
                                        }
                                        callback.onSuccess(replyId);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        callback.onSuccess(replyId);
                                    }
                                });
                            } else {
                                callback.onSuccess(replyId);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onSuccess(replyId);
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        } else {
            callback.onFailure("Failed to generate reply ID");
        }
    }

    public LiveData<List<CommunityReply>> getThreadReplies(String threadId) {
        MutableLiveData<List<CommunityReply>> repliesLiveData = new MutableLiveData<>();
        
        repliesRef.orderByChild("threadId").equalTo(threadId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<CommunityReply> replies = new ArrayList<>();
                    for (DataSnapshot replySnapshot : snapshot.getChildren()) {
                        CommunityReply reply = replySnapshot.getValue(CommunityReply.class);
                        if (reply != null) {
                            reply.setReplyId(replySnapshot.getKey());
                            replies.add(reply);
                        }
                    }
                    
                    // Sort by oldest first (chronological order)
                    Collections.sort(replies, Comparator.comparing(CommunityReply::getTimestamp));
                    repliesLiveData.setValue(replies);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    repliesLiveData.setValue(new ArrayList<>());
                }
            });

        return repliesLiveData;
    }

    // Star operations
    public interface StarCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void toggleThreadStar(String threadId, String userId, StarCallback callback) {
        threadsRef.child(threadId).child("stars").child(userId).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    boolean isCurrentlyStarred = task.getResult().exists();
                    
                    Map<String, Object> updates = new HashMap<>();
                    if (isCurrentlyStarred) {
                        // Remove star
                        updates.put("/community_threads/" + threadId + "/stars/" + userId, null);
                    } else {
                        // Add star
                        updates.put("/community_threads/" + threadId + "/stars/" + userId, true);
                    }
                    
                    database.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            updateThreadStarCount(threadId);
                            if (!isCurrentlyStarred) {
                                // Create a notification for the thread author (if starring someone else's thread)
                                threadsRef.child(threadId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        CommunityThread thread = snapshot.getValue(CommunityThread.class);
                                        if (thread != null && thread.getAuthorId() != null && !thread.getAuthorId().equals(userId)) {
                                            createNotificationForStar(thread.getAuthorId(), thread, userId);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        // no-op
                                    }
                                });
                            }
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                } else {
                    callback.onFailure("Failed to check current star status");
                }
            });
    }

    // Notifications
    private void createNotificationForReply(String recipientUserId, CommunityThread thread, CommunityReply reply) {
        String notifId = notificationsRef.child(recipientUserId).push().getKey();
        if (notifId == null) return;

        // Load actor display info
        usersRef.child(reply.getAuthorId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String actorName = snapshot.child("name").getValue(String.class);
                String actorRole = snapshot.child("role").getValue(String.class);
                com.example.csci3130group1.models.CommunityNotification n =
                        new com.example.csci3130group1.models.CommunityNotification(
                                com.example.csci3130group1.models.CommunityNotification.Type.REPLY,
                                recipientUserId,
                                reply.getAuthorId(),
                                actorName != null ? actorName : "Someone",
                                actorRole != null ? actorRole : "Student",
                                thread.getThreadId(),
                                thread.getTitle(),
                                reply.getReplyId()
                        );
                notificationsRef.child(recipientUserId).child(notifId).setValue(n)
                        .addOnFailureListener(e -> android.util.Log.e("CommunityRepo", "Failed to write reply notification: " + e.getMessage()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void createNotificationForStar(String recipientUserId, CommunityThread thread, String actorUserId) {
        String notifId = notificationsRef.child(recipientUserId).push().getKey();
        if (notifId == null) return;

        usersRef.child(actorUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String actorName = snapshot.child("name").getValue(String.class);
                String actorRole = snapshot.child("role").getValue(String.class);
                com.example.csci3130group1.models.CommunityNotification n =
                        new com.example.csci3130group1.models.CommunityNotification(
                                com.example.csci3130group1.models.CommunityNotification.Type.STAR,
                                recipientUserId,
                                actorUserId,
                                actorName != null ? actorName : "Someone",
                                actorRole != null ? actorRole : "Student",
                                thread.getThreadId(),
                                thread.getTitle(),
                                null
                        );
                notificationsRef.child(recipientUserId).child(notifId).setValue(n)
                        .addOnFailureListener(e -> android.util.Log.e("CommunityRepo", "Failed to write star notification: " + e.getMessage()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void createNotificationForReplyToReply(String recipientUserId, CommunityThread thread, CommunityReply reply) {
        String notifId = notificationsRef.child(recipientUserId).push().getKey();
        if (notifId == null) return;

        usersRef.child(reply.getAuthorId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String actorName = snapshot.child("name").getValue(String.class);
                String actorRole = snapshot.child("role").getValue(String.class);
                com.example.csci3130group1.models.CommunityNotification n =
                        new com.example.csci3130group1.models.CommunityNotification(
                                com.example.csci3130group1.models.CommunityNotification.Type.REPLY_TO_REPLY,
                                recipientUserId,
                                reply.getAuthorId(),
                                actorName != null ? actorName : "Someone",
                                actorRole != null ? actorRole : "Student",
                                thread.getThreadId(),
                                thread.getTitle(),
                                reply.getReplyId()
                        );
                notificationsRef.child(recipientUserId).child(notifId).setValue(n)
                        .addOnFailureListener(e -> android.util.Log.e("CommunityRepo", "Failed to write reply-to-reply notification: " + e.getMessage()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void createNotificationForReplyStar(String recipientUserId, String threadId, String threadTitle, String replyId, String actorUserId) {
        String notifId = notificationsRef.child(recipientUserId).push().getKey();
        if (notifId == null) return;

        usersRef.child(actorUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String actorName = snapshot.child("name").getValue(String.class);
                String actorRole = snapshot.child("role").getValue(String.class);
                com.example.csci3130group1.models.CommunityNotification n =
                        new com.example.csci3130group1.models.CommunityNotification(
                                com.example.csci3130group1.models.CommunityNotification.Type.REPLY_STAR,
                                recipientUserId,
                                actorUserId,
                                actorName != null ? actorName : "Someone",
                                actorRole != null ? actorRole : "Student",
                                threadId,
                                threadTitle != null ? threadTitle : "",
                                replyId
                        );
                notificationsRef.child(recipientUserId).child(notifId).setValue(n)
                        .addOnFailureListener(e -> android.util.Log.e("CommunityRepo", "Failed to write reply-star notification: " + e.getMessage()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    public LiveData<java.util.List<com.example.csci3130group1.models.CommunityNotification>> getNotifications(String userId) {
        MutableLiveData<java.util.List<com.example.csci3130group1.models.CommunityNotification>> live = new MutableLiveData<>();
        notificationsRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                java.util.List<com.example.csci3130group1.models.CommunityNotification> items = new java.util.ArrayList<>();
                for (DataSnapshot nSnap : snapshot.getChildren()) {
                    com.example.csci3130group1.models.CommunityNotification n = nSnap.getValue(com.example.csci3130group1.models.CommunityNotification.class);
                    if (n != null) {
                        n.setNotificationId(nSnap.getKey());
                        items.add(n);
                    }
                }
                java.util.Collections.sort(items, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                live.setValue(items);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                live.setValue(new java.util.ArrayList<>());
            }
        });
        return live;
    }

    public void markAllNotificationsRead(String userId) {
        notificationsRef.child(userId).get().addOnSuccessListener(snap -> {
            Map<String, Object> updates = new HashMap<>();
            for (DataSnapshot child : snap.getChildren()) {
                updates.put("/community_notifications/" + userId + "/" + child.getKey() + "/read", true);
            }
            if (!updates.isEmpty()) {
                database.updateChildren(updates)
                        .addOnFailureListener(e -> android.util.Log.e("CommunityRepo", "Failed to mark all notifications read: " + e.getMessage()));
            }
        });
    }

    public void markNotificationRead(String userId, String notificationId) {
        if (userId == null || notificationId == null) return;
        notificationsRef.child(userId).child(notificationId).child("read")
                .setValue(true)
                .addOnFailureListener(e -> android.util.Log.e("CommunityRepo", "Failed to mark notification read: " + e.getMessage()));
    }

    // User replies
    public LiveData<List<CommunityReply>> getUserReplies(String userId) {
        MutableLiveData<List<CommunityReply>> live = new MutableLiveData<>();
        repliesRef.orderByChild("authorId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<CommunityReply> replies = new ArrayList<>();
                        for (DataSnapshot replySnap : snapshot.getChildren()) {
                            CommunityReply r = replySnap.getValue(CommunityReply.class);
                            if (r != null) {
                                r.setReplyId(replySnap.getKey());
                                replies.add(r);
                            }
                        }
                        Collections.sort(replies, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        live.setValue(replies);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        live.setValue(new ArrayList<>());
                    }
                });
        return live;
    }

    public void toggleReplyStar(String replyId, String userId, StarCallback callback) {
        repliesRef.child(replyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                CommunityReply reply = snapshot.getValue(CommunityReply.class);
                if (reply == null) { callback.onFailure("Reply not found"); return; }
                if (reply.isDeleted()) { callback.onFailure("Cannot star a deleted reply"); return; }

                boolean isCurrentlyStarred = snapshot.child("stars").child(userId).exists();
                Map<String, Object> updates = new HashMap<>();
                if (isCurrentlyStarred) {
                    updates.put("/community_replies/" + replyId + "/stars/" + userId, null);
                } else {
                    updates.put("/community_replies/" + replyId + "/stars/" + userId, true);
                }

                database.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            updateReplyStarCount(replyId);
                            if (!isCurrentlyStarred) {
                                String threadId = reply.getThreadId();
                                threadsRef.child(threadId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot threadSnap) {
                                        CommunityThread thread = threadSnap.getValue(CommunityThread.class);
                                        String title = thread != null ? thread.getTitle() : null;
                                        if (reply.getAuthorId() != null && !reply.getAuthorId().equals(userId)) {
                                            createNotificationForReplyStar(reply.getAuthorId(), threadId, title, replyId, userId);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) { }
                                });
                            }
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure("Failed to load reply");
            }
        });
    }

    // Delete operations
    public interface DeleteCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void deleteReply(String replyId, String threadId, DeleteCallback callback) {
        // Load reply to get replyAuthorId
        repliesRef.child(replyId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot replySnap) {
                CommunityReply reply = replySnap.getValue(CommunityReply.class);
                String replyAuthorId = reply != null ? reply.getAuthorId() : null;

                // Load thread to get threadAuthorId
                threadsRef.child(threadId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot threadSnap) {
                        CommunityThread thread = threadSnap.getValue(CommunityThread.class);
                        String threadAuthorId = thread != null ? thread.getAuthorId() : null;

                        // Soft-delete: keep reply under thread, mark as deleted, clear stars
                        Map<String, Object> updates = new HashMap<>();
                        long now = System.currentTimeMillis();
                        updates.put("/community_replies/" + replyId + "/deleted", true);
                        updates.put("/community_replies/" + replyId + "/deletedAt", now);
                        updates.put("/community_replies/" + replyId + "/stars", null);
                        updates.put("/community_replies/" + replyId + "/starCount", 0);

                        database.updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // Best-effort cleanup of related notifications
                                    cleanupNotificationsForReply(replyId, replyAuthorId, threadAuthorId, () -> {
                                        callback.onSuccess();
                                    });
                                })
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }

    private void cleanupNotificationsForReply(String replyId, String replyAuthorId, String threadAuthorId, Runnable onDone) {
        // Determine which users may have notifications for this reply
        java.util.List<String> targets = new java.util.ArrayList<>();
        if (replyAuthorId != null) targets.add(replyAuthorId); // REPLY_STAR lives under reply author's notifications
        if (threadAuthorId != null) targets.add(threadAuthorId); // REPLY lives under thread author's notifications

        if (targets.isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }

        Map<String, Object> toDelete = new HashMap<>();
        final int total = targets.size();
        final int[] done = {0};

        for (String uid : targets) {
            notificationsRef.child(uid).orderByChild("replyId").equalTo(replyId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot n : snapshot.getChildren()) {
                                String nid = n.getKey();
                                if (nid != null) {
                                    toDelete.put("/community_notifications/" + uid + "/" + nid, null);
                                }
                            }
                            if (++done[0] == total) {
                                if (toDelete.isEmpty()) {
                                    if (onDone != null) onDone.run();
                                } else {
                                    database.updateChildren(toDelete)
                                            .addOnSuccessListener(v -> { if (onDone != null) onDone.run(); })
                                            .addOnFailureListener(e -> {
                                                android.util.Log.e("CommunityRepo", "Failed to cleanup reply notifications: " + e.getMessage());
                                                if (onDone != null) onDone.run();
                                            });
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (++done[0] == total) {
                                if (toDelete.isEmpty()) {
                                    if (onDone != null) onDone.run();
                                } else {
                                    database.updateChildren(toDelete)
                                            .addOnSuccessListener(v -> { if (onDone != null) onDone.run(); })
                                            .addOnFailureListener(e -> {
                                                android.util.Log.e("CommunityRepo", "Failed to cleanup reply notifications: " + e.getMessage());
                                                if (onDone != null) onDone.run();
                                            });
                                }
                            }
                        }
                    });
        }
    }

    public void deleteThread(String threadId, DeleteCallback callback) {
        // First delete all replies for this thread, then delete the thread
        repliesRef.orderByChild("threadId").equalTo(threadId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Object> updates = new HashMap<>();
                        for (DataSnapshot replySnap : snapshot.getChildren()) {
                            String rid = replySnap.getKey();
                            if (rid != null) {
                                updates.put("/community_replies/" + rid, null);
                            }
                        }
                        // Remove thread node (removes all children under it in one go)
                        updates.put("/community_threads/" + threadId, null);

                        database.updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // Best-effort cleanup of any notifications referencing this thread
                                    cleanupNotificationsForThread(threadId, () -> {
                                        callback.onSuccess();
                                    });
                                })
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onFailure(error.getMessage());
                    }
                });
    }

    // Best-effort client-side cleanup of notifications referencing a deleted thread.
    // Note: In secured environments, this is better handled by Cloud Functions.
    private void cleanupNotificationsForThread(String threadId, Runnable onDone) {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnap) {
                List<String> userIds = new ArrayList<>();
                for (DataSnapshot u : usersSnap.getChildren()) {
                    String uid = u.getKey();
                    if (uid != null) userIds.add(uid);
                }

                if (userIds.isEmpty()) {
                    if (onDone != null) onDone.run();
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                final int total = userIds.size();
                final int[] done = {0};

                for (String uid : userIds) {
                    notificationsRef.child(uid).orderByChild("threadId").equalTo(threadId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot notifSnap) {
                                    for (DataSnapshot n : notifSnap.getChildren()) {
                                        String nid = n.getKey();
                                        if (nid != null) {
                                            updates.put("/community_notifications/" + uid + "/" + nid, null);
                                        }
                                    }
                                    if (++done[0] == total) {
                                        if (updates.isEmpty()) {
                                            if (onDone != null) onDone.run();
                                        } else {
                                            database.updateChildren(updates)
                                                    .addOnSuccessListener(v -> { if (onDone != null) onDone.run(); })
                                                    .addOnFailureListener(e -> {
                                                        android.util.Log.e("CommunityRepo", "Failed to cleanup notifications: " + e.getMessage());
                                                        if (onDone != null) onDone.run();
                                                    });
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    if (++done[0] == total) {
                                        if (updates.isEmpty()) {
                                            if (onDone != null) onDone.run();
                                        } else {
                                            database.updateChildren(updates)
                                                    .addOnSuccessListener(v -> { if (onDone != null) onDone.run(); })
                                                    .addOnFailureListener(e -> {
                                                        android.util.Log.e("CommunityRepo", "Failed to cleanup notifications: " + e.getMessage());
                                                        if (onDone != null) onDone.run();
                                                    });
                                        }
                                    }
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("CommunityRepo", "Users list load failed for cleanup: " + error.getMessage());
                if (onDone != null) onDone.run();
            }
        });
    }

    // Helper methods
    private void sortThreads(List<CommunityThread> threads, String sortBy) {
        switch (sortBy) {
            case "Stars":
                Collections.sort(threads, (t1, t2) -> Integer.compare(t2.getStarCount(), t1.getStarCount()));
                break;
            case "Replies":
                Collections.sort(threads, (t1, t2) -> Integer.compare(t2.getReplyCount(), t1.getReplyCount()));
                break;
            case "Newest":
            default:
                Collections.sort(threads, (t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));
                break;
        }
    }

    private long getTimeThreshold(String timeFilter) {
        long now = System.currentTimeMillis();
        switch (timeFilter) {
            case "Today":
                return now - (24 * 60 * 60 * 1000); // 1 day
            case "This Week":
                return now - (7 * 24 * 60 * 60 * 1000); // 7 days
            case "This Month":
                return now - (30L * 24 * 60 * 60 * 1000); // 30 days
            default:
                return 0;
        }
    }

    private void updateThreadStarCount(String threadId) {
        threadsRef.child(threadId).child("stars").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int starCount = (int) snapshot.getChildrenCount();
                threadsRef.child(threadId).child("starCount").setValue(starCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error silently
            }
        });
    }

    private void updateReplyStarCount(String replyId) {
        repliesRef.child(replyId).child("stars").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int starCount = (int) snapshot.getChildrenCount();
                repliesRef.child(replyId).child("starCount").setValue(starCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error silently
            }
        });
    }

    private void updateThreadReplyCount(String threadId) {
        threadsRef.child(threadId).child("replies").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int replyCount = (int) snapshot.getChildrenCount();
                threadsRef.child(threadId).child("replyCount").setValue(replyCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error silently
            }
        });
    }

    // Get user role for current user
    public interface UserRoleCallback {
        void onSuccess(String role, String name);
        void onFailure(String error);
    }

    public void getCurrentUserRole(UserRoleCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                       FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (userId == null) {
            callback.onFailure("User not logged in");
            return;
        }

        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    String name = snapshot.child("name").getValue(String.class);
                    if (role == null || role.isEmpty()) {
                        role = "Student"; // Default role
                    }
                    callback.onSuccess(role, name != null ? name : "Unknown User");
                } else {
                    callback.onFailure("User data not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.getMessage());
            }
        });
    }
}
