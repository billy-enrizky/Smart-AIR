package com.example.myapplication.safety;

import java.util.Map;

public class TriageIncident {
    private long timestamp;
    private Map<String, Boolean> redFlags;
    private boolean rescueAttempts;
    private int rescueCount;
    private Integer pefValue;
    private String decisionShown;
    private Zone zone;
    private boolean escalated;
    private String sessionId;

    public TriageIncident() {
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Boolean> getRedFlags() {
        return redFlags;
    }

    public void setRedFlags(Map<String, Boolean> redFlags) {
        this.redFlags = redFlags;
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

    public String getDecisionShown() {
        return decisionShown;
    }

    public void setDecisionShown(String decisionShown) {
        this.decisionShown = decisionShown;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
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

