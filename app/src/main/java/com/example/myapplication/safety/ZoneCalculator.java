package com.example.myapplication.safety;

public class ZoneCalculator {
    public static Zone calculateZone(int pefValue, Integer personalBest) {
        if (personalBest == null || personalBest <= 0) {
            return Zone.UNKNOWN;
        }
        
        if (pefValue <= 0) {
            return Zone.UNKNOWN;
        }
        
        double percentage = (double) pefValue / personalBest * 100;
        
        if (percentage >= 80) {
            return Zone.GREEN;
        } else if (percentage >= 50) {
            return Zone.YELLOW;
        } else {
            return Zone.RED;
        }
    }
    
    public static double calculatePercentage(int pefValue, Integer personalBest) {
        if (personalBest == null || personalBest <= 0 || pefValue <= 0) {
            return 0.0;
        }
        return (double) pefValue / personalBest * 100;
    }
}

