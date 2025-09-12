package com.example.csci3130group1.models;

import java.util.HashMap;
import java.util.Map;

public class CommunityThread {
    private String threadId;
    private String title;
    private String description;
    private String authorId;
    private String authorName;
    private String authorRole; // "Student" or "Tutor"
    private String category; // e.g., "Computer Science", "Mathematics", etc.
    private long timestamp;
    private int starCount;
    private int replyCount;
    private Map<String, Boolean> stars; // userId -> true (for starred threads)
    private Map<String, Boolean> replies; // replyId -> true (for thread replies)
    private boolean edited;
    private long editedAt;

    // No-argument constructor for Firebase
    public CommunityThread() {
        this.stars = new HashMap<>();
        this.replies = new HashMap<>();
        this.starCount = 0;
        this.replyCount = 0;
        this.edited = false;
        this.editedAt = 0L;
    }

    // Constructor
    public CommunityThread(String title, String description, String authorId, String authorName, 
                          String authorRole, String category) {
        this.title = title;
        this.description = description;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.category = category;
        this.timestamp = System.currentTimeMillis();
        this.stars = new HashMap<>();
        this.replies = new HashMap<>();
        this.starCount = 0;
        this.replyCount = 0;
        this.edited = false;
        this.editedAt = 0L;
    }

    // Getters
    public String getThreadId() {
        return threadId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
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

    public String getCategory() {
        return category;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getStarCount() {
        return starCount;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public Map<String, Boolean> getStars() {
        return stars != null ? stars : new HashMap<>();
    }

    public Map<String, Boolean> getReplies() {
        return replies != null ? replies : new HashMap<>();
    }

    public boolean isEdited() { return edited; }
    public long getEditedAt() { return editedAt; }

    // Setters
    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public void setCategory(String category) {
        this.category = category;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setStarCount(int starCount) {
        this.starCount = starCount;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }

    public void setStars(Map<String, Boolean> stars) {
        this.stars = stars;
    }

    public void setReplies(Map<String, Boolean> replies) {
        this.replies = replies;
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

    public void addReply(String replyId) {
        if (replies == null) {
            replies = new HashMap<>();
        }
        replies.put(replyId, true);
        replyCount = replies.size();
    }

    public void removeReply(String replyId) {
        if (replies != null) {
            replies.remove(replyId);
            replyCount = replies.size();
        }
    }

    public String getFormattedTimestamp() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("America/Halifax"));
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
