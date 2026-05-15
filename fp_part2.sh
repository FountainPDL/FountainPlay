#!/bin/bash
# ── PART 2: Resources ──
# Run from inside ~/FountainPlay

cat > app/src/main/res/values/colors.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="fp_purple">#7B2FBE</color>
    <color name="fp_purple_dark">#5A1E8C</color>
    <color name="fp_purple_light">#9C4FD4</color>
    <color name="fp_red">#E53935</color>
    <color name="fp_red_dark">#B71C1C</color>
    <color name="fp_red_light">#EF5350</color>
    <color name="surface_light">#FFFFFF</color>
    <color name="surface_variant_light">#F3E5F5</color>
    <color name="background_light">#FAFAFA</color>
    <color name="on_surface_light">#1A1A2E</color>
    <color name="on_surface_variant_light">#555577</color>
    <color name="surface_dark">#0F0F1A</color>
    <color name="surface_variant_dark">#1C1C2E</color>
    <color name="background_dark">#08080F</color>
    <color name="on_surface_dark">#F0E6FF</color>
    <color name="on_surface_variant_dark">#A89BC2</color>
    <color name="player_bg_dark">#0A0A14</color>
    <color name="mini_player_bg">#1C1C2E</color>
    <color name="white">#FFFFFF</color>
    <color name="black">#000000</color>
    <color name="transparent">#00000000</color>
    <color name="overlay_dark">#88000000</color>
</resources>
EOF

cat > app/src/main/res/values/strings.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Fountain Play</string>
    <string name="nav_home">Home</string>
    <string name="nav_music">Music</string>
    <string name="nav_video">Video</string>
    <string name="nav_library">Library</string>
    <string name="now_playing">Now Playing</string>
    <string name="equalizer">Equalizer</string>
    <string name="network_stream">Network Stream</string>
    <string name="no_media_found">No media found</string>
    <string name="channel_name">Fountain Play</string>
    <string name="channel_desc">Media playback notification</string>
</resources>
EOF

cat > app/src/main/res/values/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FountainPlay" parent="Theme.Material3.Light.NoActionBar">
        <item name="colorPrimary">@color/fp_purple</item>
        <item name="colorPrimaryDark">@color/fp_purple_dark</item>
        <item name="colorAccent">@color/fp_red</item>
        <item name="colorSurface">@color/surface_light</item>
        <item name="android:windowBackground">@color/background_light</item>
        <item name="android:statusBarColor">@color/fp_purple_dark</item>
    </style>
    <style name="Theme.FountainPlay.Player" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/fp_purple</item>
        <item name="android:windowBackground">@color/player_bg_dark</item>
        <item name="android:statusBarColor">@color/transparent</item>
        <item name="android:windowTranslucentStatus">true</item>
    </style>
</resources>
EOF

cat > app/src/main/res/values-night/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FountainPlay" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/fp_purple_light</item>
        <item name="colorPrimaryDark">@color/fp_purple</item>
        <item name="colorAccent">@color/fp_red_light</item>
        <item name="colorSurface">@color/surface_dark</item>
        <item name="android:windowBackground">@color/background_dark</item>
        <item name="android:statusBarColor">@color/background_dark</item>
    </style>
</resources>
EOF

cat > app/src/main/res/menu/bottom_nav_menu.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@+id/nav_home" android:icon="@android:drawable/ic_menu_compass" android:title="@string/nav_home" />
    <item android:id="@+id/nav_music" android:icon="@android:drawable/ic_media_play" android:title="@string/nav_music" />
    <item android:id="@+id/nav_video" android:icon="@android:drawable/ic_menu_slideshow" android:title="@string/nav_video" />
    <item android:id="@+id/nav_library" android:icon="@android:drawable/ic_menu_agenda" android:title="@string/nav_library" />
</menu>
EOF

cat > app/src/main/res/navigation/nav_graph.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_graph"
    app:startDestination="@id/nav_home">
    <fragment android:id="@+id/nav_home" android:name="com.fountainpdl.fountainplay.ui.home.HomeFragment" android:label="Home" />
    <fragment android:id="@+id/nav_music" android:name="com.fountainpdl.fountainplay.ui.music.MusicFragment" android:label="Music" />
    <fragment android:id="@+id/nav_video" android:name="com.fountainpdl.fountainplay.ui.video.VideoFragment" android:label="Video" />
    <fragment android:id="@+id/nav_library" android:name="com.fountainpdl.fountainplay.ui.library.LibraryFragment" android:label="Library" />
</navigation>
EOF

cat > app/src/main/res/drawable/bg_play_button.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <gradient android:startColor="@color/fp_purple" android:endColor="@color/fp_red" android:angle="135" android:type="linear" />
</shape>
EOF

cat > app/src/main/res/drawable/nav_item_color.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/fp_purple" android:state_checked="true" />
    <item android:color="@color/on_surface_variant_light" />
</selector>
EOF

cat > app/src/main/res/layout/activity_main.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_marginBottom="56dp"
        app:defaultNavHost="true"
        app:navGraph="@navigation/nav_graph" />
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
        <TextView
            android:id="@+id/tv_greeting"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Good evening"
            android:textSize="26sp"
            android:textStyle="bold"
            android:paddingHorizontal="16dp"
            android:paddingTop="24dp" />
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
    </LinearLayout>
</androidx.core.widget.NestedScrollView>
EOF

cat > app/src/main/res/layout/activity_video_player.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
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
        app:use_controller="true" />
</FrameLayout>
EOF

cat > app/src/main/res/layout/activity_audio_player.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:background="@color/player_bg_dark"
    android:padding="24dp">
    <ImageView android:id="@+id/album_art"
        android:layout_width="260dp" android:layout_height="260dp"
        android:scaleType="centerCrop"
        android:background="@color/fp_purple_dark" />
    <TextView android:id="@+id/tv_title"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:text="Unknown Title" android:textColor="@color/white"
        android:textSize="22sp" android:textStyle="bold"
        android:gravity="center" android:paddingTop="24dp"
        android:maxLines="1" android:ellipsize="marquee" />
    <TextView android:id="@+id/tv_artist"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:text="Unknown Artist" android:textColor="@color/on_surface_variant_dark"
        android:textSize="16sp" android:gravity="center" android:paddingTop="4dp" />
    <SeekBar android:id="@+id/seek_bar"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:layout_marginTop="32dp"
        android:progressTint="@color/fp_purple"
        android:thumbTint="@color/fp_purple_light" />
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:paddingTop="4dp">
        <TextView android:id="@+id/tv_current_time"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="0:00" android:textColor="@color/on_surface_variant_dark" android:textSize="12sp" />
        <View android:layout_width="0dp" android:layout_height="1dp" android:layout_weight="1" />
        <TextView android:id="@+id/tv_total_time"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="0:00" android:textColor="@color/on_surface_variant_dark" android:textSize="12sp" />
    </LinearLayout>
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:gravity="center" android:paddingTop="24dp">
        <ImageButton android:id="@+id/btn_prev"
            android:layout_width="48dp" android:layout_height="48dp"
            android:src="@android:drawable/ic_media_previous"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:layout_marginHorizontal="8dp" />
        <ImageButton android:id="@+id/btn_play_pause"
            android:layout_width="64dp" android:layout_height="64dp"
            android:src="@android:drawable/ic_media_pause"
            android:tint="@color/white"
            android:background="@drawable/bg_play_button"
            android:layout_marginHorizontal="8dp" />
        <ImageButton android:id="@+id/btn_next"
            android:layout_width="48dp" android:layout_height="48dp"
            android:src="@android:drawable/ic_media_next"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:layout_marginHorizontal="8dp" />
    </LinearLayout>
</LinearLayout>
EOF

echo "✅ PART 2 DONE — run fp_part3.sh next"
