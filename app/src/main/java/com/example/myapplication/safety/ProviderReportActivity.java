package com.example.myapplication.safety;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.example.myapplication.userdata.ProviderAccount;
import com.example.myapplication.utils.FirebaseKeyEncoder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProviderReportActivity extends AppCompatActivity {
    private static final String TAG = "ProviderReportActivity";
    
    private TextView textViewChildName;
    private TextView textViewEmpty;
    private TextView textViewNoPermission;
    private RecyclerView recyclerViewIncidents;
    private IncidentAdapter adapter;
    private List<TriageIncident> incidents;
    private String parentId;
    private String childId;
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_provider_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (!(UserManager.currentUser instanceof ProviderAccount)) {
            Log.e(TAG, "Current user is not a ProviderAccount");
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

        textViewChildName = findViewById(R.id.textViewChildName);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        textViewNoPermission = findViewById(R.id.textViewNoPermission);
        recyclerViewIncidents = findViewById(R.id.recyclerViewIncidents);
        
        incidents = new ArrayList<>();
        adapter = new IncidentAdapter(incidents);
        recyclerViewIncidents.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewIncidents.setAdapter(adapter);

        if (childName != null) {
            textViewChildName.setText("Child: " + childName);
        }

        checkPermissionAndLoadIncidents();
    }

    private void checkPermissionAndLoadIncidents() {
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        UserManager.mDatabase.child("users").child(parentId).child("children").child(encodedChildId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        ChildAccount child = task.getResult().getValue(ChildAccount.class);
                        if (child != null && child.getPermission() != null && 
                            Boolean.TRUE.equals(child.getPermission().getTriageIncidents())) {
                            loadIncidents();
                        } else {
                            textViewNoPermission.setVisibility(View.VISIBLE);
                            recyclerViewIncidents.setVisibility(View.GONE);
                            textViewEmpty.setVisibility(View.GONE);
                        }
                    } else {
                        Log.e(TAG, "Error checking permissions", task.getException());
                        textViewNoPermission.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void loadIncidents() {
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference incidentRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("incidents");

        Query query = incidentRef.orderByChild("timestamp");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
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
                    Collections.reverse(incidents);
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
                Log.e(TAG, "Error loading incidents", error.toException());
            }
        });
    }

    private static class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.ViewHolder> {
        private List<TriageIncident> incidents;

        public IncidentAdapter(List<TriageIncident> incidents) {
            this.incidents = incidents;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_incident, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TriageIncident incident = incidents.get(position);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
            holder.textViewDate.setText(sdf.format(new java.util.Date(incident.getTimestamp())));
            
            String decision = incident.getDecisionShown();
            if ("EMERGENCY".equals(decision)) {
                holder.textViewDecision.setText("Decision: Call Emergency");
                holder.textViewDecision.setTextColor(0xFFFF0000);
            } else {
                holder.textViewDecision.setText("Decision: Home Steps");
                holder.textViewDecision.setTextColor(0xFF4CAF50);
            }
            
            java.util.Map<String, Boolean> redFlags = incident.getRedFlags();
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

            ViewHolder(android.view.View itemView) {
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

