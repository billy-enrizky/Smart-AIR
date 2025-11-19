package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;

public class DependentChildAccount extends ChildAccount {

    public DependentChildAccount() {
        super();
    }
    public DependentChildAccount(String ID, String Parent_id) {
        super(ID);
        this.Parent_id = Parent_id;
        this.Account = AccountType.DEP_CHILD;
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
                    DependentChildAccount Data = Snapshot.getValue(DependentChildAccount.class);
                    DependentChildAccount.this.ID = Data.ID;
                    DependentChildAccount.this.Account = Data.Account;
                    DependentChildAccount.this.FirstTime = Data.FirstTime;
                    if(callback != null){
                        callback.onComplete();
                    }
                }
            }
        });
    }
}
