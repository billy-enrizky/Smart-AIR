package com.example.myapplication;

import android.os.Build;

import androidx.annotation.NonNull;

import com.example.myapplication.ResultCallBack;
import com.example.myapplication.ControllerLog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class ControllerLogModel {
    public static String getTime(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            long timestamp = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
        return null;
    }
    public static void writeIntoDB(ControllerLog controllerlog, CallBack callback){
        if(controllerlog.username == null){
            return;
        }
        String username = controllerlog.username;
        UserManager.mDatabase.child("ControllerLogManager").child(username).child(controllerlog.getDate()).setValue(controllerlog).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                if(callback != null) {
                    callback.onComplete();
                }
            }
        });
    }

    public static void readFromDB(String username, ResultCallBack<HashMap<String,ControllerLog>> callback){
        UserManager.mDatabase.child("ControllerLogManager").child(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        HashMap<String, ControllerLog> map = new HashMap<>();
                        for (DataSnapshot s : snapshot.getChildren()) {
                            String date = s.getKey();
                            ControllerLog record = s.getValue(ControllerLog.class);
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

    public static void readFromDB(String username,String Date, ResultCallBack<HashMap<String,ControllerLog>> callback){
        String begin = Date + "_00:00:00"; // Date is of format of "yyyy-MM-dd" eg. "2025-01-01"
        String end   = Date + "_23:59:59";
        UserManager.mDatabase.child("ControllerLogManager").child(username)
                .orderByKey()
                .startAt(begin)     // eg. "2025-01-01" , "2025-01-01_00:00:00"
                .endAt(end)         // eg. "2025-03-01" , "2025-03-01_23:59:59"
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        HashMap<String, ControllerLog> map = new HashMap<>();
                        for (DataSnapshot s : snapshot.getChildren()) {
                            String date = s.getKey();
                            ControllerLog record = s.getValue(ControllerLog.class);
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

    public static void readFromDB(String username,String begin, String end, ResultCallBack<HashMap<String,ControllerLog>> callback){
        UserManager.mDatabase.child("ControllerLogManager").child(username)
                .orderByKey()
                .startAt(begin)     // eg. "2025-01-01" , "2025-01-01_00:00:00"
                .endAt(end)         // eg. "2025-03-01" , "2025-03-01_23:59:59"
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        HashMap<String, ControllerLog> map = new HashMap<>();
                        for (DataSnapshot s : snapshot.getChildren()) {
                            String date = s.getKey();
                            ControllerLog record = s.getValue(ControllerLog.class);
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
