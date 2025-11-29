package com.example.myapplication.providers;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.example.myapplication.providermanaging.Permission;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.userdata.ProviderAccount;

import java.util.ArrayList;

public class AccessInfoActivity extends AppCompatActivity {
    ProviderAccount currentUser;
    LinearLayout container;
    ArrayList<ChildAccount> LinkedChildren;
    ArrayList<String> LinkedParentsId;
    ChildAccount currentChild;
    TextView textView;
    Button rescueLogs;
    Button controllerAdherenceSummary;
    Button symptoms;
    Button triggers;
    Button peakFlow;
    Button triageIncidents;
    Button summaryCharts;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_access_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        rescueLogs = findViewById(R.id.rl);
        rescueLogs.setVisibility(Button.GONE);
        controllerAdherenceSummary = findViewById(R.id.cas);
        controllerAdherenceSummary.setVisibility(Button.GONE);
        symptoms = findViewById(R.id.sym);
        symptoms.setVisibility(Button.GONE);
        triggers = findViewById(R.id.tri);
        triggers.setVisibility(Button.GONE);
        peakFlow = findViewById(R.id.pef);
        peakFlow.setVisibility(Button.GONE);
        triageIncidents = findViewById(R.id.ti);
        triageIncidents.setVisibility(Button.GONE);
        summaryCharts = findViewById(R.id.sc);
        summaryCharts.setVisibility(Button.GONE);
        container = findViewById(R.id.childListContainer);
        UserManager.isProviderAccount(this);
        currentUser = (ProviderAccount) UserManager.currentUser;
        LinkedParentsId = currentUser.getLinkedParentsId();
        LinkedChildren = new ArrayList<>();
        initialization();
    }
    public void initialization(){
        textView = new TextView(this);
        textView.setText("Current Child: null" + "\nClick name to switch");
        textView.setTextSize(20);
        textView.setPadding(40,30,40,30);
        container.addView(textView);
        for(int i = 0; i < LinkedParentsId.size(); i++) {
            String ParentID = LinkedParentsId.get(i);
            ParentAccount parent = new ParentAccount();
            parent.ReadFromDatabase(ParentID, new CallBack() {
                @Override
                public void onComplete() {
                    LinkedChildren.addAll(parent.getChildren().values());
                    for(ChildAccount child: parent.getChildren().values()){
                        addChildToUI(child);
                    }
                }
            });
            AccessInfoModel.addListener(LinkedParentsId.get(i), new CallBack(){
                @Override
                public void onComplete() {
                    Toast.makeText(AccessInfoActivity.this, "Access Info Updated", Toast.LENGTH_SHORT).show();
                    AccessInfoModel.removeAllListeners();
                    container.removeAllViews();
                    initialization();
                    SetButtonVisibility();
                }
            });
        }
    }

    public void addChildToUI(ChildAccount child){
        Permission currentPermission = child.getPermission();
        if(!currentPermission.getControllerAdherenceSummary()&&
                !currentPermission.getRescueLogs()&&
                !currentPermission.getSummaryCharts()&&
                !currentPermission.getSymptoms()&&
                !currentPermission.getTriggers()&&
                !currentPermission.getPeakFlow()&&
                !currentPermission.getTriageIncidents()){
            return;
        }
        TextView tv = new TextView(this);
        tv.setText("Name: " + child.getName() + "\n" + "Notes: " + child.getNotes());
        tv.setTextSize(20);
        tv.setPadding(40,30,40,30);
        tv.setOnClickListener(v -> {
            currentChild = child;
            textView.setText("Current Child: " + child.getName() +   "\nClick name to switch");
            SetButtonVisibility();
        });
        container.addView(tv);
    }
    public void SetButtonVisibility(){
        if(currentChild == null){
            return;
        }
        if(!currentChild.getPermission().getPeakFlow()){//PEF
            peakFlow.setVisibility(Button.GONE);
        }else{
            peakFlow.setVisibility(Button.VISIBLE);
        }
        if(!currentChild.getPermission().getControllerAdherenceSummary()){//CAS
            controllerAdherenceSummary.setVisibility(Button.GONE);
        }else{
            controllerAdherenceSummary.setVisibility(Button.VISIBLE);
        }
        if(!currentChild.getPermission().getRescueLogs()) {//RL
            rescueLogs.setVisibility(Button.GONE);
        }else{
            rescueLogs.setVisibility(Button.VISIBLE);
        }
        if(!currentChild.getPermission().getSummaryCharts()){//SC
            summaryCharts.setVisibility(Button.GONE);
        }else{
            summaryCharts.setVisibility(Button.VISIBLE);
        }
        if(!currentChild.getPermission().getSymptoms()){//Sym
            symptoms.setVisibility(Button.GONE);
        }else{
            symptoms.setVisibility(Button.VISIBLE);
        }
        if(!currentChild.getPermission().getTriggers()){//Tri
            triggers.setVisibility(Button.GONE);
        }else{
            triggers.setVisibility(Button.VISIBLE);
        }
        if(!currentChild.getPermission().getTriageIncidents()){//TI
            triageIncidents.setVisibility(Button.GONE);
        }else{
            triageIncidents.setVisibility(Button.VISIBLE);
        }
    }
    public void GoBackToHome(android.view.View view){
        Intent intent = new Intent(this, ProviderActivity.class);
        startActivity(intent);
        finish();
    }
    public void RescueLogs(android.view.View view){
    }
    public void ControllerAdherenceSummary(android.view.View view){
    }
    public void Symptoms(android.view.View view){
    }
    public void Triggers(android.view.View view){
    }
    public void PeakFlow(android.view.View view){
    }
    public void TriageIncidents(android.view.View view){
    }
    public void SummaryCharts(android.view.View view){
    }
}