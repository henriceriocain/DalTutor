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

    private CommunityRepository() {
        database = FirebaseDatabase.getInstance().getReference();
        threadsRef = database.child("community_threads");
        repliesRef = database.child("community_replies");
        usersRef = database.child("users");
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
        MutableLiveData<List<CommunityThread>> threadsLiveData = new MutableLiveData<>();
        
        Query query = threadsRef;
        
        // Apply time filter
        if (timeFilter != null && !timeFilter.equals("All Time")) {
            long timeThreshold = getTimeThreshold(timeFilter);
            query = query.orderByChild("timestamp").startAt(timeThreshold);
        }

        query.addValueEventListener(new ValueEventListener() {
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
                threadsLiveData.setValue(threads);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                threadsLiveData.setValue(new ArrayList<>());
            }
        });

        return threadsLiveData;
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
                    callback.onSuccess(replyId);
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
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                } else {
                    callback.onFailure("Failed to check current star status");
                }
            });
    }

    public void toggleReplyStar(String replyId, String userId, StarCallback callback) {
        repliesRef.child(replyId).child("stars").child(userId).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    boolean isCurrentlyStarred = task.getResult().exists();
                    
                    Map<String, Object> updates = new HashMap<>();
                    if (isCurrentlyStarred) {
                        // Remove star
                        updates.put("/community_replies/" + replyId + "/stars/" + userId, null);
                    } else {
                        // Add star
                        updates.put("/community_replies/" + replyId + "/stars/" + userId, true);
                    }
                    
                    database.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            updateReplyStarCount(replyId);
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                } else {
                    callback.onFailure("Failed to check current star status");
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