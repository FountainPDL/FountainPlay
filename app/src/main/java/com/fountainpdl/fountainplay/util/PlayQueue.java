package com.fountainpdl.fountainplay.util;

import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private PlayQueue() {}
    public static synchronized PlayQueue get() { if (instance == null) instance = new PlayQueue(); return instance; }

    public void setQueue(List<MediaItem> items, int startIndex) {
        original = new ArrayList<>(items);
        queue    = new ArrayList<>(items);
        index    = queue.isEmpty() ? 0 : Math.max(0, Math.min(startIndex, queue.size() - 1));
        if (shuffle) applyShuffleKeepCurrent();
        notifyQueueChanged();
        notifyTrackChanged();
    }

    public void clear() {
        original = new ArrayList<>();
        queue    = new ArrayList<>();
        index    = 0;
        notifyQueueChanged();
    }

    public void addToQueue(MediaItem item) {
        queue.add(item);
        original.add(item);
        notifyQueueChanged();
    }

    /** Plays right after the current track (also remembered when shuffle is switched off). */
    public void addNext(MediaItem item) {
        MediaItem cur = current();
        int insertAt = index + 1;
        if (insertAt >= queue.size()) queue.add(item);
        else queue.add(insertAt, item);
        int oi = cur == null ? -1 : original.indexOf(cur);
        if (oi < 0 || oi + 1 >= original.size()) original.add(item);
        else original.add(oi + 1, item);
        notifyQueueChanged();
    }

    public MediaItem current() {
        return queue.isEmpty() ? null : queue.get(Math.max(0, Math.min(index, queue.size() - 1)));
    }

    /** Called when a track finishes by itself: honours repeat-one. */
    public MediaItem advance() {
        if (queue.isEmpty()) return null;
        if (repeatMode == REPEAT_ONE) { notifyTrackChanged(); return current(); }
        return next();
    }

    /** Called when the user presses Next. */
    public MediaItem next() {
        if (queue.isEmpty()) return null;
        if (index < queue.size() - 1) { index++; notifyTrackChanged(); return current(); }
        if (repeatMode == REPEAT_ALL) { index = 0; notifyTrackChanged(); return current(); }
        return null; // end of queue
    }

    public MediaItem previous() {
        if (queue.isEmpty()) return null;
        if (index > 0) { index--; notifyTrackChanged(); return current(); }
        if (repeatMode == REPEAT_ALL) { index = queue.size() - 1; notifyTrackChanged(); return current(); }
        return current();
    }

    /** Jump straight to a position in the queue (tap in Up Next). */
    public void jumpTo(int pos) {
        if (pos < 0 || pos >= queue.size()) return;
        index = pos;
        notifyQueueChanged();
        notifyTrackChanged();
    }

    public boolean hasNext() {
        return !queue.isEmpty() && (index < queue.size() - 1 || repeatMode == REPEAT_ALL);
    }

    public boolean hasPrevious() { return index > 0 || (repeatMode == REPEAT_ALL && queue.size() > 1); }

    public void setShuffle(boolean on) {
        shuffle = on;
        if (on) applyShuffleKeepCurrent();
        else {
            MediaItem cur = current();
            queue = new ArrayList<>(original);
            index = Math.max(0, queue.indexOf(cur));
        }
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
        notifyQueueChanged();
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
        MediaItem removed = queue.remove(pos);
        original.remove(removed);
        if (pos < index) index--;
        else if (pos == index && index >= queue.size()) index = Math.max(0, queue.size() - 1);
        notifyQueueChanged();
    }

    public void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }
    private void notifyQueueChanged() { for (Listener l : listeners) l.onQueueChanged(); }
    private void notifyTrackChanged() { MediaItem cur = current(); for (Listener l : listeners) l.onTrackChanged(cur, index); }
}
