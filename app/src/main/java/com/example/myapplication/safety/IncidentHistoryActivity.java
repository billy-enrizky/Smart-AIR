package com.example.myapplication.safety;

import android.os.Bundle;
import android.content.Intent;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
    
    // Realtime listener references
    private DatabaseReference incidentRef;
    private ValueEventListener incidentListener;

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
        
        // Detach existing listener first to prevent duplicates
        detachIncidentListener();
        
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        incidentRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("incidents");

        // Use direct listener instead of orderByChild query to avoid index requirements
        // Use addValueEventListener for realtime updates similar to personalBest
        incidentListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                incidents.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        TriageIncident incident = child.getValue(TriageIncident.class);
                        if (incident != null) {
                            incidents.add(incident);
                        }
                    }
                    // Sort by timestamp descending (newest first)
                    Collections.sort(incidents, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    Log.d(TAG, "Loaded " + incidents.size() + " incidents from Firebase path: " + incidentRef.toString());
                    Log.d(TAG, "Incident history loaded with realtime updates");
                } else {
                    Log.d(TAG, "No incidents found at Firebase path: " + incidentRef.toString());
                }
                
                adapter.notifyDataSetChanged();
                
                if (incidents.isEmpty()) {
                    textViewEmpty.setVisibility(View.VISIBLE);
                    recyclerViewIncidents.setVisibility(View.GONE);
                } else {
                    textViewEmpty.setVisibility(View.GONE);
                    recyclerViewIncidents.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading incidents from Firebase path: " + incidentRef.toString(), error.toException());
            }
        };
        
        incidentRef.addValueEventListener(incidentListener);
    }
    
    private void detachIncidentListener() {
        if (incidentRef != null && incidentListener != null) {
            incidentRef.removeEventListener(incidentListener);
            incidentListener = null;
        }
        incidentRef = null;
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

