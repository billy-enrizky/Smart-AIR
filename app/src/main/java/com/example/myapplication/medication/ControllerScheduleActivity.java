package com.example.myapplication.medication;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ControllerScheduleActivity extends AppCompatActivity {
    private static final String TAG = "ControllerScheduleActivity";

    private LinearLayout linearLayoutTimes;
    private Button buttonAddTime;
    private Button buttonSave;
    private Button buttonCancel;
    private TextView textViewChildName;

    private String parentId;
    private String childId;
    private String childName;
    private List<String> scheduleTimes;
    private List<View> timeViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_controller_schedule);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        parentId = getIntent().getStringExtra("parentId");
        childId = getIntent().getStringExtra("childId");
        childName = getIntent().getStringExtra("childName");

        if (parentId == null || childId == null) {
            Log.e(TAG, "Missing parentId or childId");
            finish();
            return;
        }

        linearLayoutTimes = findViewById(R.id.linearLayoutTimes);
        buttonAddTime = findViewById(R.id.buttonAddTime);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);
        textViewChildName = findViewById(R.id.textViewChildName);

        if (childName != null) {
            textViewChildName.setText("Child: " + childName);
        }

        scheduleTimes = new ArrayList<>();
        timeViews = new ArrayList<>();

        buttonAddTime.setOnClickListener(v -> showTimePicker());

        buttonSave.setOnClickListener(v -> saveSchedule());

        buttonCancel.setOnClickListener(v -> finish());

        loadExistingSchedule();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    if (!scheduleTimes.contains(time)) {
                        scheduleTimes.add(time);
                        addTimeView(time);
                    } else {
                        Toast.makeText(this, "This time is already added", Toast.LENGTH_SHORT).show();
                    }
                },
                hour,
                minute,
                true
        );

        timePickerDialog.show();
    }

    private void addTimeView(String time) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View timeView = inflater.inflate(R.layout.item_schedule_time, linearLayoutTimes, false);

        TextView textViewTime = timeView.findViewById(R.id.textViewTime);
        textViewTime.setText(time);

        Button buttonRemove = timeView.findViewById(R.id.buttonRemove);
        buttonRemove.setOnClickListener(v -> {
            scheduleTimes.remove(time);
            linearLayoutTimes.removeView(timeView);
            timeViews.remove(timeView);
        });

        linearLayoutTimes.addView(timeView);
        timeViews.add(timeView);
    }

    private void loadExistingSchedule() {
        DatabaseReference childRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId);

        childRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ChildAccount child = task.getResult().getValue(ChildAccount.class);
                if (child != null && child.getControllerSchedule() != null) {
                    ControllerSchedule schedule = child.getControllerSchedule();
                    if (schedule.getTimes() != null) {
                        scheduleTimes.clear();
                        scheduleTimes.addAll(schedule.getTimes());
                        linearLayoutTimes.removeAllViews();
                        timeViews.clear();
                        for (String time : scheduleTimes) {
                            addTimeView(time);
                        }
                    }
                }
            }
        });
    }

    private void saveSchedule() {
        if (scheduleTimes.isEmpty()) {
            Toast.makeText(this, "Please add at least one scheduled time", Toast.LENGTH_SHORT).show();
            return;
        }

        ControllerSchedule schedule = new ControllerSchedule(scheduleTimes);

        DatabaseReference childRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId);

        childRef.child("controllerSchedule").setValue(schedule).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Schedule saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Log.e(TAG, "Failed to save schedule", task.getException());
                Toast.makeText(this, "Failed to save schedule", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

