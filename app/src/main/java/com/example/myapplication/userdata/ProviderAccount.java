package com.example.myapplication.userdata;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class ProviderAccount extends UserData {
    String Email;
    public ProviderAccount () {
        super();
        this.Email = "";
        Account = AccountType.PROVIDER;
    }
    public ProviderAccount (String ID, String Email) {
        super(ID);
        this.Email = Email;
        this.Account = AccountType.PROVIDER;
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
                ProviderAccount Data = snapshot.getValue(ProviderAccount.class);
                ProviderAccount.this.ID = Data.ID;
                ProviderAccount.this.Account = Data.Account;
                ProviderAccount.this.FirstTime = Data.FirstTime;
                ProviderAccount.this.Email = Data.Email;
                if(callback != null){
                    callback.onComplete();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}
