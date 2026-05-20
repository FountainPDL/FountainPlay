package com.fountainpdl.fountainplay;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;
import com.fountainpdl.fountainplay.util.AppPreferences;

public class FountainApp extends Application {
    public static final String CHANNEL_ID = "fp_playback";

    @Override
    public void onCreate() {
        super.onCreate();
        // Apply saved night/day mode on startup
        applyNightMode(new AppPreferences(this).getTheme());
        createNotificationChannel();
    }

    /**
     * Sets AppCompat night mode. AMOLED is handled per-activity via setTheme().
     */
    public static void applyNightMode(String theme) {
        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
            case "amoled":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default: // "system"
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Fountain Play", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Media playback controls");
            ch.setShowBadge(false);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }
}
