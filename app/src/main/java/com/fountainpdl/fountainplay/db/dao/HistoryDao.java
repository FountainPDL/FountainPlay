package com.fountainpdl.fountainplay.db.dao;

import androidx.room.*;
import com.fountainpdl.fountainplay.db.entity.HistoryItem;
import java.util.List;

@Dao
public interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) void insert(HistoryItem item);
    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 200") List<HistoryItem> getAll();
    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT :limit") List<HistoryItem> getRecent(int limit);
    @Query("DELETE FROM history WHERE path = :path") void deleteByPath(String path);
    @Query("DELETE FROM history") void clearAll();
    @Query("SELECT COUNT(*) FROM history") int count();
}
