package com.example.myapplication.dailycheckin;

import android.os.Build;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.UserManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.util.HashMap;

public class CheckInManager {
    public CheckInManager() {
    }
    public String getDate(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return LocalDate.now().toString();
        }
        return null;
    }
    public void WriteIntoDB(DailyCheckin checkin, CallBack callback){
        String Date = getDate();
        UserManager.mDatabase.child("CheckInManager")
                .child(checkin.username).child(Date).setValue(checkin)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (callback != null) {
                    callback.onComplete();
                }
            }
        });
    }


    public void readFromDB(String username, ResultCallBack<HashMap<String,DailyCheckin>> callback){
        UserManager.mDatabase.child("CheckInManager").child(username)
            .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String, DailyCheckin> map = new HashMap<>();
                for (DataSnapshot s : snapshot.getChildren()) {
                    String date = s.getKey();
                    DailyCheckin record = s.getValue(DailyCheckin.class);
                    map.put(date, record);
                }
                if (callback != null){
                    callback.onComplete(map);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void readFromDB(String username,String Date, ResultCallBack<DailyCheckin> callback){
        UserManager.mDatabase.child("CheckInManager").child(username).child(Date)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot == null){
                            callback.onComplete(null);
                        }else{
                            callback.onComplete(snapshot.getValue(DailyCheckin.class));
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    public void readFromDB(String username,String begin, String end, ResultCallBack<HashMap<String,DailyCheckin>> callback){
        UserManager.mDatabase.child("CheckInManager").child(username)
                .orderByKey()
                .startAt(begin)     // eg. "2025-01-01"
                .endAt(end)         // eg. "2025-03-01"
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        HashMap<String, DailyCheckin> map = new HashMap<>();
                        for (DataSnapshot s : snapshot.getChildren()) {
                            String date = s.getKey();
                            DailyCheckin record = s.getValue(DailyCheckin.class);
                            map.put(date, record);
                        }
                        if (callback != null){
                            callback.onComplete(map);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
