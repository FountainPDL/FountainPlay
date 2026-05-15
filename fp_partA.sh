#!/bin/bash
# ── PART A: Manifest fix + UI layouts ──
# Run from inside ~/FountainPlay

# ════════════════════════════════════════════════════════════
# FIXED AndroidManifest — full media player intent filters
# ════════════════════════════════════════════════════════════
cat > app/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.FountainPlay"
        android:largeHeap="true">

        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Video player — registered for ALL video types -->
        <activity
            android:name=".player.VideoPlayerActivity"
            android:configChanges="orientation|screenSize|keyboardHidden"
            android:screenOrientation="sensor"
            android:exported="true"
            android:theme="@style/Theme.FountainPlay.Player">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="file" android:mimeType="video/*" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="content" android:mimeType="video/*" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:scheme="http" android:mimeType="video/*" />
            </intent-filter>
        </activity>

        <!-- Audio player — registered for ALL audio types -->
        <activity
            android:name=".player.AudioPlayerActivity"
            android:exported="true"
            android:theme="@style/Theme.FountainPlay.Player">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="file" android:mimeType="audio/*" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="content" android:mimeType="audio/*" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:scheme="http" android:mimeType="audio/*" />
            </intent-filter>
        </activity>

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
echo "✅ Manifest updated"

# ════════════════════════════════════════════════════════════
# STRINGS update
# ════════════════════════════════════════════════════════════
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
    <string name="all_songs">All Songs</string>
    <string name="all_videos">All Videos</string>
    <string name="sort_by">Sort by</string>
    <string name="lyrics">Lyrics</string>
    <string name="queue">Queue</string>
    <string name="unknown_artist">Unknown Artist</string>
    <string name="unknown_album">Unknown Album</string>
</resources>
EOF

# ════════════════════════════════════════════════════════════
# LAYOUT — fragment_music.xml (VLC/Visha style list)
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_music.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="?attr/colorSurface">

    <!-- Header bar -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingHorizontal="16dp"
        android:background="?attr/colorSurface"
        android:elevation="4dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/all_songs"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="?attr/colorOnSurface" />

        <TextView
            android:id="@+id/tv_song_count"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="0 songs"
            android:textSize="13sp"
            android:textColor="?attr/colorOnSurface"
            android:alpha="0.6" />
    </LinearLayout>

    <!-- Shuffle all bar (VLC style) -->
    <LinearLayout
        android:id="@+id/btn_shuffle_all"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingHorizontal="16dp"
        android:background="@color/fp_purple_dark"
        android:clickable="true"
        android:focusable="true">

        <ImageView
            android:layout_width="20dp"
            android:layout_height="20dp"
            android:src="@android:drawable/ic_media_play"
            android:tint="@color/white" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="  Shuffle All"
            android:textColor="@color/white"
            android:textSize="14sp"
            android:textStyle="bold" />
    </LinearLayout>

    <!-- Song list -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_songs"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:paddingBottom="72dp" />

</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# LAYOUT — item_song.xml (music list row)
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/item_song.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="64dp"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingHorizontal="12dp"
    android:clickable="true"
    android:focusable="true"
    android:background="?attr/selectableItemBackground">

    <!-- Album art -->
    <ImageView
        android:id="@+id/iv_album_art"
        android:layout_width="46dp"
        android:layout_height="46dp"
        android:scaleType="centerCrop"
        android:background="@color/surface_variant_dark" />

    <!-- Title + Artist -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical"
        android:paddingHorizontal="12dp">

        <TextView
            android:id="@+id/tv_song_title"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="14sp"
            android:textStyle="bold"
            android:textColor="?attr/colorOnSurface"
            android:maxLines="1"
            android:ellipsize="end" />

        <TextView
            android:id="@+id/tv_song_artist"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="12sp"
            android:textColor="?attr/colorOnSurface"
            android:alpha="0.6"
            android:maxLines="1"
            android:ellipsize="end" />
    </LinearLayout>

    <!-- Duration -->
    <TextView
        android:id="@+id/tv_song_duration"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="12sp"
        android:textColor="?attr/colorOnSurface"
        android:alpha="0.5"
        android:paddingEnd="4dp" />

    <!-- More button -->
    <ImageButton
        android:id="@+id/btn_more"
        android:layout_width="32dp"
        android:layout_height="32dp"
        android:src="@android:drawable/ic_menu_more"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:tint="?attr/colorOnSurface"
        android:alpha="0.6" />

</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# LAYOUT — fragment_video.xml (VLC grid style)
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_video.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="?attr/colorSurface">

    <!-- Header -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingHorizontal="16dp"
        android:background="?attr/colorSurface"
        android:elevation="4dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/all_videos"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="?attr/colorOnSurface" />

        <TextView
            android:id="@+id/tv_video_count"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="0 videos"
            android:textSize="13sp"
            android:textColor="?attr/colorOnSurface"
            android:alpha="0.6" />
    </LinearLayout>

    <!-- Video grid -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_videos"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:paddingBottom="72dp"
        android:paddingHorizontal="4dp" />

</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# LAYOUT — item_video.xml (video grid card)
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/item_video.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    android:clickable="true"
    android:focusable="true"
    app:cardCornerRadius="8dp"
    app:cardElevation="2dp"
    app:cardBackgroundColor="@color/surface_variant_dark">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <!-- Thumbnail -->
        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="100dp">

            <ImageView
                android:id="@+id/iv_thumbnail"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:scaleType="centerCrop"
                android:background="@color/surface_variant_dark" />

            <!-- Play icon overlay -->
            <ImageView
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:layout_gravity="center"
                android:src="@android:drawable/ic_media_play"
                android:tint="@color/white"
                android:alpha="0.8" />

            <!-- Duration badge -->
            <TextView
                android:id="@+id/tv_duration_badge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="bottom|end"
                android:layout_margin="6dp"
                android:textColor="@color/white"
                android:textSize="11sp"
                android:textStyle="bold"
                android:background="@color/overlay_dark"
                android:paddingHorizontal="4dp"
                android:paddingVertical="2dp" />
        </FrameLayout>

        <!-- Title + size -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="8dp">

            <TextView
                android:id="@+id/tv_video_title"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="13sp"
                android:textStyle="bold"
                android:textColor="@color/on_surface_dark"
                android:maxLines="2"
                android:ellipsize="end" />

            <TextView
                android:id="@+id/tv_video_info"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="11sp"
                android:textColor="@color/on_surface_variant_dark"
                android:paddingTop="2dp" />
        </LinearLayout>
    </LinearLayout>
</androidx.cardview.widget.CardView>
EOF

# ════════════════════════════════════════════════════════════
# LAYOUT — YouTube Music inspired audio player
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/activity_audio_player.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/player_bg_dark">

    <!-- Blurred background art (tinted) -->
    <ImageView
        android:id="@+id/iv_bg_blur"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="centerCrop"
        android:alpha="0.25"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />

    <!-- Dark gradient overlay -->
    <View
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@drawable/gradient_player_overlay"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />

    <!-- Top bar: back + options -->
    <LinearLayout
        android:id="@+id/top_bar"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingHorizontal="8dp"
        app:layout_constraintTop_toTopOf="parent">

        <ImageButton
            android:id="@+id/btn_back"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@android:drawable/ic_media_previous"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Now Playing"
            android:textColor="@color/white"
            android:textSize="14sp"
            android:gravity="center"
            android:alpha="0.7" />

        <ImageButton
            android:id="@+id/btn_options"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_more"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />
    </LinearLayout>

    <!-- Large album art — YouTube Music style -->
    <androidx.cardview.widget.CardView
        android:id="@+id/card_album_art"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_marginHorizontal="32dp"
        android:layout_marginTop="16dp"
        app:cardCornerRadius="16dp"
        app:cardElevation="24dp"
        app:layout_constraintTop_toBottomOf="@id/top_bar"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintWidth_percent="0.82"
        app:layout_constraintDimensionRatio="1:1">

        <ImageView
            android:id="@+id/iv_album_art"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:scaleType="centerCrop"
            android:background="@color/fp_purple_dark" />
    </androidx.cardview.widget.CardView>

    <!-- Song info -->
    <LinearLayout
        android:id="@+id/song_info"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingHorizontal="24dp"
        android:paddingTop="20dp"
        app:layout_constraintTop_toBottomOf="@id/card_album_art"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <TextView
            android:id="@+id/tv_title"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Unknown Title"
            android:textColor="@color/white"
            android:textSize="22sp"
            android:textStyle="bold"
            android:maxLines="1"
            android:ellipsize="marquee"
            android:marqueeRepeatLimit="marquee_forever"
            android:selected="true" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:paddingTop="4dp">

            <TextView
                android:id="@+id/tv_artist"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Unknown Artist"
                android:textColor="@color/on_surface_variant_dark"
                android:textSize="15sp"
                android:maxLines="1"
                android:ellipsize="end" />

            <ImageButton
                android:id="@+id/btn_favorite"
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:src="@android:drawable/btn_star_big_off"
                android:tint="@color/on_surface_variant_dark"
                android:background="?attr/selectableItemBackgroundBorderless" />
        </LinearLayout>
    </LinearLayout>

    <!-- Progress section -->
    <LinearLayout
        android:id="@+id/progress_section"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingHorizontal="24dp"
        android:paddingTop="16dp"
        app:layout_constraintTop_toBottomOf="@id/song_info"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <!-- Thick YTM-style seek bar -->
        <SeekBar
            android:id="@+id/seek_bar"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:progressTint="@color/fp_purple_light"
            android:thumbTint="@color/white"
            android:progressBackgroundTint="@color/surface_variant_dark"
            android:minHeight="4dp"
            android:max="1000" />

        <!-- Time row -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:paddingTop="4dp">

            <TextView
                android:id="@+id/tv_current_time"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="0:00"
                android:textColor="@color/on_surface_variant_dark"
                android:textSize="12sp" />

            <View android:layout_width="0dp" android:layout_height="1dp" android:layout_weight="1" />

            <TextView
                android:id="@+id/tv_total_time"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="0:00"
                android:textColor="@color/on_surface_variant_dark"
                android:textSize="12sp" />
        </LinearLayout>
    </LinearLayout>

    <!-- Main controls (YouTube Music layout) -->
    <LinearLayout
        android:id="@+id/main_controls"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center"
        android:paddingHorizontal="16dp"
        android:paddingTop="8dp"
        app:layout_constraintTop_toBottomOf="@id/progress_section"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageButton
            android:id="@+id/btn_shuffle"
            android:layout_width="44dp"
            android:layout_height="44dp"
            android:src="@android:drawable/ic_menu_sort_by_size"
            android:tint="@color/on_surface_variant_dark"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <ImageButton
            android:id="@+id/btn_prev"
            android:layout_width="52dp"
            android:layout_height="52dp"
            android:src="@android:drawable/ic_media_previous"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:layout_marginHorizontal="4dp" />

        <!-- Big play/pause with gradient circle -->
        <ImageButton
            android:id="@+id/btn_play_pause"
            android:layout_width="68dp"
            android:layout_height="68dp"
            android:src="@android:drawable/ic_media_pause"
            android:tint="@color/white"
            android:background="@drawable/bg_play_button"
            android:layout_marginHorizontal="4dp"
            android:padding="14dp" />

        <ImageButton
            android:id="@+id/btn_next"
            android:layout_width="52dp"
            android:layout_height="52dp"
            android:src="@android:drawable/ic_media_next"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:layout_marginHorizontal="4dp" />

        <ImageButton
            android:id="@+id/btn_repeat"
            android:layout_width="44dp"
            android:layout_height="44dp"
            android:src="@android:drawable/ic_menu_rotate"
            android:tint="@color/on_surface_variant_dark"
            android:background="?attr/selectableItemBackgroundBorderless" />
    </LinearLayout>

    <!-- Bottom action chips (YTM: Lyrics, Queue, Speed) -->
    <LinearLayout
        android:id="@+id/action_chips"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center"
        android:paddingHorizontal="16dp"
        android:paddingTop="12dp"
        app:layout_constraintTop_toBottomOf="@id/main_controls"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <com.google.android.material.chip.Chip
            android:id="@+id/chip_lyrics"
            android:layout_width="wrap_content"
            android:layout_height="36dp"
            android:text="Lyrics"
            android:textColor="@color/white"
            android:layout_margin="4dp"
            style="@style/Widget.Material3.Chip.Filter" />

        <com.google.android.material.chip.Chip
            android:id="@+id/chip_queue"
            android:layout_width="wrap_content"
            android:layout_height="36dp"
            android:text="Queue"
            android:textColor="@color/white"
            android:layout_margin="4dp"
            style="@style/Widget.Material3.Chip.Filter" />

        <com.google.android.material.chip.Chip
            android:id="@+id/chip_speed"
            android:layout_width="wrap_content"
            android:layout_height="36dp"
            android:text="1.0×"
            android:textColor="@color/white"
            android:layout_margin="4dp"
            style="@style/Widget.Material3.Chip.Filter" />

        <com.google.android.material.chip.Chip
            android:id="@+id/chip_eq"
            android:layout_width="wrap_content"
            android:layout_height="36dp"
            android:text="EQ"
            android:textColor="@color/white"
            android:layout_margin="4dp"
            style="@style/Widget.Material3.Chip.Filter" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
EOF

# ════════════════════════════════════════════════════════════
# DRAWABLE — gradient overlay for player background
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/drawable/gradient_player_overlay.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient
        android:startColor="#CC0A0A14"
        android:endColor="#F50A0A14"
        android:angle="270"
        android:type="linear" />
</shape>
EOF

echo "✅ PART A DONE — run fp_partB.sh next"
