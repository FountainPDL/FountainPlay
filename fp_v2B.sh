#!/bin/bash
# ── v2 PART B: Home, Library, Mini Player, MainActivity ──
# Run from inside ~/FountainPlay

P="app/src/main/java/com/fountainpdl/fountainplay"

# ════════════════════════════════════════════════════════════
# 1. HOME LAYOUT — Recently Added + Recently Played sections
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_home.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background_dark">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingBottom="120dp">

        <!-- Greeting -->
        <TextView android:id="@+id/tv_greeting"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:textSize="28sp" android:textStyle="bold"
            android:textColor="@color/white"
            android:paddingHorizontal="16dp" android:paddingTop="24dp" android:paddingBottom="8dp" />

        <!-- Recently Played Music -->
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp" android:paddingTop="20dp" android:paddingBottom="8dp">
            <TextView android:layout_width="0dp" android:layout_height="wrap_content"
                android:layout_weight="1" android:text="Recently Played"
                android:textSize="18sp" android:textStyle="bold" android:textColor="@color/white" />
            <TextView android:id="@+id/tv_see_all_music"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="See all" android:textColor="@color/fp_purple_light" android:textSize="13sp" />
        </LinearLayout>

        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv_recently_played"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:paddingHorizontal="8dp"
            android:clipToPadding="false" />

        <!-- Recently Added Music -->
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp" android:paddingTop="20dp" android:paddingBottom="8dp">
            <TextView android:layout_width="0dp" android:layout_height="wrap_content"
                android:layout_weight="1" android:text="Recently Added Music"
                android:textSize="18sp" android:textStyle="bold" android:textColor="@color/white" />
        </LinearLayout>

        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv_recent_music"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:paddingHorizontal="8dp"
            android:clipToPadding="false" />

        <!-- Recently Added Videos -->
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingHorizontal="16dp" android:paddingTop="20dp" android:paddingBottom="8dp">
            <TextView android:layout_width="0dp" android:layout_height="wrap_content"
                android:layout_weight="1" android:text="Recently Added Videos"
                android:textSize="18sp" android:textStyle="bold" android:textColor="@color/white" />
        </LinearLayout>

        <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv_recent_videos"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:paddingHorizontal="8dp"
            android:clipToPadding="false" />

    </LinearLayout>
</androidx.core.widget.NestedScrollView>
EOF

# ════════════════════════════════════════════════════════════
# 2. HOME CARD layouts
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/item_home_music_card.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="140dp" android:layout_height="wrap_content"
    android:layout_margin="6dp" android:clickable="true" android:focusable="true"
    app:cardCornerRadius="10dp" app:cardBackgroundColor="@color/surface_variant_dark"
    app:cardElevation="3dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical">
        <ImageView android:id="@+id/iv_card_art"
            android:layout_width="match_parent" android:layout_height="140dp"
            android:scaleType="centerCrop" android:background="@color/fp_purple_dark" />
        <TextView android:id="@+id/tv_card_title"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:textColor="@color/white" android:textSize="13sp" android:textStyle="bold"
            android:maxLines="1" android:ellipsize="end"
            android:paddingHorizontal="8dp" android:paddingTop="6dp" />
        <TextView android:id="@+id/tv_card_sub"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:textColor="@color/on_surface_variant_dark" android:textSize="11sp"
            android:maxLines="1" android:ellipsize="end"
            android:paddingHorizontal="8dp" android:paddingBottom="8dp" />
    </LinearLayout>
</androidx.cardview.widget.CardView>
EOF

cat > app/src/main/res/layout/item_home_video_card.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="180dp" android:layout_height="wrap_content"
    android:layout_margin="6dp" android:clickable="true" android:focusable="true"
    app:cardCornerRadius="10dp" app:cardBackgroundColor="@color/surface_variant_dark"
    app:cardElevation="3dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical">
        <FrameLayout android:layout_width="match_parent" android:layout_height="100dp">
            <ImageView android:id="@+id/iv_card_thumb"
                android:layout_width="match_parent" android:layout_height="match_parent"
                android:scaleType="centerCrop" android:background="@color/surface_variant_dark" />
            <ImageView android:layout_width="28dp" android:layout_height="28dp"
                android:layout_gravity="center" android:src="@android:drawable/ic_media_play"
                android:tint="@color/white" android:alpha="0.85" />
            <TextView android:id="@+id/tv_card_duration"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:layout_gravity="bottom|end" android:layout_margin="5dp"
                android:background="@color/overlay_dark"
                android:textColor="@color/white" android:textSize="10sp" android:textStyle="bold"
                android:paddingHorizontal="4dp" android:paddingVertical="2dp" />
        </FrameLayout>
        <TextView android:id="@+id/tv_card_title"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:textColor="@color/white" android:textSize="12sp" android:textStyle="bold"
            android:maxLines="2" android:ellipsize="end"
            android:paddingHorizontal="8dp" android:paddingTop="6dp" android:paddingBottom="8dp" />
    </LinearLayout>
</androidx.cardview.widget.CardView>
EOF

# ════════════════════════════════════════════════════════════
# 3. LIBRARY LAYOUT — ViewPager2 with tabs
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_library.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/background_dark">

    <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
        android:text="Library" android:textSize="24sp" android:textStyle="bold"
        android:textColor="@color/white" android:padding="16dp" />

    <com.google.android.material.tabs.TabLayout
        android:id="@+id/tab_layout"
        android:layout_width="match_parent" android:layout_height="48dp"
        app:tabMode="scrollable"
        app:tabTextColor="@color/on_surface_variant_dark"
        app:tabSelectedTextColor="@color/white"
        app:tabIndicatorColor="@color/fp_purple"
        app:tabBackground="@color/background_dark" />

    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/view_pager"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# 4. LIBRARY SUB-PAGE layouts
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_library_page.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/background_dark">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_library"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:paddingBottom="100dp" />

    <TextView android:id="@+id/tv_empty"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:text="Nothing here yet" android:textColor="@color/on_surface_variant_dark"
        android:textSize="16sp" android:gravity="center" android:padding="48dp"
        android:visibility="gone" />
</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# 5. MINI PLAYER LAYOUT (improved)
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/layout_mini_player.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@color/mini_player_bg"
    android:elevation="8dp">

    <!-- Thin progress bar at top -->
    <ProgressBar android:id="@+id/mini_progress"
        style="@style/Widget.AppCompat.ProgressBar.Horizontal"
        android:layout_width="match_parent" android:layout_height="2dp"
        android:progressTint="@color/fp_purple"
        android:progressBackgroundTint="@color/surface_variant_dark"
        android:max="1000" android:progress="0" />

    <LinearLayout android:layout_width="match_parent" android:layout_height="60dp"
        android:orientation="horizontal" android:gravity="center_vertical"
        android:paddingHorizontal="12dp">

        <ImageView android:id="@+id/mini_album_art"
            android:layout_width="42dp" android:layout_height="42dp"
            android:scaleType="centerCrop" android:background="@color/fp_purple_dark" />

        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:orientation="vertical"
            android:paddingHorizontal="12dp">
            <TextView android:id="@+id/mini_title"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Not playing" android:textColor="@color/white"
                android:textSize="13sp" android:textStyle="bold"
                android:maxLines="1" android:ellipsize="end" />
            <TextView android:id="@+id/mini_artist"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="—" android:textColor="@color/on_surface_variant_dark"
                android:textSize="12sp" android:maxLines="1" android:ellipsize="end" />
        </LinearLayout>

        <ImageButton android:id="@+id/mini_prev"
            android:layout_width="36dp" android:layout_height="36dp"
            android:src="@android:drawable/ic_media_previous" android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <ImageButton android:id="@+id/mini_play_pause"
            android:layout_width="40dp" android:layout_height="40dp"
            android:src="@android:drawable/ic_media_play" android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <ImageButton android:id="@+id/mini_next"
            android:layout_width="36dp" android:layout_height="36dp"
            android:src="@android:drawable/ic_media_next" android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <ImageButton android:id="@+id/mini_close"
            android:layout_width="32dp" android:layout_height="32dp"
            android:src="@android:drawable/ic_menu_close_clear_cancel"
            android:tint="@color/on_surface_variant_dark"
            android:background="?attr/selectableItemBackgroundBorderless" />
    </LinearLayout>
</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# 6. MAIN ACTIVITY LAYOUT
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/activity_main.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background_dark">

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_marginBottom="112dp"
        app:defaultNavHost="true"
        app:navGraph="@navigation/nav_graph" />

    <!-- Mini player sits above bottom nav -->
    <include android:id="@+id/mini_player"
        layout="@layout/layout_mini_player"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        android:layout_marginBottom="56dp"
        android:visibility="gone" />

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_nav"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:layout_gravity="bottom"
        android:background="@color/surface_dark"
        app:menu="@menu/bottom_nav_menu"
        app:itemIconTint="@color/nav_item_color"
        app:itemTextColor="@color/nav_item_color"
        app:labelVisibilityMode="labeled" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
EOF

# ════════════════════════════════════════════════════════════
# 7. PlaybackState singleton — shared player state
# ════════════════════════════════════════════════════════════
cat > $P/util/PlaybackState.java << 'EOF'
package com.fountainpdl.fountainplay.util;

import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.ArrayList;
import java.util.List;

/** Singleton holding current playback state across the app */
public class PlaybackState {
    private static PlaybackState instance;

    private MediaItem currentItem;
    private List<MediaItem> queue = new ArrayList<>();
    private int queueIndex = 0;
    private boolean isPlaying = false;
    private long position = 0;

    public interface Listener {
        void onItemChanged(MediaItem item);
        void onPlayStateChanged(boolean playing);
        void onPositionChanged(long pos, long duration);
    }

    private final List<Listener> listeners = new ArrayList<>();

    private PlaybackState() {}

    public static PlaybackState get() {
        if (instance == null) instance = new PlaybackState();
        return instance;
    }

    public void setCurrentItem(MediaItem item) {
        currentItem = item;
        for (Listener l : listeners) l.onItemChanged(item);
    }

    public MediaItem getCurrentItem() { return currentItem; }

    public void setQueue(List<MediaItem> q, int index) {
        queue = q;
        queueIndex = index;
    }

    public List<MediaItem> getQueue() { return queue; }
    public int getQueueIndex() { return queueIndex; }

    public void setPlaying(boolean p) {
        isPlaying = p;
        for (Listener l : listeners) l.onPlayStateChanged(p);
    }

    public boolean isPlaying() { return isPlaying; }

    public void setPosition(long pos) { this.position = pos; }
    public long getPosition() { return position; }

    public void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }

    public boolean hasMedia() { return currentItem != null; }
}
EOF

# ════════════════════════════════════════════════════════════
# 8. UPDATED MainActivity — mini player + full screen
# ════════════════════════════════════════════════════════════
cat > $P/MainActivity.java << 'EOF'
package com.fountainpdl.fountainplay;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.databinding.ActivityMainBinding;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.util.PlaybackState;

public class MainActivity extends AppCompatActivity implements PlaybackState.Listener {

    private ActivityMainBinding binding;
    private NavController navController;
    private static final int PERMISSION_REQUEST = 100;

    // Mini player views
    private View miniPlayer;
    private ImageView miniArt;
    private TextView miniTitle, miniArtist;
    private ImageButton miniPlayPause, miniPrev, miniNext, miniClose;
    private ProgressBar miniProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen + keep screen on for video
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        setupMiniPlayer();
        requestStoragePermissions();

        PlaybackState.get().addListener(this);
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment)
            getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNav, navController);
    }

    private void setupMiniPlayer() {
        miniPlayer = binding.miniPlayer;
        miniArt = miniPlayer.findViewById(R.id.mini_album_art);
        miniTitle = miniPlayer.findViewById(R.id.mini_title);
        miniArtist = miniPlayer.findViewById(R.id.mini_artist);
        miniPlayPause = miniPlayer.findViewById(R.id.mini_play_pause);
        miniPrev = miniPlayer.findViewById(R.id.mini_prev);
        miniNext = miniPlayer.findViewById(R.id.mini_next);
        miniClose = miniPlayer.findViewById(R.id.mini_close);
        miniProgress = miniPlayer.findViewById(R.id.mini_progress);

        // Tap mini player → reopen audio player
        miniPlayer.setOnClickListener(v -> {
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null && cur.isAudio()) {
                Intent i = new Intent(this, AudioPlayerActivity.class);
                i.putExtra(AudioPlayerActivity.EXTRA_URI, cur.getPath());
                i.putExtra(AudioPlayerActivity.EXTRA_TITLE, cur.getTitle());
                i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, cur.getArtist());
                i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, cur.getAlbumArtUri());
                i.putExtra(AudioPlayerActivity.EXTRA_RESUME, true);
                startActivity(i);
            }
        });

        miniClose.setOnClickListener(v -> {
            miniPlayer.setVisibility(View.GONE);
            PlaybackState.get().setCurrentItem(null);
        });

        // Play/pause, prev, next wired via PlaybackState broadcast
        miniPlayPause.setOnClickListener(v -> {
            // Signal the active player via broadcast
            sendBroadcast(new Intent("com.fountainpdl.fountainplay.TOGGLE_PLAY"));
        });
        miniNext.setOnClickListener(v ->
            sendBroadcast(new Intent("com.fountainpdl.fountainplay.NEXT")));
        miniPrev.setOnClickListener(v ->
            sendBroadcast(new Intent("com.fountainpdl.fountainplay.PREV")));
    }

    public void showMiniPlayer(MediaItem item) {
        if (item == null) return;
        miniPlayer.setVisibility(View.VISIBLE);
        miniTitle.setText(item.getTitle());
        miniArtist.setText(item.getArtist());
        if (item.getAlbumArtUri() != null) {
            Glide.with(this).load(item.getAlbumArtUri()).centerCrop().into(miniArt);
        } else {
            miniArt.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    public void updateMiniProgress(int progress) {
        miniProgress.setProgress(progress);
    }

    public void hideMiniPlayer() {
        miniPlayer.setVisibility(View.GONE);
    }

    @Override
    public void onItemChanged(MediaItem item) {
        runOnUiThread(() -> {
            if (item != null) showMiniPlayer(item);
            else hideMiniPlayer();
        });
    }

    @Override
    public void onPlayStateChanged(boolean playing) {
        runOnUiThread(() -> miniPlayPause.setImageResource(
            playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play));
    }

    @Override
    public void onPositionChanged(long pos, long duration) {
        if (duration > 0) runOnUiThread(() ->
            updateMiniProgress((int)(pos * 1000 / duration)));
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] perms = {Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO};
            boolean allGranted = true;
            for (String p : perms)
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) { allGranted = false; break; }
            if (!allGranted) ActivityCompat.requestPermissions(this, perms, PERMISSION_REQUEST);
        } else {
            String p = Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, new String[]{p}, PERMISSION_REQUEST);
        }
    }

    @Override public boolean onSupportNavigateUp() { return navController.navigateUp() || super.onSupportNavigateUp(); }
    @Override protected void onDestroy() { super.onDestroy(); PlaybackState.get().removeListener(this); }
}
EOF

# ════════════════════════════════════════════════════════════
# 9. HomeFragment — real data
# ════════════════════════════════════════════════════════════
cat > $P/ui/home/HomeFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.player.VideoPlayerActivity;
import com.fountainpdl.fountainplay.util.MediaScanner;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        ((TextView)view.findViewById(R.id.tv_greeting)).setText(
            h < 12 ? "Good morning ☀️" : h < 17 ? "Good afternoon 🎵" : "Good evening 🌙");

        RecyclerView rvRecentMusic = view.findViewById(R.id.rv_recent_music);
        RecyclerView rvRecentVideos = view.findViewById(R.id.rv_recent_videos);
        RecyclerView rvRecentPlayed = view.findViewById(R.id.rv_recently_played);

        rvRecentMusic.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecentVideos.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecentPlayed.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        new Thread(() -> {
            List<MediaItem> audio = MediaScanner.scanAudio(requireContext());
            List<MediaItem> video = MediaScanner.scanVideo(requireContext());

            // Recently added = first 10 from date-sorted scan
            List<MediaItem> recentAudio = audio.size() > 10 ? audio.subList(0, 10) : audio;
            List<MediaItem> recentVideo = video.size() > 10 ? video.subList(0, 10) : video;

            requireActivity().runOnUiThread(() -> {
                rvRecentMusic.setAdapter(new MusicCardAdapter(recentAudio, item -> {
                    Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
                    i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
                    i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
                    i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
                    i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
                    startActivity(i);
                }));

                rvRecentVideos.setAdapter(new VideoCardAdapter(recentVideo, item -> {
                    Intent i = new Intent(requireContext(), VideoPlayerActivity.class);
                    i.putExtra(VideoPlayerActivity.EXTRA_URI, item.getPath());
                    startActivity(i);
                }));

                // Recently played = same as recent audio for now (will improve with history)
                rvRecentPlayed.setAdapter(new MusicCardAdapter(recentAudio, item -> {
                    Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
                    i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
                    i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
                    i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
                    i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
                    startActivity(i);
                }));
            });
        }).start();
    }

    // ── Inline Music Card Adapter ──
    static class MusicCardAdapter extends RecyclerView.Adapter<MusicCardAdapter.VH> {
        interface OnClick { void onClick(MediaItem item); }
        final List<MediaItem> items; final OnClick click;
        MusicCardAdapter(List<MediaItem> i, OnClick c) { items = i; click = c; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_home_music_card, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) { h.bind(items.get(pos)); h.itemView.setOnClickListener(v -> click.onClick(items.get(pos))); }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            ImageView art; TextView title, sub;
            VH(View v) { super(v); art = v.findViewById(R.id.iv_card_art); title = v.findViewById(R.id.tv_card_title); sub = v.findViewById(R.id.tv_card_sub); }
            void bind(MediaItem m) {
                title.setText(m.getTitle()); sub.setText(m.getArtist());
                Glide.with(itemView.getContext()).load(m.getAlbumArtUri()).centerCrop().placeholder(R.drawable.bg_play_button).into(art);
            }
        }
    }

    // ── Inline Video Card Adapter ──
    static class VideoCardAdapter extends RecyclerView.Adapter<VideoCardAdapter.VH> {
        interface OnClick { void onClick(MediaItem item); }
        final List<MediaItem> items; final OnClick click;
        VideoCardAdapter(List<MediaItem> i, OnClick c) { items = i; click = c; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_home_video_card, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) { h.bind(items.get(pos)); h.itemView.setOnClickListener(v -> click.onClick(items.get(pos))); }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            ImageView thumb; TextView title, duration;
            VH(View v) { super(v); thumb = v.findViewById(R.id.iv_card_thumb); title = v.findViewById(R.id.tv_card_title); duration = v.findViewById(R.id.tv_card_duration); }
            void bind(MediaItem m) {
                title.setText(m.getTitle()); duration.setText(m.getFormattedDuration());
                Glide.with(itemView.getContext()).load(m.getPath()).centerCrop().placeholder(R.drawable.bg_play_button).into(thumb);
            }
        }
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 10. LibraryFragment — tabbed (Songs / Albums / Artists / Playlists / History)
# ════════════════════════════════════════════════════════════
cat > $P/ui/library/LibraryFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.library;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.fountainpdl.fountainplay.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LibraryFragment extends Fragment {

    private static final String[] TABS = {"Songs", "Albums", "Artists", "Playlists", "History"};

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewPager2 vp = view.findViewById(R.id.view_pager);
        TabLayout tabs = view.findViewById(R.id.tab_layout);

        vp.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int pos) { return LibraryPageFragment.newInstance(TABS[pos]); }
            @Override public int getItemCount() { return TABS.length; }
        });

        new TabLayoutMediator(tabs, vp, (tab, pos) -> tab.setText(TABS[pos])).attach();
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 11. LibraryPageFragment — each tab page
# ════════════════════════════════════════════════════════════
cat > $P/ui/library/LibraryPageFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.library;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.util.MediaScanner;
import java.util.ArrayList;
import java.util.List;

public class LibraryPageFragment extends Fragment {
    private static final String ARG_TAB = "tab";

    public static LibraryPageFragment newInstance(String tab) {
        LibraryPageFragment f = new LibraryPageFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TAB, tab);
        f.setArguments(b);
        return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String tab = getArguments() != null ? getArguments().getString(ARG_TAB, "Songs") : "Songs";

        RecyclerView rv = view.findViewById(R.id.rv_library);
        TextView tvEmpty = view.findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<MediaItem> items = new ArrayList<>();
        MediaAdapter adapter = new MediaAdapter(items, 0);
        rv.setAdapter(adapter);

        adapter.setOnItemClickListener((item, pos) -> {
            Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
            i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
            i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
            i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
            i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
            startActivity(i);
        });

        new Thread(() -> {
            List<MediaItem> all = MediaScanner.scanAudio(requireContext());
            List<MediaItem> result = new ArrayList<>();
            switch (tab) {
                case "Songs":    result = all; break;
                case "Albums":   result = filterByAlbum(all); break;
                case "Artists":  result = filterByArtist(all); break;
                case "Playlists": result = new ArrayList<>(); break; // future
                case "History":  result = new ArrayList<>(); break;  // future
            }
            final List<MediaItem> finalResult = result;
            requireActivity().runOnUiThread(() -> {
                items.clear();
                items.addAll(finalResult);
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private List<MediaItem> filterByAlbum(List<MediaItem> all) {
        // One representative per album
        List<MediaItem> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (MediaItem m : all) {
            if (seen.add(m.getAlbum())) result.add(m);
        }
        return result;
    }

    private List<MediaItem> filterByArtist(List<MediaItem> all) {
        List<MediaItem> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (MediaItem m : all) {
            if (seen.add(m.getArtist())) result.add(m);
        }
        return result;
    }
}
EOF

echo ""
echo "✅ PART B DONE — run fp_v2C.sh next"
