package com.example.myapplication.providermanaging;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.ParentActivity;
import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.providermanaging.InvitationCreateActivity;

import java.util.ArrayList;
import java.util.List;

public class ProviderManagerActivity extends AppCompatActivity {
    ParentAccount user;
    RecyclerView recyclerViewChildren;
    ChildAccount currentChild;
    TextView textView33;
    Switch rescueLogsCB;
    Switch controllerAdherenceSummaryCB;
    Switch symptomsCB;
    Switch triggersCB;
    Switch peakFlowCB;
    Switch triageIncidentsCB;
    Switch summaryChartsCB;
    ChildAdapter childAdapter;
    List<ChildAccount> childrenList;



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_provider_manager);
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
        recyclerViewChildren = findViewById(R.id.recyclerViewChildren);
        currentChild = null;
        rescueLogsCB = findViewById(R.id.RL);
        controllerAdherenceSummaryCB = findViewById(R.id.CAS);
        symptomsCB = findViewById(R.id.S);
        triggersCB = findViewById(R.id.T);
        peakFlowCB = findViewById(R.id.PEF);
        triageIncidentsCB = findViewById(R.id.TI);
        summaryChartsCB = findViewById(R.id.SC);
        textView33.setText("Current Child: null" +   "\nClick name to switch");
        
        // Setup RecyclerView
        childrenList = new ArrayList<>();
        childrenList.addAll(user.getChildren().values());
        
        childAdapter = new ChildAdapter(childrenList, this::onChildSelected);
        recyclerViewChildren.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChildren.setAdapter(childAdapter);
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

    public void onChildSelected(ChildAccount child){
        currentChild = child;
        textView33.setText("Current Child: " + currentChild.getName() +   "\nClick name to switch");
        rescueLogsCB.setChecked(currentChild.getPermission().getRescueLogs());
        controllerAdherenceSummaryCB.setChecked(currentChild.getPermission().getControllerAdherenceSummary());
        symptomsCB.setChecked(currentChild.getPermission().getSymptoms());
        triggersCB.setChecked(currentChild.getPermission().getTriggers());
        peakFlowCB.setChecked(currentChild.getPermission().getPeakFlow());
        triageIncidentsCB.setChecked(currentChild.getPermission().getTriageIncidents());
        summaryChartsCB.setChecked(currentChild.getPermission().getSummaryCharts());
        if (childAdapter != null) {
            childAdapter.notifyDataSetChanged();
        }
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
                    Toast.makeText(ProviderManagerActivity.this, 
                            "Provider permissions modified for " + currentChild.getName(), 
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ViewHolder> {
        private final List<ChildAccount> children;
        private final ChildSelectionListener selectionListener;

        public ChildAdapter(List<ChildAccount> children, ChildSelectionListener selectionListener) {
            this.children = children;
            this.selectionListener = selectionListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_child_card_provider, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position < 0 || position >= children.size()) {
                return;
            }
            ChildAccount child = children.get(position);
            holder.textViewChildName.setText(child.getName());
            
            String notes = child.getNotes();
            if (notes != null && !notes.trim().isEmpty()) {
                holder.textViewNotes.setText(notes);
            } else {
                holder.textViewNotes.setText("No notes");
            }
            
            holder.itemView.setOnClickListener(v -> {
                if (selectionListener != null) {
                    selectionListener.onChildSelected(child);
                }
            });
            
            // Highlight selected child
            if (currentChild != null && currentChild.getID().equals(child.getID())) {
                holder.itemView.setAlpha(1.0f);
                holder.cardView.setCardElevation(8f);
            } else {
                holder.itemView.setAlpha(0.7f);
                holder.cardView.setCardElevation(4f);
            }
        }

        @Override
        public int getItemCount() {
            return children.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CardView cardView;
            TextView textViewChildName;
            TextView textViewNotes;

            ViewHolder(View itemView) {
                super(itemView);
                cardView = (CardView) itemView;
                textViewChildName = itemView.findViewById(R.id.textViewChildName);
                textViewNotes = itemView.findViewById(R.id.textViewNotes);
            }
        }
    }

    private interface ChildSelectionListener {
        void onChildSelected(ChildAccount child);
    }
}

