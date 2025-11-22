package com.example.myapplication.SignIn;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;


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

    void onClickSigninButton(android.view.View view){
        //SigninPresenter.signin(emailEditText.getText().toString(), passwordEditText.getText().toString());
    }

    void onClickForgotPasswordButton(android.view.View view){
        SigninPresenter.forgotPassword();
    }

    void onClickSignupButton(android.view.View view){
        SigninPresenter.signup();
    }

    void showShortMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
