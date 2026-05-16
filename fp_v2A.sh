#!/bin/bash
# ── v2 PART A: Infrastructure ──
# Run from inside ~/FountainPlay

P="app/src/main/java/com/fountainpdl/fountainplay"

# ════════════════════════════════════════════════════════════
# 1. FULL SCREEN THEME + suppress compileSdk warning
# ════════════════════════════════════════════════════════════
cat >> gradle.properties << 'EOF'
android.suppressUnsupportedCompileSdk=34
EOF

cat > app/src/main/res/values/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FountainPlay" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/fp_purple</item>
        <item name="colorPrimaryDark">@color/fp_purple_dark</item>
        <item name="colorAccent">@color/fp_red</item>
        <item name="colorSurface">@color/surface_dark</item>
        <item name="colorOnSurface">@color/on_surface_dark</item>
        <item name="android:windowBackground">@color/background_dark</item>
        <item name="android:statusBarColor">@color/background_dark</item>
        <item name="android:navigationBarColor">@color/surface_dark</item>
        <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>
    </style>
    <style name="Theme.FountainPlay.Player" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/fp_purple</item>
        <item name="android:windowBackground">@color/player_bg_dark</item>
        <item name="android:statusBarColor">@color/transparent</item>
        <item name="android:windowTranslucentStatus">true</item>
        <item name="android:windowFullscreen">false</item>
        <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>
    </style>
</resources>
EOF

cat > app/src/main/res/values-night/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FountainPlay" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/fp_purple_light</item>
        <item name="colorPrimaryDark">@color/fp_purple</item>
        <item name="colorAccent">@color/fp_red_light</item>
        <item name="colorSurface">@color/surface_dark</item>
        <item name="colorOnSurface">@color/on_surface_dark</item>
        <item name="android:windowBackground">@color/background_dark</item>
        <item name="android:statusBarColor">@color/background_dark</item>
        <item name="android:navigationBarColor">@color/surface_dark</item>
    </style>
</resources>
EOF
echo "✅ Themes updated (dark + full screen)"

# ════════════════════════════════════════════════════════════
# 2. FIX MediaScanner — filter out non-media / 0-byte files
# ════════════════════════════════════════════════════════════
cat > $P/util/MediaScanner.java << 'EOF'
package com.fountainpdl.fountainplay.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MediaScanner {

    private static final long MIN_AUDIO_DURATION = 10_000; // 10 seconds
    private static final long MIN_VIDEO_SIZE = 100_000;    // 100 KB
    private static final long MIN_VIDEO_DURATION = 1_000;  // 1 second

    public static List<MediaItem> scanAudio(Context context) {
        return scanAudio(context, null);
    }

    public static List<MediaItem> scanAudio(Context context, Set<String> allowedFolders) {
        List<MediaItem> items = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] proj = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        };
        // Only real audio files, not recordings under 10s
        String selection = MediaStore.Audio.Media.IS_MUSIC + "=1 AND "
            + MediaStore.Audio.Media.DURATION + ">= " + MIN_AUDIO_DURATION;

        try (Cursor c = cr.query(uri, proj, selection, null,
                MediaStore.Audio.Media.DATE_ADDED + " DESC")) {
            if (c == null) return items;
            int iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int iPath = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int iSize = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            int iDate = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);

            while (c.moveToNext()) {
                String path = c.getString(iPath);
                if (path == null) continue;

                // Folder filter
                if (allowedFolders != null && !allowedFolders.isEmpty()) {
                    String folder = path.substring(0, path.lastIndexOf('/'));
                    if (!allowedFolders.contains(folder)) continue;
                }

                MediaItem item = new MediaItem(
                    c.getLong(iId), c.getString(iTitle),
                    c.getString(iArtist), path,
                    c.getLong(iDur), MediaItem.TYPE_AUDIO);
                item.setAlbum(c.getString(iAlbum));
                item.setSize(c.getLong(iSize));
                item.setDateAdded(c.getLong(iDate));
                item.setAlbumArtUri(Uri.withAppendedPath(
                    Uri.parse("content://media/external/audio/albumart"),
                    String.valueOf(c.getLong(iAlbumId))).toString());
                items.add(item);
            }
        }
        return items;
    }

    public static List<MediaItem> scanVideo(Context context) {
        return scanVideo(context, null);
    }

    public static List<MediaItem> scanVideo(Context context, Set<String> allowedFolders) {
        List<MediaItem> items = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] proj = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED
        };
        // Filter: must have real size and duration
        String selection = MediaStore.Video.Media.SIZE + " > " + MIN_VIDEO_SIZE
            + " AND " + MediaStore.Video.Media.DURATION + " > " + MIN_VIDEO_DURATION;

        try (Cursor c = cr.query(uri, proj, selection, null,
                MediaStore.Video.Media.DATE_ADDED + " DESC")) {
            if (c == null) return items;
            int iId = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int iTitle = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
            int iPath = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            int iDur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            int iSize = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            int iDate = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);

            while (c.moveToNext()) {
                String path = c.getString(iPath);
                if (path == null) continue;

                if (allowedFolders != null && !allowedFolders.isEmpty()) {
                    String folder = path.substring(0, path.lastIndexOf('/'));
                    if (!allowedFolders.contains(folder)) continue;
                }

                MediaItem item = new MediaItem(
                    c.getLong(iId), c.getString(iTitle),
                    "", path, c.getLong(iDur), MediaItem.TYPE_VIDEO);
                item.setSize(c.getLong(iSize));
                item.setDateAdded(c.getLong(iDate));
                items.add(item);
            }
        }
        return items;
    }

    /** Returns all unique folders containing audio files */
    public static List<String> getAudioFolders(Context context) {
        List<String> folders = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (MediaItem item : scanAudio(context)) {
            String folder = item.getPath().substring(0, item.getPath().lastIndexOf('/'));
            if (seen.add(folder)) folders.add(folder);
        }
        return folders;
    }

    /** Returns all unique folders containing video files */
    public static List<String> getVideoFolders(Context context) {
        List<String> folders = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (MediaItem item : scanVideo(context)) {
            String folder = item.getPath().substring(0, item.getPath().lastIndexOf('/'));
            if (seen.add(folder)) folders.add(folder);
        }
        return folders;
    }
}
EOF
echo "✅ MediaScanner fixed (filters 0-byte/non-media files)"

# ════════════════════════════════════════════════════════════
# 3. UPDATE MediaItem — add dateAdded field
# ════════════════════════════════════════════════════════════
cat > $P/model/MediaItem.java << 'EOF'
package com.fountainpdl.fountainplay.model;

public class MediaItem {
    public static final int TYPE_AUDIO = 0;
    public static final int TYPE_VIDEO = 1;

    private long id, duration, size, dateAdded;
    private String title, artist, album, path, albumArtUri;
    private int type;

    public MediaItem() {}
    public MediaItem(long id, String title, String artist, String path, long duration, int type) {
        this.id = id; this.title = title; this.artist = artist;
        this.path = path; this.duration = duration; this.type = type;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title != null ? title : "Unknown"; }
    public void setTitle(String t) { this.title = t; }
    public String getArtist() { return artist != null && !artist.equals("<unknown>") ? artist : "Unknown Artist"; }
    public void setArtist(String a) { this.artist = a; }
    public String getAlbum() { return album != null ? album : "Unknown Album"; }
    public void setAlbum(String a) { this.album = a; }
    public String getPath() { return path; }
    public void setPath(String p) { this.path = p; }
    public long getDuration() { return duration; }
    public void setDuration(long d) { this.duration = d; }
    public long getSize() { return size; }
    public void setSize(long s) { this.size = s; }
    public int getType() { return type; }
    public void setType(int t) { this.type = t; }
    public String getAlbumArtUri() { return albumArtUri; }
    public void setAlbumArtUri(String u) { this.albumArtUri = u; }
    public long getDateAdded() { return dateAdded; }
    public void setDateAdded(long d) { this.dateAdded = d; }
    public boolean isAudio() { return type == TYPE_AUDIO; }
    public boolean isVideo() { return type == TYPE_VIDEO; }

    public String getFormattedDuration() {
        long s = duration / 1000, m = s / 60, h = m / 60;
        s %= 60; m %= 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }
    public String getFolder() {
        if (path == null) return "";
        int idx = path.lastIndexOf('/');
        return idx > 0 ? path.substring(0, idx) : path;
    }
    public String getFileName() {
        if (path == null) return title;
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 4. AppPreferences — central SharedPreferences manager
# ════════════════════════════════════════════════════════════
cat > $P/util/AppPreferences.java << 'EOF'
package com.fountainpdl.fountainplay.util;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class AppPreferences {
    private static final String PREF_NAME = "fountain_play_prefs";

    // Keys
    public static final String KEY_THEME = "theme";               // dark/light/system
    public static final String KEY_PRIMARY_COLOR = "primary_color";
    public static final String KEY_ACCENT_COLOR = "accent_color";
    public static final String KEY_AUDIO_FOLDERS = "audio_folders";
    public static final String KEY_VIDEO_FOLDERS = "video_folders";
    public static final String KEY_RECENTLY_PLAYED = "recently_played";
    public static final String KEY_PLAYBACK_SPEED = "playback_speed";
    public static final String KEY_SKIP_INTERVAL = "skip_interval";
    public static final String KEY_RESUME_PLAYBACK = "resume_playback";
    public static final String KEY_GESTURES_ENABLED = "gestures_enabled";
    public static final String KEY_HW_ACCELERATION = "hw_acceleration";
    public static final String KEY_REMEMBER_POSITION = "remember_position";
    public static final String KEY_LAST_URI = "last_uri";
    public static final String KEY_LAST_POSITION = "last_position";
    public static final String KEY_EQ_ENABLED = "eq_enabled";
    public static final String KEY_EQ_PRESET = "eq_preset";

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getTheme() { return prefs.getString(KEY_THEME, "dark"); }
    public void setTheme(String t) { prefs.edit().putString(KEY_THEME, t).apply(); }

    public int getPrimaryColor() { return prefs.getInt(KEY_PRIMARY_COLOR, 0xFF7B2FBE); }
    public void setPrimaryColor(int c) { prefs.edit().putInt(KEY_PRIMARY_COLOR, c).apply(); }

    public int getAccentColor() { return prefs.getInt(KEY_ACCENT_COLOR, 0xFFE53935); }
    public void setAccentColor(int c) { prefs.edit().putInt(KEY_ACCENT_COLOR, c).apply(); }

    public Set<String> getAudioFolders() { return prefs.getStringSet(KEY_AUDIO_FOLDERS, new HashSet<>()); }
    public void setAudioFolders(Set<String> f) { prefs.edit().putStringSet(KEY_AUDIO_FOLDERS, f).apply(); }

    public Set<String> getVideoFolders() { return prefs.getStringSet(KEY_VIDEO_FOLDERS, new HashSet<>()); }
    public void setVideoFolders(Set<String> f) { prefs.edit().putStringSet(KEY_VIDEO_FOLDERS, f).apply(); }

    public float getPlaybackSpeed() { return prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f); }
    public void setPlaybackSpeed(float s) { prefs.edit().putFloat(KEY_PLAYBACK_SPEED, s).apply(); }

    public int getSkipInterval() { return prefs.getInt(KEY_SKIP_INTERVAL, 10); }
    public void setSkipInterval(int s) { prefs.edit().putInt(KEY_SKIP_INTERVAL, s).apply(); }

    public boolean getResumePlayback() { return prefs.getBoolean(KEY_RESUME_PLAYBACK, true); }
    public boolean getGesturesEnabled() { return prefs.getBoolean(KEY_GESTURES_ENABLED, true); }
    public boolean getHwAcceleration() { return prefs.getBoolean(KEY_HW_ACCELERATION, true); }
    public boolean getRememberPosition() { return prefs.getBoolean(KEY_REMEMBER_POSITION, true); }

    public void setLastUri(String uri) { prefs.edit().putString(KEY_LAST_URI, uri).apply(); }
    public String getLastUri() { return prefs.getString(KEY_LAST_URI, null); }
    public void setLastPosition(long pos) { prefs.edit().putLong(KEY_LAST_POSITION, pos).apply(); }
    public long getLastPosition() { return prefs.getLong(KEY_LAST_POSITION, 0); }

    public String getRecentlyPlayed() { return prefs.getString(KEY_RECENTLY_PLAYED, "[]"); }
    public void setRecentlyPlayed(String json) { prefs.edit().putString(KEY_RECENTLY_PLAYED, json).apply(); }

    public boolean getBoolean(String key, boolean def) { return prefs.getBoolean(key, def); }
    public void setBoolean(String key, boolean val) { prefs.edit().putBoolean(key, val).apply(); }
    public String getString(String key, String def) { return prefs.getString(key, def); }
    public void setString(String key, String val) { prefs.edit().putString(key, val).apply(); }
}
EOF
echo "✅ AppPreferences created"

# ════════════════════════════════════════════════════════════
# 5. Navigation update — add Settings
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/navigation/nav_graph.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_graph"
    app:startDestination="@id/nav_home">
    <fragment android:id="@+id/nav_home"
        android:name="com.fountainpdl.fountainplay.ui.home.HomeFragment"
        android:label="Home" />
    <fragment android:id="@+id/nav_music"
        android:name="com.fountainpdl.fountainplay.ui.music.MusicFragment"
        android:label="Music" />
    <fragment android:id="@+id/nav_video"
        android:name="com.fountainpdl.fountainplay.ui.video.VideoFragment"
        android:label="Video" />
    <fragment android:id="@+id/nav_library"
        android:name="com.fountainpdl.fountainplay.ui.library.LibraryFragment"
        android:label="Library" />
    <fragment android:id="@+id/nav_settings"
        android:name="com.fountainpdl.fountainplay.ui.settings.SettingsFragment"
        android:label="Settings" />
</navigation>
EOF

cat > app/src/main/res/menu/bottom_nav_menu.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@+id/nav_home" android:icon="@android:drawable/ic_menu_compass" android:title="Home" />
    <item android:id="@+id/nav_music" android:icon="@android:drawable/ic_media_play" android:title="Music" />
    <item android:id="@+id/nav_video" android:icon="@android:drawable/ic_menu_slideshow" android:title="Video" />
    <item android:id="@+id/nav_library" android:icon="@android:drawable/ic_menu_agenda" android:title="Library" />
    <item android:id="@+id/nav_settings" android:icon="@android:drawable/ic_menu_preferences" android:title="Settings" />
</menu>
EOF
echo "✅ Navigation updated (+ Settings tab)"

# ════════════════════════════════════════════════════════════
# 6. SETTINGS LAYOUT
# ════════════════════════════════════════════════════════════
mkdir -p app/src/main/res/xml
cat > app/src/main/res/layout/fragment_settings.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background_dark">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingBottom="100dp">

        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="Settings" android:textSize="24sp" android:textStyle="bold"
            android:textColor="@color/white" android:padding="16dp" />

        <!-- APPEARANCE -->
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="  APPEARANCE" android:textSize="11sp" android:textColor="@color/fp_purple_light"
            android:textStyle="bold" android:paddingTop="16dp" android:paddingBottom="4dp" />

        <LinearLayout android:id="@+id/pref_theme"
            android:layout_width="match_parent" android:layout_height="56dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp" android:background="?attr/selectableItemBackground"
            android:clickable="true" android:focusable="true">
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Theme" android:textColor="@color/white" android:textSize="15sp" />
                <TextView android:id="@+id/tv_theme_value" android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Dark" android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
            </LinearLayout>
        </LinearLayout>

        <LinearLayout android:id="@+id/pref_primary_color"
            android:layout_width="match_parent" android:layout_height="56dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp" android:background="?attr/selectableItemBackground"
            android:clickable="true" android:focusable="true">
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Primary Color" android:textColor="@color/white" android:textSize="15sp" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Purple (default)" android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
            </LinearLayout>
            <View android:id="@+id/color_preview_primary"
                android:layout_width="28dp" android:layout_height="28dp"
                android:background="@color/fp_purple" />
        </LinearLayout>

        <LinearLayout android:id="@+id/pref_accent_color"
            android:layout_width="match_parent" android:layout_height="56dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp" android:background="?attr/selectableItemBackground"
            android:clickable="true" android:focusable="true">
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Accent Color" android:textColor="@color/white" android:textSize="15sp" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Red (default)" android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
            </LinearLayout>
            <View android:id="@+id/color_preview_accent"
                android:layout_width="28dp" android:layout_height="28dp"
                android:background="@color/fp_red" />
        </LinearLayout>

        <!-- PLAYBACK -->
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="  PLAYBACK" android:textSize="11sp" android:textColor="@color/fp_purple_light"
            android:textStyle="bold" android:paddingTop="16dp" android:paddingBottom="4dp" />

        <LinearLayout android:id="@+id/pref_speed"
            android:layout_width="match_parent" android:layout_height="56dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp" android:background="?attr/selectableItemBackground"
            android:clickable="true" android:focusable="true">
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Default Speed" android:textColor="@color/white" android:textSize="15sp" />
                <TextView android:id="@+id/tv_speed_value" android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="1.0×" android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
            </LinearLayout>
        </LinearLayout>

        <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp">
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Resume Playback" android:textColor="@color/white" android:textSize="15sp" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Continue where you left off" android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
            </LinearLayout>
            <androidx.appcompat.widget.SwitchCompat android:id="@+id/sw_resume"
                android:layout_width="wrap_content" android:layout_height="wrap_content" />
        </LinearLayout>

        <LinearLayout android:id="@+id/pref_skip"
            android:layout_width="match_parent" android:layout_height="56dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp" android:background="?attr/selectableItemBackground"
            android:clickable="true" android:focusable="true">
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Skip Interval" android:textColor="@color/white" android:textSize="15sp" />
                <TextView android:id="@+id/tv_skip_value" android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="10 seconds" android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
            </LinearLayout>
        </LinearLayout>

        <!-- VIDEO -->
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="  VIDEO" android:textSize="11sp" android:textColor="@color/fp_purple_light"
            android:textStyle="bold" android:paddingTop="16dp" android:paddingBottom="4dp" />

        <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp">
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Hardware Acceleration" android:textColor="@color/white" android:textSize="15sp" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Use GPU for video decoding" android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
            </LinearLayout>
            <androidx.appcompat.widget.SwitchCompat android:id="@+id/sw_hw_accel"
                android:layout_width="wrap_content" android:layout_height="wrap_content" />
        </LinearLayout>

        <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp">
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Gesture Controls" android:textColor="@color/white" android:textSize="15sp" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Swipe for volume / brightness / seek" android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
            </LinearLayout>
            <androidx.appcompat.widget.SwitchCompat android:id="@+id/sw_gestures"
                android:layout_width="wrap_content" android:layout_height="wrap_content" />
        </LinearLayout>

        <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp">
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Remember Position" android:textColor="@color/white" android:textSize="15sp" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Resume video from last position" android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
            </LinearLayout>
            <androidx.appcompat.widget.SwitchCompat android:id="@+id/sw_remember_pos"
                android:layout_width="wrap_content" android:layout_height="wrap_content" />
        </LinearLayout>

        <!-- FOLDERS -->
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="  MEDIA FOLDERS" android:textSize="11sp" android:textColor="@color/fp_purple_light"
            android:textStyle="bold" android:paddingTop="16dp" android:paddingBottom="4dp" />

        <LinearLayout android:id="@+id/pref_audio_folders"
            android:layout_width="match_parent" android:layout_height="56dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp" android:background="?attr/selectableItemBackground"
            android:clickable="true" android:focusable="true">
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Music Folders" android:textColor="@color/white" android:textSize="15sp" />
                <TextView android:id="@+id/tv_audio_folders" android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="All folders" android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
            </LinearLayout>
        </LinearLayout>

        <LinearLayout android:id="@+id/pref_video_folders"
            android:layout_width="match_parent" android:layout_height="56dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp" android:background="?attr/selectableItemBackground"
            android:clickable="true" android:focusable="true">
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Video Folders" android:textColor="@color/white" android:textSize="15sp" />
                <TextView android:id="@+id/tv_video_folders" android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="All folders" android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
            </LinearLayout>
        </LinearLayout>

        <!-- ABOUT -->
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="  ABOUT" android:textSize="11sp" android:textColor="@color/fp_purple_light"
            android:textStyle="bold" android:paddingTop="16dp" android:paddingBottom="4dp" />

        <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp">
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Fountain Play" android:textColor="@color/white" android:textSize="15sp" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Version 1.0.0 — by FountainPDL" android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
            </LinearLayout>
        </LinearLayout>

    </LinearLayout>
</androidx.core.widget.NestedScrollView>
EOF
echo "✅ Settings layout created"

# ════════════════════════════════════════════════════════════
# 7. SettingsFragment.java
# ════════════════════════════════════════════════════════════
cat > $P/ui/settings/SettingsFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.MediaScanner;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class SettingsFragment extends Fragment {

    private AppPreferences prefs;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new AppPreferences(requireContext());

        // Theme
        TextView tvTheme = view.findViewById(R.id.tv_theme_value);
        tvTheme.setText(capitalize(prefs.getTheme()));
        view.findViewById(R.id.pref_theme).setOnClickListener(v -> {
            String[] opts = {"Dark", "Light", "System"};
            new AlertDialog.Builder(requireContext())
                .setTitle("Theme").setItems(opts, (d, i) -> {
                    String val = opts[i].toLowerCase();
                    prefs.setTheme(val);
                    tvTheme.setText(opts[i]);
                }).show();
        });

        // Speed
        TextView tvSpeed = view.findViewById(R.id.tv_speed_value);
        tvSpeed.setText(prefs.getPlaybackSpeed() + "×");
        view.findViewById(R.id.pref_speed).setOnClickListener(v -> {
            String[] opts = {"0.5×","0.75×","1.0×","1.25×","1.5×","1.75×","2.0×"};
            float[] vals = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
            new AlertDialog.Builder(requireContext())
                .setTitle("Default Speed").setItems(opts, (d, i) -> {
                    prefs.setPlaybackSpeed(vals[i]);
                    tvSpeed.setText(opts[i]);
                }).show();
        });

        // Skip interval
        TextView tvSkip = view.findViewById(R.id.tv_skip_value);
        tvSkip.setText(prefs.getSkipInterval() + " seconds");
        view.findViewById(R.id.pref_skip).setOnClickListener(v -> {
            String[] opts = {"5 seconds","10 seconds","15 seconds","30 seconds","60 seconds"};
            int[] vals = {5, 10, 15, 30, 60};
            new AlertDialog.Builder(requireContext())
                .setTitle("Skip Interval").setItems(opts, (d, i) -> {
                    prefs.setSkipInterval(vals[i]);
                    tvSkip.setText(opts[i]);
                }).show();
        });

        // Switches
        SwitchCompat swResume = view.findViewById(R.id.sw_resume);
        swResume.setChecked(prefs.getResumePlayback());
        swResume.setOnCheckedChangeListener((b, checked) ->
            prefs.setBoolean(AppPreferences.KEY_RESUME_PLAYBACK, checked));

        SwitchCompat swHw = view.findViewById(R.id.sw_hw_accel);
        swHw.setChecked(prefs.getHwAcceleration());
        swHw.setOnCheckedChangeListener((b, checked) ->
            prefs.setBoolean(AppPreferences.KEY_HW_ACCELERATION, checked));

        SwitchCompat swGestures = view.findViewById(R.id.sw_gestures);
        swGestures.setChecked(prefs.getGesturesEnabled());
        swGestures.setOnCheckedChangeListener((b, checked) ->
            prefs.setBoolean(AppPreferences.KEY_GESTURES_ENABLED, checked));

        SwitchCompat swPos = view.findViewById(R.id.sw_remember_pos);
        swPos.setChecked(prefs.getRememberPosition());
        swPos.setOnCheckedChangeListener((b, checked) ->
            prefs.setBoolean(AppPreferences.KEY_REMEMBER_POSITION, checked));

        // Folder selectors
        setupFolderPref(view, true);
        setupFolderPref(view, false);
    }

    private void setupFolderPref(View view, boolean isAudio) {
        int prefId = isAudio ? R.id.pref_audio_folders : R.id.pref_video_folders;
        int tvId = isAudio ? R.id.tv_audio_folders : R.id.tv_video_folders;
        TextView tv = view.findViewById(tvId);
        Set<String> current = isAudio ? prefs.getAudioFolders() : prefs.getVideoFolders();
        tv.setText(current.isEmpty() ? "All folders" : current.size() + " selected");

        view.findViewById(prefId).setOnClickListener(v -> {
            List<String> folders = isAudio
                ? MediaScanner.getAudioFolders(requireContext())
                : MediaScanner.getVideoFolders(requireContext());
            if (folders.isEmpty()) { tv.setText("No folders found"); return; }
            String[] items = folders.toArray(new String[0]);
            boolean[] checked = new boolean[items.length];
            Set<String> sel = new HashSet<>(isAudio ? prefs.getAudioFolders() : prefs.getVideoFolders());
            for (int i = 0; i < items.length; i++) checked[i] = sel.contains(items[i]);

            new AlertDialog.Builder(requireContext())
                .setTitle(isAudio ? "Music Folders" : "Video Folders")
                .setMultiChoiceItems(items, checked, (d, i, c) -> {
                    if (c) sel.add(items[i]); else sel.remove(items[i]);
                })
                .setPositiveButton("OK", (d, i) -> {
                    if (isAudio) prefs.setAudioFolders(sel); else prefs.setVideoFolders(sel);
                    tv.setText(sel.isEmpty() ? "All folders" : sel.size() + " selected");
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
EOF
echo "✅ SettingsFragment created"
echo ""
echo "✅ PART A DONE — run fp_v2B.sh next"
