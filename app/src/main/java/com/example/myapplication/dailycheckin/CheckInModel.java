package com.example.myapplication.dailycheckin;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.UserManager;
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

public class CheckInModel {
    public CheckInModel() {
    }
    public static String getDate(){
        // Use SimpleDateFormat for backward compatibility with all API levels
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    //Below are severall few methods for your use.

    //This method Write one single DailyCheckin into the database.
    //I didn't use the "users" node, but the a new node "CheckInManager"
    //Because we need to read "users" when sign in,
    //I don't wanna make sign in too slow.
    //Uses timestamp as key to allow multiple entries per day
    public void WriteIntoDB(DailyCheckin checkin, CallBack callback){
        // Ensure timestamp is set (use current time if not already set)
        if (checkin.getTimestamp() == 0) {
            checkin.setTimestamp(System.currentTimeMillis());
        }
        String timestampKey = String.valueOf(checkin.getTimestamp());
        String encodedUsername = FirebaseKeyEncoder.encode(checkin.username);
        UserManager.mDatabase.child("CheckInManager")
                .child(encodedUsername).child(timestampKey).setValue(checkin)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (callback != null) {
                    callback.onComplete();
                }
            }
        });
    }

 /*     The way to use reading method is as follows:
        Say you want to read the DB, and then use the info to do thing A
        CheckInModel.readFromDB(username, new ResultCallBack<HashMap<String,DailyCheckin>>(){
            @Override
            public void onComplete(HashMap<String,DailyCheckin> result){
                                <Here are the info you get (result)↑ >
                  <attach A here>
            }
        });
        <It's incorrect to attach A here, as reading might haven't done>
*/
    public static void readFromDB(String username, ResultCallBack<HashMap<String, DailyCheckin>> callback){
        String encodedUsername = FirebaseKeyEncoder.encode(username);
        UserManager.mDatabase.child("CheckInManager").child(encodedUsername)
            .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String, DailyCheckin> map = new HashMap<>();
                boolean foundData = false;
                if (snapshot.exists()) {
                    foundData = true;
                    for (DataSnapshot s : snapshot.getChildren()) {
                        String key = s.getKey();
                        DailyCheckin record = s.getValue(DailyCheckin.class);
                        if (record != null) {
                            // Use timestamp as key (or key itself if timestamp not set for backward compatibility)
                            String mapKey = (record.getTimestamp() != 0) ? String.valueOf(record.getTimestamp()) : key;
                            map.put(mapKey, record);
                        }
                    }
                }
                
                // If no data found at encoded path and encoded != raw, check raw path for backward compatibility
                if (!foundData && !encodedUsername.equals(username)) {
                    UserManager.mDatabase.child("CheckInManager").child(username)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot rawSnapshot) {
                                    if (rawSnapshot.exists()) {
                                        for (DataSnapshot s : rawSnapshot.getChildren()) {
                                            String key = s.getKey();
                                            DailyCheckin record = s.getValue(DailyCheckin.class);
                                            if (record != null) {
                                                // Use timestamp as key (or key itself if timestamp not set for backward compatibility)
                                                String mapKey = (record.getTimestamp() != 0) ? String.valueOf(record.getTimestamp()) : key;
                                                map.put(mapKey, record);
                                            }
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

    public static void readFromDB(String username,String Date, ResultCallBack<DailyCheckin> callback){
        String encodedUsername = FirebaseKeyEncoder.encode(username);
        UserManager.mDatabase.child("CheckInManager").child(encodedUsername).child(Date)
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

    public static void readFromDB(String username,String begin, String end, ResultCallBack<HashMap<String,DailyCheckin>> callback){
        String encodedUsername = FirebaseKeyEncoder.encode(username);
        // For date range queries, we need to load all entries and filter by date in code
        // since we now use timestamp keys instead of date keys
        UserManager.mDatabase.child("CheckInManager").child(encodedUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        HashMap<String, DailyCheckin> map = new HashMap<>();
                        boolean foundData = false;
                        if (snapshot.exists()) {
                            foundData = true;
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            for (DataSnapshot s : snapshot.getChildren()) {
                                String key = s.getKey();
                                DailyCheckin record = s.getValue(DailyCheckin.class);
                                if (record != null) {
                                    // Determine date from timestamp or key (for backward compatibility)
                                    String entryDate;
                                    if (record.getTimestamp() != 0) {
                                        entryDate = dateFormat.format(new Date(record.getTimestamp()));
                                    } else {
                                        // Backward compatibility: key might be a date string
                                        entryDate = key;
                                    }
                                    
                                    // Filter by date range
                                    if (entryDate.compareTo(begin) >= 0 && entryDate.compareTo(end) <= 0) {
                                        String mapKey = (record.getTimestamp() != 0) ? String.valueOf(record.getTimestamp()) : key;
                                        map.put(mapKey, record);
                                    }
                                }
                            }
                        }
                        
                        // If no data found at encoded path and encoded != raw, check raw path for backward compatibility
                        if (!foundData && !encodedUsername.equals(username)) {
                            UserManager.mDatabase.child("CheckInManager").child(username)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot rawSnapshot) {
                                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                            if (rawSnapshot.exists()) {
                                                for (DataSnapshot s : rawSnapshot.getChildren()) {
                                                    String key = s.getKey();
                                                    DailyCheckin record = s.getValue(DailyCheckin.class);
                                                    if (record != null) {
                                                        // Determine date from timestamp or key (for backward compatibility)
                                                        String entryDate;
                                                        if (record.getTimestamp() != 0) {
                                                            entryDate = dateFormat.format(new Date(record.getTimestamp()));
                                                        } else {
                                                            // Backward compatibility: key might be a date string
                                                            entryDate = key;
                                                        }
                                                        
                                                        // Filter by date range
                                                        if (entryDate.compareTo(begin) >= 0 && entryDate.compareTo(end) <= 0) {
                                                            String mapKey = (record.getTimestamp() != 0) ? String.valueOf(record.getTimestamp()) : key;
                                                            map.put(mapKey, record);
                                                        }
                                                    }
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
