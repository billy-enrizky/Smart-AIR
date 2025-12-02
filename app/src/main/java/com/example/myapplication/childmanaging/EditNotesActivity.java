package com.example.myapplication.childmanaging;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.CallBack;
import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;

public class EditNotesActivity extends AppCompatActivity {
    private static final String TAG = "EditNotesActivity";
    
    private TextView textViewChildName;
    private EditText editTextNotes;
    private Button buttonSave;
    private Button buttonCancel;
    
    private String childId;
    private String parentId;
    private ChildAccount childAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_notes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        childId = getIntent().getStringExtra("childId");
        parentId = getIntent().getStringExtra("parentId");

        if (childId == null || parentId == null) {
            if (UserManager.currentUser instanceof ParentAccount) {
                ParentAccount parent = (ParentAccount) UserManager.currentUser;
                if (parent.getChildren() != null && !parent.getChildren().isEmpty()) {
                    childAccount = parent.getChildren().values().iterator().next();
                    childId = childAccount.getID();
                    parentId = childAccount.getParent_id();
                } else {
                    Toast.makeText(this, "No children found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } else {
                Toast.makeText(this, "Invalid child information", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            if (UserManager.currentUser instanceof ParentAccount) {
                ParentAccount parent = (ParentAccount) UserManager.currentUser;
                if (parent.getChildren() != null && parent.getChildren().containsKey(childId)) {
                    childAccount = parent.getChildren().get(childId);
                } else {
                    childAccount = new ChildAccount();
                    childAccount.setID(childId);
                    childAccount.setParent_id(parentId);
                    childAccount.ReadFromDatabase(parentId, childId, new CallBack() {
                        @Override
                        public void onComplete() {
                            setupUI();
                        }
                    });
                    return;
                }
            } else {
                Toast.makeText(this, "Only parents can edit notes", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        setupUI();
    }

    private void setupUI() {
        textViewChildName = findViewById(R.id.textViewChildName);
        editTextNotes = findViewById(R.id.editTextNotes);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);

        if (childAccount != null) {
            textViewChildName.setText("Child: " + childAccount.getName());
            String currentNotes = childAccount.getNotes();
            if (currentNotes != null && !currentNotes.isEmpty()) {
                editTextNotes.setText(currentNotes);
            }
        }

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNotes();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void saveNotes() {
        String notes = editTextNotes.getText().toString().trim();

        if (childAccount == null) {
            Toast.makeText(this, "Child information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        childAccount.setNotes(notes);
        
        childAccount.WriteIntoDatabase(new CallBack() {
            @Override
            public void onComplete() {
                Toast.makeText(EditNotesActivity.this, "Notes saved successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }
}

