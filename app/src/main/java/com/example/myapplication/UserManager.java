package com.example.myapplication;

import com.example.myapplication.userdata.UserData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserManager {
    public static UserData currentUser;
    public static FirebaseAuth mAuth = FirebaseAuth.getInstance();
    public static DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
}
