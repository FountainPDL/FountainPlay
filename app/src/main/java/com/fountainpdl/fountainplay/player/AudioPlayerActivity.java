package com.fountainpdl.fountainplay.player;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.QueueAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.service.PlaybackService;
import com.fountainpdl.fountainplay.util.AppPreferences;
import com.fountainpdl.fountainplay.util.Favourites;
import com.fountainpdl.fountainplay.util.Fx;
import com.fountainpdl.fountainplay.util.MediaActions;
import com.fountainpdl.fountainplay.util.PlayQueue;
import com.fountainpdl.fountainplay.util.PlaybackState;
import com.fountainpdl.fountainplay.util.ThemeColors;
import com.fountainpdl.fountainplay.util.ThemeSetup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    private ViewPager2 viewPager;
    private View bgGradient, dot0, dot1, dot2;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime, tvQueueInfo;
    private TextView tvShuffleLabel, tvRepeatLabel, tvPlayLabel, tvSpeedLabel, tvSleepLabel, tvFavLabel;
    private SeekBar seekBar;
    private ImageButton btnPlayPause;
    private ImageView ivShuffle, ivRepeat, ivFavorite;
    private boolean isUserSeeking = false;
    private boolean isFavourite = false;
    private int backdropTop;
    private ValueAnimator backdropAnim;

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
        ThemeSetup.apply(this, true);
        setContentView(R.layout.activity_audio_player);
        prefs = new AppPreferences(this);

        bindViews();
        applyWindowInsets();
        setupViewPager();
        setupControls();
        backdropTop = ThemeColors.get(this, "colorSurface");

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
        boolean light = ThemeColors.isLight(ThemeColors.get(this, "colorSurface"));
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(light);
        c.setAppearanceLightNavigationBars(light);

        final View topBar = findViewById(R.id.top_bar);
        final View bottom = findViewById(R.id.bottom_controls);
        final int topPad = topBar.getPaddingTop();
        final int bottomPad = bottom.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets i = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            topBar.setPadding(topBar.getPaddingLeft(), topPad + i.top, topBar.getPaddingRight(), topBar.getPaddingBottom());
            bottom.setPadding(bottom.getPaddingLeft(), bottom.getPaddingTop(), bottom.getPaddingRight(), bottomPad + i.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void bindViews() {
        viewPager      = findViewById(R.id.view_pager);
        bgGradient     = findViewById(R.id.bg_gradient);
        tvTitle        = findViewById(R.id.tv_title);
        tvArtist       = findViewById(R.id.tv_artist);
        tvCurrentTime  = findViewById(R.id.tv_current_time);
        tvTotalTime    = findViewById(R.id.tv_total_time);
        tvQueueInfo    = findViewById(R.id.tv_queue_info);
        seekBar        = findViewById(R.id.seek_bar);
        btnPlayPause   = findViewById(R.id.btn_play_pause);
        ivShuffle      = findViewById(R.id.iv_shuffle);
        ivRepeat       = findViewById(R.id.iv_repeat);
        ivFavorite     = findViewById(R.id.iv_favorite);
        tvShuffleLabel = findViewById(R.id.tv_shuffle_label);
        tvRepeatLabel  = findViewById(R.id.tv_repeat_label);
        tvPlayLabel    = findViewById(R.id.tv_play_label);
        tvSpeedLabel   = findViewById(R.id.tv_speed_label);
        tvSleepLabel   = findViewById(R.id.tv_sleep_label);
        tvFavLabel     = findViewById(R.id.tv_favorite_label);
        dot0 = findViewById(R.id.dot0);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        tvTitle.setSelected(true);
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
            target = new MediaItem(0, d.getLastPathSegment(), null, uriStr, 0, MediaItem.TYPE_AUDIO);
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
            PlayQueue.get().setQueue(Collections.singletonList(target), 0);   // the service starts it
        }
        in.putExtra(EXTRA_RESUME, true);   // handled: a rotation must not restart the track
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
        updateDots(0);
    }

    private void updateDots(int pos) {
        dot0.setAlpha(pos == 0 ? 1f : 0.3f);
        dot1.setAlpha(pos == 1 ? 1f : 0.3f);
        dot2.setAlpha(pos == 2 ? 1f : 0.3f);
    }

    private void setupControls() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());   // playback continues in the service

        btnPlayPause.setOnClickListener(v -> { if (bound) { service.playPause(); Fx.pop(btnPlayPause); } });
        findViewById(R.id.btn_next).setOnClickListener(v -> { if (bound) service.skipNext(); });
        findViewById(R.id.btn_prev).setOnClickListener(v -> { if (bound) service.skipPrevious(); });

        findViewById(R.id.btn_shuffle).setOnClickListener(v -> {
            PlayQueue.get().setShuffle(!PlayQueue.get().isShuffle());
            updateShuffleUI();
        });
        findViewById(R.id.btn_repeat).setOnClickListener(v -> { PlayQueue.get().cycleRepeat(); updateRepeatUI(); });

        findViewById(R.id.btn_favorite).setOnClickListener(v -> {
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null) toggleFavourite(cur);
        });

        findViewById(R.id.btn_options).setOnClickListener(v -> {
            final MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur == null) return;
            PopupMenu pm = new PopupMenu(this, v);
            pm.getMenu().add(0, 1, 0, "Play next");
            pm.getMenu().add(0, 2, 1, "Add to queue");
            pm.getMenu().add(0, 3, 2, "Details");
            pm.setOnMenuItemClickListener(mi -> {
                if (mi.getItemId() == 1) { PlayQueue.get().addNext(cur); toast("Will play next"); }
                else if (mi.getItemId() == 2) { PlayQueue.get().addToQueue(cur); toast("Added to queue"); }
                else MediaActions.showDetails(this, cur);
                return true;
            });
            pm.show();
        });

        tvSpeedLabel.setText(speedLabel(prefs.getPlaybackSpeed()));
        findViewById(R.id.btn_speed).setOnClickListener(v -> pickSpeed());
        findViewById(R.id.btn_sleep).setOnClickListener(v -> pickSleep());
        findViewById(R.id.btn_queue).setOnClickListener(v -> viewPager.setCurrentItem(2, true));
        findViewById(R.id.btn_playlist).setOnClickListener(v -> {
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null) MediaActions.addToPlaylist(this, cur);
        });
        findViewById(R.id.btn_share).setOnClickListener(v -> {
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null) MediaActions.share(this, cur);
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int prog, boolean fromUser) {
                if (fromUser && bound && service.getDuration() > 0)
                    tvCurrentTime.setText(fmt((long) (prog / 1000f * service.getDuration())));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                if (bound && service.getDuration() > 0)
                    service.seekTo((long) (sb.getProgress() / 1000f * service.getDuration()));
            }
        });
    }

    private void pickSpeed() {
        if (!bound) return;
        final String[] opts = {"0.25×", "0.5×", "0.75×", "1.0×", "1.25×", "1.5×", "1.75×", "2.0×", "3.0×"};
        final float[] vals = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f};
        float cur = service.getSpeed();
        int sel = 3;
        for (int i = 0; i < vals.length; i++) if (Math.abs(vals[i] - cur) < 0.01f) { sel = i; break; }
        new MaterialAlertDialogBuilder(this).setTitle("Playback speed")
            .setSingleChoiceItems(opts, sel, (d, i) -> {
                service.setPlaybackSpeed(vals[i]);
                prefs.setPlaybackSpeed(vals[i]);
                tvSpeedLabel.setText(opts[i]);
                d.dismiss();
            }).show();
    }

    private void pickSleep() {
        if (!bound) return;
        final String[] opts = {"5 minutes", "10 minutes", "15 minutes", "30 minutes", "45 minutes", "1 hour", "Turn timer off"};
        final long[] ms = {5 * 60000L, 10 * 60000L, 15 * 60000L, 30 * 60000L, 45 * 60000L, 60 * 60000L, 0};
        new MaterialAlertDialogBuilder(this).setTitle("Sleep timer").setItems(opts, (d, i) -> {
            service.setSleepTimer(ms[i]);
            tvSleepLabel.setText(ms[i] > 0 ? opts[i].replace(" minutes", " min").replace("1 hour", "1 hr") : "Sleep");
            toast(ms[i] > 0 ? "Music will pause in " + opts[i] : "Sleep timer is off");
        }).show();
    }

    // ── Favourites ──────────────────────────────────────────────────────────────

    private void toggleFavourite(final MediaItem item) {
        final Context app = getApplicationContext();
        new Thread(() -> {
            final boolean now = Favourites.toggle(app, item);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                isFavourite = now;
                updateFavouriteUI();
                Fx.pop(ivFavorite);
                toast(now ? "Added to Favourites" : "Removed from Favourites");
            });
        }).start();
    }

    private void refreshFavouriteState(final MediaItem item) {
        final Context app = getApplicationContext();
        new Thread(() -> {
            boolean fav = false;
            try { fav = Favourites.isFavourite(app, item.getPath()); } catch (Exception ignored) { }
            final boolean result = fav;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                isFavourite = result;
                updateFavouriteUI();
            });
        }).start();
    }

    private void updateFavouriteUI() {
        ivFavorite.setImageResource(isFavourite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        tint(ivFavorite, isFavourite);
        tvFavLabel.setText(isFavourite ? "Liked" : "Like");
    }

    // ── UI sync ─────────────────────────────────────────────────────────────────

    private void tint(ImageView iv, boolean active) {
        int c = ThemeColors.get(this, active ? "colorPrimary" : "colorOnSurfaceVariant");
        ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(c));
    }

    private void syncUI() {
        MediaItem cur = PlaybackState.get().getCurrentItem();
        if (cur != null) updateTrackUI(cur);
        setPlayingUI(bound && service.isPlaying());
        updateShuffleUI();
        updateRepeatUI();
        updateQueueInfo();
        if (bound && service.getDuration() > 0) tvTotalTime.setText(fmt(service.getDuration()));
        if (bound && service.getSleepRemaining() == 0) tvSleepLabel.setText("Sleep");
    }

    private void setPlayingUI(boolean playing) {
        btnPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        tvPlayLabel.setText(playing ? "Pause" : "Play");
    }

    private void updateTrackUI(MediaItem item) {
        tvTitle.setText(item.getTitle());
        tvArtist.setText(item.getArtist());
        updateBackdrop(item.getAlbumArtUri());
        Fragment f = getSupportFragmentManager().findFragmentByTag("f0");
        if (f instanceof NowPlayingPageFragment) ((NowPlayingPageFragment) f).updateArt(item.getAlbumArtUri());
        refreshFavouriteState(item);
    }

    /** Tints the background with the album art's colour, fading from the previous one. */
    private void updateBackdrop(String art) {
        final int surface = ThemeColors.get(this, "colorSurface");
        final int fallback = ThemeColors.get(this, "colorPrimaryContainer");
        if (art == null) { animateBackdrop(fallback, surface); return; }
        Glide.with(this).asBitmap().load(art).into(new CustomTarget<Bitmap>(96, 96) {
            @Override public void onResourceReady(@NonNull Bitmap bmp, Transition<? super Bitmap> t) {
                Palette.from(bmp).generate(p -> {
                    int c = p == null ? fallback : p.getVibrantColor(p.getDominantColor(fallback));
                    animateBackdrop(c, surface);
                });
            }
            @Override public void onLoadFailed(Drawable errorDrawable) { animateBackdrop(fallback, surface); }
            @Override public void onLoadCleared(Drawable placeholder) { }
        });
    }

    private void animateBackdrop(int toTop, final int surface) {
        if (isDestroyed()) return;
        if (backdropAnim != null) backdropAnim.cancel();
        backdropAnim = ValueAnimator.ofObject(new ArgbEvaluator(), backdropTop, toTop);
        backdropAnim.setDuration(450);
        backdropAnim.addUpdateListener(a -> {
            int top = (Integer) a.getAnimatedValue();
            backdropTop = top;
            int mixed = ThemeColors.blend(top, surface, 0.45f);
            bgGradient.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{mixed, surface}));
        });
        backdropAnim.start();
    }

    private void updateShuffleUI() {
        boolean on = PlayQueue.get().isShuffle();
        tint(ivShuffle, on);
        tvShuffleLabel.setText(on ? "Shuffle on" : "Shuffle");
    }

    private void updateRepeatUI() {
        int rm = PlayQueue.get().getRepeatMode();
        ivRepeat.setImageResource(rm == PlayQueue.REPEAT_ONE ? R.drawable.ic_repeat_one : R.drawable.ic_repeat);
        tint(ivRepeat, rm != PlayQueue.REPEAT_NONE);
        tvRepeatLabel.setText(rm == PlayQueue.REPEAT_ONE ? "Repeat one" : rm == PlayQueue.REPEAT_ALL ? "Repeat all" : "Repeat");
    }

    private void updateQueueInfo() {
        int idx = PlayQueue.get().getIndex(), total = PlayQueue.get().size();
        tvQueueInfo.setText(total > 1 ? (idx + 1) + " of " + total : "Single track");
    }

    @Override public void onItemChanged(MediaItem item) {
        runOnUiThread(() -> { if (item != null && !isDestroyed()) updateTrackUI(item); });
    }
    @Override public void onPlayStateChanged(boolean p) {
        runOnUiThread(() -> {
            setPlayingUI(p);
            if (!p && bound && service.getSleepRemaining() == 0) tvSleepLabel.setText("Sleep");
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

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    private static String speedLabel(float s) { return (s == (int) s ? String.valueOf((int) s) + ".0" : String.valueOf(s)) + "×"; }

    private String fmt(long ms) {
        if (ms <= 0) return "0:00";
        long s = ms / 1000, m = s / 60, h = m / 60; s %= 60; m %= 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, s) : String.format(Locale.US, "%d:%02d", m, s);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (backdropAnim != null) backdropAnim.cancel();
        PlaybackState.get().removeListener(this);
        PlayQueue.get().removeListener(this);
        if (bound) { unbindService(connection); bound = false; }
    }

    // ─── Inner Fragment Pages ───────────────────────────────

    public static class NowPlayingPageFragment extends Fragment {
        private ImageView albumArt;
        @Override public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
            final View v = inf.inflate(R.layout.fragment_player_now_playing, c, false);
            albumArt = v.findViewById(R.id.iv_album_art);
            final View card = v.findViewById(R.id.art_card);
            // Square art that fits whatever room the pager has (portrait or landscape)
            v.post(() -> {
                int pad = (int) (48 * v.getResources().getDisplayMetrics().density);
                int size = Math.min(v.getWidth(), v.getHeight()) - pad;
                if (size > 0) {
                    ViewGroup.LayoutParams lp = card.getLayoutParams();
                    lp.width = size;
                    lp.height = size;
                    card.setLayoutParams(lp);
                }
            });
            MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur != null && cur.getAlbumArtUri() != null)
                Glide.with(requireContext()).load(cur.getAlbumArtUri()).centerCrop().into(albumArt);
            return v;
        }
        public void updateArt(String url) {
            if (albumArt == null || !isAdded()) return;
            if (url == null) { albumArt.setImageResource(R.drawable.bg_art_placeholder); return; }
            albumArt.animate().alpha(0).setDuration(150).withEndAction(() -> {
                if (!isAdded()) return;
                Glide.with(requireContext()).load(url).centerCrop().into(albumArt);
                albumArt.animate().alpha(1).setDuration(150).start();
            }).start();
        }
    }

    public static class LyricsPageFragment extends Fragment {
        @Override public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
            return inf.inflate(R.layout.fragment_player_lyrics, c, false);
        }
    }

    /** "Up next": tap a row to jump to it (forward or backward). Refreshes when the queue changes. */
    public static class QueuePageFragment extends Fragment implements PlayQueue.Listener {
        private RecyclerView rv;
        private TextView tvCount;

        @Override public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
            View v = inf.inflate(R.layout.fragment_player_queue, c, false);
            rv = v.findViewById(R.id.rv_queue);
            tvCount = v.findViewById(R.id.tv_queue_count);
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            bindQueue();
            return v;
        }

        @Override public void onStart() { super.onStart(); PlayQueue.get().addListener(this); bindQueue(); }
        @Override public void onStop() { super.onStop(); PlayQueue.get().removeListener(this); }

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
