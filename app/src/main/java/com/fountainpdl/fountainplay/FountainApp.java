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
        applyTheme(new AppPreferences(this).getTheme());
        createNotificationChannel();
    }

    public static void applyTheme(String theme) {
        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case "amoled":
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
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
