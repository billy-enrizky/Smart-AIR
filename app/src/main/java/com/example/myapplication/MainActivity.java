package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.SignIn.SignInView;
import com.example.myapplication.userdata.AccountType;
import com.example.myapplication.userdata.ChildAccount;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        if(UserManager.currentUser.getAccount() == AccountType.CHILD){
            ChildAccount child = (ChildAccount) UserManager.currentUser;
            String parentID = child.getParent_id();
            String username = child.getID();
            UserManager.ChildUserListener(parentID, username);
            Intent intent1 = new Intent(MainActivity.this, ChildActivity.class);
            startActivity(intent1);
            finish();
        }else if(UserManager.currentUser.getAccount() == AccountType.PARENT){
            String uid = UserManager.currentUser.getID();
            UserManager.UserListener(uid, AccountType.PARENT);
            Intent intent1 = new Intent(MainActivity.this, ParentActivity.class);
            startActivity(intent1);
            finish();
        }else{
            String uid = UserManager.currentUser.getID();
            UserManager.UserListener(uid, AccountType.PROVIDER);
            Intent intent1 = new Intent(MainActivity.this, ProviderActivity.class);
            startActivity(intent1);
            finish();
        }
    }

    public void Onclick(android.view.View view) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");
        myRef.setValue("Hello, World!");
    }
    public void GoToSignUp(android.view.View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }
    public void GoToSignIn(android.view.View view) {
        Intent intent = new Intent(this, SignInView.class);
        startActivity(intent);
    }
}