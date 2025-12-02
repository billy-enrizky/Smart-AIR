package com.example.myapplication.dailycheckin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.applandeo.materialcalendarview.listeners.OnDayClickListener;
import com.example.myapplication.ParentActivity;
import com.example.myapplication.R;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import com.example.myapplication.providers.AccessInfoActivity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FilterCheckInByDate extends AppCompatActivity {

    private static final String TAG = "FilterCheckInByDate";
    
    CalendarView range;
    Button goToSymptoms;
    Button goBackChild;
    Button goBackProvider;
    
    private boolean isFirstSelection = true;
    private Calendar initialEndDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter_check_in_history_date);
        this.range = (CalendarView)findViewById(R.id.selectDateRange);
        this.goToSymptoms = (Button)findViewById(R.id.go_to_filter_by_symptoms);
        goBackChild = (Button)findViewById(R.id.back_to_sign_in_child);
        goBackProvider = (Button)findViewById(R.id.providerback);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("isProvider")) {
            goBackChild.setVisibility(View.GONE);
            goBackProvider.setVisibility(View.VISIBLE);
        } else {
            goBackChild.setVisibility(View.VISIBLE);
            goBackProvider.setVisibility(View.GONE);
        }
        
        setupDateSelectionListener();
    }
    
    private void setupDateSelectionListener() {
        range.setOnDayClickListener(new OnDayClickListener() {
            @Override
            public void onDayClick(EventDay eventDay) {
                if (!isFirstSelection) {
                    // After first selection, allow normal range adjustment
                    Log.d(TAG, "Date range adjusted by user");
                    return;
                }
                
                // First date clicked becomes the end date
                Calendar clickedDate = eventDay.getCalendar();
                initialEndDate = (Calendar) clickedDate.clone();
                
                // Calculate start date as 90 days before the end date
                Calendar startDate = (Calendar) clickedDate.clone();
                startDate.add(Calendar.DAY_OF_MONTH, -90);
                
                // Programmatically select the range by selecting all dates from start to end
                List<Calendar> datesToSelect = new ArrayList<>();
                Calendar current = (Calendar) startDate.clone();
                while (!current.after(initialEndDate)) {
                    datesToSelect.add((Calendar) current.clone());
                    current.add(Calendar.DAY_OF_MONTH, 1);
                }
                
                // Use post with a small delay to ensure the selection happens after the current selection is processed
                range.postDelayed(() -> {
                    try {
                        // Try to set selected dates - the Material Calendar View library
                        // may support setSelectedDates or we may need to use a different approach
                        // For range_picker type, we need to select the start date first, then end date
                        // Clear current selection first
                        range.setSelectedDates(new ArrayList<>());
                        
                        // Now set the full range
                        range.setSelectedDates(datesToSelect);
                        
                        isFirstSelection = false;
                        
                        Log.d(TAG, "First selection: End date set to " + 
                            String.format("%tF", initialEndDate.getTimeInMillis()) + 
                            ", Start date set to " + 
                            String.format("%tF", startDate.getTimeInMillis()));
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting date range programmatically. Library API may differ.", e);
                        // Fallback: Show calculated dates to user
                        Toast.makeText(FilterCheckInByDate.this, 
                            "Calculated range: " + String.format("%tF", startDate.getTimeInMillis()) + 
                            " to " + String.format("%tF", initialEndDate.getTimeInMillis()) + 
                            ". Please adjust manually if needed.", 
                            Toast.LENGTH_LONG).show();
                        // Mark as no longer first selection so user can manually adjust
                        isFirstSelection = false;
                    }
                }, 100); // Small delay to ensure the library has processed the click
            }
        });
    }

    public void filterBySymptomsButton(View view) {
        List<Calendar> selectedDates = range.getSelectedDates();
        if (selectedDates.isEmpty()) {
            Toast.makeText(this, "Please select dates", Toast.LENGTH_SHORT).show();
            return;
        }
        Calendar start = selectedDates.getFirst();
        Calendar end = selectedDates.getLast();
        BigInteger threeMonths = new BigInteger("7889238000");
        BigInteger sixMonths = new BigInteger("15778476000");
        BigInteger startToEnd = new BigInteger(String.valueOf(end.getTimeInMillis()-start.getTimeInMillis()));
        if (startToEnd.compareTo(threeMonths) < 0 || startToEnd.compareTo(sixMonths) > 0) {
            Toast.makeText(this, "Time not within 3-6 months", Toast.LENGTH_SHORT).show();
            return;
        }
        CheckInHistoryFilters filters = CheckInHistoryFilters.getInstance();
        filters.setStartDate(String.format("%tF", start.getTimeInMillis()));
        filters.setEndDate(String.format("%tF", end.getTimeInMillis()));
        Toast.makeText(this, filters.getStartDate() + " to " + filters.getEndDate(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(FilterCheckInByDate.this, FilterCheckInBySymptoms.class);
        Intent thisintent = getIntent();
        if(thisintent.hasExtra("permissionToTriggers")){
            intent.putExtra("permissionToTriggers", thisintent.getStringExtra("permissionToTriggers"));
        }
        if(thisintent.hasExtra("permissionToSymptoms")){
            intent.putExtra("permissionToSymptoms", thisintent.getStringExtra("permissionToSymptoms"));
        }
        if(thisintent.hasExtra("isProvider")){
            intent.putExtra("isProvider", thisintent.getStringExtra("isProvider"));
            intent.putExtra("childName", thisintent.getStringExtra("childName"));
        }
        startActivity(intent);
    }

    public void backToChildSignIn (View view) {
        Intent intent = new Intent(FilterCheckInByDate.this, ParentActivity.class);
        intent.putExtra("defaultTab", "children");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    public void backToProviderSignIn (View view) {
        Intent intent = new Intent(FilterCheckInByDate.this, AccessInfoActivity.class);
        startActivity(intent);
        finish();
    }
}
