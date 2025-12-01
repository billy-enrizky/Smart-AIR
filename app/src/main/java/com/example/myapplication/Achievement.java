package com.example.myapplication;

import java.util.ArrayList;
import java.util.List;

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

    public void updateStreak() {
        long now = System.currentTimeMillis();
        if (timeOfLastDose == 0) {
            currentStreak = 1;
        } else {
            long days = (now - timeOfLastDose) / DAYINMS;
            if (days == 1)
                currentStreak++;
            else if (days > 1)
                currentStreak = 1;
        }
        timeOfLastDose = now;
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
