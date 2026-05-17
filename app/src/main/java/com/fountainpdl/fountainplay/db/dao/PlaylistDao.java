package com.fountainpdl.fountainplay.db.dao;

import androidx.room.*;
import com.fountainpdl.fountainplay.db.entity.PlaylistEntity;
import com.fountainpdl.fountainplay.db.entity.PlaylistSong;
import java.util.List;

@Dao
public interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insertPlaylist(PlaylistEntity p);
    @Update void updatePlaylist(PlaylistEntity p);
    @Delete void deletePlaylist(PlaylistEntity p);
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC") List<PlaylistEntity> getAllPlaylists();
    @Query("SELECT * FROM playlists WHERE id = :id") PlaylistEntity getPlaylist(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE) void insertSong(PlaylistSong song);
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :pid ORDER BY position ASC") List<PlaylistSong> getSongsForPlaylist(int pid);
    @Query("DELETE FROM playlist_songs WHERE playlistId = :pid AND path = :path") void removeSong(int pid, String path);
    @Query("DELETE FROM playlist_songs WHERE playlistId = :pid") void clearPlaylist(int pid);
    @Query("UPDATE playlists SET songCount = (SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :pid) WHERE id = :pid") void updateCount(int pid);
}
