package com.fountainpdl.fountainplay.util;

import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.ArrayList;
import java.util.List;

/** The list of videos the player was opened from, so Next / Previous work. Kept in memory (an Intent would hit size limits). */
public final class VideoQueue {
    private static List<MediaItem> items = new ArrayList<>();
    private static int index = 0;

    private VideoQueue() {}

    public static synchronized void set(List<MediaItem> list, int startIndex) {
        items = new ArrayList<>(list);
        index = items.isEmpty() ? 0 : Math.max(0, Math.min(startIndex, items.size() - 1));
    }

    public static synchronized List<MediaItem> get() { return new ArrayList<>(items); }
    public static synchronized int getIndex() { return index; }

    public static synchronized void clear() { items = new ArrayList<>(); index = 0; }
}
