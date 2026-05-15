package com.fountainpdl.fountainplay.player;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;

public class AudioPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "media_uri";
    public static final String EXTRA_TITLE = "media_title";
    public static final String EXTRA_ARTIST = "media_artist";
    public static final String EXTRA_ALBUM_ART = "album_art_uri";

    private ExoPlayer player;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressUpdater;

    private ImageView ivAlbumArt, ivBgBlur;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnShuffle, btnRepeat;

    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String artist = getIntent().getStringExtra(EXTRA_ARTIST);
        String albumArt = getIntent().getStringExtra(EXTRA_ALBUM_ART);

        if (uriStr == null) { finish(); return; }

        // Bind views
        ivAlbumArt = findViewById(R.id.iv_album_art);
        ivBgBlur = findViewById(R.id.iv_bg_blur);
        tvTitle = findViewById(R.id.tv_title);
        tvArtist = findViewById(R.id.tv_artist);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        seekBar = findViewById(R.id.seek_bar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnRepeat = findViewById(R.id.btn_repeat);

        // Set metadata
        tvTitle.setText(title != null ? title : "Unknown Title");
        tvArtist.setText(artist != null ? artist : "Unknown Artist");

        if (albumArt != null) {
            Glide.with(this).load(albumArt).centerCrop().into(ivAlbumArt);
            Glide.with(this).load(albumArt).centerCrop().into(ivBgBlur);
        }

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Init ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)));
        player.prepare();
        player.setPlayWhenReady(true);
        isPlaying = true;

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    long dur = player.getDuration();
                    seekBar.setMax(1000);
                    tvTotalTime.setText(formatTime(dur));
                    startProgressUpdater();
                }
            }
            @Override
            public void onIsPlayingChanged(boolean playing) {
                isPlaying = playing;
                btnPlayPause.setImageResource(playing
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play);
            }
        });

        btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) player.pause();
            else player.play();
        });

        // Speed chip
        findViewById(R.id.chip_speed).setOnClickListener(v -> {
            float cur = player.getPlaybackParameters().speed;
            float next = cur >= 2.0f ? 0.5f : cur + 0.25f;
            player.setPlaybackSpeed(next);
            ((com.google.android.material.chip.Chip) v).setText(next + "×");
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    long pos = (long)(progress / 1000f * player.getDuration());
                    player.seekTo(pos);
                    tvCurrentTime.setText(formatTime(pos));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void startProgressUpdater() {
        progressUpdater = new Runnable() {
            @Override public void run() {
                if (player != null && player.getDuration() > 0) {
                    long pos = player.getCurrentPosition();
                    long dur = player.getDuration();
                    seekBar.setProgress((int)(pos * 1000 / dur));
                    tvCurrentTime.setText(formatTime(pos));
                }
                handler.postDelayed(this, 500);
            }
        };
        handler.post(progressUpdater);
    }

    private String formatTime(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        s %= 60; m %= 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }

    @Override protected void onPause() { super.onPause(); if (player != null) player.pause(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (player != null) { player.release(); player = null; }
    }
}

// Note: also handles direct file/content URI from file manager via getIntent().getData()
