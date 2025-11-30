package com.example.myapplication.safety;

public class RescueUsage {
    private long timestamp;
    private int count;

    public RescueUsage() {
        this.timestamp = System.currentTimeMillis();
        this.count = 1;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

