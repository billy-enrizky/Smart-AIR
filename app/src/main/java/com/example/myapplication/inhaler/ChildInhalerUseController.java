package com.example.myapplication;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ChildInhalerUseController extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inhaler_use_controller);
        SeekBar seekBar = findViewById(R.id.seekBar);
        Intent intent = new Intent(ChildInhalerUseController.this, ChildInhalerUseReady.class);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                intent.putExtra("breathrating",progress + 1);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        Button happyButton = findViewById(R.id.happybutton);
        Button neutralButton = findViewById(R.id.neutralbutton);
        Button sadButton = findViewById(R.id.sadbutton);
        Button readyButton = findViewById(R.id.readybutton);
        intent.putExtra("feelrating","Better");
        happyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                happyButton.setBackgroundTintList(ColorStateList.valueOf(0xFF94D95F));
                neutralButton.setBackgroundTintList(ColorStateList.valueOf(0xFFFFD498));
                sadButton.setBackgroundTintList(ColorStateList.valueOf(0xFFFF7B7B));
                intent.putExtra("feelrating","Better");
            }
        });

        neutralButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                happyButton.setBackgroundTintList(ColorStateList.valueOf(0xFFAEFF70));
                neutralButton.setBackgroundTintList(ColorStateList.valueOf(0xFFD8B481));
                sadButton.setBackgroundTintList(ColorStateList.valueOf(0xFFFF7B7B));
                intent.putExtra("feelrating","Same");
            }
        });

        sadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                happyButton.setBackgroundTintList(ColorStateList.valueOf(0xFFAEFF70));
                neutralButton.setBackgroundTintList(ColorStateList.valueOf(0xFFFFD498));
                sadButton.setBackgroundTintList(ColorStateList.valueOf(0xFFD86868));
                intent.putExtra("feelrating","Worse");
            }
        });

        readyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                startActivity(intent);
            }
        });

        findViewById(R.id.backbutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                startActivity(new Intent(ChildInhalerUseController.this, ChildInhalerUse.class));
            }
        });
    }
}
