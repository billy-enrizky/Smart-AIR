package com.example.myapplication;

import com.google.firebase.database.DatabaseReference;

enum accountType {PARENT, CHILD, PROVIDER}

public class UserData {
    String ID;
    String Email;
    accountType account;

    public UserData(String ID, String Email, accountType account) {
        this.ID = ID;
        this.Email = Email;
        this.account = account;

    }

    public void WriteIntoDatabase(DatabaseReference mDatabase) {
        mDatabase.child("users").child(ID).child("ID").setValue(ID);
        mDatabase.child("users").child(ID).child("Email").setValue(Email);
        mDatabase.child("users").child(ID).child("Account").setValue(account);
    }
}
