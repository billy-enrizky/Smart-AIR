package com.example.myapplication.userdata;

import com.example.myapplication.userdata.selectoritem.SharedItems;

public class ChildAccount extends UserData {
    String Parent_id;
    String dob;
    int age;
    SharedItems SharedItems;
    public ChildAccount(String ID) {
        super(ID);
    }
    public ChildAccount() {
        super();
    }
    public ChildAccount(String ID, String Parent_id, String dob, int age) {
        this.ID = ID;
        this.Parent_id = Parent_id;
        this.dob = dob;
        this.age = age;
    }
    public void setDob(int year, int month, int day) {
        this.dob = ""+year+"/" + month + "/" + day;
    }
    public void setAge(int age) {
        this.age = age;
    }

    public void setSharedItems(SharedItems SharedItems) {
        this.SharedItems = SharedItems;
    }
    public SharedItems getSharedItems() {
        return SharedItems;
    }
}
