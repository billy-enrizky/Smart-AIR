package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.SignIn.SignInView;
import com.example.myapplication.providers.ProviderActivity;
import com.example.myapplication.userdata.AccountType;
import com.example.myapplication.userdata.ChildAccount;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

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
        com.example.myapplication.UserManager.initialization();
        Log.d(TAG, "MainActivity onCreate - currentUser: " + com.example.myapplication.UserManager.currentUser);

        if (com.example.myapplication.UserManager.currentUser == null) {
            Log.w(TAG, "No user logged in, redirecting to SignIn");
            // If no user is logged in, redirect to sign in
            Intent intent = new Intent(MainActivity.this, SignInView.class);
            startActivity(intent);
            finish();
            return;
        }

        AccountType accountType = com.example.myapplication.UserManager.currentUser.getAccount();
        Log.d(TAG, "Account type: " + accountType);

        if (accountType == AccountType.CHILD) {
            Log.d(TAG, "Redirecting to ChildActivity");
            ChildAccount child = (ChildAccount) com.example.myapplication.UserManager.currentUser;
            String parentID = child.getParent_id();
            String username = child.getID();
            com.example.myapplication.UserManager.ChildUserListener(parentID, username);
            Intent intent1 = new Intent(MainActivity.this, ChildActivity.class);
            startActivity(intent1);
            finish();
        } else if (accountType == AccountType.PARENT) {
            Log.d(TAG, "Redirecting to ParentActivity");
            String uid = com.example.myapplication.UserManager.currentUser.getID();
            com.example.myapplication.UserManager.UserListener(uid, AccountType.PARENT);
            Intent intent1 = new Intent(MainActivity.this, com.example.myapplication.ParentActivity.class);
            startActivity(intent1);
            finish();
        } else if (accountType == AccountType.PROVIDER) {
            Log.d(TAG, "Redirecting to ProviderActivity");
            String uid = com.example.myapplication.UserManager.currentUser.getID();
            com.example.myapplication.UserManager.UserListener(uid, AccountType.PROVIDER);
            Intent intent1 = new Intent(MainActivity.this, ProviderActivity.class);
            startActivity(intent1);
            finish();
        } else {
            Log.w(TAG, "Unknown account type: " + accountType + ", redirecting to SignIn");
            // Unknown account type, redirect to sign in
            Intent intent = new Intent(MainActivity.this, SignInView.class);
            startActivity(intent);
            finish();
        }
    }

    public void Onclick(android.view.View view) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");
        myRef.setValue("Hello, World!");
    }
    public void GoToSignUp(android.view.View view) {
        Intent intent = new Intent(this, com.example.myapplication.SignUpActivity.class);
        startActivity(intent);
    }
    public void GoToSignIn(android.view.View view) {
        Intent intent = new Intent(this, SignInView.class);
        startActivity(intent);
    }
}