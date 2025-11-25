package com.example.myapplication.childmanaging;

import static com.google.firebase.auth.FirebaseAuth.getInstance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.CallBack;
import com.example.myapplication.ParentActivity;
import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

public class CreateChildActivity extends AppCompatActivity {
    private EditText childUsernameEditText;

    private EditText childNameEditText;
    private CalendarView childDobCalendarView;
    public int year = Calendar.getInstance().get(Calendar.YEAR);
    public int month = Calendar.getInstance().get(Calendar.MONTH)+1;
    public int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    private EditText childAgeEditText;
    private EditText childNotesEditText;
    private EditText childPasswordEditText;
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
        childPasswordEditText = (EditText)findViewById(R.id.editTextTextPassword2);
        childDobCalendarView = (CalendarView)findViewById(R.id.input_child_dob);//editTextTextPassword2
        childDobCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                setDate(year, month+1, dayOfMonth);
            }
        });
        UserManager.isParentAccount(this);
        childAgeEditText = (EditText)findViewById(R.id.input_child_age);
        childNotesEditText = (EditText)findViewById(R.id.input_child_notes);
        createChildButton = (Button)findViewById(R.id.create_child_button);
        mAuth = getInstance();
    }

    public void createChildClick(View view) {
        usernameTaken = false;
        FirebaseUser currentUser = mAuth.getCurrentUser();
        ParentAccount parentData = (ParentAccount) UserManager.currentUser;
        String username = childUsernameEditText.getText().toString().trim();
        String name = childNameEditText.getText().toString().trim();
        String dob = ""+this.year+"/"+this.month+"/"+this.day;
        String age = childAgeEditText.getText().toString().trim();
        String notes = childNotesEditText.getText().toString().trim();
        String password = childPasswordEditText.getText().toString().trim();


        // check if username given
        if (username.equals("")) {
            Toast.makeText(CreateChildActivity.this, "Must have username", Toast.LENGTH_SHORT).show();
            return;
        }
        if(password.equals("")) {
            Toast.makeText(CreateChildActivity.this, "Must have password", Toast.LENGTH_SHORT).show();
            return;
        }

        // check to see if username taken
        //boolean usernameTaken = false;
        UserManager.mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot userSnapshot: dataSnapshot.getChildren()) {
                    if(userSnapshot.child("account").getValue() == null)continue;
                    if (userSnapshot.child("account").getValue().equals("PARENT") && userSnapshot.child("children").hasChild(username)) {
                        Toast.makeText(CreateChildActivity.this, "Username already in use", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                ChildAccount child = new ChildAccount(parentData.getID(), password, dob, name, notes, age, username);
                parentData.addChild(child);
                parentData.WriteIntoDatabase(new CallBack() {
                    @Override
                    public void onComplete() {
                        Toast.makeText(CreateChildActivity.this, "Child Creation Successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(CreateChildActivity.this, ParentActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }

        });
    }
    public void GoBackToHome(android.view.View view){
        Intent intent = new Intent(this, ParentActivity.class);
        startActivity(intent);
        finish();
    }
}
