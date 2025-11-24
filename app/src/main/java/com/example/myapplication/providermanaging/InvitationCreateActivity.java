package com.example.myapplication.providermanaging;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.CallBack;
import com.example.myapplication.ParentActivity;
import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ParentAccount;

public class InvitationCreateActivity extends AppCompatActivity {
    ParentAccount currentParent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_invitation_create);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @Override
    public void onStart() {
        super.onStart();
        UserManager.checkUserNull(this);
        UserManager.isParentAccount(this);
        currentParent = (ParentAccount) UserManager.currentUser;
        InviteCode.checkExpire(new CallBack() {
            @Override
            public void onComplete() {
                DisplayCodeAndDate();
            }
        });
    }
    public void GoBackToHome(android.view.View view){
        Intent intent = new Intent(this, ParentActivity.class);
        startActivity(intent);
        finish();
    }

    public void GenerateCode(android.view.View view){
        InviteCode newcode = new InviteCode();
        newcode.generateNew();
        currentParent.setInviteCode(newcode);
        currentParent.WriteIntoDatabase(new CallBack() {
            @Override
            public void onComplete() {
                DisplayCodeAndDate();
            }
        });
    }

    public void RevokeCode(android.view.View view){
        currentParent.setInviteCode(null);
        currentParent.WriteIntoDatabase(new CallBack() {
            @Override
            public void onComplete() {
                DisplayCodeAndDate();
            }
        });
    }


    public void DisplayCodeAndDate(){
        TextView CodeText = findViewById(R.id.textView19);
        TextView ExipireDateText = findViewById(R.id.textView21);
        if(currentParent.getInviteCode() == null){
            CodeText.setText("Code: null");
            ExipireDateText.setText("Expire Date: null");
        }else{
            CodeText.setText("Code: " + currentParent.getInviteCode().getCode());
            ExipireDateText.setText("Expire Date: " + currentParent.getInviteCode().ExipireDate());
        }
    }
}