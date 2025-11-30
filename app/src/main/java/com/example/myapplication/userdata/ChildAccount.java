package com.example.myapplication.userdata;


import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.example.myapplication.providermanaging.Permission;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;

public class ChildAccount extends UserData {
    String Parent_id;
    String password;
    String dob;
    String name;
    String notes;
    String age;
    Permission permission;
    Integer personalBest;
    String actionPlanGreen;
    String actionPlanYellow;
    String actionPlanRed;

    public ChildAccount(String ID) {
        super(ID);
    }
    public ChildAccount() {
        super();
    }
    public ChildAccount(String Parent_id, String password, String dob, String name, String notes, String age, String ID) {
        this.Parent_id = Parent_id;
        this.password = password;
        this.dob = dob;
        this.name = name;
        this.notes = notes;
        this.age = age;
        this.ID = ID;
        this.Account = AccountType.CHILD;
        this.FirstTime = true;
        this.permission = new Permission();
    }

    public Permission getPermission() {
        if (permission == null) {
            permission = new Permission();
        }
        return permission;
    }
    public void setPermission(Permission permission) {
        this.permission = permission;
    }
    public void setDob(String Dob) {
        this.dob = Dob;
    }
    public void setAge(String age) {
        this.age = age;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setParent_id(String Parent_id) {
        this.Parent_id = Parent_id;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setID(String ID) {
        this.ID = ID;
    }
    public String getParent_id(){
        return Parent_id;
    }
    public String getPassword(){
        return password;
    }
    public String getDob(){
        return dob;
    }
    public String getName(){
        return name;
    }
    public String getNotes(){
        return notes;
    }
    public String getAge(){
        return age;
    }
    public Integer getPersonalBest() {
        return personalBest;
    }
    public void setPersonalBest(Integer personalBest) {
        this.personalBest = personalBest;
    }
    public String getActionPlanGreen() {
        return actionPlanGreen;
    }
    public void setActionPlanGreen(String actionPlanGreen) {
        this.actionPlanGreen = actionPlanGreen;
    }
    public String getActionPlanYellow() {
        return actionPlanYellow;
    }
    public void setActionPlanYellow(String actionPlanYellow) {
        this.actionPlanYellow = actionPlanYellow;
    }
    public String getActionPlanRed() {
        return actionPlanRed;
    }
    public void setActionPlanRed(String actionPlanRed) {
        this.actionPlanRed = actionPlanRed;
    }
    @Override
    public void WriteIntoDatabase(CallBack callback) {
        UserManager.mDatabase.child("users").child(Parent_id).child("children").child(ID).setValue(this).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (callback != null) {
                    callback.onComplete();
                }
            }
        });
    }


    public void ReadFromDatabase(String Parent_id,String ID, CallBack callback) {
        UserManager.mDatabase.child("users").child(Parent_id).child("children").child(ID).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    DataSnapshot Snapshot = task.getResult();
                    ChildAccount Data = Snapshot.getValue(ChildAccount.class);
                    ChildAccount.this.ID = Data.ID;
                    ChildAccount.this.Account = Data.Account;
                    ChildAccount.this.FirstTime = Data.FirstTime;
                    ChildAccount.this.Parent_id = Data.Parent_id;
                    ChildAccount.this.dob = Data.dob;
                    ChildAccount.this.age = Data.age;
                    ChildAccount.this.notes = Data.notes;
                    ChildAccount.this.password = Data.password;
                    ChildAccount.this.name = Data.name;
                    ChildAccount.this.permission = Data.permission;
                    ChildAccount.this.personalBest = Data.personalBest;
                    ChildAccount.this.actionPlanGreen = Data.actionPlanGreen;
                    ChildAccount.this.actionPlanYellow = Data.actionPlanYellow;
                    ChildAccount.this.actionPlanRed = Data.actionPlanRed;
                    if(callback != null){
                        callback.onComplete();
                    }
                }
            }
        });
    }
}
