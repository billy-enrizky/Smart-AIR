package com.example.myapplication.safety;

public class ZoneChangeEvent {
    private long timestamp;
    private Zone previousZone;
    private Zone newZone;
    private int pefValue;
    private double percentage;

    public ZoneChangeEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    public ZoneChangeEvent(long timestamp, Zone previousZone, Zone newZone, int pefValue, double percentage) {
        this.timestamp = timestamp;
        this.previousZone = previousZone;
        this.newZone = newZone;
        this.pefValue = pefValue;
        this.percentage = percentage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Zone getPreviousZone() {
        return previousZone;
    }

    public void setPreviousZone(Zone previousZone) {
        this.previousZone = previousZone;
    }

    public Zone getNewZone() {
        return newZone;
    }

    public void setNewZone(Zone newZone) {
        this.newZone = newZone;
    }

    public int getPefValue() {
        return pefValue;
    }

    public void setPefValue(int pefValue) {
        this.pefValue = pefValue;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}

