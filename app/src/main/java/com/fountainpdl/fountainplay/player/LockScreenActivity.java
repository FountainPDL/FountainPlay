package com.fountainpdl.fountainplay.player;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.palette.graphics.Palette;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.service.PlaybackService;
import com.fountainpdl.fountainplay.util.ArtLoader;
import com.fountainpdl.fountainplay.util.Favourites;
import com.fountainpdl.fountainplay.util.Fx;
import com.fountainpdl.fountainplay.util.PlayQueue;
import com.fountainpdl.fountainplay.util.PlaybackState;
import com.fountainpdl.fountainplay.util.ThemeColors;
import com.fountainpdl.fountainplay.util.ThemeSetup;

/**
 * The lock-screen player: clock, spinning vinyl with cover art, controls and "Slide to unlock".
 * Shown over the keyguard while music plays. Open it with EXTRA_PREVIEW to try it without locking.
 */
public class LockScreenActivity extends AppCompatActivity implements PlaybackState.Listener, PlayQueue.Listener {

    public static final String EXTRA_PREVIEW = "preview";

    private boolean preview;
    private TextView tvTitle, tvArtist, tvPlayLabel, tvRepeatLabel, tvFavLabel, tvSlide;
    private ImageView ivArt, ivVinyl, ivPlay, ivRepeat, ivFav;
    private View backdrop;
    private ObjectAnimator spin;
    private boolean favourite = false;
    private String artPath;
    private int backdropTop;
    private ValueAnimator backdropAnim;

    private final BroadcastReceiver userPresent = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { if (!preview) finish(); }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeSetup.apply(this, true);
        preview = getIntent().getBooleanExtra(EXTRA_PREVIEW, false);

        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true);
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        boolean light = ThemeColors.isLight(ThemeColors.get(this, "colorSurface"));
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(light);
        c.setAppearanceLightNavigationBars(light);

        setContentView(R.layout.activity_lock_screen);
        bind();
        sizeArt();
        setupControls();
        setupSlide();
        backdropTop = ThemeColors.get(this, "colorSurface");

        PlaybackState.get().addListener(this);
        PlayQueue.get().addListener(this);
        ContextCompat.registerReceiver(this, userPresent, new IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED);

        if (preview) {
            View close = findViewById(R.id.btn_lock_close);
            close.setVisibility(View.VISIBLE);
            close.setOnClickListener(v -> finish());
        }
        MediaItem cur = PlaybackState.get().getCurrentItem();
        if (cur == null) cur = PlayQueue.get().current();
        if (cur != null) showItem(cur);
        else if (!preview) finish();
        updatePlaying(PlaybackState.get().isPlaying());
        refreshRepeat();
    }

    private void bind() {
        backdrop = findViewById(R.id.lock_backdrop);
        tvTitle = findViewById(R.id.tv_lock_title);
        tvArtist = findViewById(R.id.tv_lock_artist);
        tvPlayLabel = findViewById(R.id.tv_lock_play);
        tvRepeatLabel = findViewById(R.id.tv_lock_repeat);
        tvFavLabel = findViewById(R.id.tv_lock_fav);
        tvSlide = findViewById(R.id.tv_slide);
        ivArt = findViewById(R.id.iv_lock_art);
        ivVinyl = findViewById(R.id.iv_vinyl);
        ivPlay = findViewById(R.id.iv_lock_play);
        ivRepeat = findViewById(R.id.iv_lock_repeat);
        ivFav = findViewById(R.id.iv_lock_fav);
        tvTitle.setSelected(true);
    }

    /** Square cover art with the vinyl peeking out above it, sized to the room available. */
    private void sizeArt() {
        final View area = findViewById(R.id.art_area);
        area.post(() -> {
            int w = area.getWidth(), h = area.getHeight();
            int art = Math.max(1, Math.min((int) (w * 0.74f), (int) (h * 0.80f)));
            ViewGroup.LayoutParams a = ivArt.getLayoutParams();
            a.width = art; a.height = art;
            ivArt.setLayoutParams(a);
            ViewGroup.LayoutParams v = ivVinyl.getLayoutParams();
            v.width = art; v.height = art;
            ivVinyl.setLayoutParams(v);
        });
        ivVinyl.post(() -> ivVinyl.setPivotY(ivVinyl.getHeight() / 2f));
        spin = ObjectAnimator.ofFloat(ivVinyl, View.ROTATION, 0f, 360f);
        spin.setDuration(14000);
        spin.setInterpolator(new LinearInterpolator());
        spin.setRepeatCount(ValueAnimator.INFINITE);
    }

    private void setupControls() {
        findViewById(R.id.lock_play).setOnClickListener(v -> { send(PlaybackService.ACTION_PLAY_PAUSE); Fx.pop(ivPlay); });
        findViewById(R.id.lock_next).setOnClickListener(v -> send(PlaybackService.ACTION_NEXT));
        findViewById(R.id.lock_prev).setOnClickListener(v -> send(PlaybackService.ACTION_PREVIOUS));
        findViewById(R.id.lock_repeat).setOnClickListener(v -> { PlayQueue.get().cycleRepeat(); refreshRepeat(); });
        findViewById(R.id.lock_fav).setOnClickListener(v -> {
            final MediaItem cur = PlaybackState.get().getCurrentItem();
            if (cur == null) return;
            final Context app = getApplicationContext();
            new Thread(() -> {
                final boolean now = Favourites.toggle(app, cur);
                runOnUiThread(() -> { favourite = now; refreshFav(); Fx.pop(ivFav); });
            }).start();
        });
    }

    private void send(String action) {
        if (preview) return;
        startService(new Intent(this, PlaybackService.class).setAction(action));
    }

    /** Drag "Slide to unlock" to the right (or up) to dismiss the lock screen. */
    private void setupSlide() {
        final float[] down = new float[2];
        tvSlide.setOnTouchListener((v, e) -> {
            float travel = getResources().getDisplayMetrics().widthPixels * 0.35f;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    down[0] = e.getRawX(); down[1] = e.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    float dx = Math.max(0, e.getRawX() - down[0]);
                    v.setTranslationX(dx);
                    v.setAlpha(Math.max(0.3f, 1f - dx / (travel * 1.6f)));
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    float dx = e.getRawX() - down[0], dy = down[1] - e.getRawY();
                    if (e.getActionMasked() == MotionEvent.ACTION_UP && (dx > travel || dy > travel * 0.6f)) {
                        unlock();
                    } else {
                        v.animate().translationX(0f).alpha(1f).setDuration(220).start();
                    }
                    return true;
                }
                default: return false;
            }
        });
        // a gentle pulse so it is obvious that it can be slid
        ObjectAnimator pulse = ObjectAnimator.ofFloat(tvSlide, View.SCALE_X, 1f, 1.04f, 1f);
        pulse.setDuration(1800);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.start();
    }

    private void unlock() {
        if (preview) { finish(); return; }
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (km != null && km.isKeyguardLocked()) {
            km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                @Override public void onDismissSucceeded() { finish(); }
                @Override public void onDismissError() { finish(); }
                @Override public void onDismissCancelled() {
                    tvSlide.animate().translationX(0f).alpha(1f).setDuration(200).start();
                }
            });
        } else {
            finish();
        }
    }

    // ── State ───────────────────────────────────────────────────────────────────

    private void showItem(final MediaItem item) {
        tvTitle.setText(item.getTitle());
        tvArtist.setText(item.getArtist());
        artPath = item.getPath();
        final Context app = getApplicationContext();
        final String path = item.getPath();
        new Thread(() -> {
            final Bitmap art = ArtLoader.load(app, item, 720);
            boolean fav = false;
            try { fav = Favourites.isFavourite(app, path); } catch (Exception ignored) { }
            final boolean isFav = fav;
            runOnUiThread(() -> {
                if (isDestroyed() || path == null || !path.equals(artPath)) return;
                if (art != null) ivArt.setImageBitmap(art); else ivArt.setImageResource(R.drawable.bg_art_placeholder);
                favourite = isFav;
                refreshFav();
                tintBackdrop(art);
            });
        }).start();
    }

    private void tintBackdrop(Bitmap art) {
        final int surface = ThemeColors.get(this, "colorSurface");
        final int fallback = ThemeColors.get(this, "colorPrimaryContainer");
        int target = fallback;
        if (art != null) {
            Palette p = Palette.from(art).generate();
            target = p.getVibrantColor(p.getDominantColor(fallback));
        }
        if (backdropAnim != null) backdropAnim.cancel();
        backdropAnim = ValueAnimator.ofObject(new ArgbEvaluator(), backdropTop, target);
        backdropAnim.setDuration(500);
        backdropAnim.addUpdateListener(a -> {
            int top = (Integer) a.getAnimatedValue();
            backdropTop = top;
            backdrop.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ThemeColors.blend(top, surface, 0.5f), surface}));
        });
        backdropAnim.start();
    }

    private void updatePlaying(boolean playing) {
        ivPlay.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        tvPlayLabel.setText(playing ? "Pause" : "Play");
        if (playing) { if (!spin.isStarted()) spin.start(); else spin.resume(); }
        else if (spin.isRunning()) spin.pause();
    }

    private void refreshRepeat() {
        int m = PlayQueue.get().getRepeatMode();
        ivRepeat.setImageResource(m == PlayQueue.REPEAT_ONE ? R.drawable.ic_repeat_one : R.drawable.ic_repeat);
        ImageViewCompat.setImageTintList(ivRepeat, ColorStateList.valueOf(
            ThemeColors.get(this, m == PlayQueue.REPEAT_NONE ? "colorOnSurface" : "colorPrimary")));
        tvRepeatLabel.setText(m == PlayQueue.REPEAT_ONE ? "Repeat one" : m == PlayQueue.REPEAT_ALL ? "Repeat all" : "Repeat");
    }

    private void refreshFav() {
        ivFav.setImageResource(favourite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        ImageViewCompat.setImageTintList(ivFav, ColorStateList.valueOf(
            ThemeColors.get(this, favourite ? "colorPrimary" : "colorOnSurface")));
        tvFavLabel.setText(favourite ? "Liked" : "Like");
    }

    @Override public void onItemChanged(MediaItem item) {
        runOnUiThread(() -> { if (isDestroyed()) return; if (item == null) { if (!preview) finish(); } else showItem(item); });
    }
    @Override public void onPlayStateChanged(boolean playing) { runOnUiThread(() -> updatePlaying(playing)); }
    @Override public void onPositionChanged(long pos, long duration) { }
    @Override public void onQueueChanged() { runOnUiThread(this::refreshRepeat); }
    @Override public void onTrackChanged(MediaItem item, int index) { }

    /** Back dismisses the player like a swipe would, but never exits the lock screen unlocked. */
    @Override public void onBackPressed() { if (preview) finish(); else moveTaskToBack(true); }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (spin != null) spin.cancel();
        if (backdropAnim != null) backdropAnim.cancel();
        PlaybackState.get().removeListener(this);
        PlayQueue.get().removeListener(this);
        try { unregisterReceiver(userPresent); } catch (Exception ignored) { }
    }
}
