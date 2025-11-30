package com.example.myapplication.reports;

import android.util.Log;

import com.example.myapplication.ControllerLog;
import com.example.myapplication.ControllerLogModel;
import com.example.myapplication.medication.ControllerSchedule;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.UserManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AdherenceCalculator {
    private static final String TAG = "AdherenceCalculator";

    public static void calculateAdherence(String childId, ControllerSchedule schedule, long startDate, long endDate, ResultCallBack<Double> callback) {
        if (schedule == null || schedule.isEmpty()) {
            if (callback != null) {
                callback.onComplete(0.0);
            }
            return;
        }

        Set<String> scheduledDays = getScheduledDays(startDate, endDate);
        int totalScheduledDays = scheduledDays.size();

        if (totalScheduledDays == 0) {
            if (callback != null) {
                callback.onComplete(0.0);
            }
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDateStr = dateFormat.format(new Date(startDate));
        String endDateStr = dateFormat.format(new Date(endDate));

        ControllerLogModel.readFromDB(childId, startDateStr, endDateStr, new ResultCallBack<HashMap<String, ControllerLog>>() {
            @Override
            public void onComplete(HashMap<String, ControllerLog> logs) {
                Set<String> daysWithLogs = new HashSet<>();
                if (logs != null) {
                    for (String dateKey : logs.keySet()) {
                        String dayKey = dateKey.split("_")[0];
                        daysWithLogs.add(dayKey);
                    }
                }

                int adherentDays = 0;
                for (String scheduledDay : scheduledDays) {
                    if (daysWithLogs.contains(scheduledDay)) {
                        adherentDays++;
                    }
                }

                double adherence = totalScheduledDays > 0 ? (adherentDays * 100.0 / totalScheduledDays) : 0.0;
                if (callback != null) {
                    callback.onComplete(adherence);
                }
            }
        });
    }

    private static Set<String> getScheduledDays(long startDate, long endDate) {
        Set<String> days = new HashSet<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        while (cal.getTimeInMillis() <= endDate) {
            days.add(dateFormat.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return days;
    }
}

