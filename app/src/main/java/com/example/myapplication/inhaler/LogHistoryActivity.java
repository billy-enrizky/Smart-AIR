package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.ChildActivity;
import com.example.myapplication.ChildInhalerLogs;
import com.example.myapplication.ParentBadge;
import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import com.example.myapplication.safety.PEFHistoryActivity;
import com.example.myapplication.userdata.ChildAccount;

public class LogHistoryActivity extends AppCompatActivity {
    private static final String TAG = "LogHistoryActivity";
    Button viewRescueControllerLog;
    Button viewPEFLog;
    Button backToChildActivity;
    private String childId;
    private String parentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_child_log_entry);
        
        // Get childId and parentId from intent, UserManager, or SignInChildProfileActivity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("childId") && intent.hasExtra("parentId")) {
            childId = intent.getStringExtra("childId");
            parentId = intent.getStringExtra("parentId");
        } else if (UserManager.currentUser instanceof ChildAccount) {
            ChildAccount childAccount = (ChildAccount) UserManager.currentUser;
            childId = childAccount.getID();
            parentId = childAccount.getParent_id();
        } else if (SignInChildProfileActivity.currentChild != null) {
            // When logged in via children manager
            ChildAccount currentChild = SignInChildProfileActivity.currentChild;
            childId = currentChild.getID();
            parentId = currentChild.getParent_id();
        } else {
            Log.e(TAG, "No childId/parentId available");
            Toast.makeText(this, "Unable to determine child context", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        viewRescueControllerLog = (Button)findViewById(R.id.rescueLogButton);
        viewRescueControllerLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogHistoryActivity.this, ChildInhalerLogs.class);
                if (childId != null && parentId != null) {
                    intent.putExtra("childId", childId);
                    intent.putExtra("parentId", parentId);
                }
                startActivity(intent);
            }
        });
        viewPEFLog = (Button)findViewById(R.id.PEFLogButton);
        viewPEFLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogHistoryActivity.this, PEFHistoryActivity.class);
                intent.putExtra("childId", childId);
                intent.putExtra("parentId", parentId);
                startActivity(intent);
                finish();
            }
        });
        backToChildActivity = (Button)findViewById(R.id.backToChildActivity);
        backToChildActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogHistoryActivity.this, ChildActivity.class);
                if (childId != null && parentId != null) {
                    intent.putExtra("childId", childId);
                    intent.putExtra("parentId", parentId);
                }
                startActivity(intent);
                finish();
            }
        });

    }
}
