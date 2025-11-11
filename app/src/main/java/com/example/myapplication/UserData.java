package com.example.myapplication;

import com.google.firebase.database.DatabaseReference;

public class UserData {
    String ID;
    String Email;

    public UserData(String ID, String Email) {
        this.ID = ID;
        this.Email = Email;
    }

    public void WriteIntoDatabase(DatabaseReference mDatabase) {
        mDatabase.child("users").child(ID).child("ID").setValue(ID);
        mDatabase.child("users").child(ID).child("Email").setValue(Email);
    }
}
