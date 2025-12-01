package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChildInhalerBadge extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inhaler_badge);

        TextView badge1Text = findViewById(R.id.badge1);
        TextView badge2Text = findViewById(R.id.badge2);
        TextView badge3Text = findViewById(R.id.badge3);

        AchievementsModel.readFromDB(UserManager.currentUser.getID(), new ResultCallBack<Achievement>() {
            @Override
            public void onComplete(Achievement achievement) {

                if (achievement == null) {Toast.makeText(ChildInhalerBadge.this, "Achievement Error (Probably frame perfect click)", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ChildInhalerBadge.this, ChildInhalerDailyStreak.class));
                }
                else {
                    badge1Text.setText("Gain this badge by reaching a streak of " + String.valueOf(achievement.badgeRequirements.get(0)) + ".");
                    badge2Text.setText("Gain this badge by practicing technique " + String.valueOf(achievement.badgeRequirements.get(1)) + " times every "+String.valueOf(achievement.badgeRequirements.get(2))+" days.");
                    badge3Text.setText("Gain this badge by " + String.valueOf(achievement.badgeRequirements.get(3)) + " or less rescues in " + String.valueOf(achievement.badgeRequirements.get(4)) + " days.");
                }
            }
        });

        Button back = findViewById(R.id.backbutton);
        back.setOnClickListener(v ->
                startActivity(new Intent(ChildInhalerBadge.this, ChildInhalerDailyStreak.class))
        );
    }
}
