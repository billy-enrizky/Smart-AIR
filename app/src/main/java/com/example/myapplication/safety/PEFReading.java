package com.example.myapplication.safety;

public class PEFReading {
    private int value;
    private long timestamp;
    private boolean preMed;
    private boolean postMed;
    private String notes;

    public PEFReading() {
        this.timestamp = System.currentTimeMillis();
        this.preMed = false;
        this.postMed = false;
        this.notes = "";
    }

    public PEFReading(int value, long timestamp, boolean preMed, boolean postMed, String notes) {
        this.value = value;
        this.timestamp = timestamp;
        this.preMed = preMed;
        this.postMed = postMed;
        this.notes = notes != null ? notes : "";
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isPreMed() {
        return preMed;
    }

    public void setPreMed(boolean preMed) {
        this.preMed = preMed;
    }

    public boolean isPostMed() {
        return postMed;
    }

    public void setPostMed(boolean postMed) {
        this.postMed = postMed;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes : "";
    }
}

