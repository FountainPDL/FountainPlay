package com.fountainpdl.fountainplay.player;

import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.fountainpdl.fountainplay.R;

public class VideoPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_URI = "media_uri";
    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_player);

        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        if (uriStr == null) { finish(); return; }

        PlayerView playerView = findViewById(R.id.player_view);
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)));
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override protected void onPause() { super.onPause(); player.pause(); }
    @Override protected void onDestroy() { super.onDestroy(); if (player != null) { player.release(); player = null; } }
}
