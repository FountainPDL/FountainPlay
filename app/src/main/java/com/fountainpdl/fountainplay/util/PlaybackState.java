package com.fountainpdl.fountainplay.util;

import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.ArrayList;
import java.util.List;

/** Singleton holding current playback state across the app */
public class PlaybackState {
    private static PlaybackState instance;

    private MediaItem currentItem;
    private List<MediaItem> queue = new ArrayList<>();
    private int queueIndex = 0;
    private boolean isPlaying = false;
    private long position = 0;

    public interface Listener {
        void onItemChanged(MediaItem item);
        void onPlayStateChanged(boolean playing);
        void onPositionChanged(long pos, long duration);
    }

    private final List<Listener> listeners = new ArrayList<>();

    private PlaybackState() {}

    public static PlaybackState get() {
        if (instance == null) instance = new PlaybackState();
        return instance;
    }

    public void setCurrentItem(MediaItem item) {
        currentItem = item;
        for (Listener l : listeners) l.onItemChanged(item);
    }

    public MediaItem getCurrentItem() { return currentItem; }

    public void setQueue(List<MediaItem> q, int index) {
        queue = q;
        queueIndex = index;
    }

    public List<MediaItem> getQueue() { return queue; }
    public int getQueueIndex() { return queueIndex; }

    public void setPlaying(boolean p) {
        isPlaying = p;
        for (Listener l : listeners) l.onPlayStateChanged(p);
    }

    public boolean isPlaying() { return isPlaying; }

    public void setPosition(long pos) { this.position = pos; }
    public long getPosition() { return position; }

    public void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }

    public boolean hasMedia() { return currentItem != null; }
}
