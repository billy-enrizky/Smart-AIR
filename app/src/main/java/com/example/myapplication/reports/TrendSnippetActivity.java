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

        Query query = pefRef.orderByChild("timestamp").startAt(startDate).endAt(endDate);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
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
                        if (reading != null) {
                            Zone zone = ZoneCalculator.calculateZone(reading.getValue(), personalBest);
                            String zoneName = ChartComponent.normalizeZoneName(zone.getDisplayName());
                            zoneCounts.put(zoneName, zoneCounts.getOrDefault(zoneName, 0) + 1);
                        }
                    }
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
                Log.e(TAG, "Error loading zone distribution", error.toException());
            }
        });
    }

    private void loadPEFTrend() {
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

        Query query = pefRef.orderByChild("timestamp").startAt(startDate).endAt(endDate);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<ChartComponent.PEFDataPoint> dataPoints = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        PEFReading reading = child.getValue(PEFReading.class);
                        if (reading != null) {
                            dataPoints.add(new ChartComponent.PEFDataPoint(reading.getTimestamp(), reading.getValue()));
                        }
                    }
                }

                dataPoints.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

                runOnUiThread(() -> {
                    frameLayoutTrendChart.removeAllViews();
                    View chartView = ChartComponent.createChartView(TrendSnippetActivity.this, frameLayoutTrendChart, ChartComponent.ChartType.LINE);
                    frameLayoutTrendChart.addView(chartView);
                    LineChart lineChart = chartView.findViewById(R.id.lineChart);
                    ChartComponent.setupLineChart(lineChart, dataPoints, "PEF Trend");
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading PEF trend", error.toException());
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

        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference incidentRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("incidents");

        Query query = incidentRef.orderByChild("timestamp").startAt(startDate).endAt(endDate);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, Integer> dailyCounts = new HashMap<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        TriageIncident incident = child.getValue(TriageIncident.class);
                        if (incident != null) {
                            String dateKey = dateFormat.format(new Date(incident.getTimestamp()));
                            dailyCounts.put(dateKey, dailyCounts.getOrDefault(dateKey, 0) + 1);
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

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading symptoms per day", error.toException());
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

