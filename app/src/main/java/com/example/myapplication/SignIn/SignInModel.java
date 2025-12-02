package com.example.myapplication.SignIn;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.ResultCallBack;
import com.example.myapplication.userdata.AccountType;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.userdata.ProviderAccount;
import com.example.myapplication.userdata.UserData;
import com.example.myapplication.utils.FirebaseKeyEncoder;
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
    public void ReloadUserAuth(){
        if(mAuth.getCurrentUser() != null){
            mAuth.getCurrentUser().reload();
        }
    }
    void SignInAuth(String email, String password, ResultCallBack<Boolean> callBack) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    if (callBack != null) {
                        callBack.onComplete(true);
                    }
                }else{
                    if(callBack != null){
                        callBack.onComplete(false);
                    }
                }
            }
        });
    }
    String GetCurrentUIDAuth(){
        if(mAuth.getCurrentUser() != null){
            return mAuth.getCurrentUser().getUid();
        }
        return null;
    }

    void QueryDBforNonChildren(String ID, ResultCallBack<UserData> callBack){
        mDatabase.child("users").child(ID).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    DataSnapshot Snapshot = task.getResult();
                    AccountType type =  Snapshot.child("account").getValue(AccountType.class);
                    UserData Data;
                    if(type == AccountType.PARENT){
                        Data = Snapshot.getValue(ParentAccount.class);
                    }else{
                        Data = Snapshot.getValue(ProviderAccount.class);
                    }
                    if(callBack != null){
                        callBack.onComplete(Data);
                   }
                }
            }
        });
    }
    void usernameExists(String username, ResultCallBack<String> callBack){
        String encodedUsername = FirebaseKeyEncoder.encode(username);
        mDatabase.child("users").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    DataSnapshot Snapshot = task.getResult();
                    Boolean exists = false;
                    for(DataSnapshot userSnapshot: Snapshot.getChildren()){
                        String accountType = userSnapshot.child("account").getValue(String.class);
                        if("PARENT".equals(accountType) && userSnapshot.child("children").hasChild(encodedUsername)){
                            if(callBack != null){
                                callBack.onComplete(userSnapshot.getKey());
                                exists = true;
                                break;
                            }
                        }
                    }
                    if(!exists){
                        if (callBack != null) {
                        callBack.onComplete("");
                        }
                    }
                }
            }
        });
    }

    void QueryDBforChildren(String ParentID, String username, ResultCallBack<UserData> callBack){
        String encodedUsername = FirebaseKeyEncoder.encode(username);
        mDatabase.child("users").child(ParentID).child("children").child(encodedUsername).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    DataSnapshot Snapshot = task.getResult();
                    AccountType type =  Snapshot.child("account").getValue(AccountType.class);
                    UserData Data = Snapshot.getValue(ChildAccount.class);;
                    if(callBack != null){
                        callBack.onComplete(Data);
                    }
                }
            }
        });
    }
}
