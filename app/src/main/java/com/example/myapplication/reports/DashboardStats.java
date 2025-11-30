package com.example.myapplication.reports;

import com.example.myapplication.safety.Zone;

public class DashboardStats {
    private String childId;
    private String childName;
    private Zone todayZone;
    private Long lastRescueTime;
    private int weeklyRescueCount;
    private String lastRescueTimeFormatted;

    public DashboardStats() {
    }

    public DashboardStats(String childId, String childName) {
        this.childId = childId;
        this.childName = childName;
        this.todayZone = Zone.UNKNOWN;
        this.lastRescueTime = null;
        this.weeklyRescueCount = 0;
        this.lastRescueTimeFormatted = "Never";
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

    public Zone getTodayZone() {
        return todayZone;
    }

    public void setTodayZone(Zone todayZone) {
        this.todayZone = todayZone;
    }

    public Long getLastRescueTime() {
        return lastRescueTime;
    }

    public void setLastRescueTime(Long lastRescueTime) {
        this.lastRescueTime = lastRescueTime;
    }

    public int getWeeklyRescueCount() {
        return weeklyRescueCount;
    }

    public void setWeeklyRescueCount(int weeklyRescueCount) {
        this.weeklyRescueCount = weeklyRescueCount;
    }

    public String getLastRescueTimeFormatted() {
        return lastRescueTimeFormatted;
    }

    public void setLastRescueTimeFormatted(String lastRescueTimeFormatted) {
        this.lastRescueTimeFormatted = lastRescueTimeFormatted;
    }
}

