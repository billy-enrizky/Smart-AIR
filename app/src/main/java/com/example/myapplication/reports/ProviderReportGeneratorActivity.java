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
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(20f);

            float y = 30f;
            canvas.drawText("Provider Report: " + childName, 50, y, paint);
            y += 30;

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            canvas.drawText("Period: " + dateFormat.format(new Date(startDate)) + " to " + dateFormat.format(new Date(endDate)), 50, y, paint);
            y += 40;

            paint.setTextSize(14f);
            canvas.drawText("Rescue Frequency: " + reportData.getRescueFrequency() + " uses", 50, y, paint);
            y += 20;
            canvas.drawText("Controller Adherence: " + String.format(Locale.getDefault(), "%.1f%%", reportData.getControllerAdherence()), 50, y, paint);
            y += 20;
            canvas.drawText("Symptom Burden: " + reportData.getSymptomBurdenDays() + " problem days", 50, y, paint);
            y += 30;

            if (reportData.getZoneDistribution() != null) {
                canvas.drawText("Zone Distribution:", 50, y, paint);
                y += 20;
                for (Map.Entry<String, Integer> entry : reportData.getZoneDistribution().entrySet()) {
                    canvas.drawText(entry.getKey() + ": " + entry.getValue(), 70, y, paint);
                    y += 20;
                }
            }

            document.finishPage(page);
            document.writeTo(new FileOutputStream(outputFile));
            document.close();

            Toast.makeText(this, "PDF generated: " + filename, Toast.LENGTH_LONG).show();

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(outputFile));
            startActivity(Intent.createChooser(shareIntent, "Share PDF Report"));

        } catch (IOException e) {
            Log.e(TAG, "Error generating PDF", e);
            Toast.makeText(this, "Error generating PDF", Toast.LENGTH_SHORT).show();
        }
    }
}

