package com.example.csci3130group1.models;

import java.util.HashMap;
import java.util.Map;

public class CommunityReply {
    private String replyId;
    private String threadId;
    private String content;
    private String authorId;
    private String authorName;
    private String authorRole; // "Student" or "Tutor"
    private long timestamp;
    private int starCount;
    private Map<String, Boolean> stars; // userId -> true (for starred replies)

    // No-argument constructor for Firebase
    public CommunityReply() {
        this.stars = new HashMap<>();
        this.starCount = 0;
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
    }

    // Getters
    public String getReplyId() {
        return replyId;
    }

    public String getThreadId() {
        return threadId;
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

    public int getStarCount() {
        return starCount;
    }

    public Map<String, Boolean> getStars() {
        return stars != null ? stars : new HashMap<>();
    }

    // Setters
    public void setReplyId(String replyId) {
        this.replyId = replyId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
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

    public void setStarCount(int starCount) {
        this.starCount = starCount;
    }

    public void setStars(Map<String, Boolean> stars) {
        this.stars = stars;
    }

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