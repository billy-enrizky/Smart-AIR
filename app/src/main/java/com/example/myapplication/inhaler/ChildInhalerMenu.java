package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChildInhalerMenu extends AppCompatActivity {

    private Achievement achievementData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inhaler_main_child);
        Button techniqueButton = findViewById(R.id.techniquebutton);
        Button streakButton = findViewById(R.id.streakbutton);
        Button useButton = findViewById(R.id.usebutton);
        Button logsButton = findViewById(R.id.logsButton);

        streakButton.setEnabled(false);

        AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
            @Override
            public void onComplete(Achievement achievement) {
                if (achievement == null) {
                    Achievement newAch = new Achievement(UserManager.currentUser.getID());
                    AchievementsModel.writeIntoDB(newAch, new CallBack() {
                        @Override
                        public void onComplete() {
                            achievementData = newAch;
                            streakButton.setEnabled(true);
                        }
                    });
                    return;
                }

                achievementData = achievement;

                if (!achievementData.badges.get(2)) {
                    if (achievementData.checkBadge3()) {
                        Toast.makeText(ChildInhalerMenu.this, "You've earned a new badge!", Toast.LENGTH_SHORT).show();
                        achievementData.badges.set(2, true);
                        AchievementsModel.writeIntoDB(achievementData, new CallBack() {
                            @Override
                            public void onComplete() {
                                streakButton.setEnabled(true);
                            }
                        });
                        return;
                    }
                }

                streakButton.setEnabled(true);
            }
        });

        techniqueButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ChildInhalerInstructions.class));
        });

        logsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ChildInhalerLogs.class));
        });

        useButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ChildInhalerUse.class));
        });

        streakButton.setOnClickListener(v -> {
            if (achievementData == null) {
                Toast.makeText(this, "Preparing data...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!achievementData.badges.get(0)) {
                if (achievementData.checkBadge1()) {
                    Toast.makeText(this, "You've earned a new badge!", Toast.LENGTH_SHORT).show();
                    achievementData.badges.set(0, true);
                    AchievementsModel.writeIntoDB(achievementData, new CallBack() {
                        @Override
                        public void onComplete() {
                            startActivity(new Intent(ChildInhalerMenu.this, ChildInhalerDailyStreak.class));
                        }
                    });
                    return;
                }
            }

            startActivity(new Intent(this, ChildInhalerDailyStreak.class));
        });
    }
}
