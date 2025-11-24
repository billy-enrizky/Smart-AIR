package com.example.myapplication.providermanaging;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.ProviderActivity;
import com.example.myapplication.R;
import com.example.myapplication.ResultCallBack;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.userdata.ProviderAccount;

public class InvitationAcceptActivity extends AppCompatActivity {
    ProviderAccount currentProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_invitation_accept);
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
        UserManager.isProviderAccount(this);
        currentProvider = (ProviderAccount) UserManager.currentUser;
    }
    public void GoBackToHome(android.view.View view){
        Intent intent = new Intent(this, ProviderActivity.class);
        startActivity(intent);
    }

    public void LinkToParents(android.view.View view){
        EditText InputCode = findViewById(R.id.editTextText);
        String code = InputCode.getText().toString();
        InviteCode.CodeInquiry(code, new ResultCallBack<ParentAccount>() {
            @Override
            public void onComplete(ParentAccount result) {
                if(result != null){
                    ParentAccount Parent = result;
                    if(!checkDuplicates(Parent)){
                        Parent.addLinkedProvider(currentProvider.getID());
                        currentProvider.addLinkedParents(Parent.getID());
                        currentProvider.WriteIntoDatabase(null);
                        Parent.WriteIntoDatabase(null);
                        Toast.makeText(InvitationAcceptActivity.this, "Invitation accepted", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(InvitationAcceptActivity.this, "Already linked", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(InvitationAcceptActivity.this, "No user found with this code", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public Boolean checkDuplicates(ParentAccount Parent){
        String ParentID = Parent.getID();
        return currentProvider.getLinkedParentsId().contains(ParentID);
    }
}