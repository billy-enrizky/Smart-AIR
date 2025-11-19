package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.util.Map;

public class ProviderAccount extends UserData {
    String Email;
    public ProviderAccount(){
        Email = "";
    }
    public ProviderAccount (String ID, String Email) {
        super(ID);
        this.Email = Email;
        this.Account = AccountType.PROVIDER;
    }

    public void WriteIntoDatabase (DatabaseReference mDatabase) {
        super.WriteIntoDatabase(mDatabase);
        mDatabase.child("users").child(ID).child("Email").setValue(Email);
        mDatabase.child("users").child(ID).child("Account").setValue("PROVIDER");
    }

    @Override
    public void ReadFromDatabase(DatabaseReference mDatabase, String ID, CallBack callback) {
        mDatabase.child("users").child(ID).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    Map<String, Object> temp = (Map<String, Object>) task.getResult().getValue();
                    mDatabase.child("test").setValue((Boolean)temp.get("FirstTime"));
                    ProviderAccount.this.ID = (String)temp.get("ID");
                    ProviderAccount.this.Email = (String)temp.get("Email");
                    ProviderAccount.this.Account = AccountType.valueOf((String)temp.get("Account"));
                    Boolean fT = (Boolean)temp.get("FirstTime");
                    ProviderAccount.this.firstTime = ((fT != null ) && fT);
                    if(callback != null){
                        callback.onComplete();
                    }
                }
            }
        });
    }
}
