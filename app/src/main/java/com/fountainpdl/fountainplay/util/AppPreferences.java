package com.fountainpdl.fountainplay.util;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class AppPreferences {
    public static final String PREF = "fp_prefs";

    public static final String KEY_THEME             = "theme";            // system | light | dark | amoled
    public static final String KEY_ACCENT            = "accent_preset";    // fountain | ocean | sunset | forest | rose | graphite
    public static final String KEY_DYNAMIC_COLOR    = "dynamic_color";
    public static final String KEY_PLAYBACK_SPEED    = "playback_speed";
    public static final String KEY_SKIP_INTERVAL     = "skip_interval";
    public static final String KEY_GESTURES          = "gestures_enabled";
    public static final String KEY_REMEMBER_POS      = "remember_position";
    public static final String KEY_AUTO_PIP          = "auto_pip";
    public static final String KEY_BACKGROUND        = "background_playback";
    public static final String KEY_SHOW_NOTIFICATION = "show_notification";
    public static final String KEY_EXCLUDED_FOLDERS  = "excluded_folders";
    public static final String KEY_EXTRA_FOLDERS     = "extra_folders";
    public static final String KEY_SHOW_HIDDEN       = "show_hidden_files";

    private final SharedPreferences p;

    public AppPreferences(Context ctx) {
        p = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public String  getTheme()               { return p.getString(KEY_THEME, "system"); }
    public void    setTheme(String t)       { p.edit().putString(KEY_THEME, t).apply(); }
    public String  getAccent()              { return p.getString(KEY_ACCENT, "fountain"); }
    public void    setAccent(String a)      { p.edit().putString(KEY_ACCENT, a).apply(); }
    public boolean getDynamicColor()        { return p.getBoolean(KEY_DYNAMIC_COLOR, false); }

    public float   getPlaybackSpeed()       { return p.getFloat(KEY_PLAYBACK_SPEED, 1.0f); }
    public void    setPlaybackSpeed(float s){ p.edit().putFloat(KEY_PLAYBACK_SPEED, s).apply(); }
    public int     getSkipInterval()        { return p.getInt(KEY_SKIP_INTERVAL, 10); }
    public void    setSkipInterval(int s)   { p.edit().putInt(KEY_SKIP_INTERVAL, s).apply(); }
    public boolean getGesturesEnabled()     { return p.getBoolean(KEY_GESTURES, true); }
    public boolean getRememberPosition()    { return p.getBoolean(KEY_REMEMBER_POS, true); }

    public Set<String> getExcludedFolders() { return new HashSet<>(p.getStringSet(KEY_EXCLUDED_FOLDERS, new HashSet<>())); }
    public void setExcludedFolders(Set<String> s) { p.edit().putStringSet(KEY_EXCLUDED_FOLDERS, new HashSet<>(s)).apply(); }
    public Set<String> getExtraFolders()    { return new HashSet<>(p.getStringSet(KEY_EXTRA_FOLDERS, new HashSet<>())); }
    public void setExtraFolders(Set<String> s) { p.edit().putStringSet(KEY_EXTRA_FOLDERS, new HashSet<>(s)).apply(); }

    public boolean getBoolean(String k, boolean def) { return p.getBoolean(k, def); }
    public void    setBoolean(String k, boolean v)   { p.edit().putBoolean(k, v).apply(); }
    public String  getString(String k, String def)   { return p.getString(k, def); }
    public int     getInt(String k, int def)         { return p.getInt(k, def); }
    public void    setInt(String k, int v)           { p.edit().putInt(k, v).apply(); }

    /**
     * Earlier builds of the Settings screen could save every switch as "off" when the screen was restored.
     * Put the switches back to their defaults, once.
     */
    public void resetTogglesOnce() {
        if (p.getBoolean("toggle_reset_v1", false)) return;
        p.edit().remove(KEY_BACKGROUND).remove(KEY_SHOW_NOTIFICATION).remove(KEY_REMEMBER_POS)
            .remove(KEY_GESTURES).remove(KEY_AUTO_PIP).remove(KEY_DYNAMIC_COLOR)
            .remove("asked_post_notifications")
            .putBoolean("toggle_reset_v1", true).apply();
    }
}
