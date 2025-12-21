package com.example.myapplication.dailycheckin;

import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.AccountType;

import java.util.ArrayList;
import java.util.Collections;

public class CheckInEntry {
    String username;
    boolean nightWaking;
    String activityLimits;
    double coughWheezeLevel = -1;
    ArrayList<String> triggers = new ArrayList<>();
    long timestamp; // Timestamp to allow multiple entries per day
    public CheckInEntry(String username, boolean nightWaking, String activityLimits, double coughWheezeLevel, ArrayList<String>triggers) {
        this.username = username;
        this.nightWaking = nightWaking;
        this.activityLimits = activityLimits;
        this.coughWheezeLevel = coughWheezeLevel;
        this.triggers = triggers;
        this.timestamp = System.currentTimeMillis(); // Set timestamp to current time
    }
    public CheckInEntry() {

    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getUsername() {
        return this.username;
    }

    public void setNightWaking(boolean nightWaking) {
        this.nightWaking = nightWaking;
    }
    public boolean getNightWaking() {
        return this.nightWaking;
    }
    public String getActivityLimits() {
        return this.activityLimits;
    }
    public void setActivityLimits(String activityLimits) {
        this.activityLimits=activityLimits;
    }
    public double getCoughWheezeLevel() {
        return this.coughWheezeLevel;
    }
    public void setCoughWheezeLevel(double coughWheezeLevel) {
        this.coughWheezeLevel = coughWheezeLevel;
    }
    public ArrayList<String>getTriggers() {
        return this.triggers;
    }
    public void setTriggers(ArrayList<String> triggers) {
        this.triggers = triggers;
    }
    public long getTimestamp() {
        return this.timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /*@Override public boolean equals (Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof CheckInEntry) {
            CheckInEntry c = (CheckInEntry)o;
            boolean req_1 = this.nightWaking == c.getNightWaking() && this.activityLimits.equals(c.getActivityLimits()) && this.coughWheezeLevel == c.getCoughWheezeLevel();
            if (req_1) {
                Collections.sort(this.triggers);
                Collections.sort(c.getTriggers());
                return this.triggers.equals(c.getTriggers());
            }
        }
        return false;
    }*/
}
