package com.example.myapplication.safety;

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

import com.example.myapplication.ChildInhalerLogs;
import com.example.myapplication.LogHistoryActivity;
import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
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

public class PEFHistoryActivity extends AppCompatActivity {
    private static final String TAG = "PEFHistoryActivity";
    
    private RecyclerView recyclerViewPEF;
    private TextView textViewEmpty;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyItems;
    private String parentId;
    private String childId;
    private boolean isProvider;
    
    // Realtime listener references
    private DatabaseReference pefRef;
    private DatabaseReference historyRef;
    private ValueEventListener pefListener;
    private ValueEventListener historyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pef_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Only apply horizontal padding, not vertical to avoid white space at top and bottom
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("childId") && intent.hasExtra("parentId")) {
            childId = intent.getStringExtra("childId");
            parentId = intent.getStringExtra("parentId");
        } else if (UserManager.currentUser instanceof ChildAccount) {
            ChildAccount childAccount = (ChildAccount) UserManager.currentUser;
            childId = childAccount.getID();
            parentId = childAccount.getParent_id();
        } else if (SignInChildProfileActivity.currentChild != null) {
            // When logged in via children manager
            ChildAccount currentChild = SignInChildProfileActivity.currentChild;
            childId = currentChild.getID();
            parentId = currentChild.getParent_id();
        } else {
            Log.e(TAG, "No childId/parentId provided and current user is not a ChildAccount");
            finish();
            return;
        }
        
        recyclerViewPEF = findViewById(R.id.recyclerViewPEF);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        Button buttonBack = findViewById(R.id.buttonBack);
        Button buttonRemoveAll = findViewById(R.id.buttonRemoveAll);

        if (buttonBack == null) {
            Log.e(TAG, "buttonBack not found in layout");
            finish();
            return;
        }

        if (intent != null && intent.hasExtra("isProvider")) {
            isProvider = true;
            if (buttonRemoveAll != null) {
                buttonRemoveAll.setVisibility(View.GONE);
            }
        }

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // For provider view, just return to the previous screen (AccessInfoActivity)
                if (isProvider) {
                    finish();
                    return;
                }

                // If a parent is logged in, go back to ParentActivity with the Children tab selected
                if (UserManager.currentUser instanceof com.example.myapplication.userdata.ParentAccount) {
                    Intent parentIntent = new Intent(PEFHistoryActivity.this, com.example.myapplication.ParentActivity.class);
                    parentIntent.putExtra("defaultTab", "children");
                    parentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(parentIntent);
                    finish();
                } else {
                    // Child context: return to the log history menu
                    Intent logHistoryIntent = new Intent(PEFHistoryActivity.this, LogHistoryActivity.class);
                    logHistoryIntent.putExtra("childId", childId);
                    logHistoryIntent.putExtra("parentId", parentId);
                    startActivity(logHistoryIntent);
                    finish();
                }
            }
        });

        
        if (buttonRemoveAll != null) {
            buttonRemoveAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeAllHistory();
                }
            });
        }
        
        historyItems = new ArrayList<>();
        adapter = new HistoryAdapter(historyItems, parentId, childId, this);
        recyclerViewPEF.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPEF.setAdapter(adapter);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        attachHistoryListeners();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        detachHistoryListeners();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        detachHistoryListeners();
    }
    
    public void refreshHistory() {
        // Realtime listeners will automatically update, but we can trigger a refresh
        attachHistoryListeners();
    }
    
    private void attachHistoryListeners() {
        if (parentId == null || childId == null) {
            return;
        }
        
        // Detach existing listeners first to prevent duplicates
        detachHistoryListeners();
        
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("pefReadings");

        historyRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("history");

        // IMPORTANT: PEF entries persist forever, just like rescue medicine logs
        // This method loads ALL PEF readings from Firebase with realtime updates
        // Use addValueEventListener for realtime updates similar to personalBest
        // No time limit - all historical PEF entries are loaded and displayed
        final List<PEFReading>[] pefReadingsRef = new List[]{new ArrayList<>()};
        final List<ZoneChangeEvent>[] zoneChangesRef = new List[]{new ArrayList<>()};
        final boolean[] pefLoaded = {false};
        final boolean[] historyLoaded = {false};

        pefListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                pefReadingsRef[0].clear();
                if (snapshot.exists()) {
                    long oldestTimestamp = Long.MAX_VALUE;
                    long newestTimestamp = 0;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        PEFReading reading = child.getValue(PEFReading.class);
                        if (reading != null) {
                            pefReadingsRef[0].add(reading);
                            long timestamp = reading.getTimestamp();
                            if (timestamp < oldestTimestamp) {
                                oldestTimestamp = timestamp;
                            }
                            if (timestamp > newestTimestamp) {
                                newestTimestamp = timestamp;
                            }
                        }
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    if (pefReadingsRef[0].size() > 0) {
                        Log.d(TAG, "Loaded " + pefReadingsRef[0].size() + " PEF readings from Firebase path: " + pefRef.toString());
                        Log.d(TAG, "PEF history date range: " + sdf.format(new Date(oldestTimestamp)) + " to " + sdf.format(new Date(newestTimestamp)));
                        Log.d(TAG, "PEF entries persist forever - all historical entries loaded with realtime updates (no date filtering)");
                    } else {
                        Log.d(TAG, "No PEF readings found at Firebase path: " + pefRef.toString());
                    }
                } else {
                    Log.d(TAG, "No PEF readings found at Firebase path: " + pefRef.toString());
                }
                pefLoaded[0] = true;
                // Combine and display when both are loaded
                if (pefLoaded[0] && historyLoaded[0]) {
                    combineAndDisplayHistory(new ArrayList<>(pefReadingsRef[0]), new ArrayList<>(zoneChangesRef[0]));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading PEF history from Firebase path: " + pefRef.toString(), error.toException());
            }
        };

        pefRef.addValueEventListener(pefListener);

        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                zoneChangesRef[0].clear();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        ZoneChangeEvent event = child.getValue(ZoneChangeEvent.class);
                        if (event != null && event.getNewZone() != null) {
                            zoneChangesRef[0].add(event);
                        }
                    }
                    Log.d(TAG, "Loaded " + zoneChangesRef[0].size() + " zone changes from Firebase path: " + historyRef.toString());
                    Log.d(TAG, "Zone change history persists forever - all historical entries loaded with realtime updates (no date filtering)");
                } else {
                    Log.d(TAG, "No zone changes found at Firebase path: " + historyRef.toString());
                }
                historyLoaded[0] = true;
                // Combine and display when both are loaded
                if (pefLoaded[0] && historyLoaded[0]) {
                    combineAndDisplayHistory(new ArrayList<>(pefReadingsRef[0]), new ArrayList<>(zoneChangesRef[0]));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading zone change history from Firebase path: " + historyRef.toString(), error.toException());
            }
        };

        historyRef.addValueEventListener(historyListener);
    }
    
    private void detachHistoryListeners() {
        if (pefRef != null && pefListener != null) {
            pefRef.removeEventListener(pefListener);
            pefListener = null;
        }
        
        if (historyRef != null && historyListener != null) {
            historyRef.removeEventListener(historyListener);
            historyListener = null;
        }
        
        pefRef = null;
        historyRef = null;
    }


    private void combineAndDisplayHistory(List<PEFReading> pefReadings, List<ZoneChangeEvent> zoneChanges) {
        historyItems.clear();
        
        for (PEFReading reading : pefReadings) {
            ZoneChangeEvent matchingZoneChange = null;
            long readingTimestamp = reading.getTimestamp();
            
            for (ZoneChangeEvent event : zoneChanges) {
                if (Math.abs(event.getTimestamp() - readingTimestamp) < 1000) {
                    matchingZoneChange = event;
                    break;
                }
            }
            
            if (matchingZoneChange != null) {
                historyItems.add(new HistoryItem(reading, matchingZoneChange));
                zoneChanges.remove(matchingZoneChange);
            } else {
                historyItems.add(new HistoryItem(reading));
            }
        }
        
        for (ZoneChangeEvent event : zoneChanges) {
            historyItems.add(new HistoryItem(event));
        }
        
        Collections.sort(historyItems, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        adapter.notifyDataSetChanged();
        
        if (historyItems.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
            recyclerViewPEF.setVisibility(View.GONE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
            recyclerViewPEF.setVisibility(View.VISIBLE);
        }
    }

    private void removeAllHistory() {
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("pefReadings");

        DatabaseReference historyRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("history");

        pefRef.removeValue().addOnCompleteListener(task1 -> {
            historyRef.removeValue().addOnCompleteListener(task2 -> {
                if (task1.isSuccessful() && task2.isSuccessful()) {
                    historyItems.clear();
                    adapter.notifyDataSetChanged();
                    textViewEmpty.setVisibility(View.VISIBLE);
                    recyclerViewPEF.setVisibility(View.GONE);
                    android.widget.Toast.makeText(this, "All history removed", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Failed to remove all history");
                    android.widget.Toast.makeText(this, "Failed to remove all history", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void deleteHistoryItem(HistoryItem item, int position) {
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        if (item.isZoneChange()) {
            ZoneChangeEvent event = item.getZoneChange();
            DatabaseReference historyRef = UserManager.mDatabase
                    .child("users")
                    .child(parentId)
                    .child("children")
                    .child(encodedChildId)
                    .child("history")
                    .child(String.valueOf(event.getTimestamp()));

            historyRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    historyItems.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, historyItems.size() - position);
                    
                    if (historyItems.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerViewPEF.setVisibility(View.GONE);
                    }
                    android.widget.Toast.makeText(this, "Zone change removed", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Failed to delete zone change", task.getException());
                    android.widget.Toast.makeText(this, "Failed to delete zone change", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            PEFReading reading = item.getPefReading();
            DatabaseReference pefRef = UserManager.mDatabase
                    .child("users")
                    .child(parentId)
                    .child("children")
                    .child(encodedChildId)
                    .child("pefReadings")
                    .child(String.valueOf(reading.getTimestamp()));

            pefRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (item.hasZoneChange()) {
                        ZoneChangeEvent event = item.getZoneChange();
                        DatabaseReference historyRef = UserManager.mDatabase
                                .child("users")
                                .child(parentId)
                                .child("children")
                                .child(encodedChildId)
                                .child("history")
                                .child(String.valueOf(event.getTimestamp()));
                        historyRef.removeValue();
                    }
                    
                    historyItems.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, historyItems.size() - position);
                    
                    if (historyItems.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerViewPEF.setVisibility(View.GONE);
                    }
                    android.widget.Toast.makeText(this, "PEF reading removed", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Failed to delete PEF reading", task.getException());
                    android.widget.Toast.makeText(this, "Failed to delete PEF reading", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private static class HistoryItem {
        private PEFReading pefReading;
        private ZoneChangeEvent zoneChange;
        private boolean isZoneChange;

        HistoryItem(PEFReading reading) {
            this.pefReading = reading;
            this.isZoneChange = false;
        }

        HistoryItem(ZoneChangeEvent event) {
            this.zoneChange = event;
            this.isZoneChange = true;
        }

        HistoryItem(PEFReading reading, ZoneChangeEvent event) {
            this.pefReading = reading;
            this.zoneChange = event;
            this.isZoneChange = false;
        }

        long getTimestamp() {
            return isZoneChange ? zoneChange.getTimestamp() : pefReading.getTimestamp();
        }

        boolean isZoneChange() {
            return isZoneChange;
        }

        PEFReading getPefReading() {
            return pefReading;
        }

        ZoneChangeEvent getZoneChange() {
            return zoneChange;
        }

        boolean hasZoneChange() {
            return zoneChange != null;
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_PEF = 0;
        private static final int TYPE_ZONE_CHANGE = 1;
        
        private List<HistoryItem> items;
        private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        private String parentId;
        private String childId;
        private PEFHistoryActivity activity;

        public HistoryAdapter(List<HistoryItem> items, String parentId, String childId, PEFHistoryActivity activity) {
            this.items = items;
            this.parentId = parentId;
            this.childId = childId;
            this.activity = activity;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).isZoneChange() ? TYPE_ZONE_CHANGE : TYPE_PEF;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_ZONE_CHANGE) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_zone_change, parent, false);
                return new ZoneChangeViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_pef_reading, parent, false);
                return new PEFViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            HistoryItem item = items.get(position);
            
            if (holder instanceof PEFViewHolder) {
                PEFViewHolder pefHolder = (PEFViewHolder) holder;
                PEFReading reading = item.getPefReading();
                pefHolder.textViewDate.setText(sdf.format(new Date(reading.getTimestamp())));
                pefHolder.textViewPEFValue.setText("PEF: " + reading.getValue() + " L/min");
                
                if (item.hasZoneChange()) {
                    ZoneChangeEvent event = item.getZoneChange();
                    String zoneText;
                    if (event.getPreviousZone() == event.getNewZone()) {
                        zoneText = "Remains in " + event.getNewZone().getDisplayName();
                    } else {
                        zoneText = event.getPreviousZone().getDisplayName() + " → " + event.getNewZone().getDisplayName();
                    }
                    pefHolder.textViewZoneInfo.setText(zoneText);
                    pefHolder.textViewZoneInfo.setTextColor(event.getNewZone().getColorResource());
                    pefHolder.textViewZoneInfo.setVisibility(View.VISIBLE);
                    
                    String percentageText = String.format(Locale.getDefault(), "%.1f%% of Personal Best", event.getPercentage());
                    pefHolder.textViewZonePercentage.setText(percentageText);
                    pefHolder.textViewZonePercentage.setVisibility(View.VISIBLE);
                } else {
                    pefHolder.textViewZoneInfo.setVisibility(View.GONE);
                    pefHolder.textViewZonePercentage.setVisibility(View.GONE);
                }
                
                if (reading.isPreMed()) {
                    pefHolder.textViewPreMed.setVisibility(View.VISIBLE);
                } else {
                    pefHolder.textViewPreMed.setVisibility(View.GONE);
                }
                
                if (reading.isPostMed()) {
                    pefHolder.textViewPostMed.setVisibility(View.VISIBLE);
                } else {
                    pefHolder.textViewPostMed.setVisibility(View.GONE);
                }
                
                if (reading.getNotes() != null && !reading.getNotes().isEmpty()) {
                    pefHolder.textViewNotes.setText("Notes: " + reading.getNotes());
                    pefHolder.textViewNotes.setVisibility(View.VISIBLE);
                } else {
                    pefHolder.textViewNotes.setVisibility(View.GONE);
                }

                if(PEFHistoryActivity.this.isProvider){
                    pefHolder.buttonDelete.setVisibility(View.GONE);
                }
                pefHolder.buttonDelete.setOnClickListener(v -> {
                    activity.deleteHistoryItem(item, position);
                });
            } else if (holder instanceof ZoneChangeViewHolder) {
                ZoneChangeViewHolder zoneHolder = (ZoneChangeViewHolder) holder;
                ZoneChangeEvent event = item.getZoneChange();
                zoneHolder.textViewDate.setText(sdf.format(new Date(event.getTimestamp())));
                
                String zoneChangeText;
                if (event.getPreviousZone() == event.getNewZone()) {
                    zoneChangeText = "Remains in " + event.getNewZone().getDisplayName();
                } else {
                    zoneChangeText = event.getPreviousZone().getDisplayName() + " → " + event.getNewZone().getDisplayName();
                }
                zoneHolder.textViewZoneChange.setText(zoneChangeText);
                zoneHolder.textViewZoneChange.setTextColor(event.getNewZone().getColorResource());
                
                String details = String.format(Locale.getDefault(), "%.1f%% of Personal Best", event.getPercentage());
                zoneHolder.textViewZoneDetails.setText(details);
                
                if (event.getPefValue() > 0) {
                    zoneHolder.textViewPEFValue.setText("PEF: " + event.getPefValue() + " L/min");
                    zoneHolder.textViewPEFValue.setVisibility(View.VISIBLE);
                } else {
                    zoneHolder.textViewPEFValue.setVisibility(View.GONE);
                }

                if(PEFHistoryActivity.this.isProvider){
                    zoneHolder.buttonDelete.setVisibility(View.GONE);
                }
                zoneHolder.buttonDelete.setOnClickListener(v -> {
                    activity.deleteHistoryItem(item, position);
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class PEFViewHolder extends RecyclerView.ViewHolder {
            TextView textViewDate;
            TextView textViewPEFValue;
            TextView textViewZoneInfo;
            TextView textViewZonePercentage;
            TextView textViewPreMed;
            TextView textViewPostMed;
            TextView textViewNotes;
            Button buttonDelete;

            PEFViewHolder(View itemView) {
                super(itemView);
                textViewDate = itemView.findViewById(R.id.textViewDate);
                textViewPEFValue = itemView.findViewById(R.id.textViewPEFValue);
                textViewZoneInfo = itemView.findViewById(R.id.textViewZoneInfo);
                textViewZonePercentage = itemView.findViewById(R.id.textViewZonePercentage);
                textViewPreMed = itemView.findViewById(R.id.textViewPreMed);
                textViewPostMed = itemView.findViewById(R.id.textViewPostMed);
                textViewNotes = itemView.findViewById(R.id.textViewNotes);
                buttonDelete = itemView.findViewById(R.id.buttonDelete);
            }
        }

        class ZoneChangeViewHolder extends RecyclerView.ViewHolder {
            TextView textViewDate;
            TextView textViewZoneChange;
            TextView textViewZoneDetails;
            TextView textViewPEFValue;
            Button buttonDelete;

            ZoneChangeViewHolder(View itemView) {
                super(itemView);
                textViewDate = itemView.findViewById(R.id.textViewDate);
                textViewZoneChange = itemView.findViewById(R.id.textViewZoneChange);
                textViewZoneDetails = itemView.findViewById(R.id.textViewZoneDetails);
                textViewPEFValue = itemView.findViewById(R.id.textViewPEFValue);
                buttonDelete = itemView.findViewById(R.id.buttonDelete);
            }
        }
    }
}

