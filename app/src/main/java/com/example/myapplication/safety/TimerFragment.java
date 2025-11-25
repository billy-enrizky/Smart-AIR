package com.example.myapplication.safety;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.myapplication.R;

public class TimerFragment extends Fragment {
    private static final long TIMER_DURATION = 10 * 60 * 1000; // 10 minutes
    private TextView textViewTimer;
    private Button buttonRecheck;
    private Button buttonDismiss;
    private CountDownTimer countDownTimer;
    private long timeRemaining = TIMER_DURATION;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer, container, false);
        
        textViewTimer = view.findViewById(R.id.textViewTimer);
        buttonRecheck = view.findViewById(R.id.buttonRecheck);
        buttonDismiss = view.findViewById(R.id.buttonDismiss);
        
        buttonRecheck.setOnClickListener(v -> {
            if (getActivity() instanceof TriageActivity) {
                ((TriageActivity) getActivity()).restartTriage();
            }
        });
        
        buttonDismiss.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
        
        startTimer();
        
        return view;
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeRemaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished;
                updateTimerDisplay(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                timeRemaining = 0;
                textViewTimer.setText("00:00");
                buttonRecheck.setVisibility(View.VISIBLE);
                buttonDismiss.setVisibility(View.VISIBLE);
            }
        };
        countDownTimer.start();
    }

    private void updateTimerDisplay(long millisUntilFinished) {
        long minutes = millisUntilFinished / 60000;
        long seconds = (millisUntilFinished % 60000) / 1000;
        String timeString = String.format("%02d:%02d", minutes, seconds);
        textViewTimer.setText(timeString);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (timeRemaining > 0 && countDownTimer == null) {
            startTimer();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}

