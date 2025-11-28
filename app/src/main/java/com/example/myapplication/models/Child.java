package com.example.myapplication.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "children")
public class Child {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public long dob;          // store timestamp for DOB
    public String notes;

    public Child(String name, long dob, String notes) {
        this.name = name;
        this.dob = dob;
        this.notes = notes;
    }
}
