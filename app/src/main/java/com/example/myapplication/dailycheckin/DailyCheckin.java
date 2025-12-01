package com.example.myapplication.dailycheckin;

import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.AccountType;

import java.util.ArrayList;
import java.util.InputMismatchException;

public class DailyCheckin extends CheckInEntry{
    AccountType loggedBy;
    String yet_to_implement;
    // mind that, to have setValue(this) works, a pair of getter setter is needed for each field
    // a constructor of empty parameter is also needed.
    public DailyCheckin(String username, boolean nightWaking, String activityLimits, double coughWheezeLevel, ArrayList<String>triggers) {
        super(username, nightWaking, activityLimits, coughWheezeLevel, triggers);
        this.loggedBy = UserManager.currentUser.getAccount();
    }
    public DailyCheckin() {

    }
    public AccountType getLoggedBy() {
        return this.loggedBy;
    }
    public void setLoggedBy(AccountType loggedBy) {
        this.loggedBy = loggedBy;
    }
}
