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
            Button button13 = findViewById(R.id.button13);
            Button button6 = findViewById(R.id.button6);
            Button button10 = findViewById(R.id.button10);
            Button button11 = findViewById(R.id.button11);
            AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
                @Override
                public void onComplete(Achievement achievement) {
                    if (achievement == null) {
                        Achievement a = new Achievement(UserManager.currentUser.getID());
                        AchievementsModel.writeIntoDB(a, new CallBack() {
                            @Override
                            public void onComplete() {
                                button13.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerInstructions.class);
                                        startActivity(intent);
                                    }
                                });

                                button11.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerLogs.class);
                                        startActivity(intent);
                                    }
                                });
                            }
                        });
                    }
                    else if (!achievement.badges[2]){
                        if (achievement.checkBadge3()) {Toast.makeText(ChildInhalerMenu.this, "You've earned a new badge!", Toast.LENGTH_SHORT).show();
                            achievement.badges[2] = true;}
                        AchievementsModel.writeIntoDB(achievement, new CallBack() {
                            @Override
                            public void onComplete() {
                                button13.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerInstructions.class);
                                        startActivity(intent);
                                    }
                                });

                                button11.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerLogs.class);
                                        startActivity(intent);
                                    }
                                });
                            }
                        });
                    }
                    else{
                        button13.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerInstructions.class);
                                startActivity(intent);
                            }
                        });

                        button11.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerLogs.class);
                                startActivity(intent);
                            }
                        });
                    }
                }
            });
            button6.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerDailyStreak.class);
                    AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
                        @Override
                        public void onComplete(Achievement achievement) {
                            if (achievement == null) {Toast.makeText(ChildInhalerMenu.this, "Achievement Error (Probably frame perfect click)", Toast.LENGTH_SHORT).show();
                                startActivity(intent);
                            }
                            else if (!achievement.badges[0]){
                                if (achievement.checkBadge1()) {Toast.makeText(ChildInhalerMenu.this, "You've earned a new badge!", Toast.LENGTH_SHORT).show();
                                    achievement.badges[0] = true;}
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

            button10.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerUse.class);
                    startActivity(intent);
                }
            });
        }
}