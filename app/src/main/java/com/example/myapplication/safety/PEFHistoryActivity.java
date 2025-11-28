package com.example.myapplication.safety;

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
    private PEFHistoryAdapter adapter;
    private List<PEFReading> pefReadings;

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

        if (!(UserManager.currentUser instanceof ChildAccount)) {
            Log.e(TAG, "Current user is not a ChildAccount");
            finish();
            return;
        }

        ChildAccount childAccount = (ChildAccount) UserManager.currentUser;
        recyclerViewPEF = findViewById(R.id.recyclerViewPEF);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        Button buttonBack = findViewById(R.id.buttonBack);
        
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        pefReadings = new ArrayList<>();
        adapter = new PEFHistoryAdapter(pefReadings);
        recyclerViewPEF.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPEF.setAdapter(adapter);

        loadPEFHistory(childAccount);
    }

    private void loadPEFHistory(ChildAccount childAccount) {
        String parentId = childAccount.getParent_id();
        String childId = childAccount.getID();

        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("pefReadings");

        Query query = pefRef.orderByChild("timestamp");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                pefReadings.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        PEFReading reading = child.getValue(PEFReading.class);
                        if (reading != null) {
                            pefReadings.add(reading);
                        }
                    }
                    Collections.reverse(pefReadings);
                }
                
                adapter.notifyDataSetChanged();
                
                if (pefReadings.isEmpty()) {
                    textViewEmpty.setVisibility(View.VISIBLE);
                    recyclerViewPEF.setVisibility(View.GONE);
                } else {
                    textViewEmpty.setVisibility(View.GONE);
                    recyclerViewPEF.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading PEF history", error.toException());
            }
        });
    }

    private static class PEFHistoryAdapter extends RecyclerView.Adapter<PEFHistoryAdapter.ViewHolder> {
        private List<PEFReading> readings;
        private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        public PEFHistoryAdapter(List<PEFReading> readings) {
            this.readings = readings;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pef_reading, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PEFReading reading = readings.get(position);
            holder.textViewDate.setText(sdf.format(new Date(reading.getTimestamp())));
            holder.textViewPEFValue.setText("PEF: " + reading.getValue() + " L/min");
            
            if (reading.isPreMed()) {
                holder.textViewPreMed.setVisibility(View.VISIBLE);
            } else {
                holder.textViewPreMed.setVisibility(View.GONE);
            }
            
            if (reading.isPostMed()) {
                holder.textViewPostMed.setVisibility(View.VISIBLE);
            } else {
                holder.textViewPostMed.setVisibility(View.GONE);
            }
            
            if (reading.getNotes() != null && !reading.getNotes().isEmpty()) {
                holder.textViewNotes.setText("Notes: " + reading.getNotes());
                holder.textViewNotes.setVisibility(View.VISIBLE);
            } else {
                holder.textViewNotes.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return readings.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewDate;
            TextView textViewPEFValue;
            TextView textViewPreMed;
            TextView textViewPostMed;
            TextView textViewNotes;

            ViewHolder(View itemView) {
                super(itemView);
                textViewDate = itemView.findViewById(R.id.textViewDate);
                textViewPEFValue = itemView.findViewById(R.id.textViewPEFValue);
                textViewPreMed = itemView.findViewById(R.id.textViewPreMed);
                textViewPostMed = itemView.findViewById(R.id.textViewPostMed);
                textViewNotes = itemView.findViewById(R.id.textViewNotes);
            }
        }
    }
}

