package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;

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
        UserManager.mDatabase.child("users").child(ID).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    DataSnapshot Snapshot = task.getResult();
                    ProviderAccount Data = Snapshot.getValue(ProviderAccount.class);
                    ProviderAccount.this.ID = Data.ID;
                    ProviderAccount.this.Account = Data.Account;
                    ProviderAccount.this.FirstTime = Data.FirstTime;
                    ProviderAccount.this.Email = Data.Email;
                    if(callback != null){
                        callback.onComplete();
                    }
                }
            }
        });
    }

}
