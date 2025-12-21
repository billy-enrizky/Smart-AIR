package com.example.myapplication;

import android.os.Build;

import androidx.annotation.NonNull;

import com.example.myapplication.ResultCallBack;
import com.example.myapplication.ControllerLog;
import com.example.myapplication.utils.FirebaseKeyEncoder;
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
            android.util.Log.e("ControllerLogModel", "Cannot save controller log: username is null");
            return;
        }
        String username = controllerlog.username;
        String encodedUsername = FirebaseKeyEncoder.encode(username);
        com.google.firebase.database.DatabaseReference logRef = UserManager.mDatabase.child("ControllerLogManager").child(encodedUsername).child(controllerlog.getDate());
        logRef.setValue(controllerlog).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                if (task.isSuccessful()) {
                    android.util.Log.d("ControllerLogModel", "Controller log saved successfully to Firebase: " + logRef.toString());
                } else {
                    android.util.Log.e("ControllerLogModel", "Failed to save controller log", task.getException());
                }
                if(callback != null) {
                    callback.onComplete();
                }
            }
        });
    }

    public static void readFromDB(String username, ResultCallBack<HashMap<String,ControllerLog>> callback){
        String encodedUsername = FirebaseKeyEncoder.encode(username);
        UserManager.mDatabase.child("ControllerLogManager").child(encodedUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        HashMap<String, ControllerLog> map = new HashMap<>();
                        boolean foundData = false;
                        if (snapshot.exists()) {
                            foundData = true;
                            for (DataSnapshot s : snapshot.getChildren()) {
                                String date = s.getKey();
                                ControllerLog record = s.getValue(ControllerLog.class);
                                map.put(date, record);
                            }
                        }
                        
                        // If no data found at encoded path and encoded != raw, check raw path for backward compatibility
                        if (!foundData && !encodedUsername.equals(username)) {
                            UserManager.mDatabase.child("ControllerLogManager").child(username)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot rawSnapshot) {
                                            if (rawSnapshot.exists()) {
                                                for (DataSnapshot s : rawSnapshot.getChildren()) {
                                                    String date = s.getKey();
                                                    ControllerLog record = s.getValue(ControllerLog.class);
                                                    map.put(date, record);
                                                }
                                            }
                                            if (callback != null){
                                                callback.onComplete(map);
                                            }
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            if (callback != null){
                                                callback.onComplete(map);
                                            }
                                        }
                                    });
                        } else {
                            if (callback != null){
                                callback.onComplete(map);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (callback != null){
                            callback.onComplete(new HashMap<>());
                        }
                    }
                });
    }

    public static void readFromDB(String username,String Date, ResultCallBack<HashMap<String,ControllerLog>> callback){
        String begin = Date + "_00:00:00"; // Date is of format of "yyyy-MM-dd" eg. "2025-01-01"
        String end   = Date + "_23:59:59";
        String encodedUsername = FirebaseKeyEncoder.encode(username);
        UserManager.mDatabase.child("ControllerLogManager").child(encodedUsername)
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
        String encodedUsername = FirebaseKeyEncoder.encode(username);
        UserManager.mDatabase.child("ControllerLogManager").child(encodedUsername)
                .orderByKey()
                .startAt(begin)     // eg. "2025-01-01" , "2025-01-01_00:00:00"
                .endAt(end)         // eg. "2025-03-01" , "2025-03-01_23:59:59"
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        HashMap<String, ControllerLog> map = new HashMap<>();
                        boolean foundData = false;
                        if (snapshot.exists()) {
                            foundData = true;
                            for (DataSnapshot s : snapshot.getChildren()) {
                                String date = s.getKey();
                                ControllerLog record = s.getValue(ControllerLog.class);
                                map.put(date, record);
                            }
                        }
                        
                        // If no data found at encoded path and encoded != raw, check raw path for backward compatibility
                        if (!foundData && !encodedUsername.equals(username)) {
                            UserManager.mDatabase.child("ControllerLogManager").child(username)
                                    .orderByKey()
                                    .startAt(begin)
                                    .endAt(end)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot rawSnapshot) {
                                            if (rawSnapshot.exists()) {
                                                for (DataSnapshot s : rawSnapshot.getChildren()) {
                                                    String date = s.getKey();
                                                    ControllerLog record = s.getValue(ControllerLog.class);
                                                    map.put(date, record);
                                                }
                                            }
                                            if (callback != null){
                                                callback.onComplete(map);
                                            }
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            if (callback != null){
                                                callback.onComplete(map);
                                            }
                                        }
                                    });
                        } else {
                            if (callback != null){
                                callback.onComplete(map);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (callback != null){
                            callback.onComplete(new HashMap<>());
                        }
                    }
                });
    }
}
