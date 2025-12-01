package com.example.myapplication;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChildInhalerMenu extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_inhaler_main_child);
            Button techniqueButton = findViewById(R.id.techniquebutton);
            Button streakButton = findViewById(R.id.streakbutton);
            Button useButton = findViewById(R.id.usebutton);
            Button logsButton = findViewById(R.id.logsButton);
            AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
                @Override
                public void onComplete(Achievement achievement) {
                    if (achievement == null) {
                        Achievement a = new Achievement(UserManager.currentUser.getID());
                        AchievementsModel.writeIntoDB(a, new CallBack() {
                            @Override
                            public void onComplete() {
                                techniqueButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerInstructions.class);
                                        startActivity(intent);
                                    }
                                });

                                logsButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerLogs.class);
                                        startActivity(intent);
                                    }
                                });
                                streakButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerDailyStreak.class);
                                        AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
                                            @Override
                                            public void onComplete(Achievement achievement) {
                                                if (achievement == null) {Toast.makeText(ChildInhalerMenu.this, "Achievement Error (Probably frame perfect click)", Toast.LENGTH_SHORT).show();
                                                    startActivity(intent);
                                                }
                                                else if (!achievement.badges.get(0)){
                                                    if (achievement.checkBadge1()) {Toast.makeText(ChildInhalerMenu.this, "You've earned a new badge!", Toast.LENGTH_SHORT).show();
                                                        achievement.badges.set(0,true);}
                                                    AchievementsModel.writeIntoDB(achievement, new CallBack() {
                                                        @Override
                                                        public void onComplete() {
                                                            startActivity(intent);
                                                        }
                                                    });
                                                }
                                                else{
                                                    startActivity(intent);
                                                }
                                            }
                                        });
                                        startActivity(intent);
                                    }
                                });

                                useButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerUse.class);
                                        startActivity(intent);
                                    }
                                });
                            }
                        });
                    }
                    else if (!achievement.badges.get(2)){
                        if (achievement.checkBadge3()) {Toast.makeText(ChildInhalerMenu.this, "You've earned a new badge!", Toast.LENGTH_SHORT).show();
                            achievement.badges.set(2,true);}
                        AchievementsModel.writeIntoDB(achievement, new CallBack() {
                            @Override
                            public void onComplete() {
                                techniqueButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerInstructions.class);
                                        startActivity(intent);
                                    }
                                });

                                logsButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerLogs.class);
                                        startActivity(intent);
                                    }
                                });
                                streakButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerDailyStreak.class);
                                        AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
                                            @Override
                                            public void onComplete(Achievement achievement) {
                                                if (achievement == null) {Toast.makeText(ChildInhalerMenu.this, "Achievement Error (Probably frame perfect click)", Toast.LENGTH_SHORT).show();
                                                    startActivity(intent);
                                                }
                                                else if (!achievement.badges.get(0)){
                                                    if (achievement.checkBadge1()) {Toast.makeText(ChildInhalerMenu.this, "You've earned a new badge!", Toast.LENGTH_SHORT).show();
                                                        achievement.badges.set(0,true);}
                                                    AchievementsModel.writeIntoDB(achievement, new CallBack() {
                                                        @Override
                                                        public void onComplete() {
                                                            startActivity(intent);
                                                        }
                                                    });
                                                }
                                                else{
                                                    startActivity(intent);
                                                }
                                            }
                                        });
                                        startActivity(intent);
                                    }
                                });

                                useButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerUse.class);
                                        startActivity(intent);
                                    }
                                });
                            }
                        });
                    }
                    else{
                        techniqueButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerInstructions.class);
                                startActivity(intent);
                            }
                        });

                        logsButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerLogs.class);
                                startActivity(intent);
                            }
                        });
                        streakButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerDailyStreak.class);
                                AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
                                    @Override
                                    public void onComplete(Achievement achievement) {
                                        if (achievement == null) {Toast.makeText(ChildInhalerMenu.this, "Achievement Error (Probably frame perfect click)", Toast.LENGTH_SHORT).show();
                                            startActivity(intent);
                                        }
                                        else if (!achievement.badges.get(0)){
                                            if (achievement.checkBadge1()) {Toast.makeText(ChildInhalerMenu.this, "You've earned a new badge!", Toast.LENGTH_SHORT).show();
                                                achievement.badges.set(0,true);}
                                            AchievementsModel.writeIntoDB(achievement, new CallBack() {
                                                @Override
                                                public void onComplete() {
                                                    startActivity(intent);
                                                }
                                            });
                                        }
                                        else{
                                            startActivity(intent);
                                        }
                                    }
                                });
                                startActivity(intent);
                            }
                        });

                        useButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerUse.class);
                                startActivity(intent);
                            }
                        });
                    }
                }
            });
        }
}