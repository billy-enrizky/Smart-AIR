package com.example.myapplication.SignIn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.ResetPasswordActivity;
import com.google.firebase.auth.FirebaseAuth;


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
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignInPresenter.signIn(SignInActivity.this, emailEditText.getText().toString(), passwordEditText.getText().toString());
            }
        });
    }
}
