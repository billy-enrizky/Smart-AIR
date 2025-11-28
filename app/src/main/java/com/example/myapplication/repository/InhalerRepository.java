package com.example.myapplication.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.myapplication.data.dao.InhalerDao;
import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.models.Inhaler;

public class InhalerRepository {

    private final InhalerDao dao;

    public InhalerRepository(Context context) {
        dao = AppDatabase.getInstance(context).inhalerDao();
    }

    public LiveData<Inhaler> getInhaler(int childId) {
        return dao.getInhaler(childId);
    }

    public void saveInhaler(Inhaler inhaler) {
        new Thread(() -> dao.saveInhaler(inhaler)).start();
    }

    public boolean needsReminder(Inhaler inh) {
        long now = System.currentTimeMillis();

        if (inh.amountLeft < 20) return true;

        long sevenDays = 7L * 24 * 60 * 60 * 1000;

        return inh.expiryDate - now <= sevenDays;
    }
}
