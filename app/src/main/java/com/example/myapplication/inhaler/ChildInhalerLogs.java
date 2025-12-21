package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.providers.AccessInfoActivity;
import com.example.myapplication.SignIn.SignInView;
import com.example.myapplication.utils.FirebaseKeyEncoder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ChildInhalerLogs extends AppCompatActivity {
    private static final String TAG = "ChildInhalerLogs";
    
    // Realtime listener references
    private DatabaseReference rescueLogRef;
    private DatabaseReference controllerLogRef;
    private ValueEventListener rescueLogListener;
    private ValueEventListener controllerLogListener;
    private String currentID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inhaler_logs);

        Intent intent = getIntent();
        String ID;

        if(intent.hasExtra("isProvider")){
            findViewById(R.id.logsbackbutton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(ChildInhalerLogs.this, AccessInfoActivity.class));
                }
            });
            ID = intent.getStringExtra("ID");
        } else {
            if (UserManager.currentUser == null) {
                Intent signInIntent = new Intent(ChildInhalerLogs.this, SignInView.class);
                startActivity(signInIntent);
                finish();
                return;
            }
            
            // Get childId and parentId from intent, UserManager, or SignInChildProfileActivity
            String childId = null;
            String parentId = null;
            if (intent != null && intent.hasExtra("childId") && intent.hasExtra("parentId")) {
                childId = intent.getStringExtra("childId");
                parentId = intent.getStringExtra("parentId");
            } else if (UserManager.currentUser instanceof com.example.myapplication.userdata.ChildAccount) {
                com.example.myapplication.userdata.ChildAccount childAccount = (com.example.myapplication.userdata.ChildAccount) UserManager.currentUser;
                childId = childAccount.getID();
                parentId = childAccount.getParent_id();
            } else if (com.example.myapplication.childmanaging.SignInChildProfileActivity.currentChild != null) {
                com.example.myapplication.userdata.ChildAccount currentChild = com.example.myapplication.childmanaging.SignInChildProfileActivity.currentChild;
                childId = currentChild.getID();
                parentId = currentChild.getParent_id();
            }
            
            final String finalChildId = childId;
            final String finalParentId = parentId;
            findViewById(R.id.logsbackbutton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent backIntent = new Intent(ChildInhalerLogs.this, LogHistoryActivity.class);
                    if (finalChildId != null && finalParentId != null) {
                        backIntent.putExtra("childId", finalChildId);
                        backIntent.putExtra("parentId", finalParentId);
                    }
                    startActivity(backIntent);
                }
            });
            ID = UserManager.currentUser.getID();
        }
        
        // Clear existing views
        LinearLayout rescueLayout = findViewById(R.id.linearlayout1);
        LinearLayout controllerLayout = findViewById(R.id.linearlayout2);
        if (rescueLayout != null) {
            rescueLayout.removeAllViews();
        }
        if (controllerLayout != null) {
            controllerLayout.removeAllViews();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        attachLogListeners();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        detachLogListeners();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        detachLogListeners();
    }
    
    private void attachLogListeners() {
        Intent intent = getIntent();
        String ID;
        
        if(intent.hasExtra("isProvider")){
            ID = intent.getStringExtra("ID");
        } else {
            if (UserManager.currentUser == null) {
                return;
            }
            ID = UserManager.currentUser.getID();
        }
        
        if (ID == null) {
            return;
        }
        
        currentID = ID;
        
        // Detach existing listeners first to prevent duplicates
        detachLogListeners();
        
        String encodedID = FirebaseKeyEncoder.encode(ID);
        
        // Attach realtime listener for rescue logs
        rescueLogRef = UserManager.mDatabase.child("RescueLogManager").child(encodedID);
        rescueLogListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<RescueLog> logs = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot s : snapshot.getChildren()) {
                        RescueLog record = s.getValue(RescueLog.class);
                        if (record != null) {
                            logs.add(record);
                        }
                    }
                    Log.d(TAG, "Loaded " + logs.size() + " rescue logs with realtime updates");
                }
                // Clear and rebuild rescue log views
                LinearLayout rescueLayout = findViewById(R.id.linearlayout1);
                if (rescueLayout != null) {
                    rescueLayout.removeAllViews();
                    addRescueLogs(logs);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading rescue logs", error.toException());
            }
        };
        rescueLogRef.addValueEventListener(rescueLogListener);
        
        // Attach realtime listener for controller logs
        controllerLogRef = UserManager.mDatabase.child("ControllerLogManager").child(encodedID);
        controllerLogListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<ControllerLog> logs = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot s : snapshot.getChildren()) {
                        ControllerLog record = s.getValue(ControllerLog.class);
                        if (record != null) {
                            logs.add(record);
                        }
                    }
                    Log.d(TAG, "Loaded " + logs.size() + " controller logs with realtime updates");
                }
                // Clear and rebuild controller log views
                LinearLayout controllerLayout = findViewById(R.id.linearlayout2);
                if (controllerLayout != null) {
                    controllerLayout.removeAllViews();
                    addControllerLogs(logs);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading controller logs", error.toException());
            }
        };
        controllerLogRef.addValueEventListener(controllerLogListener);
    }
    
    private void detachLogListeners() {
        if (rescueLogRef != null && rescueLogListener != null) {
            rescueLogRef.removeEventListener(rescueLogListener);
            rescueLogListener = null;
        }
        
        if (controllerLogRef != null && controllerLogListener != null) {
            controllerLogRef.removeEventListener(controllerLogListener);
            controllerLogListener = null;
        }
        
        rescueLogRef = null;
        controllerLogRef = null;
    }
    private void addRescueLogs(ArrayList<RescueLog> logs) {
        Collections.sort(logs);
        for (RescueLog log : logs) {
            LinearLayout logItem = new LinearLayout(this);
            logItem.setOrientation(LinearLayout.VERTICAL);
            logItem.setPadding(0, 16, 0, 16);
            logItem.setBackgroundColor(Color.argb(40,255,255,255));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 10, 0, 10);
            logItem.setLayoutParams(params);
            TextView title = new TextView(this);
            TextView content = new TextView(this);
            title.setText(log.getDate());
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP,15);
            title.setTextColor(0xFF000000);
            logItem.addView(title);
            content.setText(log.getInfo());
            content.setTextSize(TypedValue.COMPLEX_UNIT_DIP,12);
            content.setTextColor(0xFF000000);
            logItem.addView(content);
            ((LinearLayout)findViewById(R.id.linearlayout1)).addView(logItem);
        }
    }
    private void addControllerLogs(ArrayList<ControllerLog> logs) {
        Collections.sort(logs);
        for (ControllerLog log : logs) {
            LinearLayout logItem = new LinearLayout(this);
            logItem.setOrientation(LinearLayout.VERTICAL);
            logItem.setPadding(0, 16, 0, 16);
            logItem.setBackgroundColor(Color.argb(40,255,255,255));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 10, 0, 10);
            logItem.setLayoutParams(params);
            TextView title = new TextView(this);
            TextView content = new TextView(this);
            title.setText(log.getDate());
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP,15);
            title.setTextColor(0xFF000000);
            logItem.addView(title);
            content.setText(log.getInfo());
            content.setTextSize(TypedValue.COMPLEX_UNIT_DIP,12);
            content.setTextColor(0xFF000000);
            logItem.addView(content);
            ((LinearLayout)findViewById(R.id.linearlayout2)).addView(logItem);
        }
    }
}