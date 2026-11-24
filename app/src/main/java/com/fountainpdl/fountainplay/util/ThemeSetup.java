package com.fountainpdl.fountainplay.util;

import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatActivity;
import com.fountainpdl.fountainplay.R;
import com.google.android.material.color.DynamicColors;

/** Applies the user's accent palette, Material You colours and AMOLED surfaces to an activity's theme. */
public final class ThemeSetup {
    private ThemeSetup() {}

    /** Call after super.onCreate() and before setContentView(). */
    public static void apply(AppCompatActivity a, boolean allowDynamic) {
        AppPreferences p = new AppPreferences(a);
        if (allowDynamic && p.getDynamicColor() && DynamicColors.isDynamicColorAvailable()) {
            DynamicColors.applyToActivityIfAvailable(a);
        } else {
            int overlay = accentOverlay(p.getAccent());
            if (overlay != 0) a.getTheme().applyStyle(overlay, true);
        }
        boolean night = (a.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES;
        if (allowDynamic && night && "amoled".equals(p.getTheme())) {
            a.getTheme().applyStyle(R.style.ThemeOverlay_FP_Amoled, true);
        }
    }

    private static int accentOverlay(String accent) {
        if (accent == null) return 0;
        switch (accent) {
            case "ocean":    return R.style.ThemeOverlay_FP_Accent_Ocean;
            case "sunset":   return R.style.ThemeOverlay_FP_Accent_Sunset;
            case "forest":   return R.style.ThemeOverlay_FP_Accent_Forest;
            case "rose":     return R.style.ThemeOverlay_FP_Accent_Rose;
            case "graphite": return R.style.ThemeOverlay_FP_Accent_Graphite;
            default:         return R.style.ThemeOverlay_FP_Accent_Fountain;
        }
    }
}
