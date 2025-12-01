package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.providers.AccessInfoActivity;
import com.example.myapplication.SignIn.SignInView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ChildInhalerLogs extends AppCompatActivity {

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
            findViewById(R.id.logsbackbutton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(ChildInhalerLogs.this, ChildInhalerMenu.class));
                }
            });
            ID = UserManager.currentUser.getID();
        }

        RescueLogModel.readFromDB(ID, new ResultCallBack<HashMap<String, RescueLog>>() {
            @Override
            public void onComplete(HashMap<String, RescueLog> result) {
                addRescueLogs(new ArrayList<>(result.values()));
            }
        });
        ControllerLogModel.readFromDB(ID, new ResultCallBack<HashMap<String, ControllerLog>>() {
            @Override
            public void onComplete(HashMap<String, ControllerLog> result) {
                addControllerLogs(new ArrayList<>(result.values()));
            }
        });
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