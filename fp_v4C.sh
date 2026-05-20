#!/bin/bash
# ── v4 PART C: Video Player (full features), VideoFragment, Library (music+video) ──
# Run from ~/FountainPlay
set -e
P="app/src/main/java/com/fountainpdl/fountainplay"

echo "════════════════ v4 Part C ════════════════"

# ════════════════════════════════════════════════════════════
# 1. VIDEO PLAYER LAYOUT — full controls overlay
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/activity_video_player.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <!-- ExoPlayer surface -->
    <androidx.media3.ui.PlayerView
        android:id="@+id/player_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:show_timeout="3000"
        app:resize_mode="fit"
        app:use_controller="false" />

    <!-- Custom overlay (shows/hides on tap) -->
    <LinearLayout
        android:id="@+id/controls_overlay"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:background="#66000000"
        android:visibility="visible">

        <!-- Top bar -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="56dp"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:paddingHorizontal="8dp">

            <ImageButton android:id="@+id/btn_back"
                android:layout_width="44dp" android:layout_height="44dp"
                android:src="@android:drawable/ic_media_previous"
                android:tint="#FFFFFF"
                android:background="?attr/selectableItemBackgroundBorderless" />

            <TextView android:id="@+id/tv_video_title"
                android:layout_width="0dp" android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textColor="#FFFFFF" android:textSize="15sp" android:textStyle="bold"
                android:maxLines="1" android:ellipsize="end"
                android:paddingHorizontal="8dp" />

            <ImageButton android:id="@+id/btn_pip"
                android:layout_width="40dp" android:layout_height="40dp"
                android:src="@android:drawable/ic_menu_view"
                android:tint="#FFFFFF"
                android:background="?attr/selectableItemBackgroundBorderless" />

            <ImageButton android:id="@+id/btn_lock"
                android:layout_width="40dp" android:layout_height="40dp"
                android:src="@android:drawable/ic_lock_idle_lock"
                android:tint="#FFFFFF"
                android:background="?attr/selectableItemBackgroundBorderless" />

            <ImageButton android:id="@+id/btn_more"
                android:layout_width="40dp" android:layout_height="40dp"
                android:src="@android:drawable/ic_menu_more"
                android:tint="#FFFFFF"
                android:background="?attr/selectableItemBackgroundBorderless" />
        </LinearLayout>

        <!-- Spacer -->
        <View android:layout_width="match_parent" android:layout_height="0dp"
            android:layout_weight="1" />

        <!-- Centre controls -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center"
            android:paddingBottom="8dp">

            <ImageButton android:id="@+id/btn_prev_video"
                android:layout_width="52dp" android:layout_height="52dp"
                android:src="@android:drawable/ic_media_previous"
                android:tint="#FFFFFF"
                android:background="?attr/selectableItemBackgroundBorderless" />

            <ImageButton android:id="@+id/btn_rew"
                android:layout_width="52dp" android:layout_height="52dp"
                android:src="@android:drawable/ic_media_rew"
                android:tint="#FFFFFF"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:layout_marginHorizontal="8dp" />

            <ImageButton android:id="@+id/btn_play_video"
                android:layout_width="64dp" android:layout_height="64dp"
                android:src="@android:drawable/ic_media_pause"
                android:tint="#FFFFFF"
                android:background="@drawable/bg_play_button"
                android:padding="14dp"
                android:layout_marginHorizontal="8dp" />

            <ImageButton android:id="@+id/btn_ff"
                android:layout_width="52dp" android:layout_height="52dp"
                android:src="@android:drawable/ic_media_ff"
                android:tint="#FFFFFF"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:layout_marginHorizontal="8dp" />

            <ImageButton android:id="@+id/btn_next_video"
                android:layout_width="52dp" android:layout_height="52dp"
                android:src="@android:drawable/ic_media_next"
                android:tint="#FFFFFF"
                android:background="?attr/selectableItemBackgroundBorderless" />
        </LinearLayout>

        <!-- Bottom bar: seek + time -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingHorizontal="12dp"
            android:paddingBottom="12dp">

            <!-- Speed / Ratio / Subtitle / Audio chips -->
            <HorizontalScrollView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:scrollbars="none"
                android:paddingBottom="6dp">
                <com.google.android.material.chip.ChipGroup
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content">
                    <com.google.android.material.chip.Chip android:id="@+id/chip_speed_video"
                        android:layout_width="wrap_content" android:layout_height="32dp"
                        android:text="1.0×" android:textColor="#FFFFFF"
                        style="@style/Widget.Material3.Chip.Filter" />
                    <com.google.android.material.chip.Chip android:id="@+id/chip_ratio"
                        android:layout_width="wrap_content" android:layout_height="32dp"
                        android:text="Fit" android:textColor="#FFFFFF"
                        style="@style/Widget.Material3.Chip.Filter" />
                    <com.google.android.material.chip.Chip android:id="@+id/chip_subtitle"
                        android:layout_width="wrap_content" android:layout_height="32dp"
                        android:text="Subtitle" android:textColor="#FFFFFF"
                        style="@style/Widget.Material3.Chip.Filter" />
                    <com.google.android.material.chip.Chip android:id="@+id/chip_audio_track"
                        android:layout_width="wrap_content" android:layout_height="32dp"
                        android:text="Audio" android:textColor="#FFFFFF"
                        style="@style/Widget.Material3.Chip.Filter" />
                    <com.google.android.material.chip.Chip android:id="@+id/chip_sleep_video"
                        android:layout_width="wrap_content" android:layout_height="32dp"
                        android:text="Sleep" android:textColor="#FFFFFF"
                        style="@style/Widget.Material3.Chip.Filter" />
                </com.google.android.material.chip.ChipGroup>
            </HorizontalScrollView>

            <!-- Seek bar -->
            <SeekBar android:id="@+id/video_seek_bar"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:progressTint="#BB86FC"
                android:thumbTint="#FFFFFF"
                android:max="1000" />

            <!-- Time row -->
            <LinearLayout android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:paddingTop="2dp">
                <TextView android:id="@+id/tv_pos"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="0:00" android:textColor="#CCFFFFFF" android:textSize="12sp" />
                <View android:layout_width="0dp" android:layout_height="1dp" android:layout_weight="1"/>
                <TextView android:id="@+id/tv_dur"
                    android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="0:00" android:textColor="#CCFFFFFF" android:textSize="12sp" />
            </LinearLayout>
        </LinearLayout>
    </LinearLayout>

    <!-- Gesture feedback: left = brightness -->
    <LinearLayout android:id="@+id/overlay_left"
        android:layout_width="72dp" android:layout_height="match_parent"
        android:layout_gravity="start" android:gravity="center"
        android:orientation="vertical" android:visibility="invisible"
        android:background="#55000000">
        <TextView android:id="@+id/tv_brightness_label"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="☀" android:textSize="20sp" android:textColor="#FFFFFF" />
        <TextView android:id="@+id/tv_brightness_val"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="50%" android:textColor="#FFFFFF" android:textSize="12sp" />
    </LinearLayout>

    <!-- Gesture feedback: right = volume -->
    <LinearLayout android:id="@+id/overlay_right"
        android:layout_width="72dp" android:layout_height="match_parent"
        android:layout_gravity="end" android:gravity="center"
        android:orientation="vertical" android:visibility="invisible"
        android:background="#55000000">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="🔊" android:textSize="20sp" android:textColor="#FFFFFF" />
        <TextView android:id="@+id/tv_volume_val"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="50%" android:textColor="#FFFFFF" android:textSize="12sp" />
    </LinearLayout>

    <!-- Seek feedback: centre -->
    <TextView android:id="@+id/tv_seek_indicator"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:textColor="#FFFFFF" android:textSize="18sp" android:textStyle="bold"
        android:background="#99000000"
        android:paddingHorizontal="16dp" android:paddingVertical="8dp"
        android:visibility="invisible" />

</FrameLayout>
EOF

# ════════════════════════════════════════════════════════════
# 2. VideoPlayerActivity — full implementation
# ════════════════════════════════════════════════════════════
cat > $P/player/VideoPlayerActivity.java << 'EOF'
package com.fountainpdl.fountainplay.player;

import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Rational;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import com.fountainpdl.fountainplay.BaseActivity;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.google.android.material.chip.Chip;

public class VideoPlayerActivity extends BaseActivity {

    public static final String EXTRA_URI   = "media_uri";
    public static final String EXTRA_TITLE = "media_title";

    private ExoPlayer player;
    private PlayerView playerView;
    private AppPreferences prefs;
    private AudioManager audio;
    private Handler handler = new Handler(Looper.getMainLooper());

    // Controls
    private View controlsOverlay;
    private ImageButton btnBack, btnPlay, btnRew, btnFf, btnLock, btnPip, btnMore;
    private ImageButton btnPrevVideo, btnNextVideo;
    private SeekBar seekBar;
    private TextView tvPos, tvDur, tvTitle, tvSeekIndicator;
    private View overlayLeft, overlayRight;
    private TextView tvBrightness, tvVolume;
    private Chip chipSpeed, chipRatio, chipSubtitle, chipAudio, chipSleep;

    // State
    private boolean isLocked = false;
    private boolean controlsVisible = true;
    private boolean isUserSeeking = false;
    private boolean gesturesEnabled = true;

    // Gesture tracking
    private float touchStartX, touchStartY;
    private long seekStart;
    private int startVolume, startBrightness;
    private int activeGesture = -1; // 0=seek 1=vol 2=bright
    private static final int G_SEEK = 0, G_VOL = 1, G_BRIGHT = 2;

    // Aspect ratio cycling
    private int[] ratioModes = {
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_FILL,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
    };
    private String[] ratioLabels = {"Fit","Fill","Zoom","Fixed W"};
    private int ratioIndex = 0;

    private final Runnable hideControls = () -> {
        if (!isLocked) setControlsVisible(false);
    };

    private final Runnable progressUpdater = new Runnable() {
        @Override public void run() {
            if (player != null && !isUserSeeking) {
                long pos = player.getCurrentPosition();
                long dur = player.getDuration();
                if (dur > 0) {
                    seekBar.setProgress((int)(pos * 1000 / dur));
                    tvPos.setText(fmt(pos));
                    tvDur.setText(fmt(dur));
                }
            }
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_player);

        prefs = new AppPreferences(this);
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        gesturesEnabled = prefs.getGesturesEnabled();

        bindViews();
        setupPlayer();
        setupControls();
        if (gesturesEnabled) setupGestures();

        handler.post(progressUpdater);
        scheduleHideControls();
    }

    private void bindViews() {
        playerView      = findViewById(R.id.player_view);
        controlsOverlay = findViewById(R.id.controls_overlay);
        btnBack         = findViewById(R.id.btn_back);
        btnPlay         = findViewById(R.id.btn_play_video);
        btnRew          = findViewById(R.id.btn_rew);
        btnFf           = findViewById(R.id.btn_ff);
        btnLock         = findViewById(R.id.btn_lock);
        btnPip          = findViewById(R.id.btn_pip);
        btnMore         = findViewById(R.id.btn_more);
        btnPrevVideo    = findViewById(R.id.btn_prev_video);
        btnNextVideo    = findViewById(R.id.btn_next_video);
        seekBar         = findViewById(R.id.video_seek_bar);
        tvPos           = findViewById(R.id.tv_pos);
        tvDur           = findViewById(R.id.tv_dur);
        tvTitle         = findViewById(R.id.tv_video_title);
        tvSeekIndicator = findViewById(R.id.tv_seek_indicator);
        overlayLeft     = findViewById(R.id.overlay_left);
        overlayRight    = findViewById(R.id.overlay_right);
        tvBrightness    = findViewById(R.id.tv_brightness_val);
        tvVolume        = findViewById(R.id.tv_volume_val);
        chipSpeed       = findViewById(R.id.chip_speed_video);
        chipRatio       = findViewById(R.id.chip_ratio);
        chipSubtitle    = findViewById(R.id.chip_subtitle);
        chipAudio       = findViewById(R.id.chip_audio_track);
        chipSleep       = findViewById(R.id.chip_sleep_video);
    }

    private void setupPlayer() {
        Uri uri;
        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        if (uriStr != null) uri = Uri.parse(uriStr);
        else if (getIntent().getData() != null) uri = getIntent().getData();
        else { finish(); return; }

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (tvTitle != null && title != null) tvTitle.setText(title);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();
        player.setPlayWhenReady(true);
        player.setPlaybackSpeed(prefs.getPlaybackSpeed());

        player.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean playing) {
                btnPlay.setImageResource(playing
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play);
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) tvDur.setText(fmt(player.getDuration()));
            }
        });
    }

    private void setupControls() {
        btnBack.setOnClickListener(v -> finish());

        btnPlay.setOnClickListener(v -> {
            if (player.isPlaying()) player.pause(); else player.play();
            scheduleHideControls();
        });

        int skip = prefs.getSkipInterval() * 1000;
        btnRew.setOnClickListener(v -> {
            player.seekTo(Math.max(0, player.getCurrentPosition() - skip));
            scheduleHideControls();
        });
        btnFf.setOnClickListener(v -> {
            player.seekTo(Math.min(player.getDuration(), player.getCurrentPosition() + skip));
            scheduleHideControls();
        });

        btnPrevVideo.setOnClickListener(v -> {
            // If in a queue context, skip prev — else restart
            player.seekTo(0);
        });
        btnNextVideo.setOnClickListener(v -> finish()); // placeholder — no video queue yet

        btnLock.setOnClickListener(v -> {
            isLocked = !isLocked;
            btnLock.setImageResource(isLocked
                ? android.R.drawable.ic_lock_lock
                : android.R.drawable.ic_lock_idle_lock);
            if (isLocked) setControlsVisible(false);
        });

        btnPip.setOnClickListener(v -> enterPiP());

        btnMore.setOnClickListener(v -> showMoreMenu());

        // Speed chip
        chipSpeed.setText(prefs.getPlaybackSpeed() + "×");
        chipSpeed.setOnClickListener(v -> {
            String[] opts = {"0.25×","0.5×","0.75×","1.0×","1.25×","1.5×","2.0×","3.0×"};
            float[]  vals = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f};
            new AlertDialog.Builder(this).setTitle("Speed")
                .setItems(opts, (d, i) -> {
                    player.setPlaybackSpeed(vals[i]);
                    chipSpeed.setText(opts[i]);
                }).show();
        });

        // Aspect ratio chip
        chipRatio.setOnClickListener(v -> {
            ratioIndex = (ratioIndex + 1) % ratioModes.length;
            playerView.setResizeMode(ratioModes[ratioIndex]);
            chipRatio.setText(ratioLabels[ratioIndex]);
        });

        // Subtitle chip
        chipSubtitle.setOnClickListener(v ->
            Toast.makeText(this, "Subtitle loading coming in next update", Toast.LENGTH_SHORT).show());

        // Audio track chip
        chipAudio.setOnClickListener(v ->
            Toast.makeText(this, "Multi-track audio coming in next update", Toast.LENGTH_SHORT).show());

        // Sleep timer
        chipSleep.setOnClickListener(v -> {
            String[] opts = {"5 min","10 min","15 min","30 min","1 hour","Cancel"};
            long[] ms = {5*60000L,10*60000L,15*60000L,30*60000L,60*60000L,-1};
            new AlertDialog.Builder(this).setTitle("Sleep Timer").setItems(opts,(d,i)->{
                if (ms[i] > 0) {
                    chipSleep.setText(opts[i]);
                    handler.postDelayed(()->{ player.pause(); chipSleep.setText("Sleep"); }, ms[i]);
                }
            }).show();
        });

        // Seek bar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean user) {
                if (user && player.getDuration() > 0)
                    tvPos.setText(fmt((long)(p/1000f*player.getDuration())));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                if (player.getDuration() > 0)
                    player.seekTo((long)(sb.getProgress()/1000f*player.getDuration()));
            }
        });
    }

    private void showMoreMenu() {
        String[] opts = {"Share","File Info","Next Video","Loop"};
        new AlertDialog.Builder(this).setTitle("Options")
            .setItems(opts, (d, i) -> {
                switch (i) {
                    case 0:
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("video/*");
                        String u = getIntent().getStringExtra(EXTRA_URI);
                        if (u!=null) share.putExtra(Intent.EXTRA_STREAM, Uri.parse(u));
                        startActivity(Intent.createChooser(share,"Share"));
                        break;
                    case 3:
                        player.setRepeatMode(
                            player.getRepeatMode()==Player.REPEAT_MODE_OFF
                            ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
                        Toast.makeText(this,
                            player.getRepeatMode()==Player.REPEAT_MODE_ONE?"Loop ON":"Loop OFF",
                            Toast.LENGTH_SHORT).show();
                        break;
                }
            }).show();
    }

    private void setupGestures() {
        controlsOverlay.setOnTouchListener((v, event) -> {
            int sw = getWindow().getDecorView().getWidth();
            int sh = getWindow().getDecorView().getHeight();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartX = event.getX(); touchStartY = event.getY();
                    seekStart   = player.getCurrentPosition();
                    startVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                    startBrightness = getBrightness();
                    activeGesture = -1;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isLocked) break;
                    float dx = event.getX() - touchStartX;
                    float dy = event.getY() - touchStartY;
                    if (activeGesture == -1) {
                        if (Math.abs(dx) > Math.abs(dy)*1.5f && Math.abs(dx) > 20) activeGesture = G_SEEK;
                        else if (Math.abs(dy) > 20)
                            activeGesture = event.getX() < sw/2 ? G_BRIGHT : G_VOL;
                    }
                    if (activeGesture == G_SEEK) {
                        long delta = (long)(dx * 250);
                        long newPos = Math.max(0, Math.min(player.getDuration(), seekStart + delta));
                        String sign = delta >= 0 ? "+" : "";
                        showSeekLabel(sign + (delta/1000) + "s → " + fmt(newPos));
                        player.seekTo(newPos);
                    } else if (activeGesture == G_VOL) {
                        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int nv = Math.max(0, Math.min(max, startVolume - (int)(dy/sh*max*2)));
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, nv, 0);
                        showVolLabel((int)(nv*100f/max)+"%");
                    } else if (activeGesture == G_BRIGHT) {
                        int nb = Math.max(0, Math.min(255, startBrightness - (int)(dy/sh*255*2)));
                        setBrightness(nb);
                        showBrightLabel((int)(nb*100f/255)+"%");
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    hideGestureOverlays();
                    activeGesture = -1;
                    // Small movement = tap = toggle controls
                    float mdx = Math.abs(event.getX()-touchStartX);
                    float mdy = Math.abs(event.getY()-touchStartY);
                    if (mdx < 12 && mdy < 12) {
                        if (!isLocked) toggleControls();
                    }
                    break;
            }
            return true;
        });
    }

    private void toggleControls() {
        setControlsVisible(!controlsVisible);
        if (controlsVisible) scheduleHideControls();
    }

    private void setControlsVisible(boolean show) {
        controlsVisible = show;
        controlsOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        handler.removeCallbacks(hideControls);
    }

    private void scheduleHideControls() {
        handler.removeCallbacks(hideControls);
        handler.postDelayed(hideControls, 3500);
    }

    private void showSeekLabel(String text) {
        tvSeekIndicator.setText(text);
        tvSeekIndicator.setVisibility(View.VISIBLE);
        overlayLeft.setVisibility(View.INVISIBLE);
        overlayRight.setVisibility(View.INVISIBLE);
    }
    private void showVolLabel(String pct) {
        tvVolume.setText(pct);
        overlayRight.setVisibility(View.VISIBLE);
        overlayLeft.setVisibility(View.INVISIBLE);
        tvSeekIndicator.setVisibility(View.INVISIBLE);
    }
    private void showBrightLabel(String pct) {
        tvBrightness.setText(pct);
        overlayLeft.setVisibility(View.VISIBLE);
        overlayRight.setVisibility(View.INVISIBLE);
        tvSeekIndicator.setVisibility(View.INVISIBLE);
    }
    private void hideGestureOverlays() {
        handler.postDelayed(()->{ overlayLeft.setVisibility(View.INVISIBLE);
            overlayRight.setVisibility(View.INVISIBLE);
            tvSeekIndicator.setVisibility(View.INVISIBLE); }, 700);
    }

    private int getBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (lp.screenBrightness < 0) {
            try { return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS); }
            catch (Exception e) { return 128; }
        }
        return (int)(lp.screenBrightness * 255);
    }
    private void setBrightness(int v) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = v / 255f;
        getWindow().setAttributes(lp);
    }

    private void enterPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams p = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9)).build();
            enterPictureInPictureMode(p);
        }
    }

    @Override public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (player != null && player.isPlaying()) enterPiP();
    }

    @Override public void onPictureInPictureModeChanged(boolean inPiP,
            android.content.res.Configuration cfg) {
        super.onPictureInPictureModeChanged(inPiP, cfg);
        setControlsVisible(!inPiP);
    }

    private String fmt(long ms) {
        if (ms <= 0) return "0:00";
        long s=ms/1000, m=s/60, h=m/60; s%=60; m%=60;
        return h>0 ? String.format("%d:%02d:%02d",h,m,s) : String.format("%d:%02d",m,s);
    }

    @Override protected void onPause() {
        super.onPause();
        if (!isInPictureInPictureMode() && player != null) player.pause();
    }
    @Override protected void onResume() {
        super.onResume();
        if (!isInPictureInPictureMode() && player != null) player.play();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (player != null) { player.release(); player = null; }
    }
}
EOF
echo "✅ VideoPlayerActivity rebuilt"

# ════════════════════════════════════════════════════════════
# 3. VideoFragment — sort, search, context menu, folder filter
# ════════════════════════════════════════════════════════════
cat > $P/ui/video/VideoFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.video;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.VideoPlayerActivity;
import com.fountainpdl.fountainplay.util.*;
import java.util.*;

public class VideoFragment extends Fragment {

    private MediaAdapter adapter;
    private final List<MediaItem> videos = new ArrayList<>();
    private boolean isGrid = true;
    private RecyclerView rv;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_video, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rv = view.findViewById(R.id.rv_videos);
        TextView tvCount = view.findViewById(R.id.tv_video_count);
        SearchView sv = view.findViewById(R.id.search_view_video);

        adapter = new MediaAdapter(videos, 1);
        applyLayout();
        rv.setAdapter(adapter);

        adapter.setOnItemClickListener((item, pos) -> {
            Intent i = new Intent(requireContext(), VideoPlayerActivity.class);
            i.putExtra(VideoPlayerActivity.EXTRA_URI, item.getPath());
            i.putExtra(VideoPlayerActivity.EXTRA_TITLE, item.getTitle());
            startActivity(i);
        });

        adapter.setOnItemLongListener((item, pos, anchor) ->
            showVideoMenu(item, pos));

        view.findViewById(R.id.btn_sort_video).setOnClickListener(v ->
            showSortDialog());

        view.findViewById(R.id.btn_toggle_layout).setOnClickListener(v -> {
            isGrid = !isGrid;
            applyLayout();
            ((ImageButton)v).setImageResource(isGrid
                ? android.R.drawable.ic_menu_sort_by_size
                : android.R.drawable.ic_menu_agenda);
        });

        view.findViewById(R.id.btn_refresh_video).setOnClickListener(v ->
            loadVideos(tvCount));

        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { adapter.filter(q); return true; }
            @Override public boolean onQueryTextChange(String q) { adapter.filter(q); return true; }
        });

        loadVideos(tvCount);
    }

    private void applyLayout() {
        if (isGrid) {
            rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        } else {
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
    }

    private void loadVideos(TextView tvCount) {
        new Thread(() -> {
            AppPreferences prefs = new AppPreferences(requireContext());
            Set<String> folders  = prefs.getVideoFolders();
            List<MediaItem> result = MediaScanner.scanVideo(
                requireContext(), folders.isEmpty() ? null : folders);
            requireActivity().runOnUiThread(() -> {
                videos.clear(); videos.addAll(result);
                adapter.updateAll(result);
                tvCount.setText(videos.size() + " videos");
            });
        }).start();
    }

    private void showSortDialog() {
        String[] labels = {"Name","Date Added","Size","Duration","Folder"};
        String[] keys   = {"name","date","size","duration","folder"};
        new AlertDialog.Builder(requireContext()).setTitle("Sort by")
            .setItems(labels, (d, i) -> adapter.sort(keys[i]))
            .show();
    }

    private void showVideoMenu(MediaItem item, int pos) {
        String[] opts = {
            "▶  Play",
            "⭐  Add to Favourites",
            "↗  Share",
            "ℹ  Info",
            "🗑  Delete"
        };
        new AlertDialog.Builder(requireContext())
            .setTitle(item.getTitle())
            .setItems(opts, (d, i) -> {
                switch (i) {
                    case 0:
                        Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
                        intent.putExtra(VideoPlayerActivity.EXTRA_URI, item.getPath());
                        intent.putExtra(VideoPlayerActivity.EXTRA_TITLE, item.getTitle());
                        startActivity(intent);
                        break;
                    case 1: addVideoToFavourites(item); break;
                    case 2:
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("video/*");
                        share.putExtra(Intent.EXTRA_STREAM, Uri.parse(item.getPath()));
                        startActivity(Intent.createChooser(share,"Share"));
                        break;
                    case 3: showVideoInfo(item); break;
                    case 4: confirmDeleteVideo(item); break;
                }
            }).show();
    }

    private void addVideoToFavourites(MediaItem item) {
        new Thread(() -> {
            com.fountainpdl.fountainplay.db.AppDatabase db =
                com.fountainpdl.fountainplay.db.AppDatabase.get(requireContext());
            List<com.fountainpdl.fountainplay.db.entity.PlaylistEntity> all =
                db.playlistDao().getAllPlaylists();
            com.fountainpdl.fountainplay.db.entity.PlaylistEntity fav = null;
            for (com.fountainpdl.fountainplay.db.entity.PlaylistEntity p : all)
                if ("Favourites".equals(p.name)) { fav = p; break; }
            if (fav == null) {
                com.fountainpdl.fountainplay.db.entity.PlaylistEntity nf =
                    new com.fountainpdl.fountainplay.db.entity.PlaylistEntity();
                nf.name = "Favourites"; nf.createdAt = System.currentTimeMillis();
                long id = db.playlistDao().insertPlaylist(nf); nf.id = (int) id; fav = nf;
            }
            com.fountainpdl.fountainplay.db.entity.PlaylistSong s =
                new com.fountainpdl.fountainplay.db.entity.PlaylistSong();
            s.playlistId = fav.id; s.path = item.getPath();
            s.title = item.getTitle(); s.artist = "Video";
            s.duration = item.getDuration();
            db.playlistDao().insertSong(s);
            db.playlistDao().updateCount(fav.id);
            requireActivity().runOnUiThread(()->
                Toast.makeText(requireContext(),"Added to Favourites ⭐",Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void showVideoInfo(MediaItem item) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Video Info")
            .setMessage("Title:    " + item.getTitle()
                + "\nDuration: " + item.getFormattedDuration()
                + "\nSize:     " + (item.getSize()/1024/1024) + " MB"
                + "\nFolder:   " + item.getFolder()
                + "\nPath:     " + item.getPath())
            .setPositiveButton("OK", null).show();
    }

    private void confirmDeleteVideo(MediaItem item) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete \"" + item.getTitle() + "\"?")
            .setMessage("This permanently deletes the video file.")
            .setPositiveButton("Delete", (d, i) -> new Thread(() -> {
                boolean ok = false;
                try {
                    int rows = requireContext().getContentResolver().delete(
                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        android.provider.MediaStore.Video.Media.DATA + "=?",
                        new String[]{item.getPath()});
                    ok = rows > 0;
                } catch (Exception ignored) {}
                if (!ok) { java.io.File f = new java.io.File(item.getPath()); ok = f.exists() && f.delete(); }
                final boolean success = ok;
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        videos.remove(item);
                        adapter.updateAll(new ArrayList<>(videos));
                        Toast.makeText(requireContext(),"Deleted",Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),"Delete failed — try a file manager",Toast.LENGTH_SHORT).show();
                    }
                });
            }).start())
            .setNegativeButton("Cancel", null).show();
    }
}
EOF
echo "✅ VideoFragment rebuilt"

# ════════════════════════════════════════════════════════════
# 4. UPDATE fragment_video.xml — add toggle layout button
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/fragment_video.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="?attr/android:colorBackground">

    <!-- Toolbar -->
    <LinearLayout android:layout_width="match_parent" android:layout_height="56dp"
        android:orientation="horizontal" android:gravity="center_vertical"
        android:paddingHorizontal="8dp"
        android:background="?attr/colorSurface">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="Videos" android:textSize="20sp" android:textStyle="bold"
            android:textColor="?attr/colorOnSurface" android:paddingHorizontal="8dp" />
        <TextView android:id="@+id/tv_video_count"
            android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textColor="@color/on_surface_variant_dark" android:textSize="13sp" />
        <ImageButton android:id="@+id/btn_toggle_layout"
            android:layout_width="40dp" android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_sort_by_size"
            android:tint="?attr/colorOnSurface"
            android:background="?attr/selectableItemBackgroundBorderless" />
        <ImageButton android:id="@+id/btn_sort_video"
            android:layout_width="40dp" android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_agenda"
            android:tint="?attr/colorOnSurface"
            android:background="?attr/selectableItemBackgroundBorderless" />
        <ImageButton android:id="@+id/btn_refresh_video"
            android:layout_width="40dp" android:layout_height="40dp"
            android:src="@android:drawable/ic_menu_rotate"
            android:tint="?attr/colorOnSurface"
            android:background="?attr/selectableItemBackgroundBorderless" />
    </LinearLayout>

    <!-- Search -->
    <androidx.appcompat.widget.SearchView android:id="@+id/search_view_video"
        android:layout_width="match_parent" android:layout_height="48dp"
        android:background="?attr/colorSurface" />

    <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv_videos"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:paddingHorizontal="4dp"
        android:paddingBottom="120dp" />
</LinearLayout>
EOF

# ════════════════════════════════════════════════════════════
# 5. LibraryFragment — 6 tabs covering both music and video
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

    // Songs / Albums / Artists / Videos / Playlists / History
    private static final String[] TABS = {
        "Songs", "Albums", "Artists", "Videos", "Playlists", "History"
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_library, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewPager2 vp   = view.findViewById(R.id.view_pager);
        TabLayout  tabs = view.findViewById(R.id.tab_layout);

        vp.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int pos) {
                return LibraryPageFragment.newInstance(TABS[pos]);
            }
            @Override public int getItemCount() { return TABS.length; }
        });

        new TabLayoutMediator(tabs, vp, (tab, pos) -> tab.setText(TABS[pos])).attach();
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 6. LibraryPageFragment — handle "Videos" tab too
# ════════════════════════════════════════════════════════════
python3 << 'PYEOF'
with open("app/src/main/java/com/fountainpdl/fountainplay/ui/library/LibraryPageFragment.java","r") as f:
    c = f.read()

# Add video tab handling inside the main switch
old = '''        if (tab.equals("Playlists")) { loadPlaylists(rv, tvEmpty); return; }
        if (tab.equals("History"))   { loadHistory(rv, tvEmpty);   return; }'''

new = '''        if (tab.equals("Playlists")) { loadPlaylists(rv, tvEmpty); return; }
        if (tab.equals("History"))   { loadHistory(rv, tvEmpty);   return; }
        if (tab.equals("Videos"))    { loadVideos(rv, tvEmpty);    return; }'''

c = c.replace(old, new)

# Add import for VideoPlayerActivity and MediaItem TYPE_VIDEO
c = c.replace(
    "import com.fountainpdl.fountainplay.player.AudioPlayerActivity;",
    "import com.fountainpdl.fountainplay.player.AudioPlayerActivity;\nimport com.fountainpdl.fountainplay.player.VideoPlayerActivity;"
)

# Add loadVideos method before the onePerAlbum method
insert = '''
    private void loadVideos(RecyclerView rv, TextView tvEmpty) {
        new Thread(() -> {
            List<MediaItem> result = com.fountainpdl.fountainplay.util.MediaScanner
                .scanVideo(requireContext());
            requireActivity().runOnUiThread(() -> {
                if (result.isEmpty()) {
                    tvEmpty.setText("No videos found");
                    tvEmpty.setVisibility(android.view.View.VISIBLE);
                    return;
                }
                List<MediaItem> items = new java.util.ArrayList<>(result);
                MediaAdapter adapter = new MediaAdapter(items, 1);
                rv.setAdapter(adapter);
                adapter.setOnItemClickListener((item, pos) -> {
                    Intent i = new Intent(requireContext(), VideoPlayerActivity.class);
                    i.putExtra(VideoPlayerActivity.EXTRA_URI, item.getPath());
                    i.putExtra(VideoPlayerActivity.EXTRA_TITLE, item.getTitle());
                    startActivity(i);
                });
            });
        }).start();
    }

'''
c = c.replace("    private List<MediaItem> onePerAlbum", insert + "    private List<MediaItem> onePerAlbum")

with open("app/src/main/java/com/fountainpdl/fountainplay/ui/library/LibraryPageFragment.java","w") as f:
    f.write(c)
print("✅ LibraryPageFragment — Videos tab added")
PYEOF

echo ""
echo "✅ PART C DONE — run fp_v4D.sh next"
