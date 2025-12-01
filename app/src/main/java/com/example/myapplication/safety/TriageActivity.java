package com.example.myapplication.safety;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.notifications.AlertDetector;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

public class TriageActivity extends AppCompatActivity {
    private static final String TAG = "TriageActivity";
    
    private static final int STEP_RED_FLAGS = 0;
    private static final int STEP_RESCUE = 1;
    private static final int STEP_PEF = 2;
    private static final int STEP_DECISION = 3;
    
    private FrameLayout frameLayoutSteps;
    private TriageSession session;
    private TriageSession previousSession;
    private int currentStep;
    private ChildAccount childAccount;
    private View currentStepView;
    
    private Map<String, Boolean> redFlags;
    private boolean rescueAttempts;
    private int rescueCount;
    private Integer pefValue;
    private boolean isRecheck;
    
    private Map<Zone, String> cachedActionPlans;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_triage);
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
            
            DatabaseReference childRef = UserManager.mDatabase
                    .child("users")
                    .child(parentId)
                    .child("children")
                    .child(childId);
            
            childRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().getValue() != null) {
                    childAccount = task.getResult().getValue(ChildAccount.class);
                    if (childAccount != null) {
                        initializeTriage();
                    } else {
                        Log.e(TAG, "Failed to load child account");
                        finish();
                    }
                } else {
                    Log.e(TAG, "Failed to load child account", task.getException());
                    finish();
                }
            });
            return;
        } else if (UserManager.currentUser instanceof ChildAccount) {
            childAccount = (ChildAccount) UserManager.currentUser;
            childId = childAccount.getID();
            parentId = childAccount.getParent_id();
        } else {
            Log.e(TAG, "No childId/parentId provided and current user is not a ChildAccount");
            finish();
            return;
        }
        
        initializeTriage();
    }
    
    private void initializeTriage() {
        if (childAccount == null) {
            Log.e(TAG, "ChildAccount is null");
            finish();
            return;
        }
        session = new TriageSession();
        previousSession = null;
        currentStep = STEP_RED_FLAGS;
        redFlags = new HashMap<>();
        isRecheck = false;
        cachedActionPlans = new HashMap<>();
        
        frameLayoutSteps = findViewById(R.id.frameLayoutSteps);
        Button buttonBack = findViewById(R.id.buttonBack);
        
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Load action plans from parent account
        loadActionPlans();

        // Write triage session flag for parent real-time alert
        String parentId = childAccount.getParent_id();
        String childId = childAccount.getID();
        DatabaseReference triageRef = UserManager.mDatabase
                .child("triageSessions")
                .child(parentId)
                .child(childId);
        Map<String, Object> triageInfo = new HashMap<>();
        triageInfo.put("sessionId", session.getSessionId());
        triageInfo.put("startTime", session.getStartTime());
        triageInfo.put("childName", childAccount.getName());
        triageRef.setValue(triageInfo);

        Toast.makeText(this, "Breathing assessment started", Toast.LENGTH_SHORT).show();
        showRedFlagsStep();
    }
    
    private void loadActionPlans() {
        if (childAccount == null) {
            return;
        }
        
        String parentId = childAccount.getParent_id();
        DatabaseReference actionPlansRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("actionPlans");
        
        actionPlansRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getValue() != null) {
                com.google.firebase.database.DataSnapshot snapshot = task.getResult();
                String greenPlan = snapshot.child("green").getValue(String.class);
                String yellowPlan = snapshot.child("yellow").getValue(String.class);
                String redPlan = snapshot.child("red").getValue(String.class);
                
                if (greenPlan != null && !greenPlan.isEmpty()) {
                    cachedActionPlans.put(Zone.GREEN, greenPlan);
                }
                if (yellowPlan != null && !yellowPlan.isEmpty()) {
                    cachedActionPlans.put(Zone.YELLOW, yellowPlan);
                }
                if (redPlan != null && !redPlan.isEmpty()) {
                    cachedActionPlans.put(Zone.RED, redPlan);
                }
            }
        });
    }

    private void showRedFlagsStep() {
        currentStep = STEP_RED_FLAGS;
        LayoutInflater inflater = LayoutInflater.from(this);
        currentStepView = inflater.inflate(R.layout.layout_red_flags, frameLayoutSteps, false);
        frameLayoutSteps.removeAllViews();
        frameLayoutSteps.addView(currentStepView);
        
        Button buttonYes1 = currentStepView.findViewById(R.id.buttonYes1);
        Button buttonNo1 = currentStepView.findViewById(R.id.buttonNo1);
        Button buttonYes2 = currentStepView.findViewById(R.id.buttonYes2);
        Button buttonNo2 = currentStepView.findViewById(R.id.buttonNo2);
        Button buttonYes3 = currentStepView.findViewById(R.id.buttonYes3);
        Button buttonNo3 = currentStepView.findViewById(R.id.buttonNo3);
        Button buttonNext = currentStepView.findViewById(R.id.buttonNextRedFlags);
        
        buttonYes1.setOnClickListener(v -> {
            toggleRedFlagChoice("speak", true, buttonYes1, buttonNo1, buttonNext);
        });
        buttonNo1.setOnClickListener(v -> {
            toggleRedFlagChoice("speak", false, buttonYes1, buttonNo1, buttonNext);
        });
        
        buttonYes2.setOnClickListener(v -> {
            toggleRedFlagChoice("chest", true, buttonYes2, buttonNo2, buttonNext);
        });
        buttonNo2.setOnClickListener(v -> {
            toggleRedFlagChoice("chest", false, buttonYes2, buttonNo2, buttonNext);
        });
        
        buttonYes3.setOnClickListener(v -> {
            toggleRedFlagChoice("lips", true, buttonYes3, buttonNo3, buttonNext);
        });
        buttonNo3.setOnClickListener(v -> {
            toggleRedFlagChoice("lips", false, buttonYes3, buttonNo3, buttonNext);
        });
        
        buttonNext.setOnClickListener(v -> {
            session.setRedFlags(redFlags);

            if (isRecheck) {
                boolean hasRedFlagNow = session.hasAnyRedFlag();
                if (hasRedFlagNow) {
                    checkAutoEscalation();
                }
            }

            showRescueStep();
        });
    }

    private void toggleRedFlagChoice(String flagKey, boolean isYes, Button buttonYes, Button buttonNo, Button buttonNext) {
        Boolean currentValue = redFlags.get(flagKey);
        
        if (currentValue != null && currentValue == isYes) {
            redFlags.remove(flagKey);
            resetButtonPair(buttonYes, buttonNo);
        } else {
            redFlags.put(flagKey, isYes);
            if (isYes) {
                highlightButton(buttonYes, buttonNo, true);
            } else {
                highlightButton(buttonNo, buttonYes, false);
            }
        }
        
        checkAllRedFlagsAnswered(buttonNext);
    }
    
    private void resetButtonPair(Button buttonYes, Button buttonNo) {
        int defaultColor = 0xFF6200EE;
        buttonYes.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
        buttonYes.setTextColor(0xFFFFFFFF);
        buttonNo.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
        buttonNo.setTextColor(0xFFFFFFFF);
    }
    
    private void checkAllRedFlagsAnswered(Button buttonNext) {
        if (redFlags.size() == 3) {
            buttonNext.setVisibility(View.VISIBLE);
        } else {
            buttonNext.setVisibility(View.GONE);
        }
    }

    private void showRescueStep() {
        currentStep = STEP_RESCUE;
        LayoutInflater inflater = LayoutInflater.from(this);
        currentStepView = inflater.inflate(R.layout.layout_rescue_attempts, frameLayoutSteps, false);
        frameLayoutSteps.removeAllViews();
        frameLayoutSteps.addView(currentStepView);
        
        Button buttonYes = currentStepView.findViewById(R.id.buttonRescueYes);
        Button buttonNo = currentStepView.findViewById(R.id.buttonRescueNo);
        TextView textViewCountLabel = currentStepView.findViewById(R.id.textViewRescueCountLabel);
        EditText editTextCount = currentStepView.findViewById(R.id.editTextRescueCount);
        Button buttonNext = currentStepView.findViewById(R.id.buttonNextRescue);
        
        buttonYes.setOnClickListener(v -> {
            if (rescueAttempts) {
                rescueAttempts = false;
                resetButtonPair(buttonYes, buttonNo);
                textViewCountLabel.setVisibility(View.GONE);
                editTextCount.setVisibility(View.GONE);
                editTextCount.setText("");
                buttonNext.setVisibility(View.GONE);
                rescueCount = 0;
            } else {
                rescueAttempts = true;
                highlightButton(buttonYes, buttonNo, true);
                textViewCountLabel.setVisibility(View.VISIBLE);
                editTextCount.setVisibility(View.VISIBLE);
                buttonNext.setVisibility(View.VISIBLE);
            }
        });
        
        buttonNo.setOnClickListener(v -> {
            rescueAttempts = false;
            highlightButton(buttonNo, buttonYes, false);
            rescueCount = 0;
            textViewCountLabel.setVisibility(View.GONE);
            editTextCount.setVisibility(View.GONE);
            editTextCount.setText("");
            buttonNext.setVisibility(View.VISIBLE);
        });
        
        buttonNext.setOnClickListener(v -> {
            if (rescueAttempts) {
                String countText = editTextCount.getText().toString().trim();
                if (countText.isEmpty()) {
                    Toast.makeText(this, "Please enter rescue count", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    rescueCount = Integer.parseInt(countText);
                    if (rescueCount < 1 || rescueCount > 10) {
                        Toast.makeText(this, "Please enter a number between 1 and 10", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    return;
                }
                session.setRescueAttempts(true);
                session.setRescueCount(rescueCount);
            } else {
                session.setRescueAttempts(false);
                session.setRescueCount(0);
            }
            showPEFStep();
        });
    }

    private void showPEFStep() {
        currentStep = STEP_PEF;
        LayoutInflater inflater = LayoutInflater.from(this);
        currentStepView = inflater.inflate(R.layout.layout_pef_triage, frameLayoutSteps, false);
        frameLayoutSteps.removeAllViews();
        frameLayoutSteps.addView(currentStepView);
        
        EditText editTextPEF = currentStepView.findViewById(R.id.editTextPEFTriage);
        Button buttonSkip = currentStepView.findViewById(R.id.buttonSkipPEF);
        Button buttonNext = currentStepView.findViewById(R.id.buttonNextPEF);
        
        buttonSkip.setOnClickListener(v -> {
            pefValue = null;
            session.setPefValue(null);
            calculateCurrentZone();
        });
        
        editTextPEF.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().trim();
                if (!text.isEmpty()) {
                    try {
                        int value = Integer.parseInt(text);
                        if (value > 0 && value <= 800) {
                            buttonNext.setVisibility(View.VISIBLE);
                        } else {
                            buttonNext.setVisibility(View.GONE);
                        }
                    } catch (NumberFormatException e) {
                        buttonNext.setVisibility(View.GONE);
                    }
                } else {
                    buttonNext.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        
        buttonNext.setOnClickListener(v -> {
            String pefText = editTextPEF.getText().toString().trim();
            if (pefText.isEmpty()) {
                Toast.makeText(this, "Please enter a PEF value or skip", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int value = Integer.parseInt(pefText);
                if (value <= 0 || value > 800) {
                    Toast.makeText(this, "PEF value must be between 1 and 800 L/min", Toast.LENGTH_SHORT).show();
                    return;
                }
                pefValue = value;
                session.setPefValue(pefValue);
                
                if (pefValue != null) {
                    savePEFReading(pefValue);
                }
                
                calculateCurrentZone();
                showDecisionStep();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePEFReading(int value) {
        long timestamp = System.currentTimeMillis();
        PEFReading reading = new PEFReading(value, timestamp, false, false, "From triage");
        
        String parentId = childAccount.getParent_id();
        String childId = childAccount.getID();
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("pefReadings")
                .child(String.valueOf(timestamp));
        
        pefRef.setValue(reading).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Check for alerts after PEF is saved
                String childName = childAccount.getName();
                Integer personalBest = childAccount.getPersonalBest();
                if (personalBest != null && personalBest > 0) {
                    AlertDetector.checkRedZoneDay(parentId, childId, childName, value, personalBest);
                    AlertDetector.checkWorseAfterDose(parentId, childId, childName, value, personalBest);
                }
            }
        });
    }

    private void calculateCurrentZone() {
        Integer personalBest = childAccount.getPersonalBest();
        if (personalBest == null || personalBest <= 0) {
            session.setCurrentZone(Zone.UNKNOWN);
            return;
        }
        
        if (pefValue != null) {
            Zone zone = ZoneCalculator.calculateZone(pefValue, personalBest);
            session.setCurrentZone(zone);
        } else {
            fetchLatestPEFAndCalculateZone(personalBest);
        }
    }
    
    private void fetchLatestPEFAndCalculateZone(Integer personalBest) {
        String parentId = childAccount.getParent_id();
        String childId = childAccount.getID();
        
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("pefReadings");
        
        pefRef.orderByChild("timestamp").limitToLast(1).get().addOnCompleteListener(task -> {
            Zone calculatedZone = Zone.UNKNOWN;
            if (task.isSuccessful() && task.getResult().hasChildren()) {
                for (com.google.firebase.database.DataSnapshot snapshot : task.getResult().getChildren()) {
                    PEFReading reading = snapshot.getValue(PEFReading.class);
                    if (reading != null) {
                        calculatedZone = ZoneCalculator.calculateZone(reading.getValue(), personalBest);
                        break;
                    }
                }
            }
            session.setCurrentZone(calculatedZone);
            runOnUiThread(() -> {
                showDecisionStep();
            });
        });
    }

    private void showDecisionStep() {
        currentStep = STEP_DECISION;
        LayoutInflater inflater = LayoutInflater.from(this);
        
        boolean hasRedFlag = session.hasAnyRedFlag();

        if (hasRedFlag && !isRecheck) {
            // Initial triage with critical flags → Emergency decision card only, no 10-minute timer
            showEmergencyDecisionCard(inflater);
            saveTriageIncident();
        } else {
            // Home-steps path and all 10-minute re-check sessions:
            // always show Home Steps + timer. For re-checks with red-flag symptoms,
            // parent notification is handled by checkAutoEscalation().
            showHomeStepsDecisionCard(inflater);
            saveTriageIncident();
        }
    }
    
    private void showEmergencyDecisionCard(LayoutInflater inflater) {
        currentStepView = inflater.inflate(R.layout.layout_decision_emergency, frameLayoutSteps, false);
        session.setDecisionShown("EMERGENCY");
        
        frameLayoutSteps.removeAllViews();
        frameLayoutSteps.addView(currentStepView);
        
        Button buttonViewSteps = currentStepView.findViewById(R.id.buttonViewSteps);
        if (buttonViewSteps != null) {
            buttonViewSteps.setOnClickListener(v -> {
                showHomeStepsDecisionCard(inflater);
            });
        }
        
        sendTriageEscalationNotification();
    }

    private void sendTriageEscalationNotification() {
        if (childAccount == null) {
            return;
        }
        String childName = childAccount.getName();
        com.example.myapplication.notifications.AlertDetector.checkTriageEscalation(
                childAccount.getParent_id(),
                childAccount.getID(),
                childName
        );
    }
    
    private void showHomeStepsDecisionCard(LayoutInflater inflater) {
        currentStepView = inflater.inflate(R.layout.layout_decision_home_steps, frameLayoutSteps, false);
        session.setDecisionShown("HOME_STEPS");
        
        TextView textViewZoneInfo = currentStepView.findViewById(R.id.textViewZoneInfo);
        TextView textViewSteps = currentStepView.findViewById(R.id.textViewSteps);
        
        Zone zone = session.getCurrentZone();
        if (zone == null) {
            zone = Zone.UNKNOWN;
        }
        
        if (zone == Zone.UNKNOWN) {
            textViewZoneInfo.setText("Zone: Not Available");
        } else {
            textViewZoneInfo.setText("Current Zone: " + zone.getDisplayName());
            textViewZoneInfo.setTextColor(zone.getColorResource());
        }
        
        String steps = getActionPlanSteps(zone);
        textViewSteps.setText(steps);
        
        frameLayoutSteps.removeAllViews();
        frameLayoutSteps.addView(currentStepView);

        // Always attach a fresh TimerFragment so the 10-minute countdown restarts
        // every time Home Steps are shown (initial decision and each re-check loop).
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.timerContainer, new TimerFragment())
                .commitAllowingStateLoss();
    }

    private String getActionPlanSteps(Zone zone) {
        // Use cached action plan if available
        if (cachedActionPlans != null && cachedActionPlans.containsKey(zone)) {
            return cachedActionPlans.get(zone);
        }

        // Fallback to default steps if no custom plan exists
        switch (zone) {
            case GREEN:
                return "1. Continue current medication as prescribed\n2. Monitor symptoms regularly\n3. Follow your regular routine\n4. Keep rescue medication available\n5. Contact healthcare provider if symptoms change";
            case YELLOW:
                return "1. Use rescue medication as prescribed by your healthcare provider\n2. Monitor symptoms closely every 15-30 minutes\n3. Rest in a comfortable position\n4. Avoid known triggers (dust, pollen, exercise, etc.)\n5. Contact your healthcare provider if symptoms do not improve within 1 hour\n6. Be prepared to seek emergency care if symptoms worsen";
            case RED:
                return "1. Use rescue medication immediately as prescribed\n2. Rest in a comfortable, upright position\n3. Monitor symptoms very closely\n4. Contact your healthcare provider right away\n5. Be prepared to seek emergency care immediately if symptoms do not improve or worsen\n6. Have someone stay with you if possible\n7. Keep emergency contact numbers readily available";
            default:
                return "1. Follow your personalized action plan\n2. Monitor symptoms closely\n3. Use rescue medication as prescribed\n4. Contact your healthcare provider if you have concerns\n5. Seek emergency care if symptoms become severe";
        }
    }

    private void saveTriageIncident() {
        TriageIncident incident = new TriageIncident();
        incident.setTimestamp(session.getStartTime());
        incident.setRedFlags(session.getRedFlags());
        incident.setRescueAttempts(session.isRescueAttempts());
        incident.setRescueCount(session.getRescueCount());
        incident.setPefValue(session.getPefValue());
        incident.setDecisionShown(session.getDecisionShown());
        incident.setZone(session.getCurrentZone());
        incident.setEscalated(session.isEscalated());
        incident.setSessionId(session.getSessionId());
        
        String parentId = childAccount.getParent_id();
        String childId = childAccount.getID();
        DatabaseReference incidentRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("incidents")
                .child(String.valueOf(incident.getTimestamp()));
        
        incidentRef.setValue(incident);
        
        if (session.isRescueAttempts() && session.getRescueCount() > 0) {
            saveRescueUsage(session.getRescueCount());
        }
    }

    private void saveRescueUsage(int count) {
        for (int i = 0; i < count; i++) {
            DatabaseReference rescueRef = UserManager.mDatabase
                    .child("users")
                    .child(childAccount.getParent_id())
                    .child("children")
                    .child(childAccount.getID())
                    .child("rescueUsage")
                    .child(String.valueOf(System.currentTimeMillis() + i));
            
            RescueUsage usage = new RescueUsage();
            usage.setTimestamp(System.currentTimeMillis());
            usage.setCount(1);
            rescueRef.setValue(usage);
        }
        
        checkRapidRescueAlert();
    }

    private void checkRapidRescueAlert() {
        String parentId = childAccount.getParent_id();
        String childId = childAccount.getID();
        
        long threeHoursAgo = System.currentTimeMillis() - (3 * 60 * 60 * 1000);
        
        DatabaseReference rescueRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("rescueUsage");
        
        rescueRef.orderByChild("timestamp").startAt(threeHoursAgo).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                int count = 0;
                for (com.google.firebase.database.DataSnapshot snapshot : task.getResult().getChildren()) {
                    RescueUsage usage = snapshot.getValue(RescueUsage.class);
                    if (usage != null) {
                        count += usage.getCount();
                    }
                }
                
                if (count >= 3) {
                    sendRapidRescueAlert();
                }
            }
        });
    }

    private void sendRapidRescueAlert() {
        Toast.makeText(this, "Rapid rescue alert: rescue used 3+ times in 3 hours. Consider seeking medical care.", Toast.LENGTH_LONG).show();
        if (childAccount != null) {
            String childName = childAccount.getName();
            com.example.myapplication.notifications.AlertDetector.checkRapidRescue(
                    childAccount.getParent_id(),
                    childAccount.getID(),
                    childName
            );
        }
    }
    
    public void restartTriage() {
        previousSession = session;
        session = new TriageSession();
        redFlags = new HashMap<>();
        rescueAttempts = false;
        rescueCount = 0;
        pefValue = null;
        isRecheck = true;

        showRedFlagsStep();
    }

    private void checkAutoEscalation() {
        // After 10-minute re-check, if there is still any red-flag symptom,
        // notify parent with a worsening alert and mark session as escalated.
        if (session == null) {
            return;
        }

        if (session.hasAnyRedFlag()) {
            session.setEscalated(true);
            sendWorseningAlertToParent();
        }
    }

    private void sendWorseningAlertToParent() {
        if (childAccount == null) {
            Log.e(TAG, "sendWorseningAlertToParent: childAccount is null");
            return;
        }

        String parentId = childAccount.getParent_id();
        String childId = childAccount.getID();

        if (parentId == null || childId == null) {
            Log.e(TAG, "sendWorseningAlertToParent: parentId or childId is null");
            return;
        }

        DatabaseReference triageRef = UserManager.mDatabase
                .child("triageSessions")
                .child(parentId)
                .child(childId);

        long now = System.currentTimeMillis();
        Map<String, Object> update = new HashMap<>();
        update.put("worseningId", String.valueOf(now));
        update.put("worseningTime", now);
        update.put("worseningHasRedFlag", true);

        triageRef.updateChildren(update);

        Toast.makeText(
                this,
                "Symptoms are still present after the 10-minute check. Your parent has been notified.",
                Toast.LENGTH_LONG
        ).show();
    }
    
    private void highlightButton(Button selectedButton, Button otherButton, boolean isYes) {
        int selectedColor = 0xFFFFEB3B;
        int defaultColor = 0xFF6200EE;
        
        selectedButton.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
        selectedButton.setTextColor(0xFF000000);
        
        otherButton.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
        otherButton.setTextColor(0xFFFFFFFF);
    }
}

