package com.fountainpdl.fountainplay.util;

import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.ArrayList;
import java.util.List;

public class PlaybackState {
    private static PlaybackState instance;
    private MediaItem currentItem;
    private boolean isPlaying = false;
    private long position = 0;

    public interface Listener {
        void onItemChanged(MediaItem item);
        void onPlayStateChanged(boolean playing);
        void onPositionChanged(long pos, long duration);
    }

    private final List<Listener> listeners = new ArrayList<>();
    private PlaybackState() {}
    public static PlaybackState get() { if (instance == null) instance = new PlaybackState(); return instance; }

    public void setCurrentItem(MediaItem item) {
        currentItem = item;
        for (Listener l : new ArrayList<>(listeners)) l.onItemChanged(item);
    }
    public MediaItem getCurrentItem() { return currentItem; }
    public void setPlaying(boolean p) {
        isPlaying = p;
        for (Listener l : new ArrayList<>(listeners)) l.onPlayStateChanged(p);
    }
    public boolean isPlaying() { return isPlaying; }
    public void setPosition(long p) { position = p; }
    public long getPosition() { return position; }
    public void notifyPosition(long pos, long dur) {
        for (Listener l : new ArrayList<>(listeners)) l.onPositionChanged(pos, dur);
    }
    public boolean hasMedia() { return currentItem != null; }
    public void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }
}
