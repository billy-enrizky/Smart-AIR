package com.example.myapplication;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.userdata.ProviderAccount;
import com.example.myapplication.userdata.UserData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

    //Below are some methods that is probably would be frequently used, use them if they help.

    public static void checkUserNull(AppCompatActivity activity){
        if(mAuth.getCurrentUser() == null) {
            Intent intent1 = new Intent(activity, SignInActivity.class);
            activity.startActivity(intent1);
        }
    }

    public static void IsParentAccount(AppCompatActivity activity){
        if(!(currentUser instanceof ParentAccount)){
            Intent intent1 = new Intent(activity, SignInActivity.class);
            activity.startActivity(intent1);
        }
    }

    public static void IsProviderAccount(AppCompatActivity activity){
        if(!(currentUser instanceof ProviderAccount)) {
            Intent intent1 = new Intent(activity, SignInActivity.class);
            activity.startActivity(intent1);
        }
    }

    public static void backToLogin(AppCompatActivity activity){
        mAuth.signOut();
        Intent intent1 = new Intent(activity, SignInActivity.class);
        activity.startActivity(intent1);
    }
}
