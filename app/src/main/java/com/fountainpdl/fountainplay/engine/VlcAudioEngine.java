package com.fountainpdl.fountainplay.engine;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import java.io.File;
import java.util.ArrayList;

/** LibVLC: software decoding for the formats ExoPlayer cannot play (APE, WMA, DTS, TrueHD, AIFF, tracker modules...). */
public final class VlcAudioEngine implements AudioEngine {

    private final Context app;
    private final Callback callback;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AudioManager audio;

    private LibVLC lib;
    private MediaPlayer mp;
    private float speed = 1f;
    private long pendingSeek = -1;
    private boolean playing = false;
    private boolean resumeOnFocusGain = false;
    private AudioFocusRequest focusRequest;

    public VlcAudioEngine(Context ctx, Callback cb) {
        app = ctx.getApplicationContext();
        callback = cb;
        audio = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
    }

    private void ensure() {
        if (lib != null) return;
        ArrayList<String> options = new ArrayList<>();
        options.add("--no-video");
        options.add("--audio-time-stretch");
        lib = new LibVLC(app, options);
        mp = new MediaPlayer(lib);
        mp.setEventListener(event -> main.post(() -> onVlcEvent(event.type)));
    }

    private void onVlcEvent(int type) {
        switch (type) {
            case MediaPlayer.Event.Playing:
                if (pendingSeek > 0) { mp.setTime(pendingSeek); pendingSeek = -1; }
                mp.setRate(speed);
                setPlaying(true);
                break;
            case MediaPlayer.Event.Paused:
            case MediaPlayer.Event.Stopped:
                setPlaying(false);
                break;
            case MediaPlayer.Event.EndReached:
                setPlaying(false);
                callback.onEnded(this);
                break;
            case MediaPlayer.Event.EncounteredError:
                setPlaying(false);
                callback.onError(this, "VLC could not play this file");
                break;
            default: break;
        }
    }

    private void setPlaying(boolean p) {
        if (p == playing) return;
        playing = p;
        callback.onPlayingChanged(this, p);
    }

    @Override public String name() { return "VLC"; }

    @Override public void load(String path, long startMs, boolean autoPlay) {
        ensure();
        Uri uri = path.startsWith("/") ? Uri.fromFile(new File(path)) : Uri.parse(path);
        Media media = new Media(lib, uri);
        mp.setMedia(media);
        media.release();
        pendingSeek = startMs > 0 ? startMs : -1;
        if (autoPlay) play();
    }

    @Override public void play() {
        if (mp == null) return;
        requestFocus();
        mp.play();
    }

    @Override public void pause() {
        if (mp != null) mp.pause();
    }

    @Override public void stop() {
        if (mp != null) mp.stop();
        abandonFocus();
        setPlaying(false);
    }

    @Override public void release() {
        abandonFocus();
        if (mp != null) { mp.setEventListener(null); mp.release(); mp = null; }
        if (lib != null) { lib.release(); lib = null; }
        playing = false;
    }

    @Override public void seekTo(long ms) { if (mp != null) mp.setTime(Math.max(0, ms)); }
    @Override public long getPosition() { return mp == null ? 0 : Math.max(0, mp.getTime()); }
    @Override public long getDuration() { return mp == null ? 0 : Math.max(0, mp.getLength()); }
    @Override public boolean isPlaying() { return playing; }

    @Override public void setSpeed(float s) {
        speed = s;
        if (mp != null) mp.setRate(s);
    }

    // ── Audio focus (ExoPlayer does this itself; VLC does not) ──────────────────

    private final AudioManager.OnAudioFocusChangeListener focusListener = change -> {
        switch (change) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mp != null) mp.setVolume(100);
                if (resumeOnFocusGain) { resumeOnFocusGain = false; if (mp != null) mp.play(); }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mp != null) mp.setVolume(30);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                resumeOnFocusGain = playing;
                if (mp != null) mp.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                resumeOnFocusGain = false;
                if (mp != null) mp.pause();
                break;
            default: break;
        }
    };

    private void requestFocus() {
        if (audio == null) return;
        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener(focusListener)
            .build();
        audio.requestAudioFocus(focusRequest);
    }

    private void abandonFocus() {
        if (audio != null && focusRequest != null) audio.abandonAudioFocusRequest(focusRequest);
        focusRequest = null;
    }
}
