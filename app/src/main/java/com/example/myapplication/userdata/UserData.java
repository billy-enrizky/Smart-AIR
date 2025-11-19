package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

enum accountType {PARENT, DEP_CHILD, INDEP_CHILD, PROVIDER}

public class UserData {
    String ID;
    accountType Account;
    Boolean firstTime;

    public UserData(){
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

    public void WriteIntoDatabase(CallBack callback) {
        //UserManager.mDatabase.child("users").child(ID).setValue(this);
        if(callback != null){
            callback.onComplete();
        }
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
                    UserData.this.firstTime = Data.firstTime;
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
