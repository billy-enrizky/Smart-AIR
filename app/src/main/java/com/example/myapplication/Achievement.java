package com.example.myapplication;
import java.util.Arrays;
public class Achievement {
    String username;
    long timeOfLastDose;
    int currentStreak;
    int videoswatched;
    long[] rescueTimes;
    boolean[] badges;

    int[] badgeRequirements; //(Days amount streak, Times video, Less than or equal x, within y days)

    final long DAYINMS = 86400000;
    public Achievement() {
        this.badges = new boolean[]{false, false, false};
        this.currentStreak = 0;
        this.videoswatched = 0;
        this.timeOfLastDose = -1;
        this.badgeRequirements = new int[]{7,10,4,30};
        this.rescueTimes = new long[4];
    }

    public Achievement(String username) {
        this.username = username;
        this.badges = new boolean[]{false, false, false};
        this.rescueTimes = new long[]{-1, -1, -1, -1};
        this.currentStreak = 0;
        this.videoswatched = 0;
        this.timeOfLastDose = -1;
        this.badgeRequirements = new int[]{7,10,4,30};
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getTimeOfLastDose() {
        return timeOfLastDose;
    }

    public void setTimeOfLastDose(long timeOfLastDose) {
        this.timeOfLastDose = timeOfLastDose;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getVideoswatched() {
        return videoswatched;
    }

    public void setVideoswatched(int videoswatched) {
        this.videoswatched = videoswatched;
    }

    public long[] getRescueTimes() {
        return rescueTimes;
    }

    public void setRescueTimes(long[] rescueTimes) {
        this.rescueTimes = rescueTimes;
    }

    public long getRescueTimeAt(int index) {
        return rescueTimes[index];
    }

    public void setRescueTimeAt(int index, long value) {
        rescueTimes[index] = value;
    }

    public boolean[] getBadges() {
        return badges;
    }

    public void setBadges(boolean[] badges) {
        this.badges = badges;
    }

    public boolean getBadgeAt(int index) {
        return badges[index];
    }

    public void setBadgeAt(int index, boolean value) {
        badges[index] = value;
    }
    public int[] getBadgeRequirements() {
        return badgeRequirements;
    }

    public void setBadgeRequirements(int[] badgeRequirements) {
        this.badgeRequirements = badgeRequirements;
    }
    public void changeDayRequirement(int a){
        this.badgeRequirements[2] = a;
        this.rescueTimes = Arrays.copyOf(this.rescueTimes,a);
    }
    public void updateStreak() {
        long currentMillis = System.currentTimeMillis();

        if (this.timeOfLastDose < -1) {
            this.currentStreak = 1;
        } else {
            long daysSinceLastCheckin = (currentMillis - this.timeOfLastDose) / DAYINMS;
            if (daysSinceLastCheckin == 0){}
            else if (daysSinceLastCheckin == 1){this.currentStreak += 1;}
            else {this.currentStreak = 1;}
        }
        this.timeOfLastDose = currentMillis;
    }

    public void usedRescue(){
        long currentTime = System.currentTimeMillis();
        boolean isEmpty = false;
        for (int i = 0; i < rescueTimes.length; i++) {
            if (rescueTimes[i] == 0) {
                rescueTimes[i] = currentTime;
                isEmpty = true;
                break;
            }
        }
        if (!isEmpty) {
            for (int i = 0; i < rescueTimes.length - 1; i++) {
                rescueTimes[i] = rescueTimes[i + 1];
            }
            rescueTimes[rescueTimes.length - 1] = currentTime;
        }
    }

    public boolean checkBadge1() {
        return (this.currentStreak >= this.badgeRequirements[0]);
    }

    public boolean checkBadge2() {
        return (this.videoswatched >= this.badgeRequirements[1]);
    }
    public boolean checkBadge3() {
        if (this.rescueTimes[0] == 0){
            return false;
        }
        long currentTime = System.currentTimeMillis();
        return (currentTime - rescueTimes[0] >= DAYINMS * this.badgeRequirements[3]);
    }
}
