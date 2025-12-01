package com.example.myapplication.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.medication.ControllerScheduleActivity;
import com.example.myapplication.reports.ProviderReportGeneratorActivity;
import com.example.myapplication.safety.ActionPlanActivity;
import com.example.myapplication.ParentInhalerMenu;
import com.example.myapplication.ParentBadge;
import com.example.myapplication.dailycheckin.CheckInView;
import com.example.myapplication.dailycheckin.FilterCheckInByDate;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import com.example.myapplication.MainActivity;
import com.example.myapplication.safety.IncidentHistoryActivity;
import com.example.myapplication.safety.PEFHistoryActivity;
import com.example.myapplication.safety.PEFReading;
import com.example.myapplication.safety.SetPersonalBestActivity;
import com.example.myapplication.safety.Zone;
import com.example.myapplication.safety.ZoneCalculator;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.childmanaging.CreateChildActivity;
import com.example.myapplication.childmanaging.EditNotesActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChildrenFragment extends Fragment {
    private static final String TAG = "ChildrenFragment";
    
    private RecyclerView recyclerViewChildren;
    private ParentAccount parentAccount;
    private SimpleChildAdapter adapter;
    private List<ChildZoneInfo> childrenZoneInfo;
    
    private Map<String, Query> childPEFQueries = new HashMap<>();
    private Map<String, ValueEventListener> childPEFListeners = new HashMap<>();
    private Map<String, DatabaseReference> childAccountRefs = new HashMap<>();
    private Map<String, ValueEventListener> childAccountListeners = new HashMap<>();
    private Map<String, ChildAccount> latestChildAccounts = new HashMap<>();
    
    private ChildZoneInfo selectedChildInfo;
    private View buttonsLayout;
    private TextView textViewCurrentChild;
    private Button buttonNewChild;
    private Button buttonSignIn;
    private Button buttonDailyCheckin;
    private Button buttonSetPersonalBest;
    private Button buttonInhaler;
    private Button buttonModifyNotes;
    private Button buttonBadges;
    private Button buttonGenerateReport;
    private Button buttonControllerSchedule;
    private Button buttonDailyCheckinHistory;
    private Button buttonIncidentHistory;
    private Button buttonDeleteChild;
    private Button buttonActionPlan;
    private Button buttonPEFHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_children, container, false);
        
        if (!(UserManager.currentUser instanceof ParentAccount)) {
            Log.e(TAG, "Current user is not a ParentAccount");
            return view;
        }
        
        parentAccount = (ParentAccount) UserManager.currentUser;
        
        recyclerViewChildren = view.findViewById(R.id.recyclerViewChildren);
        childrenZoneInfo = new ArrayList<>();
        adapter = new SimpleChildAdapter(childrenZoneInfo, this::onChildSelected);
        recyclerViewChildren.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerViewChildren.setAdapter(adapter);
        
        buttonNewChild = view.findViewById(R.id.buttonNewChild);
        buttonNewChild.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreateChildActivity.class);
            startActivity(intent);
        });
        
        buttonsLayout = view.findViewById(R.id.layoutChildButtons);
        textViewCurrentChild = buttonsLayout.findViewById(R.id.textViewCurrentChild);
        buttonSignIn = buttonsLayout.findViewById(R.id.buttonSignIn);
        buttonDailyCheckin = buttonsLayout.findViewById(R.id.buttonDailyCheckin);
        buttonSetPersonalBest = buttonsLayout.findViewById(R.id.buttonSetPersonalBest);
        buttonInhaler = buttonsLayout.findViewById(R.id.buttonInhaler);
        buttonModifyNotes = buttonsLayout.findViewById(R.id.buttonModifyNotes);
        buttonBadges = buttonsLayout.findViewById(R.id.buttonBadges);
        buttonGenerateReport = buttonsLayout.findViewById(R.id.buttonGenerateReport);
        buttonControllerSchedule = buttonsLayout.findViewById(R.id.buttonControllerSchedule);
        buttonDailyCheckinHistory = buttonsLayout.findViewById(R.id.buttonDailyCheckinHistory);
        buttonIncidentHistory = buttonsLayout.findViewById(R.id.buttonIncidentHistory);
        buttonDeleteChild = buttonsLayout.findViewById(R.id.buttonDeleteChild);
        buttonActionPlan = buttonsLayout.findViewById(R.id.buttonActionPlan);
        buttonPEFHistory = buttonsLayout.findViewById(R.id.buttonPEFHistory);
        
        setupButtons();
        updateButtonsVisibility(false);
        
        attachChildrenZoneListeners();
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        attachChildrenZoneListeners();
    }

    @Override
    public void onPause() {
        super.onPause();
        detachChildrenZoneListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detachChildrenZoneListeners();
    }

    private void attachChildrenZoneListeners() {
        if (parentAccount == null || parentAccount.getChildren() == null) {
            return;
        }
        
        detachChildrenZoneListeners();
        
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                childrenZoneInfo.clear();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            });
        }
        
        HashMap<String, ChildAccount> children = parentAccount.getChildren();
        
        if (children.isEmpty()) {
            return;
        }
        
        for (Map.Entry<String, ChildAccount> entry : children.entrySet()) {
            ChildAccount child = entry.getValue();
            attachChildZoneListener(child);
        }
    }

    private void attachChildZoneListener(ChildAccount child) {
        String parentId = child.getParent_id();
        String childId = child.getID();
        
        DatabaseReference pefRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("pefReadings");
        
        Query latestPEFQuery = pefRef.orderByChild("timestamp").limitToLast(1);
        childPEFQueries.put(childId, latestPEFQuery);
        
        latestChildAccounts.put(childId, child);
        
        ValueEventListener pefListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ChildAccount latestChild = latestChildAccounts.get(childId);
                if (latestChild == null) {
                    latestChild = child;
                }
                updateChildZoneFromSnapshot(latestChild, snapshot);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading child zone for " + childId, error.toException());
                ChildAccount latestChild = latestChildAccounts.get(childId);
                if (latestChild == null) {
                    latestChild = child;
                }
                ChildZoneInfo info = new ChildZoneInfo(latestChild, Zone.UNKNOWN, 0.0, null, null);
                updateChildZoneInfo(info);
            }
        };
        
        childPEFListeners.put(childId, pefListener);
        latestPEFQuery.addValueEventListener(pefListener);
        
        DatabaseReference childAccountRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("children")
                .child(childId);
        
        childAccountRefs.put(childId, childAccountRef);
        
        ValueEventListener accountListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ChildAccount updatedChild = snapshot.getValue(ChildAccount.class);
                if (updatedChild != null) {
                    latestChildAccounts.put(childId, updatedChild);
                    
                    Query pefQuery = childPEFQueries.get(childId);
                    if (pefQuery != null) {
                        pefQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot pefSnapshot) {
                                updateChildZoneFromSnapshot(updatedChild, pefSnapshot);
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Log.e(TAG, "Error refreshing zone after personalBest change", error.toException());
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading child account for " + childId, error.toException());
            }
        };
        
        childAccountListeners.put(childId, accountListener);
        childAccountRef.addValueEventListener(accountListener);
    }

    private void updateChildZoneFromSnapshot(ChildAccount child, DataSnapshot snapshot) {
        if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            return;
        }
        
        Integer personalBest = child.getPersonalBest();
        
        if (personalBest == null || personalBest <= 0) {
            ChildZoneInfo info = new ChildZoneInfo(child, Zone.UNKNOWN, 0.0, null, null);
            updateChildZoneInfo(info);
            return;
        }
        
        PEFReading latestReading = null;
        if (snapshot.exists() && snapshot.hasChildren()) {
            for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                latestReading = childSnapshot.getValue(PEFReading.class);
                break;
            }
        }
        
        Zone zone = Zone.UNKNOWN;
        double percentage = 0.0;
        String lastPEFDate = null;
        
        if (latestReading != null) {
            int pefValue = latestReading.getValue();
            zone = ZoneCalculator.calculateZone(pefValue, personalBest);
            percentage = ZoneCalculator.calculatePercentage(pefValue, personalBest);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            lastPEFDate = sdf.format(new Date(latestReading.getTimestamp()));
        }
        
        ChildZoneInfo info = new ChildZoneInfo(child, zone, percentage, lastPEFDate, latestReading);
        updateChildZoneInfo(info);
    }

    private void detachChildrenZoneListeners() {
        for (Map.Entry<String, Query> entry : childPEFQueries.entrySet()) {
            String childId = entry.getKey();
            Query query = entry.getValue();
            ValueEventListener listener = childPEFListeners.get(childId);
            if (query != null && listener != null) {
                query.removeEventListener(listener);
            }
        }
        childPEFQueries.clear();
        childPEFListeners.clear();
        
        for (Map.Entry<String, DatabaseReference> entry : childAccountRefs.entrySet()) {
            String childId = entry.getKey();
            DatabaseReference ref = entry.getValue();
            ValueEventListener listener = childAccountListeners.get(childId);
            if (ref != null && listener != null) {
                ref.removeEventListener(listener);
            }
        }
        childAccountRefs.clear();
        childAccountListeners.clear();
        
        latestChildAccounts.clear();
    }

    private void updateChildZoneInfo(ChildZoneInfo newInfo) {
        if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            return;
        }
        getActivity().runOnUiThread(() -> {
            if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
                return;
            }
            boolean found = false;
            for (int i = 0; i < childrenZoneInfo.size(); i++) {
                if (childrenZoneInfo.get(i).child.getID().equals(newInfo.child.getID())) {
                    childrenZoneInfo.set(i, newInfo);
                    if (adapter != null) {
                        adapter.notifyItemChanged(i);
                    }
                    found = true;
                    if (selectedChildInfo != null && selectedChildInfo.child.getID().equals(newInfo.child.getID())) {
                        selectedChildInfo = newInfo;
                        updateButtonsForSelectedChild();
                    }
                    return;
                }
            }
            if (!found) {
                childrenZoneInfo.add(newInfo);
                if (adapter != null) {
                    adapter.notifyItemInserted(childrenZoneInfo.size() - 1);
                }
                if (selectedChildInfo == null && childrenZoneInfo.size() == 1) {
                    onChildSelected(newInfo, 0);
                }
            }
        });
    }

    private void onChildSelected(ChildZoneInfo info, int position) {
        selectedChildInfo = info;
        updateButtonsForSelectedChild();
        updateButtonsVisibility(true);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void updateButtonsForSelectedChild() {
        if (selectedChildInfo == null) {
            return;
        }
        
        textViewCurrentChild.setText("Current Child: " + selectedChildInfo.child.getName());
    }

    private void setupButtons() {
        buttonSignIn.setOnClickListener(v -> {
            if (selectedChildInfo != null) {
                SignInChildProfileActivity.currentChild = selectedChildInfo.child;
                UserManager.currentUser = selectedChildInfo.child;
                UserManager.mAuth.signOut();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
        
        buttonDailyCheckin.setOnClickListener(v -> {
            if (selectedChildInfo != null) {
                SignInChildProfileActivity.currentChild = selectedChildInfo.child;
                Intent intent = new Intent(getActivity(), CheckInView.class);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
        
        buttonInhaler.setOnClickListener(v -> {
            if (selectedChildInfo != null) {
                SignInChildProfileActivity.currentChild = selectedChildInfo.child;
                Intent intent = new Intent(getActivity(), ParentInhalerMenu.class);
                startActivity(intent);
            }
        });
        
        buttonBadges.setOnClickListener(v -> {
            if (selectedChildInfo != null) {
                SignInChildProfileActivity.currentChild = selectedChildInfo.child;
                Intent intent = new Intent(getActivity(), ParentBadge.class);
                startActivity(intent);
            }
        });
        
        buttonDailyCheckinHistory.setOnClickListener(v -> {
            if (selectedChildInfo != null) {
                SignInChildProfileActivity.currentChild = selectedChildInfo.child;
                com.example.myapplication.dailycheckin.CheckInHistoryFilters.getInstance().setUsername(selectedChildInfo.child.getID());
                Intent intent = new Intent(getActivity(), FilterCheckInByDate.class);
                startActivity(intent);
            }
        });
        
        buttonGenerateReport.setOnClickListener(v -> {
            if (selectedChildInfo != null) {
                Intent intent = new Intent(getActivity(), ProviderReportGeneratorActivity.class);
                intent.putExtra("parentId", selectedChildInfo.child.getParent_id());
                intent.putExtra("childId", selectedChildInfo.child.getID());
                intent.putExtra("childName", selectedChildInfo.child.getName());
                startActivity(intent);
            }
        });
        
        buttonControllerSchedule.setOnClickListener(v -> {
            if (selectedChildInfo != null) {
                Intent intent = new Intent(getActivity(), ControllerScheduleActivity.class);
                intent.putExtra("parentId", selectedChildInfo.child.getParent_id());
                intent.putExtra("childId", selectedChildInfo.child.getID());
                intent.putExtra("childName", selectedChildInfo.child.getName());
                startActivity(intent);
            }
        });
        
        buttonSetPersonalBest.setOnClickListener(v -> {
            if (selectedChildInfo != null) {
                Intent intent = new Intent(getActivity(), SetPersonalBestActivity.class);
                intent.putExtra("parentId", selectedChildInfo.child.getParent_id());
                intent.putExtra("childId", selectedChildInfo.child.getID());
                intent.putExtra("childName", selectedChildInfo.child.getName());
                startActivity(intent);
            }
        });
        
        buttonIncidentHistory.setOnClickListener(v -> {
            if (selectedChildInfo != null) {
                Intent intent = new Intent(getActivity(), IncidentHistoryActivity.class);
                intent.putExtra("parentId", selectedChildInfo.child.getParent_id());
                intent.putExtra("childId", selectedChildInfo.child.getID());
                intent.putExtra("childName", selectedChildInfo.child.getName());
                startActivity(intent);
            }
        });
        
        buttonDeleteChild.setOnClickListener(v -> {
            if (selectedChildInfo != null) {
                int position = -1;
                for (int i = 0; i < childrenZoneInfo.size(); i++) {
                    if (childrenZoneInfo.get(i).child.getID().equals(selectedChildInfo.child.getID())) {
                        position = i;
                        break;
                    }
                }
                if (position >= 0) {
                    deleteChild(selectedChildInfo.child, position);
                }
            }
        });
        
        buttonPEFHistory.setOnClickListener(v -> {
            if (selectedChildInfo != null) {
                Intent intent = new Intent(getActivity(), PEFHistoryActivity.class);
                intent.putExtra("childId", selectedChildInfo.child.getID());
                intent.putExtra("parentId", selectedChildInfo.child.getParent_id());
                startActivity(intent);
            }
        });
        
        buttonActionPlan.setOnClickListener(v -> {
            if (selectedChildInfo != null) {
                Intent intent = new Intent(getActivity(), ActionPlanActivity.class);
                intent.putExtra("parentId", selectedChildInfo.child.getParent_id());
                startActivity(intent);
            }
        });
        
        buttonModifyNotes.setOnClickListener(v -> {
            if (selectedChildInfo != null) {
                Intent intent = new Intent(getActivity(), EditNotesActivity.class);
                intent.putExtra("parentId", selectedChildInfo.child.getParent_id());
                intent.putExtra("childId", selectedChildInfo.child.getID());
                intent.putExtra("childName", selectedChildInfo.child.getName());
                startActivity(intent);
            }
        });
    }

    private void updateButtonsVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        buttonSignIn.setVisibility(visibility);
        buttonDailyCheckin.setVisibility(visibility);
        buttonSetPersonalBest.setVisibility(visibility);
        buttonInhaler.setVisibility(visibility);
        buttonModifyNotes.setVisibility(visibility);
        buttonBadges.setVisibility(visibility);
        buttonGenerateReport.setVisibility(visibility);
        buttonControllerSchedule.setVisibility(visibility);
        buttonDailyCheckinHistory.setVisibility(visibility);
        buttonIncidentHistory.setVisibility(visibility);
        buttonDeleteChild.setVisibility(visibility);
        buttonActionPlan.setVisibility(visibility);
        buttonPEFHistory.setVisibility(visibility);
    }

    private void deleteChild(ChildAccount child, int position) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Child")
                .setMessage("Are you sure you want to delete " + child.getName() + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    DatabaseReference childRef = UserManager.mDatabase
                            .child("users")
                            .child(child.getParent_id())
                            .child("children")
                            .child(child.getID());
                    
                    childRef.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            childrenZoneInfo.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, childrenZoneInfo.size() - position);
                            if (selectedChildInfo != null && selectedChildInfo.child.getID().equals(child.getID())) {
                                selectedChildInfo = null;
                                updateButtonsVisibility(false);
                                textViewCurrentChild.setText("Current Child");
                            }
                            android.widget.Toast.makeText(getContext(), "Child deleted successfully", android.widget.Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Failed to delete child", task.getException());
                            android.widget.Toast.makeText(getContext(), "Failed to delete child", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static class ChildZoneInfo {
        ChildAccount child;
        Zone zone;
        double percentage;
        String lastPEFDate;
        PEFReading latestReading;

        ChildZoneInfo(ChildAccount child, Zone zone, double percentage, String lastPEFDate, PEFReading latestReading) {
            this.child = child;
            this.zone = zone;
            this.percentage = percentage;
            this.lastPEFDate = lastPEFDate;
            this.latestReading = latestReading;
        }
    }

    private interface ChildSelectionListener {
        void onChildSelected(ChildZoneInfo info, int position);
    }

    private class SimpleChildAdapter extends RecyclerView.Adapter<SimpleChildAdapter.ViewHolder> {
        private final List<ChildZoneInfo> children;
        private final ChildSelectionListener selectionListener;

        public SimpleChildAdapter(List<ChildZoneInfo> children, ChildSelectionListener selectionListener) {
            this.children = children;
            this.selectionListener = selectionListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_child_card_simple, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position < 0 || position >= children.size()) {
                return;
            }
            ChildZoneInfo info = children.get(position);
            holder.textViewChildName.setText(info.child.getName());
            
            String notes = info.child.getNotes();
            if (notes != null && !notes.trim().isEmpty()) {
                holder.textViewNotes.setText(notes);
            } else {
                holder.textViewNotes.setText("No notes");
            }
            
            holder.itemView.setOnClickListener(v -> {
                if (selectionListener != null) {
                    selectionListener.onChildSelected(info, position);
                }
            });
            
            if (selectedChildInfo != null && selectedChildInfo.child.getID().equals(info.child.getID())) {
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
}
