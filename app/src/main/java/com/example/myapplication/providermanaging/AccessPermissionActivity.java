package com.example.myapplication.providermanaging;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.ParentActivity;
import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.providermanaging.InvitationCreateActivity;

public class AccessPermissionActivity extends AppCompatActivity {
    ParentAccount user;
    LinearLayout container;
    ChildAccount currentChild;
    TextView textView33;
    CheckBox rescueLogsCB;
    CheckBox controllerAdherenceSummaryCB;
    CheckBox symptomsCB;
    CheckBox triggersCB;
    CheckBox peakFlowCB;
    CheckBox triageIncidentsCB;
    CheckBox summaryChartsCB;



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_access_permission);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        UserManager.isParentAccount(this);
        user = (ParentAccount) UserManager.currentUser;
        for(ChildAccount child: user.getChildren().values()){
            if(child.getPermission() == null){
                child.setPermission(new Permission());
            }
        }
        UserManager.currentUser.WriteIntoDatabase(null);
        textView33 = findViewById(R.id.textView33);
        container = findViewById(R.id.ChildList2);
        currentChild = null;
        rescueLogsCB = findViewById(R.id.RL);
        controllerAdherenceSummaryCB = findViewById(R.id.CAS);
        symptomsCB = findViewById(R.id.S);
        triggersCB = findViewById(R.id.T);
        peakFlowCB = findViewById(R.id.PEF);
        triageIncidentsCB = findViewById(R.id.TI);
        summaryChartsCB = findViewById(R.id.SC);
        textView33.setText("Current Child: null" +   "\nClick name to switch");
        for(ChildAccount child : user.getChildren().values()){
            addChildToUI(child);
        }
    }
    public void GoBackToHome(android.view.View view){
        Intent intent = new Intent(this, ParentActivity.class);
        startActivity(intent);
        finish();
    }

    public void CreateInvitation(android.view.View view) {
        Intent intent = new Intent(this, InvitationCreateActivity.class);
        startActivity(intent);
        this.finish();
    }

    public void addChildToUI(ChildAccount child){
        TextView tv = new TextView(this);
        tv.setText("Name: " + child.getName() + "\n" + "Notes: " + child.getNotes());
        tv.setTextSize(20);
        tv.setPadding(40,30,40,30);
        tv.setOnClickListener(v -> {
            currentChild = child;
            textView33.setText("Current Child: " + currentChild.getName() +   "\nClick name to switch");
            rescueLogsCB.setChecked(currentChild.getPermission().getRescueLogs());
            controllerAdherenceSummaryCB.setChecked(currentChild.getPermission().getControllerAdherenceSummary());
            symptomsCB.setChecked(currentChild.getPermission().getSymptoms());
            triggersCB.setChecked(currentChild.getPermission().getTriggers());
            peakFlowCB.setChecked(currentChild.getPermission().getPeakFlow());
            triageIncidentsCB.setChecked(currentChild.getPermission().getTriageIncidents());
            summaryChartsCB.setChecked(currentChild.getPermission().getSummaryCharts());
        });
        container.addView(tv);
    }

    public Permission getPermission(){
        Boolean rescueLogs = rescueLogsCB.isChecked();
        Boolean controllerAdherenceSummary = controllerAdherenceSummaryCB.isChecked();
        Boolean symptoms = symptomsCB.isChecked();
        Boolean triggers = triggersCB.isChecked();
        Boolean peakFlow = peakFlowCB.isChecked();
        Boolean triageIncidents = triageIncidentsCB.isChecked();
        Boolean summaryCharts = summaryChartsCB.isChecked();
        Permission permission = new Permission();
        permission.setRescueLogs(rescueLogs);
        permission.setControllerAdherenceSummary(controllerAdherenceSummary);
        permission.setSymptoms(symptoms);
        permission.setTriggers(triggers);
        permission.setPeakFlow(peakFlow);
        permission.setTriageIncidents(triageIncidents);
        permission.setSummaryCharts(summaryCharts);
        return permission;
    }

    public void ApplyChange(android.view.View view){
        if(currentChild == null){
            Toast.makeText(this, "Please select a child first", Toast.LENGTH_SHORT).show();
            return;
        }else{
            Permission newPermission = getPermission();
            currentChild.setPermission(newPermission);
            currentChild.WriteIntoDatabase(new com.example.myapplication.CallBack() {
                @Override
                public void onComplete() {
                    Toast.makeText(AccessPermissionActivity.this, 
                            "Access permissions modified for " + currentChild.getName(), 
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}