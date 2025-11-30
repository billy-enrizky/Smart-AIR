package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.safety.PEFHistoryActivity;
import com.example.myapplication.safety.PEFReading;
import com.example.myapplication.safety.RescueUsage;
import com.example.myapplication.safety.SetPersonalBestActivity;
import com.example.myapplication.safety.Zone;
import com.example.myapplication.safety.ZoneCalculator;
import com.example.myapplication.safety.ActionPlanActivity;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.reports.DashboardStats;
import com.example.myapplication.reports.ProviderReportGeneratorActivity;
import com.example.myapplication.charts.ChartComponent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.example.myapplication.SignIn.SignInView;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import com.example.myapplication.childmanaging.CreateChildActivity;
import com.example.myapplication.providermanaging.AccessPermissionActivity;
import com.example.myapplication.providermanaging.InvitationCreateActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParentActivity extends AppCompatActivity {
    private static final String TAG = "ParentActivity";
    
    private RecyclerView recyclerViewChildren;
    private RecyclerView recyclerViewDashboardTiles;
    private Button createChildButton;
    private Button button7Days;
    private Button button30Days;
    private FrameLayout frameLayoutTrendChart;
    private TextView textViewNotificationBadge;
    private ParentAccount parentAccount;
    private ChildrenZoneAdapter adapter;
    private DashboardTileAdapter dashboardTileAdapter;
    private List<ChildZoneInfo> childrenZoneInfo;
    private List<DashboardStats> dashboardStatsList;
    private int trendDays = 7;
    private com.google.firebase.database.DatabaseReference triageSessionsRef;
    private com.google.firebase.database.ChildEventListener triageListener;
    private com.google.firebase.database.DatabaseReference notificationsRef;
    private com.google.firebase.database.ValueEventListener notificationsListener;
    private java.util.Map<String, String> lastSeenSessions = new java.util.HashMap<>();
    private java.util.Map<String, String> lastSeenWorseningIds = new java.util.HashMap<>();
    private SharedPreferences dismissedAlertsPrefs;
    private static final String PREFS_NAME = "ParentActivityDismissedAlerts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_parent);
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
        
        parentAccount = (ParentAccount) UserManager.currentUser;
        
        dismissedAlertsPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        recyclerViewChildren = findViewById(R.id.recyclerViewChildren);
        recyclerViewDashboardTiles = findViewById(R.id.recyclerViewDashboardTiles);
        createChildButton = findViewById(R.id.createChildPageButton);
        button7Days = findViewById(R.id.button7Days);
        button30Days = findViewById(R.id.button30Days);
        frameLayoutTrendChart = findViewById(R.id.frameLayoutTrendChart);
        textViewNotificationBadge = findViewById(R.id.textViewNotificationBadge);
        
        childrenZoneInfo = new ArrayList<>();
        adapter = new ChildrenZoneAdapter(childrenZoneInfo);
        recyclerViewChildren.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChildren.setAdapter(adapter);
        
        dashboardStatsList = new ArrayList<>();
        dashboardTileAdapter = new DashboardTileAdapter(dashboardStatsList);
        recyclerViewDashboardTiles.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewDashboardTiles.setAdapter(dashboardTileAdapter);
        
        button7Days.setOnClickListener(v -> {
            trendDays = 7;
            button7Days.setBackgroundColor(android.graphics.Color.parseColor("#FFC107")); // Yellow
            button30Days.setBackgroundColor(android.graphics.Color.GRAY);
            loadTrendChart();
        });
        
        button30Days.setOnClickListener(v -> {
            trendDays = 30;
            button7Days.setBackgroundColor(android.graphics.Color.GRAY);
            button30Days.setBackgroundColor(android.graphics.Color.parseColor("#FFC107")); // Yellow
            loadTrendChart();
        });
        
        // Set initial state: 7 days selected (yellow), 30 days unselected (gray)
        button7Days.setBackgroundColor(android.graphics.Color.parseColor("#FFC107")); // Yellow
        button30Days.setBackgroundColor(android.graphics.Color.GRAY);
        
        Button notificationButton = findViewById(R.id.buttonNotificationButton);
        if (notificationButton != null) {
            notificationButton.setOnClickListener(v -> {
                Intent intent = new Intent(ParentActivity.this, com.example.myapplication.notifications.NotificationActivity.class);
                startActivity(intent);
            });
        }
        
        createChildButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ParentActivity.this, CreateChildActivity.class);
                startActivity(intent);
            }
        });
        
        loadDashboardStats();
        loadChildrenZones();
        loadTrendChart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
        loadChildrenZones();
        loadTrendChart();
        attachTriageListener();
        attachNotificationsListener();
    }

    private void loadChildrenZones() {
        if (parentAccount == null || parentAccount.getChildren() == null) {
            return;
        }
        
        runOnUiThread(() -> {
            childrenZoneInfo.clear();
            adapter.notifyDataSetChanged();
        });
        
        HashMap<String, ChildAccount> children = parentAccount.getChildren();
        
        if (children.isEmpty()) {
            return;
        }
        
        for (Map.Entry<String, ChildAccount> entry : children.entrySet()) {
            ChildAccount child = entry.getValue();
            loadChildZone(child);
        }
    }

    private void loadChildZone(ChildAccount child) {
        String parentId = child.getParent_id();
        String childId = child.getID();
        Integer personalBest = child.getPersonalBest();
        
        if (personalBest == null || personalBest <= 0) {
            ChildZoneInfo info = new ChildZoneInfo(child, Zone.UNKNOWN, 0.0, null, null);
            updateChildZoneInfo(info);
            return;
        }
        
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("pefReadings");
        
        Query latestPEFQuery = pefRef.orderByChild("timestamp").limitToLast(1);
        
        latestPEFQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                PEFReading latestReading = null;
                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        latestReading = childSnapshot.getValue(PEFReading.class);
                        break;
                    }
                }
                
                Zone zone = Zone.UNKNOWN;
                double percentage = 0.0;
                String lastPEFDate = null;
                
                if (latestReading != null) {
                    int pefValue = latestReading.getValue();
                    zone = ZoneCalculator.calculateZone(pefValue, personalBest);
                    percentage = ZoneCalculator.calculatePercentage(pefValue, personalBest);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                    lastPEFDate = sdf.format(new Date(latestReading.getTimestamp()));
                }
                
                ChildZoneInfo info = new ChildZoneInfo(child, zone, percentage, lastPEFDate, latestReading);
                updateChildZoneInfo(info);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading child zone", error.toException());
                ChildZoneInfo info = new ChildZoneInfo(child, Zone.UNKNOWN, 0.0, null, null);
                updateChildZoneInfo(info);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        detachTriageListener();
        detachNotificationsListener();
    }

    private void attachTriageListener() {
        if (parentAccount == null) {
            return;
        }
        if (triageSessionsRef == null) {
            triageSessionsRef = UserManager.mDatabase
                    .child("triageSessions")
                    .child(parentAccount.getID());
        }
        if (triageListener != null) {
            return;
        }

        triageListener = new com.google.firebase.database.ChildEventListener() {
            @Override
            public void onChildAdded(com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {
                handleTriageSnapshot(snapshot);
            }

            @Override
            public void onChildChanged(com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {
                handleTriageSnapshot(snapshot);
            }

            @Override
            public void onChildRemoved(com.google.firebase.database.DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(com.google.firebase.database.DataSnapshot snapshot, String previousChildName) {
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Triage listener cancelled", error.toException());
            }
        };

        triageSessionsRef.addChildEventListener(triageListener);
    }

    private void detachTriageListener() {
        if (triageSessionsRef != null && triageListener != null) {
            triageSessionsRef.removeEventListener(triageListener);
            triageListener = null;
        }
    }

    private void attachNotificationsListener() {
        if (parentAccount == null) {
            return;
        }
        if (notificationsRef == null) {
            notificationsRef = UserManager.mDatabase
                    .child("users")
                    .child(parentAccount.getID())
                    .child("notifications");
        }
        if (notificationsListener != null) {
            return;
        }

        notificationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int unreadCount = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        com.example.myapplication.notifications.NotificationItem notification = child.getValue(com.example.myapplication.notifications.NotificationItem.class);
                        if (notification != null && !notification.isRead()) {
                            unreadCount++;
                        }
                    }
                }
                updateNotificationBadge(unreadCount);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Notifications listener cancelled", error.toException());
            }
        };

        notificationsRef.addValueEventListener(notificationsListener);
    }

    private void detachNotificationsListener() {
        if (notificationsRef != null && notificationsListener != null) {
            notificationsRef.removeEventListener(notificationsListener);
            notificationsListener = null;
        }
    }

    private void updateNotificationBadge(int count) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        runOnUiThread(() -> {
            if (textViewNotificationBadge != null) {
                if (count > 0) {
                    textViewNotificationBadge.setText(String.valueOf(count));
                    textViewNotificationBadge.setVisibility(View.VISIBLE);
                } else {
                    textViewNotificationBadge.setVisibility(View.GONE);
                }
            }
        });
    }

    private void handleTriageSnapshot(com.google.firebase.database.DataSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        String childId = snapshot.getKey();
        if (childId == null) {
            return;
        }

        String childName = snapshot.child("childName").getValue(String.class);
        if (childName == null && parentAccount != null && parentAccount.getChildren() != null) {
            ChildAccount childAccount = parentAccount.getChildren().get(childId);
            if (childAccount != null && childAccount.getName() != null) {
                childName = childAccount.getName();
            }
        }
        final String finalChildName = childName != null ? childName : "Your child";

        // Handle triage start notification (sessionId changes)
        String sessionId = snapshot.child("sessionId").getValue(String.class);
        if (sessionId != null && !sessionId.isEmpty()) {
            String lastSeen = lastSeenSessions.get(childId);
            if (!sessionId.equals(lastSeen)) {
                lastSeenSessions.put(childId, sessionId);
                
                // Check if this alert has already been dismissed
                String alertKey = "triage_start_" + childId + "_" + sessionId;
                boolean isDismissed = dismissedAlertsPrefs.getBoolean(alertKey, false);
                
                if (!isDismissed) {
                    runOnUiThread(() -> {
                        new androidx.appcompat.app.AlertDialog.Builder(ParentActivity.this)
                                .setTitle("Breathing Assessment Started")
                                .setMessage(finalChildName + " started a breathing assessment.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    // Mark this alert as dismissed
                                    dismissedAlertsPrefs.edit().putBoolean(alertKey, true).apply();
                                })
                                .show();

                        android.widget.Toast.makeText(
                                ParentActivity.this,
                                finalChildName + " started a breathing assessment.",
                                android.widget.Toast.LENGTH_SHORT
                        ).show();
                    });
                }
            }
        }

        // Handle worsening notification after 10-minute re-check
        String worseningId = snapshot.child("worseningId").getValue(String.class);
        Boolean worseningHasRedFlag = snapshot.child("worseningHasRedFlag").getValue(Boolean.class);

        if (worseningId != null && !worseningId.isEmpty() && Boolean.TRUE.equals(worseningHasRedFlag)) {
            String lastWorsening = lastSeenWorseningIds.get(childId);
            if (!worseningId.equals(lastWorsening)) {
                lastSeenWorseningIds.put(childId, worseningId);
                
                // Check if this alert has already been dismissed
                String alertKey = "worsening_" + childId + "_" + worseningId;
                boolean isDismissed = dismissedAlertsPrefs.getBoolean(alertKey, false);
                
                if (!isDismissed) {
                    runOnUiThread(() -> {
                        new androidx.appcompat.app.AlertDialog.Builder(ParentActivity.this)
                                .setTitle("Breathing Symptoms Still Present")
                                .setMessage(finalChildName + " still has breathing symptoms after the 10-minute re-check.")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    // Mark this alert as dismissed
                                    dismissedAlertsPrefs.edit().putBoolean(alertKey, true).apply();
                                })
                                .show();

                        android.widget.Toast.makeText(
                                ParentActivity.this,
                                finalChildName + " still has breathing symptoms after the 10-minute re-check.",
                                android.widget.Toast.LENGTH_LONG
                        ).show();
                    });
                }
            }
        }
    }

    private void updateChildZoneInfo(ChildZoneInfo newInfo) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            boolean found = false;
            for (int i = 0; i < childrenZoneInfo.size(); i++) {
                if (childrenZoneInfo.get(i).child.getID().equals(newInfo.child.getID())) {
                    childrenZoneInfo.set(i, newInfo);
                    if (adapter != null) {
                        adapter.notifyItemChanged(i);
                    }
                    found = true;
                    return;
                }
            }
            if (!found) {
                childrenZoneInfo.add(newInfo);
                if (adapter != null) {
                    adapter.notifyItemInserted(childrenZoneInfo.size() - 1);
                }
            }
        });
    }

    public void SignInChildrenProfile(android.view.View view) {
        Intent intent = new Intent(ParentActivity.this, SignInChildProfileActivity.class);
        startActivity(intent);
    }

    public void Signout(android.view.View view) {
        UserManager.currentUser = null;
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, SignInView.class);
        startActivity(intent);
        finish();
    }

    public void CreateInvitation(android.view.View view) {
        Intent intent = new Intent(this, InvitationCreateActivity.class);
        startActivity(intent);
        this.finish();
    }

    public void AccessPermission(android.view.View view) {
        Intent intent = new Intent(this, AccessPermissionActivity.class);
        startActivity(intent);
        this.finish();
    }

    public void OpenActionPlan(android.view.View view) {
        Intent intent = new Intent(this, ActionPlanActivity.class);
        intent.putExtra("parentId", parentAccount.getID());
        startActivity(intent);
    }

    private static class ChildZoneInfo {
        ChildAccount child;
        Zone zone;
        double percentage;
        String lastPEFDate;
        PEFReading latestReading;

        ChildZoneInfo(ChildAccount child, Zone zone, double percentage, String lastPEFDate, PEFReading latestReading) {
            this.child = child;
            this.zone = zone;
            this.percentage = percentage;
            this.lastPEFDate = lastPEFDate;
            this.latestReading = latestReading;
        }
    }

    private class ChildrenZoneAdapter extends RecyclerView.Adapter<ChildrenZoneAdapter.ViewHolder> {
        private final List<ChildZoneInfo> children;

        public ChildrenZoneAdapter(List<ChildZoneInfo> children) {
            this.children = children;
    }

    @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_child_zone, parent, false);
            return new ViewHolder(view);
    }

    @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (position < 0 || position >= children.size()) {
                return;
            }
            ChildZoneInfo info = children.get(position);
            holder.textViewChildName.setText(info.child.getName());
            holder.textViewZoneName.setText(info.zone.getDisplayName());
            holder.textViewZoneName.setTextColor(info.zone.getColorResource());
            
            if (info.zone != Zone.UNKNOWN) {
                holder.textViewZonePercentage.setText(String.format(Locale.getDefault(), "%.1f%% of Personal Best", info.percentage));
                holder.textViewZonePercentage.setVisibility(View.VISIBLE);
            } else {
                holder.textViewZonePercentage.setText("Personal Best not set or no PEF readings");
                holder.textViewZonePercentage.setVisibility(View.VISIBLE);
            }
            
            if (info.lastPEFDate != null) {
                holder.textViewLastPEF.setText("Last PEF: " + info.lastPEFDate);
                holder.textViewLastPEF.setVisibility(View.VISIBLE);
            } else {
                holder.textViewLastPEF.setVisibility(View.GONE);
            }
            
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ParentActivity.this, SetPersonalBestActivity.class);
                intent.putExtra("childId", info.child.getID());
                intent.putExtra("parentId", info.child.getParent_id());
                startActivity(intent);
            });
            
            holder.itemView.setOnLongClickListener(v -> {
                Intent intent = new Intent(ParentActivity.this, PEFHistoryActivity.class);
                intent.putExtra("childId", info.child.getID());
                intent.putExtra("parentId", info.child.getParent_id());
                startActivity(intent);
                return true;
            });
            
            holder.buttonGenerateReport.setOnClickListener(v -> {
                Intent intent = new Intent(ParentActivity.this, ProviderReportGeneratorActivity.class);
                intent.putExtra("parentId", info.child.getParent_id());
                intent.putExtra("childId", info.child.getID());
                intent.putExtra("childName", info.child.getName());
                startActivity(intent);
            });
            
            holder.buttonDeleteChild.setOnClickListener(v -> {
                deleteChild(info.child, position);
            });
        }

    @Override
        public int getItemCount() {
            return children.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewChildName;
            TextView textViewZoneName;
            TextView textViewZonePercentage;
            TextView textViewLastPEF;
            Button buttonGenerateReport;
            Button buttonDeleteChild;

            ViewHolder(View itemView) {
                super(itemView);
                textViewChildName = itemView.findViewById(R.id.textViewChildName);
                textViewZoneName = itemView.findViewById(R.id.textViewZoneName);
                textViewZonePercentage = itemView.findViewById(R.id.textViewZonePercentage);
                textViewLastPEF = itemView.findViewById(R.id.textViewLastPEF);
                buttonGenerateReport = itemView.findViewById(R.id.buttonGenerateReport);
                buttonDeleteChild = itemView.findViewById(R.id.buttonDeleteChild);
    }
        }
    }

    private void deleteChild(ChildAccount child, int position) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Child")
                .setMessage("Are you sure you want to delete " + child.getName() + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    DatabaseReference childRef = UserManager.mDatabase
                            .child("users")
                            .child(child.getParent_id())
                            .child("children")
                            .child(child.getID());
                    
                    childRef.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            childrenZoneInfo.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, childrenZoneInfo.size() - position);
                            android.widget.Toast.makeText(this, "Child deleted successfully", android.widget.Toast.LENGTH_SHORT).show();
                        } else {
                            android.util.Log.e("ParentActivity", "Failed to delete child", task.getException());
                            android.widget.Toast.makeText(this, "Failed to delete child", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadDashboardStats() {
        if (parentAccount == null || parentAccount.getChildren() == null) {
            return;
        }

        dashboardStatsList.clear();
        HashMap<String, ChildAccount> children = parentAccount.getChildren();

        if (children.isEmpty()) {
            runOnUiThread(() -> dashboardTileAdapter.notifyDataSetChanged());
            return;
        }

        for (Map.Entry<String, ChildAccount> entry : children.entrySet()) {
            ChildAccount child = entry.getValue();
            loadDashboardStatsForChild(child);
        }
    }

    private void loadDashboardStatsForChild(ChildAccount child) {
        String parentId = child.getParent_id();
        String childId = child.getID();
        DashboardStats stats = new DashboardStats(childId, child.getName());

        Integer personalBest = child.getPersonalBest();
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

                    loadRescueStats(stats, parentId, childId);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Error loading PEF for dashboard", error.toException());
                    loadRescueStats(stats, parentId, childId);
                }
            });
        } else {
            loadRescueStats(stats, parentId, childId);
        }
    }

    private void loadRescueStats(DashboardStats stats, String parentId, String childId) {
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

                updateDashboardStats(stats);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading rescue stats", error.toException());
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
            boolean found = false;
            for (int i = 0; i < dashboardStatsList.size(); i++) {
                if (dashboardStatsList.get(i).getChildId().equals(newStats.getChildId())) {
                    dashboardStatsList.set(i, newStats);
                    dashboardTileAdapter.notifyItemChanged(i);
                    found = true;
                    return;
                }
            }
            if (!found) {
                dashboardStatsList.add(newStats);
                dashboardTileAdapter.notifyItemInserted(dashboardStatsList.size() - 1);
            }
        });
    }

    private void loadTrendChart() {
        if (parentAccount == null || parentAccount.getChildren() == null || parentAccount.getChildren().isEmpty()) {
            return;
        }

        ChildAccount firstChild = parentAccount.getChildren().values().iterator().next();
        String parentId = firstChild.getParent_id();
        String childId = firstChild.getID();
        Integer personalBest = firstChild.getPersonalBest();

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
                    View chartView = ChartComponent.createChartView(ParentActivity.this, frameLayoutTrendChart, ChartComponent.ChartType.LINE);
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
            if (position < 0 || position >= getItemCount()) {
                return;
            }
            DashboardStats stats = getStatsForPosition(position);

            int tileType = position % 3;
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
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ParentActivity.this, PEFHistoryActivity.class);
                intent.putExtra("childId", stats.getChildId());
                intent.putExtra("parentId", parentAccount.getID());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return statsList.size() * 3;
        }

        private DashboardStats getStatsForPosition(int position) {
            return statsList.get(position / 3);
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
