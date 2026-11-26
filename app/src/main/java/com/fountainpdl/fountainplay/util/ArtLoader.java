package com.fountainpdl.fountainplay.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.LruCache;
import android.util.Size;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.io.File;

/**
 * Finds cover art the way music players do: the picture embedded in the file first, then the system
 * thumbnail, then a cover.jpg / folder.jpg next to the file. Blocking: call from a background thread.
 */
public final class ArtLoader {
    private ArtLoader() {}

    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(24);
    private static final String[] FOLDER_ART = {
        "cover.jpg", "cover.png", "folder.jpg", "folder.png", "album.jpg", "album.png",
        "front.jpg", "front.png", "albumart.jpg", "AlbumArtSmall.jpg", "Cover.jpg", "Folder.jpg"
    };

    public static Bitmap load(Context ctx, MediaItem item, int size) {
        if (item == null || item.getPath() == null) return null;
        String key = item.getPath() + "@" + size;
        Bitmap cached = CACHE.get(key);
        if (cached != null) return cached;

        Bitmap b = embedded(ctx, item.getPath(), size);
        if (b == null) b = systemThumbnail(ctx, item, size);
        if (b == null) b = folderCover(item.getPath(), size);
        if (b != null) CACHE.put(key, b);
        return b;
    }

    private static Bitmap embedded(Context ctx, String path, int size) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            if (path.startsWith("/")) r.setDataSource(path); else r.setDataSource(ctx, Uri.parse(path));
            byte[] pic = r.getEmbeddedPicture();
            return pic == null ? null : decode(pic, size);
        } catch (Exception e) {
            return null;
        } finally {
            try { r.release(); } catch (Exception ignored) { }
        }
    }

    private static Bitmap systemThumbnail(Context ctx, MediaItem item, int size) {
        if (Build.VERSION.SDK_INT < 29) return null;
        try {
            Uri uri = MediaUris.contentUri(ctx, item);
            if (uri == null) return null;
            return ctx.getContentResolver().loadThumbnail(uri, new Size(size, size), null);
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap folderCover(String path, int size) {
        if (!path.startsWith("/")) return null;
        File dir = new File(path).getParentFile();
        if (dir == null) return null;
        for (String name : FOLDER_ART) {
            File f = new File(dir, name);
            if (f.isFile()) {
                Bitmap b = decodeFile(f.getAbsolutePath(), size);
                if (b != null) return b;
            }
        }
        return null;
    }

    private static Bitmap decode(byte[] data, int size) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, o);
        o.inSampleSize = sample(o.outWidth, o.outHeight, size);
        o.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, o);
    }

    private static Bitmap decodeFile(String file, int size) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, o);
        o.inSampleSize = sample(o.outWidth, o.outHeight, size);
        o.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file, o);
    }

    private static int sample(int w, int h, int target) {
        int s = 1;
        while (w / (s * 2) >= target && h / (s * 2) >= target) s *= 2;
        return s;
    }
}
