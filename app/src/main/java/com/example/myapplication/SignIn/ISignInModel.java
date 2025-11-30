package com.example.myapplication.SignIn;

import com.example.myapplication.ResultCallBack;
import com.example.myapplication.userdata.UserData;

public interface ISignInModel {
    void ReloadUserAuth();
    void SignInAuth(String email, String password, ResultCallBack<Boolean> callBack);
    String GetCurrentUIDAuth();
    void QueryDBforNonChildren(String ID, ResultCallBack<UserData> callBack);
    void usernameExists(String username, ResultCallBack<String> callBack);
    void QueryDBforChildren(String parentID, String username, ResultCallBack<UserData> callBack);
}
