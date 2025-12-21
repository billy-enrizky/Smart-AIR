package com.example.myapplication.reports;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.ControllerLog;
import com.example.myapplication.ControllerLogModel;
import com.example.myapplication.RescueLog;
import com.example.myapplication.RescueLogModel;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.charts.ChartComponent;
import com.example.myapplication.safety.PEFReading;
import com.example.myapplication.safety.RescueUsage;
import com.example.myapplication.safety.TriageIncident;
import com.example.myapplication.safety.Zone;
import com.example.myapplication.safety.ZoneCalculator;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.utils.FirebaseKeyEncoder;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrendSnippetActivity extends AppCompatActivity {
    private static final String TAG = "TrendSnippetActivity";

    private FrameLayout frameLayoutZoneChart;
    private FrameLayout frameLayoutTrendChart;
    private FrameLayout frameLayoutRescueChart;
    private FrameLayout frameLayoutSymptomsChart;
    private FrameLayout frameLayoutMedicineChart;
    private TextView textViewChildName;

    private String parentId;
    private String childId;
    private String childName;
    private ChildAccount childAccount;
    private Integer personalBest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trend_snippet);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        parentId = getIntent().getStringExtra("parentId");
        childId = getIntent().getStringExtra("childId");
        childName = getIntent().getStringExtra("childName");
        if (parentId == null || childId == null) {
            Log.e(TAG, "Missing parentId or childId");
            finish();
            return;
        }

        initializeViews();
        loadChildAccount();
    }

    private void initializeViews() {
        Button buttonBack = findViewById(R.id.buttonBack);
        frameLayoutZoneChart = findViewById(R.id.frameLayoutZoneChart);
        frameLayoutTrendChart = findViewById(R.id.frameLayoutTrendChart);
        frameLayoutRescueChart = findViewById(R.id.frameLayoutRescueChart);
        frameLayoutSymptomsChart = findViewById(R.id.frameLayoutSymptomsChart);
        frameLayoutMedicineChart = findViewById(R.id.frameLayoutMedicineChart);
        textViewChildName = findViewById(R.id.textViewChildName);

        if (childName != null) {
            textViewChildName.setText(childName + " - Trend Snippet");
        }

        buttonBack.setOnClickListener(v -> finish());
    }

    private void loadChildAccount() {
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference childRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId);

        childRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                childAccount = task.getResult().getValue(ChildAccount.class);
                if (childAccount != null) {
                    personalBest = childAccount.getPersonalBest();
                    loadAllCharts();
                }
            } else {
                Log.e(TAG, "Error loading child account", task.getException());
            }
        });
    }

    private void loadAllCharts() {
        loadZoneDistribution();
        if(!getIntent().hasExtra("PEFBanned")){
            loadPEFTrend();
        }else{
            findViewById(R.id.PEF).setVisibility(View.GONE);
        }
        if(!getIntent().hasExtra("RescueLogBanned")){
            loadRescuePerDay();
        }else{
            findViewById(R.id.RM).setVisibility(View.GONE);
        }
        if(!getIntent().hasExtra("SymptomsBanned")) {
            loadSymptomsPerDay();
        }else{
            findViewById(R.id.SC).setVisibility(View.GONE);
        }
        loadMedicinePerDay();
    }

    private void loadZoneDistribution() {
        if (personalBest == null || personalBest <= 0) {
            return;
        }

        Calendar cal = Calendar.getInstance();
        long endDate = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        long startDate = cal.getTimeInMillis();

        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("pefReadings");

        // Use direct listener instead of orderByChild query to avoid index requirements
        // Filter by date range in code after loading
        pefRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, Integer> zoneCounts = new HashMap<>();
                zoneCounts.put("green", 0);
                zoneCounts.put("yellow", 0);
                zoneCounts.put("red", 0);
                zoneCounts.put("unknown", 0);

                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        PEFReading reading = child.getValue(PEFReading.class);
                        if (reading != null && reading.getTimestamp() >= startDate && reading.getTimestamp() <= endDate) {
                            Zone zone = ZoneCalculator.calculateZone(reading.getValue(), personalBest);
                            String zoneName = ChartComponent.normalizeZoneName(zone.getDisplayName());
                            zoneCounts.put(zoneName, zoneCounts.getOrDefault(zoneName, 0) + 1);
                        }
                    }
                    Log.d(TAG, "Loaded PEF readings for zone distribution from Firebase path: " + pefRef.toString());
                } else {
                    Log.d(TAG, "No PEF readings found at Firebase path: " + pefRef.toString());
                }

                runOnUiThread(() -> {
                    frameLayoutZoneChart.removeAllViews();
                    View chartView = ChartComponent.createChartView(TrendSnippetActivity.this, frameLayoutZoneChart, ChartComponent.ChartType.BAR);
                    frameLayoutZoneChart.addView(chartView);
                    BarChart barChart = chartView.findViewById(R.id.barChart);
                    ChartComponent.setupBarChart(barChart, zoneCounts, "Zone Distribution");
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading zone distribution from Firebase path: " + pefRef.toString(), error.toException());
            }
        });
    }

    private void loadPEFTrend() {
        if (personalBest == null || personalBest <= 0) {
            Log.w(TAG, "loadPEFTrend: Skipping because personalBest is null or <= 0. personalBest=" + personalBest);
            return;
        }

        Calendar cal = Calendar.getInstance();
        long endDate = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        long startDate = cal.getTimeInMillis();

        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        String firebasePath = "users/" + parentId + "/children/" + encodedChildId + "/pefReadings";
        Log.d(TAG, "loadPEFTrend: Loading PEF data for chart");
        Log.d(TAG, "loadPEFTrend: childId=" + childId + ", encodedChildId=" + encodedChildId + ", parentId=" + parentId);
        Log.d(TAG, "loadPEFTrend: Firebase path=" + firebasePath);
        Log.d(TAG, "loadPEFTrend: Date range: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(startDate)) + " to " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(endDate)));
        
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("pefReadings");

        // Use direct listener instead of orderByChild query to avoid index requirements
        // Filter by date range in code after loading
        // Check encoded path first (current standard), then raw path for backward compatibility
        final List<ChartComponent.PEFDataPoint> dataPoints = new ArrayList<>();
        final long finalStartDate = startDate;
        final long finalEndDate = endDate;
        
        pefRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int totalReadings = 0;
                boolean foundData = false;
                
                if (snapshot.exists()) {
                    foundData = true;
                    totalReadings = (int) snapshot.getChildrenCount();
                    Log.d(TAG, "loadPEFTrend: Found " + totalReadings + " total PEF readings in Firebase at encoded path");
                    
                    for (DataSnapshot child : snapshot.getChildren()) {
                        PEFReading reading = child.getValue(PEFReading.class);
                        if (reading != null) {
                            long timestamp = reading.getTimestamp();
                            if (timestamp >= finalStartDate && timestamp <= finalEndDate) {
                                dataPoints.add(new ChartComponent.PEFDataPoint(timestamp, reading.getValue()));
                            }
                            Log.d(TAG, "loadPEFTrend: Reading - timestamp=" + timestamp + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timestamp)) + "), value=" + reading.getValue() + ", inRange=" + (timestamp >= finalStartDate && timestamp <= finalEndDate));
                        } else {
                            Log.w(TAG, "loadPEFTrend: Failed to parse PEF reading from snapshot: " + child.getKey());
                        }
                    }
                    Log.d(TAG, "loadPEFTrend: Loaded " + dataPoints.size() + " PEF readings in date range out of " + totalReadings + " total from Firebase path: " + pefRef.toString());
                } else {
                    Log.d(TAG, "loadPEFTrend: No PEF readings found at encoded Firebase path: " + pefRef.toString());
                }

                // If no data found at encoded path and encoded != raw, check raw path for backward compatibility
                // This handles data saved before the encoding fix was applied
                if (!foundData && !encodedChildId.equals(childId)) {
                    Log.d(TAG, "loadPEFTrend: Checking raw childId path for backward compatibility: " + childId);
                    DatabaseReference rawPefRef = UserManager.mDatabase
                            .child("users")
                            .child(parentId)
                            .child("children")
                            .child(childId)
                            .child("pefReadings");
                    
                    rawPefRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot rawSnapshot) {
                            if (rawSnapshot.exists()) {
                                int rawTotalReadings = (int) rawSnapshot.getChildrenCount();
                                Log.d(TAG, "loadPEFTrend: Found " + rawTotalReadings + " total PEF readings in Firebase at raw path (backward compatibility)");
                                
                                for (DataSnapshot child : rawSnapshot.getChildren()) {
                                    PEFReading reading = child.getValue(PEFReading.class);
                                    if (reading != null) {
                                        long timestamp = reading.getTimestamp();
                                        if (timestamp >= finalStartDate && timestamp <= finalEndDate) {
                                            dataPoints.add(new ChartComponent.PEFDataPoint(timestamp, reading.getValue()));
                                        }
                                        Log.d(TAG, "loadPEFTrend: Reading (raw path) - timestamp=" + timestamp + " (" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(timestamp)) + "), value=" + reading.getValue() + ", inRange=" + (timestamp >= finalStartDate && timestamp <= finalEndDate));
                                    }
                                }
                                Log.d(TAG, "loadPEFTrend: Loaded " + dataPoints.size() + " total PEF readings in date range from raw path: " + rawPefRef.toString());
                            } else {
                                Log.d(TAG, "loadPEFTrend: No PEF readings found at raw Firebase path either: " + rawPefRef.toString());
                            }
                            
                            dataPoints.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

                            runOnUiThread(() -> {
                                frameLayoutTrendChart.removeAllViews();
                                View chartView = ChartComponent.createChartView(TrendSnippetActivity.this, frameLayoutTrendChart, ChartComponent.ChartType.LINE);
                                frameLayoutTrendChart.addView(chartView);
                                LineChart lineChart = chartView.findViewById(R.id.lineChart);
                                ChartComponent.setupLineChart(lineChart, dataPoints, "PEF Trend");
                                Log.d(TAG, "loadPEFTrend: Chart updated with " + dataPoints.size() + " data points");
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e(TAG, "loadPEFTrend: Error loading PEF trend from raw Firebase path: " + rawPefRef.toString(), error.toException());
                            // Still update chart with data from encoded path (if any)
                            dataPoints.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                            runOnUiThread(() -> {
                                frameLayoutTrendChart.removeAllViews();
                                View chartView = ChartComponent.createChartView(TrendSnippetActivity.this, frameLayoutTrendChart, ChartComponent.ChartType.LINE);
                                frameLayoutTrendChart.addView(chartView);
                                LineChart lineChart = chartView.findViewById(R.id.lineChart);
                                ChartComponent.setupLineChart(lineChart, dataPoints, "PEF Trend");
                                Log.d(TAG, "loadPEFTrend: Chart updated with " + dataPoints.size() + " data points");
                            });
                        }
                    });
                } else {
                    // Data found at encoded path or encoded == raw, update chart
                    dataPoints.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

                    runOnUiThread(() -> {
                        frameLayoutTrendChart.removeAllViews();
                        View chartView = ChartComponent.createChartView(TrendSnippetActivity.this, frameLayoutTrendChart, ChartComponent.ChartType.LINE);
                        frameLayoutTrendChart.addView(chartView);
                        LineChart lineChart = chartView.findViewById(R.id.lineChart);
                        ChartComponent.setupLineChart(lineChart, dataPoints, "PEF Trend");
                        Log.d(TAG, "loadPEFTrend: Chart updated with " + dataPoints.size() + " data points");
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "loadPEFTrend: Error loading PEF trend from Firebase path: " + pefRef.toString(), error.toException());
                Log.e(TAG, "loadPEFTrend: Error code: " + error.getCode() + ", message: " + error.getMessage());
            }
        });
    }

    private void loadRescuePerDay() {
        Calendar cal = Calendar.getInstance();
        long endDate = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        long startDate = cal.getTimeInMillis();

        Map<String, Integer> dailyCounts = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDateStr = logDateFormat.format(new Date(startDate));
        String endDateStr = logDateFormat.format(new Date(endDate));

        // Load from triage sessions (rescueUsage)
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference rescueRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("rescueUsage");

        Query query = rescueRef.orderByChild("timestamp").startAt(startDate).endAt(endDate);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        RescueUsage usage = child.getValue(RescueUsage.class);
                        if (usage != null) {
                            String dateKey = dateFormat.format(new Date(usage.getTimestamp()));
                            int currentCount = dailyCounts.getOrDefault(dateKey, 0);
                            dailyCounts.put(dateKey, currentCount + usage.getCount());
                        }
                    }
                }

                // Load from direct rescue inhaler usage (RescueLogManager)
                RescueLogModel.readFromDB(childId, startDateStr + "_00:00:00", endDateStr + "_23:59:59", new ResultCallBack<HashMap<String, RescueLog>>() {
                    @Override
                    public void onComplete(HashMap<String, RescueLog> logs) {
                        if (logs != null) {
                            for (RescueLog log : logs.values()) {
                                String dateKey = dateFormat.format(new Date(log.getTimestamp()));
                                int currentCount = dailyCounts.getOrDefault(dateKey, 0);
                                dailyCounts.put(dateKey, currentCount + 1); // Each RescueLog entry is 1 dose
                            }
                        }

                        runOnUiThread(() -> {
                            frameLayoutRescueChart.removeAllViews();
                            View chartView = ChartComponent.createChartView(TrendSnippetActivity.this, frameLayoutRescueChart, ChartComponent.ChartType.BAR);
                            frameLayoutRescueChart.addView(chartView);
                            BarChart barChart = chartView.findViewById(R.id.barChart);
                            ChartComponent.setupDailyBarChart(barChart, dailyCounts, "Rescue Medicine Use Per Day", android.graphics.Color.parseColor("#F44336"));
                        });
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading rescue per day from triage sessions", error.toException());
                // Still try to load from direct usage even if triage data fails
                RescueLogModel.readFromDB(childId, startDateStr + "_00:00:00", endDateStr + "_23:59:59", new ResultCallBack<HashMap<String, RescueLog>>() {
                    @Override
                    public void onComplete(HashMap<String, RescueLog> logs) {
                        Map<String, Integer> dailyCounts = new HashMap<>();
                        if (logs != null) {
                            for (RescueLog log : logs.values()) {
                                String dateKey = dateFormat.format(new Date(log.getTimestamp()));
                                int currentCount = dailyCounts.getOrDefault(dateKey, 0);
                                dailyCounts.put(dateKey, currentCount + 1);
                            }
                        }

                        runOnUiThread(() -> {
                            frameLayoutRescueChart.removeAllViews();
                            View chartView = ChartComponent.createChartView(TrendSnippetActivity.this, frameLayoutRescueChart, ChartComponent.ChartType.BAR);
                            frameLayoutRescueChart.addView(chartView);
                            BarChart barChart = chartView.findViewById(R.id.barChart);
                            ChartComponent.setupDailyBarChart(barChart, dailyCounts, "Rescue Medicine Use Per Day", android.graphics.Color.parseColor("#F44336"));
                        });
                    }
                });
            }
        });
    }

    private void loadSymptomsPerDay() {
        Calendar cal = Calendar.getInstance();
        long endDate = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        long startDate = cal.getTimeInMillis();

        Map<String, Integer> dailyCounts = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDateStr = logDateFormat.format(new Date(startDate));
        String endDateStr = logDateFormat.format(new Date(endDate));

        // Load from triage sessions (incidents)
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference incidentRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("incidents");

        // Use direct listener instead of orderByChild query to avoid index requirements
        // Filter by date range in code after loading
        incidentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        TriageIncident incident = child.getValue(TriageIncident.class);
                        if (incident != null && incident.getTimestamp() >= startDate && incident.getTimestamp() <= endDate) {
                            String dateKey = dateFormat.format(new Date(incident.getTimestamp()));
                            dailyCounts.put(dateKey, dailyCounts.getOrDefault(dateKey, 0) + 1);
                        }
                    }
                    Log.d(TAG, "Loaded incidents from Firebase path: " + incidentRef.toString());
                } else {
                    Log.d(TAG, "No incidents found at Firebase path: " + incidentRef.toString());
                }

                // Also load from daily check-ins (CheckInManager)
                com.example.myapplication.dailycheckin.CheckInModel.readFromDB(childId, startDateStr, endDateStr, new ResultCallBack<HashMap<String, com.example.myapplication.dailycheckin.DailyCheckin>>() {
                    @Override
                    public void onComplete(HashMap<String, com.example.myapplication.dailycheckin.DailyCheckin> checkIns) {
                        if (checkIns != null) {
                            for (Map.Entry<String, com.example.myapplication.dailycheckin.DailyCheckin> entry : checkIns.entrySet()) {
                                String dateKey = entry.getKey(); // CheckInManager uses date as key (yyyy-MM-dd)
                                com.example.myapplication.dailycheckin.DailyCheckin checkIn = entry.getValue();
                                // Count check-ins with symptoms (coughWheezeLevel > 0 or nightWaking or activityLimits)
                                if (checkIn != null && (checkIn.getCoughWheezeLevel() > 0 || checkIn.getNightWaking() || 
                                        (checkIn.getActivityLimits() != null && !checkIn.getActivityLimits().isEmpty()))) {
                                    if (dateKey != null && !dateKey.isEmpty()) {
                                        dailyCounts.put(dateKey, dailyCounts.getOrDefault(dateKey, 0) + 1);
                                    }
                                }
                            }
                        }

                        runOnUiThread(() -> {
                            frameLayoutSymptomsChart.removeAllViews();
                            View chartView = ChartComponent.createChartView(TrendSnippetActivity.this, frameLayoutSymptomsChart, ChartComponent.ChartType.BAR);
                            frameLayoutSymptomsChart.addView(chartView);
                            BarChart barChart = chartView.findViewById(R.id.barChart);
                            ChartComponent.setupDailyBarChart(barChart, dailyCounts, "Symptoms Count Per Day", android.graphics.Color.parseColor("#9C27B0"));
                        });
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading symptoms per day from triage sessions at Firebase path: " + incidentRef.toString(), error.toException());
                // Still try to load from daily check-ins even if triage data fails
                com.example.myapplication.dailycheckin.CheckInModel.readFromDB(childId, startDateStr, endDateStr, new ResultCallBack<HashMap<String, com.example.myapplication.dailycheckin.DailyCheckin>>() {
                    @Override
                    public void onComplete(HashMap<String, com.example.myapplication.dailycheckin.DailyCheckin> checkIns) {
                        Map<String, Integer> dailyCounts = new HashMap<>();
                        if (checkIns != null) {
                            for (Map.Entry<String, com.example.myapplication.dailycheckin.DailyCheckin> entry : checkIns.entrySet()) {
                                String dateKey = entry.getKey(); // CheckInManager uses date as key (yyyy-MM-dd)
                                com.example.myapplication.dailycheckin.DailyCheckin checkIn = entry.getValue();
                                if (checkIn != null && (checkIn.getCoughWheezeLevel() > 0 || checkIn.getNightWaking() || 
                                        (checkIn.getActivityLimits() != null && !checkIn.getActivityLimits().isEmpty()))) {
                                    if (dateKey != null && !dateKey.isEmpty()) {
                                        dailyCounts.put(dateKey, dailyCounts.getOrDefault(dateKey, 0) + 1);
                                    }
                                }
                            }
                        }

                        runOnUiThread(() -> {
                            frameLayoutSymptomsChart.removeAllViews();
                            View chartView = ChartComponent.createChartView(TrendSnippetActivity.this, frameLayoutSymptomsChart, ChartComponent.ChartType.BAR);
                            frameLayoutSymptomsChart.addView(chartView);
                            BarChart barChart = chartView.findViewById(R.id.barChart);
                            ChartComponent.setupDailyBarChart(barChart, dailyCounts, "Symptoms Count Per Day", android.graphics.Color.parseColor("#9C27B0"));
                        });
                    }
                });
            }
        });
    }

    private void loadMedicinePerDay() {
        Calendar cal = Calendar.getInstance();
        long endDate = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        long startDate = cal.getTimeInMillis();

        Map<String, Integer> dailyCounts = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDateStr = logDateFormat.format(new Date(startDate));
        String endDateStr = logDateFormat.format(new Date(endDate));

        // Load from direct controller inhaler usage (ControllerLogManager)
        ControllerLogModel.readFromDB(childId, startDateStr + "_00:00:00", endDateStr + "_23:59:59", new ResultCallBack<HashMap<String, ControllerLog>>() {
            @Override
            public void onComplete(HashMap<String, ControllerLog> logs) {
                if (logs != null) {
                    for (ControllerLog log : logs.values()) {
                        String dateKey = dateFormat.format(new Date(log.getTimestamp()));
                        int currentCount = dailyCounts.getOrDefault(dateKey, 0);
                        dailyCounts.put(dateKey, currentCount + 1); // Each ControllerLog entry is 1 dose
                    }
                }

                runOnUiThread(() -> {
                    frameLayoutMedicineChart.removeAllViews();
                    View chartView = ChartComponent.createChartView(TrendSnippetActivity.this, frameLayoutMedicineChart, ChartComponent.ChartType.BAR);
                    frameLayoutMedicineChart.addView(chartView);
                    BarChart barChart = chartView.findViewById(R.id.barChart);
                    ChartComponent.setupDailyBarChart(barChart, dailyCounts, "Controller Medicine Use Per Day", android.graphics.Color.parseColor("#2196F3"));
                });
            }
        });
    }
}

