package com.example.moneyrecord.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Expense expense);

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    LiveData<List<Expense>> getAllExpenses();

    @Query("DELETE FROM expenses")
    void clearAll();
}
