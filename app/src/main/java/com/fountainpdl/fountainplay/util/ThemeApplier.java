package com.fountainpdl.fountainplay.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

/**
 * Applies the user's chosen primary/accent colors to common widgets.
 * Does nothing while the user is still on the default colors.
 */
public final class ThemeApplier {
    private static final int DEF_PRIMARY = 0xFF7B2FBE;
    private static final int DEF_ACCENT  = 0xFFE53935;

    private ThemeApplier() {}

    public static void apply(Context ctx, View root) {
        if (root == null) return;
        AppPreferences p = new AppPreferences(ctx);
        int primary = p.getPrimaryColor();
        int accent  = p.getAccentColor();
        boolean cp = primary != DEF_PRIMARY;
        boolean ca = accent != DEF_ACCENT;
        if (!cp && !ca) return;
        walk(root, primary, accent, cp, ca);
    }

    private static void walk(View v, int primary, int accent, boolean cp, boolean ca) {
        if (cp) tintPrimary(v, primary);
        if (ca) tintAccent(v, accent);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                walk(g.getChildAt(i), primary, accent, cp, ca);
            }
        }
    }

    private static ColorStateList onOff(int on, int off) {
        return new ColorStateList(
            new int[][] {
                { android.R.attr.state_checked },
                { android.R.attr.state_selected },
                {}
            },
            new int[] { on, on, off });
    }

    private static int defaultOf(ColorStateList l, int fallback) {
        return l != null ? l.getDefaultColor() : fallback;
    }

    private static void tintPrimary(View v, int primary) {
        if (v instanceof BottomNavigationView) {
            BottomNavigationView b = (BottomNavigationView) v;
            int off = defaultOf(b.getItemIconTintList(), 0x99FFFFFF);
            b.setItemIconTintList(onOff(primary, off));
            b.setItemTextColor(onOff(primary, off));
        } else if (v instanceof TabLayout) {
            TabLayout t = (TabLayout) v;
            int off = defaultOf(t.getTabTextColors(), 0x99FFFFFF);
            t.setSelectedTabIndicatorColor(primary);
            t.setTabTextColors(off, primary);
        } else if (v instanceof Chip) {
            Chip c = (Chip) v;
            if (c.isCheckable()) {
                int off = defaultOf(c.getChipBackgroundColor(), 0x33FFFFFF);
                c.setChipBackgroundColor(onOff(primary, off));
                c.setTextColor(onOff(0xFFFFFFFF, defaultOf(c.getTextColors(), 0xFFFFFFFF)));
            }
        } else if (v instanceof SwitchCompat) {
            SwitchCompat s = (SwitchCompat) v;
            s.setThumbTintList(onOff(primary, 0xFFB0B0C0));
            s.setTrackTintList(onOff((primary & 0x00FFFFFF) | 0x88000000, 0x55888899));
        }
    }

    private static void tintAccent(View v, int accent) {
        if (v instanceof ProgressBar) {
            ProgressBar p = (ProgressBar) v;
            ColorStateList c = ColorStateList.valueOf(accent);
            if (p.isIndeterminate()) {
                p.setIndeterminateTintList(c);
            } else {
                p.setProgressTintList(c);
            }
            if (p instanceof SeekBar) {
                ((SeekBar) p).setThumbTintList(c);
            }
        } else if (v instanceof FloatingActionButton) {
            ((FloatingActionButton) v).setBackgroundTintList(ColorStateList.valueOf(accent));
        }
    }
}
