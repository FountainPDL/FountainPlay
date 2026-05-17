package com.fountainpdl.fountainplay.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists")
public class PlaylistEntity {
    @PrimaryKey(autoGenerate = true) public int id;
    public String name;
    public long createdAt;
    public int songCount;
}
