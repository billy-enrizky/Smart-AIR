package com.example.myapplication.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.myapplication.data.dao.ChildDao;
import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.models.Child;

import java.util.List;

public class ChildRepository {

    private final ChildDao childDao;

    public ChildRepository(Context context) {
        childDao = AppDatabase.getInstance(context).childDao();
    }

    public LiveData<List<Child>> getChildren() {
        return childDao.getAllChildren();
    }

    public void addChild(Child child) {
        new Thread(() -> childDao.addChild(child)).start();
    }

    public void updateChild(Child child) {
        new Thread(() -> childDao.updateChild(child)).start();
    }

    public void deleteChild(int id) {
        new Thread(() -> childDao.deleteChild(id)).start();
    }
}
