package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChildInhalerInstructions extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inhaler_instructions);

        Button backButton = findViewById(R.id.button8);
        Button watchVideoButton = findViewById(R.id.button7);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    startActivity(new Intent(ChildInhalerInstructions.this, ChildInhalerMenu.class));
            }
        });

        watchVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
                    @Override
                    public void onComplete(Achievement achievement) {
                        if (achievement == null) {Toast.makeText(ChildInhalerInstructions.this, "Warning: Achievement Error.", Toast.LENGTH_SHORT).show();}
                        else if (!achievement.badges.get(1)){
                            achievement.updateStreakTechnique();
                            if (achievement.checkBadge2()) {Toast.makeText(ChildInhalerInstructions.this, "You've earned a new badge!", Toast.LENGTH_SHORT).show();
                                achievement.badges.set(1, true);}
                            AchievementsModel.writeIntoDB(achievement, new CallBack() {
                                @Override
                                public void onComplete() {
                                    startActivity(new Intent(ChildInhalerInstructions.this, ChildInhalerVideo.class));
                                }
                            });
                        }
                        else{
                            startActivity(new Intent(ChildInhalerInstructions.this, ChildInhalerVideo.class));
                        }
                    }
                });
            }
        });
    }
}