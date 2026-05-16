package com.fountainpdl.fountainplay.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MediaScanner {

    private static final long MIN_AUDIO_DURATION = 10_000; // 10 seconds
    private static final long MIN_VIDEO_SIZE = 100_000;    // 100 KB
    private static final long MIN_VIDEO_DURATION = 1_000;  // 1 second

    public static List<MediaItem> scanAudio(Context context) {
        return scanAudio(context, null);
    }

    public static List<MediaItem> scanAudio(Context context, Set<String> allowedFolders) {
        List<MediaItem> items = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] proj = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        };
        // Only real audio files, not recordings under 10s
        String selection = MediaStore.Audio.Media.IS_MUSIC + "=1 AND "
            + MediaStore.Audio.Media.DURATION + ">= " + MIN_AUDIO_DURATION;

        try (Cursor c = cr.query(uri, proj, selection, null,
                MediaStore.Audio.Media.DATE_ADDED + " DESC")) {
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

                // Folder filter
                if (allowedFolders != null && !allowedFolders.isEmpty()) {
                    String folder = path.substring(0, path.lastIndexOf('/'));
                    if (!allowedFolders.contains(folder)) continue;
                }

                MediaItem item = new MediaItem(
                    c.getLong(iId), c.getString(iTitle),
                    c.getString(iArtist), path,
                    c.getLong(iDur), MediaItem.TYPE_AUDIO);
                item.setAlbum(c.getString(iAlbum));
                item.setSize(c.getLong(iSize));
                item.setDateAdded(c.getLong(iDate));
                item.setAlbumArtUri(Uri.withAppendedPath(
                    Uri.parse("content://media/external/audio/albumart"),
                    String.valueOf(c.getLong(iAlbumId))).toString());
                items.add(item);
            }
        }
        return items;
    }

    public static List<MediaItem> scanVideo(Context context) {
        return scanVideo(context, null);
    }

    public static List<MediaItem> scanVideo(Context context, Set<String> allowedFolders) {
        List<MediaItem> items = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] proj = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED
        };
        // Filter: must have real size and duration
        String selection = MediaStore.Video.Media.SIZE + " > " + MIN_VIDEO_SIZE
            + " AND " + MediaStore.Video.Media.DURATION + " > " + MIN_VIDEO_DURATION;

        try (Cursor c = cr.query(uri, proj, selection, null,
                MediaStore.Video.Media.DATE_ADDED + " DESC")) {
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

                if (allowedFolders != null && !allowedFolders.isEmpty()) {
                    String folder = path.substring(0, path.lastIndexOf('/'));
                    if (!allowedFolders.contains(folder)) continue;
                }

                MediaItem item = new MediaItem(
                    c.getLong(iId), c.getString(iTitle),
                    "", path, c.getLong(iDur), MediaItem.TYPE_VIDEO);
                item.setSize(c.getLong(iSize));
                item.setDateAdded(c.getLong(iDate));
                items.add(item);
            }
        }
        return items;
    }

    /** Returns all unique folders containing audio files */
    public static List<String> getAudioFolders(Context context) {
        List<String> folders = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (MediaItem item : scanAudio(context)) {
            String folder = item.getPath().substring(0, item.getPath().lastIndexOf('/'));
            if (seen.add(folder)) folders.add(folder);
        }
        return folders;
    }

    /** Returns all unique folders containing video files */
    public static List<String> getVideoFolders(Context context) {
        List<String> folders = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (MediaItem item : scanVideo(context)) {
            String folder = item.getPath().substring(0, item.getPath().lastIndexOf('/'));
            if (seen.add(folder)) folders.add(folder);
        }
        return folders;
    }
}
