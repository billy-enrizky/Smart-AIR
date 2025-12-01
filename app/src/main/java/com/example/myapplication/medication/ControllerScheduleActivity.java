package com.example.myapplication.medication;

import android.app.DatePickerDialog;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ControllerScheduleActivity extends AppCompatActivity {
    private static final String TAG = "ControllerScheduleActivity";

    private LinearLayout linearLayoutDates;
    private Button buttonAddDate;
    private Button buttonSave;
    private Button buttonCancel;
    private TextView textViewChildName;

    private String parentId;
    private String childId;
    private String childName;
    private List<String> scheduleDates;  // Dates in yyyy-MM-dd format
    private List<View> dateViews;

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

        linearLayoutDates = findViewById(R.id.linearLayoutDates);
        buttonAddDate = findViewById(R.id.buttonAddDate);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);
        textViewChildName = findViewById(R.id.textViewChildName);

        if (childName != null) {
            textViewChildName.setText("Child: " + childName);
        }

        scheduleDates = new ArrayList<>();
        dateViews = new ArrayList<>();

        buttonAddDate.setOnClickListener(v -> showDatePicker());

        buttonSave.setOnClickListener(v -> saveSchedule());

        buttonCancel.setOnClickListener(v -> finish());

        loadExistingSchedule();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(selectedYear, selectedMonth, selectedDayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String date = dateFormat.format(selectedCal.getTime());
                    
                    if (!scheduleDates.contains(date)) {
                        scheduleDates.add(date);
                        Collections.sort(scheduleDates);  // Keep dates sorted
                        refreshDateViews();
                    } else {
                        Toast.makeText(this, "This date is already added", Toast.LENGTH_SHORT).show();
                    }
                },
                year,
                month,
                day
        );

        datePickerDialog.show();
    }

    private void addDateView(String date) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dateView = inflater.inflate(R.layout.item_schedule_date, linearLayoutDates, false);

        TextView textViewDate = dateView.findViewById(R.id.textViewDate);
        // Format date for display (e.g., "Dec 02, 2025")
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            java.util.Date dateObj = inputFormat.parse(date);
            if (dateObj != null) {
                textViewDate.setText(displayFormat.format(dateObj));
            } else {
                textViewDate.setText(date);
            }
        } catch (Exception e) {
            textViewDate.setText(date);
        }

        Button buttonRemove = dateView.findViewById(R.id.buttonRemove);
        buttonRemove.setOnClickListener(v -> {
            scheduleDates.remove(date);
            linearLayoutDates.removeView(dateView);
            dateViews.remove(dateView);
        });

        linearLayoutDates.addView(dateView);
        dateViews.add(dateView);
    }

    private void refreshDateViews() {
        linearLayoutDates.removeAllViews();
        dateViews.clear();
        for (String date : scheduleDates) {
            addDateView(date);
        }
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
                    if (schedule.getDates() != null && !schedule.getDates().isEmpty()) {
                        scheduleDates.clear();
                        scheduleDates.addAll(schedule.getDates());
                        Collections.sort(scheduleDates);  // Keep dates sorted
                        refreshDateViews();
                    }
                }
            }
        });
    }

    private void saveSchedule() {
        if (scheduleDates.isEmpty()) {
            Toast.makeText(this, "Please add at least one scheduled date", Toast.LENGTH_SHORT).show();
            return;
        }

        ControllerSchedule schedule = new ControllerSchedule(scheduleDates);

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

