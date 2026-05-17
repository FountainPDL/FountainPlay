#!/bin/bash
# ── v3 PART A: Core Architecture ──
# Run from ~/FountainPlay

P="app/src/main/java/com/fountainpdl/fountainplay"

mkdir -p $P/db/entity $P/db/dao

# ════════════════════════════════════════════════════════════
# 1. FountainApp — Application class (theme + init)
# ════════════════════════════════════════════════════════════
cat > $P/FountainApp.java << 'EOF'
package com.fountainpdl.fountainplay;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;
import com.fountainpdl.fountainplay.util.AppPreferences;

public class FountainApp extends Application {
    public static final String CHANNEL_ID = "fp_playback";

    @Override
    public void onCreate() {
        super.onCreate();
        applyTheme(new AppPreferences(this).getTheme());
        createNotificationChannel();
    }

    public static void applyTheme(String theme) {
        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case "amoled":
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Fountain Play", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Media playback controls");
            ch.setShowBadge(false);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 2. PlayQueue — full queue management (shuffle/repeat/auto-advance)
# ════════════════════════════════════════════════════════════
cat > $P/util/PlayQueue.java << 'EOF'
package com.fountainpdl.fountainplay.util;

import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.*;

public class PlayQueue {
    public static final int REPEAT_NONE = 0, REPEAT_ONE = 1, REPEAT_ALL = 2;

    private static PlayQueue instance;
    private List<MediaItem> original = new ArrayList<>();
    private List<MediaItem> queue    = new ArrayList<>();
    private int index        = 0;
    private int repeatMode   = REPEAT_NONE;
    private boolean shuffle  = false;

    public interface Listener {
        void onQueueChanged();
        void onTrackChanged(MediaItem item, int index);
    }
    private final List<Listener> listeners = new ArrayList<>();

    private PlayQueue() {}
    public static PlayQueue get() { if (instance == null) instance = new PlayQueue(); return instance; }

    public void setQueue(List<MediaItem> items, int startIndex) {
        original = new ArrayList<>(items);
        queue    = new ArrayList<>(items);
        index    = startIndex;
        if (shuffle) applyShuffleKeepCurrent();
        notifyQueueChanged();
        notifyTrackChanged();
    }

    public void addToQueue(MediaItem item) {
        queue.add(item);
        original.add(item);
        notifyQueueChanged();
    }

    public void addNext(MediaItem item) {
        int insertAt = index + 1;
        if (insertAt >= queue.size()) queue.add(item);
        else queue.add(insertAt, item);
        notifyQueueChanged();
    }

    public MediaItem current() { return queue.isEmpty() ? null : queue.get(Math.max(0, Math.min(index, queue.size()-1))); }

    public MediaItem next() {
        if (queue.isEmpty()) return null;
        if (repeatMode == REPEAT_ONE) return current();
        if (index < queue.size() - 1) { index++; notifyTrackChanged(); return current(); }
        if (repeatMode == REPEAT_ALL) { index = 0; notifyTrackChanged(); return current(); }
        return null; // end of queue
    }

    public MediaItem previous() {
        if (queue.isEmpty()) return null;
        if (index > 0) { index--; notifyTrackChanged(); return current(); }
        if (repeatMode == REPEAT_ALL) { index = queue.size()-1; notifyTrackChanged(); return current(); }
        return current();
    }

    public boolean hasNext() {
        return !queue.isEmpty() && (index < queue.size()-1 || repeatMode != REPEAT_NONE);
    }

    public boolean hasPrevious() { return index > 0; }

    public void setShuffle(boolean on) {
        shuffle = on;
        if (on) applyShuffleKeepCurrent();
        else { MediaItem cur = current(); queue = new ArrayList<>(original); index = queue.indexOf(cur); }
        notifyQueueChanged();
    }

    private void applyShuffleKeepCurrent() {
        MediaItem cur = current();
        Collections.shuffle(queue);
        int idx = queue.indexOf(cur);
        if (idx >= 0) { queue.remove(idx); queue.add(0, cur); index = 0; }
    }

    public void cycleRepeat() {
        repeatMode = (repeatMode + 1) % 3;
    }

    public boolean isShuffle() { return shuffle; }
    public int getRepeatMode() { return repeatMode; }
    public int getIndex() { return index; }
    public List<MediaItem> getQueue() { return queue; }
    public int size() { return queue.size(); }

    public void moveItem(int from, int to) {
        if (from < 0 || to < 0 || from >= queue.size() || to >= queue.size()) return;
        MediaItem item = queue.remove(from);
        queue.add(to, item);
        if (index == from) index = to;
        else if (from < index && to >= index) index--;
        else if (from > index && to <= index) index++;
        notifyQueueChanged();
    }

    public void removeAt(int pos) {
        if (pos < 0 || pos >= queue.size()) return;
        queue.remove(pos);
        if (pos < index) index--;
        else if (pos == index && index >= queue.size()) index = Math.max(0, queue.size()-1);
        notifyQueueChanged();
    }

    public void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }
    private void notifyQueueChanged() { for (Listener l : new ArrayList<>(listeners)) l.onQueueChanged(); }
    private void notifyTrackChanged() { MediaItem cur = current(); for (Listener l : new ArrayList<>(listeners)) l.onTrackChanged(cur, index); }
}
EOF

# ════════════════════════════════════════════════════════════
# 3. Room DB — History, Playlist entities + DAOs + Database
# ════════════════════════════════════════════════════════════
cat > $P/db/entity/HistoryItem.java << 'EOF'
package com.fountainpdl.fountainplay.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class HistoryItem {
    @PrimaryKey(autoGenerate = true) public int id;
    public String path, title, artist, albumArtUri;
    public int type; // 0=audio, 1=video
    public long playedAt;
    public long duration;
}
EOF

cat > $P/db/entity/PlaylistEntity.java << 'EOF'
package com.fountainpdl.fountainplay.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists")
public class PlaylistEntity {
    @PrimaryKey(autoGenerate = true) public int id;
    public String name;
    public long createdAt;
    public int songCount;
}
EOF

cat > $P/db/entity/PlaylistSong.java << 'EOF'
package com.fountainpdl.fountainplay.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlist_songs")
public class PlaylistSong {
    @PrimaryKey(autoGenerate = true) public int id;
    public int playlistId;
    public String path, title, artist, albumArtUri;
    public long duration;
    public int position;
}
EOF

cat > $P/db/dao/HistoryDao.java << 'EOF'
package com.fountainpdl.fountainplay.db.dao;

import androidx.room.*;
import com.fountainpdl.fountainplay.db.entity.HistoryItem;
import java.util.List;

@Dao
public interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) void insert(HistoryItem item);
    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 200") List<HistoryItem> getAll();
    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT :limit") List<HistoryItem> getRecent(int limit);
    @Query("DELETE FROM history WHERE path = :path") void deleteByPath(String path);
    @Query("DELETE FROM history") void clearAll();
    @Query("SELECT COUNT(*) FROM history") int count();
}
EOF

cat > $P/db/dao/PlaylistDao.java << 'EOF'
package com.fountainpdl.fountainplay.db.dao;

import androidx.room.*;
import com.fountainpdl.fountainplay.db.entity.PlaylistEntity;
import com.fountainpdl.fountainplay.db.entity.PlaylistSong;
import java.util.List;

@Dao
public interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insertPlaylist(PlaylistEntity p);
    @Update void updatePlaylist(PlaylistEntity p);
    @Delete void deletePlaylist(PlaylistEntity p);
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC") List<PlaylistEntity> getAllPlaylists();
    @Query("SELECT * FROM playlists WHERE id = :id") PlaylistEntity getPlaylist(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE) void insertSong(PlaylistSong song);
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :pid ORDER BY position ASC") List<PlaylistSong> getSongsForPlaylist(int pid);
    @Query("DELETE FROM playlist_songs WHERE playlistId = :pid AND path = :path") void removeSong(int pid, String path);
    @Query("DELETE FROM playlist_songs WHERE playlistId = :pid") void clearPlaylist(int pid);
    @Query("UPDATE playlists SET songCount = (SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :pid) WHERE id = :pid") void updateCount(int pid);
}
EOF

cat > $P/db/AppDatabase.java << 'EOF'
package com.fountainpdl.fountainplay.db;

import android.content.Context;
import androidx.room.*;
import com.fountainpdl.fountainplay.db.dao.HistoryDao;
import com.fountainpdl.fountainplay.db.dao.PlaylistDao;
import com.fountainpdl.fountainplay.db.entity.*;

@Database(entities = {HistoryItem.class, PlaylistEntity.class, PlaylistSong.class},
    version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract HistoryDao historyDao();
    public abstract PlaylistDao playlistDao();

    public static synchronized AppDatabase get(Context ctx) {
        if (instance == null) {
            instance = Room.databaseBuilder(ctx.getApplicationContext(),
                AppDatabase.class, "fountain_play.db")
                .fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 4. FULL BACKGROUND PLAYBACK SERVICE
# ════════════════════════════════════════════════════════════
cat > $P/service/PlaybackService.java << 'EOF'
package com.fountainpdl.fountainplay.service;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.support.v4.media.session.MediaSessionCompat;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.fountainpdl.fountainplay.FountainApp;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.entity.HistoryItem;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.util.PlayQueue;
import com.fountainpdl.fountainplay.util.PlaybackState;

public class PlaybackService extends MediaSessionService implements PlayQueue.Listener {

    public static final String ACTION_PLAY   = "fp.PLAY";
    public static final String ACTION_PAUSE  = "fp.PAUSE";
    public static final String ACTION_NEXT   = "fp.NEXT";
    public static final String ACTION_PREV   = "fp.PREV";
    public static final String ACTION_STOP   = "fp.STOP";
    public static final String ACTION_SEEK   = "fp.SEEK";
    public static final String EXTRA_POSITION = "position";

    private ExoPlayer player;
    private MediaSession mediaSession;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    // Binder for activity connection
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public PlaybackService getService() { return PlaybackService.this; }
    }

    @Override
    public void onCreate() {
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
                    // Auto-advance to next
                    MediaItem next = PlayQueue.get().next();
                    if (next != null) playItem(next);
                    else player.pause();
                }
            }
            @Override public void onMediaItemTransition(
                    androidx.media3.common.MediaItem item, int reason) {
                updateNotification();
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

        // Save to history in background
        new Thread(() -> {
            HistoryItem h = new HistoryItem();
            h.path = item.getPath();
            h.title = item.getTitle();
            h.artist = item.getArtist();
            h.albumArtUri = item.getAlbumArtUri();
            h.type = item.getType();
            h.duration = item.getDuration();
            h.playedAt = System.currentTimeMillis();
            AppDatabase.get(this).historyDao().insert(h);
        }).start();

        showForegroundNotification(item);
    }

    public void playPause() {
        if (player.isPlaying()) player.pause(); else player.play();
    }

    public void skipNext() {
        MediaItem next = PlayQueue.get().next();
        if (next != null) playItem(next);
    }

    public void skipPrevious() {
        if (player.getCurrentPosition() > 3000) {
            player.seekTo(0);
        } else {
            MediaItem prev = PlayQueue.get().previous();
            if (prev != null) playItem(prev);
        }
    }

    public void seekTo(long pos) { player.seekTo(pos); }
    public long getCurrentPosition() { return player.getCurrentPosition(); }
    public long getDuration() { return player.getDuration(); }
    public boolean isPlaying() { return player.isPlaying(); }
    public ExoPlayer getPlayer() { return player; }
    public void setPlaybackSpeed(float speed) { player.setPlaybackSpeed(speed); }

    private void showForegroundNotification(MediaItem item) {
        Intent stopIntent = new Intent(this, PlaybackService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent prevIntent = new Intent(this, PlaybackService.class);
        prevIntent.setAction(ACTION_PREV);
        PendingIntent prevPi = PendingIntent.getService(this, 1, prevIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playIntent = new Intent(this, PlaybackService.class);
        playIntent.setAction(ACTION_PLAY);
        PendingIntent playPi = PendingIntent.getService(this, 2, playIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextIntent = new Intent(this, PlaybackService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPi = PendingIntent.getService(this, 3, nextIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, FountainApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(item.getTitle())
            .setContentText(item.getArtist())
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_previous, "Prev", prevPi)
            .addAction(android.R.drawable.ic_media_pause, "Play/Pause", playPi)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPi)
            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build();

        startForeground(1, notification);
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
                case ACTION_PLAY:   playPause(); break;
                case ACTION_PAUSE:  player.pause(); break;
                case ACTION_NEXT:   skipNext(); break;
                case ACTION_PREV:   skipPrevious(); break;
                case ACTION_STOP:
                    player.stop();
                    stopForeground(true);
                    stopSelf();
                    break;
                case ACTION_SEEK:
                    seekTo(intent.getLongExtra(EXTRA_POSITION, 0)); break;
            }
        }
        return START_STICKY; // Restart if killed
    }

    @Override public IBinder onBind(Intent intent) {
        IBinder b = super.onBind(intent);
        return b != null ? b : binder;
    }

    @Override public void onQueueChanged() {}
    @Override public void onTrackChanged(MediaItem item, int index) { if (item != null) playItem(item); }

    @Override public MediaSession onGetSession(MediaSession.ControllerInfo info) { return mediaSession; }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        PlayQueue.get().removeListener(this);
        if (mediaSession != null) { mediaSession.getPlayer().release(); mediaSession.release(); }
        super.onDestroy();
    }
}
EOF

# ════════════════════════════════════════════════════════════
# 5. Update PlaybackState — add notifyPosition
# ════════════════════════════════════════════════════════════
cat > $P/util/PlaybackState.java << 'EOF'
package com.fountainpdl.fountainplay.util;

import com.fountainpdl.fountainplay.model.MediaItem;
import java.util.ArrayList;
import java.util.List;

public class PlaybackState {
    private static PlaybackState instance;
    private MediaItem currentItem;
    private boolean isPlaying = false;
    private long position = 0;

    public interface Listener {
        void onItemChanged(MediaItem item);
        void onPlayStateChanged(boolean playing);
        void onPositionChanged(long pos, long duration);
    }

    private final List<Listener> listeners = new ArrayList<>();
    private PlaybackState() {}
    public static PlaybackState get() { if (instance == null) instance = new PlaybackState(); return instance; }

    public void setCurrentItem(MediaItem item) {
        currentItem = item;
        for (Listener l : new ArrayList<>(listeners)) l.onItemChanged(item);
    }
    public MediaItem getCurrentItem() { return currentItem; }
    public void setPlaying(boolean p) {
        isPlaying = p;
        for (Listener l : new ArrayList<>(listeners)) l.onPlayStateChanged(p);
    }
    public boolean isPlaying() { return isPlaying; }
    public void setPosition(long p) { position = p; }
    public long getPosition() { return position; }
    public void notifyPosition(long pos, long dur) {
        for (Listener l : new ArrayList<>(listeners)) l.onPositionChanged(pos, dur);
    }
    public boolean hasMedia() { return currentItem != null; }
    public void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }
}
EOF

# ════════════════════════════════════════════════════════════
# 6. UPDATE build.gradle — add media-compat + Room
# ════════════════════════════════════════════════════════════
cat > app/build.gradle << 'EOF'
plugins { id 'com.android.application' }

android {
    namespace 'com.fountainpdl.fountainplay'
    compileSdk 34
    defaultConfig {
        applicationId "com.fountainpdl.fountainplay"
        minSdk 26
        targetSdk 34
        versionCode 3
        versionName "1.2.0"
    }
    buildTypes {
        release { minifyEnabled false; proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro' }
    }
    buildFeatures { viewBinding true }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.navigation:navigation-fragment:2.7.6'
    implementation 'androidx.navigation:navigation-ui:2.7.6'
    // Media3
    implementation 'androidx.media3:media3-exoplayer:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-hls:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-dash:1.2.1'
    implementation 'androidx.media3:media3-ui:1.2.1'
    implementation 'androidx.media3:media3-session:1.2.1'
    // Media compat (for notification MediaStyle)
    implementation 'androidx.media:media:1.7.0'
    // Room
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    // Image loading
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
    implementation 'androidx.palette:palette:1.0.0'
    // UI
    implementation 'androidx.viewpager2:viewpager2:1.0.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'androidx.recyclerview:recyclerview-selection:1.1.0'
}
EOF

# ════════════════════════════════════════════════════════════
# 7. UPDATE AndroidManifest
# ════════════════════════════════════════════════════════════
cat > app/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29"/>
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:name=".FountainApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.FountainPlay"
        android:largeHeap="true">

        <activity android:name=".MainActivity" android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".player.VideoPlayerActivity"
            android:configChanges="orientation|screenSize|keyboardHidden|smallestScreenSize|screenLayout"
            android:supportsPictureInPicture="true"
            android:exported="true"
            android:theme="@style/Theme.FountainPlay.Player">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="file" android:mimeType="video/*" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="content" android:mimeType="video/*" />
            </intent-filter>
        </activity>

        <activity android:name=".player.AudioPlayerActivity"
            android:configChanges="orientation|screenSize"
            android:exported="true"
            android:theme="@style/Theme.FountainPlay.Player">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="file" android:mimeType="audio/*" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="content" android:mimeType="audio/*" />
            </intent-filter>
        </activity>

        <!-- Sticky foreground service — survives app removal from recents -->
        <service android:name=".service.PlaybackService"
            android:exported="true"
            android:stopWithTask="false"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>

        <!-- Restart service after boot -->
        <receiver android:name=".service.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

    </application>
</manifest>
EOF

# Boot receiver
cat > $P/service/BootReceiver.java << 'EOF'
package com.fountainpdl.fountainplay.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        // Optionally restart service on boot if was playing
    }
}
EOF

echo ""
echo "✅ PART A DONE — run fp_v3B.sh next"
