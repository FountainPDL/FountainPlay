package com.fountainpdl.fountainplay.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.ArrayList;
import java.util.List;

/** Reads audio and video from MediaStore. Folder filtering is done by LibraryRepo. */
public class MediaScanner {
    private static final long MIN_AUDIO_DURATION = 1_000;   // 1 second: keeps podcasts, audiobooks, voice notes
    private static final long MIN_VIDEO_SIZE     = 50_000;  // 50 KB
    private static final long MIN_VIDEO_DURATION = 500;

    public static List<MediaItem> scanAudio(Context context) {
        List<MediaItem> items = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] proj = {
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATE_ADDED
        };
        // Everything that is real audio. Ringtones, alarms and notification sounds are left out.
        String selection = MediaStore.Audio.Media.IS_RINGTONE + "=0 AND "
            + MediaStore.Audio.Media.IS_ALARM + "=0 AND "
            + MediaStore.Audio.Media.IS_NOTIFICATION + "=0 AND "
            + MediaStore.Audio.Media.DURATION + ">=" + MIN_AUDIO_DURATION;
        try (Cursor c = cr.query(uri, proj, selection, null, MediaStore.Audio.Media.DATE_ADDED + " DESC")) {
            if (c == null) return items;
            int iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int iPath = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int iSize = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            int iDate = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);
            while (c.moveToNext()) {
                String path = c.getString(iPath);
                if (path == null) continue;
                MediaItem item = new MediaItem(c.getLong(iId), c.getString(iTitle),
                    c.getString(iArtist), path, c.getLong(iDur), MediaItem.TYPE_AUDIO);
                item.setAlbum(c.getString(iAlbum));
                item.setSize(c.getLong(iSize));
                item.setDateAdded(c.getLong(iDate));
                item.setAlbumArtUri(Uri.withAppendedPath(
                    Uri.parse("content://media/external/audio/albumart"),
                    String.valueOf(c.getLong(iAlbumId))).toString());
                items.add(item);
            }
        } catch (Exception ignored) { }
        return items;
    }

    public static List<MediaItem> scanVideo(Context context) {
        List<MediaItem> items = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] proj = {
            MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION, MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DATE_ADDED
        };
        String selection = MediaStore.Video.Media.SIZE + ">" + MIN_VIDEO_SIZE
            + " AND " + MediaStore.Video.Media.DURATION + ">" + MIN_VIDEO_DURATION;
        try (Cursor c = cr.query(uri, proj, selection, null, MediaStore.Video.Media.DATE_ADDED + " DESC")) {
            if (c == null) return items;
            int iId = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int iTitle = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
            int iPath = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            int iDur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            int iSize = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            int iDate = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);
            while (c.moveToNext()) {
                String path = c.getString(iPath);
                if (path == null) continue;
                MediaItem item = new MediaItem(c.getLong(iId), c.getString(iTitle),
                    "", path, c.getLong(iDur), MediaItem.TYPE_VIDEO);
                item.setSize(c.getLong(iSize));
                item.setDateAdded(c.getLong(iDate));
                items.add(item);
            }
        } catch (Exception ignored) { }
        return items;
    }
}
