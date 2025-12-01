package com.example.myapplication.reports;

import android.util.Log;

import com.example.myapplication.ControllerLog;
import com.example.myapplication.ControllerLogModel;
import com.example.myapplication.medication.ControllerSchedule;
import com.example.myapplication.ResultCallBack;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Utility class for calculating controller medication adherence.
 * 
 * Adherence is calculated as: (days with logged controller dose / planned dose days) × 100
 * 
 * Time matching is not required - a day is considered adherent if at least one
 * controller log exists for that day, regardless of the time.
 * 
 * If a controller schedule exists with configured times, it is assumed that
 * medication should be taken daily (all days in the date range are planned dose days).
 */
public class AdherenceCalculator {
    private static final String TAG = "AdherenceCalculator";

    /**
     * Calculates adherence percentage for a child's controller medication usage.
     * 
     * @param childId The child's ID/username used to retrieve controller logs
     * @param schedule The controller medication schedule (list of times)
     * @param startDate Start of date range (timestamp in milliseconds)
     * @param endDate End of date range (timestamp in milliseconds, inclusive)
     * @param callback Callback to receive the adherence percentage (0.0-100.0)
     */
    public static void calculateAdherence(String childId, ControllerSchedule schedule, long startDate, long endDate, ResultCallBack<Double> callback) {
        if (schedule == null || schedule.isEmpty()) {
            Log.d(TAG, "No schedule configured for childId: " + childId);
            if (callback != null) {
                callback.onComplete(0.0);
            }
            return;
        }

        if (startDate > endDate) {
            Log.w(TAG, "Invalid date range: startDate > endDate for childId: " + childId);
            if (callback != null) {
                callback.onComplete(0.0);
            }
            return;
        }

        // Get all days in the date range as "planned dose days"
        // If schedule has times configured, medication should be taken daily
        Set<String> scheduledDays = getScheduledDays(startDate, endDate);
        int totalScheduledDays = scheduledDays.size();

        if (totalScheduledDays == 0) {
            Log.d(TAG, "No scheduled days in date range for childId: " + childId);
            if (callback != null) {
                callback.onComplete(0.0);
            }
            return;
        }

        Log.d(TAG, String.format("Calculating adherence for childId: %s, date range: %d days", childId, totalScheduledDays));

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDateStr = dateFormat.format(new Date(startDate));
        String endDateStr = dateFormat.format(new Date(endDate));

        // Retrieve controller logs for the date range
        ControllerLogModel.readFromDB(childId, startDateStr, endDateStr, new ResultCallBack<HashMap<String, ControllerLog>>() {
            @Override
            public void onComplete(HashMap<String, ControllerLog> logs) {
                // Extract unique days from logs (date keys are in format "yyyy-MM-dd_HH:mm:ss")
                Set<String> daysWithLogs = new HashSet<>();
                if (logs != null) {
                    for (String dateKey : logs.keySet()) {
                        // Extract date part (before underscore)
                        String dayKey = dateKey.split("_")[0];
                        daysWithLogs.add(dayKey);
                    }
                }

                // Count how many scheduled days have at least one log
                // Time matching is not required - any log on a scheduled day counts
                int adherentDays = 0;
                for (String scheduledDay : scheduledDays) {
                    if (daysWithLogs.contains(scheduledDay)) {
                        adherentDays++;
                    }
                }

                // Calculate adherence: (days with logged controller dose / planned dose days) × 100
                double adherence = totalScheduledDays > 0 ? (adherentDays * 100.0 / totalScheduledDays) : 0.0;
                
                Log.d(TAG, String.format("Adherence calculation for childId: %s - Adherent days: %d/%d (%.2f%%)", 
                    childId, adherentDays, totalScheduledDays, adherence));

                if (callback != null) {
                    callback.onComplete(adherence);
                }
            }
        });
    }

    /**
     * Gets all days in the date range as a set of date strings (yyyy-MM-dd format).
     * The end date is inclusive.
     * 
     * @param startDate Start date (timestamp in milliseconds)
     * @param endDate End date (timestamp in milliseconds, inclusive)
     * @return Set of date strings in yyyy-MM-dd format
     */
    private static Set<String> getScheduledDays(long startDate, long endDate) {
        Set<String> days = new HashSet<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Ensure end date is at end of day for inclusive comparison
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(endDate);
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        long endDateInclusive = endCal.getTimeInMillis();

        // Add all days from start to end (inclusive)
        while (cal.getTimeInMillis() <= endDateInclusive) {
            days.add(dateFormat.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return days;
    }
}

