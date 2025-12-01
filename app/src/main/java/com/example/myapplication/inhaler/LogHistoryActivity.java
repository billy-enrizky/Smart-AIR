package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import com.example.myapplication.safety.PEFHistoryActivity;

public class LogHistoryActivity extends AppCompatActivity {
    Button viewRescueControllerLog;
    Button viewPEFLog;
    Button backToChildActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_child_log_entry);
        viewRescueControllerLog = (Button)findViewById(R.id.rescueLogButton);
        viewRescueControllerLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogHistoryActivity.this, ChildInhalerLogs.class);
                startActivity(intent);
                finish();
            }
        });
        viewPEFLog = (Button)findViewById(R.id.PEFLogButton);
        viewPEFLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogHistoryActivity.this, PEFHistoryActivity.class);
                startActivity(intent);
                finish();
            }
        });
        backToChildActivity = (Button)findViewById(R.id.backToChildActivity);
        backToChildActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogHistoryActivity.this, ChildActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }
}
