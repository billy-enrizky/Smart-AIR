package com.example.myapplication.reports;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.medication.ControllerSchedule;
import com.example.myapplication.providers.AccessInfoActivity;
import com.example.myapplication.userdata.ChildAccount;
import com.google.firebase.database.DatabaseReference;

import java.util.Locale;

public class AdherenceSummaryActivity extends AppCompatActivity {
    TextView currentChildName;
    TextView adherenceText;
    String childName;
    String childId;
    String parentId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_adherence_summary);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        currentChildName = findViewById(R.id.textView39);
        adherenceText = findViewById(R.id.textView50);
        Intent intent = getIntent();
        childName = intent.getStringExtra("childName");
        childId = intent.getStringExtra("childId");
        parentId = intent.getStringExtra("parentId");
        currentChildName.setText("Current Child: " + childName);

    }
    public void Back(android.view.View view){
        Intent intent = new Intent(this, AccessInfoActivity.class);
        startActivity(intent);
        this.finish();
    }
    private void loadAdherence() {
        DatabaseReference childRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId);

        childRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ChildAccount child = task.getResult().getValue(ChildAccount.class);
                if (child != null && child.getControllerSchedule() != null) {
                    ControllerSchedule schedule = child.getControllerSchedule();
                    AdherenceCalculator.calculateAdherence(childId, schedule, startDate, endDate, new com.example.myapplication.ResultCallBack<Double>() {
                        @Override
                        public void onComplete(Double adherence) {
                            adherenceText.setText("Controller Adherence: " + String.format(Locale.getDefault(), "%.1f%%", adherence));
                        }
                    });
                } else {
                    adherenceText.setText("Controller Adherence: 0%");
                }
            }
        });
    }
}