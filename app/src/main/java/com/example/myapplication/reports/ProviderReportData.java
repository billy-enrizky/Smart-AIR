package com.example.myapplication.reports;

import com.example.myapplication.safety.TriageIncident;
import java.util.List;
import java.util.Map;

public class ProviderReportData {
    private long startDate;
    private long endDate;
    private String childName;
    private int rescueFrequency;
    private double controllerAdherence;
    private int symptomBurdenDays;
    private Map<String, Integer> zoneDistribution;
    private List<TriageIncident> notableIncidents;
    private List<PEFDataPoint> pefTrendData;

    public ProviderReportData() {
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public String getChildName() {
        return childName;
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }

    public int getRescueFrequency() {
        return rescueFrequency;
    }

    public void setRescueFrequency(int rescueFrequency) {
        this.rescueFrequency = rescueFrequency;
    }

    public double getControllerAdherence() {
        return controllerAdherence;
    }

    public void setControllerAdherence(double controllerAdherence) {
        this.controllerAdherence = controllerAdherence;
    }

    public int getSymptomBurdenDays() {
        return symptomBurdenDays;
    }

    public void setSymptomBurdenDays(int symptomBurdenDays) {
        this.symptomBurdenDays = symptomBurdenDays;
    }

    public Map<String, Integer> getZoneDistribution() {
        return zoneDistribution;
    }

    public void setZoneDistribution(Map<String, Integer> zoneDistribution) {
        this.zoneDistribution = zoneDistribution;
    }

    public List<TriageIncident> getNotableIncidents() {
        return notableIncidents;
    }

    public void setNotableIncidents(List<TriageIncident> notableIncidents) {
        this.notableIncidents = notableIncidents;
    }

    public List<PEFDataPoint> getPefTrendData() {
        return pefTrendData;
    }

    public void setPefTrendData(List<PEFDataPoint> pefTrendData) {
        this.pefTrendData = pefTrendData;
    }

    public static class PEFDataPoint {
        private long timestamp;
        private int pefValue;
        private double percentageOfPB;

        public PEFDataPoint() {
        }

        public PEFDataPoint(long timestamp, int pefValue, double percentageOfPB) {
            this.timestamp = timestamp;
            this.pefValue = pefValue;
            this.percentageOfPB = percentageOfPB;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public int getPefValue() {
            return pefValue;
        }

        public void setPefValue(int pefValue) {
            this.pefValue = pefValue;
        }

        public double getPercentageOfPB() {
            return percentageOfPB;
        }

        public void setPercentageOfPB(double percentageOfPB) {
            this.percentageOfPB = percentageOfPB;
        }
    }
}

