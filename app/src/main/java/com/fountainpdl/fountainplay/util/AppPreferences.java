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
