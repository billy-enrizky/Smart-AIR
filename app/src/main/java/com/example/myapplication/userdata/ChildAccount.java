package com.example.myapplication.userdata;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;

public class ChildAccount extends UserData {
    String Parent_id;
    public ChildAccount() {
        super();
        Parent_id = "";
    }
    public ChildAccount(String ID) {
        super(ID);
        Parent_id = "";
    }
    @Override
    public void WriteIntoDatabase(CallBack callback) {
        UserManager.mDatabase.child("users").child(ID).setValue(this);
        if(callback != null){
            callback.onComplete();
        }
    }
}
