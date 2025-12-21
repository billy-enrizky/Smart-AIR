package com.example.myapplication.providers;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.CallBack;
import com.example.myapplication.ChildInhalerLogs;
import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.dailycheckin.CheckInHistoryFilters;
import com.example.myapplication.dailycheckin.FilterCheckInByDate;
import com.example.myapplication.providermanaging.Permission;
import com.example.myapplication.reports.AdherenceSummaryActivity;
import com.example.myapplication.reports.TrendSnippetActivity;
import com.example.myapplication.safety.IncidentHistoryActivity;
import com.example.myapplication.safety.PEFHistoryActivity;
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
    TextView textViewClickChildFirst;

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
        textViewClickChildFirst = findViewById(R.id.textViewClickChildFirst);
        container = findViewById(R.id.childListContainer);
        textView = findViewById(R.id.textViewCurrentChild);
        textView.setText("Current Child: null\nClick name to switch");
        UserManager.isProviderAccount(this);
        currentUser = (ProviderAccount) UserManager.currentUser;
        LinkedParentsId = currentUser.getLinkedParentsId();
        LinkedChildren = new ArrayList<>();
        initialization();
        SetButtonVisibility();
    }
    public void initialization(){
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
                    AccessInfoModel.removeAllListeners();
                    container.removeAllViews();
                    currentChild = null;
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
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.item_child_card_provider, container, false);
        
        TextView textViewChildName = cardView.findViewById(R.id.textViewChildName);
        TextView textViewNotes = cardView.findViewById(R.id.textViewNotes);
        
        textViewChildName.setText(child.getName());
        
        String notes = child.getNotes();
        if(notes != null && !notes.trim().isEmpty()){
            textViewNotes.setText(notes);
        } else {
            textViewNotes.setText("No notes");
        }
        
        cardView.setOnClickListener(v -> {
            currentChild = child;
            textView.setText("Current Child: " + child.getName() + "\nClick name to switch");
            SetButtonVisibility();
        });
        
        container.addView(cardView);
    }
    public void SetButtonVisibility(){
        if(currentChild == null){
            rescueLogs.setVisibility(Button.GONE);
            controllerAdherenceSummary.setVisibility(Button.GONE);
            symptoms.setVisibility(Button.GONE);
            triggers.setVisibility(Button.GONE);
            peakFlow.setVisibility(Button.GONE);
            triageIncidents.setVisibility(Button.GONE);
            summaryCharts.setVisibility(Button.GONE);
            textViewClickChildFirst.setVisibility(TextView.VISIBLE);
            return;
        }
        textViewClickChildFirst.setVisibility(TextView.GONE);
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
        Intent intent = new Intent(this, ChildInhalerLogs.class);
        intent.putExtra("ID", currentChild.getID());
        intent.putExtra("isProvider", "true");
        startActivity(intent);
    }
    public void ControllerAdherenceSummary(android.view.View view){
        Intent intent = new Intent(this, AdherenceSummaryActivity.class);
        intent.putExtra("childId", currentChild.getID());
        intent.putExtra("parentId", currentChild.getParent_id());
        intent.putExtra("childName", currentChild.getName());
        intent.putExtra("isProvider", "true");
        startActivity(intent);
    }
    public void Symptoms(android.view.View view){
        CheckInHistoryFilters.getInstance().setUsername(currentChild.getID());
        Intent intent = new Intent(this, FilterCheckInByDate.class);
        intent.putExtra("childName", currentChild.getName());
        intent.putExtra("isProvider", "true");
        intent.putExtra("permissionToSymptoms", "true");
        startActivity(intent);
    }
    public void Triggers(android.view.View view){
        CheckInHistoryFilters.getInstance().setUsername(currentChild.getID());
        Intent intent = new Intent(this, FilterCheckInByDate.class);
        intent.putExtra("childName", currentChild.getName());
        intent.putExtra("isProvider", "true");
        intent.putExtra("permissionToTriggers", "true");
        startActivity(intent);
    }
    public void PeakFlow(android.view.View view){
        Intent intent = new Intent(this, PEFHistoryActivity.class);
        intent.putExtra("childId", currentChild.getID());
        intent.putExtra("parentId", currentChild.getParent_id());
        intent.putExtra("isProvider", "true");
        startActivity(intent);
    }
    public void TriageIncidents(android.view.View view){
        Intent intent = new Intent(this, IncidentHistoryActivity.class);
        intent.putExtra("childId", currentChild.getID());
        intent.putExtra("parentId", currentChild.getParent_id());
        intent.putExtra("isProvider", "true");
        startActivity(intent);
    }
    public void SummaryCharts(android.view.View view){
        Intent intent = new Intent(this, TrendSnippetActivity.class);
        intent.putExtra("childId", currentChild.getID());
        intent.putExtra("parentId", currentChild.getParent_id());
        intent.putExtra("childName", currentChild.getName());
        if(!currentChild.getPermission().getPeakFlow()){
            intent.putExtra("PEFBanned", currentChild.getName());
        }
        if(!currentChild.getPermission().getRescueLogs()){
            intent.putExtra("RescueLogBanned", currentChild.getName());
        }
        if(!currentChild.getPermission().getSymptoms()){
            intent.putExtra("SymptomsBanned", currentChild.getName());
        }
        intent.putExtra("isProvider", "true");
        startActivity(intent);
    }
}