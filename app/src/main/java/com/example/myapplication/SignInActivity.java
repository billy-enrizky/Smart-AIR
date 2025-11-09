package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class SignInActivity extends AppCompatActivity {

    private Button signInButton;
    private EditText emailEditText;
    private EditText passwordEditText;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        signInButton = (Button)findViewById(R.id.sign_in_button);
        emailEditText = (EditText) findViewById(R.id.sign_in_email);
        passwordEditText = (EditText) findViewById(R.id.sign_in_password);
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            currentUser.reload();
        }
    }
    public void signInClick(android.view.View view) {
        mAuth.signInWithEmailAndPassword(emailEditText.getText().toString().trim(), passwordEditText.getText().toString().trim()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            // if successful, switch to main activity
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                // if successful, notify user and return to main page.
                if(task.isSuccessful()){
                    Toast.makeText(SignInActivity.this, "Welcome!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                    startActivity(intent);
                }else{
                    Toast.makeText(SignInActivity.this, "User Not Found", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
