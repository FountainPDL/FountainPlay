package com.fountainpdl.fountainplay.util;

import android.content.Context;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

/** Reads colours from the active theme by attribute name, so code never contains colour values. */
public final class ThemeColors {
    private ThemeColors() {}

    /** @param attrName e.g. "colorPrimary", "colorOnSurface", "colorSurfaceContainerHigh" */
    public static int get(Context ctx, String attrName) {
        int id = ctx.getResources().getIdentifier(attrName, "attr", ctx.getPackageName());
        if (id == 0) return 0;
        TypedValue tv = new TypedValue();
        if (!ctx.getTheme().resolveAttribute(id, tv, true)) return 0;
        if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) return tv.data;
        if (tv.resourceId != 0) return ContextCompat.getColor(ctx, tv.resourceId);
        return tv.data;
    }

    public static int blend(int from, int to, float ratio) { return ColorUtils.blendARGB(from, to, ratio); }
    public static boolean isLight(int color) { return ColorUtils.calculateLuminance(color) > 0.5; }
}
