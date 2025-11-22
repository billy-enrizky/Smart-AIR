package com.example.myapplication.SignIn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.CallBack;
import com.example.myapplication.MainActivity;
import com.example.myapplication.OnBoardingActivity;
import com.example.myapplication.R;
import com.example.myapplication.ResetPasswordActivity;
import com.example.myapplication.SignUpActivity;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.UserData;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class SignInActivity extends AppCompatActivity {

    private Button signInButton;
    private Button forgotPasswordButton;
    private EditText emailEditText;
    private EditText passwordEditText;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        signInButton = (Button)findViewById(R.id.sign_in_button);
        forgotPasswordButton = (Button)findViewById(R.id.sign_in_forgot_password);
        emailEditText = (EditText) findViewById(R.id.sign_in_email);
        passwordEditText = (EditText) findViewById(R.id.sign_in_password);
        mAuth = FirebaseAuth.getInstance();

        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInActivity.this, ResetPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        UserManager.currentUser = new UserData();
        if(currentUser != null) {
            currentUser.reload();
        }
    }
    public void signInClick(android.view.View view) {
        mAuth.signInWithEmailAndPassword(emailEditText.getText().toString().trim(), passwordEditText.getText().toString().trim()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                // if successful, notify user and return to main page.
                if(task.isSuccessful()){
                    Toast.makeText(SignInActivity.this, "Welcome!", Toast.LENGTH_SHORT).show();
                    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
                    UserManager.currentUser.ReadFromDatabase(mAuth.getCurrentUser().getUid(), new CallBack(){
                        @Override
                        public void onComplete(){
                            if(UserManager.currentUser.getFirstTime()){
                                Intent intent1 = new Intent(SignInActivity.this, OnBoardingActivity.class);
                                startActivity(intent1);
                            }else{
                                Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                                startActivity(intent);
                            }
                        }
                    });
                }else{
                    Toast.makeText(SignInActivity.this, "User Not Found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public void CreateNewAccount(android.view.View view) {
        Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
        startActivity(intent);
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
