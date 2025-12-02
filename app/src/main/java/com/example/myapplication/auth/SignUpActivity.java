package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.SignIn.SignInView;
import com.example.myapplication.userdata.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth; // get the shared instance of the FirebaseAuth object

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // get the instance of Email Text
        EditText userEmailView = findViewById(R.id.editTextTextEmailAddress);
        // get the instance of Password Text
        EditText userPasswordView = findViewById(R.id.editTextTextPassword);
        // get the instance of the return text
        TextView GoBackText = findViewById(R.id.signInLink);
        // underlined the return text to notify user it's clickable
        GoBackText.setPaintFlags(GoBackText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        // Set up click listener for the sign-up button
        Button[] types = new Button[2];
        types[0] = findViewById(R.id.choose_parent);
        types[1] = findViewById(R.id.choose_provider);
        for (int i = 0; i < types.length; i++) {
            types[i].setOnClickListener(new View.OnClickListener() {
                // On click: execute following coding
                @Override
                public void onClick(View v) {
                    // if email is not valid, then change color to red and return
                    if (!email_validation(userEmailView)) {
                        userEmailView.setBackgroundColor(Color.parseColor("#FFCDD2"));
                        findViewById(R.id.emailReqs).setBackgroundColor(Color.parseColor("#FFCDD2"));
                        return;
                    }

                    // if password is not valid, then change color to red and return
                    if (!password_validation(userPasswordView)) {
                        userPasswordView.setBackgroundColor(Color.parseColor("#FFCDD2"));
                        findViewById(R.id.passwordReqs).setBackgroundColor(Color.parseColor("#FFCDD2"));
                        return;
                    }
                    // Otherwise, create new account
                    mAuth.createUserWithEmailAndPassword(userEmailView.getText().toString().trim(), userPasswordView.getText().toString().trim()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // if successful, notify user and return to main page.
                            if (task.isSuccessful()) {
                                // get reference of database
                                DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
                                // get reference of current user (automatically login)
                                FirebaseUser currentUser = mAuth.getCurrentUser();
                                // structurize data of current user
                                if (currentUser != null) {
                                    UserData CurrentUserData = new ParentAccount(currentUser.getUid(), userEmailView.getText().toString().trim());
                                    if (v.getId() == R.id.choose_parent) {
                                        CurrentUserData = new ParentAccount(currentUser.getUid(), userEmailView.getText().toString().trim());
                                    } else if (v.getId() == R.id.choose_provider) {
                                        CurrentUserData = new ProviderAccount(currentUser.getUid(), userEmailView.getText().toString().trim());
                                    } else {
                                            Toast.makeText(SignUpActivity.this, "Sign up major malfunction in buttons.", Toast.LENGTH_SHORT).show();
                                    }
                                    CurrentUserData.WriteIntoDatabase(new CallBack() {
                                        @Override
                                        public void onComplete() {
                                            Toast.makeText(SignUpActivity.this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                                            GoToSignIn();
                                        }
                                    });
                                }
                            } else {
                                Exception e = task.getException();
                                // if fail by duplicate email, notify user.
                                if (e instanceof FirebaseAuthUserCollisionException) {
                                    Toast.makeText(SignUpActivity.this, "This email is already been used", Toast.LENGTH_SHORT).show();
                                } else {
                                    // Otherwise, notify user.
                                    Toast.makeText(SignUpActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
                }
            });
        }
        GoBackText.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                GoToSignIn();
            }
        });
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        UserManager.currentUser = new UserData();
        if(currentUser != null) {
            currentUser.reload();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
    }
    @Override
    public void onStop() {
        super.onStop();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    // Return if an email is of valid form
    public boolean email_validation(EditText EmailView){
        String Email = EmailView.getText().toString().trim();
        return Patterns.EMAIL_ADDRESS.matcher(Email).matches();
    }
    // Return if a password is of valid form
    public boolean password_validation(EditText PasswordView){
        String Password = PasswordView.getText().toString().trim();
        return Password.length() >= 6 && Password.matches("^.*(?=.*[a-z].*)(?=.*[A-Z].*)(?=.*\\d.*)(?=.*[!@#$%^&*()_+\\-].*).*$");
    }
    public void GoToSignIn() {
        Intent intent = new Intent(SignUpActivity.this, SignInView.class);
        startActivity(intent);
    }
}