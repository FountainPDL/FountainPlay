package com.fountainpdl.fountainplay.engine;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import java.io.File;

/** Media3 ExoPlayer: hardware-accelerated, gapless, handles the common formats. */
public final class ExoAudioEngine implements AudioEngine {

    private final ExoPlayer player;
    private final Callback callback;
    private boolean lastPlaying = false;
    private boolean released = false;

    public ExoAudioEngine(Context ctx, Callback cb) {
        callback = cb;
        AudioAttributes attrs = new AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build();
        player = new ExoPlayer.Builder(ctx.getApplicationContext(),
                new DefaultRenderersFactory(ctx.getApplicationContext()).setEnableDecoderFallback(true))
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build();
        player.addListener(new Player.Listener() {
            @Override public void onPlayWhenReadyChanged(boolean ready, int reason) { publish(); }
            @Override public void onIsPlayingChanged(boolean playing) { publish(); }
            @Override public void onPlaybackStateChanged(int state) {
                publish();
                if (state == Player.STATE_ENDED) callback.onEnded(ExoAudioEngine.this);
            }
            @Override public void onPlayerError(PlaybackException e) {
                callback.onError(ExoAudioEngine.this, e.getErrorCodeName());
            }
            @Override public void onTracksChanged(Tracks tracks) {
                // The file has audio, but no installed decoder can play any of it: hand over to VLC
                boolean hasAudio = false, anySupported = false;
                for (Tracks.Group g : tracks.getGroups()) {
                    if (g.getType() != C.TRACK_TYPE_AUDIO) continue;
                    hasAudio = true;
                    if (g.isSupported()) anySupported = true;
                }
                if (hasAudio && !anySupported) callback.onError(ExoAudioEngine.this, "no supported audio track");
            }
        });
    }

    private void publish() {
        boolean p = isPlaying();
        if (p != lastPlaying) {
            lastPlaying = p;
            callback.onPlayingChanged(this, p);
        }
    }

    @Override public String name() { return "ExoPlayer"; }

    @Override public void load(String path, long startMs, boolean autoPlay) {
        Uri uri = path.startsWith("/") ? Uri.fromFile(new File(path)) : Uri.parse(path);
        player.setMediaItem(MediaItem.fromUri(uri), Math.max(0, startMs));
        player.prepare();
        player.setPlayWhenReady(autoPlay);
    }

    @Override public void play() {
        if (player.getPlaybackState() == Player.STATE_ENDED) player.seekTo(0);
        player.play();
    }
    @Override public void pause() { player.pause(); }
    @Override public void stop() { player.stop(); player.clearMediaItems(); publish(); }
    @Override public void release() { released = true; player.release(); }

    @Override public void seekTo(long ms) { player.seekTo(Math.max(0, ms)); }
    @Override public long getPosition() { return released ? 0 : player.getCurrentPosition(); }
    @Override public long getDuration() { return released ? 0 : Math.max(0, player.getDuration()); }

    @Override public boolean isPlaying() {
        if (released) return false;
        int s = player.getPlaybackState();
        return player.getPlayWhenReady() && s != Player.STATE_ENDED && s != Player.STATE_IDLE;
    }

    @Override public void setSpeed(float speed) { player.setPlaybackSpeed(speed); }
}
