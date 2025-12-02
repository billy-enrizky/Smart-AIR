package com.example.myapplication.medication;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a controller medication schedule.
 * 
 * The schedule stores specific dates (in yyyy-MM-dd format) when controller medication
 * should be taken. This allows for discontinuous schedules (not every day).
 * Multiple controller doses can be scheduled for the same day.
 * 
 * Adherence is calculated as: (days with logged controller dose / planned dose days) × 100
 * Time matching is not required - a day is considered adherent if at least one
 * controller log exists for that day.
 */
public class ControllerSchedule {
    private List<String> dates;  // Dates in yyyy-MM-dd format

    public ControllerSchedule() {
        this.dates = new ArrayList<>();
    }

    public ControllerSchedule(List<String> dates) {
        this.dates = dates != null ? new ArrayList<>(dates) : new ArrayList<>();
    }

    /**
     * Get the list of scheduled dates (yyyy-MM-dd format).
     * @return List of date strings
     */
    public List<String> getDates() {
        return dates;
    }

    /**
     * Set the list of scheduled dates.
     * @param dates List of date strings in yyyy-MM-dd format
     */
    public void setDates(List<String> dates) {
        this.dates = dates != null ? new ArrayList<>(dates) : new ArrayList<>();
    }

    /**
     * Add a date to the schedule.
     * @param date Date string in yyyy-MM-dd format
     */
    public void addDate(String date) {
        if (dates == null) {
            dates = new ArrayList<>();
        }
        if (date != null && !dates.contains(date)) {
            dates.add(date);
        }
    }

    /**
     * Remove a date from the schedule.
     * @param date Date string in yyyy-MM-dd format
     */
    public void removeDate(String date) {
        if (dates != null) {
            dates.remove(date);
        }
    }

    /**
     * Check if the schedule is empty.
     * @return true if no dates are scheduled
     */
    public boolean isEmpty() {
        return dates == null || dates.isEmpty();
    }

    /**
     * Get the number of scheduled dates.
     * @return Number of dates in the schedule
     */
    public int size() {
        return dates != null ? dates.size() : 0;
    }

    // Backward compatibility: support old "times" field for migration
    // If dates is empty but times exists, we'll need to handle migration
    @Deprecated
    public List<String> getTimes() {
        return new ArrayList<>();  // Return empty list for old format
    }

    @Deprecated
    public void setTimes(List<String> times) {
        // Ignore old times format
    }
}

