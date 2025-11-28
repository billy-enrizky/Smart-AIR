package com.example.myapplication.ui.inhaler;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.models.Inhaler;
import com.example.myapplication.repository.InhalerRepository;

public class InhalerReminderWorker extends Worker {

    public InhalerReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        Inhaler inhaler = db.inhalerDao().getInhaler(1).getValue();

        if (inhaler == null) return Result.success();

        InhalerRepository repo = new InhalerRepository(getApplicationContext());
        if (repo.needsReminder(inhaler)) {
            // TODO: send notification
        }

        return Result.success();
    }
}
