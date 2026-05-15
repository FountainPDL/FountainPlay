package com.fountainpdl.fountainplay.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.ArrayList;
import java.util.List;

public class MediaScanner {

    public static List<MediaItem> scanAudio(Context context) {
        List<MediaItem> items = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] proj = {
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.ALBUM_ID
        };
        try (Cursor c = cr.query(uri, proj, null, null, MediaStore.Audio.Media.TITLE + " ASC")) {
            if (c == null) return items;
            int iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int iPath = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int iSize = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            while (c.moveToNext()) {
                long dur = c.getLong(iDur);
                if (dur < 1000) continue;
                MediaItem item = new MediaItem(c.getLong(iId), c.getString(iTitle),
                    c.getString(iArtist), c.getString(iPath), dur, MediaItem.TYPE_AUDIO);
                item.setAlbum(c.getString(iAlbum));
                item.setSize(c.getLong(iSize));
                item.setAlbumArtUri(Uri.withAppendedPath(
                    Uri.parse("content://media/external/audio/albumart"),
                    String.valueOf(c.getLong(iAlbumId))).toString());
                items.add(item);
            }
        }
        return items;
    }

    public static List<MediaItem> scanVideo(Context context) {
        List<MediaItem> items = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] proj = {
            MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA, MediaStore.Video.Media.DURATION, MediaStore.Video.Media.SIZE
        };
        try (Cursor c = cr.query(uri, proj, null, null, MediaStore.Video.Media.DATE_MODIFIED + " DESC")) {
            if (c == null) return items;
            int iId = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int iTitle = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
            int iPath = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            int iDur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            int iSize = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            while (c.moveToNext()) {
                MediaItem item = new MediaItem(c.getLong(iId), c.getString(iTitle),
                    "", c.getString(iPath), c.getLong(iDur), MediaItem.TYPE_VIDEO);
                item.setSize(c.getLong(iSize));
                items.add(item);
            }
        }
        return items;
    }
}
