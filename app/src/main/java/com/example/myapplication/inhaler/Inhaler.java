package com.example.myapplication;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Inhaler {
    public String username;
    public long datePurchased;
    public long dateExpiry;
    boolean isRescue;

    final long DATEEXPIRYTHRESHOLD = 604800000L;//7 days in milliseconds

    final double PERCENTEMPTYTHRESHOLD = 0.25; //25% or lower I guess

    final int SPRAYTOMLMULT = 10; //10 sprays is 1 ml (I googled)
    int maxcapacity;
    private int spraycount;

    public Inhaler(){}

    public Inhaler(String username, long datePurchased, long dateExpiry, int maxcapacity, int spraycount, boolean isRescue){
        this.username = username;
        if (isRescue)
            this.username = username+"1";
        else
            this.username = username+"0";
        this.isRescue = isRescue;
        this.datePurchased = datePurchased;
        this.dateExpiry = dateExpiry;
        this.maxcapacity = maxcapacity;
        this.spraycount = spraycount;
    }

    public boolean checkExpiry(){
        return (this.dateExpiry - System.currentTimeMillis() < DATEEXPIRYTHRESHOLD);
    }

    public boolean checkEmpty(){
        return (this.maxcapacity - SPRAYTOMLMULT*this.spraycount < this.maxcapacity * PERCENTEMPTYTHRESHOLD);
    }

    public void use(){
        this.spraycount++;
    }
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
           this.username = username;
    }
    public long getDatePurchased() {
        return datePurchased;
    }

    public void setDatePurchased(long datePurchased) {
        this.datePurchased = datePurchased;
    }

    public long getDateExpiry() {
        return dateExpiry;
    }

    public void setDateExpiry(long dateExpiry) {
        this.dateExpiry = dateExpiry;
    }
    public int getMaxcapacity() {
        return maxcapacity;
    }

    public void setMaxcapacity(int maxcapacity) {
        this.maxcapacity = maxcapacity;
    }
    public int getSpraycount() {
        return spraycount;
    }
    public void setSpraycount(int spraycount) {
        this.spraycount = spraycount;
    }

    public void oneDose(){
        this.spraycount++;
    }

    public int getCapacity(){
        return Math.max(this.maxcapacity - (this.spraycount * this.SPRAYTOMLMULT),0);
    }
    public String getPurchased(){
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(this.datePurchased / 1000, 0, ZoneOffset.UTC);
        dateTime = dateTime.atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
        return dateTime.format(formatter);
    }
    public String getExpiry(){
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(this.dateExpiry / 1000, 0, ZoneOffset.UTC);
        dateTime = dateTime.atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
        return dateTime.format(formatter);
    }

    public void ultraSet(String username, long datePurchased, long dateExpiry, boolean isRescue){
        this.isRescue = isRescue;
        this.datePurchased = datePurchased;
        this.dateExpiry = dateExpiry;
        if (isRescue)
            this.username = username+"0";
        else
            this.username = username+"1";
    }
    public String displayInfo(){
        return "Date Purchased: "+getPurchased()+"\nDate Expires: "+getExpiry()+"\nStorage: "+getCapacity()+"ml/"+getMaxcapacity()+"ml\nSpray Count: "+getSpraycount();
    }
}