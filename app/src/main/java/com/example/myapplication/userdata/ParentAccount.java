package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class ParentAccount extends UserData implements Cloneable {
    String Email;
    HashMap<String, ChildAccount> children;
    InviteCode InviteCode;
    ArrayList<String> LinkedProvidersId;

    public ParentAccount() {
        super();
        Email = "";
        this.children = new HashMap<String, ChildAccount>();
        this.LinkedProvidersId = new ArrayList<String>();
        Account = AccountType.PARENT;
    }
    public ParentAccount(String ID, String Email) {
        super(ID);
        this.Email = Email;
        this.Account = AccountType.PARENT;
        this.children = new HashMap<String, ChildAccount>();
        this.LinkedProvidersId = new ArrayList<String>();
    }

    public ParentAccount(ParentAccount other) {
        this.Email = other.Email;
        this.Account = AccountType.PARENT;
        this.LinkedProvidersId = new ArrayList<String>();

    }

    public void addChild(ChildAccount child) {
        this.children.put(child.ID, child);
    }
    public void setEmail(String Email){
        this.Email = Email;
    }
    public String getEmail(){
        return Email;
    }
    public HashMap<String, ChildAccount> getChildren(){
        return this.children;
    }
    public void setChildren(HashMap<String, ChildAccount> children){
        this.children = children;
    }


    public void setInviteCode(InviteCode InviteCode){
        this.InviteCode = InviteCode;
    }
    public InviteCode getInviteCode(){
        return InviteCode;
    }
    public void setLinkedProvidersId(ArrayList<String> LinkedProviders){
        this.LinkedProvidersId = LinkedProviders;
    }
    public ArrayList<String> getLinkedProvidersId(){
        return LinkedProvidersId;
    }

    public void addLinkedProvider(String ID){
        LinkedProvidersId.add(ID);
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
                    ParentAccount Data = Snapshot.getValue(ParentAccount.class);
                    ParentAccount.this.ID = Data.ID;
                    ParentAccount.this.Account = Data.Account;
                    ParentAccount.this.FirstTime = Data.FirstTime;
                    ParentAccount.this.Email = Data.Email;
                    ParentAccount.this.children = Data.children;
                    ParentAccount.this.InviteCode = Data.InviteCode;
                    ParentAccount.this.LinkedProvidersId = Data.LinkedProvidersId;
                    if(callback != null){
                        callback.onComplete();
                    }
                }
            }
        });
    }
}
