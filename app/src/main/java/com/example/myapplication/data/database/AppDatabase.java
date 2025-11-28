package com.example.myapplication.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.myapplication.data.dao.InhalerDao;
import com.example.myapplication.models.Inhaler;

@Database(entities = {Inhaler.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract InhalerDao inhalerDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "asthma_app_db"
            ).fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}
