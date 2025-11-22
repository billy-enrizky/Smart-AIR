package com.example.myapplication.SignIn;

import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.MainActivity;
import com.example.myapplication.OnBoardingActivity;
import com.example.myapplication.ResetPasswordActivity;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.SignUpActivity;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.UserData;

public class SignInPresenter {
    static void signIn(AppCompatActivity activity, String emailinput, String passwordinput) {
        String email = emailinput.
        SignInModel.AuthSignIn(email, password, new ResultCallBack<Boolean>() {
            @Override
            public void onComplete(Boolean result) {
                if(result){
                    Toast.makeText(activity, "Welcome!", Toast.LENGTH_SHORT).show();
                    String ID = SignInModel.AuthGetCurrentUID();
                    SignInModel.DataBaseRead(ID, new ResultCallBack<UserData>() {
                        @Override
                        public void onComplete(UserData result) {
                            UserManager.currentUser = result;
                            if(UserManager.currentUser.getFirstTime()){
                                Intent intent1 = new Intent(activity, OnBoardingActivity.class);
                                activity.startActivity(intent1);
                            }else{
                                Intent intent1 = new Intent(activity, MainActivity.class);
                                activity.startActivity(intent1);
                            }
                        }
                    });
                }else{
                    Toast.makeText(activity, "User Not found!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public void CreateNewAccount(AppCompatActivity activity) {
        Intent intent = new Intent(activity, SignUpActivity.class);
        activity.startActivity(intent);
    }

    public void forgotPassword(AppCompatActivity activity) {
        Intent intent = new Intent(activity, ResetPasswordActivity.class);
        activity.startActivity(intent);
    }
}
