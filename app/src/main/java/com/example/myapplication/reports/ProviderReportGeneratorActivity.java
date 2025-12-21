package com.example.myapplication.reports;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.ControllerLog;
import com.example.myapplication.ControllerLogModel;
import com.example.myapplication.RescueLog;
import com.example.myapplication.RescueLogModel;
import com.example.myapplication.charts.ChartComponent;
import com.example.myapplication.dailycheckin.CheckInEntry;
import com.example.myapplication.medication.ControllerSchedule;
import com.example.myapplication.safety.PEFReading;
import com.example.myapplication.safety.RescueUsage;
import com.example.myapplication.safety.TriageIncident;
import com.example.myapplication.safety.Zone;
import com.example.myapplication.safety.ZoneCalculator;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.utils.FirebaseKeyEncoder;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProviderReportGeneratorActivity extends AppCompatActivity {
    private static final String TAG = "ProviderReportGenerator";

    private Button buttonStartDate;
    private Button buttonEndDate;
    private Button buttonGeneratePDF;
    private TextView textViewChildName;
    private FrameLayout frameLayoutZoneChart;
    private FrameLayout frameLayoutTrendChart;
    private FrameLayout frameLayoutRescueChart;
    private FrameLayout frameLayoutSymptomsChart;
    private FrameLayout frameLayoutMedicineChart;

    private RecyclerView recyclerViewSharedItems;
    private RecyclerView recyclerViewReportSummary;
    private TextView textViewNoSharing;
    private SharingStatusAdapter sharingStatusAdapter;
    private ReportSummaryAdapter reportSummaryAdapter;

    private String parentId;
    private String childId;
    private String childName;
    private long startDate;
    private long endDate;
    private ProviderReportData reportData;
    private Integer personalBest;
    private List<TriageIncident> triageIncidents;
    private List<CheckInEntry> checkInEntries;
    private com.example.myapplication.providermanaging.Permission childPermissions;
    private Map<String, Integer> rescueDailyCounts;
    private Map<String, Integer> symptomsDailyCounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_provider_report_generator);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (!(UserManager.currentUser instanceof ParentAccount)) {
            Log.e(TAG, "Current user is not a ParentAccount");
            finish();
            return;
        }

        parentId = getIntent().getStringExtra("parentId");
        childId = getIntent().getStringExtra("childId");
        childName = getIntent().getStringExtra("childName");

        if (parentId == null || childId == null) {
            Log.e(TAG, "Missing parentId or childId");
            finish();
            return;
        }

        Calendar cal = Calendar.getInstance();
        endDate = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, -3);
        startDate = cal.getTimeInMillis();

        initializeViews();
        loadReportData();
    }

    private void initializeViews() {
        Button buttonBack = findViewById(R.id.buttonBack);
        buttonStartDate = findViewById(R.id.buttonStartDate);
        buttonEndDate = findViewById(R.id.buttonEndDate);
        buttonGeneratePDF = findViewById(R.id.buttonGeneratePDF);
        textViewChildName = findViewById(R.id.textViewChildName);
        frameLayoutZoneChart = findViewById(R.id.frameLayoutZoneChart);
        frameLayoutTrendChart = findViewById(R.id.frameLayoutTrendChart);
        frameLayoutRescueChart = findViewById(R.id.frameLayoutRescueChart);
        frameLayoutSymptomsChart = findViewById(R.id.frameLayoutSymptomsChart);
        frameLayoutMedicineChart = findViewById(R.id.frameLayoutMedicineChart);

        recyclerViewSharedItems = findViewById(R.id.recyclerViewSharedItems);
        recyclerViewReportSummary = findViewById(R.id.recyclerViewReportSummary);
        textViewNoSharing = findViewById(R.id.textViewNoSharing);

        sharingStatusAdapter = new SharingStatusAdapter(new ArrayList<>());
        recyclerViewSharedItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSharedItems.setAdapter(sharingStatusAdapter);

        reportSummaryAdapter = new ReportSummaryAdapter(new ArrayList<>());
        recyclerViewReportSummary.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewReportSummary.setAdapter(reportSummaryAdapter);

        if (childName != null) {
            textViewChildName.setText("Child: " + childName);
        }

        updateDateButtons();
        loadChildPermissions();

        buttonBack.setOnClickListener(v -> finish());
        buttonStartDate.setOnClickListener(v -> showDatePicker(true));
        buttonEndDate.setOnClickListener(v -> showDatePicker(false));
        buttonGeneratePDF.setOnClickListener(v -> generatePDF());
    }

    private void loadChildPermissions() {
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference childRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId);

        childRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ChildAccount child = task.getResult().getValue(ChildAccount.class);
                if (child != null && child.getPermission() != null) {
                    childPermissions = child.getPermission();
                    updateSharedTag();
                } else {
                    childPermissions = new com.example.myapplication.providermanaging.Permission();
                    updateSharedTag();
                }
            } else {
                childPermissions = new com.example.myapplication.providermanaging.Permission();
                updateSharedTag();
            }
        });
    }

    private void updateSharedTag() {
        if (childPermissions == null) {
            recyclerViewSharedItems.setVisibility(View.GONE);
            textViewNoSharing.setVisibility(View.VISIBLE);
            sharingStatusAdapter.updateItems(new ArrayList<>());
            return;
        }

        List<String> sharedItems = new ArrayList<>();

        if (Boolean.TRUE.equals(childPermissions.getRescueLogs())) {
            sharedItems.add("Rescue logs");
        }
        if (Boolean.TRUE.equals(childPermissions.getControllerAdherenceSummary())) {
            sharedItems.add("Controller adherence summary");
        }
        if (Boolean.TRUE.equals(childPermissions.getSymptoms())) {
            sharedItems.add("Symptoms");
        }
        if (Boolean.TRUE.equals(childPermissions.getTriggers())) {
            sharedItems.add("Triggers");
        }
        if (Boolean.TRUE.equals(childPermissions.getPeakFlow())) {
            sharedItems.add("Peak-flow (PEF)");
        }
        if (Boolean.TRUE.equals(childPermissions.getTriageIncidents())) {
            sharedItems.add("Triage incidents");
        }
        if (Boolean.TRUE.equals(childPermissions.getSummaryCharts())) {
            sharedItems.add("Summary charts");
        }

        if (sharedItems.isEmpty()) {
            recyclerViewSharedItems.setVisibility(View.GONE);
            textViewNoSharing.setVisibility(View.VISIBLE);
            sharingStatusAdapter.updateItems(new ArrayList<>());
        } else {
            textViewNoSharing.setVisibility(View.GONE);
            recyclerViewSharedItems.setVisibility(View.VISIBLE);
            sharingStatusAdapter.updateItems(sharedItems);
        }
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar cal = Calendar.getInstance();
        if (isStartDate) {
            cal.setTimeInMillis(startDate);
        } else {
            cal.setTimeInMillis(endDate);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    cal.set(year, month, dayOfMonth);
                    long selectedDate = cal.getTimeInMillis();
                    if (isStartDate) {
                        if (selectedDate < endDate) {
                            startDate = selectedDate;
                            updateDateButtons();
                            loadReportData();
                        } else {
                            Toast.makeText(this, "Start date must be before end date", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (selectedDate > startDate) {
                            endDate = selectedDate;
                            updateDateButtons();
                            loadReportData();
                        } else {
                            Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void updateDateButtons() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        buttonStartDate.setText("Start: " + sdf.format(new Date(startDate)));
        buttonEndDate.setText("End: " + sdf.format(new Date(endDate)));
    }

    private void loadReportData() {
        reportData = new ProviderReportData();
        reportData.setStartDate(startDate);
        reportData.setEndDate(endDate);
        reportData.setChildName(childName);

        loadRescueFrequency();
        loadAdherence();
        loadSymptomBurden();
        loadZoneDistribution();
        loadPEFTrend();
        loadRescuePerDay();
        loadSymptomsPerDay();
        loadMedicinePerDay();
        loadPersonalBest();
        loadTriageIncidents();
        loadTriggers();
    }

    private void loadRescueFrequency() {
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
                int count = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        RescueUsage usage = child.getValue(RescueUsage.class);
                        if (usage != null) {
                            count += usage.getCount();
                        }
                    }
                }
                final int triageCount = count;

                // Also count direct rescue inhaler usage
                SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String startDateStr = logDateFormat.format(new Date(startDate));
                String endDateStr = logDateFormat.format(new Date(endDate));

                RescueLogModel.readFromDB(childId, startDateStr + "_00:00:00", endDateStr + "_23:59:59", new com.example.myapplication.ResultCallBack<HashMap<String, RescueLog>>() {
                    @Override
                    public void onComplete(HashMap<String, RescueLog> logs) {
                        int directCount = 0;
                        if (logs != null) {
                            directCount = logs.size(); // Each RescueLog entry is 1 dose
                        }
                        reportData.setRescueFrequency(triageCount + directCount);
                        updateUI();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading rescue frequency from triage sessions", error.toException());
                // Still try to load from direct usage
                SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String startDateStr = logDateFormat.format(new Date(startDate));
                String endDateStr = logDateFormat.format(new Date(endDate));

                RescueLogModel.readFromDB(childId, startDateStr + "_00:00:00", endDateStr + "_23:59:59", new com.example.myapplication.ResultCallBack<HashMap<String, RescueLog>>() {
                    @Override
                    public void onComplete(HashMap<String, RescueLog> logs) {
                        int directCount = 0;
                        if (logs != null) {
                            directCount = logs.size();
                        }
                        reportData.setRescueFrequency(directCount);
                        updateUI();
                    }
                });
            }
        });
    }

    private void loadAdherence() {
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference childRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId);

        childRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ChildAccount child = task.getResult().getValue(ChildAccount.class);
                if (child != null && child.getControllerSchedule() != null) {
                    ControllerSchedule schedule = child.getControllerSchedule();
                    AdherenceCalculator.calculateAdherence(childId, schedule, startDate, endDate, new com.example.myapplication.ResultCallBack<Double>() {
                        @Override
                        public void onComplete(Double adherence) {
                            reportData.setControllerAdherence(adherence);
                            updateUI();
                        }
                    });
                } else {
                    reportData.setControllerAdherence(0.0);
                    updateUI();
                }
            }
        });
    }

    private void loadSymptomBurden() {
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
                int problemDays = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        TriageIncident incident = child.getValue(TriageIncident.class);
                        if (incident != null) {
                            problemDays++;
                        }
                    }
                }
                reportData.setSymptomBurdenDays(problemDays);
                updateUI();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading symptom burden", error.toException());
            }
        });
    }

    private void loadZoneDistribution() {
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("pefReadings");

        DatabaseReference childRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId);

        // Use direct listener instead of orderByChild query to avoid index requirements
        // Filter by date range in code after loading
        // Check encoded path first (current standard), then raw path for backward compatibility
        pefRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean foundData = snapshot.exists();
                childRef.child("personalBest").get().addOnCompleteListener(pbTask -> {
                    Integer personalBest = null;
                    if (pbTask.isSuccessful() && pbTask.getResult().getValue() != null) {
                        Object pbValue = pbTask.getResult().getValue();
                        if (pbValue instanceof Long) {
                            personalBest = ((Long) pbValue).intValue();
                        } else if (pbValue instanceof Integer) {
                            personalBest = (Integer) pbValue;
                        }
                    }

                    // Create final copy for use in inner class
                    final Integer finalPersonalBest = personalBest;

                    Map<String, Integer> zoneCounts = new HashMap<>();
                    zoneCounts.put("green", 0);
                    zoneCounts.put("yellow", 0);
                    zoneCounts.put("red", 0);
                    zoneCounts.put("unknown", 0);

                    if (snapshot.exists() && finalPersonalBest != null && finalPersonalBest > 0) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            PEFReading reading = child.getValue(PEFReading.class);
                            if (reading != null && reading.getTimestamp() >= startDate && reading.getTimestamp() <= endDate) {
                                Zone zone = ZoneCalculator.calculateZone(reading.getValue(), finalPersonalBest);
                                String zoneName = com.example.myapplication.charts.ChartComponent.normalizeZoneName(zone.getDisplayName());
                                zoneCounts.put(zoneName, zoneCounts.getOrDefault(zoneName, 0) + 1);
                            }
                        }
                        Log.d(TAG, "Loaded PEF readings for zone distribution from Firebase path: " + pefRef.toString());
                    } else {
                        if (!snapshot.exists()) {
                            Log.d(TAG, "No PEF readings found at Firebase path: " + pefRef.toString());
                        }
                    }
                    
                    // If no data found at encoded path and encoded != raw, check raw path for backward compatibility
                    if (!foundData && !encodedChildId.equals(childId)) {
                        Log.d(TAG, "Checking raw childId path for zone distribution (backward compatibility): " + childId);
                        DatabaseReference rawPefRef = UserManager.mDatabase
                                .child("users")
                                .child(parentId)
                                .child("children")
                                .child(childId)
                                .child("pefReadings");
                        
                        rawPefRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot rawSnapshot) {
                                if (rawSnapshot.exists() && finalPersonalBest != null && finalPersonalBest > 0) {
                                    for (DataSnapshot child : rawSnapshot.getChildren()) {
                                        PEFReading reading = child.getValue(PEFReading.class);
                                        if (reading != null && reading.getTimestamp() >= startDate && reading.getTimestamp() <= endDate) {
                                            Zone zone = ZoneCalculator.calculateZone(reading.getValue(), finalPersonalBest);
                                            String zoneName = com.example.myapplication.charts.ChartComponent.normalizeZoneName(zone.getDisplayName());
                                            zoneCounts.put(zoneName, zoneCounts.getOrDefault(zoneName, 0) + 1);
                                        }
                                    }
                                    Log.d(TAG, "Loaded PEF readings for zone distribution from raw path (backward compatibility): " + rawPefRef.toString());
                                }
                                reportData.setZoneDistribution(zoneCounts);
                                updateUI();
                                updateZoneChart();
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Log.e(TAG, "Error loading zone distribution from raw Firebase path: " + rawPefRef.toString(), error.toException());
                                reportData.setZoneDistribution(zoneCounts);
                                updateUI();
                                updateZoneChart();
                            }
                        });
                    } else {
                        reportData.setZoneDistribution(zoneCounts);
                        updateUI();
                        updateZoneChart();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading zone distribution from Firebase path: " + pefRef.toString(), error.toException());
            }
        });
    }

    private void loadPEFTrend() {
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("pefReadings");

        // Use direct listener instead of orderByChild query to avoid index requirements
        // Filter by date range in code after loading
        // Check encoded path first (current standard), then raw path for backward compatibility
        pefRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<ProviderReportData.PEFDataPoint> dataPoints = new ArrayList<>();
                boolean foundData = false;
                if (snapshot.exists()) {
                    foundData = true;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        PEFReading reading = child.getValue(PEFReading.class);
                        if (reading != null && reading.getTimestamp() >= startDate && reading.getTimestamp() <= endDate) {
                            dataPoints.add(new ProviderReportData.PEFDataPoint(
                                    reading.getTimestamp(),
                                    reading.getValue(),
                                    0.0
                            ));
                        }
                    }
                    Log.d(TAG, "Loaded PEF readings for trend chart from Firebase path: " + pefRef.toString());
                } else {
                    Log.d(TAG, "No PEF readings found at Firebase path: " + pefRef.toString());
                }
                
                // If no data found at encoded path and encoded != raw, check raw path for backward compatibility
                if (!foundData && !encodedChildId.equals(childId)) {
                    Log.d(TAG, "Checking raw childId path for PEF trend (backward compatibility): " + childId);
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
                                for (DataSnapshot child : rawSnapshot.getChildren()) {
                                    PEFReading reading = child.getValue(PEFReading.class);
                                    if (reading != null && reading.getTimestamp() >= startDate && reading.getTimestamp() <= endDate) {
                                        dataPoints.add(new ProviderReportData.PEFDataPoint(
                                                reading.getTimestamp(),
                                                reading.getValue(),
                                                0.0
                                        ));
                                    }
                                }
                                Log.d(TAG, "Loaded PEF readings for trend chart from raw path (backward compatibility): " + rawPefRef.toString());
                            }
                            dataPoints.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                            reportData.setPefTrendData(dataPoints);
                            updateTrendChart();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e(TAG, "Error loading PEF trend from raw Firebase path: " + rawPefRef.toString(), error.toException());
                            dataPoints.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                            reportData.setPefTrendData(dataPoints);
                            updateTrendChart();
                        }
                    });
                } else {
                    dataPoints.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                    reportData.setPefTrendData(dataPoints);
                    updateTrendChart();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading PEF trend from Firebase path: " + pefRef.toString(), error.toException());
            }
        });
    }

    private void updateUI() {
        if (reportData.getRescueFrequency() >= 0 && reportData.getControllerAdherence() >= 0 && reportData.getSymptomBurdenDays() >= 0) {
            List<String> summaryItems = new ArrayList<>();
            summaryItems.add("Rescue Frequency: " + reportData.getRescueFrequency() + " uses");
            summaryItems.add("Controller Adherence: " + String.format(Locale.getDefault(), "%.1f%%", reportData.getControllerAdherence()));
            summaryItems.add("Symptom Burden: " + reportData.getSymptomBurdenDays() + " problem days");
            reportSummaryAdapter.updateItems(summaryItems);
        } else {
            List<String> summaryItems = new ArrayList<>();
            summaryItems.add("Rescue Frequency: Loading...");
            summaryItems.add("Controller Adherence: Loading...");
            summaryItems.add("Symptom Burden: Loading...");
            reportSummaryAdapter.updateItems(summaryItems);
        }
    }

    private void updateZoneChart() {
        if (reportData.getZoneDistribution() == null) {
            return;
        }
        frameLayoutZoneChart.removeAllViews();
        View chartView = ChartComponent.createChartView(this, frameLayoutZoneChart, ChartComponent.ChartType.BAR);
        frameLayoutZoneChart.addView(chartView);
        com.github.mikephil.charting.charts.BarChart barChart = chartView.findViewById(R.id.barChart);
        ChartComponent.setupBarChart(barChart, reportData.getZoneDistribution(), "Zone Distribution");
    }

    private void updateTrendChart() {
        if (reportData.getPefTrendData() == null) {
            return;
        }
        List<ChartComponent.PEFDataPoint> dataPoints = new ArrayList<>();
        for (ProviderReportData.PEFDataPoint point : reportData.getPefTrendData()) {
            dataPoints.add(new ChartComponent.PEFDataPoint(point.getTimestamp(), point.getPefValue()));
        }
        frameLayoutTrendChart.removeAllViews();
        View chartView = ChartComponent.createChartView(this, frameLayoutTrendChart, ChartComponent.ChartType.LINE);
        frameLayoutTrendChart.addView(chartView);
        LineChart lineChart = chartView.findViewById(R.id.lineChart);
        ChartComponent.setupLineChart(lineChart, dataPoints, "PEF Trend");
    }

    private void loadRescuePerDay() {
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
                Map<String, Integer> dailyCounts = new HashMap<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

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

                // Also load from direct rescue inhaler usage (RescueLogManager)
                SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String startDateStr = logDateFormat.format(new Date(startDate));
                String endDateStr = logDateFormat.format(new Date(endDate));

                RescueLogModel.readFromDB(childId, startDateStr + "_00:00:00", endDateStr + "_23:59:59", new com.example.myapplication.ResultCallBack<HashMap<String, RescueLog>>() {
                    @Override
                    public void onComplete(HashMap<String, RescueLog> logs) {
                        if (logs != null) {
                            for (RescueLog log : logs.values()) {
                                String dateKey = dateFormat.format(new Date(log.getTimestamp()));
                                int currentCount = dailyCounts.getOrDefault(dateKey, 0);
                                dailyCounts.put(dateKey, currentCount + 1); // Each RescueLog entry is 1 dose
                            }
                        }

                        updateRescueChart(dailyCounts);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading rescue per day from triage sessions", error.toException());
                // Still try to load from direct usage even if triage data fails
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String startDateStr = logDateFormat.format(new Date(startDate));
                String endDateStr = logDateFormat.format(new Date(endDate));

                RescueLogModel.readFromDB(childId, startDateStr + "_00:00:00", endDateStr + "_23:59:59", new com.example.myapplication.ResultCallBack<HashMap<String, RescueLog>>() {
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

                        updateRescueChart(dailyCounts);
                    }
                });
            }
        });
    }

    private void updateRescueChart(Map<String, Integer> dailyCounts) {
        rescueDailyCounts = dailyCounts;
        frameLayoutRescueChart.removeAllViews();
        View chartView = ChartComponent.createChartView(this, frameLayoutRescueChart, ChartComponent.ChartType.BAR);
        frameLayoutRescueChart.addView(chartView);
        BarChart barChart = chartView.findViewById(R.id.barChart);
        ChartComponent.setupDailyBarChart(barChart, dailyCounts, "Rescue Medicine Use Per Day", Color.parseColor("#F44336"));
    }

    private void loadSymptomsPerDay() {
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
        // Check encoded path first (current standard), then raw path for backward compatibility
        incidentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean foundData = false;
                if (snapshot.exists()) {
                    foundData = true;
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
                
                // If no data found at encoded path and encoded != raw, check raw path for backward compatibility
                if (!foundData && !encodedChildId.equals(childId)) {
                    Log.d(TAG, "Checking raw childId path for symptoms per day (backward compatibility): " + childId);
                    DatabaseReference rawIncidentRef = UserManager.mDatabase
                            .child("users")
                            .child(parentId)
                            .child("children")
                            .child(childId)
                            .child("incidents");
                    
                    rawIncidentRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot rawSnapshot) {
                            if (rawSnapshot.exists()) {
                                for (DataSnapshot child : rawSnapshot.getChildren()) {
                                    TriageIncident incident = child.getValue(TriageIncident.class);
                                    if (incident != null && incident.getTimestamp() >= startDate && incident.getTimestamp() <= endDate) {
                                        String dateKey = dateFormat.format(new Date(incident.getTimestamp()));
                                        dailyCounts.put(dateKey, dailyCounts.getOrDefault(dateKey, 0) + 1);
                                    }
                                }
                                Log.d(TAG, "Loaded incidents from raw path (backward compatibility): " + rawIncidentRef.toString());
                            }
                            
                            // Also load from daily check-ins (CheckInManager)
                            com.example.myapplication.dailycheckin.CheckInModel.readFromDB(childId, startDateStr, endDateStr, new com.example.myapplication.ResultCallBack<HashMap<String, com.example.myapplication.dailycheckin.DailyCheckin>>() {
                                @Override
                                public void onComplete(HashMap<String, com.example.myapplication.dailycheckin.DailyCheckin> checkIns) {
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

                                    updateSymptomsChart(dailyCounts);
                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Log.e(TAG, "Error loading symptoms per day from raw Firebase path: " + rawIncidentRef.toString(), error.toException());
                            // Still try to load from daily check-ins even if triage data fails
                            com.example.myapplication.dailycheckin.CheckInModel.readFromDB(childId, startDateStr, endDateStr, new com.example.myapplication.ResultCallBack<HashMap<String, com.example.myapplication.dailycheckin.DailyCheckin>>() {
                                @Override
                                public void onComplete(HashMap<String, com.example.myapplication.dailycheckin.DailyCheckin> checkIns) {
                                    Map<String, Integer> fallbackCounts = new HashMap<>();
                                    if (checkIns != null) {
                                        for (Map.Entry<String, com.example.myapplication.dailycheckin.DailyCheckin> entry : checkIns.entrySet()) {
                                            String dateKey = entry.getKey();
                                            com.example.myapplication.dailycheckin.DailyCheckin checkIn = entry.getValue();
                                            if (checkIn != null && (checkIn.getCoughWheezeLevel() > 0 || checkIn.getNightWaking() || 
                                                    (checkIn.getActivityLimits() != null && !checkIn.getActivityLimits().isEmpty()))) {
                                                if (dateKey != null && !dateKey.isEmpty()) {
                                                    fallbackCounts.put(dateKey, fallbackCounts.getOrDefault(dateKey, 0) + 1);
                                                }
                                            }
                                        }
                                    }
                                    updateSymptomsChart(fallbackCounts);
                                }
                            });
                        }
                    });
                } else {
                    // Also load from daily check-ins (CheckInManager)
                    com.example.myapplication.dailycheckin.CheckInModel.readFromDB(childId, startDateStr, endDateStr, new com.example.myapplication.ResultCallBack<HashMap<String, com.example.myapplication.dailycheckin.DailyCheckin>>() {
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

                            updateSymptomsChart(dailyCounts);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading symptoms per day from triage sessions at Firebase path: " + incidentRef.toString(), error.toException());
                // Still try to load from daily check-ins even if triage data fails
                com.example.myapplication.dailycheckin.CheckInModel.readFromDB(childId, startDateStr, endDateStr, new com.example.myapplication.ResultCallBack<HashMap<String, com.example.myapplication.dailycheckin.DailyCheckin>>() {
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

                        updateSymptomsChart(dailyCounts);
                    }
                });
            }
        });
    }

    private void updateSymptomsChart(Map<String, Integer> dailyCounts) {
        symptomsDailyCounts = dailyCounts;
        frameLayoutSymptomsChart.removeAllViews();
        View chartView = ChartComponent.createChartView(this, frameLayoutSymptomsChart, ChartComponent.ChartType.BAR);
        frameLayoutSymptomsChart.addView(chartView);
        BarChart barChart = chartView.findViewById(R.id.barChart);
        ChartComponent.setupDailyBarChart(barChart, dailyCounts, "Symptoms Count Per Day", Color.parseColor("#9C27B0"));
    }

    private void loadMedicinePerDay() {
        Map<String, Integer> dailyCounts = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDateStr = logDateFormat.format(new Date(startDate));
        String endDateStr = logDateFormat.format(new Date(endDate));

        // Load from direct controller inhaler usage (ControllerLogManager)
        ControllerLogModel.readFromDB(childId, startDateStr + "_00:00:00", endDateStr + "_23:59:59", new com.example.myapplication.ResultCallBack<HashMap<String, ControllerLog>>() {
            @Override
            public void onComplete(HashMap<String, ControllerLog> logs) {
                if (logs != null) {
                    for (ControllerLog log : logs.values()) {
                        String dateKey = dateFormat.format(new Date(log.getTimestamp()));
                        int currentCount = dailyCounts.getOrDefault(dateKey, 0);
                        dailyCounts.put(dateKey, currentCount + 1); // Each ControllerLog entry is 1 dose
                    }
                }

                updateMedicineChart(dailyCounts);
            }
        });
    }

    private void updateMedicineChart(Map<String, Integer> dailyCounts) {
        frameLayoutMedicineChart.removeAllViews();
        View chartView = ChartComponent.createChartView(this, frameLayoutMedicineChart, ChartComponent.ChartType.BAR);
        frameLayoutMedicineChart.addView(chartView);
        BarChart barChart = chartView.findViewById(R.id.barChart);
        ChartComponent.setupDailyBarChart(barChart, dailyCounts, "Controller Medicine Use Per Day", Color.parseColor("#2196F3"));
    }

    private void loadPersonalBest() {
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference childRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId);

        childRef.child("personalBest").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() != null) {
                Object pbValue = task.getResult().getValue();
                if (pbValue instanceof Long) {
                    personalBest = ((Long) pbValue).intValue();
                } else if (pbValue instanceof Integer) {
                    personalBest = (Integer) pbValue;
                }
            }
        });
    }

    private void loadTriageIncidents() {
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
                triageIncidents = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        TriageIncident incident = child.getValue(TriageIncident.class);
                        if (incident != null) {
                            triageIncidents.add(incident);
                        }
                    }
                }
                reportData.setNotableIncidents(triageIncidents);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading triage incidents", error.toException());
                triageIncidents = new ArrayList<>();
            }
        });
    }

    private void loadTriggers() {
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference checkInRef = UserManager.mDatabase
                .child("CheckInManager")
                .child(encodedChildId);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDateStr = dateFormat.format(new Date(startDate));
        String endDateStr = dateFormat.format(new Date(endDate));

        Query query = checkInRef.orderByKey().startAt(startDateStr).endAt(endDateStr);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                checkInEntries = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        CheckInEntry entry = child.getValue(CheckInEntry.class);
                        if (entry != null && entry.getTriggers() != null && !entry.getTriggers().isEmpty()) {
                            checkInEntries.add(entry);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading triggers", error.toException());
                checkInEntries = new ArrayList<>();
            }
        });
    }

    private BarChart createBarChartForPDF(int width, int height) {
        BarChart barChart = new BarChart(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        barChart.setLayoutParams(params);
        barChart.setBackgroundColor(Color.WHITE);
        
        if (reportData.getZoneDistribution() != null && !reportData.getZoneDistribution().isEmpty()) {
            ChartComponent.setupBarChart(barChart, reportData.getZoneDistribution(), "Zone Distribution");
        }
        
        return barChart;
    }

    private LineChart createLineChartForPDF(int width, int height) {
        LineChart lineChart = new LineChart(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        lineChart.setLayoutParams(params);
        lineChart.setBackgroundColor(Color.WHITE);
        
        if (reportData.getPefTrendData() != null && !reportData.getPefTrendData().isEmpty()) {
            List<ChartComponent.PEFDataPoint> dataPoints = new ArrayList<>();
            for (ProviderReportData.PEFDataPoint point : reportData.getPefTrendData()) {
                dataPoints.add(new ChartComponent.PEFDataPoint(point.getTimestamp(), point.getPefValue()));
            }
            ChartComponent.setupLineChart(lineChart, dataPoints, "PEF Trend");
        }
        
        return lineChart;
    }

    private BarChart createRescueBarChartForPDF(int width, int height) {
        BarChart barChart = new BarChart(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        barChart.setLayoutParams(params);
        barChart.setBackgroundColor(Color.WHITE);
        
        if (rescueDailyCounts != null && !rescueDailyCounts.isEmpty()) {
            ChartComponent.setupDailyBarChart(barChart, rescueDailyCounts, "Rescue Medicine Use Per Day", Color.parseColor("#F44336"));
        }
        
        return barChart;
    }

    private BarChart createSymptomsBarChartForPDF(int width, int height) {
        BarChart barChart = new BarChart(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        barChart.setLayoutParams(params);
        barChart.setBackgroundColor(Color.WHITE);
        
        if (symptomsDailyCounts != null && !symptomsDailyCounts.isEmpty()) {
            ChartComponent.setupDailyBarChart(barChart, symptomsDailyCounts, "Symptoms Count Per Day", Color.parseColor("#9C27B0"));
        }
        
        return barChart;
    }

    private Bitmap getChartBitmap(View chartView, int width, int height) {
        if (chartView == null) {
            return null;
        }
        
        int measuredWidth = width;
        int measuredHeight = height;
        
        FrameLayout container = new FrameLayout(this);
        container.setLayoutParams(new android.view.ViewGroup.LayoutParams(measuredWidth, measuredHeight));
        container.setBackgroundColor(Color.WHITE);
        container.addView(chartView);
        
        container.measure(
                View.MeasureSpec.makeMeasureSpec(measuredWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(measuredHeight, View.MeasureSpec.EXACTLY)
        );
        container.layout(0, 0, measuredWidth, measuredHeight);
        
        if (chartView instanceof com.github.mikephil.charting.charts.Chart) {
            com.github.mikephil.charting.charts.Chart chart = (com.github.mikephil.charting.charts.Chart) chartView;
            chart.notifyDataSetChanged();
            chart.invalidate();
            chart.requestLayout();
        }
        
        Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        
        container.draw(canvas);
        
        return bitmap;
    }

    private static class SharingPreferences {
        boolean includeRescueLogs;
        boolean includeControllerAdherence;
        boolean includeSymptoms;
        boolean includeTriggers;
        boolean includePEF;
        boolean includeTriageIncidents;
        boolean includeSummaryCharts;

        SharingPreferences(boolean rescueLogs, boolean controllerAdherence, boolean symptoms,
                          boolean triggers, boolean pef, boolean triageIncidents, boolean summaryCharts) {
            this.includeRescueLogs = rescueLogs;
            this.includeControllerAdherence = controllerAdherence;
            this.includeSymptoms = symptoms;
            this.includeTriggers = triggers;
            this.includePEF = pef;
            this.includeTriageIncidents = triageIncidents;
            this.includeSummaryCharts = summaryCharts;
        }
    }

    private void generatePDF() {
        try {
            File documentsDir = this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (documentsDir == null) {
                Toast.makeText(this, "Storage not available", Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            
            // Generate parent PDF (all data)
            SharingPreferences parentPrefs = new SharingPreferences(true, true, true, true, true, true, true);
            String parentFilename = childId + "_report_parent_" + timestamp + ".pdf";
            File parentOutputFile = new File(documentsDir, parentFilename);
            generatePDFFile(parentOutputFile, "Parent Report", parentPrefs);

            // Generate provider PDF (filtered by permissions)
            SharingPreferences providerPrefs = new SharingPreferences(
                Boolean.TRUE.equals(childPermissions != null ? childPermissions.getRescueLogs() : false),
                Boolean.TRUE.equals(childPermissions != null ? childPermissions.getControllerAdherenceSummary() : false),
                Boolean.TRUE.equals(childPermissions != null ? childPermissions.getSymptoms() : false),
                Boolean.TRUE.equals(childPermissions != null ? childPermissions.getTriggers() : false),
                Boolean.TRUE.equals(childPermissions != null ? childPermissions.getPeakFlow() : false),
                Boolean.TRUE.equals(childPermissions != null ? childPermissions.getTriageIncidents() : false),
                Boolean.TRUE.equals(childPermissions != null ? childPermissions.getSummaryCharts() : false)
            );
            String providerFilename = childId + "_report_provider_" + timestamp + ".pdf";
            File providerOutputFile = new File(documentsDir, providerFilename);
            generatePDFFile(providerOutputFile, "Provider Report", providerPrefs);

            Toast.makeText(this, "PDFs generated successfully", Toast.LENGTH_LONG).show();

            // Show parent PDF first
            Intent viewIntent = new Intent(this, ProviderReportViewActivity.class);
            viewIntent.putExtra("pdfFilePath", parentOutputFile.getAbsolutePath());
            viewIntent.putExtra("childName", childName);
            viewIntent.putExtra("reportType", "parent");
            viewIntent.putExtra("providerPdfPath", providerOutputFile.getAbsolutePath());
            startActivity(viewIntent);

        } catch (Exception e) {
            Log.e(TAG, "Error generating PDF", e);
            Toast.makeText(this, "Error generating PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void generatePDFFile(File outputFile, String reportTitle, SharingPreferences prefs) throws IOException {
        PdfDocument document = new PdfDocument();
        int pageWidth = 595;
        int pageHeight = 842;
        float margin = 50f;
        float y = margin;
        float lineHeight = 20f;
        float sectionSpacing = 30f;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(24f);
        titlePaint.setFakeBoldText(true);

        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);

        canvas.drawText(reportTitle + ": " + childName, margin, y, titlePaint);
        y += 30;

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        paint.setTextSize(14f);
        canvas.drawText("Period: " + dateFormat.format(new Date(startDate)) + " to " + dateFormat.format(new Date(endDate)), margin, y, paint);
        y += sectionSpacing;

        paint.setTextSize(12f);
        paint.setFakeBoldText(true);
        canvas.drawText("Summary Statistics", margin, y, paint);
        y += lineHeight;
        paint.setFakeBoldText(false);

        if (prefs.includePEF) {
            if (personalBest != null && personalBest > 0) {
                canvas.drawText("Personal Best PEF: " + personalBest + " L/min", margin, y, paint);
                y += lineHeight;
            }

            double avgPEF = calculateAveragePEF();
            if (avgPEF > 0) {
                canvas.drawText("Average PEF: " + String.format(Locale.getDefault(), "%.1f", avgPEF) + " L/min", margin, y, paint);
                y += lineHeight;
                if (personalBest != null && personalBest > 0) {
                    double avgPercentage = (avgPEF / personalBest) * 100;
                    canvas.drawText("Average % of Personal Best: " + String.format(Locale.getDefault(), "%.1f%%", avgPercentage), margin, y, paint);
                    y += lineHeight;
                }
            }
        }

        if (prefs.includeRescueLogs) {
            canvas.drawText("Rescue Frequency: " + reportData.getRescueFrequency() + " uses", margin, y, paint);
            y += lineHeight;
        }

        if (prefs.includeControllerAdherence) {
            canvas.drawText("Controller Adherence: " + String.format(Locale.getDefault(), "%.1f%%", reportData.getControllerAdherence()), margin, y, paint);
            y += lineHeight;
        }

        if (prefs.includeSymptoms) {
            canvas.drawText("Symptom Burden: " + reportData.getSymptomBurdenDays() + " problem days", margin, y, paint);
            y += lineHeight;
        }
        y += sectionSpacing;

        if (prefs.includeSummaryCharts && reportData.getZoneDistribution() != null) {
            paint.setFakeBoldText(true);
            canvas.drawText("Zone Distribution", margin, y, paint);
            y += lineHeight;
            paint.setFakeBoldText(false);
            int totalReadings = 0;
            for (Integer count : reportData.getZoneDistribution().values()) {
                totalReadings += count;
            }
            for (Map.Entry<String, Integer> entry : reportData.getZoneDistribution().entrySet()) {
                String zoneName = entry.getKey();
                int count = entry.getValue();
                double percentage = totalReadings > 0 ? (count * 100.0 / totalReadings) : 0;
                canvas.drawText(zoneName + ": " + count + " (" + String.format(Locale.getDefault(), "%.1f%%", percentage) + ")", margin + 20, y, paint);
                y += lineHeight;
            }
            y += sectionSpacing;
        }

        if (y > pageHeight - 200) {
            document.finishPage(page);
            pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
            page = document.startPage(pageInfo);
            canvas = page.getCanvas();
            y = margin;
        }

        if (prefs.includeSummaryCharts && reportData.getZoneDistribution() != null && !reportData.getZoneDistribution().isEmpty()) {
            if (y > pageHeight - 200) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin;
            }

            paint.setFakeBoldText(true);
            canvas.drawText("Zone Distribution Chart", margin, y, paint);
            y += lineHeight + 10;
            paint.setFakeBoldText(false);
            
            int chartWidth = pageWidth - (int)(margin * 2);
            int chartHeight = 400;
            BarChart barChart = createBarChartForPDF(chartWidth, chartHeight);
            if (barChart != null) {
                Bitmap zoneChartBitmap = getChartBitmap(barChart, chartWidth, chartHeight);
                if (zoneChartBitmap != null) {
                    canvas.drawBitmap(zoneChartBitmap, margin, y, paint);
                    y += zoneChartBitmap.getHeight() + sectionSpacing;
                    zoneChartBitmap.recycle();
                }
            }
        }

        if (y > pageHeight - 200) {
            document.finishPage(page);
            pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
            page = document.startPage(pageInfo);
            canvas = page.getCanvas();
            y = margin;
        }

        if (prefs.includePEF && prefs.includeSummaryCharts && reportData.getPefTrendData() != null && !reportData.getPefTrendData().isEmpty()) {
            if (y > pageHeight - 200) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin;
            }

            paint.setFakeBoldText(true);
            canvas.drawText("PEF Trend Chart", margin, y, paint);
            y += lineHeight + 10;
            paint.setFakeBoldText(false);
            
            int chartWidth = pageWidth - (int)(margin * 2);
            int chartHeight = 400;
            LineChart lineChart = createLineChartForPDF(chartWidth, chartHeight);
            if (lineChart != null) {
                Bitmap trendChartBitmap = getChartBitmap(lineChart, chartWidth, chartHeight);
                if (trendChartBitmap != null) {
                    canvas.drawBitmap(trendChartBitmap, margin, y, paint);
                    y += trendChartBitmap.getHeight() + sectionSpacing;
                    trendChartBitmap.recycle();
                }
            }
        }

        if (prefs.includeRescueLogs && prefs.includeSummaryCharts && rescueDailyCounts != null && !rescueDailyCounts.isEmpty()) {
            if (y > pageHeight - 200) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin;
            }

            paint.setFakeBoldText(true);
            canvas.drawText("Rescue Medicine Use Per Day Chart", margin, y, paint);
            y += lineHeight + 10;
            paint.setFakeBoldText(false);
            
            int chartWidth = pageWidth - (int)(margin * 2);
            int chartHeight = 400;
            BarChart rescueChart = createRescueBarChartForPDF(chartWidth, chartHeight);
            if (rescueChart != null) {
                Bitmap rescueChartBitmap = getChartBitmap(rescueChart, chartWidth, chartHeight);
                if (rescueChartBitmap != null) {
                    canvas.drawBitmap(rescueChartBitmap, margin, y, paint);
                    y += rescueChartBitmap.getHeight() + sectionSpacing;
                    rescueChartBitmap.recycle();
                }
            }
        }

        if (prefs.includeSymptoms && prefs.includeSummaryCharts && symptomsDailyCounts != null && !symptomsDailyCounts.isEmpty()) {
            if (y > pageHeight - 200) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin;
            }

            paint.setFakeBoldText(true);
            canvas.drawText("Symptoms Count Per Day Chart", margin, y, paint);
            y += lineHeight + 10;
            paint.setFakeBoldText(false);
            
            int chartWidth = pageWidth - (int)(margin * 2);
            int chartHeight = 400;
            BarChart symptomsChart = createSymptomsBarChartForPDF(chartWidth, chartHeight);
            if (symptomsChart != null) {
                Bitmap symptomsChartBitmap = getChartBitmap(symptomsChart, chartWidth, chartHeight);
                if (symptomsChartBitmap != null) {
                    canvas.drawBitmap(symptomsChartBitmap, margin, y, paint);
                    y += symptomsChartBitmap.getHeight() + sectionSpacing;
                    symptomsChartBitmap.recycle();
                }
            }
        }

        if (prefs.includeTriageIncidents && triageIncidents != null && !triageIncidents.isEmpty()) {
            if (y > pageHeight - 300) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin;
            }

            paint.setFakeBoldText(true);
            canvas.drawText("Triage Incidents (" + triageIncidents.size() + ")", margin, y, paint);
            y += lineHeight;
            paint.setFakeBoldText(false);

            int incidentsToShow = Math.min(triageIncidents.size(), 10);
            for (int i = 0; i < incidentsToShow; i++) {
                TriageIncident incident = triageIncidents.get(i);
                if (y > pageHeight - 100) {
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = margin;
                }

                SimpleDateFormat incidentDateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                String incidentDate = incidentDateFormat.format(new Date(incident.getTimestamp()));
                canvas.drawText((i + 1) + ". " + incidentDate, margin + 20, y, paint);
                y += lineHeight;
                canvas.drawText("   Decision: " + (incident.getDecisionShown() != null ? incident.getDecisionShown() : "N/A"), margin + 20, y, paint);
                y += lineHeight;
                if (incident.getZone() != null) {
                    canvas.drawText("   Zone: " + incident.getZone().getDisplayName(), margin + 20, y, paint);
                    y += lineHeight;
                }
                if (incident.getPefValue() != null && incident.getPefValue() > 0) {
                    canvas.drawText("   PEF: " + incident.getPefValue() + " L/min", margin + 20, y, paint);
                    y += lineHeight;
                }
                y += 5;
            }
            if (triageIncidents.size() > 10) {
                canvas.drawText("... and " + (triageIncidents.size() - 10) + " more incidents", margin + 20, y, paint);
            }
        }

        if (prefs.includeTriggers && checkInEntries != null && !checkInEntries.isEmpty()) {
            if (y > pageHeight - 300) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin;
            }

            paint.setFakeBoldText(true);
            canvas.drawText("Triggers", margin, y, paint);
            y += lineHeight;
            paint.setFakeBoldText(false);

            Map<String, Integer> triggerCounts = new HashMap<>();
            for (CheckInEntry entry : checkInEntries) {
                if (entry.getTriggers() != null) {
                    for (String trigger : entry.getTriggers()) {
                        triggerCounts.put(trigger, triggerCounts.getOrDefault(trigger, 0) + 1);
                    }
                }
            }

            for (Map.Entry<String, Integer> entry : triggerCounts.entrySet()) {
                if (y > pageHeight - 100) {
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = margin;
                }
                canvas.drawText(entry.getKey() + ": " + entry.getValue() + " occurrences", margin + 20, y, paint);
                y += lineHeight;
            }
        }

        document.finishPage(page);
        document.writeTo(new FileOutputStream(outputFile));
        document.close();
    }

    private double calculateAveragePEF() {
        if (reportData.getPefTrendData() == null || reportData.getPefTrendData().isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (ProviderReportData.PEFDataPoint point : reportData.getPefTrendData()) {
            sum += point.getPefValue();
        }
        return sum / reportData.getPefTrendData().size();
    }

    private class SharingStatusAdapter extends RecyclerView.Adapter<SharingStatusAdapter.ViewHolder> {
        private List<String> sharedItems;

        public SharingStatusAdapter(List<String> sharedItems) {
            this.sharedItems = sharedItems;
        }

        public void updateItems(List<String> newItems) {
            this.sharedItems = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_provider_sharing_status, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position >= 0 && position < sharedItems.size()) {
                holder.textViewSharedItem.setText("• " + sharedItems.get(position));
            }
        }

        @Override
        public int getItemCount() {
            return sharedItems != null ? sharedItems.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewSharedItem;

            ViewHolder(View itemView) {
                super(itemView);
                textViewSharedItem = itemView.findViewById(R.id.textViewSharedItem);
            }
        }
    }

    private class ReportSummaryAdapter extends RecyclerView.Adapter<ReportSummaryAdapter.ViewHolder> {
        private List<String> summaryItems;

        public ReportSummaryAdapter(List<String> summaryItems) {
            this.summaryItems = summaryItems;
        }

        public void updateItems(List<String> newItems) {
            this.summaryItems = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_report_summary, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position >= 0 && position < summaryItems.size()) {
                holder.textViewSummaryItem.setText(summaryItems.get(position));
            }
        }

        @Override
        public int getItemCount() {
            return summaryItems != null ? summaryItems.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewSummaryItem;

            ViewHolder(View itemView) {
                super(itemView);
                textViewSummaryItem = itemView.findViewById(R.id.textViewSummaryItem);
            }
        }
    }
}

