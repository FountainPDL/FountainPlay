package com.fountainpdl.fountainplay.player;

import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import com.fountainpdl.fountainplay.R;

public class AudioPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_URI = "media_uri";
    public static final String EXTRA_TITLE = "media_title";
    public static final String EXTRA_ARTIST = "media_artist";
    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);
        String uriStr = getIntent().getStringExtra(EXTRA_URI);
        if (uriStr == null) { finish(); return; }
        player = new ExoPlayer.Builder(this).build();
        player.setMediaItem(MediaItem.fromUri(Uri.parse(uriStr)));
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override protected void onDestroy() { super.onDestroy(); if (player != null) { player.release(); player = null; } }
}
