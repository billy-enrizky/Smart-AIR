package com.example.myapplication;

import androidx.annotation.NonNull;

import com.example.myapplication.ResultCallBack;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class AchievementsModel {

    public static void writeIntoDB(Achievement achievement, CallBack callback){
        String username = achievement.getUsername();
        UserManager.mDatabase.child("AchievementsManager").child(username).setValue(achievement).addOnCompleteListener(new OnCompleteListener<Void>() {
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
        AchievementsModel.readFromDB(username, new ResultCallBack<HashMap<String,Achievement>>(){
            @Override
            public void onComplete(HashMap<String,Achievement> result){
                                <Here are the info you get (result)↑ >
                  <attach A here>
            }
        });

        ReturnValue = AchievementsModel.readFromDB;

        <It's incorrect to attach A here, as reading might haven't done>
*/

    public static void readFromDB(String username, ResultCallBack<Achievement> callback) {
        UserManager.mDatabase.child("AchievementsManager").child(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot == null){
                            if (callback != null) {
                                callback.onComplete(null);
                            }
                        }
                        Achievement achievement = new Achievement();
                        achievement = snapshot.getValue(Achievement.class);
                        if (callback != null) {
                            callback.onComplete(achievement);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }
   /* public static void readFromDB(String username, ResultCallBack<HashMap<String,DailyCheckin>> callback){
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
    }*/

}
