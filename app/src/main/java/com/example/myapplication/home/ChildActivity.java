package com.example.myapplication.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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

import com.example.myapplication.Achievement;
import com.example.myapplication.AchievementsModel;
import com.example.myapplication.CallBack;
import com.example.myapplication.ChildInhalerDailyStreak;
import com.example.myapplication.ChildInhalerInstructions;
import com.example.myapplication.ChildInhalerUse;
import com.example.myapplication.LogHistoryActivity;
import com.example.myapplication.R;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.SignIn.SignInView;
import com.example.myapplication.UserManager;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import com.example.myapplication.dailycheckin.CheckInView;
import com.example.myapplication.safety.PEFEntryActivity;
import com.example.myapplication.safety.PEFHistoryActivity;
import com.example.myapplication.safety.PEFReading;
import com.example.myapplication.safety.TriageActivity;
import com.example.myapplication.safety.Zone;
import com.example.myapplication.safety.ZoneCalculator;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.utils.FirebaseKeyEncoder;
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

        // Register for activity results BEFORE any async operations
        // This must be done in onCreate() before the activity is STARTED
        pefEntryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // Zone will update automatically via real-time listener
                    }
                });

        // Try to get childId and parentId from intent extras first
        Intent intent = getIntent();
        String childId = null;
        String parentId = null;
        if (intent != null && intent.hasExtra("childId") && intent.hasExtra("parentId")) {
            childId = intent.getStringExtra("childId");
            parentId = intent.getStringExtra("parentId");
        }

        // Check if user is properly set up
        if (UserManager.currentUser == null) {
            // If we have childId/parentId, try to restore child context
            if (childId != null && parentId != null) {
                restoreChildContext(childId, parentId);
                return;
            }
            Log.e(TAG, "UserManager.currentUser is null, redirecting to sign in");
            Intent signInIntent = new Intent(this, SignInView.class);
            startActivity(signInIntent);
            finish();
            return;
        }

        if (!(UserManager.currentUser instanceof ChildAccount)) {
            // If we have childId/parentId, try to restore child context
            if (childId != null && parentId != null) {
                restoreChildContext(childId, parentId);
                return;
            }
            // Also check SignInChildProfileActivity.currentChild as fallback
            if (SignInChildProfileActivity.currentChild != null) {
                ChildAccount currentChild = SignInChildProfileActivity.currentChild;
                if (currentChild.getID() != null && !currentChild.getID().isEmpty()) {
                    restoreChildContext(currentChild.getID(), currentChild.getParent_id());
                    return;
                }
            }
            Log.e(TAG, "UserManager.currentUser is not a ChildAccount, redirecting to sign in");
            Intent signInIntent = new Intent(this, SignInView.class);
            startActivity(signInIntent);
            finish();
            return;
        }

        childAccount = (ChildAccount) UserManager.currentUser;

        if (childAccount.getID() == null || childAccount.getID().isEmpty()) {
            Log.e(TAG, "ChildAccount ID is null or empty, redirecting to sign in");
            Intent signInIntent = new Intent(this, SignInView.class);
            startActivity(signInIntent);
            finish();
            return;
        }

        initializeActivity();
    }

    private void restoreChildContext(String childId, String parentId) {
        Log.d(TAG, "Restoring child context: childId=" + childId + ", parentId=" + parentId);
        ChildAccount tempChild = new ChildAccount();
        tempChild.ReadFromDatabase(parentId, childId, new CallBack() {
            @Override
            public void onComplete() {
                if (tempChild.getID() == null || tempChild.getID().isEmpty()) {
                    Log.e(TAG, "Failed to load child account from Firebase");
                    Intent signInIntent = new Intent(ChildActivity.this, SignInView.class);
                    startActivity(signInIntent);
                    finish();
                    return;
                }

                // Restore child context
                childAccount = tempChild;
                UserManager.currentUser = childAccount;
                SignInChildProfileActivity.currentChild = childAccount;

                Log.d(TAG, "Child context restored successfully");

                // Continue with normal initialization
                initializeActivity();
            }
        });
    }

    private void initializeActivity() {
        setContentView(R.layout.activity_child);
        Button techniqueButton = findViewById(R.id.techniquebutton);
        Button streakButton = findViewById(R.id.streakbutton);
        Button useButton = findViewById(R.id.usebutton);
        Button logsButton = findViewById(R.id.logsButton);
        Button dailycheck = findViewById(R.id.dailycheckinbutton);
        ImageButton signout = findViewById(R.id.signOutButton);

        streakButton.setEnabled(false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Only apply horizontal padding, not vertical to avoid white space at top and bottom
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });
        AchievementsModel.readFromDB(childAccount.getID(), new ResultCallBack<Achievement>() {
            @Override
            public void onComplete(Achievement achievement) {
                if (achievement == null) {
                    if (childAccount == null || childAccount.getID() == null) {
                        Log.e(TAG, "Cannot create achievement: childAccount or ID is null");
                        return;
                    }
                    Achievement newAch = new Achievement(childAccount.getID());
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

                if (!achievementData.getBadgeAt(2)) {
                    if (achievementData.checkBadge3()) {
                        Toast.makeText(ChildActivity.this, "You've earned a new badge!", Toast.LENGTH_SHORT).show();
                        achievementData.setBadgeAt(2, true);
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
        textViewZoneName = findViewById(R.id.textViewZoneName);
        textViewZonePercentage = findViewById(R.id.textViewZonePercentage);
        textViewLastPEF = findViewById(R.id.textViewLastPEF);
        cardViewZone = findViewById(R.id.cardViewZone);
        Button pefButton = findViewById(R.id.pefButton);
        Button emergencyButton = findViewById(R.id.emergencyButton);

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
            Intent intent = new Intent(ChildActivity.this, LogHistoryActivity.class);
            intent.putExtra("childId", childAccount.getID());
            intent.putExtra("parentId", childAccount.getParent_id());
            startActivity(intent);
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
        String encodedChildId = FirebaseKeyEncoder.encode(childId);
        pefReadingsRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(encodedChildId)
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
                .child(encodedChildId);

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

