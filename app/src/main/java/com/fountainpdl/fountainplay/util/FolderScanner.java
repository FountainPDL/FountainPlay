package com.fountainpdl.fountainplay.util;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** File-system side of the library: the Folders tab, .nomedia handling and user-added folders. */
public final class FolderScanner {
    private FolderScanner() {}

    private static final Set<String> AUDIO_EXT = new HashSet<>(Arrays.asList(
        "mp3", "m4a", "aac", "flac", "wav", "ogg", "oga", "opus", "wma", "amr", "mka", "ac3", "eac3", "dts", "alac", "mid", "midi"));
    private static final Set<String> VIDEO_EXT = new HashSet<>(Arrays.asList(
        "mp4", "mkv", "avi", "mov", "flv", "webm", "wmv", "m4v", "3gp", "ts", "mpg", "mpeg", "m2ts"));

    public static String ext(String name) {
        int i = name == null ? -1 : name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }
    public static boolean isAudio(String name) { return AUDIO_EXT.contains(ext(name)); }
    public static boolean isVideo(String name) { return VIDEO_EXT.contains(ext(name)); }
    public static boolean isMedia(String name) { return isAudio(name) || isVideo(name); }

    public static String parentOf(String path) {
        int i = path == null ? -1 : path.lastIndexOf('/');
        return i > 0 ? path.substring(0, i) : "/";
    }
    public static String nameOf(String path) {
        int i = path == null ? -1 : path.lastIndexOf('/');
        return i >= 0 ? path.substring(i + 1) : String.valueOf(path);
    }

    public static boolean hasNoMedia(File dir) { return new File(dir, ".nomedia").exists(); }

    /** True when path is the folder itself or inside it. */
    public static boolean isInside(String path, String folder) {
        return path.equals(folder) || path.startsWith(folder + "/");
    }

    public static MediaItem fromFile(File f, boolean readDuration) {
        String name = f.getName();
        boolean video = isVideo(name);
        int dot = name.lastIndexOf('.');
        String title = dot > 0 ? name.substring(0, dot) : name;
        MediaItem m = new MediaItem(0, title, null, f.getAbsolutePath(), 0,
            video ? MediaItem.TYPE_VIDEO : MediaItem.TYPE_AUDIO);
        m.setSize(f.length());
        m.setDateAdded(f.lastModified() / 1000);
        if (readDuration) {
            MediaMetadataRetriever r = new MediaMetadataRetriever();
            try {
                r.setDataSource(f.getAbsolutePath());
                String d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (d != null) m.setDuration(Long.parseLong(d));
                if (!video) {
                    String artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    String t = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                    if (artist != null) m.setArtist(artist);
                    if (t != null && !t.isEmpty()) m.setTitle(t);
                }
            } catch (Exception ignored) {
            } finally {
                try { r.release(); } catch (Exception ignored) { }
            }
        }
        return m;
    }

    /** Recursively collects media files, including inside hidden and .nomedia folders (the user asked for them). */
    public static List<MediaItem> scanTree(File root, int maxFiles, boolean readDuration) {
        List<MediaItem> out = new ArrayList<>();
        walk(root, out, maxFiles, readDuration, 0);
        return out;
    }

    private static void walk(File dir, List<MediaItem> out, int max, boolean dur, int depth) {
        if (depth > 12 || out.size() >= max) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (out.size() >= max) return;
            if (f.isDirectory()) walk(f, out, max, dur, depth + 1);
            else if (isMedia(f.getName())) out.add(fromFile(f, dur));
        }
    }

    public static final class Entry {
        public File file;
        public boolean isDir;
        public boolean noMedia;   // directory that the system scanner skips
        public boolean hidden;    // name starts with a dot
    }

    /** Folders first, then media files. Dot-files and dot-folders only when showHidden is on. */
    public static List<Entry> list(File dir, boolean showHidden) {
        List<Entry> out = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return out;
        for (File f : files) {
            boolean hidden = f.getName().startsWith(".");
            if (hidden && !showHidden) continue;
            Entry e = new Entry();
            e.file = f;
            e.hidden = hidden;
            if (f.isDirectory()) {
                e.isDir = true;
                e.noMedia = hasNoMedia(f);
                out.add(e);
            } else if (isMedia(f.getName())) {
                out.add(e);
            }
        }
        Collections.sort(out, (a, b) -> {
            if (a.isDir != b.isDir) return a.isDir ? -1 : 1;
            return a.file.getName().compareToIgnoreCase(b.file.getName());
        });
        return out;
    }

    /** Internal storage plus any SD card / USB volumes. */
    public static List<File> storageRoots(Context ctx) {
        List<File> roots = new ArrayList<>();
        File primary = Environment.getExternalStorageDirectory();
        roots.add(primary);
        File[] dirs = ctx.getExternalFilesDirs(null);
        if (dirs != null) {
            for (File d : dirs) {
                if (d == null) continue;
                String p = d.getAbsolutePath();
                int i = p.indexOf("/Android/data");
                if (i <= 0) continue;
                File root = new File(p.substring(0, i));
                if (!root.getAbsolutePath().equals(primary.getAbsolutePath()) && !roots.contains(root)) roots.add(root);
            }
        }
        return roots;
    }

    /** Converts a folder picked with the system file picker into a normal path, or null. */
    public static String fromTreeUri(Uri tree) {
        try {
            String docId = DocumentsContract.getTreeDocumentId(tree);
            int colon = docId.indexOf(':');
            String volume = colon >= 0 ? docId.substring(0, colon) : docId;
            String rel = colon >= 0 ? docId.substring(colon + 1) : "";
            File base = "primary".equalsIgnoreCase(volume)
                ? Environment.getExternalStorageDirectory() : new File("/storage/" + volume);
            File f = rel.isEmpty() ? base : new File(base, rel);
            return f.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    public static String prettyName(File f, Context ctx) {
        if (f.equals(Environment.getExternalStorageDirectory())) return "Internal storage";
        return f.getName().isEmpty() ? f.getAbsolutePath() : f.getName();
    }
}
