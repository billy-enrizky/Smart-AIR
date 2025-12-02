package com.example.myapplication.safety;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.CallBack;
import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.google.firebase.database.DatabaseReference;

public class SetPersonalBestActivity extends AppCompatActivity {
    private static final String TAG = "SetPersonalBestActivity";
    
    private TextView textViewChildName;
    private TextView textViewCurrentPB;
    private EditText editTextPB;
    private Button buttonSave;
    private Button buttonCancel;
    
    private String childId;
    private String parentId;
    private ChildAccount childAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_personal_best);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        childId = getIntent().getStringExtra("childId");
        parentId = getIntent().getStringExtra("parentId");

        if (childId == null || parentId == null) {
            if (UserManager.currentUser instanceof ParentAccount) {
                ParentAccount parent = (ParentAccount) UserManager.currentUser;
                if (parent.getChildren() != null && !parent.getChildren().isEmpty()) {
                    childAccount = parent.getChildren().values().iterator().next();
                    childId = childAccount.getID();
                    parentId = childAccount.getParent_id();
                } else {
                    Toast.makeText(this, "No children found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } else {
                Toast.makeText(this, "Invalid child information", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            if (UserManager.currentUser instanceof ParentAccount) {
                ParentAccount parent = (ParentAccount) UserManager.currentUser;
                if (parent.getChildren() != null && parent.getChildren().containsKey(childId)) {
                    childAccount = parent.getChildren().get(childId);
                } else {
                    childAccount = new ChildAccount();
                    childAccount.setID(childId);
                    childAccount.setParent_id(parentId);
                    childAccount.ReadFromDatabase(parentId, childId, new CallBack() {
                        @Override
                        public void onComplete() {
                            setupUI();
                        }
                    });
                    return;
                }
            } else {
                Toast.makeText(this, "Only parents can set Personal Best", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        setupUI();
    }

    private void setupUI() {
        textViewChildName = findViewById(R.id.textViewChildName);
        textViewCurrentPB = findViewById(R.id.textViewCurrentPB);
        editTextPB = findViewById(R.id.editTextPB);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);

        if (childAccount != null) {
            textViewChildName.setText("Child: " + childAccount.getName());
            Integer currentPB = childAccount.getPersonalBest();
            if (currentPB != null && currentPB > 0) {
                textViewCurrentPB.setText("Current Personal Best: " + currentPB + " L/min");
                editTextPB.setText(String.valueOf(currentPB));
            } else {
                textViewCurrentPB.setText("No Personal Best set yet");
            }
        }

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePersonalBest();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void savePersonalBest() {
        String pbText = editTextPB.getText().toString().trim();
        
        if (pbText.isEmpty()) {
            Toast.makeText(this, "Please enter a Personal Best value", Toast.LENGTH_SHORT).show();
            return;
        }

        int pbValue;
        try {
            pbValue = Integer.parseInt(pbText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pbValue < 100 || pbValue > 800) {
            Toast.makeText(this, "Personal Best must be between 100 and 800 L/min", Toast.LENGTH_SHORT).show();
            return;
        }

        if (childAccount == null) {
            Toast.makeText(this, "Child information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer previousPB = childAccount.getPersonalBest();
        childAccount.setPersonalBest(pbValue);
        
        childAccount.WriteIntoDatabase(new CallBack() {
            @Override
            public void onComplete() {
                Toast.makeText(SetPersonalBestActivity.this, "Personal Best saved successfully", Toast.LENGTH_SHORT).show();
                
                if (previousPB != null && !previousPB.equals(pbValue)) {
                    logPBChange(previousPB, pbValue);
                }
                
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void logPBChange(Integer previousPB, Integer newPB) {
        DatabaseReference historyRef = UserManager.mDatabase
                .child("users")
                .child(childAccount.getParent_id())
                .child("children")
                .child(childAccount.getID())
                .child("history");
        
        ZoneChangeEvent event = new ZoneChangeEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setPreviousZone(Zone.UNKNOWN);
        event.setNewZone(Zone.UNKNOWN);
        event.setPefValue(0);
        event.setPercentage(0.0);
        
        historyRef.child(String.valueOf(event.getTimestamp())).setValue(event);
    }
}

