package com.example.myapplication.userdata;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class DependentChildAccount extends ChildAccount {

    public DependentChildAccount() {
        super();
        Account = AccountType.DEP_CHILD;
    }
    public DependentChildAccount(String ID, String Parent_id) {
        super(ID);
        this.Parent_id = Parent_id;
        this.Account = AccountType.DEP_CHILD;
    }

    @Override
    public void ReadFromDatabase(String ID, CallBack callback) {
        UserManager.mDatabase.child("users").child(ID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                DependentChildAccount Data = snapshot.getValue(DependentChildAccount.class);
                DependentChildAccount.this.ID = Data.ID;
                DependentChildAccount.this.Account = Data.Account;
                DependentChildAccount.this.FirstTime = Data.FirstTime;
                DependentChildAccount.this.Parent_id = Data.Parent_id;
                if(callback != null){
                    callback.onComplete();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}
