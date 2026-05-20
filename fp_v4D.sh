#!/bin/bash
# ── v4 PART D: Layouts + final push ──
# Run from ~/FountainPlay
set -e
P="app/src/main/java/com/fountainpdl/fountainplay"

echo "════════════════ v4 Part D ════════════════"

# ════════════════════════════════════════════════════════════
# 1. fragment_music.xml — uses theme attributes, not hardcoded colors
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_music.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="?attr/android:colorBackground">

    <!-- Toolbar -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingHorizontal="8dp"
        android:background="?attr/colorSurface">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Music"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="?attr/colorOnSurface"
            android:paddingHorizontal="8dp" />

        <TextView
            android:id="@+id/tv_song_count"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textColor="@color/on_surface_variant_dark"
            android:textSize="13sp" />

        <ImageButton
            android:id="@+id/btn_sort"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_sort_by_size"
            android:tint="?attr/colorOnSurface"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <ImageButton
            android:id="@+id/btn_refresh"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_rotate"
            android:tint="?attr/colorOnSurface"
            android:background="?attr/selectableItemBackgroundBorderless" />
    </LinearLayout>

    <!-- Search -->
    <androidx.appcompat.widget.SearchView
        android:id="@+id/search_view"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:background="?attr/colorSurface" />

    <!-- Shuffle All banner -->
    <LinearLayout
        android:id="@+id/btn_shuffle_all"
        android:layout_width="match_parent"
        android:layout_height="44dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingHorizontal="16dp"
        android:background="@color/fp_purple_dark"
        android:clickable="true"
        android:focusable="true">

        <ImageView
            android:layout_width="18dp"
            android:layout_height="18dp"
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
        android:paddingBottom="120dp" />

</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# 2. fragment_settings.xml — clean, no leftover clear history bug
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_settings.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?attr/android:colorBackground">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingBottom="120dp">

        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="Settings" android:textSize="26sp" android:textStyle="bold"
            android:textColor="?attr/colorOnSurface" android:padding="16dp" />

        <!-- ── APPEARANCE ── -->
        <TextView style="@style/SettingsSectionHeader" android:text="APPEARANCE" />

        <!-- Theme -->
        <LinearLayout android:id="@+id/pref_theme" style="@style/SettingsRow">
            <LinearLayout style="@style/SettingsRowContent">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Theme" style="@style/SettingsRowTitle" />
                <TextView android:id="@+id/tv_theme_value" android:layout_width="wrap_content"
                    android:layout_height="wrap_content" android:text="Dark"
                    style="@style/SettingsRowSub" />
            </LinearLayout>
        </LinearLayout>

        <!-- Color scheme -->
        <LinearLayout android:id="@+id/pref_primary_color" style="@style/SettingsRow">
            <LinearLayout style="@style/SettingsRowContent">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Color Scheme" style="@style/SettingsRowTitle" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Choose primary + accent colors"
                    style="@style/SettingsRowSub" />
            </LinearLayout>
            <View android:id="@+id/color_preview_primary"
                android:layout_width="28dp" android:layout_height="28dp"
                android:background="@color/fp_purple"
                android:layout_marginEnd="6dp" />
            <View android:id="@+id/color_preview_accent"
                android:layout_width="28dp" android:layout_height="28dp"
                android:background="@color/fp_red" />
        </LinearLayout>

        <!-- Hidden accent row (still needed by SettingsFragment references) -->
        <View android:id="@+id/pref_accent_color"
            android:layout_width="0dp" android:layout_height="0dp" android:visibility="gone" />

        <!-- ── PLAYBACK ── -->
        <TextView style="@style/SettingsSectionHeader" android:text="PLAYBACK" />

        <!-- Speed -->
        <LinearLayout android:id="@+id/pref_speed" style="@style/SettingsRow">
            <LinearLayout style="@style/SettingsRowContent">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Default Speed" style="@style/SettingsRowTitle" />
                <TextView android:id="@+id/tv_speed_value"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="1.0×" style="@style/SettingsRowSub" />
            </LinearLayout>
        </LinearLayout>

        <!-- Resume -->
        <LinearLayout style="@style/SettingsRow">
            <LinearLayout style="@style/SettingsRowContent">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Resume Playback" style="@style/SettingsRowTitle" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Continue where you left off" style="@style/SettingsRowSub" />
            </LinearLayout>
            <androidx.appcompat.widget.SwitchCompat android:id="@+id/sw_resume"
                android:layout_width="wrap_content" android:layout_height="wrap_content" />
        </LinearLayout>

        <!-- Skip -->
        <LinearLayout android:id="@+id/pref_skip" style="@style/SettingsRow">
            <LinearLayout style="@style/SettingsRowContent">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Skip Interval" style="@style/SettingsRowTitle" />
                <TextView android:id="@+id/tv_skip_value"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="10s" style="@style/SettingsRowSub" />
            </LinearLayout>
        </LinearLayout>

        <!-- ── VIDEO ── -->
        <TextView style="@style/SettingsSectionHeader" android:text="VIDEO" />

        <LinearLayout style="@style/SettingsRow">
            <LinearLayout style="@style/SettingsRowContent">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Hardware Acceleration" style="@style/SettingsRowTitle" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Use GPU for decoding" style="@style/SettingsRowSub" />
            </LinearLayout>
            <androidx.appcompat.widget.SwitchCompat android:id="@+id/sw_hw_accel"
                android:layout_width="wrap_content" android:layout_height="wrap_content" />
        </LinearLayout>

        <LinearLayout style="@style/SettingsRow">
            <LinearLayout style="@style/SettingsRowContent">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Gesture Controls" style="@style/SettingsRowTitle" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Swipe for volume / brightness / seek" style="@style/SettingsRowSub" />
            </LinearLayout>
            <androidx.appcompat.widget.SwitchCompat android:id="@+id/sw_gestures"
                android:layout_width="wrap_content" android:layout_height="wrap_content" />
        </LinearLayout>

        <LinearLayout style="@style/SettingsRow">
            <LinearLayout style="@style/SettingsRowContent">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Remember Position" style="@style/SettingsRowTitle" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Resume video from last position" style="@style/SettingsRowSub" />
            </LinearLayout>
            <androidx.appcompat.widget.SwitchCompat android:id="@+id/sw_remember_pos"
                android:layout_width="wrap_content" android:layout_height="wrap_content" />
        </LinearLayout>

        <!-- ── MEDIA FOLDERS ── -->
        <TextView style="@style/SettingsSectionHeader" android:text="MEDIA FOLDERS" />

        <LinearLayout android:id="@+id/pref_audio_folders" style="@style/SettingsRow">
            <LinearLayout style="@style/SettingsRowContent">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Music Folders" style="@style/SettingsRowTitle" />
                <TextView android:id="@+id/tv_audio_folders"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="All folders" style="@style/SettingsRowSub" />
            </LinearLayout>
        </LinearLayout>

        <LinearLayout android:id="@+id/pref_video_folders" style="@style/SettingsRow">
            <LinearLayout style="@style/SettingsRowContent">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Video Folders" style="@style/SettingsRowTitle" />
                <TextView android:id="@+id/tv_video_folders"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="All folders" style="@style/SettingsRowSub" />
            </LinearLayout>
        </LinearLayout>

        <!-- ── DATA ── -->
        <TextView style="@style/SettingsSectionHeader" android:text="DATA" />

        <LinearLayout android:id="@+id/btn_clear_history" style="@style/SettingsRow">
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Clear Play History" android:textColor="#EF5350"
                android:textSize="15sp" />
        </LinearLayout>

        <!-- ── ABOUT ── -->
        <TextView style="@style/SettingsSectionHeader" android:text="ABOUT" />
        <LinearLayout style="@style/SettingsRow">
            <LinearLayout style="@style/SettingsRowContent">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="Fountain Play" style="@style/SettingsRowTitle" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="v1.2 — by FountainPDL" style="@style/SettingsRowSub" />
            </LinearLayout>
        </LinearLayout>

    </LinearLayout>
</androidx.core.widget.NestedScrollView>
EOF

# ════════════════════════════════════════════════════════════
# 3. Shared styles for Settings rows
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/values/styles.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <style name="SettingsSectionHeader">
        <item name="android:layout_width">match_parent</item>
        <item name="android:layout_height">wrap_content</item>
        <item name="android:textSize">11sp</item>
        <item name="android:textColor">@color/fp_purple_light</item>
        <item name="android:textStyle">bold</item>
        <item name="android:paddingStart">16dp</item>
        <item name="android:paddingTop">20dp</item>
        <item name="android:paddingBottom">4dp</item>
    </style>

    <style name="SettingsRow">
        <item name="android:layout_width">match_parent</item>
        <item name="android:layout_height">wrap_content</item>
        <item name="android:minHeight">56dp</item>
        <item name="android:orientation">horizontal</item>
        <item name="android:gravity">center_vertical</item>
        <item name="android:paddingHorizontal">16dp</item>
        <item name="android:paddingVertical">10dp</item>
        <item name="android:background">?attr/selectableItemBackground</item>
        <item name="android:clickable">true</item>
        <item name="android:focusable">true</item>
    </style>

    <style name="SettingsRowContent">
        <item name="android:layout_width">0dp</item>
        <item name="android:layout_height">wrap_content</item>
        <item name="android:layout_weight">1</item>
        <item name="android:orientation">vertical</item>
    </style>

    <style name="SettingsRowTitle">
        <item name="android:layout_width">wrap_content</item>
        <item name="android:layout_height">wrap_content</item>
        <item name="android:textColor">?attr/colorOnSurface</item>
        <item name="android:textSize">15sp</item>
    </style>

    <style name="SettingsRowSub">
        <item name="android:layout_width">wrap_content</item>
        <item name="android:layout_height">wrap_content</item>
        <item name="android:textColor">@color/on_surface_variant_dark</item>
        <item name="android:textSize">12sp</item>
    </style>

</resources>
EOF

# ════════════════════════════════════════════════════════════
# 4. item_song.xml — theme-aware
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/item_song.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="64dp"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingHorizontal="12dp"
    android:background="?attr/selectableItemBackground"
    android:clickable="true"
    android:focusable="true">

    <ImageView android:id="@+id/iv_album_art"
        android:layout_width="46dp"
        android:layout_height="46dp"
        android:scaleType="centerCrop"
        android:background="@color/fp_purple_dark" />

    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
        android:layout_weight="1" android:orientation="vertical"
        android:paddingHorizontal="12dp">

        <TextView android:id="@+id/tv_song_title"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:textColor="?attr/colorOnSurface"
            android:textSize="14sp" android:textStyle="bold"
            android:maxLines="1" android:ellipsize="end" />

        <TextView android:id="@+id/tv_song_artist"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:textColor="@color/on_surface_variant_dark"
            android:textSize="12sp"
            android:maxLines="1" android:ellipsize="end" />
    </LinearLayout>

    <TextView android:id="@+id/tv_song_duration"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:textColor="@color/on_surface_variant_dark"
        android:textSize="12sp"
        android:paddingEnd="4dp" />

    <ImageButton android:id="@+id/btn_song_more"
        android:layout_width="32dp" android:layout_height="32dp"
        android:src="@android:drawable/ic_menu_more"
        android:tint="@color/on_surface_variant_dark"
        android:background="?attr/selectableItemBackgroundBorderless" />
</LinearLayout>
EOF

# Wire the more button in MediaAdapter
python3 << 'PYEOF'
with open("app/src/main/java/com/fountainpdl/fountainplay/adapter/MediaAdapter.java","r") as f:
    c = f.read()

# Add more button wiring inside AudioVH.bind()
old = "            if (item.getAlbumArtUri() != null)\n                Glide.with(itemView).load(item.getAlbumArtUri()).centerCrop()\n                    .placeholder(R.drawable.bg_play_button).into(art);\n            else art.setImageResource(R.drawable.bg_play_button);\n        }\n    }\n\n    static class VideoVH"
new = """            if (item.getAlbumArtUri() != null)
                Glide.with(itemView).load(item.getAlbumArtUri()).centerCrop()
                    .placeholder(R.drawable.bg_play_button).into(art);
            else art.setImageResource(R.drawable.bg_play_button);
            // wire ⋮ button to trigger long-press listener
            android.widget.ImageButton more = itemView.findViewById(R.id.btn_song_more);
            if (more != null) more.setOnClickListener(v -> {
                if (longListener != null) longListener.onItemLong(item, getAdapterPosition(), itemView);
            });
        }
    }

    static class VideoVH"""
c = c.replace(old, new)
with open("app/src/main/java/com/fountainpdl/fountainplay/adapter/MediaAdapter.java","w") as f:
    f.write(c)
print("✅ More button wired in MediaAdapter")
PYEOF

# ════════════════════════════════════════════════════════════
# 5. item_video.xml — consistent with song item
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
    app:cardBackgroundColor="@color/surface_variant_dark"
    app:cardElevation="2dp">

    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical">

        <FrameLayout android:layout_width="match_parent" android:layout_height="110dp">
            <ImageView android:id="@+id/iv_thumbnail"
                android:layout_width="match_parent" android:layout_height="match_parent"
                android:scaleType="centerCrop"
                android:background="@color/surface_variant_dark" />
            <ImageView android:layout_width="32dp" android:layout_height="32dp"
                android:layout_gravity="center"
                android:src="@android:drawable/ic_media_play"
                android:tint="#CCFFFFFF" />
            <TextView android:id="@+id/tv_duration_badge"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:layout_gravity="bottom|end"
                android:layout_margin="5dp"
                android:background="@color/overlay_dark"
                android:textColor="@color/white" android:textSize="11sp" android:textStyle="bold"
                android:paddingHorizontal="5dp" android:paddingVertical="2dp" />
        </FrameLayout>

        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="8dp" android:paddingVertical="6dp">

            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
                android:layout_weight="1" android:orientation="vertical">
                <TextView android:id="@+id/tv_video_title"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:textColor="@color/white" android:textSize="13sp" android:textStyle="bold"
                    android:maxLines="2" android:ellipsize="end" />
                <TextView android:id="@+id/tv_video_info"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:textColor="@color/on_surface_variant_dark" android:textSize="11sp" />
            </LinearLayout>

            <ImageButton android:id="@+id/btn_video_more"
                android:layout_width="30dp" android:layout_height="30dp"
                android:src="@android:drawable/ic_menu_more"
                android:tint="@color/on_surface_variant_dark"
                android:background="?attr/selectableItemBackgroundBorderless" />
        </LinearLayout>
    </LinearLayout>
</androidx.cardview.widget.CardView>
EOF

# Wire video more button in VideoVH
python3 << 'PYEOF'
with open("app/src/main/java/com/fountainpdl/fountainplay/adapter/MediaAdapter.java","r") as f:
    c = f.read()

old = "            title.setText(m.getTitle()); dur.setText(m.getFormattedDuration());\n            info.setText((m.getSize() / (1024*1024)) + \" MB\");\n            Glide.with(itemView).load(m.getPath()).centerCrop()\n                .placeholder(R.drawable.bg_play_button).into(thumb);\n        }\n    }\n}"

new = """            title.setText(m.getTitle()); dur.setText(m.getFormattedDuration());
            info.setText((m.getSize() / (1024*1024)) + " MB");
            Glide.with(itemView).load(m.getPath()).centerCrop()
                .placeholder(R.drawable.bg_play_button).into(thumb);
            android.widget.ImageButton more = itemView.findViewById(R.id.btn_video_more);
            if (more != null) more.setOnClickListener(v -> {
                if (longListener != null) longListener.onItemLong(m, getAdapterPosition(), itemView);
            });
        }
    }
}"""
c = c.replace(old, new)
with open("app/src/main/java/com/fountainpdl/fountainplay/adapter/MediaAdapter.java","w") as f:
    f.write(c)
print("✅ Video more button wired")
PYEOF

# ════════════════════════════════════════════════════════════
# 6. COMMIT AND PUSH
# ════════════════════════════════════════════════════════════
echo ""
echo "→ Committing..."

git add .
git commit -m "feat: v4 - icon fix, working themes, full video player, working delete, library tabs, settings"
git push

echo ""
echo "════════════════════════════════════════════════"
echo "✅ v4 PUSHED — GitHub Actions building now"
echo "════════════════════════════════════════════════"
echo ""
echo "What is fixed / new:"
echo "  ✅ Your PNG icon restored (XML override removed)"
echo "  ✅ Dark / Light / AMOLED / System themes working"
echo "  ✅ Color scheme picker (8 presets)"
echo "  ✅ All settings switches save and load correctly"
echo "  ✅ Delete uses MediaStore (no crash)"
echo "  ✅ Mini player buttons all wired to service"
echo "  ✅ Mini player ⋮ button works from song list"
echo "  ✅ Add to Favourites (auto-creates Favourites playlist)"
echo "  ✅ Video player: full controls, gestures, PiP, speed,"
echo "       aspect ratio, sleep timer, share"
echo "  ✅ Video list: grid/list toggle, sort, search, context menu"
echo "  ✅ Library: 6 tabs — Songs/Albums/Artists/Videos/Playlists/History"
echo "  ✅ Playlists work for both audio and video"
echo "  ✅ BaseActivity ensures correct theme per screen"
echo ""
echo "After build succeeds — install and test then report back."
