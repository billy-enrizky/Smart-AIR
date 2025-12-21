package com.example.myapplication.dailycheckin;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
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
    
    // Realtime listener references
    private DatabaseReference checkInRef;
    private ValueEventListener checkInListener;

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
            return;
        }
        
        // Detach existing listener first to prevent duplicates
        detachCheckInListener();
        
        String encodedUsername = FirebaseKeyEncoder.encode(username);
        checkInRef = UserManager.mDatabase.child("CheckInManager").child(encodedUsername);
        
        // Use addValueEventListener for realtime updates similar to personalBest
        // Apply date range and filters in code after loading
        checkInListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                HashMap<String, DailyCheckin> result = new HashMap<>();
                if (snapshot.exists()) {
                    for (DataSnapshot s : snapshot.getChildren()) {
                        String date = s.getKey();
                        DailyCheckin record = s.getValue(DailyCheckin.class);
                        if (record != null) {
                            // Apply date range filter
                            String startDate = filters.getStartDate();
                            String endDate = filters.getEndDate();
                            if ((startDate == null || date.compareTo(startDate) >= 0) &&
                                (endDate == null || date.compareTo(endDate) <= 0)) {
                                result.put(date, record);
                            }
                        }
                    }
                    Log.d(TAG, "Loaded " + result.size() + " check-ins with realtime updates (after date filtering)");
                } else {
                    Log.d(TAG, "No check-ins found at Firebase path: " + checkInRef.toString());
                }
                
                // Process and display history
                ArrayList<String> datesInOrder = new ArrayList<>(result.keySet());
                Collections.sort(datesInOrder);
                history = "";
                int datesProcessed = 0;
                for (String date: datesInOrder) {
                    DailyCheckin entry = result.get(date);
                    if (entry != null && filters.matchFilters(entry)) {
                        datesProcessed++;
                        String message = "" + date + "\n";
                        message = message + "Logged by: " + entry.getLoggedBy() + "\n";
                        if (!UserManager.currentUser.getAccount().equals(AccountType.PROVIDER) /*|| provider has permission to see symptoms*/) {
                            message = message + "Night waking: " + entry.getNightWaking() + "\n";
                            message = message + entry.getActivityLimits() + "\n";
                            message = message + "Cough/Wheeze level: " + entry.getCoughWheezeLevel() + "\n";
                        }

                        if (!UserManager.currentUser.getAccount().equals(AccountType.PROVIDER) /*|| provider has permission to see triggers*/) {
                            message = message + "Triggers: ";
                            for (String trigger : entry.getTriggers()) {
                                message = message + trigger + ", ";
                            }
                        }
                        message = message + "\n";
                        history = history + message + "\n";
                    }
                }
                if (history.isEmpty()) {
                    history = "No data for selected filters.";
                }
                historyText.setText(history);
                historyText.setTextSize(14);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading check-in history from Firebase path: " + checkInRef.toString(), error.toException());
            }
        };
        
        checkInRef.addValueEventListener(checkInListener);
    }
    
    private void detachCheckInListener() {
        if (checkInRef != null && checkInListener != null) {
            checkInRef.removeEventListener(checkInListener);
            checkInListener = null;
        }
        checkInRef = null;
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
