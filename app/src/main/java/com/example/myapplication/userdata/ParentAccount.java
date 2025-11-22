package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;

public class ParentAccount extends UserData {
    ArrayList<String> Children_id;
    String Email;
    InviteCode InviteCode;
    ArrayList<String> LinkedProvidersId;
    public ParentAccount() {
        super();
        Email = "";
        Children_id = new ArrayList<String>();
        LinkedProvidersId = new ArrayList<String>();
        Account = AccountType.PARENT;
    }
    public ParentAccount(String ID, String Email) {
        super(ID);
        this.Email = Email;
        this.Account = AccountType.PARENT;
        Children_id = new ArrayList<String>();
        LinkedProvidersId = new ArrayList<String>();
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
    public void addChild(String id) {
        Children_id.add(id);
    }
    public void setEmail(String Email){
        this.Email = Email;
    }
    public String getEmail(){
        return Email;
    }
    public ArrayList<String> getChildrenid(){
        return Children_id;
    }

    public void setChildrenid(ArrayList<String> Children_id){
        this.Children_id = Children_id;
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
                    //     UserManager.mDatabase.child("test").setValue(Data.childrenid);
                    ParentAccount.this.ID = Data.ID;
                    ParentAccount.this.Account = Data.Account;
                    ParentAccount.this.FirstTime = Data.FirstTime;
                    ParentAccount.this.Email = Data.Email;
                    ParentAccount.this.Children_id = Data.Children_id;
                    ParentAccount.this.InviteCode = Data.InviteCode;
                    ParentAccount.this.LinkedProvidersId = Data.LinkedProvidersId;

                    //UserManager.mDatabase.child("test").setValue(ParentAccount.this.Children_id);
                    if(callback != null){
                        callback.onComplete();
                    }
                }
            }
        });
    }
}

