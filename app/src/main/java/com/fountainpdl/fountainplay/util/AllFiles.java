package com.fountainpdl.fountainplay.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

/** "All files access": lets the library find media that Android never indexed (hidden, unscanned, other apps' folders). */
public final class AllFiles {
    private AllFiles() {}

    public static boolean granted() {
        return Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager();
    }

    public static boolean supported() { return Build.VERSION.SDK_INT >= 30; }

    /** Opens the system screen where the user turns the permission on. */
    public static Intent settingsIntent(Context ctx) {
        Uri pkg = Uri.parse("package:" + ctx.getPackageName());
        if (Build.VERSION.SDK_INT >= 30) return new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, pkg);
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg);
    }
}
