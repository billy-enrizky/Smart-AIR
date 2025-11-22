package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.time.LocalDate;
import java.util.Map;

public class ChildAccount extends UserData {
    String Parent_id;
    String dob;
    int age;
    public ChildAccount(String ID) {
        super(ID);
    }
    public ChildAccount() {
        super();
    }
    public ChildAccount(String ID, String Parent_id, String dob, int age) {
        this.ID = ID;
        this.Parent_id = Parent_id;
        this.dob = dob;
        this.age = age;
    }
    public void setDob(int year, int month, int day) {
        this.dob = ""+year+"/" + month + "/" + day;
    }
    public void setAge(int age) {
        this.age = age;
    }
}
