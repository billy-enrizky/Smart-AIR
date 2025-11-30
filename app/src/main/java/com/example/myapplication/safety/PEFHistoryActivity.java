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

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pef_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
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
        } else {
            Log.e(TAG, "No childId/parentId provided and current user is not a ChildAccount");
            finish();
            return;
        }
        
        recyclerViewPEF = findViewById(R.id.recyclerViewPEF);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        Button buttonBack = findViewById(R.id.buttonBack);
        Button buttonEnterPEF = findViewById(R.id.buttonEnterPEF);
        Button buttonRemoveAll = findViewById(R.id.buttonRemoveAll);
        
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        buttonEnterPEF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PEFHistoryActivity.this, PEFEntryActivity.class);
                intent.putExtra("childId", childId);
                intent.putExtra("parentId", parentId);
                startActivity(intent);
            }
        });
        
        buttonRemoveAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeAllHistory();
            }
        });
        
        historyItems = new ArrayList<>();
        adapter = new HistoryAdapter(historyItems, parentId, childId, this);
        recyclerViewPEF.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPEF.setAdapter(adapter);

        loadHistory(parentId, childId);
    }
    
    public void refreshHistory() {
        loadHistory(parentId, childId);
    }

    private void loadHistory(String parentId, String childId) {
        historyItems.clear();
        
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("pefReadings");

        DatabaseReference historyRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("history");

        Query pefQuery = pefRef.orderByChild("timestamp");
        Query historyQuery = historyRef.orderByKey();

        final int[] loadCount = {0};
        final int totalLoads = 2;

        final List<PEFReading> pefReadings = new ArrayList<>();
        final List<ZoneChangeEvent> zoneChanges = new ArrayList<>();

        pefQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        PEFReading reading = child.getValue(PEFReading.class);
                        if (reading != null) {
                            pefReadings.add(reading);
                        }
                    }
                }
                loadCount[0]++;
                if (loadCount[0] == totalLoads) {
                    combineAndDisplayHistory(pefReadings, zoneChanges);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading PEF history", error.toException());
                loadCount[0]++;
                if (loadCount[0] == totalLoads) {
                    combineAndDisplayHistory(pefReadings, zoneChanges);
                }
            }
        });

        historyQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        ZoneChangeEvent event = child.getValue(ZoneChangeEvent.class);
                        if (event != null && event.getNewZone() != null) {
                            zoneChanges.add(event);
                        }
                    }
                }
                loadCount[0]++;
                if (loadCount[0] == totalLoads) {
                    combineAndDisplayHistory(pefReadings, zoneChanges);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading zone change history", error.toException());
                loadCount[0]++;
                if (loadCount[0] == totalLoads) {
                    combineAndDisplayHistory(pefReadings, zoneChanges);
                }
            }
        });
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
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("pefReadings");

        DatabaseReference historyRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
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
        if (item.isZoneChange()) {
            ZoneChangeEvent event = item.getZoneChange();
            DatabaseReference historyRef = UserManager.mDatabase
                    .child("users")
                    .child(parentId)
                    .child("children")
                    .child(childId)
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
                    .child(childId)
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
                                .child(childId)
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

    private static class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
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
                
                zoneHolder.buttonDelete.setOnClickListener(v -> {
                    activity.deleteHistoryItem(item, position);
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class PEFViewHolder extends RecyclerView.ViewHolder {
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

        static class ZoneChangeViewHolder extends RecyclerView.ViewHolder {
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

