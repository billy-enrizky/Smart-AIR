package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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

        // streak.setText(...)
        // badge1.setImageResource(...)
        // badge2.setImageResource(...)
        // badge3.setImageResource(...)

        donebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChildInhalerDailyStreak.this, ChildInhalerMenu.class));
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
