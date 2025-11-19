package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;

public class IndependentChildAccount extends ChildAccount {
    String Email;
    public IndependentChildAccount () {
        super();
        this.Email = "";

    }
    public IndependentChildAccount (String ID, String Email) {
        super(ID);
        this.Email = Email;
        this.Account = AccountType.INDEP_CHILD;

    }
    public void setEmail(String Email){
        this.Email = Email;
    }
    public String getEmail(){
        return Email;
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
                    IndependentChildAccount Data = Snapshot.getValue(IndependentChildAccount.class);
                    IndependentChildAccount.this.ID = Data.ID;
                    IndependentChildAccount.this.Account = Data.Account;
                    IndependentChildAccount.this.FirstTime = Data.FirstTime;
                    IndependentChildAccount.this.Email = Data.Email;
                    IndependentChildAccount.this.Parent_id = Data.Parent_id;
                    if(callback != null){
                        callback.onComplete();
                    }
                }
            }
        });
    }

}
