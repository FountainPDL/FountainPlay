package com.fountainpdl.fountainplay.player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.QueueAdapter;
import com.fountainpdl.fountainplay.db.AppDatabase;
import com.fountainpdl.fountainplay.db.dao.PlaylistDao;
import com.fountainpdl.fountainplay.db.entity.PlaylistEntity;
import com.fountainpdl.fountainplay.db.entity.PlaylistSong;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.service.PlaybackService;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.MediaUris;
import com.fountainpdl.fountainplay.util.PlayQueue;
import com.fountainpdl.fountainplay.util.PlaybackState;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.Collections;
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
    private final Handler ui = new Handler(Looper.getMainLooper());

    // Views
    private ViewPager2 viewPager;
    private ImageView ivBgBlur;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime, tvQueueInfo;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnBack, btnShuffle, btnRepeat, btnFavorite, btnOptions;
    private Chip chipSpeed, chipSleep, chipShare;
    private View dot0, dot1, dot2;
    private boolean isUserSeeking = false;
    private boolean isFavourite = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder b) {
            service = ((PlaybackService.LocalBinder) b).getService();
            bound = true;
            startRequestedTrack();
            syncUI();
        }
        @Override public void onServiceDisconnected(ComponentName name) { bound = false; }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);
        prefs = new AppPreferences(this);

        bindViews();
        applyWindowInsets();
        setupViewPager();
        setupControls();

        PlaybackState.get().addListener(this);
        PlayQueue.get().addListener(this);

        Intent svc = new Intent(this, PlaybackService.class);
        startService(svc);
        bindService(svc, connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (bound) { startRequestedTrack(); syncUI(); }
    }

    /** Edge-to-edge: keep the top bar and the controls clear of the status and navigation bars. */
    private void applyWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);

        final View topBar = findViewById(R.id.top_bar);
        final View bottom = findViewById(R.id.bottom_controls);
        final int topPad = topBar.getPaddingTop();
        final int bottomPad = bottom.getPaddingBottom();
        View content = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout());
            topBar.setPadding(topBar.getPaddingLeft(), topPad + i.top, topBar.getPaddingRight(), topBar.getPaddingBottom());
            bottom.setPadding(bottom.getPaddingLeft(), bottom.getPaddingTop(), bottom.getPaddingRight(), bottomPad + i.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
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

    /**
     * Works out which track this launch asked for and makes sure it is playing exactly once.
     * - opened from the mini player / notification: just show the current track
     * - opened from a list: the list already filled the queue, the service is already playing it
     * - opened from another app (VIEW intent): build a one-item queue
     */
    private void startRequestedTrack() {
        Intent in = getIntent();
        if (in == null || in.getBooleanExtra(EXTRA_RESUME, false)) return;

        String uriStr = in.getStringExtra(EXTRA_URI);
        MediaItem target;
        if (uriStr == null && in.getData() != null) {
            Uri d = in.getData();
            uriStr = d.toString();
            String name = d.getLastPathSegment();
            target = new MediaItem(0, name, null, uriStr, 0, MediaItem.TYPE_AUDIO);
        } else if (uriStr != null) {
            MediaItem cur = PlayQueue.get().current();
            if (cur != null && uriStr.equals(cur.getPath())) {
                target = cur;
            } else {
                target = new MediaItem(0, in.getStringExtra(EXTRA_TITLE),
                    in.getStringExtra(EXTRA_ARTIST), uriStr, 0, MediaItem.TYPE_AUDIO);
                target.setAlbumArtUri(in.getStringExtra(EXTRA_ALBUM_ART));
            }
        } else {
            return;
        }

        MediaItem cur = PlayQueue.get().current();
        if (cur != null && target.getPath() != null && target.getPath().equals(cur.getPath())) {
            service.ensurePlaying(cur);
        } else {
            // Queue listener (the service) starts it.
            PlayQueue.get().setQueue(Collections.singletonList(target), 0);
        }
        // Handled: a later configuration change must not restart the track.
        in.putExtra(EXTRA_RESUME, true);
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
        btnBack.setOnClickListener(v -> finish()); // playback continues in the service

        btnPlayPause.setOnClickListener(v -> { if (bound) { service.playPause(); pulse(btnPlayPause); }});
        btnNext.setOnClickListener(v -> { if (bound) service.skipNext(); });
        btnPrev.setOnClickListener(v -> { if (bound) service.skipPrevious(); });

        btnShuffle.setOnClickListener(v -> {
            PlayQueue.get().setShuffle(!PlayQueue.get().isShuffle());
            updateShuffleUI();
        });
        btnRepeat.setOnClickListener(v -> { PlayQueue.get().cycleRepeat(); updateRepeatUI(); });

        btnFavorite.setOnClickListener(v -> {
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null) toggleFavourite(cur);
        });

        btnOptions.setOnClickListener(v -> {
            final MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur == null) return;
            String[] opts = {"Play Next", "Add to Queue", "Add to Playlist", "Share", "File Info"};
            new AlertDialog.Builder(this).setTitle(cur.getTitle())
                .setItems(opts, (d, i) -> {
                    switch (i) {
                        case 0:
                            PlayQueue.get().addNext(cur);
                            toast("Will play next");
                            break;
                        case 1:
                            PlayQueue.get().addToQueue(cur);
                            toast("Added to queue");
                            break;
                        case 2: showAddToPlaylistDialog(cur); break;
                        case 3: shareAudio(cur); break;
                        case 4: showFileInfo(cur); break;
                    }
                }).show();
        });

        chipSpeed.setText(prefs.getPlaybackSpeed() + "×");
        chipSpeed.setOnClickListener(v -> {
            if (!bound) return;
            final String[] opts = {"0.25×","0.5×","0.75×","1.0×","1.25×","1.5×","1.75×","2.0×","3.0×"};
            final float[] vals  = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f};
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
                })
                .setNegativeButton("Cancel", null).show();
        });

        chipSleep.setOnClickListener(v -> {
            if (!bound) return;
            final String[] opts = {"5 min","10 min","15 min","30 min","45 min","1 hour","Cancel timer"};
            final long[] ms = {5*60000L,10*60000L,15*60000L,30*60000L,45*60000L,60*60000L,0};
            new AlertDialog.Builder(this).setTitle("Sleep Timer").setItems(opts, (d, i) -> {
                service.setSleepTimer(ms[i]);
                chipSleep.setText(ms[i] > 0 ? opts[i] : "Sleep");
                toast(ms[i] > 0 ? "Music will pause in " + opts[i] : "Sleep timer cancelled");
            }).show();
        });

        chipShare.setOnClickListener(v -> {
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null) shareAudio(cur);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int prog, boolean fromUser) {
                if (fromUser && bound && service.getDuration() > 0)
                    tvCurrentTime.setText(fmt((long)(prog / 1000f * service.getDuration())));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                if (bound && service.getDuration() > 0)
                    service.seekTo((long)(sb.getProgress() / 1000f * service.getDuration()));
            }
        });
    }

    // ── Favourites / playlists ──────────────────────────────────────────────────

    private void toggleFavourite(final MediaItem item) {
        final Context app = getApplicationContext();
        new Thread(() -> {
            PlaylistDao dao = AppDatabase.get(app).playlistDao();
            PlaylistEntity fav = null;
            for (PlaylistEntity p : dao.getAllPlaylists()) {
                if ("Favourites".equals(p.name)) { fav = p; break; }
            }
            if (fav == null) {
                PlaylistEntity n = new PlaylistEntity();
                n.name = "Favourites";
                n.createdAt = System.currentTimeMillis();
                n.id = (int) dao.insertPlaylist(n);
                fav = n;
            }
            boolean already = false;
            for (PlaylistSong s : dao.getSongsForPlaylist(fav.id)) {
                if (item.getPath() != null && item.getPath().equals(s.path)) { already = true; break; }
            }
            final boolean nowFav;
            if (already) {
                dao.removeSong(fav.id, item.getPath());
                nowFav = false;
            } else {
                PlaylistSong s = new PlaylistSong();
                s.playlistId = fav.id; s.path = item.getPath(); s.title = item.getTitle();
                s.artist = item.getArtist(); s.albumArtUri = item.getAlbumArtUri();
                s.duration = item.getDuration();
                dao.insertSong(s);
                nowFav = true;
            }
            dao.updateCount(fav.id);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                isFavourite = nowFav;
                updateFavouriteUI();
                toast(nowFav ? "Added to Favourites" : "Removed from Favourites");
            });
        }).start();
    }

    private void refreshFavouriteState(final MediaItem item) {
        final Context app = getApplicationContext();
        new Thread(() -> {
            boolean fav = false;
            try {
                PlaylistDao dao = AppDatabase.get(app).playlistDao();
                for (PlaylistEntity p : dao.getAllPlaylists()) {
                    if (!"Favourites".equals(p.name)) continue;
                    for (PlaylistSong s : dao.getSongsForPlaylist(p.id)) {
                        if (item.getPath() != null && item.getPath().equals(s.path)) { fav = true; break; }
                    }
                }
            } catch (Exception ignored) { }
            final boolean result = fav;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                isFavourite = result;
                updateFavouriteUI();
            });
        }).start();
    }

    private void updateFavouriteUI() {
        btnFavorite.setColorFilter(isFavourite ? 0xFFE53935 : 0xFFA89BC2);
    }

    private void showAddToPlaylistDialog(final MediaItem item) {
        final Context app = getApplicationContext();
        new Thread(() -> {
            final List<PlaylistEntity> playlists = AppDatabase.get(app).playlistDao().getAllPlaylists();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (playlists.isEmpty()) { toast("No playlists yet. Create one in Library."); return; }
                String[] names = new String[playlists.size()];
                for (int i = 0; i < playlists.size(); i++) names[i] = playlists.get(i).name;
                new AlertDialog.Builder(this).setTitle("Add to Playlist")
                    .setItems(names, (d, i) -> addToPlaylist(item, playlists.get(i))).show();
            });
        }).start();
    }

    private void addToPlaylist(final MediaItem item, final PlaylistEntity playlist) {
        final Context app = getApplicationContext();
        new Thread(() -> {
            PlaylistDao dao = AppDatabase.get(app).playlistDao();
            PlaylistSong s = new PlaylistSong();
            s.playlistId = playlist.id; s.path = item.getPath(); s.title = item.getTitle();
            s.artist = item.getArtist(); s.albumArtUri = item.getAlbumArtUri();
            s.duration = item.getDuration();
            dao.insertSong(s);
            dao.updateCount(playlist.id);
            runOnUiThread(() -> { if (!isFinishing()) toast("Added to " + playlist.name); });
        }).start();
    }

    // ── Misc actions ────────────────────────────────────────────────────────────

    private void shareAudio(MediaItem item) {
        Intent chooser = MediaUris.shareIntent(this, item);
        if (chooser == null) { toast("This file can't be shared"); return; }
        startActivity(chooser);
    }

    private void showFileInfo(MediaItem item) {
        new AlertDialog.Builder(this).setTitle("File Info")
            .setMessage("Title: " + item.getTitle() + "\nArtist: " + item.getArtist()
                + "\nAlbum: " + item.getAlbum() + "\nDuration: " + item.getFormattedDuration()
                + "\nSize: " + (item.getSize() / 1024 / 1024) + " MB\nPath: " + item.getPath())
            .setPositiveButton("OK", null).show();
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    // ── UI sync ─────────────────────────────────────────────────────────────────

    private void syncUI() {
        MediaItem cur = PlaybackState.get().getCurrentItem();
        if (cur != null) updateTrackUI(cur);
        btnPlayPause.setImageResource(bound && service.isPlaying()
            ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        updateShuffleUI(); updateRepeatUI(); updateQueueInfo();
        if (bound && service.getDuration() > 0) tvTotalTime.setText(fmt(service.getDuration()));
        if (bound && service.getSleepRemaining() == 0) chipSleep.setText("Sleep");
    }

    private void updateTrackUI(MediaItem item) {
        tvTitle.setText(item.getTitle());
        tvArtist.setText(item.getArtist());
        tvTitle.setSelected(true);
        String art = item.getAlbumArtUri();
        Glide.with(this).load(art).centerCrop().placeholder(R.drawable.bg_play_button).into(ivBgBlur);
        Fragment f = getSupportFragmentManager().findFragmentByTag("f0");
        if (f instanceof NowPlayingPageFragment) ((NowPlayingPageFragment) f).updateArt(art);
        refreshFavouriteState(item);
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
        tvQueueInfo.setText(total > 1 ? (idx + 1) + " / " + total : "Now Playing");
    }

    @Override public void onItemChanged(MediaItem item) {
        runOnUiThread(() -> { if (item != null && !isDestroyed()) updateTrackUI(item); });
    }
    @Override public void onPlayStateChanged(boolean p) {
        runOnUiThread(() -> {
            btnPlayPause.setImageResource(p ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            if (!p && bound && service.getSleepRemaining() == 0) chipSleep.setText("Sleep");
        });
    }
    @Override public void onPositionChanged(long pos, long dur) {
        runOnUiThread(() -> {
            if (!isUserSeeking && dur > 0) {
                seekBar.setProgress((int) (pos * 1000 / dur));
                tvCurrentTime.setText(fmt(pos));
                tvTotalTime.setText(fmt(dur));
            }
        });
    }
    @Override public void onQueueChanged() { runOnUiThread(() -> { updateQueueInfo(); updateRepeatUI(); updateShuffleUI(); }); }
    @Override public void onTrackChanged(MediaItem item, int idx) {
        runOnUiThread(() -> { if (item != null && !isDestroyed()) updateTrackUI(item); updateQueueInfo(); });
    }

    private void pulse(View v) {
        ScaleAnimation a = new ScaleAnimation(1f, 1.2f, 1f, 1.2f,
            Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        a.setDuration(80); a.setRepeatCount(1); a.setRepeatMode(Animation.REVERSE);
        v.startAnimation(a);
    }

    private String fmt(long ms) {
        if (ms <= 0) return "0:00";
        long s = ms / 1000, m = s / 60, h = m / 60; s %= 60; m %= 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        ui.removeCallbacksAndMessages(null);
        PlaybackState.get().removeListener(this);
        PlayQueue.get().removeListener(this);
        if (bound) { unbindService(connection); bound = false; }
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
            if (albumArt == null || url == null || !isAdded()) return;
            albumArt.animate().alpha(0).setDuration(150).withEndAction(() -> {
                if (!isAdded()) return;
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

    /** "Up Next": tap a row to jump to it (forward or backward). Refreshes when the queue changes. */
    public static class QueuePageFragment extends Fragment implements PlayQueue.Listener {
        private RecyclerView rv;
        private TextView tvCount;

        @Override public View onCreateView(@NonNull android.view.LayoutInflater inf,
                android.view.ViewGroup c, Bundle s) {
            View v = inf.inflate(R.layout.fragment_player_queue, c, false);
            rv = v.findViewById(R.id.rv_queue);
            tvCount = v.findViewById(R.id.tv_queue_count);
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            bindQueue();
            return v;
        }

        @Override public void onStart() {
            super.onStart();
            PlayQueue.get().addListener(this);
            bindQueue();
        }

        @Override public void onStop() {
            super.onStop();
            PlayQueue.get().removeListener(this);
        }

        private void bindQueue() {
            if (rv == null) return;
            List<MediaItem> queue = new ArrayList<>(PlayQueue.get().getQueue());
            int curIdx = PlayQueue.get().getIndex();
            tvCount.setText(queue.size() + (queue.size() == 1 ? " song" : " songs"));
            rv.setAdapter(new QueueAdapter(queue, curIdx, item -> {
                int idx = PlayQueue.get().getQueue().indexOf(item);
                if (idx >= 0) PlayQueue.get().jumpTo(idx);
            }));
            rv.scrollToPosition(Math.max(0, curIdx - 2));
        }

        private void refresh() {
            if (getActivity() != null && isAdded()) getActivity().runOnUiThread(this::bindQueue);
        }

        @Override public void onQueueChanged() { refresh(); }
        @Override public void onTrackChanged(MediaItem item, int index) { refresh(); }
    }
}
