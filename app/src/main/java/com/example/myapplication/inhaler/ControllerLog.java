package com.example.myapplication;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.time.ZoneId;
public class ControllerLog implements Comparable<ControllerLog>{
    String username;
    long timestamp;
    String feelingB;
    String feelingA;
    int ratingB;
    int ratingA;
    String extraInfo;

    public ControllerLog(){
        this.feelingA = "Better";
        this.feelingB = "Better";
        this.ratingA = 1;
        this.ratingB = 1;
        this.extraInfo = "";
        this.timestamp = System.currentTimeMillis();
    }
    public ControllerLog(String username, String feelingB, String feelingA, int ratingB, int ratingA, String extraInfo){
        this.username = username;
        this.feelingB = feelingB;
        this.ratingB = ratingB;
        this.feelingA = feelingA;
        this.ratingA = ratingA;
        this.extraInfo = extraInfo;
        this.timestamp = System.currentTimeMillis();
    }
    public String getDate(){
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(this.timestamp / 1000, 0, ZoneOffset.UTC);
        dateTime = dateTime.atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
        return dateTime.format(formatter);
    }
    public String getUsername() {return username;}
    public void setUsername(String username) {this.username = username;}
    public long getTimestamp() {return timestamp;}
    public void setTimestamp(long timestamp) {this.timestamp = timestamp;}
    public String getFeelingB() {return feelingB;}
    public void setFeelingB(String feelingB) {this.feelingB = feelingB;}
    public String getFeelingA() {return feelingA;}
    public void setFeelingA(String feelingA) {this.feelingA = feelingA;}
    public int getRatingB() {return ratingB;}
    public void setRatingB(int ratingB) {this.ratingB = ratingB;}
    public int getRatingA() {return ratingA;}
    public void setRatingA(int ratingA) {this.ratingA = ratingA;}
    public void setExtraInfo(String extraInfo){this.extraInfo = extraInfo;}
    public String getExtraInfo(){return this.extraInfo;}
    public String getInfo(){
        if (this.getExtraInfo().isEmpty()){
            return "Before: \n\tBreath Rating: "+this.getRatingB()+"/10\n\tFeeling: "+this.getFeelingB()+"\nAfter: \n\tBreath Rating: "+this.getRatingA()+"/10\n\tFeeling: "+this.getFeelingA();
        }
        return "Before: \n\tBreath Rating: "+this.getRatingB()+"/10\n\tFeeling: "+this.getFeelingB()+"\nAfter: \n\tBreath Rating: "+this.getRatingA()+"/10\n\tFeeling: "+this.getFeelingA()+"\n"+this.getExtraInfo();
    }
    @Override
    public int compareTo(ControllerLog other){
        if (other.getTimestamp() > this.getTimestamp()){
            return 1;
        }
        else if (other.getTimestamp() < this.getTimestamp()){
            return -1;
        }
        return 0;
    }
}
