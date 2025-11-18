package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

public class UserData {
    public String ID;
    public AccountType Account;
    public Boolean firstTime;

    public UserData(){
        ID = "";
        Account = AccountType.DEP_CHILD;
        firstTime = true;
    }
    public UserData(String ID) {
        this.ID = ID;
        this.firstTime = true;
    }

    public String getID() {
        return this.ID;
    }
    public void setID(String ID) {
        this.ID = ID;
    }
    public boolean getFirstTime() {
        return this.firstTime;
    }
    public void setFirstTime (boolean firstTime) {
        this.firstTime = firstTime;
    }

    public void WriteIntoDatabase(DatabaseReference mDatabase) {
        mDatabase.child("users").child(ID).child("ID").setValue(ID);
        mDatabase.child("users").child(ID).child("Account").setValue(this.Account);
        mDatabase.child("users").child(ID).child("FirstTime").setValue(true);
    }

    public void ReadFromDatabase(DatabaseReference mDatabase, FirebaseUser User, CallBack callback) {
        mDatabase.child("users").child(User.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    Map<String, Object> temp = (Map<String, Object>) task.getResult().getValue();
                    mDatabase.child("test").setValue((Boolean)temp.get("FirstTime"));
                    UserData.this.ID = (String)temp.get("ID");
                    UserData.this.Account = AccountType.valueOf((String)temp.get("Account"));
                    Boolean fT = (Boolean)temp.get("FirstTime");
                    UserData.this.firstTime = ((fT != null ) && fT);
                    if(callback != null){
                        callback.onComplete();
                    }
                }
            }
        });
    }

    public void changeFirstTime(String UserID, Boolean ft){
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("users").child(UserID).child("FirstTime").setValue(ft);
        firstTime = ft;
    }
}
