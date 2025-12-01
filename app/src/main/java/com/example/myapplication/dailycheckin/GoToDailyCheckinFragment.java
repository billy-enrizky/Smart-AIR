package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.SignIn.SignInView;
import com.example.myapplication.childmanaging.SignInChildProfileActivity;
import com.example.myapplication.dailycheckin.CheckInView;
import com.example.myapplication.userdata.AccountType;
import com.example.myapplication.userdata.ChildAccount;
import com.google.firebase.auth.FirebaseAuth;

public class GoToDailyCheckinFragment extends Fragment {
    public GoToDailyCheckinFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_leave_daily_checkin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button checkInButton = view.findViewById(R.id.goToDailyCheckIn);
        checkInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CheckInView.class);
                startActivity(intent);
                getActivity().finish();
            }
        });
    }
}
