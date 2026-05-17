package com.fountainpdl.fountainplay.util;

import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.*;

public class PlayQueue {
    public static final int REPEAT_NONE = 0, REPEAT_ONE = 1, REPEAT_ALL = 2;

    private static PlayQueue instance;
    private List<MediaItem> original = new ArrayList<>();
    private List<MediaItem> queue    = new ArrayList<>();
    private int index        = 0;
    private int repeatMode   = REPEAT_NONE;
    private boolean shuffle  = false;

    public interface Listener {
        void onQueueChanged();
        void onTrackChanged(MediaItem item, int index);
    }
    private final List<Listener> listeners = new ArrayList<>();

    private PlayQueue() {}
    public static PlayQueue get() { if (instance == null) instance = new PlayQueue(); return instance; }

    public void setQueue(List<MediaItem> items, int startIndex) {
        original = new ArrayList<>(items);
        queue    = new ArrayList<>(items);
        index    = startIndex;
        if (shuffle) applyShuffleKeepCurrent();
        notifyQueueChanged();
        notifyTrackChanged();
    }

    public void addToQueue(MediaItem item) {
        queue.add(item);
        original.add(item);
        notifyQueueChanged();
    }

    public void addNext(MediaItem item) {
        int insertAt = index + 1;
        if (insertAt >= queue.size()) queue.add(item);
        else queue.add(insertAt, item);
        notifyQueueChanged();
    }

    public MediaItem current() { return queue.isEmpty() ? null : queue.get(Math.max(0, Math.min(index, queue.size()-1))); }

    public MediaItem next() {
        if (queue.isEmpty()) return null;
        if (repeatMode == REPEAT_ONE) return current();
        if (index < queue.size() - 1) { index++; notifyTrackChanged(); return current(); }
        if (repeatMode == REPEAT_ALL) { index = 0; notifyTrackChanged(); return current(); }
        return null; // end of queue
    }

    public MediaItem previous() {
        if (queue.isEmpty()) return null;
        if (index > 0) { index--; notifyTrackChanged(); return current(); }
        if (repeatMode == REPEAT_ALL) { index = queue.size()-1; notifyTrackChanged(); return current(); }
        return current();
    }

    public boolean hasNext() {
        return !queue.isEmpty() && (index < queue.size()-1 || repeatMode != REPEAT_NONE);
    }

    public boolean hasPrevious() { return index > 0; }

    public void setShuffle(boolean on) {
        shuffle = on;
        if (on) applyShuffleKeepCurrent();
        else { MediaItem cur = current(); queue = new ArrayList<>(original); index = queue.indexOf(cur); }
        notifyQueueChanged();
    }

    private void applyShuffleKeepCurrent() {
        MediaItem cur = current();
        Collections.shuffle(queue);
        int idx = queue.indexOf(cur);
        if (idx >= 0) { queue.remove(idx); queue.add(0, cur); index = 0; }
    }

    public void cycleRepeat() {
        repeatMode = (repeatMode + 1) % 3;
    }

    public boolean isShuffle() { return shuffle; }
    public int getRepeatMode() { return repeatMode; }
    public int getIndex() { return index; }
    public List<MediaItem> getQueue() { return queue; }
    public int size() { return queue.size(); }

    public void moveItem(int from, int to) {
        if (from < 0 || to < 0 || from >= queue.size() || to >= queue.size()) return;
        MediaItem item = queue.remove(from);
        queue.add(to, item);
        if (index == from) index = to;
        else if (from < index && to >= index) index--;
        else if (from > index && to <= index) index++;
        notifyQueueChanged();
    }

    public void removeAt(int pos) {
        if (pos < 0 || pos >= queue.size()) return;
        queue.remove(pos);
        if (pos < index) index--;
        else if (pos == index && index >= queue.size()) index = Math.max(0, queue.size()-1);
        notifyQueueChanged();
    }

    public void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }
    private void notifyQueueChanged() { for (Listener l : new ArrayList<>(listeners)) l.onQueueChanged(); }
    private void notifyTrackChanged() { MediaItem cur = current(); for (Listener l : new ArrayList<>(listeners)) l.onTrackChanged(cur, index); }
}
