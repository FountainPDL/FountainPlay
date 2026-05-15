# paste the script content, then Ctrl+X → Y → Enter to save

# 3. Run it
bash setup_fountainplay.sh

# 4. Push everything to GitHub
git add .
git commit -m "feat: initial project scaffold"
git push#!/bin/bash
# ============================================================
#  FOUNTAIN PLAY — Android Project Setup Script
#  Run this in Termux: bash setup_fountainplay.sh
# ============================================================

PROJECT="FountainPlay"
PKG="com.fountainpdl.fountainplay"
PKG_PATH="com/fountainpdl/fountainplay"

echo "🎵 Setting up $PROJECT..."
mkdir -p $PROJECT && cd $PROJECT

# ── Directory Tree ──────────────────────────────────────────
mkdir -p .github/workflows
mkdir -p app/src/main/java/$PKG_PATH/ui/{home,music,video,library,settings}
mkdir -p app/src/main/java/$PKG_PATH/player
mkdir -p app/src/main/java/$PKG_PATH/service
mkdir -p app/src/main/java/$PKG_PATH/model
mkdir -p app/src/main/java/$PKG_PATH/adapter
mkdir -p app/src/main/java/$PKG_PATH/util
mkdir -p app/src/main/res/{layout,menu,navigation,values,values-night,drawable,drawable-v24,mipmap-hdpi,xml}
mkdir -p gradle/wrapper

echo "✅ Directories created"

# ════════════════════════════════════════════════════════════
# GRADLE WRAPPER PROPERTIES
# ════════════════════════════════════════════════════════════
cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

# ════════════════════════════════════════════════════════════
# ROOT build.gradle
# ════════════════════════════════════════════════════════════
cat > build.gradle << 'EOF'
// Top-level build file
plugins {
    id 'com.android.application' version '8.1.0' apply false
}
EOF

# ════════════════════════════════════════════════════════════
# settings.gradle
# ════════════════════════════════════════════════════════════
cat > settings.gradle << 'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "FountainPlay"
include ':app'
EOF

# ════════════════════════════════════════════════════════════
# gradle.properties
# ════════════════════════════════════════════════════════════
cat > gradle.properties << 'EOF'
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true
EOF

# ════════════════════════════════════════════════════════════
# APP build.gradle
# ════════════════════════════════════════════════════════════
cat > app/build.gradle << 'EOF'
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.fountainpdl.fountainplay'
    compileSdk 34

    defaultConfig {
        applicationId "com.fountainpdl.fountainplay"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            applicationIdSuffix ".debug"
            debuggable true
        }
    }

    buildFeatures {
        viewBinding true
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    // AndroidX Core
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.core:core-ktx:1.12.0'

    // Navigation Component
    implementation 'androidx.navigation:navigation-fragment:2.7.6'
    implementation 'androidx.navigation:navigation-ui:2.7.6'

    // Media3 (ExoPlayer) — replaces standalone ExoPlayer
    implementation 'androidx.media3:media3-exoplayer:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-hls:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-rtsp:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-dash:1.2.1'
    implementation 'androidx.media3:media3-ui:1.2.1'
    implementation 'androidx.media3:media3-session:1.2.1'
    implementation 'androidx.media3:media3-datasource-okhttp:1.2.1'

    // FFmpeg extension for extra codec support (like VLC)
    implementation 'androidx.media3:media3-decoder:1.2.1'

    // Room — local media library database
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'

    // Glide — album art / thumbnails
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'

    // Palette — dynamic color from album art
    implementation 'androidx.palette:palette:1.0.0'

    // ViewPager2 + TabLayout
    implementation 'androidx.viewpager2:viewpager2:1.0.0'

    // Lifecycle + ViewModel
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'

    // Preference (settings screen)
    implementation 'androidx.preference:preference:1.2.1'

    // OkHttp (network streams)
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'

    // Lottie (animations)
    implementation 'com.airbnb.android:lottie:6.3.0'
}
EOF

# ════════════════════════════════════════════════════════════
# AndroidManifest.xml
# ════════════════════════════════════════════════════════════
cat > app/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.FountainPlay"
        android:largeHeap="true">

        <!-- Main Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <!-- Open media files from file manager -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:scheme="file" />
                <data android:mimeType="video/*" />
                <data android:mimeType="audio/*" />
            </intent-filter>
        </activity>

        <!-- Video Player -->
        <activity
            android:name=".player.VideoPlayerActivity"
            android:configChanges="orientation|screenSize|keyboardHidden"
            android:screenOrientation="sensor"
            android:exported="false" />

        <!-- Audio Player -->
        <activity
            android:name=".player.AudioPlayerActivity"
            android:exported="false" />

        <!-- Background Playback Service -->
        <service
            android:name=".service.PlaybackService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>

    </application>
</manifest>
EOF

# ════════════════════════════════════════════════════════════
# COLORS — Purple/Red default, dark & light
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/values/colors.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Brand Colors -->
    <color name="fp_purple">#7B2FBE</color>
    <color name="fp_purple_dark">#5A1E8C</color>
    <color name="fp_purple_light">#9C4FD4</color>
    <color name="fp_red">#E53935</color>
    <color name="fp_red_dark">#B71C1C</color>
    <color name="fp_red_light">#EF5350</color>
    <color name="fp_gradient_start">#7B2FBE</color>
    <color name="fp_gradient_end">#E53935</color>

    <!-- Light Theme Surfaces -->
    <color name="surface_light">#FFFFFF</color>
    <color name="surface_variant_light">#F3E5F5</color>
    <color name="background_light">#FAFAFA</color>
    <color name="on_surface_light">#1A1A2E</color>
    <color name="on_surface_variant_light">#555577</color>

    <!-- Dark Theme Surfaces -->
    <color name="surface_dark">#0F0F1A</color>
    <color name="surface_variant_dark">#1C1C2E</color>
    <color name="background_dark">#08080F</color>
    <color name="on_surface_dark">#F0E6FF</color>
    <color name="on_surface_variant_dark">#A89BC2</color>

    <!-- Player UI -->
    <color name="player_bg_dark">#0A0A14</color>
    <color name="progress_bar_active">#7B2FBE</color>
    <color name="progress_bar_buffered">#4D2080</color>
    <color name="mini_player_bg">#1C1C2E</color>
    <color name="equalizer_bar">#E53935</color>

    <!-- Misc -->
    <color name="white">#FFFFFF</color>
    <color name="black">#000000</color>
    <color name="transparent">#00000000</color>
    <color name="overlay_dark">#88000000</color>
</resources>
EOF

# ════════════════════════════════════════════════════════════
# STRINGS
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/values/strings.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Fountain Play</string>
    <string name="nav_home">Home</string>
    <string name="nav_music">Music</string>
    <string name="nav_video">Video</string>
    <string name="nav_library">Library</string>
    <string name="nav_settings">Settings</string>
    <string name="now_playing">Now Playing</string>
    <string name="up_next">Up Next</string>
    <string name="shuffle">Shuffle</string>
    <string name="repeat">Repeat</string>
    <string name="equalizer">Equalizer</string>
    <string name="playback_speed">Playback Speed</string>
    <string name="subtitle_track">Subtitle Track</string>
    <string name="audio_track">Audio Track</string>
    <string name="network_stream">Network Stream</string>
    <string name="open_file">Open File</string>
    <string name="settings">Settings</string>
    <string name="theme">Theme</string>
    <string name="accent_color">Accent Color</string>
    <string name="permission_required">Storage permission is required to browse media files.</string>
    <string name="no_media_found">No media found on device</string>
    <string name="channel_name">Fountain Play</string>
    <string name="channel_desc">Media playback notification</string>
</resources>
EOF

# ════════════════════════════════════════════════════════════
# THEMES — Light
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/values/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FountainPlay" parent="Theme.Material3.Light.NoActionBar">
        <item name="colorPrimary">@color/fp_purple</item>
        <item name="colorPrimaryDark">@color/fp_purple_dark</item>
        <item name="colorAccent">@color/fp_red</item>
        <item name="colorSurface">@color/surface_light</item>
        <item name="colorOnSurface">@color/on_surface_light</item>
        <item name="android:windowBackground">@color/background_light</item>
        <item name="android:statusBarColor">@color/fp_purple_dark</item>
        <item name="android:navigationBarColor">@color/surface_light</item>
    </style>

    <style name="Theme.FountainPlay.Player" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/fp_purple</item>
        <item name="colorAccent">@color/fp_red</item>
        <item name="android:windowBackground">@color/player_bg_dark</item>
        <item name="android:statusBarColor">@color/transparent</item>
        <item name="android:windowTranslucentStatus">true</item>
    </style>

    <style name="Theme.FountainPlay.Splash" parent="Theme.Material3.Dark.NoActionBar">
        <item name="android:windowBackground">@color/fp_purple_dark</item>
        <item name="android:statusBarColor">@color/fp_purple_dark</item>
    </style>
</resources>
EOF

# ════════════════════════════════════════════════════════════
# THEMES — Dark (night override)
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/values-night/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FountainPlay" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/fp_purple_light</item>
        <item name="colorPrimaryDark">@color/fp_purple</item>
        <item name="colorAccent">@color/fp_red_light</item>
        <item name="colorSurface">@color/surface_dark</item>
        <item name="colorOnSurface">@color/on_surface_dark</item>
        <item name="android:windowBackground">@color/background_dark</item>
        <item name="android:statusBarColor">@color/background_dark</item>
        <item name="android:navigationBarColor">@color/surface_dark</item>
    </style>
</resources>
EOF

# ════════════════════════════════════════════════════════════
# BOTTOM NAV MENU
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/menu/bottom_nav_menu.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/nav_home"
        android:icon="@android:drawable/ic_menu_compass"
        android:title="@string/nav_home" />
    <item
        android:id="@+id/nav_music"
        android:icon="@android:drawable/ic_media_play"
        android:title="@string/nav_music" />
    <item
        android:id="@+id/nav_video"
        android:icon="@android:drawable/ic_menu_slideshow"
        android:title="@string/nav_video" />
    <item
        android:id="@+id/nav_library"
        android:icon="@android:drawable/ic_menu_agenda"
        android:title="@string/nav_library" />
</menu>
EOF

# ════════════════════════════════════════════════════════════
# NAVIGATION GRAPH
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/navigation/nav_graph.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_graph"
    app:startDestination="@id/nav_home">

    <fragment android:id="@+id/nav_home"
        android:name="com.fountainpdl.fountainplay.ui.home.HomeFragment"
        android:label="Home" />

    <fragment android:id="@+id/nav_music"
        android:name="com.fountainpdl.fountainplay.ui.music.MusicFragment"
        android:label="Music" />

    <fragment android:id="@+id/nav_video"
        android:name="com.fountainpdl.fountainplay.ui.video.VideoFragment"
        android:label="Video" />

    <fragment android:id="@+id/nav_library"
        android:name="com.fountainpdl.fountainplay.ui.library.LibraryFragment"
        android:label="Library" />

    <fragment android:id="@+id/nav_settings"
        android:name="com.fountainpdl.fountainplay.ui.settings.SettingsFragment"
        android:label="Settings" />
</navigation>
EOF

# ════════════════════════════════════════════════════════════
# LAYOUT — activity_main.xml
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/activity_main.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Fragment container -->
    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_marginBottom="56dp"
        app:defaultNavHost="true"
        app:navGraph="@navigation/nav_graph" />

    <!-- Mini Player (sits above bottom nav) -->
    <include
        android:id="@+id/mini_player"
        layout="@layout/layout_mini_player"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        android:layout_marginBottom="56dp"
        android:visibility="gone" />

    <!-- Bottom Navigation -->
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_nav"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:layout_gravity="bottom"
        app:menu="@menu/bottom_nav_menu"
        app:itemIconTint="@color/nav_item_color"
        app:itemTextColor="@color/nav_item_color"
        app:labelVisibilityMode="labeled" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
EOF

# ════════════════════════════════════════════════════════════
# LAYOUT — Mini Player
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/layout_mini_player.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="64dp"
    app:cardBackgroundColor="@color/mini_player_bg"
    app:cardElevation="8dp"
    app:cardCornerRadius="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingHorizontal="12dp">

        <!-- Album Art -->
        <ImageView
            android:id="@+id/mini_album_art"
            android:layout_width="44dp"
            android:layout_height="44dp"
            android:scaleType="centerCrop"
            android:background="@color/fp_purple_dark" />

        <!-- Title + Artist -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:paddingHorizontal="12dp">

            <TextView
                android:id="@+id/mini_title"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="No media playing"
                android:textColor="@color/white"
                android:textSize="14sp"
                android:maxLines="1"
                android:ellipsize="end" />

            <TextView
                android:id="@+id/mini_artist"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="—"
                android:textColor="@color/on_surface_variant_dark"
                android:textSize="12sp"
                android:maxLines="1"
                android:ellipsize="end" />
        </LinearLayout>

        <!-- Play/Pause -->
        <ImageButton
            android:id="@+id/mini_play_pause"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@android:drawable/ic_media_play"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:tint="@color/white" />

        <!-- Next -->
        <ImageButton
            android:id="@+id/mini_next"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@android:drawable/ic_media_next"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:tint="@color/white" />

    </LinearLayout>

    <!-- Progress bar at bottom of mini player -->
    <ProgressBar
        android:id="@+id/mini_progress"
        style="@style/Widget.AppCompat.ProgressBar.Horizontal"
        android:layout_width="match_parent"
        android:layout_height="2dp"
        android:layout_gravity="bottom"
        android:progressTint="@color/fp_purple"
        android:max="100"
        android:progress="0" />

</androidx.cardview.widget.CardView>
EOF

# ════════════════════════════════════════════════════════════
# LAYOUT — Fragment Home
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_home.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingBottom="16dp">

        <!-- Header -->
        <TextView
            android:id="@+id/tv_greeting"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Good evening"
            android:textSize="26sp"
            android:textStyle="bold"
            android:paddingHorizontal="16dp"
            android:paddingTop="24dp"
            android:paddingBottom="4dp" />

        <!-- Recently Played -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Recently Played"
            android:textSize="18sp"
            android:textStyle="bold"
            android:paddingHorizontal="16dp"
            android:paddingTop="20dp"
            android:paddingBottom="8dp" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rv_recent"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:paddingHorizontal="8dp"
            android:clipToPadding="false" />

        <!-- Quick Access Grid (2 columns) -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Quick Access"
            android:textSize="18sp"
            android:textStyle="bold"
            android:paddingHorizontal="16dp"
            android:paddingTop="20dp"
            android:paddingBottom="8dp" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rv_quick_access"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:paddingHorizontal="8dp"
            android:clipToPadding="false"
            android:nestedScrollingEnabled="false" />

    </LinearLayout>

</androidx.core.widget.NestedScrollView>
EOF

# ════════════════════════════════════════════════════════════
# JAVA — MainActivity.java
# ════════════════════════════════════════════════════════════
cat > app/src/main/java/$PKG_PATH/MainActivity.java << 'EOF'
package com.fountainpdl.fountainplay;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.fountainpdl.fountainplay.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private static final int PERMISSION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        requestStoragePermissions();
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment)
            getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNav, navController);
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            String[] perms = {
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            };
            boolean allGranted = true;
            for (String p : perms) {
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) ActivityCompat.requestPermissions(this, perms, PERMISSION_REQUEST);
        } else {
            String perm = Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{perm}, PERMISSION_REQUEST);
            }
        }
    }

    public void showMiniPlayer(String title, String artist) {
        binding.miniPlayer.setVisibility(View.VISIBLE);
        // TODO: bind mini player data
    }

    public void hideMiniPlayer() {
        binding.miniPlayer.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Refresh media after permission granted
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
EOF

# ════════════════════════════════════════════════════════════
# JAVA — MediaItem model
# ════════════════════════════════════════════════════════════
cat > app/src/main/java/$PKG_PATH/model/MediaItem.java << 'EOF'
package com.fountainpdl.fountainplay.model;

public class MediaItem {
    public static final int TYPE_AUDIO = 0;
    public static final int TYPE_VIDEO = 1;

    private long id;
    private String title;
    private String artist;
    private String album;
    private String path;
    private long duration; // ms
    private long size;
    private int type;
    private String albumArtUri;

    public MediaItem() {}

    public MediaItem(long id, String title, String artist, String path, long duration, int type) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.duration = duration;
        this.type = type;
    }

    // ── Getters & Setters ──
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public String getAlbumArtUri() { return albumArtUri; }
    public void setAlbumArtUri(String albumArtUri) { this.albumArtUri = albumArtUri; }

    public boolean isAudio() { return type == TYPE_AUDIO; }
    public boolean isVideo() { return type == TYPE_VIDEO; }

    public String getFormattedDuration() {
        long seconds = duration / 1000;
        long mins = seconds / 60;
        long secs = seconds % 60;
        long hours = mins / 60;
        mins = mins % 60;
        if (hours > 0) return String.format("%d:%02d:%02d", hours, mins, secs);
        return String.format("%d:%02d", mins, secs);
    }
}
EOF

# ════════════════════════════════════════════════════════════
# JAVA — MediaScanner utility
# ════════════════════════════════════════════════════════════
cat > app/src/main/java/$PKG_PATH/util/MediaScanner.java << 'EOF'
package com.fountainpdl.fountainplay.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.ArrayList;
import java.util.List;

public class MediaScanner {

    // Scan all audio files on device
    public static List<MediaItem> scanAudio(Context context) {
        List<MediaItem> items = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID
        };
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        try (Cursor cursor = cr.query(uri, projection, null, null, sortOrder)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
                int albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    String album = cursor.getString(albumCol);
                    String path = cursor.getString(pathCol);
                    long duration = cursor.getLong(durationCol);
                    long size = cursor.getLong(sizeCol);
                    long albumId = cursor.getLong(albumIdCol);

                    if (duration < 1000) continue; // skip very short clips

                    MediaItem item = new MediaItem(id, title, artist, path, duration, MediaItem.TYPE_AUDIO);
                    item.setAlbum(album);
                    item.setSize(size);
                    item.setAlbumArtUri(
                        Uri.withAppendedPath(
                            Uri.parse("content://media/external/audio/albumart"),
                            String.valueOf(albumId)
                        ).toString()
                    );
                    items.add(item);
                }
            }
        }
        return items;
    }

    // Scan all video files on device
    public static List<MediaItem> scanVideo(Context context) {
        List<MediaItem> items = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();

        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        };
        String sortOrder = MediaStore.Video.Media.DATE_MODIFIED + " DESC";

        try (Cursor cursor = cr.query(uri, projection, null, null, sortOrder)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
                int pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String path = cursor.getString(pathCol);
                    long duration = cursor.getLong(durationCol);
                    long size = cursor.getLong(sizeCol);

                    MediaItem item = new MediaItem(id, title, "", path, duration, MediaItem.TYPE_VIDEO);
                    item.setSize(size);
                    items.add(item);
                }
            }
        }
        return items;
    }
}
EOF

# ════════════════════════════════════════════════════════════
# JAVA — PlaybackService (Media3 background playback)
# ════════════════════════════════════════════════════════════
cat > app/src/main/java/$PKG_PATH/service/PlaybackService.java << 'EOF'
package com.fountainpdl.fountainplay.service;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class PlaybackService extends MediaSessionService {

    private MediaSession mediaSession;
    private ExoPlayer player;

    @Override
    public void onCreate() {
        super.onCreate();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build();

        player = new ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // handle audio focus
            .setHandleAudioBecomingNoisy(true)         // pause on headset unplug
            .build();

        mediaSession = new MediaSession.Builder(this, player).build();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.getPlayer().release();
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}
EOF

# ════════════════════════════════════════════════════════════
# JAVA — HomeFragment
# ════════════════════════════════════════════════════════════
cat > app/src/main/java/$PKG_PATH/ui/home/HomeFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.databinding.FragmentHomeBinding;

import java.util.Calendar;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.tvGreeting.setText(getGreeting());
    }

    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning ☀️";
        if (hour < 17) return "Good afternoon 🎵";
        return "Good evening 🌙";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
EOF

# ════════════════════════════════════════════════════════════
# JAVA — MusicFragment (stub)
# ════════════════════════════════════════════════════════════
cat > app/src/main/java/$PKG_PATH/ui/music/MusicFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.music;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.util.MediaScanner;
import com.fountainpdl.fountainplay.model.MediaItem;
import android.widget.TextView;
import com.fountainpdl.fountainplay.R;
import java.util.List;

public class MusicFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // TODO: replace with proper binding + RecyclerView
        TextView tv = new TextView(requireContext());
        tv.setText("Music Library — loading...");
        tv.setPadding(32, 64, 32, 32);
        tv.setTextSize(18f);

        new Thread(() -> {
            List<MediaItem> audio = MediaScanner.scanAudio(requireContext());
            requireActivity().runOnUiThread(() ->
                tv.setText("Found " + audio.size() + " audio files"));
        }).start();

        return tv;
    }
}
EOF

# ════════════════════════════════════════════════════════════
# JAVA — VideoFragment (stub)
# ════════════════════════════════════════════════════════════
cat > app/src/main/java/$PKG_PATH/ui/video/VideoFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.video;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.fountainpdl.fountainplay.util.MediaScanner;
import com.fountainpdl.fountainplay.model.MediaItem;
import android.widget.TextView;
import java.util.List;

public class VideoFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setText("Video Library — loading...");
        tv.setPadding(32, 64, 32, 32);
        tv.setTextSize(18f);

        new Thread(() -> {
            List<MediaItem> videos = MediaScanner.scanVideo(requireContext());
            requireActivity().runOnUiThread(() ->
                tv.setText("Found " + videos.size() + " video files"));
        }).start();

        return tv;
    }
}
EOF

# ════════════════════════════════════════════════════════════
# JAVA — LibraryFragment (stub)
# ════════════════════════════════════════════════════════════
cat > app/src/main/java/$PKG_PATH/ui/library/LibraryFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.library;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import android.widget.TextView;

public class LibraryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        TextView tv = new TextView(requireContext());
        tv.setText("Library — Playlists, History, Downloads");
        tv.setPadding(32, 64, 32, 32);
        tv.setTextSize(18f);
        return tv;
    }
}
EOF

# ════════════════════════════════════════════════════════════
# JAVA — VideoPlayerActivity
# ════════════════════════════════════════════════════════════
cat > app/src/main/java/$PKG_PATH/player/VideoPlayerActivity.java << 'EOF'
package com.fountainpdl.fountainplay.player;

import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.fountainpdl.fountainplay.R;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "media_uri";
    public static final String EXTRA_TITLE = "media_title";

    private ExoPlayer player;
    private PlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.player_view);

        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        if (uriStr == null) { finish(); return; }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(uriStr));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
EOF

# ════════════════════════════════════════════════════════════
# LAYOUT — activity_video_player.xml
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/activity_video_player.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/black">

    <androidx.media3.ui.PlayerView
        android:id="@+id/player_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:show_timeout="3000"
        app:resize_mode="fit"
        app:use_controller="true"
        app:controller_layout_id="@layout/custom_player_controls" />

</FrameLayout>
EOF

# ════════════════════════════════════════════════════════════
# LAYOUT — Custom Player Controls
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/custom_player_controls.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:gravity="bottom"
    android:background="@color/overlay_dark"
    android:padding="16dp">

    <!-- Title -->
    <TextView
        android:id="@id/exo_error_message"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textColor="@color/white"
        android:textSize="14sp"
        android:paddingBottom="8dp" />

    <!-- Progress bar -->
    <androidx.media3.ui.DefaultTimeBar
        android:id="@id/exo_progress"
        android:layout_width="match_parent"
        android:layout_height="26dp"
        android:focusable="true" />

    <!-- Controls row -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center"
        android:paddingTop="8dp">

        <ImageButton android:id="@id/exo_rew_with_amount"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@android:drawable/ic_media_rew"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <ImageButton android:id="@id/exo_prev"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@android:drawable/ic_media_previous"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <ImageButton android:id="@id/exo_play_pause"
            android:layout_width="56dp"
            android:layout_height="56dp"
            android:src="@android:drawable/ic_media_play"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <ImageButton android:id="@id/exo_next"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@android:drawable/ic_media_next"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <ImageButton android:id="@id/exo_ffwd_with_amount"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@android:drawable/ic_media_ff"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />

    </LinearLayout>

    <!-- Time row -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingTop="4dp">

        <TextView android:id="@id/exo_position"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="@color/white"
            android:textSize="12sp" />

        <View android:layout_width="0dp"
            android:layout_height="1dp"
            android:layout_weight="1" />

        <TextView android:id="@id/exo_duration"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="@color/white"
            android:textSize="12sp" />
    </LinearLayout>

</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# JAVA — AudioPlayerActivity
# ════════════════════════════════════════════════════════════
cat > app/src/main/java/$PKG_PATH/player/AudioPlayerActivity.java << 'EOF'
package com.fountainpdl.fountainplay.player;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.fountainpdl.fountainplay.R;

public class AudioPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "media_uri";
    public static final String EXTRA_TITLE = "media_title";
    public static final String EXTRA_ARTIST = "media_artist";

    private ExoPlayer player;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String artist = getIntent().getStringExtra(EXTRA_ARTIST);

        if (uriStr == null) { finish(); return; }

        player = new ExoPlayer.Builder(this).build();
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)));
        player.prepare();
        player.setPlayWhenReady(true);

        // TODO: wire up full audio player UI (seek bar, lyrics, equalizer)
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) { player.release(); player = null; }
    }
}
EOF

# ════════════════════════════════════════════════════════════
# LAYOUT — activity_audio_player.xml
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/activity_audio_player.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:background="@color/player_bg_dark"
    android:padding="24dp">

    <!-- Album Art -->
    <ImageView
        android:id="@+id/album_art"
        android:layout_width="260dp"
        android:layout_height="260dp"
        android:scaleType="centerCrop"
        android:src="@android:drawable/ic_media_play"
        android:background="@color/fp_purple_dark" />

    <!-- Title -->
    <TextView
        android:id="@+id/tv_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Unknown Title"
        android:textColor="@color/white"
        android:textSize="22sp"
        android:textStyle="bold"
        android:gravity="center"
        android:paddingTop="24dp"
        android:maxLines="1"
        android:ellipsize="marquee" />

    <!-- Artist -->
    <TextView
        android:id="@+id/tv_artist"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Unknown Artist"
        android:textColor="@color/on_surface_variant_dark"
        android:textSize="16sp"
        android:gravity="center"
        android:paddingTop="4dp" />

    <!-- Seek Bar -->
    <SeekBar
        android:id="@+id/seek_bar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="32dp"
        android:progressTint="@color/fp_purple"
        android:thumbTint="@color/fp_purple_light" />

    <!-- Time row -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingTop="4dp">

        <TextView android:id="@+id/tv_current_time"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="0:00"
            android:textColor="@color/on_surface_variant_dark"
            android:textSize="12sp" />

        <View android:layout_width="0dp"
            android:layout_height="1dp"
            android:layout_weight="1" />

        <TextView android:id="@+id/tv_total_time"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="0:00"
            android:textColor="@color/on_surface_variant_dark"
            android:textSize="12sp" />
    </LinearLayout>

    <!-- Controls -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center"
        android:paddingTop="24dp">

        <ImageButton android:id="@+id/btn_shuffle"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_sort_by_size"
            android:tint="@color/fp_purple_light"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <ImageButton android:id="@+id/btn_prev"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:src="@android:drawable/ic_media_previous"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:layout_marginHorizontal="8dp" />

        <ImageButton android:id="@+id/btn_play_pause"
            android:layout_width="64dp"
            android:layout_height="64dp"
            android:src="@android:drawable/ic_media_pause"
            android:tint="@color/white"
            android:background="@drawable/bg_play_button"
            android:layout_marginHorizontal="8dp" />

        <ImageButton android:id="@+id/btn_next"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:src="@android:drawable/ic_media_next"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:layout_marginHorizontal="8dp" />

        <ImageButton android:id="@+id/btn_repeat"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_rotate"
            android:tint="@color/fp_purple_light"
            android:background="?attr/selectableItemBackgroundBorderless" />

    </LinearLayout>

</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# DRAWABLE — Play button circle background
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/drawable/bg_play_button.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <gradient
        android:startColor="@color/fp_purple"
        android:endColor="@color/fp_red"
        android:angle="135"
        android:type="linear" />
</shape>
EOF

# ════════════════════════════════════════════════════════════
# DRAWABLE — Nav item color selector
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/drawable/nav_item_color.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/fp_purple" android:state_checked="true" />
    <item android:color="@color/on_surface_variant_light" />
</selector>
EOF

# ════════════════════════════════════════════════════════════
# LAUNCHER ICON (placeholder)
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/mipmap-hdpi/ic_launcher.png.placeholder << 'EOF'
# Replace with actual PNG icon
# Purple/Red gradient with "FP" text or animated character
# Use Android Studio Asset Studio or generate via https://romannurik.github.io/AndroidAssetStudio/
EOF

# ════════════════════════════════════════════════════════════
# GITHUB ACTIONS — Build APK
# ════════════════════════════════════════════════════════════
cat > .github/workflows/build.yml << 'EOF'
name: 🎵 Build Fountain Play APK

on:
  push:
    branches: [ main, dev ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    name: Build Debug APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant Gradle execute permission
        run: chmod +x gradlew

      - name: Build Debug APK
        run: ./gradlew assembleDebug --stacktrace

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: FountainPlay-debug-${{ github.sha }}
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 30

      - name: Get APK info
        run: |
          echo "✅ Build complete"
          ls -lh app/build/outputs/apk/debug/
EOF

# ════════════════════════════════════════════════════════════
# .gitignore
# ════════════════════════════════════════════════════════════
cat > .gitignore << 'EOF'
*.iml
.gradle
/local.properties
/.idea
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
app/build/
*.apk
*.aab
EOF

# ════════════════════════════════════════════════════════════
# README
# ════════════════════════════════════════════════════════════
cat > README.md << 'EOF'
# 🎵 Fountain Play

A feature-rich music and video player for Android.  
Inspired by VLC (codec support), Visha (UI), and YouTube Music (experience).

## Color Scheme
- Primary: Purple `#7B2FBE`
- Accent: Red `#E53935`
- Full dark & light mode support
- User-customizable accent colors (coming soon)

## Features (Planned)
- [x] ExoPlayer/Media3 core playback
- [x] Background audio service
- [x] Bottom navigation (Home / Music / Video / Library)
- [x] Mini player
- [x] Media scanning (audio + video)
- [ ] Full audio player UI with lyrics
- [ ] Video player with gesture controls
- [ ] Subtitle support
- [ ] Equalizer
- [ ] Network streams (HTTP/RTSP/HLS)
- [ ] Chromecast
- [ ] Playlist management
- [ ] Theme customization

## Build
GitHub Actions automatically builds the APK on every push.
Download the APK from the Actions tab > Artifacts.

## Stack
- Java (Android)
- Media3 / ExoPlayer
- Material Design 3
- Room DB
- Navigation Component
EOF

# ════════════════════════════════════════════════════════════
# DONE
# ════════════════════════════════════════════════════════════
echo ""
echo "✅ Fountain Play project scaffolded successfully!"
echo ""
echo "📁 Structure:"
find . -not -path './.git/*' -not -name '*.placeholder' | head -60
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "NEXT STEPS:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1. cd FountainPlay"
echo "2. git init"
echo "3. git remote add origin https://github.com/YOUR_USERNAME/FountainPlay.git"
echo "4. git add ."
echo "5. git commit -m 'Initial scaffold'"
echo "6. git push -u origin main"
echo "7. Go to GitHub → Actions tab → Watch the APK build!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
