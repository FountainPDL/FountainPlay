package com.fountainpdl.fountainplay.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class HistoryItem {
    @PrimaryKey(autoGenerate = true) public int id;
    public String path, title, artist, albumArtUri;
    public int type; // 0=audio, 1=video
    public long playedAt;
    public long duration;
}
