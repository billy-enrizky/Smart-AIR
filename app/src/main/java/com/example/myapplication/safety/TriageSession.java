package com.example.myapplication.safety;

import java.util.HashMap;
import java.util.Map;

public class TriageSession {
    private long startTime;
    private Map<String, Boolean> redFlags;
    private boolean rescueAttempts;
    private int rescueCount;
    private Integer pefValue;
    private Zone currentZone;
    private String decisionShown;
    private boolean escalated;
    private String sessionId;

    public TriageSession() {
        this.startTime = System.currentTimeMillis();
        this.redFlags = new HashMap<>();
        this.rescueAttempts = false;
        this.rescueCount = 0;
        this.escalated = false;
        this.sessionId = String.valueOf(startTime);
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Map<String, Boolean> getRedFlags() {
        return redFlags;
    }

    public void setRedFlags(Map<String, Boolean> redFlags) {
        this.redFlags = redFlags;
    }

    public boolean hasAnyRedFlag() {
        if (redFlags == null) {
            return false;
        }
        for (Boolean value : redFlags.values()) {
            if (value != null && value) {
                return true;
            }
        }
        return false;
    }

    public boolean isRescueAttempts() {
        return rescueAttempts;
    }

    public void setRescueAttempts(boolean rescueAttempts) {
        this.rescueAttempts = rescueAttempts;
    }

    public int getRescueCount() {
        return rescueCount;
    }

    public void setRescueCount(int rescueCount) {
        this.rescueCount = rescueCount;
    }

    public Integer getPefValue() {
        return pefValue;
    }

    public void setPefValue(Integer pefValue) {
        this.pefValue = pefValue;
    }

    public Zone getCurrentZone() {
        return currentZone;
    }

    public void setCurrentZone(Zone currentZone) {
        this.currentZone = currentZone;
    }

    public String getDecisionShown() {
        return decisionShown;
    }

    public void setDecisionShown(String decisionShown) {
        this.decisionShown = decisionShown;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public void setEscalated(boolean escalated) {
        this.escalated = escalated;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}

