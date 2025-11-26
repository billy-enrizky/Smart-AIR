package com.example.myapplication.dailycheckin;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import com.example.myapplication.userdata.AccountType;
import com.example.myapplication.userdata.ChildAccount;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;

public class CheckInView extends AppCompatActivity {
    private CheckBox nightWakingCheck;
    private String username;
    private ChipGroup activityLimitsChips;
    private Slider coughWheezeLevelSlider;
    private ChipGroup triggersChips;
    CheckInPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_checkin);
        nightWakingCheck = (CheckBox) findViewById(R.id.night_waking);
        activityLimitsChips = (ChipGroup) findViewById(R.id.activity_limits);
        coughWheezeLevelSlider = (Slider) findViewById(R.id.cough_wheeze_slider);
        triggersChips = (ChipGroup) findViewById(R.id.triggers);
        /*if (UserManager.currentUser.getAccount().equals(AccountType.CHILD)) {
            this.username = ((ChildAccount)UserManager.currentUser).getID();
        } else {
            this.username = SignInChildProfileActivity.getCurrentChildUsername();
        }*/
        presenter = new CheckInPresenter(this, new CheckInModel());
        presenter.initialize();
    }

    public void goToCheckInActivity (android.view.View view) {
        int selectedActivityLimitId = activityLimitsChips.getCheckedChipId();
        if (selectedActivityLimitId == View.NO_ID) {
            showShortMessage("Must select an activity limit!");
            return;
        }
        Chip selectedActivityLimit = activityLimitsChips.findViewById(activityLimitsChips.getCheckedChipId());
        String selectedActivityLimitText = selectedActivityLimit.getText().toString();

        ArrayList<Integer> triggerIds = (ArrayList<Integer>) triggersChips.getCheckedChipIds();
        ArrayList<String> triggers = new ArrayList<String>();
        for (Integer id: triggerIds) {
            Chip triggerChip = triggersChips.findViewById(id);
            triggers.add(triggerChip.getText().toString());
        }
        CheckInPresenter.logEntry(this.username, nightWakingCheck.isChecked(), selectedActivityLimitText, coughWheezeLevelSlider.getValue(), triggers);
    }
    public void showShortMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
