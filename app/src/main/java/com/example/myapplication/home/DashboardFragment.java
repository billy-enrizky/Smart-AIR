package com.example.myapplication.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.reports.TrendSnippetActivity;
import com.example.myapplication.safety.PEFReading;
import com.example.myapplication.safety.Zone;
import com.example.myapplication.safety.ZoneCalculator;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";
    
    private RecyclerView recyclerViewChildren;
    private ParentAccount parentAccount;
    private DashboardAdapter adapter;
    private List<ChildZoneInfo> childrenZoneInfo;
    
    private Map<String, Query> childPEFQueries = new HashMap<>();
    private Map<String, ValueEventListener> childPEFListeners = new HashMap<>();
    private Map<String, DatabaseReference> childAccountRefs = new HashMap<>();
    private Map<String, ValueEventListener> childAccountListeners = new HashMap<>();
    private Map<String, ChildAccount> latestChildAccounts = new HashMap<>();
    
    private TextView textViewNotificationBadge;
    private DatabaseReference notificationsRef;
    private ChildEventListener notificationsListener;
    private ValueEventListener notificationsCountListener;
    private Set<String> seenNotificationIds = new HashSet<>();
    private SharedPreferences dismissedAlertsPrefs;
    private static final String PREFS_NAME = "DashboardFragmentDismissedAlerts";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        
        if (!(UserManager.currentUser instanceof ParentAccount)) {
            Log.e(TAG, "Current user is not a ParentAccount");
            return view;
        }
        
        parentAccount = (ParentAccount) UserManager.currentUser;
        
        dismissedAlertsPrefs = getContext().getSharedPreferences(PREFS_NAME, 0);
        
        textViewNotificationBadge = view.findViewById(R.id.textViewNotificationBadge);
        
        Button buttonSignOut = view.findViewById(R.id.buttonSignOut);
        if (buttonSignOut != null) {
            buttonSignOut.setOnClickListener(v -> {
                UserManager.currentUser = null;
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), com.example.myapplication.SignIn.SignInView.class);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        }
        
        Button notificationButton = view.findViewById(R.id.buttonNotificationButton);
        if (notificationButton != null) {
            notificationButton.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), com.example.myapplication.notifications.NotificationActivity.class);
                startActivity(intent);
            });
        }
        
        recyclerViewChildren = view.findViewById(R.id.recyclerViewChildren);
        childrenZoneInfo = new ArrayList<>();
        adapter = new DashboardAdapter(childrenZoneInfo);
        recyclerViewChildren.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewChildren.setAdapter(adapter);
        
        attachChildrenZoneListeners();
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        attachChildrenZoneListeners();
        loadExistingNotificationIds();
        attachNotificationsListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        detachChildrenZoneListeners();
        detachNotificationsListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detachChildrenZoneListeners();
        detachNotificationsListener();
    }
    
    private void loadExistingNotificationIds() {
        if (parentAccount == null) {
            return;
        }
        if (notificationsRef == null) {
            notificationsRef = UserManager.mDatabase
                    .child("users")
                    .child(parentAccount.getID())
                    .child("notifications");
        }
        
        notificationsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                seenNotificationIds.clear();
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        if (child.getKey() != null) {
                            seenNotificationIds.add(child.getKey());
                        }
                    }
                }
            }
        });
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
        
        if (notificationsListener == null) {
            notificationsListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    handleNewNotification(snapshot);
                    updateNotificationBadgeCount();
                }

                @Override
                public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                    updateNotificationBadgeCount();
                }

                @Override
                public void onChildRemoved(DataSnapshot snapshot) {
                    if (snapshot.getKey() != null) {
                        seenNotificationIds.remove(snapshot.getKey());
                    }
                    updateNotificationBadgeCount();
                }

                @Override
                public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Notifications listener cancelled", error.toException());
                }
            };
            notificationsRef.addChildEventListener(notificationsListener);
        }
        
        if (notificationsCountListener == null) {
            notificationsCountListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    updateNotificationBadgeCount();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Notifications count listener cancelled", error.toException());
                }
            };
            notificationsRef.addValueEventListener(notificationsCountListener);
        }
    }

    private void detachNotificationsListener() {
        if (notificationsRef != null) {
            if (notificationsListener != null) {
                notificationsRef.removeEventListener(notificationsListener);
                notificationsListener = null;
            }
            if (notificationsCountListener != null) {
                notificationsRef.removeEventListener(notificationsCountListener);
                notificationsCountListener = null;
            }
        }
    }
    
    private void handleNewNotification(DataSnapshot snapshot) {
        if (snapshot == null || snapshot.getKey() == null) {
            return;
        }
        
        String notificationId = snapshot.getKey();
        
        if (seenNotificationIds.contains(notificationId)) {
            return;
        }
        
        seenNotificationIds.add(notificationId);
        
        com.example.myapplication.notifications.NotificationItem notification = 
            snapshot.getValue(com.example.myapplication.notifications.NotificationItem.class);
        
        if (notification == null || notification.isRead()) {
            return;
        }
        
        String alertKey = "notification_" + notificationId;
        boolean isDismissed = dismissedAlertsPrefs.getBoolean(alertKey, false);
        
        if (isDismissed) {
            return;
        }
        
        String childName = notification.getChildName() != null ? notification.getChildName() : "Your child";
        String title = getNotificationTitle(notification.getType());
        String message = notification.getMessage() != null ? notification.getMessage() : "New alert for " + childName;
        
        if (getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed()) {
            getActivity().runOnUiThread(() -> {
                new androidx.appcompat.app.AlertDialog.Builder(getActivity())
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("OK", (dialog, which) -> {
                            dismissedAlertsPrefs.edit().putBoolean(alertKey, true).apply();
                        })
                        .show();
                
                android.widget.Toast.makeText(
                        getActivity(),
                        message,
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            });
        }
    }
    
    private String getNotificationTitle(com.example.myapplication.notifications.NotificationItem.NotificationType type) {
        if (type == null) {
            return "New Alert";
        }
        switch (type) {
            case RED_ZONE_DAY:
                return "Red Zone Alert";
            case RAPID_RESCUE:
                return "Rapid Rescue Alert";
            case WORSE_AFTER_DOSE:
                return "Medication Effectiveness Alert";
            case TRIAGE_ESCALATION:
                return "Emergency Guidance Alert";
            case INVENTORY_LOW:
                return "Inventory Low Alert";
            case INVENTORY_EXPIRED:
                return "Inventory Expired Alert";
            default:
                return "New Alert";
        }
    }
    
    private void updateNotificationBadgeCount() {
        if (notificationsRef == null || parentAccount == null) {
            return;
        }
        
        notificationsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                int unreadCount = 0;
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        com.example.myapplication.notifications.NotificationItem notification = 
                            child.getValue(com.example.myapplication.notifications.NotificationItem.class);
                        if (notification != null && !notification.isRead()) {
                            unreadCount++;
                        }
                    }
                }
                updateNotificationBadge(unreadCount);
            }
        });
    }

    private void updateNotificationBadge(int count) {
        if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            return;
        }
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
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
    }

    private void attachChildrenZoneListeners() {
        if (parentAccount == null || parentAccount.getChildren() == null) {
            return;
        }
        
        detachChildrenZoneListeners();
        
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                childrenZoneInfo.clear();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            });
        }
        
        HashMap<String, ChildAccount> children = parentAccount.getChildren();
        
        if (children.isEmpty()) {
            return;
        }
        
        for (Map.Entry<String, ChildAccount> entry : children.entrySet()) {
            ChildAccount child = entry.getValue();
            attachChildZoneListener(child);
        }
    }

    private void attachChildZoneListener(ChildAccount child) {
        String parentId = child.getParent_id();
        String childId = child.getID();
        
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("pefReadings");
        
        Query latestPEFQuery = pefRef.orderByChild("timestamp").limitToLast(1);
        childPEFQueries.put(childId, latestPEFQuery);
        
        latestChildAccounts.put(childId, child);
        
        ValueEventListener pefListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ChildAccount latestChild = latestChildAccounts.get(childId);
                if (latestChild == null) {
                    latestChild = child;
                }
                updateChildZoneFromSnapshot(latestChild, snapshot);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading child zone for " + childId, error.toException());
                ChildAccount latestChild = latestChildAccounts.get(childId);
                if (latestChild == null) {
                    latestChild = child;
                }
                ChildZoneInfo info = new ChildZoneInfo(latestChild, Zone.UNKNOWN, 0.0, null, null);
                updateChildZoneInfo(info);
            }
        };
        
        childPEFListeners.put(childId, pefListener);
        latestPEFQuery.addValueEventListener(pefListener);
        
        DatabaseReference childAccountRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId);
        
        childAccountRefs.put(childId, childAccountRef);
        
        ValueEventListener accountListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ChildAccount updatedChild = snapshot.getValue(ChildAccount.class);
                if (updatedChild != null) {
                    latestChildAccounts.put(childId, updatedChild);
                    
                    Query pefQuery = childPEFQueries.get(childId);
                    if (pefQuery != null) {
                        pefQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot pefSnapshot) {
                                updateChildZoneFromSnapshot(updatedChild, pefSnapshot);
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Log.e(TAG, "Error refreshing zone after personalBest change", error.toException());
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading child account for " + childId, error.toException());
            }
        };
        
        childAccountListeners.put(childId, accountListener);
        childAccountRef.addValueEventListener(accountListener);
    }

    private void updateChildZoneFromSnapshot(ChildAccount child, DataSnapshot snapshot) {
        if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            return;
        }
        
        Integer personalBest = child.getPersonalBest();
        
        if (personalBest == null || personalBest <= 0) {
            ChildZoneInfo info = new ChildZoneInfo(child, Zone.UNKNOWN, 0.0, null, null);
            updateChildZoneInfo(info);
            return;
        }
        
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

    private void detachChildrenZoneListeners() {
        for (Map.Entry<String, Query> entry : childPEFQueries.entrySet()) {
            String childId = entry.getKey();
            Query query = entry.getValue();
            ValueEventListener listener = childPEFListeners.get(childId);
            if (query != null && listener != null) {
                query.removeEventListener(listener);
            }
        }
        childPEFQueries.clear();
        childPEFListeners.clear();
        
        for (Map.Entry<String, DatabaseReference> entry : childAccountRefs.entrySet()) {
            String childId = entry.getKey();
            DatabaseReference ref = entry.getValue();
            ValueEventListener listener = childAccountListeners.get(childId);
            if (ref != null && listener != null) {
                ref.removeEventListener(listener);
            }
        }
        childAccountRefs.clear();
        childAccountListeners.clear();
        
        latestChildAccounts.clear();
    }

    private void updateChildZoneInfo(ChildZoneInfo newInfo) {
        if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            return;
        }
        getActivity().runOnUiThread(() -> {
            if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
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

    private class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.ViewHolder> {
        private final List<ChildZoneInfo> children;

        public DashboardAdapter(List<ChildZoneInfo> children) {
            this.children = children;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_child_zone_dashboard, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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
            
            // Check if summaryCharts permission is enabled and highlight Trend Snippet button
            com.example.myapplication.providermanaging.Permission permission = info.child.getPermission();
            boolean isSummaryChartsShared = permission != null && Boolean.TRUE.equals(permission.getSummaryCharts());
            if (isSummaryChartsShared) {
                holder.buttonTrendSnippet.setTextColor(0xFFF44336); // Red color
            } else {
                holder.buttonTrendSnippet.setTextColor(0xFFFFFFFF); // White color (default)
            }
            
            holder.buttonTrendSnippet.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), TrendSnippetActivity.class);
                intent.putExtra("parentId", info.child.getParent_id());
                intent.putExtra("childId", info.child.getID());
                intent.putExtra("childName", info.child.getName());
                startActivity(intent);
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
            Button buttonTrendSnippet;

            ViewHolder(View itemView) {
                super(itemView);
                textViewChildName = itemView.findViewById(R.id.textViewChildName);
                textViewZoneName = itemView.findViewById(R.id.textViewZoneName);
                textViewZonePercentage = itemView.findViewById(R.id.textViewZonePercentage);
                textViewLastPEF = itemView.findViewById(R.id.textViewLastPEF);
                buttonTrendSnippet = itemView.findViewById(R.id.buttonTrendSnippet);
            }
        }
    }
}

