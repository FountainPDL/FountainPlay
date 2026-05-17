#!/bin/bash
# ── v3 PART B: Redesigned Audio Player ──
# Run from ~/FountainPlay

P="app/src/main/java/com/fountainpdl/fountainplay"

# ════════════════════════════════════════════════════════════
# 1. AUDIO PLAYER LAYOUT — ViewPager2 (Song / Lyrics / Queue)
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/activity_audio_player.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/player_bg_dark">

    <!-- Dynamic blurred background art -->
    <ImageView android:id="@+id/iv_bg_blur"
        android:layout_width="match_parent" android:layout_height="match_parent"
        android:scaleType="centerCrop" android:alpha="0.18"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />

    <View android:layout_width="match_parent" android:layout_height="match_parent"
        android:background="@drawable/gradient_player_overlay"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />

    <!-- Top bar -->
    <LinearLayout android:id="@+id/top_bar"
        android:layout_width="match_parent" android:layout_height="56dp"
        android:orientation="horizontal" android:gravity="center_vertical"
        android:paddingHorizontal="4dp"
        app:layout_constraintTop_toTopOf="parent">

        <ImageButton android:id="@+id/btn_back"
            android:layout_width="48dp" android:layout_height="48dp"
            android:src="@android:drawable/ic_media_previous"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />

        <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:orientation="vertical" android:gravity="center">
            <TextView android:id="@+id/tv_queue_info"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="Now Playing" android:textColor="@color/white"
                android:textSize="11sp" android:alpha="0.6" />
        </LinearLayout>

        <ImageButton android:id="@+id/btn_options"
            android:layout_width="48dp" android:layout_height="48dp"
            android:src="@android:drawable/ic_menu_more"
            android:tint="@color/white"
            android:background="?attr/selectableItemBackgroundBorderless" />
    </LinearLayout>

    <!-- ViewPager2 for pages -->
    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/view_pager"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/top_bar"
        app:layout_constraintBottom_toTopOf="@id/page_indicator"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- Page dots indicator -->
    <LinearLayout android:id="@+id/page_indicator"
        android:layout_width="wrap_content" android:layout_height="24dp"
        android:orientation="horizontal" android:gravity="center"
        app:layout_constraintBottom_toTopOf="@id/bottom_controls"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">
        <View android:id="@+id/dot0" android:layout_width="6dp" android:layout_height="6dp"
            android:layout_margin="3dp" android:background="@color/fp_purple" />
        <View android:id="@+id/dot1" android:layout_width="6dp" android:layout_height="6dp"
            android:layout_margin="3dp" android:background="@color/on_surface_variant_dark" />
        <View android:id="@+id/dot2" android:layout_width="6dp" android:layout_height="6dp"
            android:layout_margin="3dp" android:background="@color/on_surface_variant_dark" />
    </LinearLayout>

    <!-- BOTTOM CONTROLS — always visible -->
    <LinearLayout android:id="@+id/bottom_controls"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingHorizontal="24dp" android:paddingBottom="24dp"
        app:layout_constraintBottom_toBottomOf="parent">

        <!-- Song info -->
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:gravity="center_vertical"
            android:paddingBottom="12dp">
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
                android:layout_weight="1" android:orientation="vertical">
                <TextView android:id="@+id/tv_title"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:textColor="@color/white" android:textSize="20sp" android:textStyle="bold"
                    android:maxLines="1" android:ellipsize="marquee"
                    android:marqueeRepeatLimit="marquee_forever" android:focusable="true" />
                <TextView android:id="@+id/tv_artist"
                    android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:textColor="@color/on_surface_variant_dark" android:textSize="14sp"
                    android:maxLines="1" android:ellipsize="end" />
            </LinearLayout>
            <ImageButton android:id="@+id/btn_favorite"
                android:layout_width="40dp" android:layout_height="40dp"
                android:src="@android:drawable/btn_star_big_off"
                android:tint="@color/on_surface_variant_dark"
                android:background="?attr/selectableItemBackgroundBorderless" />
        </LinearLayout>

        <!-- Seek bar -->
        <SeekBar android:id="@+id/seek_bar"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:progressTint="@color/fp_purple_light"
            android:thumbTint="@color/white"
            android:progressBackgroundTint="@color/surface_variant_dark"
            android:max="1000" />

        <!-- Time row -->
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:paddingTop="2dp" android:paddingBottom="8dp">
            <TextView android:id="@+id/tv_current_time"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="0:00" android:textColor="@color/on_surface_variant_dark" android:textSize="12sp" />
            <View android:layout_width="0dp" android:layout_height="1dp" android:layout_weight="1" />
            <TextView android:id="@+id/tv_total_time"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="0:00" android:textColor="@color/on_surface_variant_dark" android:textSize="12sp" />
        </LinearLayout>

        <!-- Main controls -->
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="horizontal" android:gravity="center" android:paddingBottom="8dp">

            <ImageButton android:id="@+id/btn_shuffle"
                android:layout_width="44dp" android:layout_height="44dp"
                android:src="@android:drawable/ic_menu_sort_by_size"
                android:tint="@color/on_surface_variant_dark"
                android:background="?attr/selectableItemBackgroundBorderless" />

            <ImageButton android:id="@+id/btn_prev"
                android:layout_width="52dp" android:layout_height="52dp"
                android:src="@android:drawable/ic_media_previous"
                android:tint="@color/white"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:layout_marginHorizontal="8dp" />

            <ImageButton android:id="@+id/btn_play_pause"
                android:layout_width="68dp" android:layout_height="68dp"
                android:src="@android:drawable/ic_media_pause"
                android:tint="@color/white"
                android:background="@drawable/bg_play_button"
                android:layout_marginHorizontal="8dp"
                android:padding="14dp" />

            <ImageButton android:id="@+id/btn_next"
                android:layout_width="52dp" android:layout_height="52dp"
                android:src="@android:drawable/ic_media_next"
                android:tint="@color/white"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:layout_marginHorizontal="8dp" />

            <ImageButton android:id="@+id/btn_repeat"
                android:layout_width="44dp" android:layout_height="44dp"
                android:src="@android:drawable/ic_menu_rotate"
                android:tint="@color/on_surface_variant_dark"
                android:background="?attr/selectableItemBackgroundBorderless" />
        </LinearLayout>

        <!-- Action chips -->
        <com.google.android.material.chip.ChipGroup
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:layout_gravity="center">
            <com.google.android.material.chip.Chip android:id="@+id/chip_speed"
                android:layout_width="wrap_content" android:layout_height="32dp"
                android:text="1.0×" android:textColor="@color/white"
                style="@style/Widget.Material3.Chip.Filter" />
            <com.google.android.material.chip.Chip android:id="@+id/chip_sleep"
                android:layout_width="wrap_content" android:layout_height="32dp"
                android:text="Sleep" android:textColor="@color/white"
                style="@style/Widget.Material3.Chip.Filter" />
            <com.google.android.material.chip.Chip android:id="@+id/chip_share"
                android:layout_width="wrap_content" android:layout_height="32dp"
                android:text="Share" android:textColor="@color/white"
                style="@style/Widget.Material3.Chip.Filter" />
        </com.google.android.material.chip.ChipGroup>
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
EOF

# ════════════════════════════════════════════════════════════
# 2. PAGE 0 — Now Playing (large art)
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_player_now_playing.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:gravity="center">

    <androidx.cardview.widget.CardView
        android:layout_width="280dp" android:layout_height="280dp"
        android:layout_gravity="center"
        app:cardCornerRadius="20dp" app:cardElevation="32dp">
        <ImageView android:id="@+id/iv_album_art"
            android:layout_width="match_parent" android:layout_height="match_parent"
            android:scaleType="centerCrop"
            android:src="@drawable/bg_play_button" />
    </androidx.cardview.widget.CardView>
</FrameLayout>
EOF

# ════════════════════════════════════════════════════════════
# 3. PAGE 1 — Lyrics
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_player_lyrics.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:padding="24dp">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" android:gravity="center">
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="Lyrics" android:textColor="@color/white"
            android:textSize="18sp" android:textStyle="bold"
            android:gravity="center" android:paddingBottom="16dp" />
        <TextView android:id="@+id/tv_lyrics"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="No lyrics available.\n\nLyrics sync coming soon."
            android:textColor="@color/on_surface_variant_dark"
            android:textSize="15sp" android:lineSpacingMultiplier="1.6"
            android:gravity="center" />
    </LinearLayout>
</ScrollView>
EOF

# ════════════════════════════════════════════════════════════
# 4. PAGE 2 — Queue
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_player_queue.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical" android:padding="8dp">

    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:gravity="center_vertical"
        android:paddingHorizontal="8dp" android:paddingBottom="8dp">
        <TextView android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:text="Up Next"
            android:textColor="@color/white" android:textSize="18sp" android:textStyle="bold" />
        <TextView android:id="@+id/tv_queue_count"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
    </LinearLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_queue"
        android:layout_width="match_parent" android:layout_height="0dp"
        android:layout_weight="1" android:clipToPadding="false" />
</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# 5. QUEUE ITEM layout
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/item_queue.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="56dp"
    android:orientation="horizontal" android:gravity="center_vertical"
    android:paddingHorizontal="8dp"
    android:background="?attr/selectableItemBackground"
    android:clickable="true" android:focusable="true">

    <TextView android:id="@+id/tv_queue_num"
        android:layout_width="28dp" android:layout_height="wrap_content"
        android:textColor="@color/on_surface_variant_dark" android:textSize="12sp"
        android:gravity="center" />

    <ImageView android:id="@+id/iv_queue_art"
        android:layout_width="38dp" android:layout_height="38dp"
        android:scaleType="centerCrop" android:background="@color/fp_purple_dark"
        android:layout_marginHorizontal="8dp" />

    <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
        android:layout_weight="1" android:orientation="vertical">
        <TextView android:id="@+id/tv_queue_title"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:textColor="@color/white" android:textSize="13sp" android:textStyle="bold"
            android:maxLines="1" android:ellipsize="end" />
        <TextView android:id="@+id/tv_queue_artist"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:textColor="@color/on_surface_variant_dark" android:textSize="11sp"
            android:maxLines="1" android:ellipsize="end" />
    </LinearLayout>

    <TextView android:id="@+id/tv_queue_dur"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:textColor="@color/on_surface_variant_dark" android:textSize="11sp"
        android:paddingEnd="8dp" />

    <ImageView android:id="@+id/iv_now_playing_indicator"
        android:layout_width="18dp" android:layout_height="18dp"
        android:src="@android:drawable/ic_media_play"
        android:tint="@color/fp_purple_light"
        android:visibility="gone" />
</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# 6. FULL AudioPlayerActivity — service-bound, all buttons work
# ════════════════════════════════════════════════════════════
cat > $P/player/AudioPlayerActivity.java << 'EOF'
package com.fountainpdl.fountainplay.player;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.service.PlaybackService;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.PlayQueue;
import com.fountainpdl.fountainplay.util.PlaybackState;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class AudioPlayerActivity extends AppCompatActivity
    implements PlaybackState.Listener, PlayQueue.Listener {

    public static final String EXTRA_URI       = "media_uri";
    public static final String EXTRA_TITLE     = "media_title";
    public static final String EXTRA_ARTIST    = "media_artist";
    public static final String EXTRA_ALBUM_ART = "album_art_uri";
    public static final String EXTRA_RESUME    = "resume_playback";

    private PlaybackService service;
    private boolean bound = false;
    private AppPreferences prefs;

    // Views
    private ViewPager2 viewPager;
    private ImageView ivBgBlur;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime, tvQueueInfo;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnBack, btnShuffle, btnRepeat, btnFavorite;
    private Chip chipSpeed, chipSleep, chipShare;
    private View dot0, dot1, dot2;

    // Queue page
    private QueueAdapter queueAdapter;
    private boolean isUserSeeking = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder b) {
            service = ((PlaybackService.LocalBinder) b).getService();
            bound = true;
            // Start playing if new track
            String uriStr = getIntent().getStringExtra(EXTRA_URI);
            boolean resume = getIntent().getBooleanExtra(EXTRA_RESUME, false);
            if (uriStr != null && !resume) {
                // Build queue from PlayQueue and play
                MediaItem cur = PlayQueue.get().current();
                if (cur != null && cur.getPath().equals(uriStr)) {
                    service.playItem(cur);
                } else {
                    // Single file open
                    MediaItem item = new MediaItem(0,
                        getIntent().getStringExtra(EXTRA_TITLE),
                        getIntent().getStringExtra(EXTRA_ARTIST),
                        uriStr, 0, MediaItem.TYPE_AUDIO);
                    item.setAlbumArtUri(getIntent().getStringExtra(EXTRA_ALBUM_ART));
                    service.playItem(item);
                }
            }
            syncUI();
        }
        @Override public void onServiceDisconnected(ComponentName name) { bound = false; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_audio_player);
        prefs = new AppPreferences(this);

        bindViews();
        setupViewPager();
        setupControls();

        PlaybackState.get().addListener(this);
        PlayQueue.get().addListener(this);

        // Bind to service
        Intent svc = new Intent(this, PlaybackService.class);
        startService(svc);
        bindService(svc, connection, BIND_AUTO_CREATE);
    }

    private void bindViews() {
        viewPager      = findViewById(R.id.view_pager);
        ivBgBlur       = findViewById(R.id.iv_bg_blur);
        tvTitle        = findViewById(R.id.tv_title);
        tvArtist       = findViewById(R.id.tv_artist);
        tvCurrentTime  = findViewById(R.id.tv_current_time);
        tvTotalTime    = findViewById(R.id.tv_total_time);
        tvQueueInfo    = findViewById(R.id.tv_queue_info);
        seekBar        = findViewById(R.id.seek_bar);
        btnPlayPause   = findViewById(R.id.btn_play_pause);
        btnPrev        = findViewById(R.id.btn_prev);
        btnNext        = findViewById(R.id.btn_next);
        btnBack        = findViewById(R.id.btn_back);
        btnShuffle     = findViewById(R.id.btn_shuffle);
        btnRepeat      = findViewById(R.id.btn_repeat);
        btnFavorite    = findViewById(R.id.btn_favorite);
        chipSpeed      = findViewById(R.id.chip_speed);
        chipSleep      = findViewById(R.id.chip_sleep);
        chipShare      = findViewById(R.id.chip_share);
        dot0           = findViewById(R.id.dot0);
        dot1           = findViewById(R.id.dot1);
        dot2           = findViewById(R.id.dot2);
    }

    private void setupViewPager() {
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int pos) {
                switch (pos) {
                    case 1: return new LyricsPageFragment();
                    case 2: return new QueuePageFragment();
                    default: return new NowPlayingPageFragment();
                }
            }
            @Override public int getItemCount() { return 3; }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) {
                updateDots(pos);
            }
        });

        viewPager.setPageTransformer((page, position) -> {
            page.setAlpha(1 - Math.abs(position) * 0.4f);
            page.setScaleX(1 - Math.abs(position) * 0.05f);
            page.setScaleY(1 - Math.abs(position) * 0.05f);
        });
    }

    private void updateDots(int pos) {
        int active = 0xFFFFFFFF, inactive = 0x80A89BC2;
        dot0.setBackgroundColor(pos == 0 ? active : inactive);
        dot1.setBackgroundColor(pos == 1 ? active : inactive);
        dot2.setBackgroundColor(pos == 2 ? active : inactive);
    }

    private void setupControls() {
        btnBack.setOnClickListener(v -> finish());

        btnPlayPause.setOnClickListener(v -> {
            if (bound) {
                service.playPause();
                animatePulse(btnPlayPause);
            }
        });

        btnNext.setOnClickListener(v -> { if (bound) { service.skipNext(); animateSlide(true); } });
        btnPrev.setOnClickListener(v -> { if (bound) { service.skipPrevious(); animateSlide(false); } });

        btnShuffle.setOnClickListener(v -> {
            PlayQueue.get().setShuffle(!PlayQueue.get().isShuffle());
            updateShuffleUI();
        });

        btnRepeat.setOnClickListener(v -> {
            PlayQueue.get().cycleRepeat();
            updateRepeatUI();
        });

        chipSpeed.setOnClickListener(v -> {
            if (!bound) return;
            String[] opts = {"0.5×","0.75×","1.0×","1.25×","1.5×","1.75×","2.0×"};
            float[] vals = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
            float cur = service.getPlayer().getPlaybackParameters().speed;
            int sel = 2;
            for (int i = 0; i < vals.length; i++) if (Math.abs(vals[i] - cur) < 0.01f) { sel = i; break; }
            final int[] picked = {sel};
            new AlertDialog.Builder(this)
                .setTitle("Playback Speed")
                .setSingleChoiceItems(opts, sel, (d, i) -> picked[0] = i)
                .setPositiveButton("OK", (d, i) -> {
                    service.setPlaybackSpeed(vals[picked[0]]);
                    chipSpeed.setText(opts[picked[0]]);
                    prefs.setPlaybackSpeed(vals[picked[0]]);
                }).show();
        });

        chipSleep.setOnClickListener(v -> showSleepTimerDialog());

        chipShare.setOnClickListener(v -> {
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur == null) return;
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("audio/*");
            share.putExtra(Intent.EXTRA_STREAM, Uri.parse(cur.getPath()));
            share.putExtra(Intent.EXTRA_TEXT, cur.getTitle() + " - " + cur.getArtist());
            startActivity(Intent.createChooser(share, "Share"));
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int prog, boolean fromUser) {
                if (fromUser && bound && service.getDuration() > 0) {
                    long pos = (long)(prog / 1000f * service.getDuration());
                    tvCurrentTime.setText(fmt(pos));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                if (bound && service.getDuration() > 0) {
                    long pos = (long)(sb.getProgress() / 1000f * service.getDuration());
                    service.seekTo(pos);
                }
            }
        });
    }

    private void showSleepTimerDialog() {
        String[] opts = {"5 min","10 min","15 min","30 min","45 min","1 hour","Cancel"};
        int[] mins = {5, 10, 15, 30, 45, 60, -1};
        new AlertDialog.Builder(this).setTitle("Sleep Timer")
            .setItems(opts, (d, i) -> {
                if (mins[i] > 0) {
                    chipSleep.setText(opts[i]);
                    new android.os.Handler().postDelayed(() -> {
                        if (bound) service.getPlayer().pause();
                        chipSleep.setText("Sleep");
                    }, mins[i] * 60 * 1000L);
                } else chipSleep.setText("Sleep");
            }).show();
    }

    private void syncUI() {
        MediaItem cur = PlaybackState.get().getCurrentItem();
        if (cur != null) updateTrackUI(cur);
        boolean playing = bound && service.isPlaying();
        btnPlayPause.setImageResource(playing
            ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        updateShuffleUI();
        updateRepeatUI();
        if (bound && service.getDuration() > 0) {
            tvTotalTime.setText(fmt(service.getDuration()));
        }
        updateQueueInfo();
    }

    private void updateTrackUI(MediaItem item) {
        tvTitle.setText(item.getTitle());
        tvArtist.setText(item.getArtist());
        tvTitle.setSelected(true);
        String art = item.getAlbumArtUri();
        Glide.with(this).load(art).centerCrop()
            .placeholder(R.drawable.bg_play_button).into(ivBgBlur);
        // Notify NowPlayingPageFragment
        Fragment f = getSupportFragmentManager().findFragmentByTag("f0");
        if (f instanceof NowPlayingPageFragment) ((NowPlayingPageFragment) f).updateArt(art);
    }

    private void updateShuffleUI() {
        btnShuffle.setAlpha(PlayQueue.get().isShuffle() ? 1.0f : 0.5f);
        btnShuffle.setColorFilter(PlayQueue.get().isShuffle() ? 0xFFBB86FC : 0xFFA89BC2);
    }

    private void updateRepeatUI() {
        int rm = PlayQueue.get().getRepeatMode();
        btnRepeat.setAlpha(rm != PlayQueue.REPEAT_NONE ? 1.0f : 0.5f);
    }

    private void updateQueueInfo() {
        int idx = PlayQueue.get().getIndex();
        int total = PlayQueue.get().size();
        if (total > 1) tvQueueInfo.setText((idx+1) + " of " + total);
        else tvQueueInfo.setText("Now Playing");
    }

    // ── PlaybackState.Listener ──
    @Override public void onItemChanged(MediaItem item) { runOnUiThread(() -> { if (item != null) updateTrackUI(item); }); }
    @Override public void onPlayStateChanged(boolean playing) {
        runOnUiThread(() -> btnPlayPause.setImageResource(
            playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play));
    }
    @Override public void onPositionChanged(long pos, long dur) {
        runOnUiThread(() -> {
            if (!isUserSeeking && dur > 0) {
                seekBar.setProgress((int)(pos * 1000 / dur));
                tvCurrentTime.setText(fmt(pos));
                tvTotalTime.setText(fmt(dur));
            }
        });
    }

    // ── PlayQueue.Listener ──
    @Override public void onQueueChanged() { runOnUiThread(this::updateQueueInfo); }
    @Override public void onTrackChanged(MediaItem item, int idx) { runOnUiThread(() -> { updateTrackUI(item); updateQueueInfo(); }); }

    // ── Animations ──
    private void animatePulse(View v) {
        ScaleAnimation anim = new ScaleAnimation(1f,1.2f,1f,1.2f,
            Animation.RELATIVE_TO_SELF,.5f, Animation.RELATIVE_TO_SELF,.5f);
        anim.setDuration(100); anim.setRepeatCount(1); anim.setRepeatMode(Animation.REVERSE);
        v.startAnimation(anim);
    }
    private void animateSlide(boolean forward) {
        Fragment f = getSupportFragmentManager().findFragmentByTag("f0");
        if (f != null && f.getView() != null) {
            float from = forward ? 0 : 0, to = forward ? -30 : 30;
            f.getView().animate().translationX(to).alpha(0.5f).setDuration(100)
                .withEndAction(() -> f.getView().animate().translationX(0).alpha(1f).setDuration(100).start()).start();
        }
    }

    private String fmt(long ms) {
        if (ms <= 0) return "0:00";
        long s = ms/1000, m = s/60, h = m/60; s%=60; m%=60;
        return h > 0 ? String.format("%d:%02d:%02d",h,m,s) : String.format("%d:%02d",m,s);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        PlaybackState.get().removeListener(this);
        PlayQueue.get().removeListener(this);
        if (bound) { unbindService(connection); bound = false; }
    }

    // ─── Inner Fragment classes ───────────────────────────

    public static class NowPlayingPageFragment extends Fragment {
        private ImageView albumArt;
        @Override public View onCreateView(@NonNull android.view.LayoutInflater inf,
                android.view.ViewGroup c, Bundle s) {
            View v = inf.inflate(R.layout.fragment_player_now_playing, c, false);
            albumArt = v.findViewById(R.id.iv_album_art);
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null && cur.getAlbumArtUri() != null) {
                Glide.with(requireContext()).load(cur.getAlbumArtUri()).centerCrop().into(albumArt);
            }
            return v;
        }
        public void updateArt(String url) {
            if (albumArt != null && url != null) {
                albumArt.animate().alpha(0).setDuration(150).withEndAction(() -> {
                    Glide.with(requireContext()).load(url).centerCrop().into(albumArt);
                    albumArt.animate().alpha(1).setDuration(150).start();
                }).start();
            }
        }
    }

    public static class LyricsPageFragment extends Fragment {
        @Override public View onCreateView(@NonNull android.view.LayoutInflater inf,
                android.view.ViewGroup c, Bundle s) {
            return inf.inflate(R.layout.fragment_player_lyrics, c, false);
        }
    }

    public static class QueuePageFragment extends Fragment {
        @Override public View onCreateView(@NonNull android.view.LayoutInflater inf,
                android.view.ViewGroup c, Bundle s) {
            View v = inf.inflate(R.layout.fragment_player_queue, c, false);
            RecyclerView rv = v.findViewById(R.id.rv_queue);
            TextView tvCount = v.findViewById(R.id.tv_queue_count);
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));

            List<MediaItem> queue = new ArrayList<>(PlayQueue.get().getQueue());
            int curIdx = PlayQueue.get().getIndex();
            tvCount.setText(queue.size() + " songs");

            QueueAdapter adapter = new QueueAdapter(queue, curIdx, item -> {
                // Jump to tapped queue item
                int idx = queue.indexOf(item);
                if (idx >= 0) {
                    // Navigate PlayQueue to that index
                    for (int i = PlayQueue.get().getIndex(); i < idx; i++) PlayQueue.get().next();
                }
            });
            rv.setAdapter(adapter);
            rv.scrollToPosition(Math.max(0, curIdx - 2));
            return v;
        }
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 7. QueueAdapter
# ════════════════════════════════════════════════════════════
cat > $P/adapter/QueueAdapter.java << 'EOF'
package com.fountainpdl.fountainplay.adapter;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.VH> {
    public interface OnClick { void onClick(MediaItem item); }
    private final List<MediaItem> items;
    private final int currentIndex;
    private final OnClick click;

    public QueueAdapter(List<MediaItem> items, int cur, OnClick click) {
        this.items = items; this.currentIndex = cur; this.click = click;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_queue, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        MediaItem item = items.get(pos);
        h.num.setText(String.valueOf(pos + 1));
        h.title.setText(item.getTitle());
        h.artist.setText(item.getArtist());
        h.dur.setText(item.getFormattedDuration());
        h.indicator.setVisibility(pos == currentIndex ? View.VISIBLE : View.GONE);
        h.num.setVisibility(pos == currentIndex ? View.GONE : View.VISIBLE);
        if (item.getAlbumArtUri() != null)
            Glide.with(h.art.getContext()).load(item.getAlbumArtUri()).centerCrop().into(h.art);
        h.itemView.setAlpha(pos < currentIndex ? 0.5f : 1.0f);
        h.itemView.setOnClickListener(v -> click.onClick(item));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView num, title, artist, dur;
        ImageView art, indicator;
        VH(View v) {
            super(v);
            num = v.findViewById(R.id.tv_queue_num);
            title = v.findViewById(R.id.tv_queue_title);
            artist = v.findViewById(R.id.tv_queue_artist);
            dur = v.findViewById(R.id.tv_queue_dur);
            art = v.findViewById(R.id.iv_queue_art);
            indicator = v.findViewById(R.id.iv_now_playing_indicator);
        }
    }
}
EOF

echo ""
echo "✅ PART B DONE — run fp_v3C.sh next"
