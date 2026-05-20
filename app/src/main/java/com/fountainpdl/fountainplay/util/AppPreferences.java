package com.fountainpdl.fountainplay.util;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class AppPreferences {
    private static final String PREF = "fp_prefs";

    public static final String KEY_THEME           = "theme";
    public static final String KEY_PRIMARY_COLOR   = "primary_color";
    public static final String KEY_ACCENT_COLOR    = "accent_color";
    public static final String KEY_AUDIO_FOLDERS   = "audio_folders";
    public static final String KEY_VIDEO_FOLDERS   = "video_folders";
    public static final String KEY_PLAYBACK_SPEED  = "playback_speed";
    public static final String KEY_SKIP_INTERVAL   = "skip_interval";
    public static final String KEY_RESUME_PLAYBACK = "resume_playback";
    public static final String KEY_GESTURES        = "gestures_enabled";
    public static final String KEY_HW_ACCEL        = "hw_acceleration";
    public static final String KEY_REMEMBER_POS    = "remember_position";
    public static final String KEY_LAST_URI        = "last_uri";
    public static final String KEY_LAST_POS        = "last_position";
    public static final String KEY_RECENTLY_PLAYED = "recently_played";

    private final SharedPreferences p;

    public AppPreferences(Context ctx) {
        p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public String  getTheme()              { return p.getString(KEY_THEME, "dark"); }
    public void    setTheme(String t)      { p.edit().putString(KEY_THEME, t).apply(); }

    // Colors stored as int (ARGB)
    public int     getPrimaryColor()       { return p.getInt(KEY_PRIMARY_COLOR, 0xFF7B2FBE); }
    public void    setPrimaryColor(int c)  { p.edit().putInt(KEY_PRIMARY_COLOR, c).apply(); }
    public int     getAccentColor()        { return p.getInt(KEY_ACCENT_COLOR, 0xFFE53935); }
    public void    setAccentColor(int c)   { p.edit().putInt(KEY_ACCENT_COLOR, c).apply(); }

    public Set<String> getAudioFolders()           { return p.getStringSet(KEY_AUDIO_FOLDERS, new HashSet<>()); }
    public void        setAudioFolders(Set<String> f) { p.edit().putStringSet(KEY_AUDIO_FOLDERS, f).apply(); }
    public Set<String> getVideoFolders()           { return p.getStringSet(KEY_VIDEO_FOLDERS, new HashSet<>()); }
    public void        setVideoFolders(Set<String> f) { p.edit().putStringSet(KEY_VIDEO_FOLDERS, f).apply(); }

    public float   getPlaybackSpeed()      { return p.getFloat(KEY_PLAYBACK_SPEED, 1.0f); }
    public void    setPlaybackSpeed(float s){ p.edit().putFloat(KEY_PLAYBACK_SPEED, s).apply(); }

    public int     getSkipInterval()       { return p.getInt(KEY_SKIP_INTERVAL, 10); }
    public void    setSkipInterval(int s)  { p.edit().putInt(KEY_SKIP_INTERVAL, s).apply(); }

    public boolean getResumePlayback()     { return p.getBoolean(KEY_RESUME_PLAYBACK, true); }
    public boolean getGesturesEnabled()    { return p.getBoolean(KEY_GESTURES, true); }
    public boolean getHwAcceleration()     { return p.getBoolean(KEY_HW_ACCEL, true); }
    public boolean getRememberPosition()   { return p.getBoolean(KEY_REMEMBER_POS, true); }

    public void    setLastUri(String u)    { p.edit().putString(KEY_LAST_URI, u).apply(); }
    public String  getLastUri()            { return p.getString(KEY_LAST_URI, null); }
    public void    setLastPos(long pos)    { p.edit().putLong(KEY_LAST_POS, pos).apply(); }
    public long    getLastPos()            { return p.getLong(KEY_LAST_POS, 0); }

    public boolean getBoolean(String k, boolean def) { return p.getBoolean(k, def); }
    public void    setBoolean(String k, boolean v)   { p.edit().putBoolean(k, v).apply(); }
}
