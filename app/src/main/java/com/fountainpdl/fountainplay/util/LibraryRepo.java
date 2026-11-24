package com.fountainpdl.fountainplay.util;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * In-memory cache of the library. Every screen reads from here, so tabs open instantly and agree with each other.
 * The visible lists leave out folders the user hid; the "all" lists (used by the Folders tab) do not.
 */
public final class LibraryRepo {
    public interface Listener { void onLibraryChanged(); }

    private static LibraryRepo instance;

    private final Context app;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private volatile List<MediaItem> audio = new ArrayList<>();
    private volatile List<MediaItem> video = new ArrayList<>();
    private volatile List<MediaItem> audioAll = new ArrayList<>();
    private volatile List<MediaItem> videoAll = new ArrayList<>();
    private volatile boolean loaded = false;
    private boolean loading = false;
    private boolean pending = false;

    private final Runnable delayedRefresh = this::refresh;
    private final ContentObserver observer = new ContentObserver(main) {
        @Override public void onChange(boolean selfChange) {
            main.removeCallbacks(delayedRefresh);
            main.postDelayed(delayedRefresh, 1500);
        }
    };

    private LibraryRepo(Context c) {
        app = c.getApplicationContext();
        try {
            app.getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer);
            app.getContentResolver().registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer);
        } catch (Exception ignored) { }
    }

    public static synchronized LibraryRepo get(Context c) {
        if (instance == null) instance = new LibraryRepo(c);
        return instance;
    }

    public boolean isLoaded() { return loaded; }
    public List<MediaItem> getAudio() { return audio; }
    public List<MediaItem> getVideo() { return video; }
    public List<MediaItem> getAudioAll() { return audioAll; }
    public List<MediaItem> getVideoAll() { return videoAll; }

    public void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }

    public void ensureLoaded() { if (!loaded) refresh(); }

    public void refresh() {
        main.post(() -> {
            if (loading) { pending = true; return; }
            loading = true;
            io.execute(this::load);
        });
    }

    private void load() {
        List<MediaItem> a = new ArrayList<>();
        List<MediaItem> v = new ArrayList<>();
        try {
            AppPreferences prefs = new AppPreferences(app);
            a.addAll(MediaScanner.scanAudio(app));
            v.addAll(MediaScanner.scanVideo(app));

            // Folders hidden from the system scan by .nomedia, added explicitly by the user
            Set<String> known = new HashSet<>();
            for (MediaItem m : a) known.add(m.getPath());
            for (MediaItem m : v) known.add(m.getPath());
            for (String extra : prefs.getExtraFolders()) {
                File dir = new File(extra);
                if (!dir.isDirectory()) continue;
                for (MediaItem m : FolderScanner.scanTree(dir, 3000, true)) {
                    if (!known.add(m.getPath())) continue;
                    if (m.isVideo()) v.add(m); else a.add(m);
                }
            }
            sortNewestFirst(a);
            sortNewestFirst(v);

            audioAll = a;
            videoAll = v;
            Set<String> excluded = prefs.getExcludedFolders();
            audio = withoutExcluded(a, excluded);
            video = withoutExcluded(v, excluded);
        } catch (Throwable ignored) {
        }
        main.post(() -> {
            loaded = true;
            loading = false;
            for (Listener l : listeners) l.onLibraryChanged();
            if (pending) { pending = false; refresh(); }
        });
    }

    private static void sortNewestFirst(List<MediaItem> l) {
        Collections.sort(l, (x, y) -> Long.compare(y.getDateAdded(), x.getDateAdded()));
    }

    private static List<MediaItem> withoutExcluded(List<MediaItem> src, Set<String> excluded) {
        if (excluded.isEmpty()) return new ArrayList<>(src);
        List<MediaItem> out = new ArrayList<>();
        for (MediaItem m : src) {
            boolean hide = false;
            for (String e : excluded) {
                if (FolderScanner.isInside(m.getPath(), e)) { hide = true; break; }
            }
            if (!hide) out.add(m);
        }
        return out;
    }
}
