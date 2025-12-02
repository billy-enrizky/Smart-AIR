package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.home.DashboardFragment;
import com.example.myapplication.home.ChildrenFragment;
import com.example.myapplication.home.ProvidersFragment;
import com.example.myapplication.userdata.ParentAccount;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

public class ParentActivity extends AppCompatActivity {
    private static final String TAG = "ParentActivity";
    
    private ParentAccount parentAccount;
    private DatabaseReference triageSessionsRef;
    private ChildEventListener triageListener;
    private Map<String, String> lastSeenSessions = new HashMap<>();
    private Map<String, String> lastSeenWorseningIds = new HashMap<>();
    private SharedPreferences dismissedAlertsPrefs;
    private static final String PREFS_NAME = "ParentActivityDismissedAlerts";
    
    private BottomNavigationView bottomNavigationView;

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
        
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.nav_children) {
                selectedFragment = new ChildrenFragment();
            } else if (itemId == R.id.nav_providers) {
                selectedFragment = new ProvidersFragment();
            }
            
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectedFragment)
                        .commit();
                return true;
            }
            
            return false;
        });
        
        if (savedInstanceState == null) {
            String defaultTab = getIntent().getStringExtra("defaultTab");
            Fragment initialFragment;
            int initialItemId;
            
            if ("children".equals(defaultTab)) {
                initialFragment = new ChildrenFragment();
                initialItemId = R.id.nav_children;
            } else {
                initialFragment = new DashboardFragment();
                initialItemId = R.id.nav_dashboard;
            }
            
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, initialFragment)
                    .commit();
            bottomNavigationView.setSelectedItemId(initialItemId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        attachTriageListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        detachTriageListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detachTriageListener();
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

        triageListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                handleTriageSnapshot(snapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                handleTriageSnapshot(snapshot);
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
            }

            @Override
            public void onCancelled(DatabaseError error) {
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


    private void handleTriageSnapshot(DataSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        String childId = snapshot.getKey();
        if (childId == null) {
            return;
        }

        String childName = snapshot.child("childName").getValue(String.class);
        if (childName == null && parentAccount != null && parentAccount.getChildren() != null) {
            com.example.myapplication.userdata.ChildAccount childAccount = parentAccount.getChildren().get(childId);
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


}
