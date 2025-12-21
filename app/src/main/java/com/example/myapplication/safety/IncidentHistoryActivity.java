package com.example.myapplication.safety;

import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.utils.FirebaseKeyEncoder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IncidentHistoryActivity extends AppCompatActivity {
    private static final String TAG = "IncidentHistoryActivity";
    
    private RecyclerView recyclerViewIncidents;
    private TextView textViewEmpty;
    private IncidentAdapter adapter;
    private List<TriageIncident> incidents;
    private String parentId;
    private String childId;
    
    // Realtime listener references for both encoded and raw paths
    private DatabaseReference incidentRefEncoded;
    private DatabaseReference incidentRefRaw;
    private ValueEventListener incidentListenerEncoded;
    private ValueEventListener incidentListenerRaw;
    // Separate result maps for encoded and raw paths (encoded takes precedence)
    private HashMap<String, TriageIncident> encodedResults = new HashMap<>();
    private HashMap<String, TriageIncident> rawResults = new HashMap<>();
    // Handler for debouncing UI updates and ensuring main thread execution
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingUpdate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_incident_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Determine which child to show incidents for
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("childId") && intent.hasExtra("parentId")) {
            childId = intent.getStringExtra("childId");
            parentId = intent.getStringExtra("parentId");
        } else if (UserManager.currentUser instanceof ChildAccount) {
            ChildAccount childAccount = (ChildAccount) UserManager.currentUser;
            childId = childAccount.getID();
            parentId = childAccount.getParent_id();
        } else {
            Log.e(TAG, "No childId/parentId provided and current user is not a ChildAccount");
            finish();
            return;
        }
        recyclerViewIncidents = findViewById(R.id.recyclerViewIncidents);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        android.widget.Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> {
            Intent parentIntent = new Intent(IncidentHistoryActivity.this, com.example.myapplication.ParentActivity.class);
            parentIntent.putExtra("defaultTab", "children");
            parentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(parentIntent);
            finish();
        });
        incidents = new ArrayList<>();
        adapter = new IncidentAdapter(incidents);
        recyclerViewIncidents.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewIncidents.setAdapter(adapter);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        attachIncidentListener();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        detachIncidentListener();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        detachIncidentListener();
    }
    
    private void attachIncidentListener() {
        if (parentId == null || childId == null) {
            return;
        }
        
        // Detach existing listeners first to prevent duplicates
        detachIncidentListener();
        
        // Clear result maps
        encodedResults.clear();
        rawResults.clear();
        
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        incidentRefEncoded = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("incidents");

        // Use addValueEventListener for realtime updates similar to personalBest
        // Attach listener to encoded path (current standard)
        incidentListenerEncoded = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Clear and rebuild encoded path results
                synchronized (encodedResults) {
                    encodedResults.clear();
                    if (snapshot.exists()) {
                        int totalEntries = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            totalEntries++;
                            TriageIncident incident = child.getValue(TriageIncident.class);
                            if (incident != null) {
                                // Use timestamp as key to handle duplicates
                                String key = String.valueOf(incident.getTimestamp());
                                encodedResults.put(key, incident);
                                Log.d(TAG, "Added incident from encoded path: timestamp=" + incident.getTimestamp());
                            }
                        }
                        Log.d(TAG, "Loaded " + encodedResults.size() + " incidents from encoded path (total: " + totalEntries + ", realtime update)");
                    } else {
                        Log.d(TAG, "No incidents found at encoded Firebase path: " + incidentRefEncoded.toString());
                    }
                }
                
                // Merge and display (encoded takes precedence) - debounced on main thread
                scheduleMergeAndDisplay();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading incidents from encoded Firebase path: " + incidentRefEncoded.toString(), error.toException());
            }
        };
        
        incidentRefEncoded.addValueEventListener(incidentListenerEncoded);
        
        // If encoded != raw, also attach listener to raw path for backward compatibility
        if (!encodedChildId.equals(childId)) {
            Log.d(TAG, "Attaching realtime listener to raw childId path for backward compatibility: " + childId);
            incidentRefRaw = UserManager.mDatabase
                    .child("users")
                    .child(parentId)
                    .child("children")
                    .child(childId)
                    .child("incidents");
            
            incidentListenerRaw = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    // Clear and rebuild raw path results
                    synchronized (rawResults) {
                        rawResults.clear();
                        if (snapshot.exists()) {
                            int totalEntries = 0;
                            for (DataSnapshot child : snapshot.getChildren()) {
                                totalEntries++;
                                TriageIncident incident = child.getValue(TriageIncident.class);
                                if (incident != null) {
                                    // Use timestamp as key to handle duplicates
                                    String key = String.valueOf(incident.getTimestamp());
                                    rawResults.put(key, incident);
                                    Log.d(TAG, "Added incident from raw path: timestamp=" + incident.getTimestamp());
                                }
                            }
                            Log.d(TAG, "Loaded " + rawResults.size() + " incidents from raw path (total: " + totalEntries + ", realtime update, backward compatibility)");
                        } else {
                            Log.d(TAG, "No incidents found at raw Firebase path: " + incidentRefRaw.toString());
                        }
                    }
                    
                    // Merge and display (encoded takes precedence) - debounced on main thread
                    scheduleMergeAndDisplay();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Error loading incidents from raw Firebase path: " + incidentRefRaw.toString(), error.toException());
                }
            };
            
            incidentRefRaw.addValueEventListener(incidentListenerRaw);
        } else {
            Log.d(TAG, "Encoded childId equals raw childId, only attaching listener to encoded path");
        }
    }
    
    /**
     * Schedule merge and display with debouncing to prevent rapid UI updates
     * when both listeners fire simultaneously
     */
    private void scheduleMergeAndDisplay() {
        // Cancel any pending update
        if (pendingUpdate != null) {
            mainHandler.removeCallbacks(pendingUpdate);
        }
        
        // Schedule new update with small delay to debounce rapid updates
        pendingUpdate = new Runnable() {
            @Override
            public void run() {
                mergeAndDisplayResults();
            }
        };
        mainHandler.postDelayed(pendingUpdate, 100); // 100ms debounce
    }
    
    private void mergeAndDisplayResults() {
        // Ensure this runs on main thread for UI updates
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> mergeAndDisplayResults());
            return;
        }
        
        // Merge encoded and raw results (encoded takes precedence for duplicates)
        HashMap<String, TriageIncident> merged = new HashMap<>();
        synchronized (rawResults) {
            merged.putAll(rawResults);
        }
        synchronized (encodedResults) {
            merged.putAll(encodedResults); // Encoded overwrites raw for same timestamps
        }
        
        // Convert to list and sort by timestamp descending (newest first)
        incidents.clear();
        incidents.addAll(merged.values());
        Collections.sort(incidents, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        
        Log.d(TAG, "Merged results: " + incidents.size() + " total incidents (encoded: " + encodedResults.size() + ", raw: " + rawResults.size() + ")");
        Log.d(TAG, "Incident history loaded with realtime updates and full persistence support");
        
        adapter.notifyDataSetChanged();
        
        if (incidents.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
            recyclerViewIncidents.setVisibility(View.GONE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
            recyclerViewIncidents.setVisibility(View.VISIBLE);
        }
    }
    
    private void detachIncidentListener() {
        // Cancel any pending UI updates
        if (pendingUpdate != null) {
            mainHandler.removeCallbacks(pendingUpdate);
            pendingUpdate = null;
        }
        
        // Detach encoded path listener
        if (incidentRefEncoded != null && incidentListenerEncoded != null) {
            incidentRefEncoded.removeEventListener(incidentListenerEncoded);
            incidentListenerEncoded = null;
            Log.d(TAG, "Detached encoded path listener");
        }
        incidentRefEncoded = null;
        
        // Detach raw path listener
        if (incidentRefRaw != null && incidentListenerRaw != null) {
            incidentRefRaw.removeEventListener(incidentListenerRaw);
            incidentListenerRaw = null;
            Log.d(TAG, "Detached raw path listener");
        }
        incidentRefRaw = null;
        
        // Clear result maps
        synchronized (encodedResults) {
            encodedResults.clear();
        }
        synchronized (rawResults) {
            rawResults.clear();
        }
    }

    private static class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.ViewHolder> {
        private List<TriageIncident> incidents;
        private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        public IncidentAdapter(List<TriageIncident> incidents) {
            this.incidents = incidents;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_incident, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TriageIncident incident = incidents.get(position);
            holder.textViewDate.setText(sdf.format(new Date(incident.getTimestamp())));
            
            String decision = incident.getDecisionShown();
            if ("EMERGENCY".equals(decision)) {
                holder.textViewDecision.setText("Decision: Call Emergency");
                holder.textViewDecision.setTextColor(0xFFFF0000);
            } else {
                holder.textViewDecision.setText("Decision: Home Steps");
                holder.textViewDecision.setTextColor(0xFF4CAF50);
            }
            
            Map<String, Boolean> redFlags = incident.getRedFlags();
            if (redFlags != null && !redFlags.isEmpty()) {
                StringBuilder flagsText = new StringBuilder("Red Flags: ");
                boolean first = true;
                if (Boolean.TRUE.equals(redFlags.get("speak"))) {
                    flagsText.append("Can't speak");
                    first = false;
                }
                if (Boolean.TRUE.equals(redFlags.get("chest"))) {
                    if (!first) flagsText.append(", ");
                    flagsText.append("Chest retracting");
                    first = false;
                }
                if (Boolean.TRUE.equals(redFlags.get("lips"))) {
                    if (!first) flagsText.append(", ");
                    flagsText.append("Blue/gray lips/nails");
                }
                if (flagsText.toString().equals("Red Flags: ")) {
                    flagsText.append("None");
                }
                holder.textViewRedFlags.setText(flagsText.toString());
            } else {
                holder.textViewRedFlags.setText("Red Flags: None");
            }
            
            Zone zone = incident.getZone();
            if (zone != null && zone != Zone.UNKNOWN) {
                holder.textViewZone.setText("Zone: " + zone.getDisplayName());
            } else {
                holder.textViewZone.setText("Zone: Not Available");
            }
            
            if (incident.getPefValue() != null) {
                holder.textViewPEF.setText("PEF: " + incident.getPefValue() + " L/min");
                holder.textViewPEF.setVisibility(View.VISIBLE);
            } else {
                holder.textViewPEF.setVisibility(View.GONE);
            }
            
            if (incident.isRescueAttempts() && incident.getRescueCount() > 0) {
                holder.textViewRescue.setText("Rescue medication: " + incident.getRescueCount() + " time(s)");
                holder.textViewRescue.setVisibility(View.VISIBLE);
            } else {
                holder.textViewRescue.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return incidents.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewDate;
            TextView textViewDecision;
            TextView textViewRedFlags;
            TextView textViewZone;
            TextView textViewPEF;
            TextView textViewRescue;

            ViewHolder(View itemView) {
                super(itemView);
                textViewDate = itemView.findViewById(R.id.textViewDate);
                textViewDecision = itemView.findViewById(R.id.textViewDecision);
                textViewRedFlags = itemView.findViewById(R.id.textViewRedFlags);
                textViewZone = itemView.findViewById(R.id.textViewZone);
                textViewPEF = itemView.findViewById(R.id.textViewPEF);
                textViewRescue = itemView.findViewById(R.id.textViewRescue);
            }
        }
    }
}

