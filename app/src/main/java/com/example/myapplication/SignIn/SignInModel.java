package com.example.myapplication.SignIn;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.UserData;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignInModel {

    public FirebaseAuth mAuth;
    public DatabaseReference mDatabase;

    public SignInModel(){
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }
    void AuthSignIn(String email, String password, ResultCallBack<Boolean> callBack) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                // if successful, notify user and return to main page.
                if(task.isSuccessful()){
                    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
                    UserManager.currentUser.ReadFromDatabase(mAuth.getCurrentUser().getUid(),new CallBack() {
                        @Override
                        public void onComplete() {
                            if(callBack != null){
                                callBack.onComplete(true);
                            }
                        }
                    });
                }else{
                    if(callBack != null){
                        callBack.onComplete(false);
                    }
                }
            }
        });
    }
    String AuthGetCurrentUID(){
        return mAuth.getCurrentUser().getUid();
    }

    void DataBaseRead(String ID, ResultCallBack<UserData> callBack){
        mDatabase.child("users").child(ID).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    DataSnapshot Snapshot = task.getResult();
                    UserData Data = Snapshot.getValue(UserData.class);
                    if(callBack != null){
                        callBack.onComplete(Data);
                    }
                }
            }
        });
    }
}
