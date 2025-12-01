package com.example.myapplication.notifications;

import android.util.Log;

import com.example.myapplication.UserManager;
import com.google.firebase.database.DatabaseReference;

public class NotificationManager {
    private static final String TAG = "NotificationManager";

    public static void createNotification(String parentId, NotificationItem notification) {
        if (parentId == null || notification == null) {
            Log.e(TAG, "Cannot create notification: parentId or notification is null");
            return;
        }

        String notificationId = String.valueOf(System.currentTimeMillis());
        notification.setNotificationId(notificationId);

        DatabaseReference notificationRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("notifications")
                .child(notificationId);

        notificationRef.setValue(notification).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Notification created: " + notificationId);
            } else {
                Log.e(TAG, "Failed to create notification", task.getException());
            }
        });
    }

    public static void markAsRead(String parentId, String notificationId) {
        if (parentId == null || notificationId == null) {
            return;
        }

        DatabaseReference notificationRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("notifications")
                .child(notificationId)
                .child("read");

        notificationRef.setValue(true);
    }

    public static void deleteNotification(String parentId, String notificationId) {
        if (parentId == null || notificationId == null) {
            return;
        }

        DatabaseReference notificationRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("notifications")
                .child(notificationId);

        notificationRef.removeValue();
    }
}

