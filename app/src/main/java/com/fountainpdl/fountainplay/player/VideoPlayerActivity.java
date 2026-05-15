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
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_player);

        // Handle both internal launch and file manager launch
        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        Uri uri;
        if (uriStr != null) {
            uri = Uri.parse(uriStr);
        } else if (getIntent().getData() != null) {
            uri = getIntent().getData(); // opened from file manager
        } else {
            finish(); return;
        }

        PlayerView playerView = findViewById(R.id.player_view);
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override protected void onPause() { super.onPause(); if(player!=null) player.pause(); }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (player != null) { player.release(); player = null; }
    }
}
