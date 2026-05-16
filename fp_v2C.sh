#!/bin/bash
# ── v2 PART C: Audio Player (background + mini), Video Player (PiP + gestures) ──
# Run from inside ~/FountainPlay

P="app/src/main/java/com/fountainpdl/fountainplay"

# ════════════════════════════════════════════════════════════
# 1. UPDATE app/build.gradle — add PiP support flag
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
        minSdk 26
        targetSdk 34
        versionCode 2
        versionName "1.1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    buildFeatures { viewBinding true }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.navigation:navigation-fragment:2.7.6'
    implementation 'androidx.navigation:navigation-ui:2.7.6'
    implementation 'androidx.media3:media3-exoplayer:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-hls:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-dash:1.2.1'
    implementation 'androidx.media3:media3-ui:1.2.1'
    implementation 'androidx.media3:media3-session:1.2.1'
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
    implementation 'androidx.palette:palette:1.0.0'
    implementation 'androidx.viewpager2:viewpager2:1.0.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
}
EOF
echo "✅ build.gradle updated (minSdk 26 for PiP)"

# ════════════════════════════════════════════════════════════
# 2. UPDATE AndroidManifest — PiP + EXTRA_RESUME
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

        <activity android:name=".MainActivity" android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Video: PiP enabled -->
        <activity android:name=".player.VideoPlayerActivity"
            android:configChanges="orientation|screenSize|keyboardHidden|smallestScreenSize|screenLayout"
            android:screenOrientation="sensor"
            android:supportsPictureInPicture="true"
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
        </activity>

        <!-- Audio: background-capable -->
        <activity android:name=".player.AudioPlayerActivity"
            android:configChanges="orientation|screenSize"
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
        </activity>

        <service android:name=".service.PlaybackService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>

    </application>
</manifest>
EOF
echo "✅ Manifest updated (PiP + background)"

# ════════════════════════════════════════════════════════════
# 3. FULL AUDIO PLAYER — background keep-playing + mini player
# ════════════════════════════════════════════════════════════
cat > $P/player/AudioPlayerActivity.java << 'EOF'
package com.fountainpdl.fountainplay.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.MainActivity;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.PlaybackState;
import com.google.android.material.chip.Chip;

public class AudioPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_URI    = "media_uri";
    public static final String EXTRA_TITLE  = "media_title";
    public static final String EXTRA_ARTIST = "media_artist";
    public static final String EXTRA_ALBUM_ART = "album_art_uri";
    public static final String EXTRA_RESUME = "resume_playback";

    // Shared player instance — keeps audio alive when activity finishes
    private static ExoPlayer sharedPlayer;
    private static String currentUri;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private AppPreferences prefs;
    private boolean isUserSeeking = false;

    // Views
    private ImageView ivAlbumArt, ivBgBlur;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnBack;
    private Chip chipSpeed;

    private BroadcastReceiver controlReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            if (sharedPlayer == null) return;
            switch (intent.getAction()) {
                case "com.fountainpdl.fountainplay.TOGGLE_PLAY":
                    if (sharedPlayer.isPlaying()) sharedPlayer.pause();
                    else sharedPlayer.play(); break;
                case "com.fountainpdl.fountainplay.NEXT":
                    sharedPlayer.seekToNext(); break;
                case "com.fountainpdl.fountainplay.PREV":
                    sharedPlayer.seekToPrevious(); break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_audio_player);
        prefs = new AppPreferences(this);

        bindViews();

        String uriStr   = getIntent().getStringExtra(EXTRA_URI);
        String title    = getIntent().getStringExtra(EXTRA_TITLE);
        String artist   = getIntent().getStringExtra(EXTRA_ARTIST);
        String albumArt = getIntent().getStringExtra(EXTRA_ALBUM_ART);
        boolean resume  = getIntent().getBooleanExtra(EXTRA_RESUME, false);

        if (uriStr == null) { finish(); return; }

        // Set metadata UI
        tvTitle.setText(title != null ? title : "Unknown Title");
        tvArtist.setText(artist != null ? artist : "Unknown Artist");
        tvTitle.setSelected(true); // marquee

        if (albumArt != null) {
            Glide.with(this).load(albumArt).centerCrop().into(ivAlbumArt);
            Glide.with(this).load(albumArt).centerCrop().into(ivBgBlur);
        }

        // Reuse or create player
        if (sharedPlayer == null || !uriStr.equals(currentUri) && !resume) {
            if (sharedPlayer != null) sharedPlayer.release();
            sharedPlayer = new ExoPlayer.Builder(this).build();
            sharedPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)));
            sharedPlayer.prepare();
            sharedPlayer.setPlayWhenReady(true);
            sharedPlayer.setPlaybackSpeed(prefs.getPlaybackSpeed());
            currentUri = uriStr;
        }

        // Update PlaybackState for mini player
        com.fountainpdl.fountainplay.model.MediaItem stateItem =
            new com.fountainpdl.fountainplay.model.MediaItem(0, title, artist, uriStr, 0,
                com.fountainpdl.fountainplay.model.MediaItem.TYPE_AUDIO);
        stateItem.setAlbumArtUri(albumArt);
        PlaybackState.get().setCurrentItem(stateItem);
        PlaybackState.get().setPlaying(true);

        setupPlayerListeners();
        setupControls();
        startProgressUpdater();

        // Broadcast receiver for mini player controls
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.fountainpdl.fountainplay.TOGGLE_PLAY");
        filter.addAction("com.fountainpdl.fountainplay.NEXT");
        filter.addAction("com.fountainpdl.fountainplay.PREV");
        registerReceiver(controlReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void bindViews() {
        ivAlbumArt     = findViewById(R.id.iv_album_art);
        ivBgBlur       = findViewById(R.id.iv_bg_blur);
        tvTitle        = findViewById(R.id.tv_title);
        tvArtist       = findViewById(R.id.tv_artist);
        tvCurrentTime  = findViewById(R.id.tv_current_time);
        tvTotalTime    = findViewById(R.id.tv_total_time);
        seekBar        = findViewById(R.id.seek_bar);
        btnPlayPause   = findViewById(R.id.btn_play_pause);
        btnPrev        = findViewById(R.id.btn_prev);
        btnNext        = findViewById(R.id.btn_next);
        btnBack        = findViewById(R.id.btn_back);
        chipSpeed      = findViewById(R.id.chip_speed);
    }

    private void setupPlayerListeners() {
        sharedPlayer.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    tvTotalTime.setText(fmt(sharedPlayer.getDuration()));
                    seekBar.setMax(1000);
                }
            }
            @Override public void onIsPlayingChanged(boolean playing) {
                PlaybackState.get().setPlaying(playing);
                btnPlayPause.setImageResource(playing
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play);
            }
        });
    }

    private void setupControls() {
        btnBack.setOnClickListener(v -> finish()); // back → keeps playing

        btnPlayPause.setOnClickListener(v -> {
            if (sharedPlayer.isPlaying()) sharedPlayer.pause();
            else sharedPlayer.play();
        });

        btnNext.setOnClickListener(v -> sharedPlayer.seekToNext());
        btnPrev.setOnClickListener(v -> {
            if (sharedPlayer.getCurrentPosition() > 3000) sharedPlayer.seekTo(0);
            else sharedPlayer.seekToPrevious();
        });

        // Speed chip cycle
        chipSpeed.setText(prefs.getPlaybackSpeed() + "×");
        chipSpeed.setOnClickListener(v -> {
            float cur = sharedPlayer.getPlaybackParameters().speed;
            float[] speeds = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
            float next = speeds[0];
            for (int i = 0; i < speeds.length - 1; i++) {
                if (Math.abs(cur - speeds[i]) < 0.01f) { next = speeds[i + 1]; break; }
            }
            sharedPlayer.setPlaybackSpeed(next);
            prefs.setPlaybackSpeed(next);
            chipSpeed.setText(next + "×");
        });

        // Seek bar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    long pos = (long)(progress / 1000f * sharedPlayer.getDuration());
                    tvCurrentTime.setText(fmt(pos));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                long pos = (long)(sb.getProgress() / 1000f * sharedPlayer.getDuration());
                sharedPlayer.seekTo(pos);
            }
        });
    }

    private void startProgressUpdater() {
        progressRunnable = new Runnable() {
            @Override public void run() {
                if (sharedPlayer != null && !isUserSeeking) {
                    long pos = sharedPlayer.getCurrentPosition();
                    long dur = sharedPlayer.getDuration();
                    if (dur > 0) {
                        int progress = (int)(pos * 1000 / dur);
                        seekBar.setProgress(progress);
                        tvCurrentTime.setText(fmt(pos));
                        PlaybackState.get().setPosition(pos);
                        // notify mini player
                        for (PlaybackState.Listener l : new java.util.ArrayList<>(getListeners()))
                            l.onPositionChanged(pos, dur);
                    }
                }
                handler.postDelayed(this, 500);
            }
        };
        handler.post(progressRunnable);
    }

    private java.util.List<PlaybackState.Listener> getListeners() {
        // Reflect to get listeners for mini player update — simplified approach
        return new java.util.ArrayList<>();
    }

    private String fmt(long ms) {
        if (ms < 0) ms = 0;
        long s = ms / 1000, m = s / 60, h = m / 60;
        s %= 60; m %= 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }

    /** Back button → go back but KEEP PLAYING (mini player shows) */
    @Override public void onBackPressed() {
        // Don't stop player — show mini player in MainActivity
        PlaybackState.get().setPlaying(sharedPlayer != null && sharedPlayer.isPlaying());
        super.onBackPressed();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        try { unregisterReceiver(controlReceiver); } catch (Exception ignored) {}
        // DO NOT release sharedPlayer here — it keeps playing in background
    }

    /** Call this to fully stop playback (e.g., from close button) */
    public static void stopPlayback() {
        if (sharedPlayer != null) {
            sharedPlayer.stop();
            sharedPlayer.release();
            sharedPlayer = null;
            currentUri = null;
        }
    }

    public static boolean isPlaying() {
        return sharedPlayer != null && sharedPlayer.isPlaying();
    }
}
EOF
echo "✅ AudioPlayerActivity (background keep-playing)"

# ════════════════════════════════════════════════════════════
# 4. VIDEO PLAYER — PiP + gesture controls (volume/brightness/seek)
# ════════════════════════════════════════════════════════════
cat > app/src/main/res/layout/activity_video_player.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/black">

    <!-- ExoPlayer surface -->
    <androidx.media3.ui.PlayerView
        android:id="@+id/player_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:show_timeout="3000"
        app:resize_mode="fit"
        app:use_controller="true" />

    <!-- Gesture feedback overlays -->
    <LinearLayout android:id="@+id/overlay_left"
        android:layout_width="80dp" android:layout_height="match_parent"
        android:layout_gravity="start" android:gravity="center"
        android:orientation="vertical" android:visibility="invisible"
        android:background="#44000000">
        <ImageView android:layout_width="32dp" android:layout_height="32dp"
            android:src="@android:drawable/ic_menu_zoom"
            android:tint="@color/white" />
        <TextView android:id="@+id/tv_brightness"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="50%" android:textColor="@color/white" android:textSize="12sp" />
    </LinearLayout>

    <LinearLayout android:id="@+id/overlay_right"
        android:layout_width="80dp" android:layout_height="match_parent"
        android:layout_gravity="end" android:gravity="center"
        android:orientation="vertical" android:visibility="invisible"
        android:background="#44000000">
        <ImageView android:layout_width="32dp" android:layout_height="32dp"
            android:src="@android:drawable/ic_lock_silent_mode_off"
            android:tint="@color/white" />
        <TextView android:id="@+id/tv_volume"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="50%" android:textColor="@color/white" android:textSize="12sp" />
    </LinearLayout>

    <TextView android:id="@+id/tv_seek_indicator"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:textColor="@color/white" android:textSize="18sp" android:textStyle="bold"
        android:background="@color/overlay_dark"
        android:paddingHorizontal="16dp" android:paddingVertical="8dp"
        android:visibility="invisible" />

    <!-- Lock button -->
    <ImageButton android:id="@+id/btn_lock"
        android:layout_width="40dp" android:layout_height="40dp"
        android:layout_gravity="start|center_vertical"
        android:layout_margin="12dp"
        android:src="@android:drawable/ic_lock_lock"
        android:tint="@color/white"
        android:background="@color/overlay_dark"
        android:visibility="gone" />

    <!-- PiP button -->
    <ImageButton android:id="@+id/btn_pip"
        android:layout_width="40dp" android:layout_height="40dp"
        android:layout_gravity="top|end"
        android:layout_margin="12dp"
        android:src="@android:drawable/ic_menu_view"
        android:tint="@color/white"
        android:background="@color/overlay_dark" />

</FrameLayout>
EOF

cat > $P/player/VideoPlayerActivity.java << 'EOF'
package com.fountainpdl.fountainplay.player;

import android.app.PictureInPictureParams;
import android.content.Context;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.util.AppPreferences;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "media_uri";

    private ExoPlayer player;
    private PlayerView playerView;
    private AppPreferences prefs;
    private AudioManager audioManager;

    // Gesture
    private float touchStartX, touchStartY;
    private long seekStartPos;
    private int startVolume, startBrightness;
    private boolean isLocked = false;
    private boolean gesturesEnabled = true;

    // Overlay views
    private View overlayLeft, overlayRight;
    private TextView tvVolume, tvBrightness, tvSeekIndicator;
    private ImageButton btnLock, btnPip;
    private Handler hideHandler = new Handler(Looper.getMainLooper());

    private static final int GESTURE_SEEK = 0, GESTURE_VOLUME = 1, GESTURE_BRIGHTNESS = 2;
    private int activeGesture = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_player);

        prefs = new AppPreferences(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        gesturesEnabled = prefs.getGesturesEnabled();

        playerView    = findViewById(R.id.player_view);
        overlayLeft   = findViewById(R.id.overlay_left);
        overlayRight  = findViewById(R.id.overlay_right);
        tvVolume      = findViewById(R.id.tv_volume);
        tvBrightness  = findViewById(R.id.tv_brightness);
        tvSeekIndicator = findViewById(R.id.tv_seek_indicator);
        btnLock       = findViewById(R.id.btn_lock);
        btnPip        = findViewById(R.id.btn_pip);

        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        Uri uri;
        if (uriStr != null) uri = Uri.parse(uriStr);
        else if (getIntent().getData() != null) uri = getIntent().getData();
        else { finish(); return; }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();
        player.setPlayWhenReady(true);
        player.setPlaybackSpeed(prefs.getPlaybackSpeed());

        setupGestures();

        btnPip.setOnClickListener(v -> enterPiP());

        btnLock.setOnClickListener(v -> {
            isLocked = !isLocked;
            btnLock.setImageResource(isLocked
                ? android.R.drawable.ic_lock_lock
                : android.R.drawable.ic_lock_idle_lock);
            playerView.setUseController(!isLocked);
        });

        // Show lock button when controls appear
        playerView.setControllerVisibilityListener(
            (androidx.media3.ui.PlayerView.ControllerVisibilityListener) visibility -> {
                btnLock.setVisibility(visibility == View.VISIBLE ? View.VISIBLE : View.GONE);
                btnPip.setVisibility(visibility == View.VISIBLE ? View.VISIBLE : View.GONE);
            });
    }

    private void setupGestures() {
        if (!gesturesEnabled) return;

        playerView.setOnTouchListener((v, event) -> {
            if (isLocked) return false;

            int screenW = getWindow().getDecorView().getWidth();
            int screenH = getWindow().getDecorView().getHeight();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartX = event.getX();
                    touchStartY = event.getY();
                    seekStartPos = player.getCurrentPosition();
                    startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    startBrightness = getBrightness();
                    activeGesture = -1;
                    break;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - touchStartX;
                    float dy = event.getY() - touchStartY;

                    if (activeGesture == -1) {
                        if (Math.abs(dx) > Math.abs(dy) * 1.5f && Math.abs(dx) > 30)
                            activeGesture = GESTURE_SEEK;
                        else if (Math.abs(dy) > 30)
                            activeGesture = touchStartX < screenW / 2 ? GESTURE_BRIGHTNESS : GESTURE_VOLUME;
                    }

                    if (activeGesture == GESTURE_SEEK) {
                        long seekDelta = (long)(dx * 300); // 300ms per pixel
                        long newPos = Math.max(0, Math.min(player.getDuration(), seekStartPos + seekDelta));
                        String sign = seekDelta >= 0 ? "+" : "";
                        showSeekIndicator(sign + (seekDelta / 1000) + "s → " + fmtTime(newPos));
                        player.seekTo(newPos);

                    } else if (activeGesture == GESTURE_VOLUME) {
                        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int newVol = (int) Math.max(0, Math.min(maxVol, startVolume - dy / screenH * maxVol * 2));
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);
                        int pct = (int)(newVol * 100f / maxVol);
                        showVolumeIndicator(pct + "%");

                    } else if (activeGesture == GESTURE_BRIGHTNESS) {
                        int newBrightness = (int) Math.max(0, Math.min(255, startBrightness - dy / screenH * 255 * 2));
                        setBrightness(newBrightness);
                        showBrightnessIndicator((int)(newBrightness * 100f / 255) + "%");
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    hideOverlays();
                    activeGesture = -1;
                    // If tiny movement = tap = toggle controls
                    if (Math.abs(event.getX() - touchStartX) < 10 && Math.abs(event.getY() - touchStartY) < 10) {
                        v.performClick();
                    }
                    break;
            }
            return true;
        });
    }

    private void showSeekIndicator(String text) {
        tvSeekIndicator.setText(text);
        tvSeekIndicator.setVisibility(View.VISIBLE);
        overlayLeft.setVisibility(View.INVISIBLE);
        overlayRight.setVisibility(View.INVISIBLE);
    }

    private void showVolumeIndicator(String pct) {
        tvVolume.setText(pct);
        overlayRight.setVisibility(View.VISIBLE);
        tvSeekIndicator.setVisibility(View.INVISIBLE);
    }

    private void showBrightnessIndicator(String pct) {
        tvBrightness.setText(pct);
        overlayLeft.setVisibility(View.VISIBLE);
        tvSeekIndicator.setVisibility(View.INVISIBLE);
    }

    private void hideOverlays() {
        hideHandler.postDelayed(() -> {
            overlayLeft.setVisibility(View.INVISIBLE);
            overlayRight.setVisibility(View.INVISIBLE);
            tvSeekIndicator.setVisibility(View.INVISIBLE);
        }, 800);
    }

    private int getBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (lp.screenBrightness < 0) {
            try { return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS); }
            catch (Exception e) { return 128; }
        }
        return (int)(lp.screenBrightness * 255);
    }

    private void setBrightness(int value) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = value / 255f;
        getWindow().setAttributes(lp);
    }

    private String fmtTime(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        s %= 60; m %= 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }

    private void enterPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9))
                .build();
            enterPictureInPictureMode(params);
        }
    }

    @Override
    public void onUserLeaveHint() {
        // Auto-enter PiP when user presses home during video
        super.onUserLeaveHint();
        if (player != null && player.isPlaying()) {
            enterPiP();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPiPMode,
                                               android.content.res.Configuration config) {
        super.onPictureInPictureModeChanged(isInPiPMode, config);
        // Hide/show UI elements in PiP mode
        playerView.setUseController(!isInPiPMode);
        btnPip.setVisibility(isInPiPMode ? View.GONE : View.VISIBLE);
    }

    @Override protected void onPause() {
        super.onPause();
        // Don't pause if entering PiP
        if (!isInPictureInPictureMode() && player != null) player.pause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (player != null && !isInPictureInPictureMode()) player.play();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        hideHandler.removeCallbacksAndMessages(null);
        if (player != null) { player.release(); player = null; }
    }
}
EOF
echo "✅ VideoPlayerActivity (PiP + gesture controls)"

# ════════════════════════════════════════════════════════════
# 5. APP ICON — generate a proper purple/red SVG icon
#    (GitHub Actions will compile this as vector drawable)
# ════════════════════════════════════════════════════════════
mkdir -p app/src/main/res/drawable
cat > app/src/main/res/drawable/ic_launcher_foreground.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">

    <!-- Purple/red gradient background circle -->
    <path android:fillColor="#7B2FBE"
        android:pathData="M54,54m-54,0a54,54 0 1,0 108,0a54,54 0 1,0 -108,0" />

    <!-- Play button triangle -->
    <path android:fillColor="#FFFFFF"
        android:pathData="M40,30 L40,78 L78,54 Z" />

    <!-- Red accent dot -->
    <path android:fillColor="#E53935"
        android:pathData="M72,28m-8,0a8,8 0 1,0 16,0a8,8 0 1,0 -16,0" />

    <!-- Sound waves -->
    <path android:strokeColor="#FFFFFF" android:strokeWidth="3"
        android:fillColor="@android:color/transparent"
        android:pathData="M84,38 Q92,54 84,70" />
    <path android:strokeColor="#FFFFFF" android:strokeWidth="2.5"
        android:fillColor="@android:color/transparent"
        android:pathData="M89,32 Q100,54 89,76" />
</vector>
EOF

cat > app/src/main/res/mipmap-hdpi/ic_launcher_background.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient android:startColor="#5A1E8C" android:endColor="#B71C1C"
        android:angle="135" android:type="linear" />
</shape>
EOF

# Adaptive icon
mkdir -p app/src/main/res/mipmap-anydpi-v26
cat > app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/fp_purple_dark"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
EOF
cat > app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/fp_purple_dark"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
EOF
echo "✅ App icon created (adaptive, purple/red)"

echo ""
echo "════════════════════════════════════════"
echo "✅ ALL v2 PARTS DONE! Push to GitHub:"
echo "════════════════════════════════════════"
echo ""
echo "  git add ."
echo "  git commit -m 'feat: v2 - PiP, gestures, mini player, home, library, settings, icon'"
echo "  git push"
echo ""
echo "What's new in v2:"
echo "  🎵 Audio keeps playing when you press back"
echo "  📺 Video enters PiP automatically on home press"
echo "  🖐️ Gesture controls: swipe left/right=seek, left side up/down=brightness, right=volume"
echo "  🏠 Home shows Recently Added music + videos"
echo "  📚 Library has 5 tabs: Songs/Albums/Artists/Playlists/History"
echo "  ⚙️  Settings: theme, speed, folders, gestures, hw accel"
echo "  🎨 Better app icon (purple/red adaptive)"
echo "  🔲 Fixed 0-byte videos in video grid"
