package com.example.myapplication.dailycheckin;

import static android.view.View.VISIBLE;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.myapplication.ChildActivity;
import com.example.myapplication.ParentActivity;
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
    private String username;
    private ImageView dailyCheckinChildBackground;
    private TextView dailyCheckinTitle;
    private CheckBox nightWakingCheck;
    private TextView activityLimitsChipsTitle;
    private ChipGroup activityLimitsChips;
    private TextView coughWheezeTitle;
    private Slider coughWheezeLevelSlider;
    private ChipGroup triggersChips;
    private Button backButton;
    private Button checkinButton;
    CheckInPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_checkin);
        dailyCheckinChildBackground = (ImageView) findViewById(R.id.daily_checkin_child_background);
        dailyCheckinTitle = (TextView) findViewById(R.id.daily_checkin_title);
        nightWakingCheck = (CheckBox) findViewById(R.id.night_waking);
        activityLimitsChipsTitle = (TextView) findViewById(R.id.activity_limits_title);
        activityLimitsChips = (ChipGroup) findViewById(R.id.activity_limits);
        coughWheezeTitle = (TextView) findViewById(R.id.cough_wheeze_title);
        coughWheezeLevelSlider = (Slider) findViewById(R.id.cough_wheeze_slider_filter);
        triggersChips = (ChipGroup) findViewById(R.id.triggers);
        backButton = (Button) findViewById(R.id.leaveCheckIn);
        checkinButton = (Button) findViewById(R.id.log_checkin);
        if (UserManager.currentUser.getAccount().equals(AccountType.CHILD)) {

            this.username = ((ChildAccount)UserManager.currentUser).getID();
            dailyCheckinChildBackground.setVisibility(View.VISIBLE);
            Typeface happyMonkeyFamily = ResourcesCompat.getFont(this, R.font.happy_monkey);
            dailyCheckinTitle.setTypeface(happyMonkeyFamily, Typeface.NORMAL);
            nightWakingCheck.setTypeface(happyMonkeyFamily, Typeface.NORMAL);
            activityLimitsChipsTitle.setTypeface(happyMonkeyFamily, Typeface.NORMAL);
            setChipGroupFont(activityLimitsChips, happyMonkeyFamily);
            coughWheezeTitle.setTypeface(happyMonkeyFamily, Typeface.NORMAL);
            setChipGroupFont(triggersChips, happyMonkeyFamily);
            int childCheckinButtonGreen = ContextCompat.getColor(this, R.color.child_checkin_button_green);
            backButton.setBackgroundColor(childCheckinButtonGreen);
            checkinButton.setBackgroundColor(childCheckinButtonGreen);

        } else {
            this.username = SignInChildProfileActivity.getCurrentChildUsername();
        }
        presenter = new CheckInPresenter(this, new CheckInModel());
        presenter.initialize();

    }

    public void goBack (android.view.View view) {
        if (UserManager.currentUser.getAccount().equals(AccountType.PARENT)) {
            Intent intent = new Intent(this, ParentActivity.class);
            intent.putExtra("defaultTab", "children");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, ChildActivity.class);
            startActivity(intent);
        }
        finish();
    }

    public void runCheckIn (android.view.View view) {
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
        presenter.logEntry(this.username, nightWakingCheck.isChecked(), selectedActivityLimitText, coughWheezeLevelSlider.getValue(), triggers);

    }
    public void showShortMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setChipGroupFont(ChipGroup chipGroup, Typeface typeface) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            chip.setTypeface(typeface, Typeface.NORMAL);
        }
    }

}
