package com.example.myapplication.SignIn;

import android.util.Log;
import androidx.annotation.NonNull;

import com.example.myapplication.ResultCallBack;
import com.example.myapplication.userdata.AccountType;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.userdata.ProviderAccount;
import com.example.myapplication.userdata.UserData;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignInModel implements ISignInModel {

    public FirebaseAuth mAuth;
    public DatabaseReference mDatabase;

    public SignInModel() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public void ReloadUserAuth() {
        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().reload();
        }
    }

    @Override
    public void SignInAuth(String email, String password, ResultCallBack<Boolean> callBack) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (callBack != null) {
                        callBack.onComplete(task.isSuccessful());
                    }
                });
    }

    @Override
    public String GetCurrentUIDAuth() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getUid();
        }
        return null;
    }

    @Override
    public void QueryDBforNonChildren(String ID, ResultCallBack<UserData> callBack) {
        mDatabase.child("users").child(ID).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                        if (callBack != null) {
                            callBack.onComplete(null);
                        }
                        return;
                    }

                    DataSnapshot snapshot = task.getResult();
                    AccountType type = snapshot.child("account").getValue(AccountType.class);

                    UserData data;
                    if (type == AccountType.PARENT) {
                        data = snapshot.getValue(ParentAccount.class);
                    } else {
                        data = snapshot.getValue(ProviderAccount.class);
                    }

                    if (callBack != null) {
                        callBack.onComplete(data);
                    }
                });
    }

    @Override
    public void usernameExists(String username, ResultCallBack<String> callBack) {
        mDatabase.child("users").get()
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                        if (callBack != null) {
                            callBack.onComplete("");
                        }
                        return;
                    }

                    DataSnapshot snapshot = task.getResult();
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {

                        String accountType = userSnapshot.child("account").getValue(String.class);

                        if ("PARENT".equals(accountType)
                                && userSnapshot.child("children").hasChild(username)) {

                            if (callBack != null) {
                                callBack.onComplete(userSnapshot.getKey());
                            }
                            return;
                        }
                    }

                    if (callBack != null) {
                        callBack.onComplete("");
                    }
                });
    }

    @Override
    public void QueryDBforChildren(String parentID, String username,
                                   ResultCallBack<UserData> callBack) {

        mDatabase.child("users").child(parentID)
                .child("children").child(username).get()
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        Log.e("firebase", "Error getting data", task.getException());
                        if (callBack != null) {
                            callBack.onComplete(null);
                        }
                        return;
                    }

                    DataSnapshot snapshot = task.getResult();
                    UserData data = snapshot.getValue(ChildAccount.class);

                    if (callBack != null) {
                        callBack.onComplete(data);
                    }
                });
    }
}
