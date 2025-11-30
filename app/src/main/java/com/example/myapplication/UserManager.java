package com.example.myapplication;

import android.content.Intent;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.SignIn.SignInView;
import com.example.myapplication.userdata.AccountType;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.userdata.ProviderAccount;
import com.example.myapplication.userdata.UserData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserManager {
    /*
    currentUser is meant for convenience.
    With it, we don't need to read database each time we need the information of current user
    */
    public static UserData currentUser;

    /*
    mAuth and mDatabase are for conveniecne, too.
    The instance in within remains same each time getInstance(), since they are one-to-one to our app.
    I found it redundant repeating getInstance() each time we use Authentication and Database,
    which is the reason I made them static so that we don't need to create new instance each time.
     */
    public static FirebaseAuth mAuth = FirebaseAuth.getInstance();
    public static DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private static ValueEventListener userListener;

    //Below are some methods that is probably would be frequently used, use them if they help.

    public static void checkUserNull(AppCompatActivity activity){
        if(mAuth.getCurrentUser() == null) {
            backToLogin(activity);
        }
    }

    public static void isParentAccount(AppCompatActivity activity){
        if(!(currentUser instanceof ParentAccount)){
            backToLogin(activity);
        }
    }

    public static void isProviderAccount(AppCompatActivity activity){
        if(!(currentUser instanceof ProviderAccount)) {
            backToLogin(activity);
        }
    }

    public static void backToLogin(AppCompatActivity activity){
        mAuth.signOut();
        Intent intent1 = new Intent(activity, SignInView.class);
        activity.startActivity(intent1);
        activity.finish();
    }

    public static void UserDataListener(String uid) {

        if (userListener != null) return;

        userListener = mDatabase.child("users").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentUser = snapshot.getValue(UserData.class);
                        Log.d("UserManager", "User updated: " + currentUser);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    public static void UserListener(String uid, AccountType type) {
        if (userListener != null) return;
        userListener = mDatabase.child("users").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(type == AccountType.PARENT){
                    currentUser = snapshot.getValue(ParentAccount.class);
                }else{
                    currentUser = snapshot.getValue(ProviderAccount.class);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("UserManager", "Listener failed: " + currentUser);
            }
        });
    }

    public static void ChildUserListener(String parent_id, String username) {
        if (userListener != null) return;
        userListener = mDatabase.child("users").child(parent_id).child("children").child(username).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                currentUser = snapshot.getValue(ChildAccount.class);
                Log.d("UserManager", "User updated: " + currentUser);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("UserManager", "Listener failed: " + currentUser);
            }
        });
    }

    public static void stopChildUserListener(String parent_id, String username) {
        if (userListener != null) {
            mDatabase.child("users").child(parent_id).child("children").child(username).removeEventListener(userListener);
            userListener = null;
        }
    }

    public static void stopUserListener(String uid) {
        if (userListener != null) {
            mDatabase.child("users").child(uid).removeEventListener(userListener);
            userListener = null;
        }
    }
}
