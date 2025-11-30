package com.example.myapplication.SignIn;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.OnBoardingActivity;
import com.example.myapplication.R;
import com.example.myapplication.ResetPasswordActivity;
import com.example.myapplication.SignUpActivity;


public class SignInView extends AppCompatActivity {


    private EditText emailEditText;
    private EditText passwordEditText;

    SignInPresenter SigninPresenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        emailEditText = (EditText) findViewById(R.id.sign_in_email);
        passwordEditText = (EditText) findViewById(R.id.sign_in_password);
        SigninPresenter = new SignInPresenter(this, new SignInModel());
        SigninPresenter.initialize();
    }

    public void GoToSignInActivity(android.view.View view){
        SigninPresenter.signin(emailEditText.getText().toString(), passwordEditText.getText().toString());
    }

    public void GoToResetPasswordActivity(android.view.View view){
        Intent intent = new Intent(this, ResetPasswordActivity.class);
        startActivity(intent);
        finish();
    }

    public void GoToSignUpActivity(android.view.View view){
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
        finish();
    }
    public void GoToMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void GoToOnBoardingActivity(){
        Intent intent = new Intent(this, OnBoardingActivity.class);
        startActivity(intent);
        finish();
    }

    public void showShortMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
