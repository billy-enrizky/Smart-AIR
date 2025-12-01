package com.example.myapplication.notifications;

import android.util.Log;

import com.example.myapplication.UserManager;
import com.example.myapplication.safety.PEFReading;
import com.example.myapplication.safety.Zone;
import com.example.myapplication.safety.ZoneCalculator;
import com.example.myapplication.Inhaler;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AlertDetector {
    private static final String TAG = "AlertDetector";

    public static void checkRedZoneDay(String parentId, String childId, String childName, int pefValue, Integer personalBest) {
        if (personalBest == null || personalBest <= 0) {
            return;
        }

        Zone zone = ZoneCalculator.calculateZone(pefValue, personalBest);
        if (zone == Zone.RED) {
            NotificationItem notification = new NotificationItem(
                    NotificationItem.NotificationType.RED_ZONE_DAY,
                    childId,
                    childName,
                    childName + " entered the Red Zone. PEF value: " + pefValue + " L/min"
            );
            NotificationManager.createNotification(parentId, notification);
        }
    }

    public static void checkRapidRescue(String parentId, String childId, String childName) {
        long threeHoursAgo = System.currentTimeMillis() - (3 * 60 * 60 * 1000L);

        DatabaseReference rescueRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("rescueUsage");

        Query query = rescueRef.orderByChild("timestamp").startAt(threeHoursAgo);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
                public void onDataChange(DataSnapshot snapshot) {
                    int count = 0;
                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            com.example.myapplication.safety.RescueUsage usage = child.getValue(com.example.myapplication.safety.RescueUsage.class);
                            if (usage != null) {
                                count += usage.getCount();
                            }
                        }
                    }

                    if (count >= 3) {
                        NotificationItem notification = new NotificationItem(
                                NotificationItem.NotificationType.RAPID_RESCUE,
                                childId,
                                childName,
                                childName + " used rescue medication " + count + " times in the last 3 hours. Consider seeking medical care."
                        );
                        NotificationManager.createNotification(parentId, notification);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Error checking rapid rescue", error.toException());
                }
            });
    }

    public static void checkWorseAfterDose(String parentId, String childId, String childName, int currentPEF, Integer personalBest) {
        if (personalBest == null || personalBest <= 0) {
            return;
        }

        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("pefReadings");

        Query query = pefRef.orderByChild("timestamp").limitToLast(2);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                PEFReading preMedReading = null;
                PEFReading postMedReading = null;

                if (snapshot.exists()) {
                    PEFReading[] readings = new PEFReading[2];
                    int index = 0;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        readings[index++] = child.getValue(PEFReading.class);
                    }

                    for (PEFReading reading : readings) {
                        if (reading != null) {
                            if (reading.isPreMed()) {
                                preMedReading = reading;
                            } else if (reading.isPostMed()) {
                                postMedReading = reading;
                            }
                        }
                    }
                }

                if (preMedReading != null && postMedReading != null) {
                    double prePercentage = ZoneCalculator.calculatePercentage(preMedReading.getValue(), personalBest);
                    double postPercentage = ZoneCalculator.calculatePercentage(postMedReading.getValue(), personalBest);

                    if (postPercentage < prePercentage) {
                        NotificationItem notification = new NotificationItem(
                                NotificationItem.NotificationType.WORSE_AFTER_DOSE,
                                childId,
                                childName,
                                childName + " PEF decreased after medication. Pre: " + String.format("%.1f", prePercentage) + "%, Post: " + String.format("%.1f", postPercentage) + "%"
                        );
                        NotificationManager.createNotification(parentId, notification);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error checking worse after dose", error.toException());
            }
        });
    }

    public static void checkTriageEscalation(String parentId, String childId, String childName) {
        NotificationItem notification = new NotificationItem(
                NotificationItem.NotificationType.TRIAGE_ESCALATION,
                childId,
                childName,
                childName + " - Emergency guidance was shown during triage. Please check on them immediately."
        );
        NotificationManager.createNotification(parentId, notification);
    }

    public static void checkInventoryStatus(String parentId, String childId, String childName) {
        DatabaseReference inhalerRef = UserManager.mDatabase
                .child("InhalerManager")
                .child(childId);

        inhalerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Inhaler inhaler = child.getValue(Inhaler.class);
                        if (inhaler != null) {
                            if (inhaler.checkExpiry()) {
                                NotificationItem notification = new NotificationItem(
                                        NotificationItem.NotificationType.INVENTORY_EXPIRED,
                                        childId,
                                        childName,
                                        childName + " has an inhaler that expires within 7 days."
                                );
                                NotificationManager.createNotification(parentId, notification);
                            }

                            if (inhaler.checkEmpty()) {
                                NotificationItem notification = new NotificationItem(
                                        NotificationItem.NotificationType.INVENTORY_LOW,
                                        childId,
                                        childName,
                                        childName + " has an inhaler that is running low (less than 25% remaining)."
                                );
                                NotificationManager.createNotification(parentId, notification);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error checking inventory", error.toException());
            }
        });
    }
}

