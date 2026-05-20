#!/bin/bash
# ── v4 PART A: Icon, BaseActivity, Themes ──
# Run from ~/FountainPlay
set -e
P="app/src/main/java/com/fountainpdl/fountainplay"

echo "════════════════ v4 Part A ════════════════"

# ════════════════════════════════════════════════════════════
# 1. ICON — remove the XML vector, use only your PNG
# ════════════════════════════════════════════════════════════
echo "→ Fixing icon..."

# Remove the vector override so your PNG shows
rm -f app/src/main/res/drawable/ic_launcher_foreground.xml
rm -f app/src/main/res/mipmap-hdpi/ic_launcher_background.xml

# Adaptive icons: use a simple bitmap reference, not vector
mkdir -p app/src/main/res/mipmap-anydpi-v26

cat > app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/fp_purple_dark"/>
    <foreground android:drawable="@mipmap/ic_launcher_fg"/>
</adaptive-icon>
EOF

cat > app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/fp_purple_dark"/>
    <foreground android:drawable="@mipmap/ic_launcher_fg"/>
</adaptive-icon>
EOF

# Create ic_launcher_fg as an inset pointing to the PNG
# We put the PNG in drawable-xxhdpi as ic_launcher_fg.png
# You need to copy your icon PNG as:
#   app/src/main/res/drawable-mdpi/ic_launcher_fg.png    (48px)
#   app/src/main/res/drawable-hdpi/ic_launcher_fg.png    (72px)
#   app/src/main/res/drawable-xhdpi/ic_launcher_fg.png   (96px)
#   app/src/main/res/drawable-xxhdpi/ic_launcher_fg.png  (144px)
#   app/src/main/res/drawable-xxxhdpi/ic_launcher_fg.png (192px)
# The script tries to copy from existing mipmap PNGs

for d in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    mkdir -p app/src/main/res/drawable-$d
    SRC="app/src/main/res/mipmap-$d/ic_launcher.png"
    DST="app/src/main/res/drawable-$d/ic_launcher_fg.png"
    [ -f "$SRC" ] && cp "$SRC" "$DST" && echo "  ✅ drawable-$d/ic_launcher_fg.png"
done

echo "✅ Icon fixed — no more XML override"

# ════════════════════════════════════════════════════════════
# 2. COLORS — complete, no missing references
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/values/colors.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Brand defaults -->
    <color name="fp_purple">#7B2FBE</color>
    <color name="fp_purple_dark">#4A1080</color>
    <color name="fp_purple_light">#BB86FC</color>
    <color name="fp_red">#E53935</color>
    <color name="fp_red_dark">#B71C1C</color>
    <color name="fp_red_light">#EF5350</color>

    <!-- Dark surfaces -->
    <color name="background_dark">#121212</color>
    <color name="surface_dark">#1E1E2E</color>
    <color name="surface_variant_dark">#2A2A3C</color>
    <color name="on_surface_dark">#E6E1F0</color>
    <color name="on_surface_variant_dark">#9E9EBF</color>
    <color name="player_bg_dark">#0A0A14</color>
    <color name="mini_player_bg">#1E1E2E</color>

    <!-- AMOLED -->
    <color name="background_amoled">#000000</color>
    <color name="surface_amoled">#0D0D0D</color>

    <!-- Light surfaces -->
    <color name="background_light">#F5F0FF</color>
    <color name="surface_light">#FFFFFF</color>
    <color name="surface_variant_light">#EDE7F6</color>
    <color name="on_surface_light">#1C1B1F</color>
    <color name="on_surface_variant_light">#49454F</color>

    <!-- Shared -->
    <color name="white">#FFFFFF</color>
    <color name="black">#000000</color>
    <color name="transparent">#00000000</color>
    <color name="overlay_dark">#99000000</color>
    <color name="divider">#22FFFFFF</color>
</resources>
EOF

# ════════════════════════════════════════════════════════════
# 3. THEMES — Dark / Light / AMOLED, all correct
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/values/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <!-- ── Default (Dark) ── -->
    <style name="Theme.FountainPlay" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/fp_purple</item>
        <item name="colorPrimaryVariant">@color/fp_purple_dark</item>
        <item name="colorSecondary">@color/fp_red</item>
        <item name="colorSurface">@color/surface_dark</item>
        <item name="colorOnSurface">@color/on_surface_dark</item>
        <item name="android:colorBackground">@color/background_dark</item>
        <item name="android:windowBackground">@color/background_dark</item>
        <item name="android:statusBarColor">@color/background_dark</item>
        <item name="android:navigationBarColor">@color/surface_dark</item>
        <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>
    </style>

    <!-- ── Light ── -->
    <style name="Theme.FountainPlay.Light" parent="Theme.Material3.Light.NoActionBar">
        <item name="colorPrimary">@color/fp_purple</item>
        <item name="colorPrimaryVariant">@color/fp_purple_dark</item>
        <item name="colorSecondary">@color/fp_red</item>
        <item name="colorSurface">@color/surface_light</item>
        <item name="colorOnSurface">@color/on_surface_light</item>
        <item name="android:colorBackground">@color/background_light</item>
        <item name="android:windowBackground">@color/background_light</item>
        <item name="android:statusBarColor">@color/fp_purple_dark</item>
        <item name="android:navigationBarColor">@color/surface_light</item>
        <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>
    </style>

    <!-- ── AMOLED (true black) ── -->
    <style name="Theme.FountainPlay.AMOLED" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/fp_purple_light</item>
        <item name="colorPrimaryVariant">@color/fp_purple</item>
        <item name="colorSecondary">@color/fp_red_light</item>
        <item name="colorSurface">@color/surface_amoled</item>
        <item name="colorOnSurface">@color/white</item>
        <item name="android:colorBackground">@color/background_amoled</item>
        <item name="android:windowBackground">@color/background_amoled</item>
        <item name="android:statusBarColor">@color/background_amoled</item>
        <item name="android:navigationBarColor">@color/background_amoled</item>
        <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>
    </style>

    <!-- ── Player (always dark, translucent status) ── -->
    <style name="Theme.FountainPlay.Player" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/fp_purple</item>
        <item name="android:windowBackground">@color/player_bg_dark</item>
        <item name="android:statusBarColor">@color/transparent</item>
        <item name="android:windowTranslucentStatus">true</item>
        <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>
    </style>

</resources>
EOF

# Remove values-night (causes conflicts — night mode handled by AppCompatDelegate)
rm -f app/src/main/res/values-night/themes.xml
mkdir -p app/src/main/res/values-night
cat > app/src/main/res/values-night/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Night mode inherits from the dark theme set by AppCompatDelegate -->
    <style name="Theme.FountainPlay" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/fp_purple_light</item>
        <item name="colorSurface">@color/surface_dark</item>
        <item name="android:colorBackground">@color/background_dark</item>
        <item name="android:windowBackground">@color/background_dark</item>
        <item name="android:statusBarColor">@color/background_dark</item>
        <item name="android:navigationBarColor">@color/surface_dark</item>
    </style>
</resources>
EOF

# ════════════════════════════════════════════════════════════
# 4. FountainApp — persist theme correctly
# ════════════════════════════════════════════════════════════
cat > $P/FountainApp.java << 'EOF'
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
EOF

# ════════════════════════════════════════════════════════════
# 5. BaseActivity — applies the right theme before setContentView
# ════════════════════════════════════════════════════════════
cat > $P/BaseActivity.java << 'EOF'
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
EOF

# ════════════════════════════════════════════════════════════
# 6. NAV COLOR selector
# ════════════════════════════════════════════════════════════
mkdir -p app/src/main/res/color
cat > app/src/main/res/color/nav_item_color.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/fp_purple_light" android:state_checked="true" />
    <item android:color="@color/on_surface_variant_dark" />
</selector>
EOF

# ════════════════════════════════════════════════════════════
# 7. bg_play_button drawable (needed by multiple layouts)
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/drawable/bg_play_button.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/fp_purple" />
</shape>
EOF

cat > app/src/main/res/drawable/bg_mini_play.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#44BB86FC" />
</shape>
EOF

cat > app/src/main/res/drawable/gradient_player_overlay.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient
        android:startColor="#CC0A0A14"
        android:endColor="#FF0A0A14"
        android:angle="270"
        android:type="linear" />
</shape>
EOF

echo ""
echo "✅ PART A DONE — run fp_v4B.sh next"
