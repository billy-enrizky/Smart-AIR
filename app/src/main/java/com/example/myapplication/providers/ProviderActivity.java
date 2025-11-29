package com.example.myapplication.providers;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.SignIn.SignInView;
import com.example.myapplication.UserManager;
import com.example.myapplication.providermanaging.InvitationAcceptActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ProviderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_provider);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    public void Signout(android.view.View view){
        UserManager.currentUser = null;
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, SignInView.class);
        startActivity(intent);
        this.finish();
    }

    public void AcceptInvitation(android.view.View view){
        Intent intent = new Intent(this, InvitationAcceptActivity.class);
        startActivity(intent);
        this.finish();
    }
    public void getLinkedParents(){

    }
}