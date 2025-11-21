package com.example.myapplication.userdata;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.UserManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class InviteCode {
    String Code;
    long ExipireTime;
    public InviteCode() {
    }
    public InviteCode(String code, long time) {
        Code = code;
        ExipireTime = time;
    }
    public long getExipireTime() {
        return ExipireTime;
    }
    public void setExipireTime(long value) {
        ExipireTime = value;
    }

    public String getCode() {
        return Code;
    }
    public void setCode(String value) {
        Code = value;
    }

    public void generateNew(){
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        Code = sb.toString();
        ExipireTime = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000;
    }

    public Boolean IsValid() {
        if (System.currentTimeMillis() > ExipireTime) {
            return false;
        }
        return true;
    }

    public String ExipireDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(ExipireTime));
    }

    public static void checkExpire(CallBack callback){
        DatabaseReference userLists = UserManager.mDatabase.child("users");
        userLists.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    DataSnapshot Snapshot = task.getResult();
                    for(DataSnapshot child: Snapshot.getChildren()){
                        if(child.getValue(UserData.class).getAccount() == AccountType.PARENT){
                            ParentAccount parent = child.getValue(ParentAccount.class);
                            if(parent.getInviteCode() != null){
                                if(!parent.getInviteCode().IsValid()){
                                    parent.setInviteCode(null);
                                    parent.WriteIntoDatabase(null);
                                }
                            }
                        }
                    }
                    callback.onComplete();
                }
            }
        });
    }

    public static void CodeInquiry(String InputCode, ResultCallBack callback){
        DatabaseReference usersRef = UserManager.mDatabase.child("users");
        Query query = usersRef.orderByChild("inviteCode/code").equalTo(InputCode);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        ParentAccount parent = child.getValue(ParentAccount.class);
                        if(parent.getInviteCode().IsValid()){
                            callback.onComplete(parent);
                            return;
                        }else{
                            parent.setInviteCode(null);
                            parent.WriteIntoDatabase(null);
                        }
                    }
                } else {
                    callback.onComplete(null);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                callback.onComplete(null);
            }
        });
    }
}
