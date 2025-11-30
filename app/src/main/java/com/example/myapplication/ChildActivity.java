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
import com.example.myapplication.safety.IncidentHistoryActivity;
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
    private Button buttonIncidentHistory;
    private Button buttonTriage;
    
    private ChildAccount childAccount;
    private ActivityResultLauncher<Intent> pefEntryLauncher;

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
        buttonIncidentHistory = findViewById(R.id.buttonIncidentHistory);
        buttonTriage = findViewById(R.id.buttonTriage);

        //DeBUG
        Button buttonDebug = findViewById(R.id.debugButton);

        pefEntryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            refreshZoneDisplay();
                        }
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

        buttonIncidentHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChildActivity.this, IncidentHistoryActivity.class);
                // IncidentHistoryActivity currently uses UserManager.currentUser as ChildAccount,
                // so no extras are strictly required, but this keeps it consistent if extended later.
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
        buttonDebug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChildActivity.this, ChildInhalerMenu.class);
                intent.putExtra("childId", childAccount.getID());
                intent.putExtra("parentId", childAccount.getParent_id());
                startActivity(intent);
            }
        });

        refreshZoneDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshZoneDisplay();
    }

    private void refreshZoneDisplay() {
        if (childAccount == null) {
            return;
        }

        Integer personalBest = childAccount.getPersonalBest();
        String parentId = childAccount.getParent_id();
        String childId = childAccount.getID();

        if (personalBest == null || personalBest <= 0) {
            textViewZoneName.setText("Zone Not Available");
            textViewZonePercentage.setText("Personal Best not set");
            textViewLastPEF.setText("");
            cardViewZone.setCardBackgroundColor(Zone.UNKNOWN.getColorResource());
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

                        textViewZoneName.setText(zone.getDisplayName());
                        textViewZonePercentage.setText(String.format(Locale.getDefault(), "%.1f%% of Personal Best", percentage));
                        
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                        String dateStr = sdf.format(new Date(latestReading.getTimestamp()));
                        textViewLastPEF.setText("Last PEF: " + dateStr);
                        
                        cardViewZone.setCardBackgroundColor(zone.getColorResource());
                    } else {
                        setUnknownZone();
                    }
                } else {
                    setUnknownZone();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading PEF reading", error.toException());
                setUnknownZone();
            }
        });
    }

    private void setUnknownZone() {
        textViewZoneName.setText("Zone Not Available");
        textViewZonePercentage.setText("No PEF readings yet");
        textViewLastPEF.setText("");
        cardViewZone.setCardBackgroundColor(Zone.UNKNOWN.getColorResource());
    }
}
