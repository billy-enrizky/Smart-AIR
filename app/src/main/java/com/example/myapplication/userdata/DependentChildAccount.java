package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.util.Map;

public class DependentChildAccount extends ChildAccount {

    public DependentChildAccount() {
        super();
        this.Parent_id = "";
        this.Account = AccountType.DEP_CHILD;
    }
    public DependentChildAccount(String ID, String Parent_id) {
        super(ID);
        this.Parent_id = Parent_id;
        this.Account = AccountType.DEP_CHILD;
    }
    @Override
    public void WriteIntoDatabase(DatabaseReference mDatabase) {
        super.WriteIntoDatabase(mDatabase);
        mDatabase.child("users").child(ID).child("Parent_id").setValue(Parent_id);
        mDatabase.child("users").child(ID).child("Account").setValue("DEP_CHILD");

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
                    DependentChildAccount.this.ID = (String)temp.get("ID");
                    DependentChildAccount.this.Account = AccountType.valueOf((String)temp.get("Account"));
                    DependentChildAccount.this.Parent_id = (String)temp.get("Parent_id");
                    Boolean fT = (Boolean)temp.get("FirstTime");
                    DependentChildAccount.this.firstTime = ((fT != null ) && fT);
                    if(callback != null){
                        callback.onComplete();
                    }
                }
            }
        });
    }
}
