package com.example.myapplication.notifications;

public class NotificationItem {
    public enum NotificationType {
        RED_ZONE_DAY,
        RAPID_RESCUE,
        WORSE_AFTER_DOSE,
        TRIAGE_ESCALATION,
        INVENTORY_LOW,
        INVENTORY_EXPIRED
    }

    private String notificationId;
    private NotificationType type;
    private String childId;
    private String childName;
    private String message;
    private long timestamp;
    private boolean read;

    public NotificationItem() {
        this.read = false;
        this.timestamp = System.currentTimeMillis();
    }

    public NotificationItem(NotificationType type, String childId, String childName, String message) {
        this.type = type;
        this.childId = childId;
        this.childName = childName;
        this.message = message;
        this.read = false;
        this.timestamp = System.currentTimeMillis();
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public String getChildName() {
        return childName;
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}

