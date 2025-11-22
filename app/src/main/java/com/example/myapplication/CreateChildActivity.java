package com.example.myapplication;

import com.example.myapplication.userdata.IndependentChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.userdata.ProviderAccount;
import com.example.myapplication.userdata.UserData;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import static com.google.firebase.auth.FirebaseAuth.*;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.userdata.DependentChildAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateChildActivity extends AppCompatActivity {
    private EditText childUsernameEditText;

    private EditText childNameEditText;
    private CalendarView childDobCalendarView;
    public int year;
    public int month;
    public int day;
    private EditText childAgeEditText;
    private EditText childNotesEditText;
    private Button createChildButton;
    boolean usernameTaken = false;

    private FirebaseAuth mAuth;

    public void setDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_child_account);
        childUsernameEditText = (EditText)findViewById(R.id.input_child_username);
        childNameEditText = (EditText)findViewById(R.id.input_child_name);
        childDobCalendarView = (CalendarView)findViewById(R.id.input_child_dob);
        childDobCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                setDate(year, month, dayOfMonth);
            }
        });
        childAgeEditText = (EditText)findViewById(R.id.input_child_age);
        childNotesEditText = (EditText)findViewById(R.id.input_child_notes);
        createChildButton = (Button)findViewById(R.id.create_child_button);
        mAuth = getInstance();
    }

    public void createChildClick(View view) throws CloneNotSupportedException {
        usernameTaken = false;
        FirebaseUser currentUser = mAuth.getCurrentUser();
        ParentAccount parentData = (ParentAccount) UserManager.currentUser;
        String username = childUsernameEditText.getText().toString().trim();
        String name = childNameEditText.getText().toString().trim();
        String dob = ""+this.year+"/"+this.month+"/"+this.day;
        String age = childAgeEditText.getText().toString().trim();
        String notes = childNotesEditText.getText().toString().trim();

        // check if username given
        if (username.equals("")) {
            Toast.makeText(CreateChildActivity.this, "Must have username", Toast.LENGTH_SHORT).show();
            return;

        }

        // check to see if username taken
        //boolean usernameTaken = false;
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot userSnapshot: dataSnapshot.getChildren()) {
                    if (userSnapshot.child("account").getValue().equals("PARENT") && userSnapshot.child("children").hasChild(username)) {
                        Toast.makeText(CreateChildActivity.this, "Username already in use", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                parentData.addChild(username, name, dob, age, notes);
                parentData.WriteIntoDatabase(new CallBack() {
                    @Override
                    public void onComplete() {
                        Toast.makeText(CreateChildActivity.this, "Child Creation Successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(CreateChildActivity.this, ParentActivity.class);
                        startActivity(intent);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }

        });

    }
}
