package com.fountainpdl.fountainplay.util;

import android.content.Context;
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.dao.PlaylistDao;
import com.fountainpdl.fountainplay.db.entity.PlaylistEntity;
import com.fountainpdl.fountainplay.db.entity.PlaylistSong;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.ArrayList;
import java.util.List;

/** The built-in "Favourites" playlist. All methods touch the database: call them off the main thread. */
public final class Favourites {
    public static final String NAME = "Favourites";
    private Favourites() {}

    private static PlaylistEntity find(PlaylistDao dao) {
        for (PlaylistEntity p : dao.getAllPlaylists()) if (NAME.equals(p.name)) return p;
        return null;
    }

    public static boolean isFavourite(Context c, String path) {
        if (path == null) return false;
        PlaylistDao dao = AppDatabase.get(c).playlistDao();
        PlaylistEntity fav = find(dao);
        if (fav == null) return false;
        for (PlaylistSong s : dao.getSongsForPlaylist(fav.id)) if (path.equals(s.path)) return true;
        return false;
    }

    /** @return the new state: true = now a favourite */
    public static boolean toggle(Context c, MediaItem item) {
        PlaylistDao dao = AppDatabase.get(c).playlistDao();
        PlaylistEntity fav = find(dao);
        if (fav == null) {
            PlaylistEntity n = new PlaylistEntity();
            n.name = NAME;
            n.createdAt = System.currentTimeMillis();
            n.id = (int) dao.insertPlaylist(n);
            fav = n;
        }
        boolean already = false;
        for (PlaylistSong s : dao.getSongsForPlaylist(fav.id)) {
            if (item.getPath() != null && item.getPath().equals(s.path)) { already = true; break; }
        }
        boolean now;
        if (already) {
            dao.removeSong(fav.id, item.getPath());
            now = false;
        } else {
            dao.insertSong(toSong(fav.id, item));
            now = true;
        }
        dao.updateCount(fav.id);
        return now;
    }

    public static List<MediaItem> getAll(Context c) {
        List<MediaItem> out = new ArrayList<>();
        PlaylistDao dao = AppDatabase.get(c).playlistDao();
        PlaylistEntity fav = find(dao);
        if (fav == null) return out;
        for (PlaylistSong s : dao.getSongsForPlaylist(fav.id)) out.add(toItem(s));
        return out;
    }

    public static PlaylistSong toSong(int playlistId, MediaItem item) {
        PlaylistSong s = new PlaylistSong();
        s.playlistId = playlistId;
        s.path = item.getPath();
        s.title = item.getTitle();
        s.artist = item.getArtist();
        s.albumArtUri = item.getAlbumArtUri();
        s.duration = item.getDuration();
        return s;
    }

    public static MediaItem toItem(PlaylistSong s) {
        MediaItem m = new MediaItem(0, s.title, s.artist, s.path, s.duration, MediaItem.TYPE_AUDIO);
        m.setAlbumArtUri(s.albumArtUri);
        return m;
    }
}
