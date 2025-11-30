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
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.charts.ChartComponent;
import com.example.myapplication.medication.ControllerSchedule;
import com.example.myapplication.reports.AdherenceCalculator;
import com.example.myapplication.safety.PEFReading;
import com.example.myapplication.safety.RescueUsage;
import com.example.myapplication.safety.TriageIncident;
import com.example.myapplication.safety.Zone;
import com.example.myapplication.safety.ZoneCalculator;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
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
    private TextView textViewRescueFrequency;
    private TextView textViewAdherence;
    private TextView textViewSymptomBurden;
    private FrameLayout frameLayoutZoneChart;
    private FrameLayout frameLayoutTrendChart;

    private String parentId;
    private String childId;
    private String childName;
    private long startDate;
    private long endDate;
    private ProviderReportData reportData;
    private Integer personalBest;
    private List<TriageIncident> triageIncidents;

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
        buttonStartDate = findViewById(R.id.buttonStartDate);
        buttonEndDate = findViewById(R.id.buttonEndDate);
        buttonGeneratePDF = findViewById(R.id.buttonGeneratePDF);
        textViewChildName = findViewById(R.id.textViewChildName);
        textViewRescueFrequency = findViewById(R.id.textViewRescueFrequency);
        textViewAdherence = findViewById(R.id.textViewAdherence);
        textViewSymptomBurden = findViewById(R.id.textViewSymptomBurden);
        frameLayoutZoneChart = findViewById(R.id.frameLayoutZoneChart);
        frameLayoutTrendChart = findViewById(R.id.frameLayoutTrendChart);

        if (childName != null) {
            textViewChildName.setText("Child: " + childName);
        }

        updateDateButtons();

        buttonStartDate.setOnClickListener(v -> showDatePicker(true));
        buttonEndDate.setOnClickListener(v -> showDatePicker(false));
        buttonGeneratePDF.setOnClickListener(v -> generatePDF());
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
        loadPersonalBest();
        loadTriageIncidents();
    }

    private void loadRescueFrequency() {
        DatabaseReference rescueRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
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
                reportData.setRescueFrequency(count);
                updateUI();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading rescue frequency", error.toException());
            }
        });
    }

    private void loadAdherence() {
        DatabaseReference childRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId);

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
        DatabaseReference incidentRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
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
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("pefReadings");

        Query query = pefRef.orderByChild("timestamp").startAt(startDate).endAt(endDate);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                DatabaseReference childRef = UserManager.mDatabase
                        .child("users")
                        .child(parentId)
                        .child("children")
                        .child(childId);

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

                    Map<String, Integer> zoneCounts = new HashMap<>();
                    zoneCounts.put("Green", 0);
                    zoneCounts.put("Yellow", 0);
                    zoneCounts.put("Red", 0);
                    zoneCounts.put("Unknown", 0);

                    if (snapshot.exists() && personalBest != null && personalBest > 0) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            PEFReading reading = child.getValue(PEFReading.class);
                            if (reading != null) {
                                Zone zone = ZoneCalculator.calculateZone(reading.getValue(), personalBest);
                                String zoneName = zone.getDisplayName();
                                zoneCounts.put(zoneName, zoneCounts.getOrDefault(zoneName, 0) + 1);
                            }
                        }
                    }

                    reportData.setZoneDistribution(zoneCounts);
                    updateUI();
                    updateZoneChart();
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading zone distribution", error.toException());
            }
        });
    }

    private void loadPEFTrend() {
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("pefReadings");

        Query query = pefRef.orderByChild("timestamp").startAt(startDate).endAt(endDate);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<ProviderReportData.PEFDataPoint> dataPoints = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        PEFReading reading = child.getValue(PEFReading.class);
                        if (reading != null) {
                            dataPoints.add(new ProviderReportData.PEFDataPoint(
                                    reading.getTimestamp(),
                                    reading.getValue(),
                                    0.0
                            ));
                        }
                    }
                }
                dataPoints.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                reportData.setPefTrendData(dataPoints);
                updateTrendChart();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading PEF trend", error.toException());
            }
        });
    }

    private void updateUI() {
        if (reportData.getRescueFrequency() >= 0 && reportData.getControllerAdherence() >= 0 && reportData.getSymptomBurdenDays() >= 0) {
            textViewRescueFrequency.setText("Rescue Frequency: " + reportData.getRescueFrequency() + " uses");
            textViewAdherence.setText("Controller Adherence: " + String.format(Locale.getDefault(), "%.1f%%", reportData.getControllerAdherence()));
            textViewSymptomBurden.setText("Symptom Burden: " + reportData.getSymptomBurdenDays() + " problem days");
        }
    }

    private void updateZoneChart() {
        if (reportData.getZoneDistribution() == null) {
            return;
        }
        frameLayoutZoneChart.removeAllViews();
        View chartView = ChartComponent.createChartView(this, frameLayoutZoneChart, ChartComponent.ChartType.BAR);
        frameLayoutZoneChart.addView(chartView);
        BarChart barChart = chartView.findViewById(R.id.barChart);
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

    private void loadPersonalBest() {
        DatabaseReference childRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId);

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
        DatabaseReference incidentRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
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

    private BarChart createBarChartForPDF(int width, int height) {
        BarChart barChart = new BarChart(this);
        barChart.setLayoutParams(new android.view.ViewGroup.LayoutParams(width, height));
        barChart.setBackgroundColor(Color.WHITE);
        
        if (reportData.getZoneDistribution() != null && !reportData.getZoneDistribution().isEmpty()) {
            ChartComponent.setupBarChart(barChart, reportData.getZoneDistribution(), "Zone Distribution");
        }
        
        return barChart;
    }

    private LineChart createLineChartForPDF(int width, int height) {
        LineChart lineChart = new LineChart(this);
        lineChart.setLayoutParams(new android.view.ViewGroup.LayoutParams(width, height));
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

    private Bitmap getChartBitmap(View chartView, int width, int height) {
        if (chartView == null) {
            return null;
        }
        
        int measuredWidth = width;
        int measuredHeight = height;
        
        chartView.setBackgroundColor(Color.WHITE);
        
        chartView.measure(
                View.MeasureSpec.makeMeasureSpec(measuredWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(measuredHeight, View.MeasureSpec.EXACTLY)
        );
        chartView.layout(0, 0, measuredWidth, measuredHeight);
        
        if (chartView instanceof com.github.mikephil.charting.charts.Chart) {
            com.github.mikephil.charting.charts.Chart chart = (com.github.mikephil.charting.charts.Chart) chartView;
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
        
        Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        
        chartView.draw(canvas);
        
        return bitmap;
    }

    private void generatePDF() {
        try {
            File documentsDir = this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (documentsDir == null) {
                Toast.makeText(this, "Storage not available", Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String filename = childId + "_report_" + sdf.format(new Date()) + ".pdf";
            File outputFile = new File(documentsDir, filename);

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

            canvas.drawText("Provider Report: " + childName, margin, y, titlePaint);
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

            canvas.drawText("Rescue Frequency: " + reportData.getRescueFrequency() + " uses", margin, y, paint);
            y += lineHeight;
            canvas.drawText("Controller Adherence: " + String.format(Locale.getDefault(), "%.1f%%", reportData.getControllerAdherence()), margin, y, paint);
            y += lineHeight;
            canvas.drawText("Symptom Burden: " + reportData.getSymptomBurdenDays() + " problem days", margin, y, paint);
            y += sectionSpacing;

            if (reportData.getZoneDistribution() != null) {
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

            if (reportData.getZoneDistribution() != null && !reportData.getZoneDistribution().isEmpty()) {
                paint.setFakeBoldText(true);
                canvas.drawText("Zone Distribution Chart", margin, y, paint);
                y += lineHeight + 10;
                paint.setFakeBoldText(false);
                
                BarChart barChart = createBarChartForPDF(pageWidth - (int)(margin * 2), 300);
                if (barChart != null) {
                    Bitmap zoneChartBitmap = getChartBitmap(barChart, pageWidth - (int)(margin * 2), 300);
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

            if (reportData.getPefTrendData() != null && !reportData.getPefTrendData().isEmpty()) {
                paint.setFakeBoldText(true);
                canvas.drawText("PEF Trend Chart", margin, y, paint);
                y += lineHeight + 10;
                paint.setFakeBoldText(false);
                
                LineChart lineChart = createLineChartForPDF(pageWidth - (int)(margin * 2), 300);
                if (lineChart != null) {
                    Bitmap trendChartBitmap = getChartBitmap(lineChart, pageWidth - (int)(margin * 2), 300);
                    if (trendChartBitmap != null) {
                        canvas.drawBitmap(trendChartBitmap, margin, y, paint);
                        y += trendChartBitmap.getHeight() + sectionSpacing;
                        trendChartBitmap.recycle();
                    }
                }
            }

            if (triageIncidents != null && !triageIncidents.isEmpty()) {
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

            document.finishPage(page);
            document.writeTo(new FileOutputStream(outputFile));
            document.close();

            Toast.makeText(this, "PDF generated: " + filename, Toast.LENGTH_LONG).show();

            android.net.Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    outputFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share PDF Report"));

        } catch (IOException e) {
            Log.e(TAG, "Error generating PDF", e);
            Toast.makeText(this, "Error generating PDF", Toast.LENGTH_SHORT).show();
        }
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
}

