package com.example.myapplication;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.service.controls.Control;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.TypedValue;
import android.graphics.Color;

import com.example.myapplication.SignIn.SignInView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

public class ChildInhalerLogs extends AppCompatActivity {
    private static final String TAG = "ChildInhalerLogs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inhaler_logs);
        
        // Check if user is logged in
        if (UserManager.currentUser == null) {
            Log.w(TAG, "No user logged in, redirecting to SignIn");
            Intent intent = new Intent(ChildInhalerLogs.this, SignInView.class);
            startActivity(intent);
            finish();
            return;
        }
        
        findViewById(R.id.logsbackbutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChildInhalerLogs.this, ChildInhalerMenu.class));
            }
        });
        RescueLogModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<HashMap<String, RescueLog>>() {
            @Override
            public void onComplete(HashMap<String, RescueLog> result) {
                addRescueLogs(new ArrayList<>(result.values()));
            }
        });
        ControllerLogModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<HashMap<String, ControllerLog>>() {
            @Override
            public void onComplete(HashMap<String, ControllerLog> result) {
                addControllerLogs(new ArrayList<>(result.values()));
            }
        });
    }
    private void addRescueLogs(ArrayList<RescueLog> logs) {
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
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP,18);
            title.setTextColor(0xFF000000);
            logItem.addView(title);
            content.setText(log.getInfo());
            content.setTextSize(TypedValue.COMPLEX_UNIT_DIP,14);
            content.setTextColor(0xFF000000);
            logItem.addView(content);
            ((LinearLayout)findViewById(R.id.linearlayout1)).addView(logItem);
        }
    }
    private void addControllerLogs(ArrayList<ControllerLog> logs) {
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
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP,18);
            title.setTextColor(0xFF000000);
            logItem.addView(title);
            content.setText(log.getInfo());
            content.setTextSize(TypedValue.COMPLEX_UNIT_DIP,14);
            content.setTextColor(0xFF000000);
            logItem.addView(content);
            ((LinearLayout)findViewById(R.id.linearlayout2)).addView(logItem);
        }
    }
}