package com.example.myapplication.providers;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AccessInfoModel {
    private static final Map<String, ValueEventListener> userListeners = new HashMap<>();
    public static void addListener(String parentId, CallBack callback) {
        String key = parentId;
        if (userListeners.containsKey(key)) return;
        DatabaseReference ref = UserManager.mDatabase.child("users").child(parentId);
        final Boolean[] isFirst = {true};
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(isFirst[0]){
                    isFirst[0] = false;
                    return;
                }
                if (callback != null) callback.onComplete();
            }
            @Override
            public void onCancelled(DatabaseError error) {
            }
        };

        ref.addValueEventListener(listener);
        userListeners.put(key, listener);
    }
    public static void removeListener(String parentId) {
        String key = parentId;

        ValueEventListener listener = userListeners.get(key);
        if (listener == null) return;

        DatabaseReference ref = UserManager.mDatabase.child("users").child(parentId);
        ref.removeEventListener(listener);
        userListeners.remove(key);  // 从 Map 中删除
    }
    public static void removeAllListeners() {
        for (String ParentID : userListeners.keySet()) {
            DatabaseReference ref = UserManager.mDatabase.child("users").child(ParentID);
            ref.removeEventListener(userListeners.get(ParentID));
        }
        userListeners.clear();
    }
}
