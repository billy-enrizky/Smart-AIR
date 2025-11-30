package com.example.myapplication.dailycheckin;

import static android.text.TextUtils.isEmpty;
import static android.widget.Toast.makeText;

import static java.util.Objects.isNull;

import android.widget.Toast;

import com.example.myapplication.CallBack;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.SignUpActivity;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.AccountType;
import com.example.myapplication.userdata.UserData;

import java.util.ArrayList;
import java.util.InputMismatchException;

public class CheckInPresenter {
    static CheckInView view;
    static CheckInModel model;
    public CheckInPresenter (CheckInView view, CheckInModel model) {
        this.view = view;
        this.model = model;
    }

    void initialize(){
        //model.ReloadUserAuth();
    }

    void logEntry(String username, boolean nightWaking, String activityLimits, double coughWheezeLevel, ArrayList<String> triggers) {
        DailyCheckin entry = new DailyCheckin(username, nightWaking, activityLimits, coughWheezeLevel, triggers);
        //view.showShortMessage("Entry for " + username + ". Night Waking: " + nightWaking + ". " + activityLimits + ". Level:" + coughWheezeLevel);
        model.WriteIntoDB(entry, new CallBack() {
            @Override
            public void onComplete() {
                view.showShortMessage("Entry successfully logged!");
            }
        });
    }
}
