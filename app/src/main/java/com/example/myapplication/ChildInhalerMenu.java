package com.example.myapplication;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

            button6.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ChildInhalerMenu.this, ChildInhalerDailyStreak.class);
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