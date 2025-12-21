package com.example.myapplication.dailycheckin;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.UserManager;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import com.example.myapplication.userdata.AccountType;
import com.example.myapplication.utils.FirebaseKeyEncoder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class ViewCheckInHistory extends AppCompatActivity {
    private static final String TAG = "ViewCheckInHistory";
    
    TextView historyTextTitle;
    TextView textViewEmpty;
    RecyclerView recyclerViewCheckIns;
    String history = "";
    Button exportButton;
    
    private CheckInAdapter adapter;
    private ArrayList<DailyCheckin> checkIns;
    
    // Realtime listener references for both encoded and raw paths
    private DatabaseReference checkInRefEncoded;
    private DatabaseReference checkInRefRaw;
    private ValueEventListener checkInListenerEncoded;
    private ValueEventListener checkInListenerRaw;
    // Separate result maps for encoded and raw paths (encoded takes precedence)
    // Keys are timestamps (as strings) to allow multiple entries per day
    private HashMap<String, DailyCheckin> encodedResults = new HashMap<>();
    private HashMap<String, DailyCheckin> rawResults = new HashMap<>();
    // Handler for debouncing UI updates and ensuring main thread execution
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingUpdate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_history);
        this.historyTextTitle = (TextView)findViewById(R.id.view_history_title);
        Intent intent = getIntent();
        if(intent.hasExtra("isProvider")&&intent.hasExtra("childName")){
            historyTextTitle.setText("History for " + intent.getStringExtra("childName"));
        }else{
            historyTextTitle.setText("History for " + SignInChildProfileActivity.getCurrentChild().getName());
        }
        this.textViewEmpty = (TextView)findViewById(R.id.textViewEmpty);
        this.recyclerViewCheckIns = (RecyclerView)findViewById(R.id.recyclerViewCheckIns);
        exportButton = (Button)findViewById(R.id.export_pdf_button);
        
        checkIns = new ArrayList<>();
        adapter = new CheckInAdapter(checkIns);
        recyclerViewCheckIns.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCheckIns.setAdapter(adapter);
        exportButton.setOnClickListener(v -> {
            // Call your PDF export logic here.
            try {
                exportToPdf();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        attachCheckInListener();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        detachCheckInListener();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        detachCheckInListener();
    }
    
    private void attachCheckInListener() {
        CheckInHistoryFilters filters = CheckInHistoryFilters.getInstance();
        String username = filters.getUsername();
        
        if (username == null) {
            Log.w(TAG, "Username is null, cannot attach check-in listener");
            return;
        }
        
        // Detach existing listeners first to prevent duplicates
        detachCheckInListener();
        
        // Clear result maps
        encodedResults.clear();
        rawResults.clear();
        
        String encodedUsername = FirebaseKeyEncoder.encode(username);
        checkInRefEncoded = UserManager.mDatabase.child("CheckInManager").child(encodedUsername);
        
        // Use addValueEventListener for realtime updates similar to personalBest
        // Attach listener to encoded path (current standard)
        checkInListenerEncoded = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                CheckInHistoryFilters filters = CheckInHistoryFilters.getInstance();
                // Clear and rebuild encoded path results
                synchronized (encodedResults) {
                    encodedResults.clear();
                    if (snapshot.exists()) {
                        int totalEntries = 0;
                        int filteredEntries = 0;
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        for (DataSnapshot s : snapshot.getChildren()) {
                            totalEntries++;
                            String key = s.getKey();
                            DailyCheckin record = s.getValue(DailyCheckin.class);
                            if (record != null) {
                                // Determine entry date from timestamp or key (for backward compatibility)
                                String entryDate;
                                long timestamp;
                                if (record.getTimestamp() != 0) {
                                    timestamp = record.getTimestamp();
                                    entryDate = dateFormat.format(new Date(timestamp));
                                } else {
                                    // Backward compatibility: key might be a date string
                                    entryDate = key;
                                    timestamp = System.currentTimeMillis(); // Fallback timestamp
                                }
                                
                                // Apply date range filter
                                String startDate = filters.getStartDate();
                                String endDate = filters.getEndDate();
                                if ((startDate == null || entryDate.compareTo(startDate) >= 0) &&
                                    (endDate == null || entryDate.compareTo(endDate) <= 0)) {
                                    // Use timestamp as key to allow multiple entries per day
                                    String mapKey = String.valueOf(timestamp);
                                    encodedResults.put(mapKey, record);
                                    filteredEntries++;
                                    Log.d(TAG, "Added check-in from encoded path: date=" + entryDate + ", timestamp=" + timestamp + ", loggedBy=" + record.getLoggedBy());
                                }
                            }
                        }
                        Log.d(TAG, "Loaded " + filteredEntries + " check-ins from encoded path (total: " + totalEntries + ", realtime update)");
                    } else {
                        Log.d(TAG, "No check-ins found at encoded Firebase path: " + checkInRefEncoded.toString());
                    }
                }
                
                // Merge and display (encoded takes precedence) - debounced on main thread
                scheduleMergeAndDisplay();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading check-in history from encoded Firebase path: " + checkInRefEncoded.toString(), error.toException());
            }
        };
        
        checkInRefEncoded.addValueEventListener(checkInListenerEncoded);
        
        // If encoded != raw, also attach listener to raw path for backward compatibility
        if (!encodedUsername.equals(username)) {
            Log.d(TAG, "Attaching realtime listener to raw username path for backward compatibility: " + username);
            checkInRefRaw = UserManager.mDatabase.child("CheckInManager").child(username);
            
            checkInListenerRaw = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    CheckInHistoryFilters filters = CheckInHistoryFilters.getInstance();
                    // Clear and rebuild raw path results
                    synchronized (rawResults) {
                        rawResults.clear();
                        if (snapshot.exists()) {
                            int totalEntries = 0;
                            int filteredEntries = 0;
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            for (DataSnapshot s : snapshot.getChildren()) {
                                totalEntries++;
                                String key = s.getKey();
                                DailyCheckin record = s.getValue(DailyCheckin.class);
                                if (record != null) {
                                    // Determine entry date from timestamp or key (for backward compatibility)
                                    String entryDate;
                                    long timestamp;
                                    if (record.getTimestamp() != 0) {
                                        timestamp = record.getTimestamp();
                                        entryDate = dateFormat.format(new Date(timestamp));
                                    } else {
                                        // Backward compatibility: key might be a date string
                                        entryDate = key;
                                        timestamp = System.currentTimeMillis(); // Fallback timestamp
                                    }
                                    
                                    // Apply date range filter
                                    String startDate = filters.getStartDate();
                                    String endDate = filters.getEndDate();
                                    if ((startDate == null || entryDate.compareTo(startDate) >= 0) &&
                                        (endDate == null || entryDate.compareTo(endDate) <= 0)) {
                                        // Use timestamp as key to allow multiple entries per day
                                        String mapKey = String.valueOf(timestamp);
                                        rawResults.put(mapKey, record);
                                        filteredEntries++;
                                        Log.d(TAG, "Added check-in from raw path: date=" + entryDate + ", timestamp=" + timestamp + ", loggedBy=" + record.getLoggedBy());
                                    }
                                }
                            }
                            Log.d(TAG, "Loaded " + filteredEntries + " check-ins from raw path (total: " + totalEntries + ", realtime update, backward compatibility)");
                        } else {
                            Log.d(TAG, "No check-ins found at raw Firebase path: " + checkInRefRaw.toString());
                        }
                    }
                    
                    // Merge and display (encoded takes precedence) - debounced on main thread
                    scheduleMergeAndDisplay();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Error loading check-in history from raw Firebase path: " + checkInRefRaw.toString(), error.toException());
                }
            };
            
            checkInRefRaw.addValueEventListener(checkInListenerRaw);
        } else {
            Log.d(TAG, "Encoded username equals raw username, only attaching listener to encoded path");
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
        // Merge encoded and raw results (encoded takes precedence for duplicates)
        // Keys are timestamps, so encoded overwrites raw for same timestamps
        HashMap<String, DailyCheckin> merged = new HashMap<>();
        synchronized (rawResults) {
            merged.putAll(rawResults);
        }
        synchronized (encodedResults) {
            merged.putAll(encodedResults); // Encoded overwrites raw for same timestamps
        }
        
        Log.d(TAG, "Merged results: " + merged.size() + " total check-ins (encoded: " + encodedResults.size() + ", raw: " + rawResults.size() + ")");
        processAndDisplayHistory(merged);
    }
    
    private void processAndDisplayHistory(HashMap<String, DailyCheckin> result) {
        // Ensure this runs on main thread for UI updates
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> processAndDisplayHistory(result));
            return;
        }
        
        CheckInHistoryFilters filters = CheckInHistoryFilters.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        // Group entries by date, then sort by timestamp within each date
        HashMap<String, ArrayList<DailyCheckin>> entriesByDate = new HashMap<>();
        for (DailyCheckin entry : result.values()) {
            if (entry != null && filters.matchFilters(entry)) {
                String entryDate;
                if (entry.getTimestamp() != 0) {
                    entryDate = dateFormat.format(new Date(entry.getTimestamp()));
                } else {
                    // Fallback for entries without timestamp
                    entryDate = "Unknown";
                }
                
                if (!entriesByDate.containsKey(entryDate)) {
                    entriesByDate.put(entryDate, new ArrayList<>());
                }
                entriesByDate.get(entryDate).add(entry);
            }
        }
        
        // Sort dates (newest first)
        ArrayList<String> datesInOrder = new ArrayList<>(entriesByDate.keySet());
        Collections.sort(datesInOrder, Collections.reverseOrder());
        
        history = "";
        int datesProcessed = 0;
        int totalEntries = result.size();
        
        for (String date : datesInOrder) {
            ArrayList<DailyCheckin> entriesForDate = entriesByDate.get(date);
            // Sort entries for this date by timestamp (newest first)
            Collections.sort(entriesForDate, (a, b) -> Long.compare(
                (b.getTimestamp() != 0 ? b.getTimestamp() : 0),
                (a.getTimestamp() != 0 ? a.getTimestamp() : 0)
            ));
            
            for (DailyCheckin entry : entriesForDate) {
                datesProcessed++;
                String message = date;
                
                // Add time if timestamp is available
                if (entry.getTimestamp() != 0) {
                    message = message + " " + timeFormat.format(new Date(entry.getTimestamp()));
                }
                message = message + "\n";
                
                // Show who logged the check-in (PARENT or CHILD)
                message = message + "Logged by: " + entry.getLoggedBy() + "\n";
                
                if (!UserManager.currentUser.getAccount().equals(AccountType.PROVIDER) /*|| provider has permission to see symptoms*/) {
                    message = message + "Night waking: " + entry.getNightWaking() + "\n";
                    message = message + entry.getActivityLimits() + "\n";
                    message = message + "Cough/Wheeze level: " + entry.getCoughWheezeLevel() + "\n";
                }

                if (!UserManager.currentUser.getAccount().equals(AccountType.PROVIDER) /*|| provider has permission to see triggers*/) {
                    message = message + "Triggers: ";
                    if (entry.getTriggers() != null && !entry.getTriggers().isEmpty()) {
                        for (String trigger : entry.getTriggers()) {
                            message = message + trigger + ", ";
                        }
                        // Remove trailing comma and space
                        message = message.substring(0, message.length() - 2);
                    } else {
                        message = message + "None";
                    }
                }
                message = message + "\n";
                history = history + message + "\n";
            }
        }
        
        if (history.isEmpty()) {
            history = "No data for selected filters.";
        }
        
        Log.d(TAG, "Displaying " + datesProcessed + " check-ins (filtered from " + totalEntries + " total entries)");
        
        // Update RecyclerView adapter
        checkIns.clear();
        for (String date : datesInOrder) {
            ArrayList<DailyCheckin> entriesForDate = entriesByDate.get(date);
            Collections.sort(entriesForDate, (a, b) -> Long.compare(
                (b.getTimestamp() != 0 ? b.getTimestamp() : 0),
                (a.getTimestamp() != 0 ? a.getTimestamp() : 0)
            ));
            checkIns.addAll(entriesForDate);
        }
        adapter.notifyDataSetChanged();
        
        if (checkIns.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
            recyclerViewCheckIns.setVisibility(View.GONE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
            recyclerViewCheckIns.setVisibility(View.VISIBLE);
        }
    }
    
    private void detachCheckInListener() {
        // Cancel any pending UI updates
        if (pendingUpdate != null) {
            mainHandler.removeCallbacks(pendingUpdate);
            pendingUpdate = null;
        }
        
        // Detach encoded path listener
        if (checkInRefEncoded != null && checkInListenerEncoded != null) {
            checkInRefEncoded.removeEventListener(checkInListenerEncoded);
            checkInListenerEncoded = null;
            Log.d(TAG, "Detached encoded path listener");
        }
        checkInRefEncoded = null;
        
        // Detach raw path listener
        if (checkInRefRaw != null && checkInListenerRaw != null) {
            checkInRefRaw.removeEventListener(checkInListenerRaw);
            checkInListenerRaw = null;
            Log.d(TAG, "Detached raw path listener");
        }
        checkInRefRaw = null;
        
        // Clear result maps
        synchronized (encodedResults) {
            encodedResults.clear();
        }
        synchronized (rawResults) {
            rawResults.clear();
        }
    }

    public void returnToSymptoms(View view) {
        Intent intent = new Intent(this, FilterCheckInBySymptoms.class);
        Intent thisintent = getIntent();
        if(thisintent.hasExtra("permissionToTriggers")){
            intent.putExtra("permissionToTriggers", thisintent.getStringExtra("permissionToTriggers"));
        }
        if(thisintent.hasExtra("permissionToSymptoms")){
            intent.putExtra("permissionToSymptoms", thisintent.getStringExtra("permissionToSymptoms"));
        }
        if(thisintent.hasExtra("isProvider")){
            intent.putExtra("isProvider", thisintent.getStringExtra("isProvider"));
            intent.putExtra("childName", thisintent.getStringExtra("childName"));
        }
        startActivity(intent);
    }


    public void exportToPdf() throws IOException {
        File documentsDir = this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (documentsDir == null) {
            Toast.makeText(this, "Storage not available.", Toast.LENGTH_SHORT).show();
            return; // Stop here
        }
        File outputFile = new File(documentsDir, CheckInHistoryFilters.getInstance().getUsername()+"_check_in_history.pdf");

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK); // Set text color
        paint.setTextSize(20f); // Set text size
        String textToWrite = "History for " + CheckInHistoryFilters.getInstance().getUsername();
        canvas.drawText(textToWrite, 200, 24, paint);
        paint.setTextSize(14f);


        String[]historyEntries = history.split("\n");
        int entryPageCount = 1;
        for (String entry: historyEntries) {
            if (18*entryPageCount > 700) {
                entryPageCount = 0;
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                paint = new Paint();
                paint.setTextSize(14f); // Set text size
                paint.setColor(Color.BLACK);
            }
            textToWrite = entry;
            canvas.drawText(textToWrite, 10, 24+(18*entryPageCount), paint);
            entryPageCount++;
        }
        document.finishPage(page);

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            document.writeTo(fos);
            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "help me", Toast.LENGTH_SHORT).show();
        }

        document.close();
    }
    
    private class CheckInAdapter extends RecyclerView.Adapter<CheckInAdapter.ViewHolder> {
        private ArrayList<DailyCheckin> checkIns;
        private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        public CheckInAdapter(ArrayList<DailyCheckin> checkIns) {
            this.checkIns = checkIns;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_check_in_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DailyCheckin entry = checkIns.get(position);
            
            // Format date and time
            String dateTime;
            if (entry.getTimestamp() != 0) {
                dateTime = dateFormat.format(new Date(entry.getTimestamp())) + " " + 
                          timeFormat.format(new Date(entry.getTimestamp()));
            } else {
                dateTime = "Unknown";
            }
            holder.textViewDate.setText(dateTime);
            
            // Show who logged the check-in
            holder.textViewLoggedBy.setText("Logged by: " + entry.getLoggedBy());
            
            // Show symptoms if not provider or if provider has permission
            if (!UserManager.currentUser.getAccount().equals(AccountType.PROVIDER)) {
                holder.textViewNightWaking.setText("Night waking: " + entry.getNightWaking());
                holder.textViewNightWaking.setVisibility(View.VISIBLE);
                
                holder.textViewActivityLimits.setText(entry.getActivityLimits());
                holder.textViewActivityLimits.setVisibility(View.VISIBLE);
                
                holder.textViewCoughWheeze.setText("Cough/Wheeze level: " + entry.getCoughWheezeLevel());
                holder.textViewCoughWheeze.setVisibility(View.VISIBLE);
            } else {
                holder.textViewNightWaking.setVisibility(View.GONE);
                holder.textViewActivityLimits.setVisibility(View.GONE);
                holder.textViewCoughWheeze.setVisibility(View.GONE);
            }
            
            // Show triggers if not provider or if provider has permission
            if (!UserManager.currentUser.getAccount().equals(AccountType.PROVIDER)) {
                StringBuilder triggersText = new StringBuilder("Triggers: ");
                if (entry.getTriggers() != null && !entry.getTriggers().isEmpty()) {
                    for (int i = 0; i < entry.getTriggers().size(); i++) {
                        if (i > 0) triggersText.append(", ");
                        triggersText.append(entry.getTriggers().get(i));
                    }
                } else {
                    triggersText.append("None");
                }
                holder.textViewTriggers.setText(triggersText.toString());
                holder.textViewTriggers.setVisibility(View.VISIBLE);
            } else {
                holder.textViewTriggers.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return checkIns.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewDate;
            TextView textViewLoggedBy;
            TextView textViewNightWaking;
            TextView textViewActivityLimits;
            TextView textViewCoughWheeze;
            TextView textViewTriggers;

            ViewHolder(View itemView) {
                super(itemView);
                textViewDate = itemView.findViewById(R.id.textViewDate);
                textViewLoggedBy = itemView.findViewById(R.id.textViewLoggedBy);
                textViewNightWaking = itemView.findViewById(R.id.textViewNightWaking);
                textViewActivityLimits = itemView.findViewById(R.id.textViewActivityLimits);
                textViewCoughWheeze = itemView.findViewById(R.id.textViewCoughWheeze);
                textViewTriggers = itemView.findViewById(R.id.textViewTriggers);
            }
        }
    }
}
