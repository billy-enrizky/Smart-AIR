package com.example.myapplication.dailycheckin;

public class DailyCheckin {
    String username;
    String yet_to_implement;
    // mind that, to have setValue(this) works, a pair of getter setter is needed for each field
    // a constructor of empty parameter is also needed.
    public DailyCheckin() {
    }
    public String getUsername(){ // format of getName, mind the upper case
        return username;
    }
    public void setUsername(String username){ // getName
        this.username = username;
    }

}
