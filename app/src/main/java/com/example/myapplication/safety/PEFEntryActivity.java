package com.example.myapplication.safety;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.safety.Zone;
import com.example.myapplication.safety.ZoneCalculator;
import com.example.myapplication.safety.ZoneChangeEvent;
import com.example.myapplication.notifications.AlertDetector;
import com.example.myapplication.utils.FirebaseKeyEncoder;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PEFEntryActivity extends AppCompatActivity {
    private static final String TAG = "PEFEntryActivity";
    
    private EditText editTextPEF;
    private CheckBox checkBoxPreMed;
    private CheckBox checkBoxPostMed;
    private EditText editTextNotes;
    private Button buttonSave;
    private Button buttonCancel;
    
    private String childId;
    private String parentId;
    private ChildAccount childAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pef_entry);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("childId") && intent.hasExtra("parentId")) {
            childId = intent.getStringExtra("childId");
            parentId = intent.getStringExtra("parentId");
            loadChildAccount();
        } else if (UserManager.currentUser instanceof ChildAccount) {
            childAccount = (ChildAccount) UserManager.currentUser;
            childId = childAccount.getID();
            parentId = childAccount.getParent_id();
        } else {
            Log.e(TAG, "No childId/parentId provided and current user is not a ChildAccount");
            finish();
            return;
        }

        editTextPEF = findViewById(R.id.editTextPEF);
        checkBoxPreMed = findViewById(R.id.checkBoxPreMed);
        checkBoxPostMed = findViewById(R.id.checkBoxPostMed);
        editTextNotes = findViewById(R.id.editTextNotes);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePEFReading();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void savePEFReading() {
        String pefText = editTextPEF.getText().toString().trim();
        
        if (pefText.isEmpty()) {
            Toast.makeText(this, "Please enter a PEF value", Toast.LENGTH_SHORT).show();
            return;
        }

        int pefValue;
        try {
            pefValue = Integer.parseInt(pefText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pefValue <= 0 || pefValue > 800) {
            Toast.makeText(this, "PEF value must be between 1 and 800 L/min", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();
        boolean preMed = checkBoxPreMed.isChecked();
        boolean postMed = checkBoxPostMed.isChecked();
        String notes = editTextNotes.getText().toString().trim();

        PEFReading reading = new PEFReading(pefValue, timestamp, preMed, postMed, notes);

        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
                .child("pefReadings")
                .child(String.valueOf(timestamp));

        pefRef.setValue(reading)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "PEF reading saved successfully to Firebase: " + pefRef.toString());
                        Toast.makeText(this, "PEF reading saved successfully", Toast.LENGTH_SHORT).show();
                        
                        checkAndLogZoneChange(pefValue);
                        
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Log.e(TAG, "Failed to save PEF reading", task.getException());
                        Toast.makeText(this, "Failed to save PEF reading", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkAndLogZoneChange(int pefValue) {
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference childRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId);
        
        childRef.child("personalBest").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult().getValue() == null) {
                return;
            }
            
            Integer personalBestTemp = null;
            Object pbValue = task.getResult().getValue();
            if (pbValue instanceof Long) {
                personalBestTemp = ((Long) pbValue).intValue();
            } else if (pbValue instanceof Integer) {
                personalBestTemp = (Integer) pbValue;
            }
            
            if (personalBestTemp == null || personalBestTemp <= 0) {
                return;
            }

            final Integer personalBest = personalBestTemp;
            Zone newZone = ZoneCalculator.calculateZone(pefValue, personalBest);
            double percentage = ZoneCalculator.calculatePercentage(pefValue, personalBest);

            DatabaseReference historyRef = UserManager.mDatabase
                    .child("users")
                    .child(parentId)
                    .child("children")
                    .child(encodedChildId)
                    .child("history");

            // Use direct listener instead of orderByKey query to avoid index requirements
            // Find the latest zone change in code after loading
            historyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    Zone previousZone = Zone.UNKNOWN;
                    long latestTimestamp = 0;
                    
                    if (snapshot.exists()) {
                        for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                            ZoneChangeEvent event = child.getValue(ZoneChangeEvent.class);
                            if (event != null && event.getNewZone() != null) {
                                long eventTimestamp = event.getTimestamp();
                                if (eventTimestamp > latestTimestamp) {
                                    latestTimestamp = eventTimestamp;
                                    previousZone = event.getNewZone();
                                }
                            }
                        }
                        Log.d(TAG, "Loaded zone history to find previous zone from Firebase path: " + historyRef.toString());
                    } else {
                        Log.d(TAG, "No zone history found at Firebase path: " + historyRef.toString());
                    }
                    
                    saveZoneChange(previousZone, newZone, pefValue, percentage, personalBest, historyRef);
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Error loading zone history from Firebase path: " + historyRef.toString(), error.toException());
                    // Continue with UNKNOWN previous zone if loading fails
                    saveZoneChange(Zone.UNKNOWN, newZone, pefValue, percentage, personalBest, historyRef);
                }
            });
        });
    }

    private void saveZoneChange(Zone previousZone, Zone newZone, int pefValue, double percentage, 
                                Integer personalBest, DatabaseReference historyRef) {

        ZoneChangeEvent zoneChange = new ZoneChangeEvent(
                System.currentTimeMillis(),
                previousZone,
                newZone,
                pefValue,
                percentage
        );
        historyRef.child(String.valueOf(zoneChange.getTimestamp())).setValue(zoneChange)
                .addOnCompleteListener(historySaveTask -> {
                    if (historySaveTask.isSuccessful()) {
                        Log.d(TAG, "Zone change saved successfully to Firebase");
                    } else {
                        Log.e(TAG, "Failed to save zone change", historySaveTask.getException());
                    }
                });

        String childName = childAccount != null ? childAccount.getName() : "Your child";
        AlertDetector.checkRedZoneDay(parentId, childId, childName, pefValue, personalBest);
        AlertDetector.checkWorseAfterDose(parentId, childId, childName, pefValue, personalBest);
    }

    private void loadChildAccount() {
        if (childId == null || parentId == null) {
            return;
        }
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        DatabaseReference childRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId);
        
        childRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                childAccount = task.getResult().getValue(ChildAccount.class);
            }
        });
    }
}

