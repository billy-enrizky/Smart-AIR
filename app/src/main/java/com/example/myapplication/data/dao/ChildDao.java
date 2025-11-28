package com.example.myapplication.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.models.Child;

import java.util.List;

@Dao
public interface ChildDao {

    @Query("SELECT * FROM children")
    LiveData<List<Child>> getAllChildren();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addChild(Child child);

    @Update
    void updateChild(Child child);

    @Query("DELETE FROM children WHERE id = :id")
    void deleteChild(int id);
}
