package com.example.myapplication.userdata;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ParentAccount extends UserData {
    ArrayList<String> Children_id;
    String Email;
    public ParentAccount() {
        super();
        Email = "";
        Children_id = new ArrayList<String>();
        Account = AccountType.PARENT;
    }
    public ParentAccount(String ID, String Email) {
        super(ID);
        this.Email = Email;
        this.Account = AccountType.PARENT;
        Children_id = new ArrayList<String>();
    }

    public void addChild(String id) {
        Children_id.add(id);
    }
    public void setEmail(String Email){
        this.Email = Email;
    }
    public String getEmail(){
        return Email;
    }
    public ArrayList<String> getChildrenid(){
        return Children_id;
    }

    public void setChildrenid(ArrayList<String> Children_id){
        this.Children_id = Children_id;
    }

    @Override
    public void ReadFromDatabase(String ID, CallBack callback) {
        UserManager.mDatabase.child("users").child(ID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ParentAccount Data = snapshot.getValue(ParentAccount.class);
                ParentAccount.this.ID = Data.ID;
                ParentAccount.this.Account = Data.Account;
                ParentAccount.this.FirstTime = Data.FirstTime;
                ParentAccount.this.Email = Data.Email;
                ParentAccount.this.Children_id = Data.Children_id;
                if(callback != null){
                    callback.onComplete();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}
