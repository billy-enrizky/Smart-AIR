package com.example.myapplication.safety;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.database.DatabaseReference;
import java.util.Map;
import java.util.HashMap;

public class ActionPlanActivity extends AppCompatActivity {

    private static final String TAG = "ActionPlanActivity";

    private EditText editTextGreen;
    private EditText editTextYellow;
    private EditText editTextRed;

    private String parentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_action_plan);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("parentId")) {
            parentId = intent.getStringExtra("parentId");
        } else if (UserManager.currentUser instanceof com.example.myapplication.userdata.ParentAccount) {
            com.example.myapplication.userdata.ParentAccount parentAccount = (com.example.myapplication.userdata.ParentAccount) UserManager.currentUser;
            parentId = parentAccount.getID();
        } else if (UserManager.currentUser instanceof ChildAccount) {
            ChildAccount childAccount = (ChildAccount) UserManager.currentUser;
            parentId = childAccount.getParent_id();
        } else {
            Log.e(TAG, "No parentId provided and current user is not a ParentAccount or ChildAccount");
            finish();
            return;
        }

        editTextGreen = findViewById(R.id.editTextGreenPlan);
        editTextYellow = findViewById(R.id.editTextYellowPlan);
        editTextRed = findViewById(R.id.editTextRedPlan);
        Button buttonSave = findViewById(R.id.buttonSaveActionPlan);
        Button buttonBack = findViewById(R.id.buttonBackActionPlan);

        buttonBack.setOnClickListener(v -> finish());

        loadExistingPlans();

        buttonSave.setOnClickListener(v -> savePlans());
    }

    private void loadExistingPlans() {
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
                
                if (!TextUtils.isEmpty(greenPlan)) {
                    editTextGreen.setText(greenPlan);
                }
                if (!TextUtils.isEmpty(yellowPlan)) {
                    editTextYellow.setText(yellowPlan);
                }
                if (!TextUtils.isEmpty(redPlan)) {
                    editTextRed.setText(redPlan);
                }
            }

            // Ensure defaults are shown if nothing is configured yet
            if (TextUtils.isEmpty(editTextGreen.getText())) {
                editTextGreen.setText(getDefaultPlanForZone(Zone.GREEN));
            }
            if (TextUtils.isEmpty(editTextYellow.getText())) {
                editTextYellow.setText(getDefaultPlanForZone(Zone.YELLOW));
            }
            if (TextUtils.isEmpty(editTextRed.getText())) {
                editTextRed.setText(getDefaultPlanForZone(Zone.RED));
            }
        });
    }

    private String getDefaultPlanForZone(Zone zone) {
        switch (zone) {
            case GREEN:
                return "1. Continue current medication as prescribed\n"
                        + "2. Monitor symptoms regularly\n"
                        + "3. Follow your regular routine\n"
                        + "4. Keep rescue medication available\n"
                        + "5. Contact healthcare provider if symptoms change";
            case YELLOW:
                return "1. Use rescue medication as prescribed by your healthcare provider\n"
                        + "2. Monitor symptoms closely every 15-30 minutes\n"
                        + "3. Rest in a comfortable position\n"
                        + "4. Avoid known triggers (dust, pollen, exercise, etc.)\n"
                        + "5. Contact your healthcare provider if symptoms do not improve within 1 hour\n"
                        + "6. Be prepared to seek emergency care if symptoms worsen";
            case RED:
                return "1. Use rescue medication immediately as prescribed\n"
                        + "2. Rest in a comfortable, upright position\n"
                        + "3. Monitor symptoms very closely\n"
                        + "4. Contact your healthcare provider right away\n"
                        + "5. Be prepared to seek emergency care immediately if symptoms do not improve or worsen\n"
                        + "6. Have someone stay with you if possible\n"
                        + "7. Keep emergency contact numbers readily available";
            default:
                return "";
        }
    }

    private void savePlans() {
        String greenPlan = editTextGreen.getText().toString().trim();
        String yellowPlan = editTextYellow.getText().toString().trim();
        String redPlan = editTextRed.getText().toString().trim();

        DatabaseReference actionPlansRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("actionPlans");

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("green", greenPlan);
        updates.put("yellow", yellowPlan);
        updates.put("red", redPlan);

        actionPlansRef.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Action plan updated for all children", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Log.e(TAG, "Failed to update action plan", task.getException());
                        Toast.makeText(this, "Failed to update action plan", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}


