package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;

public class UserData {
    String ID;
    AccountType Account;
    Boolean FirstTime;

    public UserData(){
        ID = "";
        FirstTime = true;
    }
    public UserData(String ID) {
        this.ID = ID;
        this.FirstTime = true;
        Account = AccountType.CHILD;
    }

    public String getID() {
        return this.ID;
    }
    public void setID(String ID) {
        this.ID = ID;
    }

    public boolean getFirstTime() {
        return this.FirstTime;
    }
    public void setFirstTime (boolean FirstTime) {
        this.FirstTime = FirstTime;
    }

    public void setAccount (AccountType Account) {
        this.Account = Account;
    }

    public AccountType getAccount() {
        return this.Account;
    }

    @NonNull
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void WriteIntoDatabase(CallBack callback) {
        UserManager.mDatabase.child("users").child(ID).setValue(this).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (callback != null) {
                    callback.onComplete();
                }
            }
        });
    }
    public void ReadFromDatabase(String ID, CallBack callback) {
        UserManager.mDatabase.child("users").child(ID).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    DataSnapshot Snapshot = task.getResult();
                    UserData Data = Snapshot.getValue(UserData.class);
                    UserData.this.ID = Data.ID;
                    UserData.this.Account = Data.Account;
                    UserData.this.FirstTime = Data.FirstTime;
                    if(callback != null){
                        callback.onComplete();
                    }
                }
            }
        });
    }

}
