package com.example.myapplication.safety;

public enum Zone {
    GREEN,
    YELLOW,
    RED,
    UNKNOWN;

    public String getDisplayName() {
        switch (this) {
            case GREEN:
                return "Green Zone";
            case YELLOW:
                return "Yellow Zone";
            case RED:
                return "Red Zone";
            case UNKNOWN:
            default:
                return "Zone Not Available";
        }
    }

    public int getColorResource() {
        switch (this) {
            case GREEN:
                return android.graphics.Color.GREEN;
            case YELLOW:
                return android.graphics.Color.YELLOW;
            case RED:
                return android.graphics.Color.RED;
            case UNKNOWN:
            default:
                return android.graphics.Color.GRAY;
        }
    }
}

