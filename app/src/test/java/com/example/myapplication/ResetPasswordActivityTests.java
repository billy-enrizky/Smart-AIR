package com.example.myapplication;
import android.content.Intent;
import android.widget.Toast;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
public class ResetPasswordActivityTests {
    ResetPasswordActivity activity;
    @Mock
    FirebaseAuth mauth;
    @Mock
    DatabaseReference mdatabase;

    @Mock
    DataSnapshot msnapshot;

    @Mock
    Toast mToast;

    @Mock
    Task<Void> mTask;

    String EMAIL_FAKE = "67@mail.com";
    ValueEventListener mlistener;

    @Before
    public void init(){
        activity.setFirebase(mauth);
        activity.setDatabase(mdatabase);
    }

    @Test
    public void basicEmailFunctionality(){
        when(msnapshot.exists()).thenReturn(true);
        mdatabase.child("users")
                .orderByChild("Email")
                .equalTo(EMAIL_FAKE)
                .addListenerForSingleValueEvent(mlistener);
        mlistener.onDataChange(msnapshot);
        verify(mauth).sendPasswordResetEmail(eq(EMAIL_FAKE));
        when(mTask.isSuccessful()).thenReturn(true);
    }

    @Test
    public void emailFailure(){
        when(msnapshot.exists()).thenReturn(true);
        mdatabase.child("users")
                .orderByChild("Email")
                .equalTo(EMAIL_FAKE)
                .addListenerForSingleValueEvent(mlistener);
        mlistener.onDataChange(msnapshot);
        verify(mauth).sendPasswordResetEmail(eq(EMAIL_FAKE));
        when(mTask.isSuccessful()).thenReturn(false);
        verify(Toast.makeText(eq(activity), eq("Failed to send reset email"), eq(Toast.LENGTH_SHORT))).show();
    }

    @Test
    public void emailNotFound(){
        when(msnapshot.exists()).thenReturn(false);
        mdatabase.child("users")
                .orderByChild("Email")
                .equalTo(EMAIL_FAKE)
                .addListenerForSingleValueEvent(mlistener);
        mlistener.onDataChange(msnapshot);
        verify(Toast.makeText(eq(activity), eq("Account not found with this email"),eq(Toast.LENGTH_SHORT))).show();
    }



}