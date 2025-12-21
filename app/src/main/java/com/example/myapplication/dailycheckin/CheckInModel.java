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
    public void WriteIntoDB(DailyCheckin checkin, CallBack callback){
        String Date = getDate();
        String encodedUsername = FirebaseKeyEncoder.encode(checkin.username);
        UserManager.mDatabase.child("CheckInManager")
                .child(encodedUsername).child(Date).setValue(checkin)
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
        UserManager.mDatabase.child("CheckInManager").child(encodedUsername)
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
