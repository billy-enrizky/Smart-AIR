package com.example.myapplication.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.providermanaging.InvitationCreateActivity;
import com.example.myapplication.providermanaging.Permission;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;

public class ProvidersFragment extends Fragment {
    private static final String TAG = "ProvidersFragment";

    private ParentAccount user;
    private RecyclerView recyclerViewChildren;
    private ChildAccount currentChild;
    private TextView textView33;
    private Switch rescueLogsCB;
    private Switch controllerAdherenceSummaryCB;
    private Switch symptomsCB;
    private Switch triggersCB;
    private Switch peakFlowCB;
    private Switch triageIncidentsCB;
    private Switch summaryChartsCB;
    private ChildAdapter childAdapter;
    private List<ChildAccount> childrenList;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_providers, container, false);
        
        if (!(UserManager.currentUser instanceof ParentAccount)) {
            Log.e(TAG, "Current user is not a ParentAccount");
            return view;
        }
        
        user = (ParentAccount) UserManager.currentUser;
        
        if (user != null) {
            for (ChildAccount child : user.getChildren().values()) {
                if (child.getPermission() == null) {
                    child.setPermission(new Permission());
                }
            }
            UserManager.currentUser.WriteIntoDatabase(null);
        }
        
        textView33 = view.findViewById(R.id.textView33);
        recyclerViewChildren = view.findViewById(R.id.recyclerViewChildren);
        currentChild = null;
        rescueLogsCB = view.findViewById(R.id.RL);
        controllerAdherenceSummaryCB = view.findViewById(R.id.CAS);
        symptomsCB = view.findViewById(R.id.S);
        triggersCB = view.findViewById(R.id.T);
        peakFlowCB = view.findViewById(R.id.PEF);
        triageIncidentsCB = view.findViewById(R.id.TI);
        summaryChartsCB = view.findViewById(R.id.SC);
        
        if (textView33 != null) {
            textView33.setText("Current Child: null" + "\nClick name to switch");
        }
        
        Button buttonApplyChange = view.findViewById(R.id.button3);
        if (buttonApplyChange != null) {
            buttonApplyChange.setOnClickListener(this::applyChange);
        }
        
        Button buttonLinkProvider = view.findViewById(R.id.buttonLinkProvider);
        if (buttonLinkProvider != null) {
            buttonLinkProvider.setOnClickListener(this::createInvitation);
        }
        
        // Setup RecyclerView
        childrenList = new ArrayList<>();
        if (user != null) {
            childrenList.addAll(user.getChildren().values());
        }
        
        childAdapter = new ChildAdapter(childrenList, this::onChildSelected);
        if (recyclerViewChildren != null) {
            recyclerViewChildren.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerViewChildren.setAdapter(childAdapter);
        }
        
        return view;
    }

    private void createInvitation(View view) {
        Intent intent = new Intent(getActivity(), InvitationCreateActivity.class);
        startActivity(intent);
    }

    private void onChildSelected(ChildAccount child) {
        currentChild = child;
        if (textView33 != null) {
            textView33.setText("Current Child: " + currentChild.getName() + "\nClick name to switch");
        }
        if (currentChild.getPermission() != null) {
            if (rescueLogsCB != null) {
                rescueLogsCB.setChecked(currentChild.getPermission().getRescueLogs());
            }
            if (controllerAdherenceSummaryCB != null) {
                controllerAdherenceSummaryCB.setChecked(currentChild.getPermission().getControllerAdherenceSummary());
            }
            if (symptomsCB != null) {
                symptomsCB.setChecked(currentChild.getPermission().getSymptoms());
            }
            if (triggersCB != null) {
                triggersCB.setChecked(currentChild.getPermission().getTriggers());
            }
            if (peakFlowCB != null) {
                peakFlowCB.setChecked(currentChild.getPermission().getPeakFlow());
            }
            if (triageIncidentsCB != null) {
                triageIncidentsCB.setChecked(currentChild.getPermission().getTriageIncidents());
            }
            if (summaryChartsCB != null) {
                summaryChartsCB.setChecked(currentChild.getPermission().getSummaryCharts());
            }
        }
        if (childAdapter != null) {
            childAdapter.notifyDataSetChanged();
        }
    }

    private Permission getPermission() {
        Boolean rescueLogs = rescueLogsCB != null && rescueLogsCB.isChecked();
        Boolean controllerAdherenceSummary = controllerAdherenceSummaryCB != null && controllerAdherenceSummaryCB.isChecked();
        Boolean symptoms = symptomsCB != null && symptomsCB.isChecked();
        Boolean triggers = triggersCB != null && triggersCB.isChecked();
        Boolean peakFlow = peakFlowCB != null && peakFlowCB.isChecked();
        Boolean triageIncidents = triageIncidentsCB != null && triageIncidentsCB.isChecked();
        Boolean summaryCharts = summaryChartsCB != null && summaryChartsCB.isChecked();
        
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

    private void applyChange(View view) {
        if (currentChild == null) {
            Toast.makeText(getContext(), "Please select a child first", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Permission newPermission = getPermission();
            currentChild.setPermission(newPermission);
            currentChild.WriteIntoDatabase(new com.example.myapplication.CallBack() {
                @Override
                public void onComplete() {
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                                "Provider permissions modified for " + currentChild.getName(),
                                Toast.LENGTH_SHORT).show();
                    }
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

