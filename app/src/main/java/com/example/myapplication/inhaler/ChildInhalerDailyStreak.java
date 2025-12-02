package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChildInhalerDailyStreak extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inhaler_streak);
        TextView streak;
        ImageView badge1;
        ImageView badge2;
        ImageView badge3;
        Button donebutton;
        Button badgeinfo;
        streak = findViewById(R.id.streak);
        badge1 = findViewById(R.id.badge1);
        badge2 = findViewById(R.id.badge2);
        badge3 = findViewById(R.id.badge3);
        donebutton = findViewById(R.id.donebutton);
        badgeinfo = findViewById(R.id.badgeinfo);
        AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
            @Override
            public void onComplete(Achievement achievement) {

                if (achievement == null) {
                    Toast.makeText(ChildInhalerDailyStreak.this,"Somehow no achievement.",Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ChildInhalerDailyStreak.this, ChildActivity.class));}
                else {
                    if (achievement.badges.get(0))
                        badge1.setImageResource(R.drawable.badge1);
                    if (achievement.badges.get(1))
                        badge2.setImageResource(R.drawable.badge2);
                    if (achievement.badges.get(2))
                        badge3.setImageResource(R.drawable.badge3);
                    streak.setText(String.valueOf(achievement.getCurrentStreak()));
                }
            }
        });

        donebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChildInhalerDailyStreak.this, ChildActivity.class));
            }
        });

        badgeinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChildInhalerDailyStreak.this, ChildInhalerBadge.class));
            }
        });
    }
}
