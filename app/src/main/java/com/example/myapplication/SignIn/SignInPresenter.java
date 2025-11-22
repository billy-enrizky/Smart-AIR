package com.example.myapplication.SignIn;

import android.content.Intent;

import com.example.myapplication.MainActivity;
import com.example.myapplication.OnBoardingActivity;
import com.example.myapplication.ResetPasswordActivity;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.SignUpActivity;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.UserData;

public class SignInPresenter {

    SignInView view;
    SignInModel model;

    public SignInPresenter(SignInView view, SignInModel model) {
        this.view = view;
        this.model = model;
    }

    void initialize(){
        UserManager.currentUser = new UserData();
        model.ReloadUserAuth();
    }

    void signin(String emailinput, String passwordinput) {
        String email = emailinput.trim();
        String password = passwordinput.trim();;
        model.SignInAuth(email, password, new ResultCallBack<Boolean>() {
            @Override
            public void onComplete(Boolean result) {
                if(result){
                    view.showShortMessage("Welcome!");
                    String ID = model.GetCurrentUIDAuth();
                    model.QueryDB(ID, new ResultCallBack<UserData>() {
                        @Override
                        public void onComplete(UserData result) {
                            UserManager.currentUser = result;
                            if(UserManager.currentUser.getFirstTime()){
                                Intent intent1 = new Intent(view, OnBoardingActivity.class);
                                view.startActivity(intent1);
                            }else{
                                Intent intent1 = new Intent(view, MainActivity.class);
                                view.startActivity(intent1);
                            }
                        }
                    });
                }else{
                    view.showShortMessage("User Not Found");
                }
            }
        });
    }

    public void forgotPassword() {
        Intent intent = new Intent(view, ResetPasswordActivity.class);
        view.startActivity(intent);
    }

    public void signup() {
        Intent intent = new Intent(view, SignUpActivity.class);
        view.startActivity(intent);
    }
}
