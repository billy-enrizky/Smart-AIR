package com.example.myapplication;

import android.content.Intent;
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
import com.example.myapplication.safety.SetPersonalBestActivity;
import com.example.myapplication.safety.Zone;
import com.example.myapplication.safety.ZoneCalculator;
import com.example.myapplication.safety.ActionPlanActivity;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
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
    private Button createChildButton;
    private ParentAccount parentAccount;
    private ChildrenZoneAdapter adapter;
    private List<ChildZoneInfo> childrenZoneInfo;
    private com.google.firebase.database.DatabaseReference triageSessionsRef;
    private com.google.firebase.database.ChildEventListener triageListener;
    private java.util.Map<String, String> lastSeenSessions = new java.util.HashMap<>();
    private java.util.Map<String, String> lastSeenWorseningIds = new java.util.HashMap<>();

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
        
        recyclerViewChildren = findViewById(R.id.recyclerViewChildren);
        createChildButton = findViewById(R.id.createChildPageButton);
        childrenZoneInfo = new ArrayList<>();
        adapter = new ChildrenZoneAdapter(childrenZoneInfo);
        recyclerViewChildren.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChildren.setAdapter(adapter);
        
        createChildButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ParentActivity.this, CreateChildActivity.class);
                startActivity(intent);
            }
        });
        
        loadChildrenZones();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChildrenZones();
        attachTriageListener();
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

                runOnUiThread(() -> {
                    new androidx.appcompat.app.AlertDialog.Builder(ParentActivity.this)
                            .setTitle("Breathing Assessment Started")
                            .setMessage(finalChildName + " started a breathing assessment.")
                            .setPositiveButton("OK", null)
                            .show();

                    android.widget.Toast.makeText(
                            ParentActivity.this,
                            finalChildName + " started a breathing assessment.",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();
                });
            }
        }

        // Handle worsening notification after 10-minute re-check
        String worseningId = snapshot.child("worseningId").getValue(String.class);
        Boolean worseningHasRedFlag = snapshot.child("worseningHasRedFlag").getValue(Boolean.class);

        if (worseningId != null && !worseningId.isEmpty() && Boolean.TRUE.equals(worseningHasRedFlag)) {
            String lastWorsening = lastSeenWorseningIds.get(childId);
            if (!worseningId.equals(lastWorsening)) {
                lastSeenWorseningIds.put(childId, worseningId);

                runOnUiThread(() -> {
                    new androidx.appcompat.app.AlertDialog.Builder(ParentActivity.this)
                            .setTitle("Breathing Symptoms Still Present")
                            .setMessage(finalChildName + " still has breathing symptoms after the 10-minute re-check.")
                            .setPositiveButton("OK", null)
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
            Button buttonDeleteChild;

            ViewHolder(View itemView) {
                super(itemView);
                textViewChildName = itemView.findViewById(R.id.textViewChildName);
                textViewZoneName = itemView.findViewById(R.id.textViewZoneName);
                textViewZonePercentage = itemView.findViewById(R.id.textViewZonePercentage);
                textViewLastPEF = itemView.findViewById(R.id.textViewLastPEF);
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
}
