package com.example.myapplication.userdata;

import com.google.firebase.database.DatabaseReference;

public class ChildAccount extends UserData {
    String Parent_id;
    public ChildAccount() {
        super();
        Parent_id = "";
    }
    public ChildAccount(String ID) {
        super(ID);
    }
    @Override
    public void WriteIntoDatabase(DatabaseReference mDatabase) {
        super.WriteIntoDatabase(mDatabase);
    }
}
