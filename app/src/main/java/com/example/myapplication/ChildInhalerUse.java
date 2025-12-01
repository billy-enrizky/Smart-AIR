package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ChildInhalerUse extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inhaler_use);
        Button back = findViewById(R.id.backbutton);
        Button rescue = findViewById(R.id.rescuebutton);
        Button controller = findViewById(R.id.controllerbutton);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChildInhalerUse.this, ChildInhalerMenu.class);
                startActivity(intent);
            }
        });

        rescue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChildInhalerUse.this, ChildInhalerUseRescue.class);
                startActivity(intent);
            }
        });

        controller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChildInhalerUse.this, ChildInhalerUseController.class);
                startActivity(intent);
            }
        });
    }
}
