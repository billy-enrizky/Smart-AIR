package com.example.myapplication.childmanaging;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.CallBack;
import com.example.myapplication.MainActivity;
import com.example.myapplication.ParentActivity;
import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;

import java.util.Calendar;

public class SignInChildProfileActivity extends AppCompatActivity {
    ParentAccount user;
    LinearLayout container;
    ChildAccount currentChild;
    TextView textView23;
    EditText editChildName;
    EditText editChildAge;
    EditText editChildDob;
    EditText editChildNotes;
    Calendar dobCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in_children_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        UserManager.isParentAccount(this);
        user = (ParentAccount) UserManager.currentUser;
        container = findViewById(R.id.childListContainer);
        textView23 = findViewById(R.id.textView23);
        editChildName = findViewById(R.id.editChildName);
        editChildAge = findViewById(R.id.editChildAge);
        editChildDob = findViewById(R.id.editChildDob);
        editChildNotes = findViewById(R.id.editChildNotes);
        editChildDob.setOnClickListener(this::openDobPicker);
        dobCalendar = Calendar.getInstance();
        currentChild = null;
        refreshChildList();
    }

    public void GoBackToHome(android.view.View view){
        Intent intent = new Intent(this, ParentActivity.class);
        startActivity(intent);
        finish();
    }

    public void openDobPicker(View view){
        int year = dobCalendar.get(Calendar.YEAR);
        int month = dobCalendar.get(Calendar.MONTH);
        int day = dobCalendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog pickerDialog = new DatePickerDialog(this, (datePicker, selectedYear, selectedMonth, selectedDay) -> {
            dobCalendar.set(selectedYear, selectedMonth, selectedDay);
            editChildDob.setText(formatDob(dobCalendar));
        }, year, month, day);
        pickerDialog.show();
    }

    private void refreshChildList() {
        View header = container.getChildAt(0);
        container.removeAllViews();
        if (header != null) {
            container.addView(header);
        }
        if (user.getChildren() == null) {
            return;
        }
        for(ChildAccount child : user.getChildren().values()){
            addChildToUI(child);
        }
    }

    public void addChildToUI(ChildAccount child){
        TextView tv = new TextView(this);
        String notes = TextUtils.isEmpty(child.getNotes()) ? "None" : child.getNotes();
        String dob = TextUtils.isEmpty(child.getDob()) ? "Not set" : child.getDob();
        String age = TextUtils.isEmpty(child.getAge()) ? "Not set" : child.getAge();
        tv.setText("Name: " + child.getName() + "\nDOB: " + dob + "\nAge: " + age + "\nNotes: " + notes);
        tv.setTextSize(20);
        tv.setPadding(40,30,40,30);
        tv.setOnClickListener(v -> {
            currentChild = child;
            textView23.setText("Current Child: " + currentChild.getName() +   "\nClick name to switch");
            populateChildDetails(child);
        });
        container.addView(tv);
    }

    private void populateChildDetails(ChildAccount child) {
        editChildName.setText(child.getName());
        editChildAge.setText(child.getAge());
        editChildNotes.setText(child.getNotes());
        String dob = child.getDob();
        if (!TextUtils.isEmpty(dob)) {
            editChildDob.setText(dob);
            updateDobCalendarFromString(dob);
        } else {
            editChildDob.setText("");
            dobCalendar = Calendar.getInstance();
        }
    }

    private void updateDobCalendarFromString(String dob) {
        String[] parts = dob.split("/");
        if (parts.length == 3) {
            try {
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]) - 1;
                int d = Integer.parseInt(parts[2]);
                dobCalendar.set(y, m, d);
            } catch (NumberFormatException ignored) {
                dobCalendar = Calendar.getInstance();
            }
        }
    }

    private String formatDob(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return year + "/" + month + "/" + day;
    }

    public void ChildSignin(android.view.View view){
        if(currentChild == null){
            Toast.makeText(this, "Please select a child", Toast.LENGTH_SHORT).show();
            return;
        }
        UserManager.currentUser = currentChild;
        UserManager.mAuth.signOut();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void saveChildChanges(android.view.View view){
        if(currentChild == null){
            Toast.makeText(this, "Please select a child", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = editChildName.getText().toString().trim();
        String age = editChildAge.getText().toString().trim();
        String dob = editChildDob.getText().toString().trim();
        String notes = editChildNotes.getText().toString().trim();

        if(TextUtils.isEmpty(name)){
            editChildName.setError("Name required");
            return;
        }
        if(TextUtils.isEmpty(age)){
            editChildAge.setError("Age required");
            return;
        }
        if(TextUtils.isEmpty(dob)){
            editChildDob.setError("DOB required");
            return;
        }

        currentChild.setName(name);
        currentChild.setAge(age);
        currentChild.setDob(dob);
        currentChild.setNotes(notes);
        currentChild.WriteIntoDatabase(new CallBack() {
            @Override
            public void onComplete() {
                Toast.makeText(SignInChildProfileActivity.this, "Child profile updated", Toast.LENGTH_SHORT).show();
                refreshChildList();
            }
        });
    }
}