package com.example.myapplication.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ParentAccount;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {
    private static final String TAG = "NotificationActivity";

    private RecyclerView recyclerViewNotifications;
    private TextView textViewEmpty;
    private NotificationAdapter adapter;
    private List<NotificationItem> notifications;
    private String parentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (!(UserManager.currentUser instanceof ParentAccount)) {
            Log.e(TAG, "Current user is not a ParentAccount");
            finish();
            return;
        }

        ParentAccount parentAccount = (ParentAccount) UserManager.currentUser;
        parentId = parentAccount.getID();

        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        Button buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        notifications = new ArrayList<>();
        adapter = new NotificationAdapter(notifications);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewNotifications.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        DatabaseReference notificationRef = UserManager.mDatabase
                .child("users")
                .child(parentId)
                .child("notifications");

        Query query = notificationRef.orderByChild("timestamp");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                notifications.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        NotificationItem notification = child.getValue(NotificationItem.class);
                        if (notification != null) {
                            notification.setNotificationId(child.getKey());
                            notifications.add(notification);
                        }
                    }
                }
                Collections.reverse(notifications);
                adapter.notifyDataSetChanged();

                if (notifications.isEmpty()) {
                    textViewEmpty.setVisibility(View.VISIBLE);
                    recyclerViewNotifications.setVisibility(View.GONE);
                } else {
                    textViewEmpty.setVisibility(View.GONE);
                    recyclerViewNotifications.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error loading notifications", error.toException());
            }
        });
    }

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private List<NotificationItem> notifications;

        public NotificationAdapter(List<NotificationItem> notifications) {
            this.notifications = notifications;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            NotificationItem notification = notifications.get(position);

            String typeText = getTypeText(notification.getType());
            holder.textViewNotificationType.setText(typeText);
            holder.textViewNotificationChild.setText(notification.getChildName());
            holder.textViewNotificationMessage.setText(notification.getMessage());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            holder.textViewNotificationTime.setText(sdf.format(new Date(notification.getTimestamp())));

            if (notification.isRead()) {
                holder.buttonMarkRead.setText("Mark as Unread");
            } else {
                holder.buttonMarkRead.setText("Mark as Read");
            }

            holder.buttonMarkRead.setOnClickListener(v -> {
                boolean newReadState = !notification.isRead();
                notification.setRead(newReadState);
                NotificationManager.markAsRead(parentId, notification.getNotificationId());
                adapter.notifyItemChanged(position);
            });

            holder.buttonDelete.setOnClickListener(v -> {
                NotificationManager.deleteNotification(parentId, notification.getNotificationId());
                notifications.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, notifications.size() - position);

                if (notifications.isEmpty()) {
                    textViewEmpty.setVisibility(View.VISIBLE);
                    recyclerViewNotifications.setVisibility(View.GONE);
                }
            });
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        private String getTypeText(NotificationItem.NotificationType type) {
            switch (type) {
                case RED_ZONE_DAY:
                    return "Red Zone Alert";
                case RAPID_RESCUE:
                    return "Rapid Rescue Alert";
                case WORSE_AFTER_DOSE:
                    return "Worse After Dose";
                case TRIAGE_ESCALATION:
                    return "Triage Escalation";
                case INVENTORY_LOW:
                    return "Inventory Low";
                case INVENTORY_EXPIRED:
                    return "Inventory Expired";
                default:
                    return "Notification";
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewNotificationType;
            TextView textViewNotificationTime;
            TextView textViewNotificationChild;
            TextView textViewNotificationMessage;
            Button buttonMarkRead;
            Button buttonDelete;

            ViewHolder(View itemView) {
                super(itemView);
                textViewNotificationType = itemView.findViewById(R.id.textViewNotificationType);
                textViewNotificationTime = itemView.findViewById(R.id.textViewNotificationTime);
                textViewNotificationChild = itemView.findViewById(R.id.textViewNotificationChild);
                textViewNotificationMessage = itemView.findViewById(R.id.textViewNotificationMessage);
                buttonMarkRead = itemView.findViewById(R.id.buttonMarkRead);
                buttonDelete = itemView.findViewById(R.id.buttonDelete);
            }
        }
    }
}

