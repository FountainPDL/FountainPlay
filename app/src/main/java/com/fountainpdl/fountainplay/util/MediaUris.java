package com.fountainpdl.fountainplay.util;

import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.fountainpdl.fountainplay.model.MediaItem;

/** Builds shareable content:// URIs. file:// URIs crash on Android 7+ when handed to other apps. */
public final class MediaUris {
    private MediaUris() {}

    public static Uri contentUri(Context ctx, MediaItem item) {
        if (item == null) return null;
        String path = item.getPath();
        if (path != null && path.startsWith("content://")) return Uri.parse(path);
        Uri base = item.isVideo()
            ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        if (item.getId() > 0) return ContentUris.withAppendedId(base, item.getId());
        if (path == null) return null;
        try (Cursor c = ctx.getContentResolver().query(base,
                new String[]{MediaStore.MediaColumns._ID},
                MediaStore.MediaColumns.DATA + "=?", new String[]{path}, null)) {
            if (c != null && c.moveToFirst()) return ContentUris.withAppendedId(base, c.getLong(0));
        } catch (Exception ignored) { }
        return null;
    }

    /** Returns a ready-to-start chooser intent, or null when the file has no shareable URI. */
    public static Intent shareIntent(Context ctx, MediaItem item) {
        Uri uri = contentUri(ctx, item);
        if (uri == null) return null;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(item.isVideo() ? "video/*" : "audio/*");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.setClipData(ClipData.newRawUri("", uri));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(send, "Share");
    }
}
