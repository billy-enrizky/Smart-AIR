package com.example.myapplication.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.models.Inhaler;

@Dao
public interface InhalerDao {

    @Query("SELECT * FROM inhalers WHERE childId = :childId LIMIT 1")
    LiveData<Inhaler> getInhaler(int childId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveInhaler(Inhaler inhaler);
}
