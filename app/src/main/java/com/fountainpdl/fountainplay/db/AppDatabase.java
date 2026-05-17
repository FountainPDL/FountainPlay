package com.fountainpdl.fountainplay.db;

import android.content.Context;
import androidx.room.*;
import com.fountainpdl.fountainplay.db.dao.HistoryDao;
import com.fountainpdl.fountainplay.db.dao.PlaylistDao;
import com.fountainpdl.fountainplay.db.entity.*;

@Database(entities = {HistoryItem.class, PlaylistEntity.class, PlaylistSong.class},
    version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract HistoryDao historyDao();
    public abstract PlaylistDao playlistDao();

    public static synchronized AppDatabase get(Context ctx) {
        if (instance == null) {
            instance = Room.databaseBuilder(ctx.getApplicationContext(),
                AppDatabase.class, "fountain_play.db")
                .fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}
