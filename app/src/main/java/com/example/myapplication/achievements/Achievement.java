package com.example.myapplication;

import android.util.Log;

import com.example.myapplication.ControllerLog;
import com.example.myapplication.ControllerLogModel;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.UserManager;
import com.example.myapplication.medication.ControllerSchedule;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.utils.FirebaseKeyEncoder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Achievement {
    String username;
    long timeOfLastDose;
    long timeOfLastTechnique;
    int currentStreak;
    int videoswatched;

    List<Long> rescueTimes;
    List<Boolean> badges;
    List<Integer> badgeRequirements;

    final long DAYINMS = 86400000;

    public Achievement() {
        this.badges = new ArrayList<>();
        badges.add(false);
        badges.add(false);
        badges.add(false);

        this.badgeRequirements = new ArrayList<>();
        badgeRequirements.add(7);
        badgeRequirements.add(10);
        badgeRequirements.add(1);
        badgeRequirements.add(4);
        badgeRequirements.add(30);

        this.rescueTimes = new ArrayList<>();
        rescueTimes.add(0L);
        rescueTimes.add(0L);
        rescueTimes.add(0L);
        rescueTimes.add(0L);
    }

    public Achievement(String username) {
        this();
        this.username = username;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public long getTimeOfLastDose() { return timeOfLastDose; }
    public void setTimeOfLastDose(long t) { this.timeOfLastDose = t; }

    public long getTimeOfLastTechnique(){ return timeOfLastTechnique; }
    public void setTimeOfLastTechnique(long t){ this.timeOfLastTechnique = t; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int s) { this.currentStreak = s; }

    public int getVideoswatched() { return videoswatched; }
    public void setVideoswatched(int v) { this.videoswatched = v; }

    public List<Long> getRescueTimes() { return rescueTimes; }
    public void setRescueTimes(List<Long> r) { this.rescueTimes = r; }

    public long getRescueTimeAt(int index) { return rescueTimes.get(index); }
    public void setRescueTimeAt(int index, long value) { rescueTimes.set(index, value); }

    public List<Boolean> getBadges() { return badges; }
    public void setBadges(List<Boolean> b) { this.badges = b; }
    public boolean getBadgeAt(int index) { return badges.get(index); }
    public void setBadgeAt(int index, boolean value) { badges.set(index, value); }

    public List<Integer> getBadgeRequirements() { return badgeRequirements; }
    public void setBadgeRequirements(List<Integer> req) { this.badgeRequirements = req; }

    public void changeDayRequirement(int a){
        badgeRequirements.set(2, a);

        // Resize rescueTimes list
        while (rescueTimes.size() < a)
            rescueTimes.add(0L);
        while (rescueTimes.size() > a)
            rescueTimes.remove(rescueTimes.size() - 1);
    }

    /**
     * Updates streak based on ControllerSchedule dates and controller logs.
     * Streak is calculated as consecutive scheduled dates (from ControllerSchedule) 
     * where controller medication was logged, not consecutive calendar days.
     */
    public void updateStreak() {
        // Update timeOfLastDose for backward compatibility
        timeOfLastDose = System.currentTimeMillis();
        
        // Calculate streak from ControllerSchedule (async)
        calculateStreakFromSchedule(new ResultCallBack<Integer>() {
            @Override
            public void onComplete(Integer streak) {
                if (streak != null) {
                    currentStreak = streak;
                } else {
                    // Fallback: if calculation fails, keep current streak or set to 1
                    if (currentStreak == 0) {
                        currentStreak = 1;
                    }
                }
            }
        });
    }
    
    /**
     * Calculates streak from ControllerSchedule dates and controller logs.
     * Streak = consecutive scheduled dates (from ControllerSchedule) where controller was logged.
     * 
     * @param callback Callback to receive the calculated streak count
     */
    public void calculateStreakFromSchedule(ResultCallBack<Integer> callback) {
        if (username == null) {
            Log.w("Achievement", "Username is null, cannot calculate streak");
            if (callback != null) {
                callback.onComplete(0);
            }
            return;
        }
        
        // Get parentId and childId
        String parentId = null;
        String childId = username;
        
        // Try to get parentId from UserManager.currentUser if it's a ChildAccount
        if (UserManager.currentUser instanceof ChildAccount) {
            ChildAccount childAccount = (ChildAccount) UserManager.currentUser;
            parentId = childAccount.getParent_id();
            childId = childAccount.getID();
        } else {
            // If currentUser is not a ChildAccount, we need to load it
            // For now, we'll try to find the child in the database
            // This is a fallback - ideally username should match the current user
            Log.w("Achievement", "CurrentUser is not ChildAccount, attempting to load child account");
            loadChildAccountAndCalculateStreak(username, callback);
            return;
        }
        
        if (parentId == null) {
            Log.w("Achievement", "ParentId is null, cannot calculate streak");
            if (callback != null) {
                callback.onComplete(0);
            }
            return;
        }
        
        // Load ChildAccount to get ControllerSchedule
        loadChildAccountAndCalculateStreak(parentId, childId, callback);
    }
    
    /**
     * Helper method to load ChildAccount and calculate streak.
     */
    private void loadChildAccountAndCalculateStreak(String parentId, String childId, ResultCallBack<Integer> callback) {
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        UserManager.mDatabase.child("users").child(parentId).child("children").child(encodedChildId)
            .get().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) {
                    Log.e("Achievement", "Failed to load ChildAccount for streak calculation", task.getException());
                    if (callback != null) {
                        callback.onComplete(0);
                    }
                    return;
                }
                
                ChildAccount childAccount = task.getResult().getValue(ChildAccount.class);
                if (childAccount == null) {
                    Log.e("Achievement", "ChildAccount is null");
                    if (callback != null) {
                        callback.onComplete(0);
                    }
                    return;
                }
                
                ControllerSchedule schedule = childAccount.getControllerSchedule();
                if (schedule == null || schedule.isEmpty()) {
                    Log.d("Achievement", "No ControllerSchedule found, streak is 0");
                    if (callback != null) {
                        callback.onComplete(0);
                    }
                    return;
                }
                
                // Get all scheduled dates
                List<String> scheduledDates = schedule.getDates();
                if (scheduledDates == null || scheduledDates.isEmpty()) {
                    if (callback != null) {
                        callback.onComplete(0);
                    }
                    return;
                }
                
                // Sort scheduled dates
                Collections.sort(scheduledDates);
                
                // Get all controller logs
                ControllerLogModel.readFromDB(childId, new ResultCallBack<HashMap<String, ControllerLog>>() {
                    @Override
                    public void onComplete(HashMap<String, ControllerLog> logs) {
                        // Extract days with logs (date part before underscore)
                        Set<String> daysWithLogs = new HashSet<>();
                        if (logs != null) {
                            for (String dateKey : logs.keySet()) {
                                // Extract date part (before underscore)
                                String dayKey = dateKey.split("_")[0];
                                daysWithLogs.add(dayKey);
                            }
                        }
                        
                        // Calculate consecutive scheduled days with logs (going backwards from today)
                        int streak = calculateConsecutiveStreak(scheduledDates, daysWithLogs);
                        
                        Log.d("Achievement", String.format("Calculated streak for %s: %d consecutive scheduled days", childId, streak));
                        
                        if (callback != null) {
                            callback.onComplete(streak);
                        }
                    }
                });
            });
    }
    
    /**
     * Fallback method when we only have username (not parentId/childId).
     */
    private void loadChildAccountAndCalculateStreak(String username, ResultCallBack<Integer> callback) {
        if (callback != null) {
            callback.onComplete(0);
        }
    }
    
    /**
     * Calculates consecutive streak of scheduled dates that have controller logs.
     * Goes backwards from the most recent scheduled date (up to today), 
     * counting consecutive scheduled dates with logs.
     * 
     * Streak is based on scheduled dates (from ControllerSchedule), not calendar days.
     * Example: If schedule has dates [2025-01-15, 2025-01-17, 2025-01-19] and 
     * all have logs, streak = 3 (even though there are gaps in calendar days).
     * 
     * @param scheduledDates List of scheduled dates in "yyyy-MM-dd" format (should be sorted)
     * @param daysWithLogs Set of dates (in "yyyy-MM-dd" format) that have controller logs
     * @return Number of consecutive scheduled days with logs (going backwards from most recent)
     */
    private int calculateConsecutiveStreak(List<String> scheduledDates, Set<String> daysWithLogs) {
        if (scheduledDates == null || scheduledDates.isEmpty()) {
            return 0;
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());

        // Filter scheduled dates to only include past dates (up to today)
        List<String> pastScheduledDates = new ArrayList<>();
        for (String scheduledDate : scheduledDates) {
            if (scheduledDate != null && scheduledDate.compareTo(today) <= 0) {
                pastScheduledDates.add(scheduledDate);
            }
        }
        
        if (pastScheduledDates.isEmpty()) {
            return 0;
        }
        
        // Go backwards from the most recent scheduled date, counting consecutive scheduled dates with logs
        int streak = 0;
        // Start from the most recent scheduled date and work backwards
        // Since scheduledDates is sorted ascending, reverse to get newest first
        Collections.reverse(pastScheduledDates);
        
        for (String scheduledDate : pastScheduledDates) {
            if (daysWithLogs.contains(scheduledDate)) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    public void updateStreakTechnique() {
        long now = System.currentTimeMillis();
        if (timeOfLastTechnique == 0) {
            videoswatched = 1;
        } else {
            long days = (now - timeOfLastTechnique) / DAYINMS;
            int gap = badgeRequirements.get(2);

            if (days - gap == 1)
                videoswatched++;
            else if (days - gap > 1)
                videoswatched = 1;
        }
        timeOfLastTechnique = now;
    }

    public void usedRescue(){
        long now = System.currentTimeMillis();
        boolean empty = false;

        for (int i = 0; i < rescueTimes.size(); i++) {
            if (rescueTimes.get(i) == 0) {
                rescueTimes.set(i, now);
                empty = true;
                break;
            }
        }

        if (!empty) {
            for (int i = 0; i < rescueTimes.size() - 1; i++)
                rescueTimes.set(i, rescueTimes.get(i + 1));

            rescueTimes.set(rescueTimes.size() - 1, now);
        }
    }

    /**
     * Checks if badge 1 requirement is met.
     * Badge 1 requires consecutive scheduled controller days (from ControllerSchedule) 
     * where controller was logged to exceed the requirement.
     * 
     * @return true if consecutive scheduled days with logs >= requirement
     */
    public boolean checkBadge1() {
        return currentStreak >= badgeRequirements.get(0);
    }

    public boolean checkBadge2() {
        return videoswatched >= badgeRequirements.get(1);
    }

    public boolean checkBadge3() {
        if (rescueTimes.get(0) == 0) return false;
        long now = System.currentTimeMillis();
        return now - rescueTimes.get(0) >= (DAYINMS * badgeRequirements.get(3));
    }
}