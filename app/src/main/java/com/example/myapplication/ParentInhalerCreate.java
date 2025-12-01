package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import android.widget.Toast;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.userdata.ChildAccount;

public class ParentInhalerCreate extends AppCompatActivity {
    Inhaler inhaler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_inhaler_create);

        EditText datePurchased = findViewById(R.id.datePurchased);
        EditText dateExpiry = findViewById(R.id.dateExpiry);
        EditText dosecount = findViewById(R.id.dosecount);
        EditText maxcapacity = findViewById(R.id.maxcapacity);
        Switch isrescue = findViewById(R.id.isrescue);

        Button back = findViewById(R.id.backbutton);
        Button confirm = findViewById(R.id.confirmbutton);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ParentInhalerCreate.this, ParentInhalerMenu.class));
            }
        });

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isValidInhalerInput(maxcapacity.getText().toString().trim(),dosecount.getText().toString().trim(),datePurchased.getText().toString().trim(),dateExpiry.getText().toString().trim())){
                    inhaler = new Inhaler(SignInChildProfileActivity.currentChild.getID(),dateToLong(datePurchased.getText().toString().trim()),dateToLong(dateExpiry.getText().toString().trim()),Integer.parseInt(maxcapacity.getText().toString().trim()),Integer.parseInt(dosecount.getText().toString().trim()),!isrescue.isChecked());
                    InhalerModel.writeIntoDB(inhaler, new CallBack() {
                        @Override
                        public void onComplete() {
                                startActivity(new Intent(ParentInhalerCreate.this, ParentInhalerMenu.class));
                        }
                    });
                }
                else{
                    Toast.makeText(getApplicationContext(), "Error: Invalid information provided.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private long dateToLong(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        sdf.setLenient(false);
        try{return sdf.parse(dateStr).getTime();}
        catch(ParseException e){return -1;}
    }

    private int integerParser(String i){
        try {
            return Integer.parseInt(i);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    public static boolean isValidInhalerInput(String maxCapacityStr, String doseCountStr, String datePurchasedStr, String dateExpiryStr) {
        int maxCapacity;
        int doseCount;
        if (datePurchasedStr == null || dateExpiryStr == null){
            return false;
        }
        try {
            maxCapacity = Integer.parseInt(maxCapacityStr);
            doseCount = Integer.parseInt(doseCountStr);
        } catch (NumberFormatException e) {
            return false;
        }
        if (maxCapacity <= 0 || doseCount < 0) {
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        sdf.setLenient(false);
        try {
            sdf.parse(datePurchasedStr);
            sdf.parse(dateExpiryStr);
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

}

