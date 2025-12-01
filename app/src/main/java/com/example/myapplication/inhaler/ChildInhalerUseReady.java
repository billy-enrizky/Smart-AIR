package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.app.AppCompatActivity;

public class ChildInhalerUseReady extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Intent intent2 = new Intent(ChildInhalerUseReady.this, ChildInhalerUseAfter.class);
        intent2.putExtra("breathrating",intent.getIntExtra("breathrating",1));
        intent2.putExtra("feelrating",intent.getStringExtra("feelrating"));
        setContentView(R.layout.activity_inhaler_use_ready);
        findViewById(R.id.backbutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChildInhalerUseReady.this, ChildInhalerUseController.class));
            }
        });
        findViewById(R.id.donebutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(intent2);
            }
        });
    }
}
