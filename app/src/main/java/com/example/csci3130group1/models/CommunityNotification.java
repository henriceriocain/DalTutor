package com.example.csci3130group1.models;

public class CommunityNotification {
    public enum Type { REPLY, STAR }

    private String notificationId;
    private String recipientUserId;
    private String actorUserId;
    private String actorName;
    private String actorRole;
    private String threadId;
    private String threadTitle;
    private String replyId; // optional
    private long timestamp;
    private boolean read;
    private String type; // "REPLY" | "STAR" for Firebase simplicity

    public CommunityNotification() {}

    public CommunityNotification(Type type,
                                 String recipientUserId,
                                 String actorUserId,
                                 String actorName,
                                 String actorRole,
                                 String threadId,
                                 String threadTitle,
                                 String replyId) {
        this.type = type.name();
        this.recipientUserId = recipientUserId;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.actorRole = actorRole;
        this.threadId = threadId;
        this.threadTitle = threadTitle;
        this.replyId = replyId;
        this.timestamp = System.currentTimeMillis();
        this.read = false;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getRecipientUserId() { return recipientUserId; }
    public String getActorUserId() { return actorUserId; }
    public String getActorName() { return actorName; }
    public String getActorRole() { return actorRole; }
    public String getThreadId() { return threadId; }
    public String getThreadTitle() { return threadTitle; }
    public String getReplyId() { return replyId; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }
    public String getType() { return type; }

    public void setRead(boolean read) { this.read = read; }
}

