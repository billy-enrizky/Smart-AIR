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

        String childId;
        String parentId;
        
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
        
        historyItems = new ArrayList<>();
        adapter = new HistoryAdapter(historyItems);
        recyclerViewPEF.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPEF.setAdapter(adapter);

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

        pefQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        PEFReading reading = child.getValue(PEFReading.class);
                        if (reading != null) {
                            historyItems.add(new HistoryItem(reading));
                        }
                    }
                }
                loadCount[0]++;
                if (loadCount[0] == totalLoads) {
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
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading PEF history", error.toException());
                loadCount[0]++;
            }
        });

        historyQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        ZoneChangeEvent event = child.getValue(ZoneChangeEvent.class);
                        if (event != null && event.getNewZone() != null && event.getPreviousZone() != null) {
                            if (event.getPreviousZone() != Zone.UNKNOWN || event.getNewZone() != Zone.UNKNOWN) {
                                historyItems.add(new HistoryItem(event));
                            }
                        }
                    }
                }
                loadCount[0]++;
                if (loadCount[0] == totalLoads) {
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
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading zone change history", error.toException());
                loadCount[0]++;
            }
        });
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
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_PEF = 0;
        private static final int TYPE_ZONE_CHANGE = 1;
        
        private List<HistoryItem> items;
        private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        public HistoryAdapter(List<HistoryItem> items) {
            this.items = items;
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
            } else if (holder instanceof ZoneChangeViewHolder) {
                ZoneChangeViewHolder zoneHolder = (ZoneChangeViewHolder) holder;
                ZoneChangeEvent event = item.getZoneChange();
                zoneHolder.textViewDate.setText(sdf.format(new Date(event.getTimestamp())));
                
                String zoneChangeText = event.getPreviousZone().getDisplayName() + " → " + event.getNewZone().getDisplayName();
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
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class PEFViewHolder extends RecyclerView.ViewHolder {
            TextView textViewDate;
            TextView textViewPEFValue;
            TextView textViewPreMed;
            TextView textViewPostMed;
            TextView textViewNotes;

            PEFViewHolder(View itemView) {
                super(itemView);
                textViewDate = itemView.findViewById(R.id.textViewDate);
                textViewPEFValue = itemView.findViewById(R.id.textViewPEFValue);
                textViewPreMed = itemView.findViewById(R.id.textViewPreMed);
                textViewPostMed = itemView.findViewById(R.id.textViewPostMed);
                textViewNotes = itemView.findViewById(R.id.textViewNotes);
            }
        }

        static class ZoneChangeViewHolder extends RecyclerView.ViewHolder {
            TextView textViewDate;
            TextView textViewZoneChange;
            TextView textViewZoneDetails;
            TextView textViewPEFValue;

            ZoneChangeViewHolder(View itemView) {
                super(itemView);
                textViewDate = itemView.findViewById(R.id.textViewDate);
                textViewZoneChange = itemView.findViewById(R.id.textViewZoneChange);
                textViewZoneDetails = itemView.findViewById(R.id.textViewZoneDetails);
                textViewPEFValue = itemView.findViewById(R.id.textViewPEFValue);
            }
        }
    }
}

