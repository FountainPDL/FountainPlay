package com.fountainpdl.fountainplay.theme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

public final class ThemeManager {
    public static final int DEFAULT_PRIMARY = Color.parseColor("#9333EA");
    public static final int DEFAULT_ACCENT = Color.parseColor("#EF4444");
    public static final int MODE_SYSTEM = 0, MODE_LIGHT = 1, MODE_DARK = 2, MODE_AMOLED = 3;

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("fp_theme", Context.MODE_PRIVATE);
    }

    public static int getPrimary(Context c) {
        return prefs(c).getInt("primary", DEFAULT_PRIMARY);
    }

    public static int getAccent(Context c) {
        return prefs(c).getInt("accent", DEFAULT_ACCENT);
    }

    public static int getMode(Context c) {
        return prefs(c).getInt("mode", MODE_SYSTEM);
    }

    public static void save(Context c, int primary, int accent, int mode) {
        prefs(c).edit()
            .putInt("primary", primary)
            .putInt("accent", accent)
            .putInt("mode", mode)
            .apply();
    }

    public static void reset(Context c) {
        prefs(c).edit().clear().apply();
    }

    /** Call from BaseActivity.onCreate BEFORE setContentView. */
    public static void apply(Activity a) {
        int mode = getMode(a);
        AppCompatDelegate.setDefaultNightMode(
            mode == MODE_LIGHT ? AppCompatDelegate.MODE_NIGHT_NO
            : mode == MODE_SYSTEM ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            : AppCompatDelegate.MODE_NIGHT_YES);
        DynamicColors.applyToActivityIfAvailable(a,
            new DynamicColorsOptions.Builder()
                .setContentBasedSource(getPrimary(a))
                .build());
    }
}
