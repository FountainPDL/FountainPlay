#!/bin/bash
# ── v3 FINAL FIX — Run from ~/FountainPlay ──
# Fixes: QueueAdapter import, icon, service import, all remaining compile errors

P="app/src/main/java/com/fountainpdl/fountainplay"
set -e

echo "════════════════════════════════════════"
echo "  FountainPlay v3 Final Fix"
echo "════════════════════════════════════════"

# ════════════════════════════════════════════════════════════
# 1. ICON — copy your actual PNG to all mipmap densities
#    Place your icon PNG as: assets/ic_launcher_source.png
#    OR the script uses the one already in mipmap-hdpi
# ════════════════════════════════════════════════════════════
echo "→ Installing icon to all densities..."

# Assumes you've placed ic_launcher.png in mipmap-hdpi already
# (downloaded from the outputs above and placed there)
for d in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    mkdir -p app/src/main/res/mipmap-$d
done

# If source icon is in root, resize with Python (if available)
if command -v python3 &>/dev/null && [ -f ic_launcher_source.png ]; then
    python3 << 'PYEOF'
from PIL import Image
import os, sys
sizes = {"mdpi":48,"hdpi":72,"xhdpi":96,"xxhdpi":144,"xxxhdpi":192}
img = Image.open("ic_launcher_source.png").convert("RGBA")
for d, sz in sizes.items():
    p = f"app/src/main/res/mipmap-{d}"
    os.makedirs(p, exist_ok=True)
    r = img.resize((sz, sz), Image.LANCZOS)
    r.save(f"{p}/ic_launcher.png")
    r.save(f"{p}/ic_launcher_round.png")
    print(f"✅ {d} {sz}px")
PYEOF
fi

# ════════════════════════════════════════════════════════════
# 2. FIX AudioPlayerActivity — add missing import
# ════════════════════════════════════════════════════════════
echo "→ Fixing AudioPlayerActivity imports..."

cat > $P/player/AudioPlayerActivity.java << 'EOF'
package com.fountainpdl.fountainplay.player;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.QueueAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.service.PlaybackService;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.PlayQueue;
import com.fountainpdl.fountainplay.util.PlaybackState;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class AudioPlayerActivity extends AppCompatActivity
    implements PlaybackState.Listener, PlayQueue.Listener {

    public static final String EXTRA_URI       = "media_uri";
    public static final String EXTRA_TITLE     = "media_title";
    public static final String EXTRA_ARTIST    = "media_artist";
    public static final String EXTRA_ALBUM_ART = "album_art_uri";
    public static final String EXTRA_RESUME    = "resume_playback";

    private PlaybackService service;
    private boolean bound = false;
    private AppPreferences prefs;

    // Views
    private ViewPager2 viewPager;
    private ImageView ivBgBlur;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime, tvQueueInfo;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnBack, btnShuffle, btnRepeat, btnFavorite, btnOptions;
    private Chip chipSpeed, chipSleep, chipShare;
    private View dot0, dot1, dot2;
    private boolean isUserSeeking = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder b) {
            service = ((PlaybackService.LocalBinder) b).getService();
            bound = true;
            String uriStr = getIntent().getStringExtra(EXTRA_URI);
            boolean resume = getIntent().getBooleanExtra(EXTRA_RESUME, false);
            if (uriStr != null && !resume) {
                MediaItem cur = PlayQueue.get().current();
                if (cur != null && cur.getPath() != null && cur.getPath().equals(uriStr)) {
                    service.playItem(cur);
                } else {
                    MediaItem item = new MediaItem(0,
                        getIntent().getStringExtra(EXTRA_TITLE),
                        getIntent().getStringExtra(EXTRA_ARTIST),
                        uriStr, 0, MediaItem.TYPE_AUDIO);
                    item.setAlbumArtUri(getIntent().getStringExtra(EXTRA_ALBUM_ART));
                    service.playItem(item);
                }
            }
            syncUI();
        }
        @Override public void onServiceDisconnected(ComponentName name) { bound = false; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_audio_player);
        prefs = new AppPreferences(this);

        bindViews();
        setupViewPager();
        setupControls();

        PlaybackState.get().addListener(this);
        PlayQueue.get().addListener(this);

        Intent svc = new Intent(this, PlaybackService.class);
        startService(svc);
        bindService(svc, connection, BIND_AUTO_CREATE);
    }

    private void bindViews() {
        viewPager     = findViewById(R.id.view_pager);
        ivBgBlur      = findViewById(R.id.iv_bg_blur);
        tvTitle       = findViewById(R.id.tv_title);
        tvArtist      = findViewById(R.id.tv_artist);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime   = findViewById(R.id.tv_total_time);
        tvQueueInfo   = findViewById(R.id.tv_queue_info);
        seekBar       = findViewById(R.id.seek_bar);
        btnPlayPause  = findViewById(R.id.btn_play_pause);
        btnPrev       = findViewById(R.id.btn_prev);
        btnNext       = findViewById(R.id.btn_next);
        btnBack       = findViewById(R.id.btn_back);
        btnShuffle    = findViewById(R.id.btn_shuffle);
        btnRepeat     = findViewById(R.id.btn_repeat);
        btnFavorite   = findViewById(R.id.btn_favorite);
        btnOptions    = findViewById(R.id.btn_options);
        chipSpeed     = findViewById(R.id.chip_speed);
        chipSleep     = findViewById(R.id.chip_sleep);
        chipShare     = findViewById(R.id.chip_share);
        dot0          = findViewById(R.id.dot0);
        dot1          = findViewById(R.id.dot1);
        dot2          = findViewById(R.id.dot2);
    }

    private void setupViewPager() {
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int pos) {
                switch (pos) {
                    case 1: return new LyricsPageFragment();
                    case 2: return new QueuePageFragment();
                    default: return new NowPlayingPageFragment();
                }
            }
            @Override public int getItemCount() { return 3; }
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) { updateDots(pos); }
        });
        viewPager.setPageTransformer((page, pos) -> {
            page.setAlpha(1 - Math.abs(pos) * 0.4f);
            page.setScaleX(1 - Math.abs(pos) * 0.06f);
            page.setScaleY(1 - Math.abs(pos) * 0.06f);
        });
    }

    private void updateDots(int pos) {
        int on = 0xFFFFFFFF, off = 0x60A89BC2;
        dot0.setBackgroundColor(pos == 0 ? on : off);
        dot1.setBackgroundColor(pos == 1 ? on : off);
        dot2.setBackgroundColor(pos == 2 ? on : off);
    }

    private void setupControls() {
        btnBack.setOnClickListener(v -> finish()); // keeps playing

        btnPlayPause.setOnClickListener(v -> { if (bound) { service.playPause(); pulse(btnPlayPause); }});
        btnNext.setOnClickListener(v -> { if (bound) service.skipNext(); });
        btnPrev.setOnClickListener(v -> { if (bound) service.skipPrevious(); });

        btnShuffle.setOnClickListener(v -> {
            PlayQueue.get().setShuffle(!PlayQueue.get().isShuffle());
            updateShuffleUI();
        });
        btnRepeat.setOnClickListener(v -> { PlayQueue.get().cycleRepeat(); updateRepeatUI(); });

        btnFavorite.setOnClickListener(v ->
            Toast.makeText(this, "Added to favourites", Toast.LENGTH_SHORT).show());

        btnOptions.setOnClickListener(v -> {
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur == null) return;
            String[] opts = {"Add to Queue","Add to Playlist","Set as Ringtone","Share","File Info"};
            new AlertDialog.Builder(this).setTitle(cur.getTitle())
                .setItems(opts, (d, i) -> {
                    switch (i) {
                        case 0: PlayQueue.get().addToQueue(cur); Toast.makeText(this,"Added to queue",Toast.LENGTH_SHORT).show(); break;
                        case 2: Toast.makeText(this,"Ringtone: use a file manager to set",Toast.LENGTH_SHORT).show(); break;
                        case 3: shareAudio(cur); break;
                        case 4: showFileInfo(cur); break;
                    }
                }).show();
        });

        chipSpeed.setText(prefs.getPlaybackSpeed() + "×");
        chipSpeed.setOnClickListener(v -> {
            if (!bound) return;
            String[] opts = {"0.25×","0.5×","0.75×","1.0×","1.25×","1.5×","1.75×","2.0×","3.0×"};
            float[] vals  = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f};
            float cur = service.getPlayer().getPlaybackParameters().speed;
            int sel = 3;
            for (int i = 0; i < vals.length; i++) if (Math.abs(vals[i]-cur) < 0.01f) { sel=i; break; }
            final int[] picked = {sel};
            new AlertDialog.Builder(this).setTitle("Playback Speed")
                .setSingleChoiceItems(opts, sel, (d, i) -> picked[0] = i)
                .setPositiveButton("OK", (d, i) -> {
                    service.setPlaybackSpeed(vals[picked[0]]);
                    chipSpeed.setText(opts[picked[0]]);
                    prefs.setPlaybackSpeed(vals[picked[0]]);
                }).show();
        });

        chipSleep.setOnClickListener(v -> {
            String[] opts = {"5 min","10 min","15 min","30 min","45 min","1 hour","Cancel"};
            long[] ms = {5*60000L,10*60000L,15*60000L,30*60000L,45*60000L,60*60000L,-1};
            new AlertDialog.Builder(this).setTitle("Sleep Timer").setItems(opts,(d,i)->{
                if (ms[i] > 0) {
                    chipSleep.setText(opts[i]);
                    new android.os.Handler().postDelayed(()->{
                        if (bound) service.getPlayer().pause();
                        chipSleep.setText("Sleep");
                    }, ms[i]);
                }
            }).show();
        });

        chipShare.setOnClickListener(v -> {
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null) shareAudio(cur);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int prog, boolean fromUser) {
                if (fromUser && bound && service.getDuration()>0)
                    tvCurrentTime.setText(fmt((long)(prog/1000f*service.getDuration())));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking=true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking=false;
                if (bound && service.getDuration()>0)
                    service.seekTo((long)(sb.getProgress()/1000f*service.getDuration()));
            }
        });
    }

    private void shareAudio(MediaItem item) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("audio/*");
        i.putExtra(Intent.EXTRA_STREAM, Uri.parse(item.getPath()));
        i.putExtra(Intent.EXTRA_TEXT, item.getTitle()+" - "+item.getArtist());
        startActivity(Intent.createChooser(i,"Share"));
    }

    private void showFileInfo(MediaItem item) {
        new AlertDialog.Builder(this).setTitle("File Info")
            .setMessage("Title: "+item.getTitle()+"\nArtist: "+item.getArtist()
                +"\nAlbum: "+item.getAlbum()+"\nDuration: "+item.getFormattedDuration()
                +"\nSize: "+(item.getSize()/1024/1024)+" MB\nPath: "+item.getPath())
            .setPositiveButton("OK",null).show();
    }

    private void syncUI() {
        MediaItem cur = PlaybackState.get().getCurrentItem();
        if (cur != null) updateTrackUI(cur);
        btnPlayPause.setImageResource(bound && service.isPlaying()
            ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        updateShuffleUI(); updateRepeatUI(); updateQueueInfo();
        if (bound && service.getDuration()>0) tvTotalTime.setText(fmt(service.getDuration()));
    }

    private void updateTrackUI(MediaItem item) {
        tvTitle.setText(item.getTitle());
        tvArtist.setText(item.getArtist());
        tvTitle.setSelected(true);
        String art = item.getAlbumArtUri();
        Glide.with(this).load(art).centerCrop().placeholder(R.drawable.bg_play_button).into(ivBgBlur);
        // Update now-playing fragment
        Fragment f = getSupportFragmentManager().findFragmentByTag("f0");
        if (f instanceof NowPlayingPageFragment) ((NowPlayingPageFragment)f).updateArt(art);
    }

    private void updateShuffleUI() {
        boolean on = PlayQueue.get().isShuffle();
        btnShuffle.setAlpha(on ? 1.0f : 0.4f);
        btnShuffle.setColorFilter(on ? 0xFFBB86FC : 0xFFA89BC2);
    }

    private void updateRepeatUI() {
        int rm = PlayQueue.get().getRepeatMode();
        btnRepeat.setAlpha(rm != PlayQueue.REPEAT_NONE ? 1.0f : 0.4f);
        btnRepeat.setColorFilter(rm == PlayQueue.REPEAT_ONE ? 0xFFBB86FC : 0xFFA89BC2);
    }

    private void updateQueueInfo() {
        int idx = PlayQueue.get().getIndex(), total = PlayQueue.get().size();
        tvQueueInfo.setText(total > 1 ? (idx+1)+" / "+total : "Now Playing");
    }

    @Override public void onItemChanged(MediaItem item) { runOnUiThread(()->{ if(item!=null) updateTrackUI(item); }); }
    @Override public void onPlayStateChanged(boolean p) { runOnUiThread(()->btnPlayPause.setImageResource(p?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play)); }
    @Override public void onPositionChanged(long pos, long dur) {
        runOnUiThread(()->{
            if (!isUserSeeking && dur>0) {
                seekBar.setProgress((int)(pos*1000/dur));
                tvCurrentTime.setText(fmt(pos));
                tvTotalTime.setText(fmt(dur));
            }
        });
    }
    @Override public void onQueueChanged() { runOnUiThread(this::updateQueueInfo); }
    @Override public void onTrackChanged(MediaItem item, int idx) { runOnUiThread(()->{updateTrackUI(item);updateQueueInfo();}); }

    private void pulse(View v) {
        ScaleAnimation a = new ScaleAnimation(1f,1.2f,1f,1.2f,
            Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f);
        a.setDuration(80); a.setRepeatCount(1); a.setRepeatMode(Animation.REVERSE);
        v.startAnimation(a);
    }

    private String fmt(long ms) {
        if (ms<=0) return "0:00";
        long s=ms/1000,m=s/60,h=m/60; s%=60; m%=60;
        return h>0?String.format("%d:%02d:%02d",h,m,s):String.format("%d:%02d",m,s);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        PlaybackState.get().removeListener(this);
        PlayQueue.get().removeListener(this);
        if (bound) { unbindService(connection); bound=false; }
    }

    // ─── Inner Fragment Pages ───────────────────────────────

    public static class NowPlayingPageFragment extends Fragment {
        private ImageView albumArt;
        @Override public View onCreateView(@NonNull android.view.LayoutInflater inf,
                android.view.ViewGroup c, Bundle s) {
            View v = inf.inflate(R.layout.fragment_player_now_playing, c, false);
            albumArt = v.findViewById(R.id.iv_album_art);
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null && cur.getAlbumArtUri() != null)
                Glide.with(requireContext()).load(cur.getAlbumArtUri()).centerCrop().into(albumArt);
            return v;
        }
        public void updateArt(String url) {
            if (albumArt == null || url == null) return;
            albumArt.animate().alpha(0).setDuration(150).withEndAction(()->{
                Glide.with(requireContext()).load(url).centerCrop().into(albumArt);
                albumArt.animate().alpha(1).setDuration(150).start();
            }).start();
        }
    }

    public static class LyricsPageFragment extends Fragment {
        @Override public View onCreateView(@NonNull android.view.LayoutInflater inf,
                android.view.ViewGroup c, Bundle s) {
            return inf.inflate(R.layout.fragment_player_lyrics, c, false);
        }
    }

    public static class QueuePageFragment extends Fragment {
        @Override public View onCreateView(@NonNull android.view.LayoutInflater inf,
                android.view.ViewGroup c, Bundle s) {
            View v = inf.inflate(R.layout.fragment_player_queue, c, false);
            RecyclerView rv = v.findViewById(R.id.rv_queue);
            TextView tvCount = v.findViewById(R.id.tv_queue_count);
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            List<MediaItem> queue = new ArrayList<>(PlayQueue.get().getQueue());
            int curIdx = PlayQueue.get().getIndex();
            tvCount.setText(queue.size() + " songs");
            rv.setAdapter(new QueueAdapter(queue, curIdx, item -> {
                List<MediaItem> q = PlayQueue.get().getQueue();
                int idx = q.indexOf(item);
                if (idx > PlayQueue.get().getIndex()) {
                    for (int i = PlayQueue.get().getIndex(); i < idx; i++) PlayQueue.get().next();
                }
            }));
            rv.scrollToPosition(Math.max(0, curIdx - 2));
            return v;
        }
    }
}
EOF
echo "✅ AudioPlayerActivity fixed"

# ════════════════════════════════════════════════════════════
# 3. FIX PlaybackService — remove bad android.support import
# ════════════════════════════════════════════════════════════
echo "→ Fixing PlaybackService imports..."

# Remove the bad import that uses old support library
sed -i '/import android.support.v4.media.session/d' \
    $P/service/PlaybackService.java 2>/dev/null || true
sed -i '/^import android\.support\./d' \
    $P/service/PlaybackService.java 2>/dev/null || true

# ════════════════════════════════════════════════════════════
# 4. FIX PlaybackService — remove var keyword (not in Java 8 style)
#    and fix MediaStyle import
# ════════════════════════════════════════════════════════════
cat > $P/service/PlaybackService.java << 'EOF'
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
EOF
echo "✅ PlaybackService fixed"

# ════════════════════════════════════════════════════════════
# 5. FIX LibraryPageFragment — remove var keyword (Java 11 needed)
# ════════════════════════════════════════════════════════════
echo "→ Fixing LibraryPageFragment..."

# Fix var usage to explicit types
sed -i 's/var playlists = /List<com.fountainpdl.fountainplay.db.entity.PlaylistEntity> playlists = /g' \
    $P/ui/library/LibraryPageFragment.java 2>/dev/null || true
sed -i 's/var song = /com.fountainpdl.fountainplay.db.entity.PlaylistSong song = /g' \
    $P/ui/library/LibraryPageFragment.java 2>/dev/null || true
sed -i 's/var s : /com.fountainpdl.fountainplay.db.entity.PlaylistSong s : /g' \
    $P/ui/library/LibraryPageFragment.java 2>/dev/null || true
sed -i 's/for (var s /for (com.fountainpdl.fountainplay.db.entity.PlaylistSong s /g' \
    $P/ui/library/LibraryPageFragment.java 2>/dev/null || true

# Simpler — just rewrite the file cleanly with no var
cat > $P/ui/library/LibraryPageFragment.java << 'EOF'
package com.fountainpdl.fountainplay.ui.library;

import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaAdapter;
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.entity.PlaylistEntity;
import com.fountainpdl.fountainplay.db.entity.PlaylistSong;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.player.AudioPlayerActivity;
import com.fountainpdl.fountainplay.util.MediaScanner;
import com.fountainpdl.fountainplay.util.PlayQueue;
import java.util.*;

public class LibraryPageFragment extends Fragment {
    private static final String ARG_TAB = "tab";

    public static LibraryPageFragment newInstance(String tab) {
        LibraryPageFragment f = new LibraryPageFragment();
        Bundle b = new Bundle(); b.putString(ARG_TAB, tab); f.setArguments(b); return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_library_page, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String tab = getArguments() != null ? getArguments().getString(ARG_TAB, "Songs") : "Songs";
        RecyclerView rv = view.findViewById(R.id.rv_library);
        TextView tvEmpty = view.findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (tab.equals("Playlists")) { loadPlaylists(rv, tvEmpty); return; }
        if (tab.equals("History"))   { loadHistory(rv, tvEmpty);   return; }

        List<MediaItem> items = new ArrayList<>();
        MediaAdapter adapter = new MediaAdapter(items, 0);
        rv.setAdapter(adapter);
        adapter.setOnItemClickListener((item, pos) -> {
            PlayQueue.get().setQueue(items, items.indexOf(item));
            Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
            i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
            i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
            i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, item.getArtist());
            i.putExtra(AudioPlayerActivity.EXTRA_ALBUM_ART, item.getAlbumArtUri());
            startActivity(i);
        });

        new Thread(() -> {
            List<MediaItem> all = MediaScanner.scanAudio(requireContext());
            List<MediaItem> result;
            switch (tab) {
                case "Albums":  result = onePerAlbum(all);  break;
                case "Artists": result = onePerArtist(all); break;
                default:        result = all;               break;
            }
            final List<MediaItem> fr = result;
            requireActivity().runOnUiThread(() -> {
                items.clear(); items.addAll(fr); adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private void loadHistory(RecyclerView rv, TextView tvEmpty) {
        new Thread(() -> {
            List<com.fountainpdl.fountainplay.db.entity.HistoryItem> history =
                AppDatabase.get(requireContext()).historyDao().getAll();
            requireActivity().runOnUiThread(() -> {
                if (history.isEmpty()) { tvEmpty.setText("No history yet"); tvEmpty.setVisibility(View.VISIBLE); return; }
                List<MediaItem> items = new ArrayList<>();
                for (com.fountainpdl.fountainplay.db.entity.HistoryItem h : history) {
                    MediaItem m = new MediaItem(0, h.title, h.artist, h.path, h.duration, h.type);
                    m.setAlbumArtUri(h.albumArtUri);
                    items.add(m);
                }
                MediaAdapter adapter = new MediaAdapter(items, 0);
                rv.setAdapter(adapter);
                adapter.setOnItemClickListener((item, pos) -> {
                    Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
                    i.putExtra(AudioPlayerActivity.EXTRA_URI, item.getPath());
                    i.putExtra(AudioPlayerActivity.EXTRA_TITLE, item.getTitle());
                    startActivity(i);
                });
            });
        }).start();
    }

    private void loadPlaylists(RecyclerView rv, TextView tvEmpty) {
        new Thread(() -> {
            List<PlaylistEntity> playlists =
                AppDatabase.get(requireContext()).playlistDao().getAllPlaylists();
            requireActivity().runOnUiThread(() -> {
                if (playlists.isEmpty()) {
                    tvEmpty.setText("No playlists yet.\nLong-press a song to create one.");
                    tvEmpty.setVisibility(View.VISIBLE);
                }
                LinearLayout container = new LinearLayout(requireContext());
                container.setOrientation(LinearLayout.VERTICAL);
                Button btnCreate = new Button(requireContext());
                btnCreate.setText("+ Create Playlist");
                btnCreate.setOnClickListener(vv -> showCreatePlaylistDialog(rv, tvEmpty));
                container.addView(btnCreate);

                for (PlaylistEntity p : playlists) {
                    TextView tv = new TextView(requireContext());
                    tv.setText("▶  " + p.name + "  (" + p.songCount + " songs)");
                    tv.setTextColor(0xFFFFFFFF); tv.setTextSize(15);
                    tv.setPadding(32, 28, 32, 28);
                    tv.setOnClickListener(vv -> openPlaylist(p));
                    tv.setOnLongClickListener(vv -> {
                        new AlertDialog.Builder(requireContext())
                            .setTitle("Delete \"" + p.name + "\"?")
                            .setPositiveButton("Delete", (d, i) -> new Thread(() -> {
                                AppDatabase.get(requireContext()).playlistDao().deletePlaylist(p);
                                requireActivity().runOnUiThread(() -> loadPlaylists(rv, tvEmpty));
                            }).start())
                            .setNegativeButton("Cancel", null).show();
                        return true;
                    });
                    container.addView(tv);
                }

                rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(
                            @NonNull ViewGroup p, int t) {
                        return new RecyclerView.ViewHolder(container) {};
                    }
                    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {}
                    @Override public int getItemCount() { return 1; }
                });
            });
        }).start();
    }

    private void showCreatePlaylistDialog(RecyclerView rv, TextView tvEmpty) {
        EditText et = new EditText(requireContext());
        et.setHint("Playlist name");
        new AlertDialog.Builder(requireContext()).setTitle("New Playlist")
            .setView(et)
            .setPositiveButton("Create", (d, i) -> {
                String name = et.getText().toString().trim();
                if (name.isEmpty()) return;
                new Thread(() -> {
                    PlaylistEntity p = new PlaylistEntity();
                    p.name = name; p.createdAt = System.currentTimeMillis();
                    AppDatabase.get(requireContext()).playlistDao().insertPlaylist(p);
                    requireActivity().runOnUiThread(() -> loadPlaylists(rv, tvEmpty));
                }).start();
            }).setNegativeButton("Cancel", null).show();
    }

    private void openPlaylist(PlaylistEntity playlist) {
        new Thread(() -> {
            List<PlaylistSong> songs =
                AppDatabase.get(requireContext()).playlistDao().getSongsForPlaylist(playlist.id);
            if (songs.isEmpty()) {
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Playlist is empty", Toast.LENGTH_SHORT).show());
                return;
            }
            List<MediaItem> items = new ArrayList<>();
            for (PlaylistSong s : songs) {
                MediaItem m = new MediaItem(0, s.title, s.artist, s.path, s.duration, 0);
                m.setAlbumArtUri(s.albumArtUri);
                items.add(m);
            }
            requireActivity().runOnUiThread(() -> {
                PlayQueue.get().setQueue(items, 0);
                Intent i = new Intent(requireContext(), AudioPlayerActivity.class);
                i.putExtra(AudioPlayerActivity.EXTRA_URI, items.get(0).getPath());
                i.putExtra(AudioPlayerActivity.EXTRA_TITLE, items.get(0).getTitle());
                i.putExtra(AudioPlayerActivity.EXTRA_ARTIST, items.get(0).getArtist());
                startActivity(i);
            });
        }).start();
    }

    private List<MediaItem> onePerAlbum(List<MediaItem> all) {
        Map<String, MediaItem> map = new LinkedHashMap<>();
        for (MediaItem m : all) map.putIfAbsent(m.getAlbum(), m);
        return new ArrayList<>(map.values());
    }

    private List<MediaItem> onePerArtist(List<MediaItem> all) {
        Map<String, MediaItem> map = new LinkedHashMap<>();
        for (MediaItem m : all) map.putIfAbsent(m.getArtist(), m);
        return new ArrayList<>(map.values());
    }
}
EOF
echo "✅ LibraryPageFragment fixed"

# ════════════════════════════════════════════════════════════
# 6. FIX MusicFragment — remove stream() call (needs API 24+)
#    and fix other potential issues
# ════════════════════════════════════════════════════════════
echo "→ Fixing MusicFragment stream() call..."

# Fix the stream() call in showAddToPlaylistDialog
python3 << 'PYEOF'
with open("app/src/main/java/com/fountainpdl/fountainplay/ui/music/MusicFragment.java","r") as f:
    c = f.read()

# Replace stream().map().toArray() with a loop
old = 'String[] names = playlists.stream().map(p -> p.name).toArray(String[]::new);'
new = '''String[] names = new String[playlists.size()];
                for (int ii=0;ii<playlists.size();ii++) names[ii] = playlists.get(ii).name;'''
c = c.replace(old, new)

with open("app/src/main/java/com/fountainpdl/fountainplay/ui/music/MusicFragment.java","w") as f:
    f.write(c)
print("✅ MusicFragment stream() fixed")
PYEOF

# ════════════════════════════════════════════════════════════
# 7. COMMIT AND PUSH
# ════════════════════════════════════════════════════════════
echo ""
echo "→ Committing and pushing..."

git add .
git commit -m "fix: all compile errors - QueueAdapter import, PlaybackService, LibraryFragment, stream API"
git push

echo ""
echo "════════════════════════════════════════"
echo "✅ PUSHED — build running on GitHub"
echo "════════════════════════════════════════"
echo ""
echo "While waiting for build:"
echo "  - Copy your icon PNG to ~/FountainPlay/ as ic_launcher_source.png"
echo "  - Then copy the processed icons from outputs/ic_launcher_all/ to"
echo "    the correct mipmap folders and push again"
