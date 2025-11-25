package com.example.myapplication.providermanaging;

public class Permission {
    Boolean rescueLogs;
    Boolean controllerAdherenceSummary;
    Boolean symptoms;
    Boolean triggers;
    Boolean peakFlow;
    Boolean triageIncidents;
    Boolean summaryCharts;

    public Permission() {
        this.rescueLogs = false;
        this.controllerAdherenceSummary = false;
        this.symptoms = false;
        this.triggers = false;
        this.peakFlow = false;
        this.triageIncidents = false;
        this.summaryCharts = false;
    }

    public Boolean getRescueLogs() {
        return rescueLogs;
    }

    public void setRescueLogs(Boolean rescueLogs) {
        this.rescueLogs = rescueLogs;
    }

    public Boolean getControllerAdherenceSummary() {
        return controllerAdherenceSummary;
    }

    public void setControllerAdherenceSummary(Boolean controllerAdherenceSummary) {
        this.controllerAdherenceSummary = controllerAdherenceSummary;
    }

    public Boolean getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(Boolean symptoms) {
        this.symptoms = symptoms;
    }

    public Boolean getTriggers() {
        return triggers;
    }

    public void setTriggers(Boolean triggers) {
        this.triggers = triggers;
    }

    public Boolean getPeakFlow() {
        return peakFlow;
    }

    public void setPeakFlow(Boolean peakFlow) {
        this.peakFlow = peakFlow;
    }

    public Boolean getTriageIncidents() {
        return triageIncidents;
    }

    public void setTriageIncidents(Boolean triageIncidents) {
        this.triageIncidents = triageIncidents;
    }

    public Boolean getSummaryCharts() {
        return summaryCharts;
    }

    public void setSummaryCharts(Boolean summaryCharts) {
        this.summaryCharts = summaryCharts;
    }
}
