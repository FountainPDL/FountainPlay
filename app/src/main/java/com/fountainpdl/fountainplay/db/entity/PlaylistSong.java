package com.fountainpdl.fountainplay.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlist_songs")
public class PlaylistSong {
    @PrimaryKey(autoGenerate = true) public int id;
    public int playlistId;
    public String path, title, artist, albumArtUri;
    public long duration;
    public int position;
}
