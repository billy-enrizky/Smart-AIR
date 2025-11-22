package com.example.myapplication.SignIn;

import androidx.annotation.NonNull;

import com.example.myapplication.CallBack;
import com.example.myapplication.UserManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignInModel {

    public static FirebaseAuth mAuth = FirebaseAuth.getInstance();
    public static DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    static void SignIn(String email, String password, CallBack callBack) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                // if successful, notify user and return to main page.
                if(task.isSuccessful()){
                    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
                    UserManager.currentUser.ReadFromDatabase(mAuth.getCurrentUser().getUid(),null);
                }else{
                    callBack.onComplete();
                }
            }
        });
    }
}
