package com.example.myapplication.SignIn;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.CallBack;

public class SignInPresenter {
    static void signIn(AppCompatActivity activity, String email, String password) {
        SignInModel.SignIn(email, password, new CallBack() {
            @Override
            public void onComplete() {
                Toast.makeText(activity, "User Not found!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
