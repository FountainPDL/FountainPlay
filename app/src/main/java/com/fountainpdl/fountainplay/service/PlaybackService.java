package com.fountainpdl.fountainplay.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.entity.HistoryItem;
import com.fountainpdl.fountainplay.engine.AudioEngine;
import com.fountainpdl.fountainplay.engine.EngineSelector;
import com.fountainpdl.fountainplay.engine.ExoAudioEngine;
import com.fountainpdl.fountainplay.engine.VlcAudioEngine;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.player.LockScreenActivity;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.ArtLoader;
import com.fountainpdl.fountainplay.util.Favourites;
import com.fountainpdl.fountainplay.util.PlayQueue;
import com.fountainpdl.fountainplay.util.PlaybackState;

/**
 * The music service. It owns two engines (ExoPlayer first, VLC when ExoPlayer cannot play a file),
 * a MediaSession, the notification (repeat / previous / play-pause / next / like, with cover art),
 * and starts the lock-screen player when the screen turns off while music plays.
 */
public class PlaybackService extends Service implements PlayQueue.Listener, AudioEngine.Callback {

    public static final String ACTION_PLAY_PAUSE = "com.fountainpdl.fountainplay.action.PLAY_PAUSE";
    public static final String ACTION_NEXT       = "com.fountainpdl.fountainplay.action.NEXT";
    public static final String ACTION_PREVIOUS   = "com.fountainpdl.fountainplay.action.PREVIOUS";
    public static final String ACTION_REPEAT     = "com.fountainpdl.fountainplay.action.REPEAT";
    public static final String ACTION_FAVOURITE  = "com.fountainpdl.fountainplay.action.FAVOURITE";
    public static final String ACTION_STOP       = "com.fountainpdl.fountainplay.action.STOP";

    private static final String CHANNEL_ID = "fp_playback_v2";
    private static final int NOTIFICATION_ID = 1701;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public PlaybackService getService() { return PlaybackService.this; }
    }

    private MediaSessionCompat session;
    private NotificationManager notificationManager;
    private AppPreferences prefs;

    private ExoAudioEngine exo;
    private VlcAudioEngine vlc;
    private AudioEngine active;

    private boolean playing = false;
    private boolean foreground = false;
    private boolean triedVlc = false;
    private int errorStreak = 0;
    private float speed = 1f;
    private String loadedPath;
    private long loadedDuration = 0;

    private Bitmap currentArt;
    private String currentArtPath;
    private boolean favourite = false;
    private String favouritePath;

    private Runnable sleepRunnable;
    private long sleepEndsAt = 0;
    private boolean receiversRegistered = false;

    private final Runnable progressTick = new Runnable() {
        @Override public void run() {
            publishPosition();
            if (playing) handler.postDelayed(this, 500);
        }
    };

    // ── Lifecycle ───────────────────────────────────────────────────────────────

    @Override public void onCreate() {
        super.onCreate();
        prefs = new AppPreferences(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createChannel();
        speed = prefs.getPlaybackSpeed();

        session = new MediaSessionCompat(this, "FountainPlay");
        session.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlay() { resume(); }
            @Override public void onPause() { pauseNow(); }
            @Override public void onStop() { stopPlayback(); }
            @Override public void onSkipToNext() { skipNext(); }
            @Override public void onSkipToPrevious() { skipPrevious(); }
            @Override public void onSeekTo(long pos) { seekTo(pos); }
            @Override public void onCustomAction(String action, Bundle extras) { handleAction(action); }
        });
        Intent open = new Intent(this, AudioPlayerActivity.class)
            .putExtra(AudioPlayerActivity.EXTRA_RESUME, true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        session.setSessionActivity(PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        Intent mb = new Intent(Intent.ACTION_MEDIA_BUTTON).setComponent(new ComponentName(this, MediaButtonReceiver.class));
        session.setMediaButtonReceiver(PendingIntent.getBroadcast(this, 0, mb, PendingIntent.FLAG_IMMUTABLE));
        session.setActive(true);

        exo = new ExoAudioEngine(this, this);
        PlayQueue.get().addListener(this);
        registerReceivers();
        updateSessionState();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
                // Started by a headset button from the background: must go foreground within seconds
                if (!foreground) startForegroundNow(buildNotification());
                MediaButtonReceiver.handleIntent(session, intent);
                handler.postDelayed(this::settleForeground, 1500);
            } else if (action != null) {
                handleAction(action);
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return binder; }

    /** Swiped out of Recents: keep playing unless the user turned background playback off. */
    @Override public void onTaskRemoved(Intent rootIntent) {
        boolean keep = prefs.getBoolean(AppPreferences.KEY_BACKGROUND, true);
        if (!keep || !playing) {
            stopPlayback();
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        PlayQueue.get().removeListener(this);
        unregisterReceivers();
        if (exo != null) exo.release();
        if (vlc != null) vlc.release();
        if (session != null) { session.setActive(false); session.release(); }
        PlaybackState.get().setPlaying(false);
        super.onDestroy();
    }

    // ── Public API used by the activities ───────────────────────────────────────

    public void playItem(MediaItem item) {
        if (item == null || item.getPath() == null) return;
        triedVlc = false;
        startOn(EngineSelector.audioNeedsVlc(item.getPath()) ? vlc() : exo, item.getPath(), 0);
        PlaybackState.get().setCurrentItem(item);
        saveHistory(item);
        currentArt = null;
        currentArtPath = item.getPath();
        favourite = false;
        favouritePath = item.getPath();
        updateMetadata(item);
        setPlaying(true);
        loadArtAndFavouriteAsync(item);
    }

    /** Plays the item unless it is already loaded (opening the player must not restart the song). */
    public void ensurePlaying(MediaItem item) {
        if (item == null) return;
        if (item.getPath() != null && item.getPath().equals(loadedPath) && active != null) {
            PlaybackState.get().setCurrentItem(item);
            if (!playing) resume();
            return;
        }
        playItem(item);
    }

    public void playPause() { if (playing) pauseNow(); else resume(); }

    public void skipNext() { PlayQueue.get().next(); }

    public void skipPrevious() {
        if (getCurrentPosition() > 3000) { seekTo(0); return; }
        int before = PlayQueue.get().getIndex();
        PlayQueue.get().previous();
        if (PlayQueue.get().getIndex() == before && !PlayQueue.get().isShuffle()) seekTo(0);
    }

    public void seekTo(long pos) {
        if (active != null) active.seekTo(pos);
        publishPosition();
        updateSessionState();
    }

    public long getCurrentPosition() { return active == null ? 0 : active.getPosition(); }
    public long getDuration() { return active == null ? 0 : active.getDuration(); }
    public boolean isPlaying() { return playing; }
    public float getSpeed() { return speed; }

    public void setPlaybackSpeed(float s) {
        speed = s;
        if (active != null) active.setSpeed(s);
        updateSessionState();
    }

    public void stopPlayback() {
        cancelSleepTimer();
        if (active != null) active.stop();
        loadedPath = null;
        PlayQueue.get().clear();
        PlaybackState.get().setCurrentItem(null);
        setPlaying(false);
        handler.removeCallbacks(progressTick);
        updateSessionState();
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        foreground = false;
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public void setSleepTimer(long ms) {
        cancelSleepTimer();
        if (ms <= 0) return;
        sleepEndsAt = SystemClock.elapsedRealtime() + ms;
        sleepRunnable = () -> { pauseNow(); sleepEndsAt = 0; sleepRunnable = null; };
        handler.postDelayed(sleepRunnable, ms);
    }

    public void cancelSleepTimer() {
        if (sleepRunnable != null) handler.removeCallbacks(sleepRunnable);
        sleepRunnable = null;
        sleepEndsAt = 0;
    }

    public long getSleepRemaining() {
        return sleepEndsAt > 0 ? Math.max(0, sleepEndsAt - SystemClock.elapsedRealtime()) : 0;
    }

    // ── Engines ─────────────────────────────────────────────────────────────────

    private VlcAudioEngine vlc() {
        if (vlc == null) vlc = new VlcAudioEngine(this, this);
        return vlc;
    }

    private void startOn(AudioEngine engine, String path, long startMs) {
        if (active != null && active != engine) active.stop();
        active = engine;
        loadedPath = path;
        engine.setSpeed(speed);
        engine.load(path, startMs, true);
    }

    @Override public void onPlayingChanged(AudioEngine source, boolean isPlaying) {
        if (source != active) return;
        if (isPlaying) errorStreak = 0;
        setPlaying(isPlaying);
    }

    @Override public void onEnded(AudioEngine source) {
        if (source != active) return;
        PlayQueue.get().advance();
    }

    @Override public void onError(AudioEngine source, String why) {
        if (source != active) return;
        // ExoPlayer could not play it: try VLC once, from the same position
        if (source == exo && !triedVlc && loadedPath != null) {
            triedVlc = true;
            startOn(vlc(), loadedPath, exo.getPosition());
            return;
        }
        errorStreak++;
        if (errorStreak < 3 && PlayQueue.get().hasNext()) PlayQueue.get().next();
        else setPlaying(false);
    }

    // ── Playback state, session, notification ───────────────────────────────────

    private void resume() {
        if (active == null) {
            MediaItem cur = PlayQueue.get().current();
            if (cur != null) playItem(cur);
            return;
        }
        active.play();
        setPlaying(true);
    }

    private void pauseNow() {
        if (active != null) active.pause();
        setPlaying(false);
    }

    private void setPlaying(boolean p) {
        boolean changed = p != playing;
        playing = p;
        PlaybackState.get().setPlaying(p);
        if (p) { handler.removeCallbacks(progressTick); handler.post(progressTick); }
        updateSessionState();
        postNotification();
        if (changed && !p) publishPosition();
    }

    private void publishPosition() {
        if (active == null) return;
        long pos = active.getPosition();
        long dur = active.getDuration();
        PlaybackState.get().setPosition(pos);
        if (dur > 0) {
            PlaybackState.get().notifyPosition(pos, dur);
            if (dur != loadedDuration) {            // duration becomes known after the file opens
                loadedDuration = dur;
                MediaItem cur = PlaybackState.get().getCurrentItem();
                if (cur != null) updateMetadata(cur);
            }
        }
    }

    private void updateMetadata(MediaItem item) {
        MediaMetadataCompat.Builder b = new MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.getTitle())
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, item.getArtist())
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, item.getAlbum())
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Math.max(loadedDuration, item.getDuration()));
        if (currentArt != null) {
            b.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentArt);
            b.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, currentArt);
            b.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, currentArt);
        }
        session.setMetadata(b.build());
    }

    private void updateSessionState() {
        long actions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SEEK_TO
            | PlaybackStateCompat.ACTION_STOP;
        int state = active == null ? PlaybackStateCompat.STATE_STOPPED
            : playing ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        PlaybackStateCompat.Builder b = new PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, getCurrentPosition(), playing ? speed : 0f, SystemClock.elapsedRealtime())
            .addCustomAction(ACTION_REPEAT, "Repeat", repeatIcon())
            .addCustomAction(ACTION_FAVOURITE, "Like", favourite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        session.setPlaybackState(b.build());
    }

    private int repeatIcon() {
        return PlayQueue.get().getRepeatMode() == PlayQueue.REPEAT_ONE ? R.drawable.ic_repeat_one : R.drawable.ic_repeat;
    }

    private PendingIntent actionIntent(String action, int code) {
        Intent i = new Intent(this, PlaybackService.class).setAction(action);
        return PendingIntent.getService(this, code, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification buildNotification() {
        MediaItem item = PlaybackState.get().getCurrentItem();
        Intent open = new Intent(this, AudioPlayerActivity.class)
            .putExtra(AudioPlayerActivity.EXTRA_RESUME, true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(this, 1, open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        boolean repeatOn = PlayQueue.get().getRepeatMode() != PlayQueue.REPEAT_NONE;
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_fountain)
            .setContentTitle(item == null ? "Fountain Play" : item.getTitle())
            .setContentText(item == null ? "" : item.getArtist())
            .setSubText(item == null ? null : item.getAlbum())
            .setContentIntent(content)
            .setDeleteIntent(actionIntent(ACTION_STOP, 6))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(playing)
            .addAction(repeatIcon(), repeatOn ? "Repeat on" : "Repeat", actionIntent(ACTION_REPEAT, 1))
            .addAction(R.drawable.ic_skip_previous, "Previous", actionIntent(ACTION_PREVIOUS, 2))
            .addAction(playing ? R.drawable.ic_pause : R.drawable.ic_play, playing ? "Pause" : "Play", actionIntent(ACTION_PLAY_PAUSE, 3))
            .addAction(R.drawable.ic_skip_next, "Next", actionIntent(ACTION_NEXT, 4))
            .addAction(favourite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border, favourite ? "Liked" : "Like", actionIntent(ACTION_FAVOURITE, 5))
            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(session.getSessionToken())
                .setShowActionsInCompactView(1, 2, 3));
        if (currentArt != null) nb.setLargeIcon(currentArt);
        return nb.build();
    }

    /** Posts the notification; goes foreground while playing so Android keeps the service alive in the background. */
    private void postNotification() {
        if (active == null && !foreground) return;
        Notification n = buildNotification();
        if (playing) {
            startForegroundNow(n);
        } else {
            if (foreground) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH);
                foreground = false;
            }
            if (active != null) notificationManager.notify(NOTIFICATION_ID, n);
        }
    }

    private void startForegroundNow(Notification n) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIFICATION_ID, n);
            }
            foreground = true;
        } catch (Exception e) {
            // Not allowed to start a foreground service right now: still show the notification
            notificationManager.notify(NOTIFICATION_ID, n);
        }
    }

    /** After a headset-button start: if nothing is playing, drop out of the foreground again. */
    private void settleForeground() {
        if (playing) return;
        if (active == null) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
            foreground = false;
            stopSelf();
        } else {
            postNotification();
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Music playback", NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Controls for the song that is playing");
        ch.setShowBadge(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationManager.createNotificationChannel(ch);
    }

    private void loadArtAndFavouriteAsync(final MediaItem item) {
        final Context app = getApplicationContext();
        new Thread(() -> {
            final Bitmap art = ArtLoader.load(app, item, 512);
            boolean fav = false;
            try { fav = Favourites.isFavourite(app, item.getPath()); } catch (Exception ignored) { }
            final boolean isFav = fav;
            handler.post(() -> {
                if (item.getPath() == null || !item.getPath().equals(currentArtPath)) return;
                currentArt = art;
                favourite = isFav;
                updateMetadata(item);
                updateSessionState();
                postNotification();
            });
        }).start();
    }

    // ── Notification / lock-screen button actions ───────────────────────────────

    private void handleAction(String action) {
        switch (action) {
            case ACTION_PLAY_PAUSE: playPause(); break;
            case ACTION_NEXT:       skipNext(); break;
            case ACTION_PREVIOUS:   skipPrevious(); break;
            case ACTION_STOP:       stopPlayback(); stopSelf(); break;
            case ACTION_REPEAT:
                PlayQueue.get().cycleRepeat();
                break;
            case ACTION_FAVOURITE: {
                final MediaItem item = PlaybackState.get().getCurrentItem();
                if (item == null) break;
                final Context app = getApplicationContext();
                new Thread(() -> {
                    final boolean now = Favourites.toggle(app, item);
                    handler.post(() -> {
                        favourite = now;
                        updateSessionState();
                        postNotification();
                    });
                }).start();
                break;
            }
            default: break;
        }
    }

    // ── Queue listener ──────────────────────────────────────────────────────────

    @Override public void onQueueChanged() {
        handler.post(() -> { updateSessionState(); postNotification(); });   // repeat mode may have changed
    }

    @Override public void onTrackChanged(MediaItem item, int idx) {
        if (item != null) playItem(item);
    }

    // ── System broadcasts ───────────────────────────────────────────────────────

    private final BroadcastReceiver systemReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(a)) {
                if (playing) pauseNow();
            } else if (Intent.ACTION_SCREEN_OFF.equals(a)) {
                showLockScreenPlayer();
            }
        }
    };

    private void registerReceivers() {
        IntentFilter f = new IntentFilter();
        f.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        ContextCompat.registerReceiver(this, systemReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED);
        receiversRegistered = true;
    }

    private void unregisterReceivers() {
        if (!receiversRegistered) return;
        try { unregisterReceiver(systemReceiver); } catch (Exception ignored) { }
        receiversRegistered = false;
    }

    /** Screen turned off while music plays: put our player over the lock screen. */
    private void showLockScreenPlayer() {
        if (!playing || !prefs.getBoolean(AppPreferences.KEY_LOCK_SCREEN, true)) return;
        try {
            Intent i = new Intent(this, LockScreenActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(i);
        } catch (Exception ignored) {
            // Needs "display over other apps" on Android 10+ (Settings has a shortcut)
        }
    }

    private void saveHistory(MediaItem item) {
        final Context app = getApplicationContext();
        final String path = item.getPath(), title = item.getTitle(), artist = item.getArtist();
        final String art = item.getAlbumArtUri();
        final int type = item.getType();
        final long duration = item.getDuration();
        new Thread(() -> {
            try {
                HistoryItem h = new HistoryItem();
                h.path = path; h.title = title; h.artist = artist; h.albumArtUri = art;
                h.type = type; h.duration = duration; h.playedAt = System.currentTimeMillis();
                AppDatabase db = AppDatabase.get(app);
                db.historyDao().deleteByPath(path);
                db.historyDao().insert(h);
            } catch (Exception ignored) { }
        }).start();
    }
}
