package com.example.myapplication;

import androidx.annotation.NonNull;

import com.example.myapplication.ResultCallBack;
import com.example.myapplication.Inhaler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class InhalerModel {
    //Create or modify an inhaler
    //Create is inhaler.InhalerID is null or equals ""
    //Modify if inhaler.InhalerID is not null and not equals ""

    public static void writeIntoDB(com.example.myapplication.Inhaler inhaler, CallBack callback){
        String username = inhaler.username;
        DatabaseReference Ref = UserManager.mDatabase.child("InhalerManager").child(username + (inhaler.isRescue?"_1":"_0")).getRef();
        Ref.setValue(inhaler).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                if(callback != null) {
                    callback.onComplete();
                }
            }
        });
    }

    public static void ReadFromDatabase(String username,boolean isRescue, ResultCallBack<Inhaler> callback) {
        UserManager.mDatabase.child("InhalerManager").child(username +(isRescue?"1":"0"))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(!snapshot.exists()){
                            callback.onComplete(null);
                        }else{
                            Inhaler inhaler = snapshot.getValue(Inhaler.class);
                            if (callback != null){
                                callback.onComplete(inhaler);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    public static void existsInDatabase(String username, boolean isRescue, ResultCallBack<Boolean> callback) {
        UserManager.mDatabase.child("InhalerManager").child(username + (isRescue ? "1" : "0"))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        callback.onComplete(snapshot.exists());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(false);
                    }
                });
    }

    public static void ListenToDatabase(String username, boolean isRescue, ResultCallBack<Inhaler> callback) {
        UserManager.mDatabase.child("InhalerManager").child(username + (isRescue ? "1" : "0"))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onComplete(null);
                        } else {
                            Inhaler inhaler = snapshot.getValue(Inhaler.class);
                            if (callback != null) {
                                callback.onComplete(inhaler);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }


}
