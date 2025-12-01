package com.example.myapplication.reports;

import com.example.myapplication.safety.Zone;

public class DashboardStats {
    private String childId;
    private String childName;
    private Zone todayZone;
    private Long lastRescueTime;
    private int weeklyRescueCount;
    private String lastRescueTimeFormatted;
    private int dailyRescueCount;
    private int dailySymptomsCount;
    private double controllerAdherence;

    public DashboardStats() {
    }

    public DashboardStats(String childId, String childName) {
        this.childId = childId;
        this.childName = childName;
        this.todayZone = Zone.UNKNOWN;
        this.lastRescueTime = null;
        this.weeklyRescueCount = 0;
        this.lastRescueTimeFormatted = "Never";
        this.dailyRescueCount = 0;
        this.dailySymptomsCount = 0;
        this.controllerAdherence = 0.0;
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

    public int getDailyRescueCount() {
        return dailyRescueCount;
    }

    public void setDailyRescueCount(int dailyRescueCount) {
        this.dailyRescueCount = dailyRescueCount;
    }

    public int getDailySymptomsCount() {
        return dailySymptomsCount;
    }

    public void setDailySymptomsCount(int dailySymptomsCount) {
        this.dailySymptomsCount = dailySymptomsCount;
    }

    public double getControllerAdherence() {
        return controllerAdherence;
    }

    public void setControllerAdherence(double controllerAdherence) {
        this.controllerAdherence = controllerAdherence;
    }
}

