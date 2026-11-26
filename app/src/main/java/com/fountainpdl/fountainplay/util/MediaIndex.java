package com.fountainpdl.fountainplay.util;

import android.content.Context;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Remembers what the file scan already learned (duration, tags) so the next scan only reads new or changed files.
 * One line per file in files/media_index.tsv. A file whose size or modified time changed is read again.
 */
public final class MediaIndex {
    private static MediaIndex instance;

    private static final class Row {
        long mtime, size, duration;
        String title, artist, album;
    }

    private final File file;
    private final Map<String, Row> rows = new HashMap<>();
    private boolean dirty = false;

    private MediaIndex(Context c) {
        file = new File(c.getApplicationContext().getFilesDir(), "media_index.tsv");
        load();
    }

    public static synchronized MediaIndex get(Context c) {
        if (instance == null) instance = new MediaIndex(c);
        return instance;
    }

    private void load() {
        if (!file.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\t", -1);
                if (p.length < 7) continue;
                Row row = new Row();
                row.mtime = Long.parseLong(p[1]);
                row.size = Long.parseLong(p[2]);
                row.duration = Long.parseLong(p[3]);
                row.title = p[4]; row.artist = p[5]; row.album = p[6];
                rows.put(p[0], row);
            }
        } catch (Exception ignored) { }
    }

    /** Fills duration and tags from the saved scan; returns false when the file is new or changed. */
    public synchronized boolean apply(MediaItem item, long mtime, long size) {
        Row r = rows.get(item.getPath());
        if (r == null || r.mtime != mtime || r.size != size) return false;
        item.setDuration(r.duration);
        if (!r.title.isEmpty()) item.setTitle(r.title);
        if (!r.artist.isEmpty()) item.setArtist(r.artist);
        if (!r.album.isEmpty()) item.setAlbum(r.album);
        return true;
    }

    public synchronized void put(MediaItem item, long mtime, long size) {
        Row r = new Row();
        r.mtime = mtime; r.size = size; r.duration = item.getDuration();
        r.title = clean(item.getTitle()); r.artist = clean(item.getArtist()); r.album = clean(item.getAlbum());
        rows.put(item.getPath(), r);
        dirty = true;
    }

    private static String clean(String s) { return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' '); }

    /** Writes the index, dropping files that no longer exist. */
    public synchronized void save() {
        if (!dirty) return;
        File tmp = new File(file.getParentFile(), "media_index.tmp");
        try (BufferedWriter w = new BufferedWriter(new FileWriter(tmp))) {
            for (Map.Entry<String, Row> e : rows.entrySet()) {
                Row r = e.getValue();
                w.write(e.getKey() + "\t" + r.mtime + "\t" + r.size + "\t" + r.duration + "\t"
                    + r.title + "\t" + r.artist + "\t" + r.album + "\n");
            }
        } catch (Exception e) { return; }
        if (tmp.renameTo(file)) dirty = false;
    }

    public synchronized void forgetMissing() {
        java.util.Iterator<String> it = rows.keySet().iterator();
        while (it.hasNext()) {
            String p = it.next();
            if (!new File(p).exists()) { it.remove(); dirty = true; }
        }
    }
}
