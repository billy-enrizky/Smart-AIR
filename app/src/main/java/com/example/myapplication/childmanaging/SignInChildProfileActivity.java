package com.example.myapplication.childmanaging;

import android.content.Intent;
import android.os.Bundle;
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

import com.example.myapplication.MainActivity;
import com.example.myapplication.ParentActivity;
import com.example.myapplication.R;
import com.example.myapplication.SignIn.SignInView;
import com.example.myapplication.UserManager;
import com.example.myapplication.dailycheckin.CheckInHistoryFilters;
import com.example.myapplication.dailycheckin.CheckInPresenter;
import com.example.myapplication.dailycheckin.CheckInView;
import com.example.myapplication.dailycheckin.FilterCheckInByDate;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;

public class SignInChildProfileActivity extends AppCompatActivity {
    ParentAccount user;
    LinearLayout container;
    static ChildAccount currentChild;
    TextView textView23;
    EditText editTextText2;
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
        editTextText2 = findViewById(R.id.editTextText2);
        currentChild = null;
        for(ChildAccount child : user.getChildren().values()){
            addChildToUI(child);
        }
    }
   public void GoBackToHome(android.view.View view){
        Intent intent = new Intent(this, ParentActivity.class);
        startActivity(intent);
        finish();
    }

    public void GoToDailyCheckIn(android.view.View view) {
        if(currentChild == null){
            Toast.makeText(this, "Please select a child", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, CheckInView.class);
        startActivity(intent);
        finish();
    }

    public static String getCurrentChildUsername() {
        return currentChild.getID();
    }
    public static ChildAccount getCurrentChild() {
        return currentChild;
    }

    public void addChildToUI(ChildAccount child){
        TextView tv = new TextView(this);
        tv.setText("Name: " + child.getName() + "\n" + "Notes: " + child.getNotes());
        tv.setTextSize(20);
        tv.setPadding(40,30,40,30);
        tv.setOnClickListener(v -> {
            currentChild = child;
            textView23.setText("Current Child: " + currentChild.getName() +   "\nClick name to switch");
        });
        container.addView(tv);
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
    public void changeNotes(android.view.View view){
        if(currentChild == null){
            Toast.makeText(this, "Please select a child", Toast.LENGTH_SHORT).show();
            return;
        }
        String notes = editTextText2.getText().toString();
        currentChild.setNotes(notes);
        currentChild.WriteIntoDatabase(null);
        View keep = container.getChildAt(0);
        container.removeAllViews();
        container.addView(keep);
        for(ChildAccount child : user.getChildren().values()){
            addChildToUI(child);
        }
    }

    public void goToFilterByDate(View view) {
        if(currentChild == null){
            Toast.makeText(this, "Please select a child", Toast.LENGTH_SHORT).show();
            return;
        }
        CheckInHistoryFilters.getInstance().setUsername(currentChild.getID());
        Intent intent = new Intent(this, FilterCheckInByDate.class);
        startActivity(intent);
    }
}