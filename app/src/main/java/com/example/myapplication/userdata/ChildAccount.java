package com.example.myapplication.userdata;

public class ChildAccount extends UserData {
    String Parent_id;
    public ChildAccount() {
        super();
        Parent_id = "";
    }
    public ChildAccount(String ID) {
        super(ID);
        Parent_id = "";
    }

}
