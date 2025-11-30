package com.example.myapplication.dailycheckin;

import android.widget.Toast;

import com.example.myapplication.ResultCallBack;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import com.example.myapplication.userdata.AccountType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public final class CheckInHistoryFilters extends CheckInEntry {
    String startDate;
    String endDate;
    boolean nightWakingInput;

    private static CheckInHistoryFilters filters;
    String history;
    private CheckInHistoryFilters() {
        super();
        //this.username = SignInChildProfileActivity.getCurrentChildUsername();
    }
    public static CheckInHistoryFilters getInstance() {
        if (filters == null) {
            filters = new CheckInHistoryFilters();
        }
        return filters;
    }
    public String getStartDate() {
        return this.startDate;
    }
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    public String getEndDate() {
        return this.endDate;
    }
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
    public boolean getNightWakingInput() {
        return this.nightWakingInput;
    }
    public void setNightWakingInput(boolean nightWakingInput) {
        this.nightWakingInput = nightWakingInput;
    }

    public boolean matchFilters(CheckInEntry entry) {
        if (this.nightWakingInput) {
            if (this.nightWaking != entry.getNightWaking()) {
                return false;
            }
        }
        if (this.activityLimits != null && !this.activityLimits.equals(entry.getActivityLimits())) {
            return false;
        }
        if (this.coughWheezeLevel != -1 && this.coughWheezeLevel != entry.getCoughWheezeLevel()) {
            return false;
        }
        if (this.triggers != null && !this.triggers.isEmpty()) {
            ArrayList<String>commonTriggers = new ArrayList<>(this.triggers);
            if (entry.getTriggers() != null) {
                commonTriggers.retainAll(entry.getTriggers());
                return !commonTriggers.isEmpty();
            } else {
                return false;
            }
        }
        return true;
    }
}
