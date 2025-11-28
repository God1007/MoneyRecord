package com.example.moneyrecord.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Expense.class}, version = 1, exportSchema = false)
public abstract class MoneyDatabase extends RoomDatabase {
    private static volatile MoneyDatabase INSTANCE;

    public abstract ExpenseDao expenseDao();

    public static MoneyDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MoneyDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    MoneyDatabase.class,
                                    "money_record_db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
