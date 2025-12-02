package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.dailycheckin.CheckInView;
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

import android.graphics.Color;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChildActivity extends AppCompatActivity {
    private static final String TAG = "ChildActivity";

    private Achievement achievementData;
    private TextView textViewZoneName;
    private TextView textViewZonePercentage;
    private TextView textViewLastPEF;
    private CardView cardViewZone;
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
        setContentView(R.layout.activity_child);
        Button techniqueButton = findViewById(R.id.techniquebutton);
        Button streakButton = findViewById(R.id.streakbutton);
        Button useButton = findViewById(R.id.usebutton);
        Button logsButton = findViewById(R.id.logsButton);
        Button dailycheck = findViewById(R.id.dailycheckinbutton);
        Button signout = findViewById(R.id.signOutButton);

        streakButton.setEnabled(false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Only apply horizontal padding, not vertical to avoid white space at top and bottom
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });
        AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
            @Override
            public void onComplete(Achievement achievement) {
                if (achievement == null) {
                    Achievement newAch = new Achievement(UserManager.currentUser.getID());
                    AchievementsModel.writeIntoDB(newAch, new CallBack() {
                        @Override
                        public void onComplete() {
                            achievementData = newAch;
                            streakButton.setEnabled(true);
                        }
                    });
                    return;
                }

                achievementData = achievement;

                if (!achievementData.badges.get(2)) {
                    if (achievementData.checkBadge3()) {
                        Toast.makeText(ChildActivity.this, "You've earned a new badge!", Toast.LENGTH_SHORT).show();
                        achievementData.badges.set(2, true);
                        AchievementsModel.writeIntoDB(achievementData, new CallBack() {
                            @Override
                            public void onComplete() {
                                streakButton.setEnabled(true);
                            }
                        });
                        return;
                    }
                }

                streakButton.setEnabled(true);
            }
        });
        childAccount = (ChildAccount) UserManager.currentUser;
        textViewZoneName = findViewById(R.id.textViewZoneName);
        textViewZonePercentage = findViewById(R.id.textViewZonePercentage);
        textViewLastPEF = findViewById(R.id.textViewLastPEF);
        cardViewZone = findViewById(R.id.cardViewZone);
        Button pefButton = findViewById(R.id.pefButton);
        Button emergencyButton = findViewById(R.id.emergencyButton);


        pefEntryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // Zone will update automatically via real-time listener
                    }
                });

        emergencyButton.setOnClickListener(v -> {
                Intent intent = new Intent(ChildActivity.this, TriageActivity.class);
                intent.putExtra("childId", childAccount.getID());
                intent.putExtra("parentId", childAccount.getParent_id());
                startActivity(intent);
        });
        pefButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChildActivity.this, PEFEntryActivity.class);
            intent.putExtra("childId", childAccount.getID());
            intent.putExtra("parentId", childAccount.getParent_id());
            startActivity(intent);
        });
        techniqueButton.setOnClickListener(v -> {
            startActivity(new Intent(ChildActivity.this, ChildInhalerInstructions.class));
        });

        dailycheck.setOnClickListener(v -> {
            startActivity(new Intent(ChildActivity.this, CheckInView.class));
        });

        signout.setOnClickListener(v -> {
            UserManager.backToLogin(this);
        });

        logsButton.setOnClickListener(v -> {
            startActivity(new Intent(ChildActivity.this, ChildInhalerLogs.class));
        });

        useButton.setOnClickListener(v -> {
            startActivity(new Intent(ChildActivity.this, ChildInhalerUse.class));
        });

        streakButton.setOnClickListener(v -> {
            startActivity(new Intent(ChildActivity.this, ChildInhalerDailyStreak.class));
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
                    cardViewZone.setCardBackgroundColor(getZoneColor(Zone.UNKNOWN));
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

                        cardViewZone.setCardBackgroundColor(getZoneColor(zone));
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
        cardViewZone.setCardBackgroundColor(getZoneColor(Zone.UNKNOWN));
    }

    private int getZoneColor(Zone zone) {
        switch (zone) {
            case GREEN:
                return Color.parseColor("#4CAF50"); // Material Green
            case YELLOW:
                return Color.parseColor("#FFC107"); // Material Amber
            case RED:
                return Color.parseColor("#F44336"); // Material Red
            case UNKNOWN:
            default:
                return Color.parseColor("#9E9E9E"); // Material Grey
        }
    }
}
