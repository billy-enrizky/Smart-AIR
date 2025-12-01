package com.example.myapplication;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.userdata.ChildAccount;

public class ChildInhalerUseRescue extends AppCompatActivity {
    RescueLog rescueLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rescueLog = new RescueLog();
        rescueLog.setFeeling("Better");
        rescueLog.setRating(1);
        rescueLog.setUsername(UserManager.currentUser.getID());
        setContentView(R.layout.activity_inhaler_use_rescue);

        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rescueLog.setRating(progress + 1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Button happyButton = findViewById(R.id.happybutton);
        Button neutralButton = findViewById(R.id.neutralbutton);
        Button sadButton = findViewById(R.id.sadbutton);
        Button confirmButton = findViewById(R.id.confirmbutton);
        Button lowButton = findViewById(R.id.lowbutton);

        happyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                happyButton.setBackgroundTintList(ColorStateList.valueOf(0xFF94D95F));
                neutralButton.setBackgroundTintList(ColorStateList.valueOf(0xFFFFD498));
                sadButton.setBackgroundTintList(ColorStateList.valueOf(0xFFFF7B7B));
                rescueLog.setFeeling("Better");
            }
        });

        neutralButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                happyButton.setBackgroundTintList(ColorStateList.valueOf(0xFFAEFF70));
                neutralButton.setBackgroundTintList(ColorStateList.valueOf(0xFFD8B481));
                sadButton.setBackgroundTintList(ColorStateList.valueOf(0xFFFF7B7B));
                rescueLog.setFeeling("Same");
            }
        });

        sadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                happyButton.setBackgroundTintList(ColorStateList.valueOf(0xFFAEFF70));
                neutralButton.setBackgroundTintList(ColorStateList.valueOf(0xFFFFD498));
                sadButton.setBackgroundTintList(ColorStateList.valueOf(0xFFD86868));
                rescueLog.setFeeling("Worse");
            }
        });

        findViewById(R.id.backbutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChildInhalerUseRescue.this, ChildInhalerUse.class));
            }
        });

        lowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rescueLog.getExtraInfo().isEmpty()) {
                    rescueLog.setExtraInfo("Low Inhaler Warning!");
                    lowButton.setBackgroundTintList(ColorStateList.valueOf(0xFFFF0000));
                } else {
                    rescueLog.setExtraInfo("");
                    lowButton.setBackgroundTintList(ColorStateList.valueOf(0xFFFF4646));
                }
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RescueLogModel.writeIntoDB(rescueLog, new CallBack() {
                    @Override
                    public void onComplete() {
                        InhalerModel.ReadFromDatabase(UserManager.currentUser.getID(), true, new ResultCallBack<Inhaler>() {
                            @Override
                            public void onComplete(Inhaler inhaler) {
                                if (inhaler != null) {
                                    inhaler.oneDose();
                                    InhalerModel.writeIntoDB(inhaler, new CallBack() {
                                        @Override
                                        public void onComplete() {
                                            AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
                                                @Override
                                                public void onComplete(Achievement achievement) {
                                                    if (achievement == null) {
                                                        Toast.makeText(ChildInhalerUseRescue.this, "Warning: Achievement Error.", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(ChildInhalerUseRescue.this, ChildInhalerMenu.class));
                                                    } else {
                                                        achievement.usedRescue();
                                                        AchievementsModel.writeIntoDB(achievement, new CallBack() {
                                                            @Override
                                                            public void onComplete() {
                                                                startActivity(new Intent(ChildInhalerUseRescue.this, ChildInhalerMenu.class));
                                                            }
                                                        });
                                                    }
                                                }
                                            });
                                        }
                                    });
                                } else {
                                    Toast.makeText(getApplicationContext(), "Can't find inhaler", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });
            }
        });
    }
}
