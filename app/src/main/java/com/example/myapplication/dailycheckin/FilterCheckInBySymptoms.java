package com.example.myapplication.dailycheckin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.AccountType;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;

public class FilterCheckInBySymptoms extends AppCompatActivity {

    private ChipGroup nightWakingChipsFilter;
    private ChipGroup activityLimitsChipsFilter;
    private CheckBox coughWheezeLevelFilterInput;
    private Slider coughWheezeLevelSliderFilter;
    private TextView triggersChipsFilterTitle;
    private TextView nightWakingChipsFilterTitle;
    private TextView activityLimitsFilterTitle;
    private ChipGroup triggersChipsFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter_check_in_history_symptoms);

        // symptoms
        nightWakingChipsFilterTitle = (TextView)findViewById(R.id.night_waking_filter_title);
        nightWakingChipsFilter = (ChipGroup)findViewById(R.id.night_waking_filter);
        activityLimitsFilterTitle = (TextView)findViewById(R.id.activity_limits_filter_title);
        activityLimitsChipsFilter = (ChipGroup)findViewById(R.id.activity_limits_filter);
        coughWheezeLevelFilterInput = (CheckBox)findViewById(R.id.cough_wheeze_filter_check);
        coughWheezeLevelSliderFilter = (Slider)findViewById(R.id.cough_wheeze_slider_filter);

        // triggers
        triggersChipsFilterTitle = (TextView)findViewById(R.id.select_triggers_title);
        triggersChipsFilter = (ChipGroup)findViewById(R.id.triggers_filter);
        Intent intent = getIntent();

        if (UserManager.currentUser.getAccount().equals(AccountType.PROVIDER) && !intent.hasExtra("permissionToSymptoms")) {
            nightWakingChipsFilterTitle.setVisibility(View.INVISIBLE);
            nightWakingChipsFilter.setVisibility(View.INVISIBLE);
            activityLimitsFilterTitle.setVisibility(View.INVISIBLE);
            activityLimitsChipsFilter.setVisibility(View.INVISIBLE);
            coughWheezeLevelFilterInput.setVisibility(View.INVISIBLE);
            coughWheezeLevelSliderFilter.setVisibility(View.INVISIBLE);
        } else {
            nightWakingChipsFilterTitle.setVisibility(View.VISIBLE);
            nightWakingChipsFilter.setVisibility(View.VISIBLE);
            activityLimitsFilterTitle.setVisibility(View.VISIBLE);
            activityLimitsChipsFilter.setVisibility(View.VISIBLE);
            coughWheezeLevelFilterInput.setVisibility(View.VISIBLE);
            coughWheezeLevelSliderFilter.setVisibility(View.VISIBLE);
        }

        if (UserManager.currentUser.getAccount().equals(AccountType.PROVIDER) && !intent.hasExtra("permissionToTriggers")) {
            triggersChipsFilterTitle.setVisibility(View.INVISIBLE);
            triggersChipsFilter.setVisibility(View.INVISIBLE);
        } else {
            triggersChipsFilterTitle.setVisibility(View.VISIBLE);
            triggersChipsFilter.setVisibility(View.VISIBLE);
        }
    }

    public void goBackToDateButton (View view) {
        Intent intent = new Intent(this, FilterCheckInByDate.class);
        startActivity(intent);
    }
    public void viewHistoryButton(View view) {
        CheckInHistoryFilters filters = CheckInHistoryFilters.getInstance();
        if (nightWakingChipsFilter.getCheckedChipId() != View.NO_ID) {
            filters.setNightWakingInput(true);
            if (nightWakingChipsFilter.getCheckedChipId() == R.id.yes_chip) {
                filters.setNightWaking(true);
            } else if (nightWakingChipsFilter.getCheckedChipId() == R.id.no_chip) {
                filters.setNightWaking(false);
            }
        } else {
            filters.setNightWakingInput(false);
        }

        if (activityLimitsChipsFilter.getCheckedChipId() != View.NO_ID) {
            Chip selectedActivityLimitFilter = activityLimitsChipsFilter.findViewById(activityLimitsChipsFilter.getCheckedChipId());
            String selectedActivityLimitFilterText = selectedActivityLimitFilter.getText().toString();
            filters.setActivityLimits(selectedActivityLimitFilterText);
        } else {
            filters.setActivityLimits(null);
        }
        if (coughWheezeLevelFilterInput.isChecked()) {
            filters.setCoughWheezeLevel(coughWheezeLevelSliderFilter.getValue());
        } else {
            filters.setCoughWheezeLevel(-1);
        }

        ArrayList<Integer> triggerIdsFilter = (ArrayList<Integer>) triggersChipsFilter.getCheckedChipIds();
        ArrayList<String> triggersFilter = new ArrayList<String>();
        for (Integer id: triggerIdsFilter) {
            Chip triggerChip = triggersChipsFilter.findViewById(id);
            triggersFilter.add(triggerChip.getText().toString());
        }
        filters.setTriggers(triggersFilter);
        Intent intent = new Intent(this, ViewCheckInHistory.class);
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
}
