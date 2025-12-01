package com.example.myapplication.reports;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.charts.ChartComponent;
import com.example.myapplication.medication.ControllerSchedule;
import com.example.myapplication.reports.AdherenceCalculator;
import com.example.myapplication.safety.PEFHistoryActivity;
import com.example.myapplication.safety.PEFReading;
import com.example.myapplication.safety.RescueUsage;
import com.example.myapplication.safety.Zone;
import com.example.myapplication.safety.ZoneCalculator;
import com.example.myapplication.userdata.ChildAccount;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChildDashboardActivity extends AppCompatActivity {
    private static final String TAG = "ChildDashboardActivity";

    private RecyclerView recyclerViewDashboardTiles;
    private Button button7Days;
    private Button button30Days;
    private FrameLayout frameLayoutTrendChart;
    private TextView textViewChildName;

    private DashboardTileAdapter dashboardTileAdapter;
    private List<DashboardStats> dashboardStatsList;
    private int trendDays = 7;

    private String parentId;
    private String childId;
    private String childName;
    private ChildAccount childAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_child_dashboard);
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
        recyclerViewDashboardTiles = findViewById(R.id.recyclerViewDashboardTiles);
        button7Days = findViewById(R.id.button7Days);
        button30Days = findViewById(R.id.button30Days);
        frameLayoutTrendChart = findViewById(R.id.frameLayoutTrendChart);
        textViewChildName = findViewById(R.id.textViewChildName);

        if (childName != null) {
            textViewChildName.setText(childName + "'s Dashboard");
        }

        dashboardStatsList = new ArrayList<>();
        dashboardTileAdapter = new DashboardTileAdapter(dashboardStatsList);
        recyclerViewDashboardTiles.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewDashboardTiles.setAdapter(dashboardTileAdapter);

        button7Days.setOnClickListener(v -> {
            trendDays = 7;
            button7Days.setBackgroundColor(android.graphics.Color.parseColor("#FFC107"));
            button30Days.setBackgroundColor(android.graphics.Color.GRAY);
            loadTrendChart();
        });

        button30Days.setOnClickListener(v -> {
            trendDays = 30;
            button7Days.setBackgroundColor(android.graphics.Color.GRAY);
            button30Days.setBackgroundColor(android.graphics.Color.parseColor("#FFC107"));
            loadTrendChart();
        });

        button7Days.setBackgroundColor(android.graphics.Color.parseColor("#FFC107"));
        button30Days.setBackgroundColor(android.graphics.Color.GRAY);

        buttonBack.setOnClickListener(v -> finish());
    }

    private void loadChildAccount() {
        DatabaseReference childRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId);

        childRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                childAccount = task.getResult().getValue(ChildAccount.class);
                if (childAccount != null) {
                    loadDashboardStats();
                    loadTrendChart();
                }
            } else {
                Log.e(TAG, "Error loading child account", task.getException());
            }
        });
    }

    private void loadDashboardStats() {
        if (childAccount == null) {
            return;
        }

        dashboardStatsList.clear();
        DashboardStats stats = new DashboardStats(childId, childAccount.getName());

        Integer personalBest = childAccount.getPersonalBest();
        if (personalBest != null && personalBest > 0) {
            DatabaseReference pefRef = UserManager.mDatabase
                    .child("users")
                    .child(parentId)
                    .child("children")
                    .child(childId)
                    .child("pefReadings");

            Query todayPEFQuery = pefRef.orderByChild("timestamp").limitToLast(1);
            todayPEFQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    PEFReading latestReading = null;
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            latestReading = childSnapshot.getValue(PEFReading.class);
                            break;
                        }
                    }

                    if (latestReading != null) {
                        long todayStart = getTodayStartTimestamp();
                        if (latestReading.getTimestamp() >= todayStart) {
                            int pefValue = latestReading.getValue();
                            Zone zone = ZoneCalculator.calculateZone(pefValue, personalBest);
                            stats.setTodayZone(zone);
                        }
                    }

                    loadRescueStats(stats);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Error loading PEF for dashboard", error.toException());
                    loadRescueStats(stats);
                }
            });
        } else {
            loadRescueStats(stats);
        }
    }

    private void loadRescueStats(DashboardStats stats) {
        DatabaseReference rescueRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("rescueUsage");

        long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        Query rescueQuery = rescueRef.orderByChild("timestamp").startAt(sevenDaysAgo);

        rescueQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                long lastRescueTime = 0;
                int weeklyCount = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        RescueUsage usage = childSnapshot.getValue(RescueUsage.class);
                        if (usage != null) {
                            long timestamp = usage.getTimestamp();
                            if (timestamp > lastRescueTime) {
                                lastRescueTime = timestamp;
                            }
                            weeklyCount += usage.getCount();
                        }
                    }
                }

                if (lastRescueTime > 0) {
                    stats.setLastRescueTime(lastRescueTime);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                    stats.setLastRescueTimeFormatted(sdf.format(new Date(lastRescueTime)));
                } else {
                    stats.setLastRescueTimeFormatted("Never");
                }

                stats.setWeeklyRescueCount(weeklyCount);

                long todayStart = getTodayStartTimestamp();
                int dailyCount = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        RescueUsage usage = childSnapshot.getValue(RescueUsage.class);
                        if (usage != null && usage.getTimestamp() >= todayStart) {
                            dailyCount += usage.getCount();
                        }
                    }
                }
                stats.setDailyRescueCount(dailyCount);

                loadDailySymptomsCount(stats);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading rescue stats", error.toException());
                loadDailySymptomsCount(stats);
            }
        });
    }

    private void loadDailySymptomsCount(DashboardStats stats) {
        DatabaseReference incidentRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("incidents");

        long todayStart = getTodayStartTimestamp();
        Query incidentQuery = incidentRef.orderByChild("timestamp").startAt(todayStart);

        incidentQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int dailyCount = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        com.example.myapplication.safety.TriageIncident incident = childSnapshot.getValue(com.example.myapplication.safety.TriageIncident.class);
                        if (incident != null && incident.getTimestamp() >= todayStart) {
                            dailyCount++;
                        }
                    }
                }
                stats.setDailySymptomsCount(dailyCount);
                loadControllerAdherence(stats);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading daily symptoms count", error.toException());
                loadControllerAdherence(stats);
            }
        });
    }

    private void loadControllerAdherence(DashboardStats stats) {
        if (childAccount == null) {
            updateDashboardStats(stats);
            return;
        }

        ControllerSchedule schedule = childAccount.getControllerSchedule();
        if (schedule == null || schedule.isEmpty()) {
            stats.setControllerAdherence(0.0);
            updateDashboardStats(stats);
            return;
        }

        // Calculate adherence for the last 30 days
        long endDate = System.currentTimeMillis();
        long startDate = endDate - (30L * 24 * 60 * 60 * 1000);

        AdherenceCalculator.calculateAdherence(childId, schedule, startDate, endDate, new com.example.myapplication.ResultCallBack<Double>() {
            @Override
            public void onComplete(Double adherence) {
                stats.setControllerAdherence(adherence != null ? adherence : 0.0);
                updateDashboardStats(stats);
            }
        });
    }

    private void updateDashboardStats(DashboardStats newStats) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            dashboardStatsList.clear();
            dashboardStatsList.add(newStats);
            dashboardTileAdapter.notifyDataSetChanged();
        });
    }

    private void loadTrendChart() {
        if (childAccount == null) {
            return;
        }

        Integer personalBest = childAccount.getPersonalBest();
        if (personalBest == null || personalBest <= 0) {
            return;
        }

        long daysAgo = System.currentTimeMillis() - (trendDays * 24 * 60 * 60 * 1000L);
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("pefReadings");

        Query pefQuery = pefRef.orderByChild("timestamp").startAt(daysAgo);
        pefQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<ChartComponent.PEFDataPoint> dataPoints = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        PEFReading reading = childSnapshot.getValue(PEFReading.class);
                        if (reading != null) {
                            dataPoints.add(new ChartComponent.PEFDataPoint(reading.getTimestamp(), reading.getValue()));
                        }
                    }
                }

                dataPoints.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

                runOnUiThread(() -> {
                    frameLayoutTrendChart.removeAllViews();
                    View chartView = ChartComponent.createChartView(ChildDashboardActivity.this, frameLayoutTrendChart, ChartComponent.ChartType.LINE);
                    frameLayoutTrendChart.addView(chartView);
                    com.github.mikephil.charting.charts.LineChart lineChart = chartView.findViewById(R.id.lineChart);
                    ChartComponent.setupLineChart(lineChart, dataPoints, "PEF Trend");
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading trend chart", error.toException());
            }
        });
    }

    private long getTodayStartTimestamp() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (childAccount != null) {
            loadDashboardStats();
            loadTrendChart();
        }
    }

    private class DashboardTileAdapter extends RecyclerView.Adapter<DashboardTileAdapter.ViewHolder> {
        private final List<DashboardStats> statsList;

        public DashboardTileAdapter(List<DashboardStats> statsList) {
            this.statsList = statsList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dashboard_tile, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (position < 0 || position >= getItemCount() || statsList.isEmpty()) {
                return;
            }
            DashboardStats stats = statsList.get(0);

            int tileType = position;
            String childName = stats.getChildName();

            switch (tileType) {
                case 0:
                    holder.textViewTileTitle.setText(childName + " - Today's Zone");
                    if (stats.getTodayZone() != Zone.UNKNOWN) {
                        holder.textViewTileValue.setText(stats.getTodayZone().getDisplayName());
                        holder.textViewTileValue.setTextColor(stats.getTodayZone().getColorResource());
                        holder.textViewTileSubtitle.setText("Current status");
                    } else {
                        holder.textViewTileValue.setText("Unknown");
                        holder.textViewTileValue.setTextColor(android.graphics.Color.GRAY);
                        holder.textViewTileSubtitle.setText("No PEF reading today");
                    }
                    break;
                case 1:
                    holder.textViewTileTitle.setText(childName + " - Last Rescue");
                    holder.textViewTileValue.setText(stats.getLastRescueTimeFormatted());
                    holder.textViewTileValue.setTextColor(android.graphics.Color.parseColor("#FF9800"));
                    holder.textViewTileSubtitle.setText("Most recent use");
                    break;
                case 2:
                    holder.textViewTileTitle.setText(childName + " - Weekly Count");
                    holder.textViewTileValue.setText(String.valueOf(stats.getWeeklyRescueCount()));
                    holder.textViewTileValue.setTextColor(android.graphics.Color.parseColor("#F44336"));
                    holder.textViewTileSubtitle.setText("Last 7 days");
                    break;
                case 3:
                    holder.textViewTileTitle.setText(childName + " - Daily Rescue");
                    holder.textViewTileValue.setText(String.valueOf(stats.getDailyRescueCount()));
                    holder.textViewTileValue.setTextColor(android.graphics.Color.parseColor("#FF5722"));
                    holder.textViewTileSubtitle.setText("Today");
                    break;
                case 4:
                    holder.textViewTileTitle.setText(childName + " - Daily Symptoms");
                    holder.textViewTileValue.setText(String.valueOf(stats.getDailySymptomsCount()));
                    holder.textViewTileValue.setTextColor(android.graphics.Color.parseColor("#9C27B0"));
                    holder.textViewTileSubtitle.setText("Today");
                    break;
                case 5:
                    holder.textViewTileTitle.setText(childName + " - Controller Adherence");
                    holder.textViewTileValue.setText(String.format(Locale.getDefault(), "%.1f%%", stats.getControllerAdherence()));
                    // Color code: green for >=80%, yellow for 50-79%, red for <50%
                    if (stats.getControllerAdherence() >= 80.0) {
                        holder.textViewTileValue.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                    } else if (stats.getControllerAdherence() >= 50.0) {
                        holder.textViewTileValue.setTextColor(android.graphics.Color.parseColor("#FFC107"));
                    } else {
                        holder.textViewTileValue.setTextColor(android.graphics.Color.parseColor("#F44336"));
                    }
                    holder.textViewTileSubtitle.setText("Last 30 days");
                    break;
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ChildDashboardActivity.this, PEFHistoryActivity.class);
                intent.putExtra("childId", stats.getChildId());
                intent.putExtra("parentId", parentId);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return statsList.isEmpty() ? 0 : 6;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewTileTitle;
            TextView textViewTileValue;
            TextView textViewTileSubtitle;

            ViewHolder(View itemView) {
                super(itemView);
                textViewTileTitle = itemView.findViewById(R.id.textViewTileTitle);
                textViewTileValue = itemView.findViewById(R.id.textViewTileValue);
                textViewTileSubtitle = itemView.findViewById(R.id.textViewTileSubtitle);
            }
        }
    }
}

