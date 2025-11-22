package com.example.myapplication.userdata;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class IndependentChildAccount extends ChildAccount {
    String Email;
    public IndependentChildAccount () {
        super();
        this.Email = "";
        Account = AccountType.INDEP_CHILD;

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
        UserManager.mDatabase.child("users").child(ID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                IndependentChildAccount Data = snapshot.getValue(IndependentChildAccount.class);
                IndependentChildAccount.this.ID = Data.ID;
                IndependentChildAccount.this.Account = Data.Account;
                IndependentChildAccount.this.FirstTime = Data.FirstTime;
                IndependentChildAccount.this.Email = Data.Email;
                IndependentChildAccount.this.Parent_id = Data.Parent_id;
                if(callback != null){
                    callback.onComplete();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}
