package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.safety.PEFEntryActivity;
import com.example.myapplication.safety.PEFHistoryActivity;
import com.example.myapplication.safety.PEFReading;
import com.example.myapplication.safety.TriageActivity;
import com.example.myapplication.safety.Zone;
import com.example.myapplication.safety.ZoneCalculator;
import com.example.myapplication.userdata.ChildAccount;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChildActivity extends AppCompatActivity {
    private static final String TAG = "ChildActivity";
    
    private TextView textViewZoneName;
    private TextView textViewZonePercentage;
    private TextView textViewLastPEF;
    private CardView cardViewZone;
    private Button buttonEnterPEF;
    private Button buttonViewPEFHistory;
    private Button buttonTriage;
    
    private ChildAccount childAccount;
    private ActivityResultLauncher<Intent> pefEntryLauncher;
    
    private DatabaseReference pefReadingsRef;
    private DatabaseReference childAccountRef;
    private Query latestPEFQuery;
    private ValueEventListener pefReadingsListener;
    private ValueEventListener childAccountListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_child);
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

        childAccount = (ChildAccount) UserManager.currentUser;

        textViewZoneName = findViewById(R.id.textViewZoneName);
        textViewZonePercentage = findViewById(R.id.textViewZonePercentage);
        textViewLastPEF = findViewById(R.id.textViewLastPEF);
        cardViewZone = findViewById(R.id.cardViewZone);
        buttonEnterPEF = findViewById(R.id.buttonEnterPEF);
        buttonViewPEFHistory = findViewById(R.id.buttonViewPEFHistory);
        buttonTriage = findViewById(R.id.buttonTriage);


        pefEntryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // Zone will update automatically via real-time listener
                    }
                });

        buttonEnterPEF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChildActivity.this, PEFEntryActivity.class);
                intent.putExtra("childId", childAccount.getID());
                intent.putExtra("parentId", childAccount.getParent_id());
                pefEntryLauncher.launch(intent);
            }
        });

        buttonViewPEFHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChildActivity.this, PEFHistoryActivity.class);
                intent.putExtra("childId", childAccount.getID());
                intent.putExtra("parentId", childAccount.getParent_id());
                startActivity(intent);
            }
        });

        buttonTriage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChildActivity.this, TriageActivity.class);
                intent.putExtra("childId", childAccount.getID());
                intent.putExtra("parentId", childAccount.getParent_id());
                startActivity(intent);
            }
        });

        //DEBUG
        findViewById(R.id.debugButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChildActivity.this, ChildInhalerMenu.class);
                intent.putExtra("childId", childAccount.getID());
                intent.putExtra("parentId", childAccount.getParent_id());
                startActivity(intent);
            }
        });

        attachZoneListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update childAccount from UserManager in case it changed
        if (UserManager.currentUser instanceof ChildAccount) {
            childAccount = (ChildAccount) UserManager.currentUser;
        }
        attachZoneListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        detachZoneListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detachZoneListeners();
    }

    private void attachZoneListeners() {
        if (childAccount == null) {
            return;
        }

        String parentId = childAccount.getParent_id();
        String childId = childAccount.getID();

        // Detach existing listeners first to prevent duplicates
        detachZoneListeners();

        // Attach listener for PEF readings (real-time updates)
        pefReadingsRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("pefReadings");

        latestPEFQuery = pefReadingsRef.orderByChild("timestamp").limitToLast(1);

        pefReadingsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                updateZoneDisplayFromSnapshot(snapshot);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading PEF reading", error.toException());
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        setUnknownZone();
                    }
                });
            }
        };
        
        latestPEFQuery.addValueEventListener(pefReadingsListener);

        // Attach listener for child account (to catch personalBest updates)
        childAccountRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId);

        childAccountListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Update childAccount from snapshot
                ChildAccount updatedAccount = snapshot.getValue(ChildAccount.class);
                if (updatedAccount != null) {
                    childAccount = updatedAccount;
                    // Trigger zone update by re-querying latest PEF
                    if (latestPEFQuery != null) {
                        latestPEFQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot pefSnapshot) {
                                updateZoneDisplayFromSnapshot(pefSnapshot);
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Log.e(TAG, "Error refreshing zone after personalBest change", error.toException());
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading child account", error.toException());
            }
        };
        
        childAccountRef.addValueEventListener(childAccountListener);
    }

    private void detachZoneListeners() {
        if (latestPEFQuery != null && pefReadingsListener != null) {
            latestPEFQuery.removeEventListener(pefReadingsListener);
            pefReadingsListener = null;
        }
        
        if (childAccountRef != null && childAccountListener != null) {
            childAccountRef.removeEventListener(childAccountListener);
            childAccountListener = null;
        }
        
        // Reset query references so they're recreated on next attach
        latestPEFQuery = null;
        pefReadingsRef = null;
        childAccountRef = null;
    }

    private void updateZoneDisplayFromSnapshot(DataSnapshot snapshot) {
        if (childAccount == null || isFinishing() || isDestroyed()) {
            return;
        }

        Integer personalBest = childAccount.getPersonalBest();

        if (personalBest == null || personalBest <= 0) {
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    textViewZoneName.setText("Zone Not Available");
                    textViewZonePercentage.setText("Personal Best not set");
                    textViewLastPEF.setText("");
                    cardViewZone.setCardBackgroundColor(Zone.UNKNOWN.getColorResource());
                }
            });
            return;
        }

        if (snapshot.exists() && snapshot.hasChildren()) {
            PEFReading latestReading = null;
            for (DataSnapshot child : snapshot.getChildren()) {
                latestReading = child.getValue(PEFReading.class);
                break;
            }

            if (latestReading != null) {
                int pefValue = latestReading.getValue();
                Zone zone = ZoneCalculator.calculateZone(pefValue, personalBest);
                double percentage = ZoneCalculator.calculatePercentage(pefValue, personalBest);

                final PEFReading finalReading = latestReading;
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        textViewZoneName.setText(zone.getDisplayName());
                        textViewZonePercentage.setText(String.format(Locale.getDefault(), "%.1f%% of Personal Best", percentage));
                        
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                        String dateStr = sdf.format(new Date(finalReading.getTimestamp()));
                        textViewLastPEF.setText("Last PEF: " + dateStr);
                        
                        cardViewZone.setCardBackgroundColor(zone.getColorResource());
                    }
                });
            } else {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        setUnknownZone();
                    }
                });
            }
        } else {
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    setUnknownZone();
                }
            });
        }
    }

    private void setUnknownZone() {
        textViewZoneName.setText("Zone Not Available");
        textViewZonePercentage.setText("No PEF readings yet");
        textViewLastPEF.setText("");
        cardViewZone.setCardBackgroundColor(Zone.UNKNOWN.getColorResource());
    }
}
