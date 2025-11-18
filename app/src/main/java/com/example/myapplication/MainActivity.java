package com.example.myapplication;


import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.myapplication.userdata.*;
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
        int Type = 0;
        if(UserManager.currentUser.Account == AccountType.DEP_CHILD){
            Type = 1;
        }else if(UserManager.currentUser.Account == AccountType.INDEP_CHILD){
            Type = 2;
        }else if(UserManager.currentUser.Account == AccountType.PARENT){
            Type = 3;
        }else{
            Type = 4;
        }
        if(Type  ==  1){
            UserManager.currentUser = new DependentChildAccount();
            UserManager.currentUser.ReadFromDatabase(UserManager.mDatabase, UserManager.mAuth.getCurrentUser(), new CallBack(){
                @Override
                public void onComplete(){
                    Intent intent1 = new Intent(MainActivity.this, ChildActivity.class);
                    startActivity(intent1);
                }
            });
        }else if(Type  == 2){
            UserManager.currentUser = new IndependentChildAccount();
            UserManager.currentUser.ReadFromDatabase(UserManager.mDatabase, UserManager.mAuth.getCurrentUser(), new CallBack(){
                @Override
                public void onComplete(){
                    Intent intent1 = new Intent(MainActivity.this, ChildActivity.class);
                    startActivity(intent1);
                }
            });
        }else if(Type  == 3){
            UserManager.currentUser = new ParentAccount();
            UserManager.currentUser.ReadFromDatabase(UserManager.mDatabase, UserManager.mAuth.getCurrentUser(), new CallBack(){
                @Override
                public void onComplete(){
                    Intent intent1 = new Intent(MainActivity.this, ParentActivity.class);
                    startActivity(intent1);
                }
            });
        }else{
            UserManager.currentUser = new ParentAccount();
            UserManager.currentUser.ReadFromDatabase(UserManager.mDatabase, UserManager.mAuth.getCurrentUser(), new CallBack(){
                @Override
                public void onComplete(){
                    Intent intent1 = new Intent(MainActivity.this, ProviderActivity.class);
                    startActivity(intent1);
                }
            });
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
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
    }
}