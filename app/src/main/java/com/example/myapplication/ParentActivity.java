package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.SignIn.SignInView;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import com.example.myapplication.childmanaging.CreateChildActivity;
import com.example.myapplication.providermanaging.AccessPermissionActivity;
import com.example.myapplication.providermanaging.InvitationCreateActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ParentActivity extends AppCompatActivity {

    Button createChildButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_parent);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        createChildButton = (Button)findViewById(R.id.createChildPageButton);
        createChildButton.setOnClickListener(new View.OnClickListener() {
            // On click: execute following coding
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ParentActivity.this, CreateChildActivity.class);
                startActivity(intent);
            }
        });
    }

    public void SignInChildrenProfile(android.view.View view){
        Intent intent = new Intent(ParentActivity.this, SignInChildProfileActivity.class);
        startActivity(intent);
    }

    public void Signout(android.view.View view){
        UserManager.currentUser = null;
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, SignInView.class);
        startActivity(intent);
        finish();
    }

    public void CreateInvitation(android.view.View view){
        Intent intent = new Intent(this, InvitationCreateActivity.class);
        startActivity(intent);
        this.finish();
    }

    public void AccessPermission(android.view.View view){
        Intent intent = new Intent(this, AccessPermissionActivity.class);
        startActivity(intent);
        this.finish();
    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
    }
    @Override
    public void onStop() {
        super.onStop();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}