package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.SignIn.SignInView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ResetPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ResetPasswordActivity";
    private EditText emailEditText;
    private Button resetButton;
    private TextView backToSignIn;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    public void setFirebase(FirebaseAuth auth) {
        this.mAuth = auth;
    }

    public void setDatabase(DatabaseReference database) {
        this.mDatabase = database;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        emailEditText = findViewById(R.id.editTextTextEmailAddress);
        resetButton = findViewById(R.id.signupbutton);
        backToSignIn = findViewById(R.id.backbutton);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim().toLowerCase();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(ResetPasswordActivity.this, "Enter your email", Toast.LENGTH_SHORT).show();
                    return;
                }

                // First, try to send password reset email directly through Firebase Auth
                // This will work if the user is registered in Firebase Authentication
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Password reset email sent successfully to: " + email);
                            new AlertDialog.Builder(ResetPasswordActivity.this)
                                    .setTitle("Password Reset")
                                    .setMessage("Your reset password link has been sent to your email. It might appear in your spam, so check it.")
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show();
                        } else {
                            // If Firebase Auth fails, check if email exists in database
                            Exception exception = task.getException();
                            if (exception instanceof FirebaseAuthInvalidUserException) {
                                Log.w(TAG, "Email not found in Firebase Auth: " + email);
                                // Check if email exists in database (case-insensitive)
                                checkEmailInDatabase(email);
                            } else {
                                Log.e(TAG, "Failed to send password reset email", exception);
                                // Check database as fallback
                                checkEmailInDatabase(email);
                            }
                        }
                    }
                });
            }
        });

        backToSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResetPasswordActivity.this, SignInView.class);
                startActivity(intent);
            }
        });
    }

    private void checkEmailInDatabase(String email) {
        // Search for email in database (case-insensitive)
        mDatabase.child("users").orderByChild("Email").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String foundEmail = null;
                
                // Iterate through all users to find matching email (case-insensitive)
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String userEmail = userSnapshot.child("Email").getValue(String.class);
                    if (userEmail != null && userEmail.toLowerCase().equals(email.toLowerCase())) {
                        foundEmail = userEmail;
                        break;
                    }
                }
                
                // Make foundEmail final for use in inner class
                final String finalFoundEmail = foundEmail;
                
                if (finalFoundEmail != null) {
                    // Email exists in database, try to send reset with the exact email from database
                    Log.d(TAG, "Email found in database, attempting reset with: " + finalFoundEmail);
                    mAuth.sendPasswordResetEmail(finalFoundEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Password reset email sent successfully to: " + finalFoundEmail);
                                new AlertDialog.Builder(ResetPasswordActivity.this)
                                        .setTitle("Password Reset")
                                        .setMessage("Your reset password link has been sent to your email. It might appear in your spam, so check it.")
                                        .setPositiveButton(android.R.string.ok, null)
                                        .show();
                            } else {
                                Log.e(TAG, "Failed to send password reset email to: " + finalFoundEmail, task.getException());
                                new AlertDialog.Builder(ResetPasswordActivity.this)
                                        .setTitle("Password Reset Failed")
                                        .setMessage("Your email was found in our database, but it is not registered with Firebase Authentication. Please contact support for assistance.")
                                        .setPositiveButton(android.R.string.ok, null)
                                        .show();
                            }
                        }
                    });
                } else {
                    Log.w(TAG, "Email not found in database: " + email);
                    Toast.makeText(ResetPasswordActivity.this, "Account not found with this email", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error while checking email", databaseError.toException());
                Toast.makeText(ResetPasswordActivity.this, "Database error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}