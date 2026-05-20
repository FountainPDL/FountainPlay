package com.fountainpdl.fountainplay;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.fountainpdl.fountainplay.util.AppPreferences;

/**
 * All activities extend this so theme + color are applied consistently.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyAppTheme();
        super.onCreate(savedInstanceState);
    }

    protected void applyAppTheme() {
        AppPreferences prefs = new AppPreferences(this);
        String theme = prefs.getTheme();
        // AMOLED needs a different style resource (truly black surfaces)
        if ("amoled".equals(theme)) {
            setTheme(R.style.Theme_FountainPlay_AMOLED);
        } else if ("light".equals(theme)) {
            setTheme(R.style.Theme_FountainPlay_Light);
        }
        // "dark" and "system" use the default theme + AppCompatDelegate night mode
    }
}
