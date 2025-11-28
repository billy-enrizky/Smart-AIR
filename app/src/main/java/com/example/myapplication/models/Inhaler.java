package com.example.myapplication.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "inhalers")
public class Inhaler {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int childId;
    public long purchaseDate;
    public long expiryDate;
    public int amountLeft;

    public Inhaler(int childId, long purchaseDate, long expiryDate, int amountLeft) {
        this.childId = childId;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.amountLeft = amountLeft;
    }
}
