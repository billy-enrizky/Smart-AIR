package com.example.myapplication.childmanaging;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.MainActivity;
import com.example.myapplication.ParentActivity;
import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;

public class SignInChildProfileActivity extends AppCompatActivity {
    ParentAccount user;
    LinearLayout container;
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
        for(ChildAccount child : user.getChildren().values()){
            addChildToUI(child);
        }
    }
   public void GoBackToHome(android.view.View view){
        Intent intent = new Intent(this, ParentActivity.class);
        startActivity(intent);
        finish();
    }

    public void addChildToUI(ChildAccount child){
        TextView tv = new TextView(this);
        tv.setText("Name: " + child.getName() + "\n" + "Notes: " + child.getNotes());
        tv.setTextSize(20);
        tv.setPadding(40,30,40,30);
        tv.setOnClickListener(v -> {
            UserManager.currentUser = child;
            UserManager.mAuth.signOut();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        container.addView(tv);
    }
}