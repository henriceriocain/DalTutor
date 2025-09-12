package com.example.csci3130group1.models;

import java.util.HashMap;
import java.util.Map;

public class CommunityReply {
    private String replyId;
    private String threadId;
    private String parentReplyId; // null for top-level
    private String content;
    private String authorId;
    private String authorName;
    private String authorRole; // "Student" or "Tutor"
    private long timestamp;
    private boolean deleted;
    private long deletedAt;
    private int depth; // 0 for top-level, increases by 1 per nesting
    private int starCount;
    private Map<String, Boolean> stars; // userId -> true (for starred replies)
    private boolean edited;
    private long editedAt;

    // No-argument constructor for Firebase
    public CommunityReply() {
        this.stars = new HashMap<>();
        this.starCount = 0;
        this.deleted = false;
        this.deletedAt = 0L;
        this.depth = 0;
        this.edited = false;
        this.editedAt = 0L;
    }

    // Constructor
    public CommunityReply(String threadId, String content, String authorId, String authorName,
                          String authorRole) {
        this.threadId = threadId;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.timestamp = System.currentTimeMillis();
        this.stars = new HashMap<>();
        this.starCount = 0;
        this.parentReplyId = null;
        this.depth = 0;
        this.deleted = false;
        this.deletedAt = 0L;
        this.edited = false;
        this.editedAt = 0L;
    }

    // Constructor for nested reply
    public CommunityReply(String threadId, String content, String authorId, String authorName,
                          String authorRole, String parentReplyId, int depth) {
        this.threadId = threadId;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.timestamp = System.currentTimeMillis();
        this.stars = new HashMap<>();
        this.starCount = 0;
        this.parentReplyId = parentReplyId;
        this.depth = Math.max(0, depth);
        this.deleted = false;
        this.deletedAt = 0L;
        this.edited = false;
        this.editedAt = 0L;
    }

    // Getters
    public String getReplyId() {
        return replyId;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getParentReplyId() {
        return parentReplyId;
    }

    public String getContent() {
        return content;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorRole() {
        return authorRole;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getDepth() {
        return depth;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public long getDeletedAt() {
        return deletedAt;
    }

    public int getStarCount() {
        return starCount;
    }

    public Map<String, Boolean> getStars() {
        return stars != null ? stars : new HashMap<>();
    }
    public boolean isEdited() { return edited; }
    public long getEditedAt() { return editedAt; }

    // Setters
    public void setReplyId(String replyId) {
        this.replyId = replyId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public void setParentReplyId(String parentReplyId) {
        this.parentReplyId = parentReplyId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public void setAuthorRole(String authorRole) {
        this.authorRole = authorRole;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setStarCount(int starCount) {
        this.starCount = starCount;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setDeletedAt(long deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setStars(Map<String, Boolean> stars) {
        this.stars = stars;
    }
    public void setEdited(boolean edited) { this.edited = edited; }
    public void setEditedAt(long editedAt) { this.editedAt = editedAt; }

    // Helper methods
    public boolean isStarredByUser(String userId) {
        return stars != null && stars.containsKey(userId) && Boolean.TRUE.equals(stars.get(userId));
    }

    public void addStar(String userId) {
        if (stars == null) {
            stars = new HashMap<>();
        }
        stars.put(userId, true);
        starCount = stars.size();
    }

    public void removeStar(String userId) {
        if (stars != null) {
            stars.remove(userId);
            starCount = stars.size();
        }
    }

    public String getFormattedTimestamp() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }

    public String getTimeAgo() {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        
        if (months > 0) return months + (months == 1 ? " month ago" : " months ago");
        if (weeks > 0) return weeks + (weeks == 1 ? " week ago" : " weeks ago");
        if (days > 0) return days + (days == 1 ? " day ago" : " days ago");
        if (hours > 0) return hours + (hours == 1 ? " hour ago" : " hours ago");
        if (minutes > 0) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        return "Just now";
    }
}
