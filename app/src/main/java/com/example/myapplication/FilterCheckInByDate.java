/*package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.applandeo.materialcalendarview.CalendarView;

import java.util.Calendar;
import java.util.List;

public class FilterCheckInByDate extends AppCompatActivity {

    CalendarView range;
    Button goToSymptoms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.filter_check_in_history_date);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        this.range = (CalendarView)findViewById(R.id.selectDateRange);
        this.goToSymptoms = (Button)findViewById(R.id.go_to_filter_by_symptoms);
    }

    public void filterBySymptomsButton(View view) {
        List<Calendar> selectedDates = range.getSelectedDates();


    }
}*/
