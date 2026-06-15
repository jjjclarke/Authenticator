package com.jjjclarke.authenticator;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Account.class}, version = 1)
public abstract class AccountDatabase extends RoomDatabase {
    public abstract AccountDao accountDao();

    private static volatile AccountDatabase INSTANCE;

    public static AccountDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AccountDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AccountDatabase.class, "account_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}