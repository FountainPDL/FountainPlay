package com.fountainpdl.fountainplay.util;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Remembers where each video stopped. Feeds "Continue Watching". */
public final class ResumeStore {
    public static final class Entry {
        public String path, title;
        public long position, duration, savedAt;
    }

    private final SharedPreferences p;

    public ResumeStore(Context ctx) {
        p = ctx.getApplicationContext().getSharedPreferences("fp_resume", Context.MODE_PRIVATE);
    }

    public long getPosition(String path) { return path == null ? 0 : p.getLong("pos:" + path, 0); }

    public void put(String path, long position, long duration, String title) {
        if (path == null) return;
        p.edit().putLong("pos:" + path, position)
            .putLong("dur:" + path, duration)
            .putString("ttl:" + path, title == null ? "" : title)
            .putLong("ts:" + path, System.currentTimeMillis()).apply();
    }

    public void clear(String path) {
        if (path == null) return;
        p.edit().remove("pos:" + path).remove("dur:" + path)
            .remove("ttl:" + path).remove("ts:" + path).apply();
    }

    /** Newest first. */
    public List<Entry> getRecent(int limit) {
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
            String k = e.getKey();
            if (!k.startsWith("ts:") || !(e.getValue() instanceof Long)) continue;
            String path = k.substring(3);
            Entry r = new Entry();
            r.path = path;
            r.savedAt = (Long) e.getValue();
            r.position = p.getLong("pos:" + path, 0);
            r.duration = p.getLong("dur:" + path, 0);
            r.title = p.getString("ttl:" + path, "");
            out.add(r);
        }
        Collections.sort(out, (a, b) -> Long.compare(b.savedAt, a.savedAt));
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }
}
