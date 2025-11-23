package com.example.myapplication.SignIn;

import com.example.myapplication.ResultCallBack;
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
        if(isNull(email)){
            view.showShortMessage("email cannot be empty");
            return;
        }
        if(isNull(password)){
            view.showShortMessage("password cannot be empty");
            return;
        }
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
                                view.GoToOnBoardingActivity();
                            }else{
                                view.GoToMainActivity();
                            }
                        }
                    });
                }else{
                    view.showShortMessage("User Not Found");
                }
            }
        });
    }

    Boolean isNull(String Input){
        if( Input == null || Input.equals("")){
            return true;
        }else{
            return false;
        }
    }
}
