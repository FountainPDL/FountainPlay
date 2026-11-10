package com.fountainpdl.fountainplay;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.ThemeApplier;

/**
 * All activities extend this so theme + color are applied consistently.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private final FragmentManager.FragmentLifecycleCallbacks colorCallbacks =
        new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewCreated(@NonNull FragmentManager fm,
                                              @NonNull Fragment f,
                                              @NonNull View v,
                                              @Nullable Bundle savedInstanceState) {
                ThemeApplier.apply(BaseActivity.this, v);
            }
        };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyAppTheme();
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(colorCallbacks, true);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        ThemeApplier.apply(this, getWindow().getDecorView());
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
