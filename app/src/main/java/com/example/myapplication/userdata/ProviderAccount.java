package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.Map;

public class ProviderAccount extends UserData {
    String Email;
    public ProviderAccount (String ID, String Email) {
        super(ID);
        this.Email = Email;
    }

    public void WriteIntoDatabase (DatabaseReference mDatabase) {
        super.WriteIntoDatabase(mDatabase);
        mDatabase.child("users").child(ID).child("Account").setValue("PROVIDER");
        mDatabase.child("users").child(ID).child("Email").setValue(Email);
    }

    @Override
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
                    ProviderAccount.this.ID = (String)temp.get("ID");
                    ProviderAccount.this.Email = (String)temp.get("Email");
                    ProviderAccount.this.Account = accountType.valueOf((String)temp.get("Account"));
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
