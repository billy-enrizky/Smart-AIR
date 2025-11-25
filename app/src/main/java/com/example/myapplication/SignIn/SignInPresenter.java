package com.example.myapplication.SignIn;

import android.util.Patterns;

import com.example.myapplication.ResultCallBack;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.UserData;

public class SignInPresenter {

    SignInView view;
    SignInModel model;

    public SignInPresenter(SignInView view, SignInModel model) {
        this.view = view;
        this.model = model;
    }

    void initialize(){
        UserManager.currentUser = null;
        model.ReloadUserAuth();
    }

    void signin(String input1, String input2) {
        if(isNull(input1)||isNull(input2)){
            view.showShortMessage("input cannot be empty");
            return;
        }
        String password = input2.trim();
        if(isEmail(input1.trim())){
            String email = input1.trim();
            signInForParentAndProvider(email, password);
        }else{
            String username = input1.trim();
            signInForChild(username, password);
        }
    }

    void signInForParentAndProvider(String email, String password){
        model.SignInAuth(email, password, new ResultCallBack<Boolean>() {
            @Override
            public void onComplete(Boolean result) {
                if(result){
                    view.showShortMessage("Welcome!");
                    String ID = model.GetCurrentUIDAuth();
                    model.QueryDBforNonChildren(ID, new ResultCallBack<UserData>() {
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

    void signInForChild(String username, String password){
        model.usernameExists(username, new ResultCallBack<String>() {
            @Override
            public void onComplete(String result){
                if(result.equals("")){
                    view.showShortMessage("User Not Found");
                }else{
                    String parentID = result;
                    model.QueryDBforChildren(parentID, username, new ResultCallBack<UserData>() {
                        @Override
                        public void onComplete(UserData result) {
                            ChildAccount child = (ChildAccount) result;
                            if(!child.getPassword().equals(password)){
                                view.showShortMessage("User Not Found");
                                return;
                            }
                            UserManager.currentUser = result;
                            view.showShortMessage("Welcome!");
                            if(UserManager.currentUser.getFirstTime()){
                                view.GoToOnBoardingActivity();
                            }else{
                                view.GoToMainActivity();
                            }
                        }
                    });
                }
            }
        });
    }
    Boolean isEmail(String Input){
        return Patterns.EMAIL_ADDRESS.matcher(Input).matches();
    }

    Boolean isNull(String Input){
        if( Input == null || Input.equals("")){
            return true;
        }else{
            return false;
        }
    }
}
