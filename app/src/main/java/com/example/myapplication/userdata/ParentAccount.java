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

public class ParentAccount extends UserData {
    ArrayList<String> Children_id;
    String Email;
    public ParentAccount() {
        super();
        Email = "";
        Children_id = new ArrayList<String>();
    }
    public ParentAccount(String ID, String Email) {
        super(ID);
        this.Email = Email;
        this.Account = AccountType.PARENT;
        Children_id = new ArrayList<String>();
    }

    public void addChild(String id) {
        Children_id.add(id);
    }
    @Override
    public void WriteIntoDatabase(DatabaseReference mDatabase) {
        super.WriteIntoDatabase(mDatabase);
        mDatabase.child("users").child(ID).child("Account").setValue("PARENT");
        mDatabase.child("users").child(ID).child("Email").setValue(Email);
        mDatabase.child("users").child(ID).child("Children_id").setValue(Children_id);

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
                    ParentAccount.this.ID = (String)temp.get("ID");
                    ParentAccount.this.Email = (String)temp.get("Email");
                    ParentAccount.this.Account = AccountType.valueOf((String)temp.get("Account"));
                    ParentAccount.this.Children_id = (ArrayList<String>)temp.get("Children_id");
                    Boolean fT = (Boolean)temp.get("FirstTime");
                    ParentAccount.this.firstTime = ((fT != null ) && fT);
                    if(callback != null){
                        callback.onComplete();
                    }
                }
            }
        });
    }
}
