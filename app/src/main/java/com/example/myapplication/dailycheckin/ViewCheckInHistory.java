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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ViewCheckInHistory extends AppCompatActivity {
    private static final String TAG = "ViewCheckInHistory";
    
    TextView historyTextTitle;
    TextView historyText;
    String history = "";
    Button exportButton;
    
    // Realtime listener references for both encoded and raw paths
    private DatabaseReference checkInRefEncoded;
    private DatabaseReference checkInRefRaw;
    private ValueEventListener checkInListenerEncoded;
    private ValueEventListener checkInListenerRaw;
    // Separate result maps for encoded and raw paths (encoded takes precedence)
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
        this.historyText = (TextView)findViewById(R.id.display_history);
        exportButton = (Button)findViewById(R.id.export_pdf_button);
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
                        for (DataSnapshot s : snapshot.getChildren()) {
                            totalEntries++;
                            String date = s.getKey();
                            DailyCheckin record = s.getValue(DailyCheckin.class);
                            if (record != null) {
                                // Apply date range filter
                                String startDate = filters.getStartDate();
                                String endDate = filters.getEndDate();
                                if ((startDate == null || date.compareTo(startDate) >= 0) &&
                                    (endDate == null || date.compareTo(endDate) <= 0)) {
                                    encodedResults.put(date, record);
                                    filteredEntries++;
                                    Log.d(TAG, "Added check-in from encoded path: date=" + date + ", loggedBy=" + record.getLoggedBy());
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
                            for (DataSnapshot s : snapshot.getChildren()) {
                                totalEntries++;
                                String date = s.getKey();
                                DailyCheckin record = s.getValue(DailyCheckin.class);
                                if (record != null) {
                                    // Apply date range filter
                                    String startDate = filters.getStartDate();
                                    String endDate = filters.getEndDate();
                                    if ((startDate == null || date.compareTo(startDate) >= 0) &&
                                        (endDate == null || date.compareTo(endDate) <= 0)) {
                                        rawResults.put(date, record);
                                        filteredEntries++;
                                        Log.d(TAG, "Added check-in from raw path: date=" + date + ", loggedBy=" + record.getLoggedBy());
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
        HashMap<String, DailyCheckin> merged = new HashMap<>();
        synchronized (rawResults) {
            merged.putAll(rawResults);
        }
        synchronized (encodedResults) {
            merged.putAll(encodedResults); // Encoded overwrites raw for same dates
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
        // Process and display history
        ArrayList<String> datesInOrder = new ArrayList<>(result.keySet());
        Collections.sort(datesInOrder, Collections.reverseOrder()); // Newest first
        
        history = "";
        int datesProcessed = 0;
        int totalEntries = result.size();
        
        for (String date: datesInOrder) {
            DailyCheckin entry = result.get(date);
            if (entry != null && filters.matchFilters(entry)) {
                datesProcessed++;
                String message = "" + date + "\n";
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
        historyText.setText(history);
        historyText.setTextSize(14);
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
}
