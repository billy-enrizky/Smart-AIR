package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import com.example.myapplication.userdata.ChildAccount;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class ParentInhalerMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_inhaler_menu);

        TextView rescueinhaler = findViewById(R.id.rescueinhaler);
        TextView controllerinhaler = findViewById(R.id.controllerinhaler);
        TextView rescuetitle = findViewById(R.id.textView38);
        TextView controllertitle = findViewById(R.id.textView35);
        ImageView imageViewRescue = findViewById(R.id.imageView26);
        ImageView imageViewController = findViewById(R.id.imageView25);

        InhalerModel.ListenToDatabase(SignInChildProfileActivity.currentChild.getID() + "1", true, new ResultCallBack<Inhaler>() {
            @Override
            public void onComplete(Inhaler inhaler) {
                if (inhaler != null) {
                    rescueinhaler.setText(inhaler.displayInfo());
                    if (inhaler.checkExpiry(System.currentTimeMillis())) {
                        Toast.makeText(ParentInhalerMenu.this, "Rescue inhaler almost expired.", Toast.LENGTH_SHORT).show();
                    }
                    if (inhaler.checkEmpty()) {
                        Toast.makeText(ParentInhalerMenu.this, "Rescue inhaler almost empty.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    rescueinhaler.setVisibility(View.GONE);
                    rescuetitle.setVisibility(View.GONE);
                    imageViewRescue.setVisibility(View.GONE);
                }
            }
        });

        InhalerModel.ListenToDatabase(SignInChildProfileActivity.currentChild.getID() + "0", false, new ResultCallBack<Inhaler>() {
            @Override
            public void onComplete(Inhaler inhaler) {
                if (inhaler != null) {
                    controllerinhaler.setText(inhaler.displayInfo());
                    if (inhaler.checkExpiry(System.currentTimeMillis())) {
                        Toast.makeText(ParentInhalerMenu.this, "Controller inhaler almost expired.", Toast.LENGTH_SHORT).show();
                    }
                    if (inhaler.checkEmpty()) {
                        Toast.makeText(ParentInhalerMenu.this, "Controller inhaler almost empty.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    controllerinhaler.setVisibility(View.GONE);
                    controllertitle.setVisibility(View.GONE);
                    imageViewController.setVisibility(View.GONE);
                }
            }
        });

        Button addButton = findViewById(R.id.addbutton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ParentInhalerMenu.this, ParentInhalerCreate.class));
            }
        });
        Button doneButton = findViewById(R.id.donebutton);

        doneButton.setOnClickListener(v -> {
            startActivity(new Intent(ParentInhalerMenu.this, ParentActivity.class));
        });
    }
}
