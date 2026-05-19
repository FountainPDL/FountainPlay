package com.fountainpdl.fountainplay.service;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.fountainpdl.fountainplay.FountainApp;
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.entity.HistoryItem;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.util.PlayQueue;
import com.fountainpdl.fountainplay.util.PlaybackState;

public class PlaybackService extends MediaSessionService implements PlayQueue.Listener {

    public static final String ACTION_PLAY  = "fp.PLAY";
    public static final String ACTION_PAUSE = "fp.PAUSE";
    public static final String ACTION_NEXT  = "fp.NEXT";
    public static final String ACTION_PREV  = "fp.PREV";
    public static final String ACTION_STOP  = "fp.STOP";
    public static final String ACTION_SEEK  = "fp.SEEK";
    public static final String EXTRA_POSITION = "position";

    private ExoPlayer player;
    private MediaSession mediaSession;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public PlaybackService getService() { return PlaybackService.this; }
    }

    @Override public void onCreate() {
        super.onCreate();

        AudioAttributes attrs = new AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA).build();

        player = new ExoPlayer.Builder(this)
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build();

        player.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean playing) {
                PlaybackState.get().setPlaying(playing);
                updateNotification();
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    MediaItem next = PlayQueue.get().next();
                    if (next != null) playItem(next);
                    else player.pause();
                }
            }
        });

        mediaSession = new MediaSession.Builder(this, player).build();
        PlayQueue.get().addListener(this);
        startProgressUpdater();
    }

    public void playItem(MediaItem item) {
        if (item == null) return;
        player.stop();
        player.setMediaItem(androidx.media3.common.MediaItem.fromUri(Uri.parse(item.getPath())));
        player.prepare();
        player.setPlayWhenReady(true);
        PlaybackState.get().setCurrentItem(item);
        saveHistory(item);
        showForegroundNotification(item);
    }

    private void saveHistory(MediaItem item) {
        new Thread(() -> {
            HistoryItem h = new HistoryItem();
            h.path = item.getPath(); h.title = item.getTitle();
            h.artist = item.getArtist(); h.albumArtUri = item.getAlbumArtUri();
            h.type = item.getType(); h.duration = item.getDuration();
            h.playedAt = System.currentTimeMillis();
            AppDatabase.get(this).historyDao().insert(h);
        }).start();
    }

    public void playPause() {
        if (player.isPlaying()) player.pause(); else player.play();
    }

    public void skipNext() {
        MediaItem next = PlayQueue.get().next();
        if (next != null) playItem(next);
    }

    public void skipPrevious() {
        if (player.getCurrentPosition() > 3000) player.seekTo(0);
        else { MediaItem prev = PlayQueue.get().previous(); if (prev != null) playItem(prev); }
    }

    public void seekTo(long pos) { player.seekTo(pos); }
    public long getCurrentPosition() { return player.getCurrentPosition(); }
    public long getDuration() { return player.getDuration(); }
    public boolean isPlaying() { return player.isPlaying(); }
    public ExoPlayer getPlayer() { return player; }
    public void setPlaybackSpeed(float s) { player.setPlaybackSpeed(s); }

    private void showForegroundNotification(MediaItem item) {
        Intent stopI = new Intent(this, PlaybackService.class); stopI.setAction(ACTION_STOP);
        Intent prevI = new Intent(this, PlaybackService.class); prevI.setAction(ACTION_PREV);
        Intent playI = new Intent(this, PlaybackService.class); playI.setAction(ACTION_PLAY);
        Intent nextI = new Intent(this, PlaybackService.class); nextI.setAction(ACTION_NEXT);

        PendingIntent stopPi = PendingIntent.getService(this,0,stopI,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent prevPi = PendingIntent.getService(this,1,prevI,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent playPi = PendingIntent.getService(this,2,playI,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent nextPi = PendingIntent.getService(this,3,nextI,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new NotificationCompat.Builder(this, FountainApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(item.getTitle())
            .setContentText(item.getArtist())
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_previous, "Prev", prevPi)
            .addAction(android.R.drawable.ic_media_pause, "Play/Pause", playPi)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPi)
            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0,1,2))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build();

        startForeground(1, n);
    }

    private void updateNotification() {
        MediaItem cur = PlaybackState.get().getCurrentItem();
        if (cur != null) showForegroundNotification(cur);
    }

    private void startProgressUpdater() {
        progressRunnable = new Runnable() {
            @Override public void run() {
                if (player != null) {
                    long pos = player.getCurrentPosition();
                    long dur = player.getDuration();
                    PlaybackState.get().setPosition(pos);
                    if (dur > 0) PlaybackState.get().notifyPosition(pos, dur);
                }
                handler.postDelayed(this, 500);
            }
        };
        handler.post(progressRunnable);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:  playPause(); break;
                case ACTION_PAUSE: player.pause(); break;
                case ACTION_NEXT:  skipNext(); break;
                case ACTION_PREV:  skipPrevious(); break;
                case ACTION_STOP:
                    player.stop(); stopForeground(true); stopSelf(); break;
                case ACTION_SEEK:
                    seekTo(intent.getLongExtra(EXTRA_POSITION, 0)); break;
            }
        }
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) {
        IBinder b = super.onBind(intent);
        return b != null ? b : binder;
    }

    @Override public void onQueueChanged() {}
    @Override public void onTrackChanged(MediaItem item, int idx) {
        if (item != null) playItem(item);
    }

    @Override public MediaSession onGetSession(MediaSession.ControllerInfo info) {
        return mediaSession;
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        PlayQueue.get().removeListener(this);
        if (mediaSession != null) { mediaSession.getPlayer().release(); mediaSession.release(); }
        super.onDestroy();
    }
}
