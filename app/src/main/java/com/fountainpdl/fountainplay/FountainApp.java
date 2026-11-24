package com.fountainpdl.fountainplay;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.fountainpdl.fountainplay.util.AppPreferences;

public class FountainApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Light / dark / follow-system, chosen in Settings
        String theme = new AppPreferences(this).getTheme();
        switch (theme) {
            case "light":  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case "dark":
            case "amoled": AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default:       AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
    }
}
