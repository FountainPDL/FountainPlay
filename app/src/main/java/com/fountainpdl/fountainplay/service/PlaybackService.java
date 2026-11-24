package com.fountainpdl.fountainplay.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.entity.HistoryItem;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.PlayQueue;
import com.fountainpdl.fountainplay.util.PlaybackState;
import java.io.File;

/**
 * Owns the single audio ExoPlayer. It is registered as a Media3 session, so Media3 itself shows the
 * notification, the lock-screen controls and Bluetooth/headset buttons, and keeps the service in the
 * foreground while music plays (which is what lets it survive being cleared from Recents).
 *
 * The app-level PlayQueue stays the source of truth for ordering; the session player exposes
 * Next / Previous and routes them back into the queue.
 */
public class PlaybackService extends MediaSessionService implements PlayQueue.Listener {

    /** SharedPreferences key (AppPreferences file). true = keep playing after the app is closed. */
    public static final String PREF_BACKGROUND_PLAYBACK = "background_playback";

    private ExoPlayer exo;
    private MediaSession mediaSession;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sleepRunnable;
    private long sleepEndsAt = 0;
    private int errorStreak = 0;
    private boolean progressRunning = false;

    private final Runnable progressTick = new Runnable() {
        @Override public void run() {
            publishPosition();
            if (exo != null && exo.isPlaying()) handler.postDelayed(this, 500);
            else progressRunning = false;
        }
    };

    private android.content.SharedPreferences settings;
    private boolean sessionRegistered = false;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener prefListener =
        (sp, key) -> { if (AppPreferences.KEY_SHOW_NOTIFICATION.equals(key)) syncSessionRegistration(); };

    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public PlaybackService getService() { return PlaybackService.this; }
    }

    @Override public void onCreate() {
        super.onCreate();

        AudioAttributes attrs = new AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA).build();

        exo = new ExoPlayer.Builder(this)
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build();

        exo.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean playing) {
                PlaybackState.get().setPlaying(playing);
                if (playing) { errorStreak = 0; startProgress(); }
                else publishPosition();
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) publishPosition();
                if (state == Player.STATE_ENDED) PlayQueue.get().advance();
            }
            @Override public void onPositionDiscontinuity(Player.PositionInfo oldPosition,
                    Player.PositionInfo newPosition, int reason) {
                publishPosition();
            }
            @Override public void onPlayerError(PlaybackException error) {
                errorStreak++;
                if (errorStreak < 3 && PlayQueue.get().hasNext()) PlayQueue.get().next();
                else exo.stop();
            }
        });

        // The session player advertises Next/Previous and sends them into the app queue,
        // so the notification, lock screen and Bluetooth buttons all work.
        Player sessionPlayer = new ForwardingPlayer(exo) {
            @Override public Player.Commands getAvailableCommands() {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build();
            }
            @Override public boolean isCommandAvailable(int command) {
                if (command == Player.COMMAND_SEEK_TO_NEXT
                        || command == Player.COMMAND_SEEK_TO_PREVIOUS
                        || command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
                        || command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) return true;
                return super.isCommandAvailable(command);
            }
            @Override public boolean hasNextMediaItem() { return PlayQueue.get().hasNext(); }
            @Override public boolean hasPreviousMediaItem() { return PlayQueue.get().size() > 0; }
            @Override public void seekToNext() { skipNext(); }
            @Override public void seekToNextMediaItem() { skipNext(); }
            @Override public void seekToPrevious() { skipPrevious(); }
            @Override public void seekToPreviousMediaItem() { skipPrevious(); }
        };

        Intent open = new Intent(this, AudioPlayerActivity.class);
        open.putExtra(AudioPlayerActivity.EXTRA_RESUME, true);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        mediaSession = new MediaSession.Builder(this, sessionPlayer)
            .setSessionActivity(contentIntent)
            .build();

        DefaultMediaNotificationProvider provider = new DefaultMediaNotificationProvider(this);
        provider.setSmallIcon(R.drawable.ic_stat_fountain);
        setMediaNotificationProvider(provider);

        // Register the session with Media3 right away (unless the user turned the notification off in
        // Settings). Without this the notification only appears once an external MediaController connects.
        settings = getSharedPreferences(AppPreferences.PREF, MODE_PRIVATE);
        settings.registerOnSharedPreferenceChangeListener(prefListener);
        syncSessionRegistration();

        PlayQueue.get().addListener(this);
    }

    // ── Public API used by the activities ───────────────────────────────────────

    /** Starts a track from the beginning. */
    public void playItem(MediaItem item) {
        if (item == null || item.getPath() == null) return;
        exo.setMediaItem(buildMediaItem(item));
        exo.setPlaybackSpeed(new AppPreferences(this).getPlaybackSpeed());
        exo.prepare();
        exo.setPlayWhenReady(true);
        PlaybackState.get().setCurrentItem(item);
        saveHistory(item);
    }

    /** Plays the item unless it is already the loaded track (so opening the player does not restart it). */
    public void ensurePlaying(MediaItem item) {
        if (item == null) return;
        androidx.media3.common.MediaItem loaded = exo.getCurrentMediaItem();
        if (loaded != null && item.getPath() != null && item.getPath().equals(loaded.mediaId)) {
            if (exo.getPlaybackState() == Player.STATE_IDLE) exo.prepare();
            if (exo.getPlaybackState() == Player.STATE_ENDED) exo.seekTo(0);
            if (!exo.getPlayWhenReady()) exo.play();
            PlaybackState.get().setCurrentItem(item);
            return;
        }
        playItem(item);
    }

    public void playPause() {
        if (exo.getPlayWhenReady() && exo.getPlaybackState() != Player.STATE_ENDED) exo.pause();
        else {
            if (exo.getPlaybackState() == Player.STATE_ENDED) exo.seekTo(0);
            exo.play();
        }
    }

    public void skipNext() {
        PlayQueue.get().next(); // the queue listener starts the new track
    }

    public void skipPrevious() {
        if (exo.getCurrentPosition() > 3000) { exo.seekTo(0); return; }
        int before = PlayQueue.get().getIndex();
        PlayQueue.get().previous();
        if (PlayQueue.get().getIndex() == before && !PlayQueue.get().isShuffle()) exo.seekTo(0);
    }

    public void seekTo(long pos) { exo.seekTo(pos); publishPosition(); }
    public long getCurrentPosition() { return exo.getCurrentPosition(); }
    public long getDuration() { return exo.getDuration(); }
    public boolean isPlaying() { return exo.isPlaying(); }
    public ExoPlayer getPlayer() { return exo; }
    public void setPlaybackSpeed(float s) { exo.setPlaybackSpeed(s); }

    /** Stops playback completely and removes the notification. */
    public void stopPlayback() {
        cancelSleepTimer();
        exo.stop();
        exo.clearMediaItems();
        PlayQueue.get().clear();
        PlaybackState.get().setPlaying(false);
        PlaybackState.get().setCurrentItem(null);
    }

    public void setSleepTimer(long ms) {
        cancelSleepTimer();
        if (ms <= 0) return;
        sleepEndsAt = SystemClock.elapsedRealtime() + ms;
        sleepRunnable = () -> {
            exo.pause();
            sleepEndsAt = 0;
            sleepRunnable = null;
        };
        handler.postDelayed(sleepRunnable, ms);
    }

    public void cancelSleepTimer() {
        if (sleepRunnable != null) handler.removeCallbacks(sleepRunnable);
        sleepRunnable = null;
        sleepEndsAt = 0;
    }

    /** Milliseconds left on the sleep timer, 0 when no timer is running. */
    public long getSleepRemaining() {
        return sleepEndsAt > 0 ? Math.max(0, sleepEndsAt - SystemClock.elapsedRealtime()) : 0;
    }

    // ── Queue listener ──────────────────────────────────────────────────────────

    @Override public void onQueueChanged() { }

    @Override public void onTrackChanged(MediaItem item, int idx) {
        if (item != null) playItem(item);
    }

    // ── Internals ───────────────────────────────────────────────────────────────

    private androidx.media3.common.MediaItem buildMediaItem(MediaItem item) {
        String path = item.getPath();
        Uri uri = path.startsWith("/") ? Uri.fromFile(new File(path)) : Uri.parse(path);
        MediaMetadata.Builder md = new MediaMetadata.Builder()
            .setTitle(item.getTitle())
            .setArtist(item.getArtist())
            .setAlbumTitle(item.getAlbum());
        if (item.getAlbumArtUri() != null) md.setArtworkUri(Uri.parse(item.getAlbumArtUri()));
        return new androidx.media3.common.MediaItem.Builder()
            .setMediaId(path)
            .setUri(uri)
            .setMediaMetadata(md.build())
            .build();
    }

    private void publishPosition() {
        if (exo == null) return;
        long pos = exo.getCurrentPosition();
        long dur = exo.getDuration();
        PlaybackState.get().setPosition(pos);
        if (dur > 0) PlaybackState.get().notifyPosition(pos, dur);
    }

    private void startProgress() {
        if (!progressRunning) {
            progressRunning = true;
            handler.post(progressTick);
        }
    }

    private void saveHistory(MediaItem item) {
        final Context app = getApplicationContext();
        final String path = item.getPath();
        final String title = item.getTitle();
        final String artist = item.getArtist();
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

    /** Shows or hides the notification and lock-screen controls when the Settings toggle changes. */
    private void syncSessionRegistration() {
        if (mediaSession == null || settings == null) return;
        boolean want = settings.getBoolean(AppPreferences.KEY_SHOW_NOTIFICATION, true);
        if (want && !sessionRegistered) {
            addSession(mediaSession);
            sessionRegistered = true;
        } else if (!want && sessionRegistered) {
            removeSession(mediaSession);
            sessionRegistered = false;
        }
    }

    // ── Service plumbing ────────────────────────────────────────────────────────

    @Nullable @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Nullable @Override
    public IBinder onBind(@Nullable Intent intent) {
        IBinder b = super.onBind(intent);
        return b != null ? b : binder;
    }

    /** Swiping the app out of Recents: keep playing unless the user turned background playback off. */
    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        boolean keep = new AppPreferences(this).getBoolean(AppPreferences.KEY_BACKGROUND, true);
        if (!keep || !exo.getPlayWhenReady() || exo.getMediaItemCount() == 0) {
            exo.pause();
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (settings != null) settings.unregisterOnSharedPreferenceChangeListener(prefListener);
        PlayQueue.get().removeListener(this);
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (exo != null) {
            exo.release();
            exo = null;
        }
        super.onDestroy();
    }
}
