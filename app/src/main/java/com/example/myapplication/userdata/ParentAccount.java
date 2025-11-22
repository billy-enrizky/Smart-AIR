package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class ParentAccount extends UserData implements Cloneable {
    String Email;
    HashMap<String, Object> children;

    public ParentAccount() {
        super();
        Email = "";
        this.children = new HashMap<String, Object>();
        Account = AccountType.PARENT;
    }
    public ParentAccount(String ID, String Email) {
        super(ID);
        this.Email = Email;
        this.Account = AccountType.PARENT;
        this.children = new HashMap<String, Object>();
    }

    public ParentAccount(ParentAccount other) {
        this.Email = other.Email;
        this.Account = AccountType.PARENT;

    }

    public void addChild(String username, String name, String dob, String age, String notes) {
        HashMap<String, String>childData = new HashMap<String, String>();
        childData.put("name", name);
        childData.put("dob", dob);
        childData.put("age", age);
        childData.put("notes", notes);
        this.children.put(username, childData);
    }
    public void setEmail(String Email){
        this.Email = Email;
    }
    public String getEmail(){
        return Email;
    }
    public HashMap<String, Object> getChildren(){
        return this.children;
    }

    @Override
    public void ReadFromDatabase(String ID, CallBack callback) {
        UserManager.mDatabase.child("users").child(ID).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    DataSnapshot Snapshot = task.getResult();
                    ParentAccount Data = Snapshot.getValue(ParentAccount.class);
                    ParentAccount.this.ID = Data.ID;
                    ParentAccount.this.Account = Data.Account;
                    ParentAccount.this.FirstTime = Data.FirstTime;
                    ParentAccount.this.Email = Data.Email;
                    ParentAccount.this.children = Data.children;
                    if(callback != null){
                        callback.onComplete();
                    }
                }
            }
        });
    }
}
