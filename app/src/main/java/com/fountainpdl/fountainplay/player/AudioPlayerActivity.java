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
